package com.vtsuellen.nlw.modules.students.useCases;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vtsuellen.nlw.modules.questions.entities.QuestionEntity;
import com.vtsuellen.nlw.modules.questions.repositories.QuestionRepository;
import com.vtsuellen.nlw.modules.students.dto.StudentCertificationAnswerDTO;
import com.vtsuellen.nlw.modules.students.dto.VerifyHasCertificationDTO;
import com.vtsuellen.nlw.modules.students.entities.AnswersCertificationEntity;
import com.vtsuellen.nlw.modules.students.entities.CertificationStudentEntity;
import com.vtsuellen.nlw.modules.students.entities.StudentEntity;
import com.vtsuellen.nlw.modules.students.repositories.CertificationStudentRepository;
import com.vtsuellen.nlw.modules.students.repositories.StudentRepository;

@Service
public class StudentCertificationAnswersUseCase {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CertificationStudentRepository certificationStudentRepository;

    @Autowired
    private VerifyIfHasCertificationUseCase verifyIfHasCertificationUseCase;

    public CertificationStudentEntity execute(StudentCertificationAnswerDTO dto) throws Exception {
        // Verificando se o aluno já possui certificação
        var hasCertification = this.verifyIfHasCertificationUseCase
                .execute(new VerifyHasCertificationDTO(dto.getEmail(), dto.getTechnology()));

        if (hasCertification) {
            throw new Exception("Você já tirou sua certificação!");
        }

        // Buscar as alternativas das perguntas
        // - Correct ou Incorreta
        // Recuperando perguntas para a tecnologia especificada
        List<QuestionEntity> questionsEntity = questionRepository.findByTechnology(dto.getTechnology());
        List<AnswersCertificationEntity> answersCertifications = new ArrayList<>();

        AtomicInteger correctAnswers = new AtomicInteger(0);

        // Iterando sobre as respostas do aluno
        dto.getQuestionsAnswers()
                .stream().forEach(questionAnswer -> {
                    var question = questionsEntity.stream()
                            .filter(q -> q.getId().equals(questionAnswer.getQuestionID())).findFirst().get();

                    var findCorrectAlternative = question.getAlternatives().stream()
                            .filter(alternative -> alternative.isCorrect()).findFirst().get();

                    // Verificando se a resposta fornecida está correta
                    if (findCorrectAlternative.getId().equals(questionAnswer.getAlternativeID())) {
                        questionAnswer.setCorrect(true);
                        correctAnswers.incrementAndGet();
                    } else {
                        questionAnswer.setCorrect(false);
                    }

                    // Criando objeto AnswersCertificationEntity para cada resposta
                    var answerrsCertificationsEntity = AnswersCertificationEntity.builder()
                            .answerID(questionAnswer.getAlternativeID())
                            .questionID(questionAnswer.getQuestionID())
                            .isCorrect(questionAnswer.isCorrect()).build();

                    answersCertifications.add(answerrsCertificationsEntity);
                });

        // Verificando se um aluno existe com base no e-mail
        var student = studentRepository.findByEmail(dto.getEmail());
        UUID studentID;
        if (student.isEmpty()) {
            var studentCreated = StudentEntity.builder().email(dto.getEmail()).build();
            studentCreated = studentRepository.save(studentCreated);
            studentID = studentCreated.getId();
        } else {
            studentID = student.get().getId();
        }

        // Criando objeto CertificationStudentEntity para armazenar informações da
        // certificação
        CertificationStudentEntity certificationStudentEntity = CertificationStudentEntity.builder()
                .technology(dto.getTechnology())
                .studentID(studentID)
                .grade(correctAnswers.get())
                .build();

        // Salvando informações da certificação
        var certificationStudentCreated = certificationStudentRepository.save(certificationStudentEntity);

        // Associando respostas à certificação
        answersCertifications.stream().forEach(answerCertification -> {
            answerCertification.setCertificationID(certificationStudentEntity.getId());
            answerCertification.setCertificationStudentEntity(certificationStudentEntity);
        });

        // Definindo respostas para a entidade de certificação
        certificationStudentEntity.setAnswersCertificationsEntities(answersCertifications);

        certificationStudentRepository.save(certificationStudentEntity);

        return certificationStudentCreated;
        // Retornando a certificação criada
    }
}

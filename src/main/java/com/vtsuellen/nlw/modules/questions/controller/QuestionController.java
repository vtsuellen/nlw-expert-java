package com.vtsuellen.nlw.modules.questions.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vtsuellen.nlw.modules.questions.dto.AlternativesResultDTO;
import com.vtsuellen.nlw.modules.questions.dto.QuestionResultDTO;
import com.vtsuellen.nlw.modules.questions.entities.AlternativesEntity;
import com.vtsuellen.nlw.modules.questions.entities.QuestionEntity;
import com.vtsuellen.nlw.modules.questions.repositories.QuestionRepository;

@RestController
@RequestMapping("/questions")
public class QuestionController {

  @Autowired
  private QuestionRepository questionRepository;

  @GetMapping("/technology/{technology}")
  public List<QuestionResultDTO> findByTechnology(@PathVariable String technology) {
    // Imprime a tecnologia para fins de depuração
    System.out.println("TECH === " + technology);
    // Busca as perguntas no repositório baseadas na tecnologia fornecida
    var result = this.questionRepository.findByTechnology(technology);

    // Mapeia as perguntas para DTOs e as coleta em uma lista
    var toMap = result.stream().map(question -> mapQuestionToDTO(question))
        .collect(Collectors.toList());
    return toMap;
  }

  // Método para mapear uma entidade de pergunta para seu respectivo DTO
  static QuestionResultDTO mapQuestionToDTO(QuestionEntity question) {
    var questionResultDTO = QuestionResultDTO.builder()
        .id(question.getId())
        .technology(question.getTechnology())
        .description(question.getDescription()).build();

    // Mapeia as alternativas da pergunta para DTOs e as coleta em uma lista
    List<AlternativesResultDTO> alternativesResultDTOs = question.getAlternatives()
        .stream().map(alternative -> mapAlternativeDTO(alternative))
        .collect(Collectors.toList());

    // Define as alternativas no DTO da pergunta
    questionResultDTO.setAlternatives(alternativesResultDTOs);
    return questionResultDTO;
  }

  // Método para mapear uma entidade de alternativa para seu respectivo DTO
  static AlternativesResultDTO mapAlternativeDTO(AlternativesEntity alternativesResultDTO) {
    return AlternativesResultDTO.builder()
        .id(alternativesResultDTO.getId())
        .description(alternativesResultDTO.getDescription()).build();
  }
}
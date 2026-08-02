package com.bankingtest_kotlin.data

import kotlinx.serialization.Serializable

@Serializable
data class QuizDto(
    val id: String,
    val title: String,
    val mainImageName: String? = null,
    val questions: List<QuestionDto>,
    val results: List<QuizResultDto>
)

@Serializable
data class QuestionDto(
    val id: Int,
    val text: String,
    val imageName: String? = null,
    val answers: List<AnswerDto>
)

@Serializable
data class AnswerDto(
    val text: String,
    val resultId: Int? = null,
    val scores: Map<String, Int> = emptyMap()
)

@Serializable
data class QuizResultDto(
    val id: Int,
    val text: String,
    val imageName: String? = null
)

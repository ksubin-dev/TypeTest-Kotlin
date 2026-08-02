package com.bankingtest_kotlin.domain

data class Quiz(
    val id: String,
    val title: String,
    val mainImageName: String?,
    val mainImageResId: Int?,
    val questions: List<Question>,
    val results: List<QuizResult>
)

data class Question(
    val id: Int,
    val text: String,
    val imageName: String?,
    val imageResId: Int?,
    val answers: List<Answer>
)

data class Answer(
    val text: String,
    val resultScores: Map<Int, Int>
)

data class QuizResult(
    val id: Int,
    val text: String,
    val imageName: String?,
    val imageResId: Int?
)

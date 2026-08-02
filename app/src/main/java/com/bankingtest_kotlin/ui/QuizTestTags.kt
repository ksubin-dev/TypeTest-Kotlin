package com.bankingtest_kotlin.ui

object QuizTestTags {
    const val MainScreen = "main_screen"
    const val StartButton = "start_button"
    const val QuizScreen = "quiz_screen"
    const val QuestionText = "question_text"
    const val AnswerButtonPrefix = "answer_button_"
    const val ResultScreen = "result_screen"
    const val ResultText = "result_text"
    const val RestartButton = "restart_button"

    fun answerButton(index: Int): String = "$AnswerButtonPrefix$index"
}

package com.bankingtest_kotlin.domain.calculator

import com.bankingtest_kotlin.domain.Answer
import com.bankingtest_kotlin.domain.QuizResult

interface ResultCalculator {
    fun calculate(
        selectedAnswers: List<Answer>,
        results: List<QuizResult>
    ): QuizResult
}

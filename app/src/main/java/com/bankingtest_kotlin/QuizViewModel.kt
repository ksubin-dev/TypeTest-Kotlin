package com.bankingtest_kotlin

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.bankingtest_kotlin.data.AndroidDrawableResourceMapper
import com.bankingtest_kotlin.data.AssetQuizRepository
import com.bankingtest_kotlin.data.QuizMapper
import com.bankingtest_kotlin.domain.Answer
import com.bankingtest_kotlin.domain.QuizResult
import com.bankingtest_kotlin.domain.calculator.ResultCalculator
import com.bankingtest_kotlin.domain.calculator.ScoreBasedResultCalculator

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AssetQuizRepository(
        assetManager = application.assets,
        quizMapper = QuizMapper(
            AndroidDrawableResourceMapper(
                resources = application.resources,
                packageName = application.packageName
            )
        )
    )
    private val quiz = repository.getBankingQuiz()
    private val resultCalculator: ResultCalculator = ScoreBasedResultCalculator(
        defaultResultId = DEFAULT_RESULT_ID
    )

    val questions = quiz.questions
    val results = quiz.results

    private val selectedAnswers = mutableStateListOf<Answer>()

    fun addAnswer(answer: Answer) {
        selectedAnswers.add(answer)
    }

    fun getFinalResult(): QuizResult {
        return resultCalculator.calculate(
            selectedAnswers = selectedAnswers,
            results = results
        )
    }

    fun resetQuiz() {
        selectedAnswers.clear()
    }

    private companion object {
        const val DEFAULT_RESULT_ID = 3
    }
}

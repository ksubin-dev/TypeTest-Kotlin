package com.bankingtest_kotlin.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bankingtest_kotlin.data.AndroidDrawableResourceMapper
import com.bankingtest_kotlin.data.AssetQuizRepository
import com.bankingtest_kotlin.data.QuizMapper
import com.bankingtest_kotlin.domain.Answer
import com.bankingtest_kotlin.domain.Question
import com.bankingtest_kotlin.domain.Quiz
import com.bankingtest_kotlin.domain.QuizResult
import com.bankingtest_kotlin.domain.calculator.ResultCalculator
import com.bankingtest_kotlin.domain.calculator.ScoreBasedResultCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuizViewModel(
    private val quiz: Quiz,
    private val resultCalculator: ResultCalculator = ScoreBasedResultCalculator(
        defaultResultId = DEFAULT_RESULT_ID
    )
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuizUiState.from(quiz))
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    fun startQuiz() {
        _uiState.value = QuizUiState.from(quiz)
    }

    fun selectAnswer(answer: Answer): QuizDestination {
        val state = _uiState.value
        val currentQuestion = state.currentQuestion ?: return state.currentDestination

        val selectedAnswers = state.selectedAnswersByQuestionId + (currentQuestion.id to answer)
        val nextQuestionIndex = state.currentQuestionIndex + 1

        return if (nextQuestionIndex < quiz.questions.size) {
            _uiState.value = state.copy(
                currentQuestionIndex = nextQuestionIndex,
                currentQuestion = quiz.questions[nextQuestionIndex],
                selectedAnswersByQuestionId = selectedAnswers,
                result = null
            )
            QuizDestination.Question
        } else {
            val result = resultCalculator.calculate(
                selectedAnswers = selectedAnswers.values.toList(),
                results = quiz.results
            )
            _uiState.value = state.copy(
                currentQuestion = null,
                selectedAnswersByQuestionId = selectedAnswers,
                result = result
            )
            QuizDestination.Result
        }
    }

    fun showQuestion(index: Int): Boolean {
        if (index !in quiz.questions.indices) {
            return false
        }

        _uiState.value = _uiState.value.copy(
            currentQuestionIndex = index,
            currentQuestion = quiz.questions[index],
            result = null
        )
        return true
    }

    fun restartQuiz() {
        startQuiz()
    }

    companion object {
        private const val DEFAULT_RESULT_ID = 3

        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
                        return QuizViewModel(loadBankingQuiz(application)) as T
                    }

                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }

        private fun loadBankingQuiz(application: Application): Quiz {
            val repository = AssetQuizRepository(
                assetManager = application.assets,
                quizMapper = QuizMapper(
                    AndroidDrawableResourceMapper(
                        resources = application.resources,
                        packageName = application.packageName
                    )
                )
            )
            return repository.getBankingQuiz()
        }
    }
}

data class QuizUiState(
    val title: String,
    val totalQuestionCount: Int,
    val currentQuestionIndex: Int,
    val currentQuestion: Question?,
    val selectedAnswersByQuestionId: Map<Int, Answer>,
    val result: QuizResult?
) {
    val currentDestination: QuizDestination
        get() = if (result == null) {
            QuizDestination.Question
        } else {
            QuizDestination.Result
        }

    companion object {
        fun from(quiz: Quiz): QuizUiState {
            return QuizUiState(
                title = quiz.title,
                totalQuestionCount = quiz.questions.size,
                currentQuestionIndex = 0,
                currentQuestion = quiz.questions.firstOrNull(),
                selectedAnswersByQuestionId = emptyMap(),
                result = null
            )
        }
    }
}

enum class QuizDestination {
    Question,
    Result
}

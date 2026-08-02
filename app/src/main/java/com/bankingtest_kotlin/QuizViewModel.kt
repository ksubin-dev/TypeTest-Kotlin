package com.bankingtest_kotlin

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.bankingtest_kotlin.data.AndroidDrawableResourceMapper
import com.bankingtest_kotlin.data.AssetQuizRepository
import com.bankingtest_kotlin.data.QuizMapper
import com.bankingtest_kotlin.domain.QuizResult

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

    val questions = quiz.questions
    val results = quiz.results

    // 사용자의 답변 빈도를 저장할 리스트
    private val answerCounts = mutableStateListOf<Int>().apply {
        repeat((results.maxOfOrNull { it.id } ?: 0) + 1) {
            add(0)
        }
    }

    fun addAnswer(resultId: Int) {
        if (resultId >= 1 && resultId < answerCounts.size) {
            answerCounts[resultId] = answerCounts[resultId] + 1
        }
    }

    fun getFinalResult(): QuizResult {
        // 가장 높은 빈도수를 가진 resultId 찾기
        val maxCount = answerCounts.maxOrNull() ?: 0
        val finalResultId = if (maxCount > 0) {
            answerCounts.indexOf(maxCount)
        } else {
            // 모든 답변이 0일 경우 기본값 (ex: resultId = 3)
            3
        }
        return results.first { it.id == finalResultId }
    }

    fun resetQuiz() {
        // 퀴즈를 다시 시작할 때 답변 카운트를 초기화
        for (i in 1 until answerCounts.size) {
            answerCounts[i] = 0
        }
    }
}

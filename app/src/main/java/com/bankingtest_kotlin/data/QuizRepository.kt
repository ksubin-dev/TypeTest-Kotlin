package com.bankingtest_kotlin.data

import android.content.res.AssetManager
import com.bankingtest_kotlin.domain.Quiz

interface QuizRepository {
    fun getBankingQuiz(): Quiz
}

class AssetQuizRepository(
    private val assetManager: AssetManager,
    private val quizMapper: QuizMapper,
    private val quizJsonParser: QuizJsonParser = QuizJsonParser()
) : QuizRepository {
    override fun getBankingQuiz(): Quiz {
        return loadQuiz(BANKING_QUIZ_ASSET_PATH)
    }

    private fun loadQuiz(assetPath: String): Quiz {
        val rawJson = assetManager.open(assetPath).bufferedReader().use { it.readText() }
        return quizMapper.map(quizJsonParser.parse(rawJson))
    }

    private companion object {
        const val BANKING_QUIZ_ASSET_PATH = "quizzes/banking.json"
    }
}

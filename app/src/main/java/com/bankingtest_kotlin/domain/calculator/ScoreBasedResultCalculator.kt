package com.bankingtest_kotlin.domain.calculator

import com.bankingtest_kotlin.domain.Answer
import com.bankingtest_kotlin.domain.QuizResult

class ScoreBasedResultCalculator(
    private val defaultResultId: Int? = null
) : ResultCalculator {
    override fun calculate(
        selectedAnswers: List<Answer>,
        results: List<QuizResult>
    ): QuizResult {
        require(results.isNotEmpty()) { "results must not be empty." }

        val resultIds = results.map { it.id }.toSet()
        val defaultResult = findDefaultResult(results)
        val scores = results.associate { it.id to 0 }.toMutableMap()

        selectedAnswers.forEach { answer ->
            require(answer.resultScores.isNotEmpty()) {
                "answer must have at least one result score."
            }

            answer.resultScores.forEach { (resultId, score) ->
                require(resultId in resultIds) { "Unknown result id: $resultId" }
                require(score >= 0) { "Result score must not be negative: $score" }

                scores[resultId] = scores.getValue(resultId) + score
            }
        }

        val maxScore = scores.values.maxOrNull() ?: 0
        if (maxScore == 0) {
            return defaultResult
        }

        return results.first { result ->
            scores.getValue(result.id) == maxScore
        }
    }

    private fun findDefaultResult(results: List<QuizResult>): QuizResult {
        return defaultResultId
            ?.let { resultId ->
                results.firstOrNull { it.id == resultId }
                    ?: error("Default result id does not exist: $resultId")
            }
            ?: results.last()
    }
}

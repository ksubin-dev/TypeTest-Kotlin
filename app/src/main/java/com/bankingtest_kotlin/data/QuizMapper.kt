package com.bankingtest_kotlin.data

import com.bankingtest_kotlin.domain.Answer
import com.bankingtest_kotlin.domain.Question
import com.bankingtest_kotlin.domain.Quiz
import com.bankingtest_kotlin.domain.QuizResult

class QuizMapper(
    private val drawableResourceMapper: DrawableResourceMapper
) {
    fun map(dto: QuizDto): Quiz {
        return Quiz(
            id = dto.id,
            title = dto.title,
            mainImageName = dto.mainImageName,
            mainImageResId = dto.mainImageName?.let(drawableResourceMapper::resolveDrawableId),
            questions = dto.questions.map(::mapQuestion),
            results = dto.results.map(::mapResult)
        )
    }

    private fun mapQuestion(dto: QuestionDto): Question {
        return Question(
            id = dto.id,
            text = dto.text,
            imageName = dto.imageName,
            imageResId = dto.imageName?.let(drawableResourceMapper::resolveDrawableId),
            answers = dto.answers.map { answer ->
                Answer(
                    text = answer.text,
                    resultScores = answer.toResultScores()
                )
            }
        )
    }

    private fun AnswerDto.toResultScores(): Map<Int, Int> {
        val mappedScores = scores.mapKeys { (resultId, _) ->
            resultId.toIntOrNull()
                ?: throw IllegalArgumentException("Result score key must be an integer: $resultId")
        }

        return when {
            mappedScores.isNotEmpty() -> mappedScores
            resultId != null -> mapOf(resultId to DEFAULT_RESULT_SCORE)
            else -> throw IllegalArgumentException("Answer must define scores or resultId.")
        }
    }

    private fun mapResult(dto: QuizResultDto): QuizResult {
        return QuizResult(
            id = dto.id,
            text = dto.text,
            imageName = dto.imageName,
            imageResId = dto.imageName?.let(drawableResourceMapper::resolveDrawableId)
        )
    }

    private companion object {
        const val DEFAULT_RESULT_SCORE = 1
    }
}

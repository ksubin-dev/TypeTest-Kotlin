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
                    resultId = answer.resultId
                )
            }
        )
    }

    private fun mapResult(dto: QuizResultDto): QuizResult {
        return QuizResult(
            id = dto.id,
            text = dto.text,
            imageName = dto.imageName,
            imageResId = dto.imageName?.let(drawableResourceMapper::resolveDrawableId)
        )
    }
}

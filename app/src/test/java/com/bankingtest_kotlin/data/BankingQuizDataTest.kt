package com.bankingtest_kotlin.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BankingQuizDataTest {
    private val bankingJson = File("src/main/assets/quizzes/banking.json").readText()
    private val quizDto = QuizJsonParser().parse(bankingJson)

    @Test
    fun `금융 테스트 JSON은 파싱된다`() {
        assertEquals("banking", quizDto.id)
        assertEquals("금융 테스트", quizDto.title)
    }

    @Test
    fun `금융 테스트 JSON은 질문 6개를 가진다`() {
        assertEquals(6, quizDto.questions.size)
    }

    @Test
    fun `모든 질문은 최소 두 개 이상의 답변을 가진다`() {
        quizDto.questions.forEach { question ->
            assertTrue(
                "${question.id}번 질문의 답변이 부족합니다.",
                question.answers.size >= 2
            )
        }
    }

    @Test
    fun `모든 답변 score는 정의된 결과와 연결된다`() {
        val resultIds = quizDto.results.map { it.id }.toSet()

        quizDto.questions
            .flatMap { it.answers }
            .forEach { answer ->
                assertTrue("답변 score가 비어 있습니다.", answer.scores.isNotEmpty())

                assertTrue(
                    "${answer.scores.keys} 결과가 정의되어 있지 않습니다.",
                    answer.scores.keys.all { it.toInt() in resultIds }
                )

                assertTrue(
                    "${answer.scores} 점수는 0보다 커야 합니다.",
                    answer.scores.values.all { it > 0 }
                )
            }
    }

    @Test
    fun `모든 이미지 이름은 drawable 리소스 파일과 연결된다`() {
        val drawableNames = File("src/main/res/drawable")
            .listFiles()
            .orEmpty()
            .map { it.nameWithoutExtension }
            .toSet()

        val imageNames = buildList {
            quizDto.mainImageName?.let(::add)
            addAll(quizDto.questions.mapNotNull { it.imageName })
            addAll(quizDto.results.mapNotNull { it.imageName })
        }

        imageNames.forEach { imageName ->
            assertTrue("$imageName 이미지 리소스를 찾을 수 없습니다.", imageName in drawableNames)
        }
    }

    @Test
    fun `JSON 데이터는 도메인 모델로 변환된다`() {
        val quiz = quizMapper().map(quizDto)

        assertEquals("banking", quiz.id)
        assertEquals(6, quiz.questions.size)
        assertEquals(3, quiz.results.size)
        assertNotNull(quiz.mainImageResId)
        assertTrue(quiz.questions.all { it.imageResId != null })
        assertTrue(quiz.results.all { it.imageResId != null })
    }

    @Test
    fun `기존 resultId 형식의 답변도 score map으로 변환된다`() {
        val quiz = quizMapper().map(
            quizDto.copy(
                questions = listOf(
                    quizDto.questions.first().copy(
                        answers = listOf(
                            AnswerDto(
                                text = "기존 형식 답변",
                                resultId = 1
                            )
                        )
                    )
                )
            )
        )

        assertEquals(mapOf(1 to 1), quiz.questions.first().answers.first().resultScores)
    }

    @Test
    fun `score key가 숫자가 아니면 도메인 모델 변환에 실패한다`() {
        val invalidQuiz = quizDto.copy(
            questions = listOf(
                quizDto.questions.first().copy(
                    answers = listOf(
                        AnswerDto(
                            text = "잘못된 score key",
                            scores = mapOf("expert" to 1)
                        )
                    )
                )
            )
        )

        assertThrows(IllegalArgumentException::class.java) {
            quizMapper().map(invalidQuiz)
        }
    }

    @Test
    fun `score와 resultId가 모두 없으면 도메인 모델 변환에 실패한다`() {
        val invalidQuiz = quizDto.copy(
            questions = listOf(
                quizDto.questions.first().copy(
                    answers = listOf(
                        AnswerDto(text = "점수가 없는 답변")
                    )
                )
            )
        )

        assertThrows(IllegalArgumentException::class.java) {
            quizMapper().map(invalidQuiz)
        }
    }

    private fun quizMapper(): QuizMapper {
        return QuizMapper(
            object : DrawableResourceMapper {
                override fun resolveDrawableId(name: String): Int = 1
            }
        )
    }
}

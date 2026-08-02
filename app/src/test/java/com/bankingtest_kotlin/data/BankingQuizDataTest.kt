package com.bankingtest_kotlin.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `모든 답변 resultId는 정의된 결과와 연결된다`() {
        val resultIds = quizDto.results.map { it.id }.toSet()

        quizDto.questions
            .flatMap { it.answers }
            .forEach { answer ->
                assertTrue(
                    "${answer.resultId} 결과가 정의되어 있지 않습니다.",
                    answer.resultId in resultIds
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
        val quiz = QuizMapper(
            object : DrawableResourceMapper {
                override fun resolveDrawableId(name: String): Int = 1
            }
        ).map(quizDto)

        assertEquals("banking", quiz.id)
        assertEquals(6, quiz.questions.size)
        assertEquals(3, quiz.results.size)
        assertNotNull(quiz.mainImageResId)
        assertTrue(quiz.questions.all { it.imageResId != null })
        assertTrue(quiz.results.all { it.imageResId != null })
    }
}

package com.bankingtest_kotlin.domain.calculator

import com.bankingtest_kotlin.domain.Answer
import com.bankingtest_kotlin.domain.QuizResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScoreBasedResultCalculatorTest {
    private val results = listOf(
        result(id = 1, text = "완벽한 재테크 고수"),
        result(id = 2, text = "아직 더 성장할 수 있는 묘목"),
        result(id = 3, text = "무궁무진한 성장성을 가진 새싹")
    )

    private val calculator = ScoreBasedResultCalculator(defaultResultId = 3)

    @Test
    fun `전문가 점수가 가장 높으면 전문가 결과를 반환한다`() {
        val selectedAnswers = listOf(
            answer(resultId = 1),
            answer(resultId = 1),
            answer(resultId = 2)
        )

        val actual = calculator.calculate(selectedAnswers, results)

        assertEquals(1, actual.id)
    }

    @Test
    fun `성장형 점수가 가장 높으면 성장형 결과를 반환한다`() {
        val selectedAnswers = listOf(
            answer(resultId = 2),
            answer(resultId = 2),
            answer(resultId = 3)
        )

        val actual = calculator.calculate(selectedAnswers, results)

        assertEquals(2, actual.id)
    }

    @Test
    fun `초보형 점수가 가장 높으면 초보형 결과를 반환한다`() {
        val selectedAnswers = listOf(
            answer(resultId = 3),
            answer(resultId = 3),
            answer(resultId = 1)
        )

        val actual = calculator.calculate(selectedAnswers, results)

        assertEquals(3, actual.id)
    }

    @Test
    fun `한 답변이 여러 결과에 점수를 줄 수 있다`() {
        val selectedAnswers = listOf(
            answer(scores = mapOf(1 to 2, 2 to 1)),
            answer(resultId = 2),
            answer(resultId = 3)
        )

        val actual = calculator.calculate(selectedAnswers, results)

        assertEquals(1, actual.id)
    }

    @Test
    fun `동점이면 결과 목록에서 먼저 정의된 결과를 반환한다`() {
        val selectedAnswers = listOf(
            answer(resultId = 1),
            answer(resultId = 2)
        )

        val actual = calculator.calculate(selectedAnswers, results)

        assertEquals(1, actual.id)
    }

    @Test
    fun `선택한 답변이 없으면 기본 결과를 반환한다`() {
        val actual = calculator.calculate(emptyList(), results)

        assertEquals(3, actual.id)
    }

    @Test
    fun `모든 점수가 0이면 기본 결과를 반환한다`() {
        val selectedAnswers = listOf(
            answer(scores = mapOf(1 to 0, 2 to 0))
        )

        val actual = calculator.calculate(selectedAnswers, results)

        assertEquals(3, actual.id)
    }

    @Test
    fun `알 수 없는 결과 id가 있으면 예외를 던진다`() {
        val selectedAnswers = listOf(answer(resultId = 99))

        assertThrows(IllegalArgumentException::class.java) {
            calculator.calculate(selectedAnswers, results)
        }
    }

    @Test
    fun `음수 점수가 있으면 예외를 던진다`() {
        val selectedAnswers = listOf(answer(scores = mapOf(1 to -1)))

        assertThrows(IllegalArgumentException::class.java) {
            calculator.calculate(selectedAnswers, results)
        }
    }

    @Test
    fun `답변 score가 비어 있으면 예외를 던진다`() {
        val selectedAnswers = listOf(answer(scores = emptyMap()))

        assertThrows(IllegalArgumentException::class.java) {
            calculator.calculate(selectedAnswers, results)
        }
    }

    @Test
    fun `결과 목록이 비어 있으면 예외를 던진다`() {
        assertThrows(IllegalArgumentException::class.java) {
            calculator.calculate(listOf(answer(resultId = 1)), emptyList())
        }
    }

    @Test
    fun `기본 결과 id가 결과 목록에 없으면 예외를 던진다`() {
        val calculator = ScoreBasedResultCalculator(defaultResultId = 99)

        assertThrows(IllegalStateException::class.java) {
            calculator.calculate(emptyList(), results)
        }
    }

    @Test
    fun `기본 결과 id를 지정하지 않으면 마지막 결과를 기본 결과로 사용한다`() {
        val calculator = ScoreBasedResultCalculator()

        val actual = calculator.calculate(emptyList(), results)

        assertEquals(3, actual.id)
    }

    private fun answer(resultId: Int): Answer {
        return answer(scores = mapOf(resultId to 1))
    }

    private fun answer(scores: Map<Int, Int>): Answer {
        return Answer(
            text = "답변",
            resultScores = scores
        )
    }

    private fun result(id: Int, text: String): QuizResult {
        return QuizResult(
            id = id,
            text = text,
            imageName = null,
            imageResId = null
        )
    }
}

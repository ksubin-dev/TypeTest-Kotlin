package com.bankingtest_kotlin.presentation

import com.bankingtest_kotlin.domain.Answer
import com.bankingtest_kotlin.domain.Question
import com.bankingtest_kotlin.domain.Quiz
import com.bankingtest_kotlin.domain.QuizResult
import com.bankingtest_kotlin.domain.calculator.ScoreBasedResultCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizViewModelTest {
    private val quiz = Quiz(
        id = "banking",
        title = "금융 테스트",
        mainImageName = null,
        mainImageResId = null,
        questions = listOf(
            question(
                id = 10,
                answers = listOf(
                    answer(text = "전문가 답변", resultId = 1),
                    answer(text = "성장형 답변", resultId = 2)
                )
            ),
            question(
                id = 20,
                answers = listOf(
                    answer(text = "전문가 답변", resultId = 1),
                    answer(text = "초보형 답변", resultId = 3)
                )
            )
        ),
        results = listOf(
            result(id = 1, text = "전문가"),
            result(id = 2, text = "성장형"),
            result(id = 3, text = "초보형")
        )
    )

    @Test
    fun `초기 상태에서 첫 질문을 표시한다`() {
        val viewModel = viewModel()
        val state = viewModel.uiState.value

        assertEquals("금융 테스트", state.title)
        assertEquals(2, state.totalQuestionCount)
        assertEquals(0, state.currentQuestionIndex)
        assertEquals(10, state.currentQuestion?.id)
        assertTrue(state.selectedAnswersByQuestionId.isEmpty())
        assertNull(state.result)
    }

    @Test
    fun `답변을 선택하면 다음 질문 상태로 이동한다`() {
        val viewModel = viewModel()

        val destination = viewModel.selectAnswer(quiz.questions[0].answers[0])
        val state = viewModel.uiState.value

        assertEquals(QuizDestination.Question, destination)
        assertEquals(1, state.currentQuestionIndex)
        assertEquals(20, state.currentQuestion?.id)
        assertEquals(1, state.selectedAnswersByQuestionId.size)
        assertEquals(quiz.questions[0].answers[0], state.selectedAnswersByQuestionId[10])
        assertNull(state.result)
    }

    @Test
    fun `마지막 질문에 답변하면 결과 상태로 전환된다`() {
        val viewModel = viewModel()

        viewModel.selectAnswer(quiz.questions[0].answers[0])
        val destination = viewModel.selectAnswer(quiz.questions[1].answers[0])
        val state = viewModel.uiState.value

        assertEquals(QuizDestination.Result, destination)
        assertNull(state.currentQuestion)
        assertEquals(1, state.result?.id)
        assertEquals(QuizDestination.Result, state.currentDestination)
    }

    @Test
    fun `같은 질문의 답변을 변경하면 이전 답변을 중복 집계하지 않는다`() {
        val viewModel = viewModel()

        viewModel.selectAnswer(quiz.questions[0].answers[0])
        assertTrue(viewModel.showQuestion(0))
        viewModel.selectAnswer(quiz.questions[0].answers[1])
        viewModel.selectAnswer(quiz.questions[1].answers[1])

        val state = viewModel.uiState.value

        assertEquals(2, state.selectedAnswersByQuestionId.size)
        assertEquals(quiz.questions[0].answers[1], state.selectedAnswersByQuestionId[10])
        assertEquals(2, state.result?.id)
    }

    @Test
    fun `다시 시작하면 선택 답변과 결과를 초기화한다`() {
        val viewModel = viewModel()

        viewModel.selectAnswer(quiz.questions[0].answers[0])
        viewModel.selectAnswer(quiz.questions[1].answers[0])
        viewModel.restartQuiz()

        val state = viewModel.uiState.value

        assertEquals(0, state.currentQuestionIndex)
        assertEquals(10, state.currentQuestion?.id)
        assertTrue(state.selectedAnswersByQuestionId.isEmpty())
        assertNull(state.result)
    }

    @Test
    fun `잘못된 질문 index를 요청하면 상태를 변경하지 않는다`() {
        val viewModel = viewModel()
        val before = viewModel.uiState.value

        val negativeResult = viewModel.showQuestion(-1)
        val overflowResult = viewModel.showQuestion(quiz.questions.size)

        assertFalse(negativeResult)
        assertFalse(overflowResult)
        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun `결과 상태에서 답변 선택을 다시 요청하면 현재 결과 상태를 유지한다`() {
        val viewModel = viewModel()

        viewModel.selectAnswer(quiz.questions[0].answers[0])
        viewModel.selectAnswer(quiz.questions[1].answers[0])
        val destination = viewModel.selectAnswer(quiz.questions[1].answers[1])

        assertEquals(QuizDestination.Result, destination)
        assertEquals(1, viewModel.uiState.value.result?.id)
    }

    private fun viewModel(): QuizViewModel {
        return QuizViewModel(
            quiz = quiz,
            resultCalculator = ScoreBasedResultCalculator(defaultResultId = 3)
        )
    }

    private fun question(id: Int, answers: List<Answer>): Question {
        return Question(
            id = id,
            text = "$id 번 질문",
            imageName = null,
            imageResId = null,
            answers = answers
        )
    }

    private fun answer(text: String, resultId: Int): Answer {
        return Answer(
            text = text,
            resultScores = mapOf(resultId to 1)
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

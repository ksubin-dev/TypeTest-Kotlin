package com.bankingtest_kotlin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.bankingtest_kotlin.ui.QuizTestTags
import org.junit.Rule
import org.junit.Test

class QuizFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bankingQuizFlow_fromStartToResultAndRestart() {
        composeRule.onNodeWithTag(QuizTestTags.MainScreen).assertIsDisplayed()

        composeRule.onNodeWithTag(QuizTestTags.StartButton).performClick()

        composeRule.onNodeWithTag(QuizTestTags.QuizScreen).assertIsDisplayed()
        composeRule.onNodeWithTag(QuizTestTags.QuestionText)
            .assertTextContains("당신은 평소에 어떤 금융 습관을 가지고 있나요?")

        repeat(BANKING_QUESTION_COUNT) { questionIndex ->
            composeRule.onNodeWithTag(QuizTestTags.answerButton(0)).performClick()
            composeRule.waitForIdle()

            if (questionIndex < BANKING_QUESTION_COUNT - 1) {
                waitUntilNodeExists(QuizTestTags.QuizScreen)
            }
        }

        waitUntilNodeExists(QuizTestTags.ResultScreen)
        composeRule.onNodeWithTag(QuizTestTags.ResultScreen).assertIsDisplayed()
        composeRule.onNodeWithTag(QuizTestTags.ResultText)
            .assertTextContains("완벽한 재테크 고수")

        composeRule.onNodeWithTag(QuizTestTags.RestartButton).performClick()
        composeRule.waitForIdle()

        waitUntilNodeExists(QuizTestTags.MainScreen, QuizTestTags.QuizScreen)
        if (nodeExists(QuizTestTags.MainScreen)) {
            composeRule.onNodeWithTag(QuizTestTags.StartButton).performClick()
        }

        waitUntilNodeExists(QuizTestTags.QuizScreen)
        composeRule.onNodeWithTag(QuizTestTags.QuizScreen).assertIsDisplayed()
        composeRule.onNodeWithTag(QuizTestTags.QuestionText)
            .assertTextContains("당신은 평소에 어떤 금융 습관을 가지고 있나요?")
    }

    private fun waitUntilNodeExists(vararg tags: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            tags.any(::nodeExists)
        }
    }

    private fun nodeExists(tag: String): Boolean {
        return composeRule.onAllNodesWithTag(tag)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }

    private companion object {
        const val BANKING_QUESTION_COUNT = 6
    }
}

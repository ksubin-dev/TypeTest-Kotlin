package com.bankingtest_kotlin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankingtest_kotlin.domain.Question
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.ContentScale
import com.bankingtest_kotlin.domain.Answer
import com.bankingtest_kotlin.ui.QuizTestTags

@Composable
fun QuizScreen(
    question: Question,
    onAnswerSelected: (Answer) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(QuizTestTags.QuizScreen)
    ) {
        // 배경 이미지
        question.imageResId?.let { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 답변 버튼들을 화면 중앙에서 조금 아래로 배치
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 16.dp) // 1. 먼저 좌우 여백을 적용
                .padding(top = 100.dp), // 2. 그 다음 위쪽 여백을 적용 -> 한번에 하면 오류 발생
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = question.text,
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag(QuizTestTags.QuestionText)
            )

            question.answers.forEachIndexed { index, answer ->
                Button(
                    onClick = { onAnswerSelected(answer) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag(QuizTestTags.answerButton(index))
                ) {
                    Text(answer.text, fontSize = 16.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun QuestionScreen(
    question: Question,
    onAnswerSelected: (Answer) -> Unit
) {
    QuizScreen(
        question = question,
        onAnswerSelected = onAnswerSelected
    )
}

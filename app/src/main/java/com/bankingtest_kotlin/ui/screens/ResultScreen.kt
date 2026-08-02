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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankingtest_kotlin.domain.QuizResult
import androidx.compose.ui.layout.ContentScale
import com.bankingtest_kotlin.ui.QuizTestTags

@Composable
fun ResultScreen(result: QuizResult, onRestartQuiz: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(QuizTestTags.ResultScreen)
    ) {
        // 결과 이미지를 배경으로 설정
        result.imageResId?.let { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = null, // 배경 이미지이므로 접근성 설명을 비워둠
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop // 화면에 꽉 차도록 크기 조정
            )
        }

        // 버튼을 화면 하단 중앙에 배치
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom // 버튼을 맨 아래로 정렬
        ) {
            Text(
                text = result.text,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag(QuizTestTags.ResultText)
            )

            Button(
                onClick = onRestartQuiz,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(QuizTestTags.RestartButton)
            ) {
                Text("다시 시작", fontSize = 20.sp)
            }
        }
    }
}

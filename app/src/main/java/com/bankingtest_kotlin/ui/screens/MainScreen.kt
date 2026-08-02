package com.bankingtest_kotlin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bankingtest_kotlin.R
import com.bankingtest_kotlin.navigation.Screen
import androidx.compose.ui.layout.ContentScale

@Composable
fun MainScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 배경 이미지 설정
        Image(
            painter = painterResource(id = R.drawable.bankingtest_main),
            contentDescription = "금융 테스트 메인 화면 배경",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // 화면에 꽉 차도록 이미지 크기 조정
        )

        // 버튼을 화면 하단 중앙에 배치
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom // 버튼을 맨 아래로 정렬
        ) {
            Button(
                onClick = { navController.navigate(Screen.Question.createRoute(0)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("테스트 시작!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}



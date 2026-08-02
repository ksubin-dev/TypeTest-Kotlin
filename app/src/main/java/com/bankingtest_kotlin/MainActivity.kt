package com.bankingtest_kotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bankingtest_kotlin.navigation.Screen
import com.bankingtest_kotlin.ui.screens.MainScreen
import com.bankingtest_kotlin.ui.screens.QuestionScreen
import com.bankingtest_kotlin.ui.screens.ResultScreen
import com.bankingtest_kotlin.ui.theme.BankingTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BankingTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QuizApp()
                }
            }
        }
    }
}

@Composable
fun QuizApp(quizViewModel: QuizViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            // 퀴즈 시작 시 뷰모델 초기화
            quizViewModel.resetQuiz()
            MainScreen(navController = navController)
        }

        composable(
            route = Screen.Question.route,
            arguments = listOf(navArgument("questionIndex") { type = NavType.IntType })
        ) { backStackEntry ->
            val questionIndex = backStackEntry.arguments?.getInt("questionIndex") ?: 0
            val question = quizViewModel.questions[questionIndex]

            QuestionScreen(
                question = question,
                onAnswerSelected = { resultId ->
                    quizViewModel.addAnswer(resultId)
                    if (questionIndex < quizViewModel.questions.size - 1) {
                        navController.navigate(Screen.Question.createRoute(questionIndex + 1))
                    } else {
                        navController.navigate(Screen.Result.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Result.route) {
            val result = quizViewModel.getFinalResult()
            ResultScreen(
                result = result,
                onRestartQuiz = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
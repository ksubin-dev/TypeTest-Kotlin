package com.bankingtest_kotlin

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bankingtest_kotlin.navigation.Screen
import com.bankingtest_kotlin.presentation.QuizDestination
import com.bankingtest_kotlin.presentation.QuizViewModel
import com.bankingtest_kotlin.ui.screens.MainScreen
import com.bankingtest_kotlin.ui.screens.QuizScreen
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
fun QuizApp() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as Application
    val quizViewModel: QuizViewModel = viewModel(
        factory = QuizViewModel.provideFactory(application)
    )
    val uiState by quizViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onStartQuiz = {
                    quizViewModel.startQuiz()
                    navController.navigate(Screen.Question.route)
                }
            )
        }

        composable(Screen.Question.route) {
            val question = uiState.currentQuestion

            if (question == null) {
                if (uiState.result == null) {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    }
                }
            } else {
                QuizScreen(
                    question = question,
                    onAnswerSelected = { answer ->
                        when (quizViewModel.selectAnswer(answer)) {
                            QuizDestination.Question -> Unit
                            QuizDestination.Result -> {
                                navController.navigate(Screen.Result.route) {
                                    popUpTo(Screen.Question.route) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }
        }

        composable(Screen.Result.route) {
            val result = uiState.result

            if (result == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            } else {
                ResultScreen(
                    result = result,
                    onRestartQuiz = {
                        navController.navigate(Screen.Question.route) {
                            popUpTo(Screen.Result.route) { inclusive = true }
                        }
                        quizViewModel.restartQuiz()
                    }
                )
            }
        }
    }
}

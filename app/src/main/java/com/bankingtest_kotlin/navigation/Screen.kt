package com.bankingtest_kotlin.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main_screen")
    object Question : Screen("question_screen/{questionIndex}") {
        fun createRoute(questionIndex: Int) = "question_screen/$questionIndex"
    }
    object Result : Screen("result_screen")
}
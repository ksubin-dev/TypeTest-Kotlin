package com.bankingtest_kotlin.data

import kotlinx.serialization.json.Json

class QuizJsonParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
    }
) {
    fun parse(rawJson: String): QuizDto = json.decodeFromString<QuizDto>(rawJson)
}

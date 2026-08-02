package com.bankingtest_kotlin

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf

// Define your quiz data
data class Question(
    val id: Int,
    val text: String,
    val imageResId: Int? = null,
    val answers: List<Answer>
)
data class Answer(val text: String, val resultId: Int)

data class Result(val id: Int, val text: String, val imageResId: Int? = null)

class QuizViewModel : ViewModel() {
    val questions = listOf(
        Question(
            id = 1,
            text = "당신은 평소에 어떤 금융 습관을 가지고 있나요?",
            imageResId = R.drawable.bankingtest_1,
            answers = listOf(
                Answer(text = "월급통장, 생활비통장, 적금통장, 비상금통장 등 이미 통장을 분리해서 사용하는 스타일", resultId = 1),
                Answer(text = "급여 통장에서 적금이나 필요한 곳에 넣어두는 것 외에는 분리는 못하지만 적당히 사용하는 스타일", resultId = 2),
                Answer(text = "딱히 관리하지 않고 그냥 살아가는 스타일", resultId = 3)
            )
        ),
        Question(
            id = 2,
            text = "이번에 회사 프로젝트가 성공적으로 끝나 성과급이 많이 나온다고 주변 동료에게 들었습니다. 당신은 이 성과급을 어떻게 사용하겠습니까?",
            imageResId = R.drawable.bankingtest_2,
            answers = listOf(
                Answer(text = "일단 자유입출금적금에 넣어둬야겠어!", resultId = 1),
                Answer(text = "장바구니에 넣어둔 물건을 사봐야겠어.", resultId = 2),
                Answer(text = "오늘 저녁은 무조건 맛있는 걸 먹어야겠어.", resultId = 3)
            )
        ),
        Question(
            id = 3,
            text = "집으로 돌아온 당신, 관리비 고지서가 온 걸 확인했습니다. 그런데 평소보다 많이 나온 금액!! 당신은 어떻게 할 것인가요?",
            imageResId = R.drawable.bankingtest_3,
            answers = listOf(
                Answer(text = "아니 이게 무슨 일이지, 당장 전화해서 알아봐야겠어", resultId = 1),
                Answer(text = "뭐지 보일러나 물을 더 썼나? 일단 저번 달에 어떻게 사용했었는지 곰곰이 생각해본다", resultId = 2),
                Answer(text = "내일 생각해야지 지금은 귀찮아", resultId = 3)
            )
        ),
        Question(
            id = 4,
            text = "드디어 주말, 텅텅 빈 냉장고를 보고 장을 보려고 합니다. 당신은 어떻게 장을 보나요?",
            imageResId = R.drawable.bankingtest_4,
            answers = listOf(
                Answer(text = "집 주변 마트 품목 별로 싼 곳을 파악하고 있는 상태, 느긋하게 동네를 돌면서 마트를 둘러본다.", resultId = 1),
                Answer(text = "적당히 괜찮은 마트를 들어가서 예산 내에 장을 보고 온다.", resultId = 2),
                Answer(text = "그냥 인터넷에서 시켜야지, 주말은 쉬는 게 최고야", resultId = 3)
            )
        ),
        Question(
            id = 5,
            text = "갑자기 상품권에 당첨된 당신, 마침 집 근처에 있는 대형마트 상품권입니다. 어떻게 사용하실 건가요?",
            imageResId = R.drawable.bankingtest_5,
            answers = listOf(
                Answer(text = "부모님 근처에도 있는 마트인데 선물로 드릴까", resultId = 1),
                Answer(text = "너무 좋다 당장 쓰러가야지", resultId = 3),
                Answer(text = "당근 마켓에 팔아서 현금으로 바꿔볼까", resultId = 2)
            )
        ),
        Question(
            id = 6,
            text = "적금만기가 된 오늘, 당신은 이 적금을 어떻게 사용하실 건가요?",
            imageResId = R.drawable.bankingtest_6,
            answers = listOf(
                Answer(text = "여러 가지 사이트, 은행들 이자 비교를 하면서 제일 높은 곳에 새로운 정기적금을 개설한다.", resultId = 1),
                Answer(text = "사고 싶었던 물건이 있는데 이번에 적금만기가 됐으니까 사야지", resultId = 3),
                Answer(text = "생각보다 이자가 얼마 없네, 재테크를 이번 기회에 시작해봐야겠어", resultId = 2)
            )
        )
    )

    val results = listOf(
        Result(1, "완벽한 재테크 고수", R.drawable.bankingtest_result1),
        Result(2, "아직 더 성장할 수 있는 묘목", R.drawable.bankingtest_result2),
        Result(3, "무궁무진한 성장성을 가진 새싹", R.drawable.bankingtest_result3)
    )

    // 사용자의 답변 빈도를 저장할 리스트
    private val answerCounts = mutableStateListOf(0, 0, 0, 0) // resultId는 1부터 시작하므로 인덱스 0은 사용하지 않음

    fun addAnswer(resultId: Int) {
        if (resultId >= 1 && resultId < answerCounts.size) {
            answerCounts[resultId] = answerCounts[resultId] + 1
        }
    }

    fun getFinalResult(): Result {
        // 가장 높은 빈도수를 가진 resultId 찾기
        val maxCount = answerCounts.maxOrNull() ?: 0
        val finalResultId = if (maxCount > 0) {
            answerCounts.indexOf(maxCount)
        } else {
            // 모든 답변이 0일 경우 기본값 (ex: resultId = 3)
            3
        }
        return results.first { it.id == finalResultId }
    }

    fun resetQuiz() {
        // 퀴즈를 다시 시작할 때 답변 카운트를 초기화
        for (i in 1 until answerCounts.size) {
            answerCounts[i] = 0
        }
    }
}
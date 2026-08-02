# AI 활용 테스트 자동화 정리

이 문서는 금융 테스트 Compose 리팩토링 과정에서 AI를 어떻게 활용했는지, 테스트 자동화와 커버리지 리포트를 어떤 기준으로 구성했는지 정리한다.

핵심은 “AI로 코드를 대신 작성했다”가 아니라, 사람이 반복해서 하기 귀찮은 품질 확인, 리포트 해석, 문서화 작업을 자동화해 리팩토링을 더 안전하게 진행한 것이다.

## 문제 정의

기존 Java/XML 원본 앱은 여러 테스트가 Activity, Fragment, XML 레이아웃 단위로 분산되어 있었다.

금융 테스트를 Compose로 옮길 때 다음 문제가 있었다.

- 질문 수가 늘어나면 화면 파일도 같이 늘어나기 쉬움
- 결과 계산 로직이 화면 흐름과 섞이기 쉬움
- 리팩토링 후 기존 기능이 깨졌는지 수동으로 확인해야 함
- 테스트 결과와 커버리지 수치를 매번 사람이 해석해야 함
- PR마다 어떤 품질이 좋아졌는지 설명하는 문서 작성이 반복됨

## 접근 방식

전체 원본 앱을 한 번에 옮기지 않고, 금융 테스트 1종을 기준으로 구조와 테스트 자동화를 먼저 만들었다.

진행 방향:

1. 질문/결과 데이터를 JSON으로 분리
2. 결과 계산 로직을 domain 계층으로 분리
3. ViewModel을 단일 UiState 중심으로 정리
4. 금융 테스트 주요 흐름을 Compose UI test로 고정
5. Kover 기반 focused coverage 리포트 구성
6. AI가 읽고 요약하기 쉬운 Markdown/JSON/HTML 리포트 생성
7. README와 문서에서 제3자가 이해할 수 있게 정리

## 리팩토링 전후 차이

| 구분 | 리팩토링 전 | 리팩토링 후 |
| --- | --- | --- |
| UI | Activity/Fragment/XML 중심 | 단일 Activity + Compose screen |
| 데이터 | 화면 코드와 리소스에 결합 | JSON asset |
| 결과 계산 | 화면 흐름 내부 조건문에 의존 | `ScoreBasedResultCalculator` |
| 상태 | 화면 이동과 상태 변경이 섞이기 쉬움 | `QuizViewModel` + `QuizUiState` |
| 테스트 | 수동 확인 중심 | unit test + Compose UI test |
| 품질 확인 | 별도 자동 리포트 없음 | Kover + coverage quality report |

현재 금융 테스트 기준:

| 항목 | 수치 |
| --- | ---: |
| 질문 | 6개 |
| 결과 | 3개 |
| unit test | 29개 통과 |
| Compose UI flow test | 1개 통과 |
| coverage summary script test | 10개 통과 |
| focused LINE | 98.18% |
| focused BRANCH | 87.50% |
| low coverage areas | 0개 |

## 테스트 전략

테스트는 수치를 채우기보다 리팩토링 중 실제로 깨질 수 있는 부분을 보호하는 데 집중했다.

### 순수 Kotlin 단위 테스트

- JSON 데이터가 올바르게 로드되는지 확인
- 질문/답변/결과 데이터 무결성 확인
- drawable resource key 매핑 확인

### 결과 계산 테스트

- 최고 점수 결과 선택
- 동점 처리
- 빈 답변
- 알 수 없는 result id
- 분기별 점수 계산

### ViewModel 테스트

- 테스트 시작 상태
- 답변 선택 후 다음 질문 이동
- 마지막 질문 이후 결과 이동
- 다시 시작 시 상태 초기화
- 잘못된 순서의 호출 방어

### Compose UI 테스트

금융 테스트의 주요 사용자 흐름을 최소 1개 자동화했다.

```text
메인 화면
-> 테스트 시작
-> 6개 질문 답변
-> 결과 화면 확인
-> 다시 시작
-> 첫 질문 복귀 확인
```

UI 테스트는 coverage 수치를 올리기 위한 목적보다, 실제 사용자 흐름 회귀를 막기 위한 안전망으로 본다.

## 커버리지 기준

전체 앱 coverage는 참고 지표로만 사용한다.

Compose UI, Theme, Preview, Activity glue, Navigation glue는 Android framework와 화면 렌더링 성격이 강하므로 PR 실패 기준으로 쓰지 않는다.

대표 품질 신호는 focused debug coverage다.

focused coverage 측정 대상:

- `domain`
- `data`
- `presentation.*ViewModel`

제외 대상:

- `MainActivity`
- Navigation glue
- Compose UI screen
- Theme/Preview
- generated code
- 단순 data class
- Android resource adapter

## AI 활용 지점

AI는 다음 작업에 활용했다.

- 기존 코드 구조 분석
- 이슈 단위 작업 분해
- 작업 순서와 브랜치/PR 규칙 문서화
- 테스트 후보 도출
- 테스트 코드 초안 작성과 보완
- 커버리지 수치 해석
- 낮은 coverage 영역과 다음 테스트 후보 요약
- PR 본문과 이슈 코멘트 작성 보조
- README와 문서 정리 초안 작성

특히 반복적으로 붙여넣어야 하는 커버리지 수치, 테스트 결과, 다음 보완 후보를 스크립트가 자동으로 생성하도록 했다.

## 자동화 산출물

`scripts/coverage_summary.py`는 Kover XML과 테스트 결과 XML을 읽어 다음 파일을 생성한다.

| 산출물 | 용도 |
| --- | --- |
| `coverage-summary.md` | GitHub Actions summary와 사람이 읽는 기본 요약 |
| `coverage-summary.json` | AI 분석이나 후속 자동화에 쓰기 좋은 구조화 데이터 |
| `coverage-report.html` | 제3자가 바로 이해하기 쉬운 시각화 리포트 |
| `coverage-pr-summary.md` | PR/이슈에 붙여넣기 쉬운 요약 |

HTML 리포트에는 다음 정보가 표시된다.

- Quality Signals
- Current Coverage
- Core Layer Signals
- Test Automation Signals
- Low Coverage Areas
- Next Test Candidates
- PR/Issue Summary
- Analysis Notes

## CI와 라벨 자동화

GitHub Actions는 다음 역할을 한다.

- unit test 실행
- debug build 검증
- focused Kover 리포트 생성
- coverage summary artifact 업로드
- PR 라벨 자동 부착
- 같은 PR의 이전 workflow run 취소
- docs/images만 바뀐 경우 무거운 Android CI 생략

PR 라벨 자동화는 변경 파일 경로와 연결된 이슈 라벨을 기준으로 동작한다.

이렇게 해두면 사람이 매번 라벨을 고르지 않아도 PR의 성격이 어느 정도 정리된다.

## 리포트 예시

로컬 또는 CI artifact에서 다음 HTML 파일을 확인할 수 있다.

```text
build/reports/coverage-summary/coverage-report.html
```

현재 리포트 기준:

- quality level: strong
- focused LINE: 98.18%
- focused BRANCH: 87.50%
- unit tests: 29개 통과
- Compose UI flow test: 1개 통과
- low coverage areas: 0개

<img width="760" alt="Coverage Quality Report" src="https://github.com/user-attachments/assets/0dac8576-d7ad-4394-83d4-80359db29250" />

## 다음 개선 후보

현재 focused line coverage는 충분히 높다.

다음 단계에서는 다음 작업이 효과적이다.

- branch coverage 90% 근처까지 보강
- focused coverage gate 단계적 적용
- README와 문서에서 테스트 자동화 흐름 정리
- 금융 테스트 외 다른 테스트도 같은 JSON/Quiz 구조로 옮기기
- CI 실행 시간이 실제로 느려질 때 #12에서 최적화 검토

## 정리

이번 작업의 핵심은 Compose 전환 자체보다, 리팩토링을 안전하게 반복할 수 있는 기반을 먼저 만든 것이다.

금융 테스트 하나를 기준으로 데이터, 계산, 상태, UI 흐름, 테스트, 리포트, CI를 연결해 두었기 때문에 이후 다른 유형 테스트를 옮길 때도 같은 구조를 재사용할 수 있다.

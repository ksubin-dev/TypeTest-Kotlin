# AGENTS.md

이 문서는 이 저장소에서 AI 에이전트와 함께 작업할 때 따르는 프로젝트 운영 규칙이다.

규칙이 충돌하면 사용자의 최신 지시를 우선한다.

## 프로젝트 목표

이 프로젝트는 기존 Java/XML 기반 금융 테스트 흐름을 Kotlin + Jetpack Compose 기반 구조로 재설계한다.

최종 목표는 원본 앱 전체를 한 번에 옮기는 것이 아니라, 금융 테스트를 대표 기능으로 삼아 다음을 보여주는 것이다.

- 반복적인 Activity/Fragment/XML 구조를 재사용 가능한 Compose 화면으로 줄인다.
- 질문, 답변, 결과 데이터를 JSON 기반으로 분리한다.
- 결과 계산 로직을 UI와 ViewModel에서 분리한다.
- ViewModel 상태 전이와 결과 계산을 자동 테스트로 검증한다.
- Kover 기반 HTML/XML 커버리지 리포트를 생성한다.
- AI를 분석, 설계, 구현, 테스트 자동화, PR 정리에 활용한 과정을 남긴다.

## 작업 범위

우선 금융 테스트만 완성도 높게 리팩토링한다.

다른 유형 테스트, 광고, Play Store 링크, 결과 공유 기능은 별도 이슈가 있을 때만 작업한다.

## 이슈 진행 순서

최신 작업 순서와 백로그는 GitHub Issue #1을 기준으로 한다.

`AGENTS.md`에는 자주 바뀌는 이슈 목록보다 오래 유지할 운영 규칙을 우선 기록한다.

새 이슈가 생기거나 진행 순서가 바뀌면 기본적으로 #1의 체크리스트와 권장 진행 순서를 업데이트한다.

브랜치/PR/커밋/테스트 원칙처럼 작업 방식 자체가 바뀌는 경우에만 `AGENTS.md`를 수정한다.

## 빌드 설정 규칙

Gradle 플러그인, 라이브러리 버전, Compose 관련 버전은 가능한 한 `gradle/libs.versions.toml`에서 관리한다.

`build.gradle.kts`에는 직접 버전 문자열을 하드코딩하지 않고 version catalog alias를 우선 사용한다.

새 의존성을 추가할 때는 먼저 `libs.versions.toml`에 version, library, plugin alias를 정의한 뒤 모듈 Gradle 파일에서 참조한다.

Android Gradle Plugin, Gradle Wrapper, Kotlin, Compose처럼 빌드 전체에 영향을 주는 버전을 바꿀 때는 관련 이슈나 PR 본문에 변경 이유와 검증 결과를 남긴다.

사용하지 않는 의존성은 추가하지 않고, 기존 의존성을 제거할 때는 빌드와 테스트 영향 범위를 확인한다.

## 아키텍처 규칙

단일 `:app` 모듈을 유지하되, 책임은 최소한으로 분리한다.

권장 패키지 책임:

- `data`: JSON 로딩, Repository, drawable 리소스 매핑
- `domain`: Quiz 모델, Question, Answer, Result, 결과 계산 로직
- `presentation`: ViewModel, UiState, 사용자 이벤트 처리
- `ui`: Compose 화면, Navigation host

비즈니스 로직은 Composable 안에 두지 않는다.

Composable은 가능한 한 `state`와 callback/event만 받는다.

결과 계산은 `ResultCalculator` 같은 별도 클래스로 분리한다.

질문 수나 결과 수가 늘어나도 화면 파일을 추가하지 않는 구조를 우선한다.

SOLID 원칙은 프로젝트 규모에 맞게 실용적으로 적용한다.

- 단일 책임 원칙: Composable은 UI 표시, ViewModel은 상태 전이, domain은 계산 규칙, data는 데이터 로딩을 담당한다.
- 개방-폐쇄 원칙: 새 테스트를 추가할 때 기존 공통 화면을 수정하기보다 JSON 데이터와 필요한 계산기만 추가하는 구조를 우선한다.
- 의존성 역전 원칙: ViewModel은 가능하면 구체 JSON 로더나 Android 리소스 접근보다 Repository/Calculator 추상에 의존한다.
- 인터페이스는 필요할 때만 만든다. 테스트나 확장에 이득이 없는 과한 추상화는 피한다.

SSOT 원칙을 지킨다.

- 질문, 답변, 결과 정의의 기준은 JSON 데이터로 둔다.
- 결과 계산 기준은 `ResultCalculator`에만 둔다.
- 현재 질문, 선택 답변, 결과 표시 상태의 기준은 `QuizUiState`로 둔다.
- 같은 상태를 Navigation argument, ViewModel field, Composable local state에 중복 저장하지 않는다.
- UI 텍스트나 이미지 이름을 여러 곳에 복사하지 않는다. 필요한 경우 데이터 모델 또는 resource 매핑 한 곳에서 관리한다.

## 데이터 규칙

질문, 답변, 결과는 JSON으로 관리한다.

JSON에는 Android resource id를 직접 넣지 않고 `bankingtest_1` 같은 리소스 이름을 저장한다.

앱 내부에서 리소스 이름을 drawable id로 매핑한다.

JSON 데이터는 최소한 다음을 테스트로 검증한다.

- JSON 파싱 가능 여부
- 질문 수
- 질문별 답변 수
- 답변 score가 존재하는 결과 id와 연결되는지
- 이미지 리소스 이름이 실제 drawable과 매핑되는지

## 테스트 및 커버리지 규칙

테스트 개수보다 의미 있는 회귀 방지를 우선한다.

필수 테스트:

- 결과 계산 단위 테스트
- ViewModel 상태 전이 테스트
- JSON 데이터 무결성 테스트
- 금융 테스트 주요 사용자 흐름 Compose UI 테스트

권장 커버리지 목표:

- domain/result calculator line coverage 80% 이상
- domain/result calculator branch coverage 80% 이상
- ViewModel line coverage 80% 이상
- Compose UI는 주요 사용자 흐름 검증 중심

커버리지 제외 후보:

- `R`
- `BuildConfig`
- generated code
- Compose Preview
- theme-only code
- 단순 Activity glue code

테스트명은 가능한 한 한글로 작성한다.

Kotlin 테스트 함수는 백틱 함수명을 사용해 테스트 의도를 문장처럼 표현한다.

예시:

```kotlin
@Test
fun `전문가 점수가 가장 높으면 전문가 결과를 반환한다`() {
    // ...
}

@Test
fun `마지막 질문에 답변하면 결과 화면 상태로 전환된다`() {
    // ...
}
```

테스트명 작성 규칙:

- 무엇을 준비했는지보다 어떤 동작을 검증하는지 드러낸다.
- 성공 조건과 예외/경계 조건을 이름에서 구분한다.
- 단순히 `test1`, `calculateResultTest`처럼 의도를 알 수 없는 이름은 사용하지 않는다.
- 필요하면 Given/When/Then 주석을 짧게 사용하되, 테스트명 자체가 먼저 읽히게 한다.

## 커버리지 기록 규칙

커버리지, 테스트 자동화, 리포트 자동화 작업을 완료할 때는 PR 본문과 이슈 댓글에 커버리지 기록을 남긴다.

대표 품질 수치는 전체 앱 coverage가 아니라 focused coverage를 사용한다.

기록 대상:

- 확인일
- 기준 리포트 이름: 예: focused debug coverage
- focused LINE
- focused BRANCH
- focused INSTRUCTION
- full reference 수치가 생성된 경우 해당 수치
- 낮은 coverage 영역
- 다음 테스트 후보
- 생성된 artifact 이름
- HTML 품질 리포트가 있으면 artifact 이름 또는 경로

기록 방식:

- PR 본문에는 해당 PR의 검증 결과와 커버리지 결과를 작성한다.
- 이슈 본문은 계획과 완료 조건 중심으로 유지한다.
- 실제 완료 시점의 수치는 이슈 댓글에 기록한다.
- 수치에는 확인 날짜와 기준 리포트 이름을 함께 적는다.
- generated code, Theme, Compose UI glue 등을 포함한 전체 coverage를 대표 품질 수치처럼 쓰지 않는다.
- full reference coverage는 참고 지표로만 기록한다.

권장 기록 형식:

```md
## 커버리지 기록

확인일: YYYY-MM-DD
기준: focused debug coverage

- LINE:
- BRANCH:
- INSTRUCTION:
- full reference:
- 낮은 coverage 영역:
- 다음 테스트 후보:
- artifact:
```

## 검증 명령

Windows 환경에서는 Android Studio JBR을 우선 사용한다.

현재 Android Gradle 프로젝트가 `BankingTestKotlin/` 아래에 있는 동안에는 해당 폴더에서 Gradle 명령을 실행한다.

#9 완료 이후에는 저장소 루트에서 Gradle 명령을 실행한다.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat test
.\gradlew.bat :app:assembleDebug
```

Kover 설정 이후에는 HTML/XML 리포트 생성 명령도 함께 실행한다.

## Git 브랜치 규칙

작업은 이슈 단위로 브랜치를 만든다.

`main`은 최종 안정 브랜치로 사용한다.

`develop`은 이슈 작업을 모으는 통합 브랜치로 사용한다.

모든 이슈 작업 브랜치는 `develop` 브랜치에서 분기한다.

작업 시작 전에는 `develop` 브랜치 기준 최신 상태를 확인한다.

브랜치 이름은 다음 prefix 중 하나로 시작한다.

- `feature/`: 새 기능 또는 새 구조 추가
- `fix/`: 버그 수정
- `refactor/`: 동작 유지 구조 개선
- `test/`: 테스트 또는 커버리지 작업
- `docs/`: README, 문서, 발표 자료 정리
- `chore/`: 빌드, 환경, 의존성, 설정 정리
- `ui/`: 화면 배치, Compose UI, 접근성 개선

브랜치 이름은 반드시 다음 형식을 사용한다.

```text
<prefix>/issue-<issue-number>-<short-work-summary>
```

규칙:

- `issue-<issue-number>`를 prefix 바로 뒤에 넣는다.
- `<short-work-summary>`는 작업 내용을 알 수 있는 짧은 영어 kebab-case로 작성한다.
- 공백, 한글, 특수문자는 브랜치 이름에 사용하지 않는다.
- 하나의 브랜치는 하나의 이슈 범위를 기준으로 만든다.
- 이슈 없이 작업해야 하는 경우에는 먼저 이슈를 만들거나, 사용자가 명시적으로 예외를 승인해야 한다.

예시:

```text
feature/issue-5-json-quiz-data
refactor/issue-6-result-calculator
test/issue-8-compose-regression
chore/issue-2-build-baseline
chore/issue-16-build-tool-version-update
docs/issue-1-branch-naming-rule
```

## 커밋 규칙

커밋 제목과 본문은 한글로 작성한다.

커밋 제목은 짧고 구체적으로 쓴다.

예시:

```text
chore: 빌드 및 테스트 기준선 정리 (#2)
refactor: 금융 테스트 결과 계산 로직 분리 (#6)
test: 금융 테스트 주요 흐름 UI 테스트 추가 (#8)
docs: AI 활용 리팩토링 과정 문서화
```

커밋 본문에는 필요한 경우 다음을 적는다.

- 왜 변경했는지
- 어떤 구조로 바꿨는지
- 테스트 결과
- 남은 후속 작업

## PR 대상 브랜치

일반 작업 PR은 `develop` 브랜치를 대상으로 연다.

`main`은 사용자가 어느 정도 작업을 모아 최종 병합할 때 사용한다.

`main`으로 직접 PR을 열거나 병합하는 작업은 사용자가 명시적으로 요청한 경우에만 진행한다.

`develop`으로 병합하는 PR에서는 이슈를 자동으로 닫지 않는다.

PR 본문에 `Closes #번호`, `Fixes #번호`, `Resolves #번호` 같은 자동 종료 키워드를 쓰지 않는다.

대신 다음처럼 연결만 표시한다.

```text
관련 이슈: #6
```

이슈 종료는 `main` 병합 또는 사용자의 명시적 요청 이후에 처리한다.

## main PR 작성 규칙

`develop`에 모은 작업을 `main`으로 반영하는 PR은 통합 PR로 작성한다.

`main` PR은 사용자가 명시적으로 요청한 경우에만 만든다.

`main` PR 본문에는 이번 통합에 포함된 작업을 요약한다.

`main` PR에서는 실제 완료된 하위 이슈에 대해 자동 종료 키워드를 사용할 수 있다.

예시:

```text
Closes #2
Closes #3
Closes #9
```

상위 epic 이슈는 모든 완료 조건이 끝나기 전까지 자동 종료하지 않는다.

상위 epic은 다음처럼 연결만 표시한다.

```text
관련 이슈: #1
```

`main` PR 본문에는 다음을 포함한다.

- 포함된 작업 요약
- 검증한 빌드/테스트 명령
- 커버리지 리포트 여부
- 화면 변화 여부
- 제외한 작업
- 후속 작업
- 닫을 이슈 목록

## PR 작성 규칙

PR은 기본적으로 draft로 만든다. 사용자가 ready PR을 요청한 경우에만 ready 상태로 만든다.

PR 본문에는 다음 항목을 포함한다.

```md
## 작업 목적

## 변경 내용

## 테스트 결과

## 커버리지 결과

## 화면 변화

## 영향 범위

## 제외한 작업

## 후속 작업

## 관련 이슈
```

작성 가이드:

- `작업 목적`: 이 PR이 왜 필요한지 2~3줄로 쓴다.
- `변경 내용`: 실제 변경한 구조와 파일을 요약한다.
- `테스트 결과`: 실행한 Gradle 명령과 결과를 적는다.
- `커버리지 결과`: Kover HTML/XML 리포트 경로와 핵심 수치를 적는다.
- `화면 변화`: UI 변경이 있으면 스크린샷이나 설명을 넣는다.
- `영향 범위`: 금융 테스트, ViewModel, data layer 등 영향을 받는 범위를 적는다.
- `제외한 작업`: 이번 PR에서 일부러 하지 않은 일을 명확히 적는다.
- `후속 작업`: 다음 이슈나 남은 개선을 적는다.
- `관련 이슈`: 자동 종료 키워드 없이 `관련 이슈: #번호`로 연결한다. 이 항목은 PR 라벨 자동화가 이슈 라벨을 복사하는 기준이므로 누락하지 않는다.
- PR 라벨은 GitHub Actions가 이슈 라벨과 변경 파일 경로를 기준으로 자동 부착한다. 자동화가 실패했거나 명백히 누락된 경우에만 수동으로 보정한다.

## PR 체크리스트

PR 본문 끝에는 다음 체크리스트를 포함한다.

```md
## 체크리스트

- [ ] 관련 이슈를 확인했다.
- [ ] develop 브랜치 대상으로 PR을 열었다.
- [ ] 이슈 자동 종료 키워드를 사용하지 않았다.
- [ ] 빌드 또는 테스트 명령을 실행했다.
- [ ] 테스트 결과를 PR 본문에 기록했다.
- [ ] 커버리지 변화가 있으면 리포트 경로 또는 수치를 기록했다.
- [ ] 관련 없는 파일 변경을 포함하지 않았다.
```

## 작업 전후 규칙

작업 시작 전:

- 대상 이슈를 읽는다.
- `git status`를 확인한다.
- 사용자가 만든 unrelated change를 되돌리지 않는다.

작업 완료 전:

- 관련 테스트를 실행한다.
- 변경 파일을 확인한다.
- 커밋 메시지를 한글로 작성한다.
- PR은 `develop` 대상으로 만들고 draft로 연다.
- 실제 작업 결과를 기준으로 관련 이슈 본문 또는 댓글을 업데이트한다.
- 작업 중 범위, 완료 조건, 후속 작업이 바뀌었다면 이슈에도 반영한다.

## 이슈 업데이트 규칙

작업이 끝나면 관련 이슈를 단순 링크가 아니라 작업 기록으로 업데이트한다.

이슈를 자동으로 닫지는 않는다. `develop` 대상 PR에서는 자동 종료 키워드를 사용하지 않는다.

작업 완료 후 이슈에 반영할 내용:

- 실제로 완료한 작업
- 실행한 테스트 명령과 결과
- 생성된 커버리지 리포트 경로 또는 핵심 수치
- 계획과 달라진 구현 방향
- 발견한 문제 또는 남은 위험
- 다음 이슈로 넘길 후속 작업

이슈 본문이 작업 전 계획과 달라졌다면 본문을 수정한다.

완료 기록만 남기면 충분한 경우에는 댓글로 남긴다.

후속 작업이 생기면 기존 이슈의 `후속 작업` 항목에 추가하거나 새 이슈 후보로 정리한다.

커버리지 수치, 테스트 개수, 리포트 경로처럼 시간이 지나면 바뀔 수 있는 정보는 마지막 확인 날짜와 함께 적는다.

## 새 이슈 발견 규칙

작업 중 현재 이슈 범위를 벗어나는 문제가 발견되면, 기존 작업에 억지로 포함하지 않는다.

새 이슈가 필요한 경우:

- 현재 PR 범위를 키우면 리뷰하기 어려워지는 경우
- 독립적으로 테스트하거나 검증할 수 있는 후속 작업인 경우
- 버그, 구조 개선, 문서 보강, 테스트 보강이 현재 이슈의 완료 조건과 다른 경우
- 사람의 결정이 필요해 현재 작업을 막거나 후속 판단이 필요한 경우

기본 원칙:

- 먼저 현재 이슈의 `후속 작업` 항목 또는 PR의 `후속 작업`에 후보로 기록한다.
- 사용자가 새 이슈 생성을 요청하면 GitHub 이슈를 생성한다.
- 사용자가 “필요한 이슈는 알아서 만들어도 된다”고 명시한 경우에만 작업 중 새 이슈를 직접 생성한다.
- 새 이슈를 만들 때는 기존 이슈와 연결하고, 왜 별도 이슈로 분리했는지 본문에 적는다.
- `develop` PR과 마찬가지로 새 이슈도 자동 종료 키워드를 사용하지 않는다.
- 새 이슈를 만들거나 작업 순서가 바뀌면 #1 상위 이슈를 업데이트한다.
- 새 이슈가 생겼다는 이유만으로 `AGENTS.md`를 수정하지 않는다.
- 운영 규칙 자체가 바뀐 경우에만 `AGENTS.md` 수정 PR을 만든다.

## 라벨 최소 세트

혼자 관리하는 프로젝트이므로 라벨은 적게 유지한다.

PR 라벨은 기본적으로 GitHub Actions 자동화를 따른다. 새 PR을 만들 때 에이전트가 라벨을 수동으로 붙이는 일을 기본 절차로 삼지 않는다.

권장 라벨:

- `epic`: 상위 목표
- `setup`: 빌드, 환경, CI
- `refactor`: 구조 개선
- `test`: 테스트, 커버리지
- `docs`: 문서, 발표 정리
- `priority`: 우선 처리

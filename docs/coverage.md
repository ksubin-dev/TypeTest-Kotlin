# 커버리지 운영 기준

이 프로젝트는 Kover 기반 HTML/XML 커버리지 리포트를 사용한다.

커버리지 목표는 전체 앱 수치를 억지로 높이는 것이 아니라, 금융 테스트의 핵심 계산 로직과 상태 관리 코드가 자동 테스트로 보호되는지 보여주는 것이다.

## 리포트 종류

### focused coverage

PR과 최종 품질 기준의 대표 수치로 사용할 리포트다.

현재는 `debug` variant 리포트에 다음 UI 연결 코드를 제외해 focused coverage 기준으로 사용한다.

- `MainActivity`
- `MainActivityKt`
- Compose compiler가 생성한 `ComposableSingletons*`
- `navigation`
- `ui`
- theme, Preview, 화면 렌더링 중심 Composable

향후 #5, #6, #7에서 패키지를 분리하면 focused coverage의 대표 대상은 다음 패키지로 이동한다.

- `com.bankingtest_kotlin.domain.*`
- `com.bankingtest_kotlin.data.*`
- `com.bankingtest_kotlin.presentation.*ViewModel*`

최종 목표:

- domain/result calculator line coverage 80% 이상
- domain/result calculator branch coverage 80% 이상
- ViewModel line coverage 80% 이상
- data parser/repository line coverage 70~80% 이상

### 전체 참고 coverage

전체 앱 리포트는 참고 지표로만 사용한다.

Compose UI, Theme, Preview, Activity glue, Navigation glue는 Android framework 및 화면 렌더링 성격이 강하므로 전체 앱 coverage를 PR 실패 기준으로 사용하지 않는다.

## 로컬 실행 명령

Windows 환경에서는 Android Studio JBR을 사용한다.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
```

focused coverage 리포트:

```powershell
.\gradlew.bat :app:koverHtmlReportDebug :app:koverXmlReportDebug
```

전체 참고 coverage 리포트:

```powershell
.\gradlew.bat :app:koverHtmlReport :app:koverXmlReport
```

일반 테스트:

```powershell
.\gradlew.bat test
```

## 리포트 위치

Kover 태스크 실행 후 `app/build/reports/kover/` 아래에 HTML/XML 리포트가 생성된다.

대표 확인 파일:

- focused HTML: `app/build/reports/kover/htmlDebug/index.html`
- focused XML: `app/build/reports/kover/reportDebug.xml`
- 전체 참고 HTML: `app/build/reports/kover/html/index.html`
- 전체 참고 XML: `app/build/reports/kover/report.xml`

## 현재 기준선

확인일: 2026-08-02

현재 테스트는 예제 테스트만 존재하므로 focused coverage와 전체 참고 coverage 모두 품질 목표를 대표하지 않는다.

| 리포트 | LINE | BRANCH | INSTRUCTION | 해석 |
| --- | ---: | ---: | ---: | --- |
| focused debug | 0.00% (0/70) | 0.00% (0/12) | 0.00% (0/448) | 현재는 `QuizViewModel.kt`만 측정 대상이며, 아직 의미 있는 단위 테스트가 없다. |
| 전체 참고 | 0.00% (0/230) | 0.00% (0/72) | 0.00% (0/2112) | Compose UI와 Android 연결 코드까지 포함한 참고 수치다. |

80% 기준 강제는 #6 결과 계산 로직 분리, #7 ViewModel 상태 구조 개선, #8 Compose UI 흐름 테스트 이후 별도 이슈에서 적용한다.

## CI 운영 방향

PR에서는 빠른 focused coverage 리포트를 우선 생성한다.

develop/main push 또는 수동 실행에서는 전체 참고 리포트와 필요한 UI 흐름 테스트를 별도로 실행한다.

전체 앱 coverage는 참고 지표이며, 실패 기준은 핵심 production code focused coverage에만 적용한다.

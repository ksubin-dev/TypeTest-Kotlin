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
- 단순 domain data class
- `@Serializable` DTO 및 serialization generated code
- Android resource/asset 연결 adapter
- theme, Preview, 화면 렌더링 중심 Composable

#5에서 `data`, `domain` 패키지를 만들었고, #6, #7에서 계산 로직과 ViewModel 상태 구조를 추가로 분리하면 focused coverage의 대표 대상은 다음 패키지로 이동한다.

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

coverage summary Markdown/JSON/HTML 생성:

```powershell
python3 scripts/coverage_summary.py
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
- coverage summary Markdown: `build/reports/coverage-summary/coverage-summary.md`
- coverage summary JSON: `build/reports/coverage-summary/coverage-summary.json`
- coverage quality HTML: `build/reports/coverage-summary/coverage-report.html`

## #4 완료 기준선

확인일: 2026-08-02

#4 완료 시점에는 예제 테스트만 존재하므로 focused coverage와 전체 참고 coverage 모두 품질 목표를 대표하지 않는다.

| 리포트 | LINE | BRANCH | INSTRUCTION | 해석 |
| --- | ---: | ---: | ---: | --- |
| focused debug | 0.00% (0/70) | 0.00% (0/12) | 0.00% (0/448) | #4 완료 시점 기준선이다. `QuizViewModel.kt`만 측정 대상이며, 아직 의미 있는 단위 테스트가 없다. |
| 전체 참고 | 0.00% (0/230) | 0.00% (0/72) | 0.00% (0/2112) | Compose UI와 Android 연결 코드까지 포함한 참고 수치다. |

## #5 이후 기준선

확인일: 2026-08-02

금융 테스트 데이터를 JSON과 Repository로 분리하고, JSON 데이터 무결성 테스트를 추가한 뒤의 기준선이다.

| 리포트 | LINE | BRANCH | INSTRUCTION | 해석 |
| --- | ---: | ---: | ---: | --- |
| focused debug | 52.94% (27/51) | 10.71% (3/28) | 47.25% (189/400) | `QuizJsonParser`, `QuizMapper`, `QuizViewModel` 중심 수치다. ViewModel과 결과 계산 로직 테스트가 아직 없어 80% 기준은 적용하지 않는다. |
| 전체 참고 | 26.52% (70/264) | 8.06% (10/124) | 22.15% (595/2686) | Compose UI와 Android 연결 코드까지 포함한 참고 수치다. |

80% 기준 강제는 #6 결과 계산 로직 분리, #7 ViewModel 상태 구조 개선, #8 Compose UI 흐름 테스트 이후 별도 이슈에서 적용한다.

## CI 운영 방향

develop 대상 PR에서는 빠른 focused coverage 리포트를 우선 생성한다.

GitHub Actions에서는 `kover-focused-debug` artifact로 focused HTML/XML 리포트를 업로드한다.

GitHub Actions에서는 `coverage-summary` artifact로 Markdown/JSON 요약 리포트와 HTML 품질 리포트를 업로드한다.

main push 또는 수동 실행에서는 `kover-full-reference` artifact로 전체 참고 리포트도 업로드한다.

workflow summary에는 focused coverage와 전체 참고 coverage의 기본 수치, 기준선 대비 변화량, 낮은 coverage 영역, 다음 테스트 후보를 표시한다.

HTML 품질 리포트는 같은 정보를 사람이 반복적으로 확인하기 쉽게 재구성한 산출물이다. line/branch/instruction 중 부족한 지표와 낮은 coverage class를 빠르게 파악해 다음 테스트 보완 우선순위를 정하는 데 사용한다.

CI 실행 시간을 줄이기 위해 PR에서는 전체 참고 리포트를 생략하고, Markdown/docs/images만 바뀐 PR 또는 Markdown/docs/images만 바뀐 main push는 Android CI를 실행하지 않는다. 같은 PR에 새 커밋이 올라오면 이전 실행은 concurrency 설정으로 취소한다.

develop 브랜치에 PR이 병합될 때는 별도 push CI를 실행하지 않는다. PR 단계에서 이미 검증한 내용을 develop merge commit에서 반복하지 않고, main에 반영될 때 최종 기준선 리포트를 한 번 더 생성한다.

Gradle 의존성은 `gradle/actions/setup-gradle`의 basic cache를 사용한다. PR에서는 cache read-only로 동작시켜 불필요한 cache write를 줄인다.

전체 앱 coverage는 참고 지표이며, 실패 기준은 핵심 production code focused coverage에만 적용한다.

coverage 변화량 비교와 다음 테스트 후보 자동 요약은 `scripts/coverage_summary.py`가 생성하는 Markdown/JSON/HTML 리포트를 기준으로 확인한다.

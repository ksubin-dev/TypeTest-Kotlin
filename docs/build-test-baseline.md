# 빌드 및 테스트 기준선

확인일: 2026-08-02

이 문서는 금융 테스트 리팩토링을 시작하기 전에 현재 프로젝트가 어떤 환경과 명령으로 빌드되고 테스트되는지 기록한다.

## 기준 환경

- OS: Windows 11
- 권장 JDK: Android Studio JBR 21
- 확인한 Java 버전: `openjdk version "21.0.10"`
- Gradle Wrapper: `8.13`
- Android Gradle Plugin: `8.12.3`
- Kotlin: `2.0.21`
- Compose BOM: `2024.09.00`
- compileSdk: `36`
- targetSdk: `36`
- minSdk: `24`

JDK 24에서는 Gradle unit test task 생성 실패 사례가 있었으므로 현재 기준선에서는 사용하지 않는다.

## Android SDK 설정

Windows 로컬 환경에서는 Android SDK 위치를 다음처럼 맞춘다.

```powershell
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
```

`compileSdk = 36`을 사용하므로 Android SDK Platform 36이 설치되어 있어야 한다.

## 권장 실행 명령

저장소 루트에서 Android Studio JBR을 사용해 실행한다.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME

.\gradlew.bat --version
.\gradlew.bat test
.\gradlew.bat :app:assembleDebug
```

## 확인 결과

다음 명령이 저장소 루트에서 성공했다.

| 명령 | 결과 | 비고 |
| --- | --- | --- |
| `.\gradlew.bat --version` | 성공 | Gradle 8.13, JVM 21.0.10 확인 |
| `.\gradlew.bat test` | 성공 | debug/release unit test 리포트 생성 |
| `.\gradlew.bat :app:assembleDebug` | 성공 | debug APK 생성 |

## 현재 테스트 기준선

현재 소스에 존재하는 로컬 unit test는 예제 테스트 1개다.

- 파일: `app/src/test/java/com/example/bankingtest_kotlin/ExampleUnitTest.kt`
- 테스트: `addition_isCorrect`

`.\gradlew.bat test` 실행 시 build variant별 리포트가 생성된다.

- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/tests/testReleaseUnitTest/index.html`

확인한 XML 리포트 기준:

| Task | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| `testDebugUnitTest` | 1 | 0 | 0 | 0 |
| `testReleaseUnitTest` | 1 | 0 | 0 | 0 |

## 현재 한계

- 현재 unit test는 기본 예제 테스트라 금융 테스트 기능 회귀를 막지 못한다.
- `app/src/androidTest/java/com/example/bankingtest_kotlin/ExampleInstrumentedTest.kt`는 존재하지만, 이번 기준선에서는 에뮬레이터가 필요한 `connectedAndroidTest`를 실행하지 않았다.
- instrumented test는 현재 `com.example.bankingtest_kotlin` 패키지명을 기대한다. 실제 `applicationId`는 `com.bankingtest_kotlin`이므로 #3에서 패키지명, namespace, applicationId 정리와 함께 확인이 필요하다.
- Kover 또는 JaCoCo 커버리지 리포트는 아직 설정되어 있지 않다.

## 후속 작업

- #3 패키지명, namespace, applicationId 정리
- #4 Kover 기반 커버리지 HTML/XML 리포트 구성
- #6 결과 계산 로직 분리 이후 의미 있는 unit test 추가
- #8 금융 테스트 주요 흐름 Compose UI 테스트 추가

# 빌드 및 테스트 기준선

확인일: 2026-08-02

이 문서는 금융 테스트 리팩토링을 시작하기 전에 현재 프로젝트가 어떤 환경과 명령으로 빌드되고 테스트되는지 기록한다.

## 기준 환경

- OS: Windows 11
- 권장 JDK: Android Studio JBR 21
- 확인한 Java 버전: `openjdk version "21.0.10"`
- Gradle Wrapper: `8.14.5`
- Android Gradle Plugin: `8.13.2`
- Kotlin: `2.3.20`
- Compose BOM: `2026.06.00`
- compileSdk: `36`
- targetSdk: `36`
- minSdk: `24`
- Java source/target compatibility: `21`
- Kotlin JVM target: `21`

Android Studio JBR 21은 현재 로컬 검증 기준이다. JDK 24는 이 프로젝트의 기준 런타임으로 사용하지 않는다.

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
.\gradlew.bat clean test :app:assembleDebug --console=plain
```

같은 작업트리에서 여러 Gradle 명령을 동시에 실행하면 `app/build` 아래 중간 산출물 접근이 충돌할 수 있다. 로컬 검증과 CI에서는 위처럼 하나의 Gradle 실행에서 필요한 task를 순차로 묶는다.

## 확인 결과

다음 명령이 저장소 루트에서 성공했다.

| 명령 | 결과 | 비고 |
| --- | --- | --- |
| `.\gradlew.bat --version` | 성공 | Gradle 8.14.5, JVM 21.0.10 확인 |
| `.\gradlew.bat clean test :app:assembleDebug --console=plain` | 성공 | debug/release unit test 리포트와 debug APK 생성 |
| `.\gradlew.bat clean test :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | 성공 | #3 패키지 정리 후 androidTest APK 컴파일 확인 |

## 버전 업데이트 기준

#16에서 다음 버전을 업데이트했다.

| 항목 | 이전 | 현재 | 판단 |
| --- | --- | --- | --- |
| Gradle Wrapper | `8.13` | `8.14.5` | AGP 8.x 범위에서 업데이트 |
| Android Gradle Plugin | `8.12.3` | `8.13.2` | AGP 9.x major upgrade는 제외 |
| Kotlin | `2.0.21` | `2.3.20` | Compose Compiler plugin과 함께 업데이트 |
| Compose BOM | `2024.09.00` | `2026.06.00` | Compose 라이브러리 묶음 업데이트 |
| Java source/target compatibility | `11` | `21` | Android Studio JBR 21 기준으로 정렬 |
| Kotlin JVM target | `11` | `21` | Java target과 일치하도록 정렬 |
| compileSdk | `36` | `36` | 유지 |
| targetSdk | `36` | `36` | 유지 |
| minSdk | `24` | `24` | 유지 |

Kotlin 2.3에서는 기존 `android.kotlinOptions { jvmTarget = "11" }` 방식이 빌드 에러가 되므로 `kotlin.compilerOptions` DSL로 마이그레이션했다.

Java/Kotlin target 21은 Android Studio JBR 21 기준으로 Java/Kotlin 컴파일 타깃을 맞춘다는 의미다. Android 런타임에서 Java 21 API를 자유롭게 사용할 수 있다는 뜻은 아니므로, 새 Java API 사용은 Android API 레벨과 desugaring 지원 여부를 별도로 확인한다.

AGP 9.x와 Gradle 9.x는 major upgrade라 이번 기준선에서는 제외했다. Kover와 CI 설정이 들어가기 전에 안정적인 AGP 8.x 조합을 먼저 확보한다.

## 현재 테스트 기준선

현재 소스에 존재하는 로컬 unit test는 예제 테스트 1개다.

- 파일: `app/src/test/java/com/bankingtest_kotlin/ExampleUnitTest.kt`
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
- `app/src/androidTest/java/com/bankingtest_kotlin/ExampleInstrumentedTest.kt`는 존재하지만, 이번 기준선에서는 에뮬레이터가 필요한 `connectedAndroidTest`를 실행하지 않았다.
- #3에서 main/test/androidTest 패키지와 instrumented test의 expected package를 `com.bankingtest_kotlin` 기준으로 정리했다.
- Kover 또는 JaCoCo 커버리지 리포트는 아직 설정되어 있지 않다.

## 후속 작업

- #4 Kover 기반 커버리지 HTML/XML 리포트 구성
- #6 결과 계산 로직 분리 이후 의미 있는 unit test 추가
- #8 금융 테스트 주요 흐름 Compose UI 테스트 추가

# book-search-android

Kakao 책 검색 API로 도서를 찾고, 관심 도서를 기기에 저장하는 Android 앱입니다.

- **검색 탭**: 검색어를 입력하고 키보드의 검색 버튼으로 검색합니다. 정확도순·최신순 정렬을 지원하고, 끝까지 스크롤하면 다음 페이지(20개 단위)를 불러옵니다.
- **즐겨찾기 탭**: 저장한 도서를 제목 오름차순·내림차순으로 정렬하고, 기기 안에서 검색하고, 정가 범위로 거릅니다. 앱을 다시 실행해도 유지됩니다.
- **상세 화면**: 두 탭에서 공통으로 쓰며, 여기서도 즐겨찾기를 추가·해제할 수 있습니다. 어느 화면에서 바꿔도 모든 화면에 바로 반영됩니다.

## 빌드 방법

1. 프로젝트 루트의 `local.properties`에 Kakao REST API 키를 넣습니다. 이 파일은 Git에서 제외됩니다.

   ```properties
   KAKAO_REST_API_KEY=발급받은_REST_API_키
   ```

2. 빌드하고 설치합니다.

   ```sh
   ./gradlew assembleDebug
   ./gradlew :app:installDebug
   ```

3. 테스트를 실행합니다. `core:model`은 순수 JVM 모듈이라 `test` 작업을 함께 지정해야 전체가 실행됩니다.

   ```sh
   ./gradlew testDebugUnitTest test --continue
   ```

키가 없어도 빌드와 설치는 됩니다. 이때 검색 화면은 키 설정 안내를 보여 주고 요청을 보내지 않으며, 즐겨찾기는 그대로 동작합니다.

### 제출 ZIP 구성

`scripts/make-submission-zip.sh`로 만듭니다. 저장소 전체와 `.git`에 더해, Git에서 제외한 다음 파일이 루트에 들어 있습니다. `build/`·`.gradle/` 등 빌드 산출물은 넣지 않았습니다.

- `local.properties`: 제출용 API 키. 제출자 기기의 SDK 경로(`sdk.dir`)는 뺐습니다. Android Studio로 열면 SDK 경로가 자동으로 채워지고, 명령줄 빌드는 `ANDROID_HOME` 환경 변수로 SDK를 찾습니다.
- `app-debug.apk`: debug APK. `adb install app-debug.apk`로 바로 설치할 수 있습니다.
- `demo.mp4`: 시연 영상(1분 42초)

### 빌드 환경

| 항목 | 버전 |
| --- | --- |
| Gradle | 9.4.1 (Wrapper 포함) |
| AGP | 9.2.1 |
| Kotlin | 2.4.0 |
| KSP | 2.3.10 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 |
| JDK | 앱 컴파일·바이트코드 기준 17, Gradle daemon 21 |

Metro 1.4.5의 Gradle 플러그인은 실행 JVM 21 이상을 요구합니다. 그래서 `gradle/gradle-daemon-jvm.properties`로 Gradle daemon만 JDK 21로 고정했습니다. JDK 17로 `./gradlew`를 실행해도 Gradle이 JDK 21을 찾거나 내려받아 daemon을 띄웁니다. 앱의 `sourceCompatibility`·`targetCompatibility`·`jvmTarget`은 17입니다. 근거는 [아키텍처](docs/ARCHITECTURE.md) 10절과 [구현 사양](docs/SPEC.md) 2절에 있습니다.

## 사용 기술

| 영역 | 선택 |
| --- | --- |
| UI | Jetpack Compose(BOM 2026.09.00), Material 3, Material 3 Adaptive Navigation Suite |
| 화면 이동·상태 수명 | Navigation 3 1.2.0, Compose `retain` |
| 데이터 접근 | Soil 1.0.0-alpha15 (InfiniteQuery, Subscription, Mutation) |
| 원격 | Retrofit 3.0.0, OkHttp 5.5.0, kotlinx.serialization |
| 로컬 | Room 2.8.5 |
| 의존성 주입 | Metro 1.4.5 |
| 이미지 | Coil 3.6.3 |
| 로그 | Timber 5.0.1 (debug 빌드에서만 Tree 등록) |
| 테스트 | JUnit 4, Robolectric 4.17, Compose UI Test, Molecule, Turbine, MockWebServer |

ViewModel·Repository·UseCase 계층은 두지 않았습니다. 화면 상태는 얇은 `@Composable` Presenter가 만들고, Presenter가 Soil key를 통해 Retrofit과 Room에 바로 닿습니다. 선택 이유와 비용은 [아키텍처](docs/ARCHITECTURE.md) 9절에 있습니다.

## 구조

```text
app                 Activity, Metro AppGraph, Navigation 3 백스택·탭, retain 엔트리 decorator
core:model          도서 모델, ID 규칙, 페이징 규칙, 가격 필터, 즐겨찾기 정렬 (순수 Kotlin)
core:network        Retrofit 서비스, 인증 interceptor, DTO
core:database       Room DB, 즐겨찾기 DAO·엔티티
core:data           Soil key(검색 InfiniteQuery, 즐겨찾기 Subscription), 즐겨찾기 쓰기, 오류 분류
core:ui             테마, 도서 카드·표지·상태 컴포넌트, 검색어 입력·정렬 메뉴, 즐겨찾기 토글
core:testing        Presenter 테스트 도구 (테스트 전용)
feature:search      검색 화면과 Presenter
feature:favorites   즐겨찾기 화면과 Presenter
feature:detail      상세 화면과 Presenter
```

모듈 의존 방향과 데이터 흐름은 [아키텍처](docs/ARCHITECTURE.md)에 그림으로 정리했습니다.

## 주요 구현 포인트

- **검색 경합**: 검색어·정렬마다 Soil key가 따로 있어, 이전 조건의 늦은 응답이 새 조건의 목록에 섞이지 않습니다. 빠르게 검색어·정렬을 바꾸거나 빈 검색어로 바꾸는 경우를 테스트로 확인했습니다.
- **페이지네이션**: `is_end`와 페이지 메타데이터로 마지막 페이지를 판단합니다. 마지막 페이지가 정확히 20개여도 더 요청하지 않습니다. 페이지 사이 중복 ID는 처음 나온 것만 남깁니다. 추가 로딩이 실패하면 받아 둔 목록을 유지하고 목록 아래에 재시도 버튼을 보여 줍니다.
- **즐겨찾기 단일 기준**: 즐겨찾기 상태는 Room 하나만 기준으로 삼습니다. 검색 목록·즐겨찾기 목록·상세 화면이 모두 Room Flow를 구독하므로, 어디서 바꿔도 같은 값이 보입니다.
- **쓰기 순서와 화면 이탈**: 즐겨찾기 쓰기는 앱 범위 코루틴에서 Mutex로 직렬 실행합니다. 저장 중 화면을 닫아도 쓰기는 끝까지 실행됩니다. 같은 도서는 저장 중 버튼만 비활성화하고, 화면 이동은 막지 않습니다.
- **상세 화면 중복 진입 방지**: 상세 화면이 맨 위에 있는 동안 들어오는 상세 열기 요청은 무시합니다. 두 손가락으로 카드 두 개를 동시에 눌러도 상세 화면이 하나만 쌓입니다.
- **상태 수명(retain)**: 화면별 retain 값은 Navigation 3 엔트리에 묶여 있습니다. 엔트리가 백스택에서 빠지면 함께 정리되고, 구성 변경에서는 유지됩니다.
- **구성 변경과 프로세스 재생성**: `configChanges`로 Activity 재생성을 막지 않고 실제 재생성을 견디게 했습니다. 백스택은 직렬화한 NavKey로, 입력값·정렬·필터·스크롤 위치는 `rememberSaveable`로 복원합니다. 상세 키는 진입 시점의 도서 스냅샷을 담아, 단건 조회 API가 없어도 프로세스 재생성 뒤 상세 화면을 다시 그립니다. 상태별 표는 [아키텍처](docs/ARCHITECTURE.md)의 "구성 변경과 프로세스 재생성 대응"에 있습니다.
- **오류 처리**: 키 누락·인증 실패·요청 한도 초과·서버 오류·네트워크 오류·응답 해석 실패 등을 나눠 안내합니다. 네트워크·서버 오류처럼 다시 시도할 만한 오류만 자동 재시도합니다. 오프라인에서도 저장한 도서의 목록과 상세 화면은 열립니다.
- **적응형 레이아웃**: 창 크기에 따라 하단 탭과 내비게이션 레일을 바꾸고, 목록은 `GridCells.Adaptive`로 너비에 맞게 열 수를 늘립니다.
- **보안**: 이미지 로더는 인증 interceptor가 없는 OkHttp 클라이언트를 써서 표지 요청에 API 키가 실리지 않습니다. 네트워크 로그는 debug 빌드에서만 남기며 Authorization 헤더를 가립니다.

## 검증 결과

- **자동 테스트**: 2026-10-08, 커밋 `71cdf67`에서 `./gradlew assembleDebug testDebugUnitTest test --continue` 성공. 테스트 110개(Android 모듈 87개, `core:model` 23개), 실패·오류·건너뜀 0.
- **실기기 QA**: SM-S936N(Android 17)에서 요구사항 기반 시나리오 31건을 실행해 모두 통과했습니다. 실행 중 결함 2건을 찾아, 재현 테스트를 먼저 작성한 뒤 고치고 다시 확인했습니다. 시나리오별 절차·화면·결함 기록은 [QA](docs/QA.md)에 있습니다.
- **시연 영상**: 제출 ZIP 루트의 `demo.mp4`(1분 42초). 같은 기기에서 검색·페이지네이션·정렬, 즐겨찾기 추가·해제, 즐겨찾기 정렬·검색·가격 필터, 오프라인 오류와 재시도, 회전, 다크 모드를 차례로 보여 줍니다. 용량 때문에 Git에는 넣지 않았습니다.
- **요구사항별 상태**: [요구사항 체크리스트](docs/REQUIREMENTS.md) 9절에 R-ID마다 구현 상태·검증 상태·증거·미충족 사항을 적었습니다. 실제로 확인한 항목만 체크했습니다.

## 알려진 제한

- 실제 폴더블 기기나 폴더블 에뮬레이터에서 접힘·펼침은 확인하지 않았습니다. 태블릿 크기·회전·창 크기 변경까지 확인했습니다.
- 네트워크가 다시 연결돼도 실패한 검색을 자동으로 다시 요청하지 않습니다. 재시도 버튼으로 복구합니다.
- 프로세스가 종료되는 순간 끝나지 않은 즐겨찾기 쓰기는 보장하지 않습니다.
- 표지·저자 등이 빠진 도서의 처리는 단위 테스트로만 확인했고, 실제 API 결과에서 그런 도서를 화면으로 확인하지는 못했습니다.
- 2-pane 레이아웃(목록과 상세를 나란히)은 선택 항목이라 구현하지 않았습니다.

## AI 활용

이 과제는 AI 활용이 허용되어 있어, 두 AI 도구를 단계별로 나눠 썼습니다. 결정은 개발자가 하고, AI가 낸 결과는 실행 결과로 확인한 뒤 반영했습니다. 상세 기록은 [AI 활용 기록](docs/AI_USAGE.md)에 있습니다.

- **설계(Codex)**: 개발자와 협의하며 요구사항 정리, 참고 코드 조사, 구현 전략·아키텍처 문서 초안을 작성했습니다. 코드는 작성하지 않았습니다.
- **사양 확정·구현·검증(Claude Code, Claude Opus 5.5)**: 2026-10-07 개발자 요청으로 설계 문서의 암묵지를 검토해 [구현 사양](docs/SPEC.md)을 확정하고, 앱 구현을 맡았습니다. 빌드 설정, 모든 모듈의 코드와 테스트, 실기기 QA, 검증 문서를 작성했습니다.

구현·검증 단계에서 일한 방식은 다음과 같습니다.

1. [구현 사양](docs/SPEC.md)의 기본안을 기능 단위로 나눠 구현하고, 단위마다 빌드·테스트를 실행한 뒤 커밋했습니다. 기본안이 바뀌면 사양을 먼저 고쳤습니다.
2. 지정 버전 조합의 호환성을 먼저 확인했습니다. Metro의 JVM 21 요구는 실패 로그와 근거를 개발자에게 보고하고, 개발자가 daemon JDK 21을 결정했습니다.
3. 개발자 요청으로 요구사항 기반 QA 시나리오를 만들고, adb로 실기기를 직접 조작해 실행했습니다. 화면 캡처, 요청 로그, 백스택 상태로 결과를 확인했습니다.
4. QA에서 찾은 결함은 재현 테스트를 먼저 작성해 수정 전 코드에서 실패하는 것을 확인한 뒤 고쳤습니다.
5. 문서를 쓰며 요구사항과 테스트를 다시 대조해, 빠진 테스트(빠른 조건 변경 경합, DB 다시 열기)를 추가했습니다.

개발자가 직접 판단해 바꾼 부분:

- daemon만 JDK 21로 올리고 앱 기준은 17로 유지
- 이전 프로젝트에서 쓰던 App Startup 방식으로 Timber 초기화
- 필터 시트·정렬 메뉴를 열기 전 입력란 포커스 해제, 카드 사이 간격 추가
- 이전 프로젝트 경험을 바탕으로 두 손가락 동시 탭에서 상세 화면이 두 번 쌓이는 문제 대응 요청
- 구현 뒤 요구사항 기반 QA 시나리오를 만들어 실기기에서 전부 통과시키도록 요청

## 문서

1. [요구사항 체크리스트](docs/REQUIREMENTS.md): 필수 요구사항과 R-ID별 구현·검증 기록
2. [구현 사양](docs/SPEC.md): 화면·API·ID·백스택·오류 처리 등 세부 계약
3. [아키텍처](docs/ARCHITECTURE.md): 기술별 역할, 데이터 흐름, 상태 소유권, 구성 변경 대응, 테스트 경계
4. [구현 전략](docs/STRATEGY.md): 설계 결정, 참고 코드의 활용 범위, 미채택 대안
5. [QA](docs/QA.md): 실기기 시나리오, 발견한 결함, 자동 테스트 목록
6. [AI 활용 기록](docs/AI_USAGE.md): AI에 맡긴 범위, 개발자 판단, 검증 근거
7. [저장소 작업 지침](AGENTS.md): 문서·커밋·검증·보안 기준

# book-search-android

Kakao 책 검색 API로 도서를 찾고, 관심 도서를 기기에 저장하는 Android 앱입니다.

- **검색 탭**: 검색어를 입력하고 키보드의 검색 버튼으로 검색합니다. 정확도순·최신순 정렬을 지원하고, 끝까지 스크롤하면 다음 페이지(20개 단위)를 불러옵니다.
- **즐겨찾기 탭**: 저장한 도서를 제목순으로 정렬하고, 기기 안에서 검색하고, 정가 범위로 거릅니다. 앱을 다시 실행해도 유지됩니다.
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

- `local.properties`: 제출용 API 키. `sdk.dir`는 뺐으며, Android Studio가 자동으로 채우거나 명령줄 빌드는 `ANDROID_HOME`으로 SDK를 찾습니다.
- `app-debug.apk`: debug APK. `adb install app-debug.apk`로 바로 설치할 수 있습니다.
- `demo.mp4`: 시연 영상(1분 42초)
- `demo-tablet.mp4`: 태블릿 2-pane 시연 영상(31초)

### 빌드 환경

| 항목 | 버전 |
| --- | --- |
| Gradle | 9.4.1 (Wrapper 포함) |
| AGP | 9.2.1 |
| Kotlin | 2.4.0 |
| KSP | 2.3.10 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 |
| JDK | 앱 컴파일·바이트코드 기준 17, Gradle daemon 21 |

Metro 1.4.5의 Gradle 플러그인이 실행 JVM 21 이상을 요구해 `gradle/gradle-daemon-jvm.properties`로 Gradle daemon만 JDK 21로 고정했습니다. JDK 17로 `./gradlew`를 실행해도 Gradle이 JDK 21 daemon을 찾거나 내려받습니다. 앱의 `sourceCompatibility`·`targetCompatibility`·`jvmTarget`은 17입니다. 근거는 [아키텍처](docs/ARCHITECTURE.md) 10절과 [구현 사양](docs/SPEC.md) 2절에 있습니다.

## 사용 프레임워크

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
book-search-android
├── app                          Activity, Metro AppGraph, Navigation 3 백스택·탭, retain 엔트리 decorator
│   ├── MainActivity.kt
│   ├── AppGraph.kt              앱 의존성 그래프 (Soil 클라이언트, 이미지 로더)
│   ├── initializer/             App Startup으로 debug 빌드에만 Timber 등록
│   └── navigation/
│       ├── BookSearchNavHost.kt                 탭·백스택과 NavDisplay 연결
│       ├── BookSearchBackStack.kt               두 탭 루트를 유지하는 백스택 규칙
│       ├── ListDetailSceneStrategy.kt           840dp 이상 2-pane
│       └── RetainedValuesStoreNavEntryDecorator.kt  엔트리 단위 retain 수명
├── core
│   ├── model                    도서 모델, ID 규칙, 페이징 규칙, 가격 필터, 즐겨찾기 정렬 (순수 Kotlin)
│   ├── network                  Retrofit 서비스, 인증 interceptor, DTO
│   ├── database                 Room DB, 즐겨찾기 DAO·엔티티
│   ├── data                     Soil key와 즐겨찾기 쓰기, 오류 분류
│   │   ├── SearchBooksQueryKey.kt         검색 InfiniteQuery
│   │   ├── FavoriteSubscriptionKeys.kt    즐겨찾기 Room Subscription
│   │   ├── FavoriteMutationKey.kt         화면별 즐겨찾기 Mutation
│   │   └── FavoriteWriter.kt              앱 범위 직렬 쓰기
│   ├── ui                       테마, 도서 카드·표지·상태 컴포넌트, 검색어 입력·정렬 메뉴, 즐겨찾기 토글
│   └── testing                  Presenter 테스트 도구 (테스트 전용)
├── feature                      화면마다 같은 구성
│   ├── search                   검색 (+ SearchQueryHolder: 다른 탭에 있는 동안 쿼리 유지)
│   ├── favorites                즐겨찾기 (+ PriceFilterSheet: 가격 필터)
│   └── detail                   상세
│       ├── *NavKey.kt           직렬화되는 내비게이션 키
│       ├── *ScreenGraph.kt      엔트리 단위 Metro 그래프 (retain)
│       ├── *ScreenRoot.kt       엔트리 등록, Soil·Room 값을 Presenter에 연결
│       ├── *Presenter.kt        화면 상태와 이벤트 처리 (@Composable)
│       └── *Screen.kt           UI
├── build-logic                  모듈 공통 Gradle convention 플러그인
├── docs                         요구사항, 구현 사양, 아키텍처, QA, AI 활용 기록
└── scripts                      제출 ZIP 생성 스크립트
```

모듈 의존 방향과 데이터 흐름은 [아키텍처](docs/ARCHITECTURE.md)에 그림으로 정리했습니다.

## 주요 구현 포인트

- **검색 경합**: 검색어·정렬마다 Soil key가 따로 있어 이전 조건의 늦은 응답이 새 목록에 섞이지 않습니다. 빠른 조건 변경과 빈 검색어 전환을 테스트로 확인했습니다.
- **페이지네이션**: `is_end`와 페이지 메타데이터로 마지막 페이지를 판단해, 마지막 페이지가 정확히 20개여도 더 요청하지 않습니다. 페이지 사이 중복 ID는 처음 것만 남기고, 추가 로딩이 실패하면 받아 둔 목록 아래에 재시도 버튼을 보여 줍니다.
- **즐겨찾기 단일 기준**: 즐겨찾기 상태는 Room 하나만 기준으로 삼고, 검색·즐겨찾기·상세 화면이 모두 Room Flow를 구독합니다.
- **쓰기 순서와 화면 이탈**: 즐겨찾기 쓰기는 앱 범위 코루틴에서 Mutex로 직렬 실행해, 저장 중 화면을 닫아도 끝까지 실행됩니다. 저장 중에는 그 도서의 버튼만 비활성화합니다.
- **상세 중복 진입 방지**: 상세 화면이 맨 위에 있는 동안의 상세 열기 요청은 무시해, 두 카드를 동시에 눌러도 상세는 하나만 쌓입니다. 2-pane에서는 다른 도서를 고르면 상세를 바꿉니다.
- **상태 수명과 복원**: 화면별 retain 값은 Navigation 3 엔트리에 묶여 엔트리가 빠지면 정리되고 구성 변경에서는 유지됩니다. `configChanges`로 재생성을 막지 않고, 백스택은 직렬화한 NavKey로, 입력값·정렬·필터는 Presenter의 `rememberSerializable`, 스크롤 위치는 `rememberSaveable`로 복원합니다. 서버 상태는 Soil·Room이 소유하고, 화면 전용 상태는 Presenter가 저장 데이터와 retain 데이터로 나눠 둡니다. 상세 키는 진입 시점의 도서 스냅샷을 담아 단건 조회 API 없이도 상세를 다시 그립니다([아키텍처](docs/ARCHITECTURE.md) "구성 변경과 프로세스 재생성 대응").
- **오류 처리**: 키 누락·인증 실패·요청 한도 초과·서버·네트워크·응답 해석 오류를 나눠 안내하고, 네트워크·서버 오류만 자동 재시도합니다. 오프라인에서도 저장한 도서의 목록과 상세는 열립니다.
- **적응형 레이아웃**: 창 크기에 따라 하단 탭과 내비게이션 레일을 바꾸고, 목록은 `GridCells.Adaptive`로 열 수를 늘립니다. 840dp 이상에서는 Navigation 3 `SceneStrategy`로 직접 구현한 list-detail 2-pane을 보여 줍니다(Material의 list-detail 전략은 Navigation 3 1.2.0과 맞지 않음). 창 크기가 바뀌어도 열린 상세가 유지됩니다.
- **보안**: 이미지 로더는 인증 interceptor가 없는 OkHttp 클라이언트를 써서 표지 요청에 API 키가 실리지 않습니다. 네트워크 로그는 debug 빌드에서만 남기며 Authorization 헤더를 가립니다.

## 검증 결과

- **자동 테스트**: 2026-10-08, 커밋 `ae19d98`에서 `./gradlew assembleDebug testDebugUnitTest test --continue` 성공. 테스트 120개(Android 모듈 97개, `core:model` 23개), 실패·오류·건너뜀 0.
- **실기기 QA**: SM-S936N(Android 17)에서 요구사항 기반 시나리오 35행, Galaxy Tab S9 SM-X710(Android 16)에서 2-pane 시나리오 5행을 실행해 모두 통과했습니다. 실기기 QA는 화면 상태 구조 정리(`91da389`~`ae19d98`) 이전 빌드에서 실행했고, 이 정리는 자동 테스트로만 확인했습니다. 시나리오별 절차와 화면은 [QA](docs/QA.md)에 있습니다.
- **시연 영상**: 제출 ZIP 루트에 있으며 용량 때문에 Git에는 넣지 않았습니다. `demo.mp4`(1분 42초)는 검색·페이지네이션·정렬, 즐겨찾기 추가·해제·정렬·검색·가격 필터, 오프라인 오류와 재시도, 회전, 다크 모드를 보여 줍니다. `demo-tablet.mp4`(31초)는 2-pane에서 상세 교체, 뒤로 가기, 탭 전환, 회전 뒤 상세 유지를 보여 줍니다.
- **요구사항별 상태**: [요구사항 체크리스트](docs/REQUIREMENTS.md) 9절에 R-ID마다 구현 상태·검증 상태·증거·미충족 사항을 적었습니다. 실제로 확인한 항목만 체크했습니다.

## 알려진 제한

- 실제 폴더블 기기나 폴더블 에뮬레이터에서 접힘·펼침은 확인하지 않았습니다. 태블릿 실기기의 회전과 창 크기 변경까지 확인했습니다.
- 네트워크가 다시 연결돼도 실패한 검색을 자동으로 다시 요청하지 않습니다. 재시도 버튼으로 복구합니다.
- 프로세스가 종료되는 순간 끝나지 않은 즐겨찾기 쓰기는 보장하지 않습니다.
- 표지·저자 등이 빠진 도서의 처리는 단위 테스트로만 확인했고, 실제 API 결과에서 그런 도서를 화면으로 확인하지는 못했습니다.

## AI 활용

### 활용한 작업

- **설계(Codex)**: 개발자와 협의하며 요구사항 정리, 참고 코드·Kakao API·Android 문서 조사, 위험 식별, 아키텍처 문서 초안을 맡았습니다. 코드는 작성하지 않았습니다.
- **사양·구현·검증(Claude Code, Claude Opus 5.5)**: 개발자의 요청으로 [구현 사양](docs/SPEC.md) 확정, 빌드 설정, 모든 모듈의 코드와 테스트, adb로 실행한 실기기 QA, 시연 영상, 제출 ZIP 스크립트, 검증 문서를 맡았습니다.

### 검증 방법

- AI 결과에는 근거(공식 문서, 라이브러리 소스·바이트코드, 실행한 명령과 결과)를 붙이고, 개발자가 요구사항·동작·유지보수 관점에서 검토해 채택하거나 고쳤습니다.
- 기능 단위로 빌드와 테스트를 실행한 뒤 커밋했습니다. 결과는 위 [검증 결과](#검증-결과)에 있습니다.
- 요구사항마다 실기기 시나리오를 만들어 휴대폰과 태블릿에서 실행했습니다([QA](docs/QA.md)).

### 직접 판단해 바꾼 부분

| 주제 | AI 제안·초안 | 개발자 결정 |
| --- | --- | --- |
| 상태 수명 | 일정과 익숙함을 이유로 ViewModel | Compose `retain` + Navigation 3. 대신 엔트리 수명·정리·복원 테스트를 먼저 작성 |
| 원격 통신 | 참고 프로젝트의 Ktor 구성을 기준으로 조사 | 경험이 많은 Retrofit. Soil key에 Retrofit 서비스를 주입 |
| 모듈 | `core:data` 하나 | `core:network`·`core:database` 분리 |
| JDK | Metro 버전을 낮추거나 DI 교체 | 지정 버전 유지, Gradle daemon만 JDK 21 |
| Timber 초기화 | Application에서 직접 등록 | App Startup initializer, debug 빌드에서만 등록 |
| 상세 중복 진입 | 드문 경우라 후순위 | 두 카드 동시 탭 대응과 Compose 테스트 |
| QA | 자동 테스트와 일부 기기 확인 | 요구사항 기반 시나리오 전체를 실기기에서 실행 |
| 2-pane | 선택 항목이라 미구현 | list-detail 2-pane 구현, 태블릿 실기기 확인과 시연 영상 |

결정별 근거는 [AI 활용 기록](docs/AI_USAGE.md)에 있습니다.

## 문서

1. [요구사항 체크리스트](docs/REQUIREMENTS.md): 필수 요구사항과 R-ID별 구현·검증 기록
2. [구현 사양](docs/SPEC.md): 화면·API·ID·백스택·오류 처리 등 세부 계약
3. [아키텍처](docs/ARCHITECTURE.md): 기술별 역할, 데이터 흐름, 상태 소유권, 구성 변경 대응, 테스트 경계, 선택 이유·참고 코드·미채택 대안
4. [QA](docs/QA.md): 실기기 시나리오와 자동 테스트 목록
5. [AI 활용 기록](docs/AI_USAGE.md): AI에 맡긴 범위, 개발자 판단, 검증 근거
6. [저장소 작업 지침](AGENTS.md): 문서·커밋·검증·보안 기준

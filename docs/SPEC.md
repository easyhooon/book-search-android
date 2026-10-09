# 구현 사양

원문과 UI 가이드에 근거가 없는 세부사항의 기본안과 구현이 따르는 계약을 적습니다. 동작을 바꿀 때는 이 문서를 먼저 갱신합니다. 구조·상태 소유권·참고 코드는 [ARCHITECTURE](ARCHITECTURE.md)를 봅니다.

## 1. 원문 요구 요약

원문 필수 요구, 사용자 선택, 제품 정책과 R-ID별 확인 방법은 [REQUIREMENTS](REQUIREMENTS.md)에 있습니다.

## 2. 프로젝트 설정

| 항목 | 결정 | 이유 |
| --- | --- | --- |
| applicationId / namespace 접두 | `io.github.easyhooon.booksearch` | 회사명을 넣지 않고, 참고 저장소의 `com.easyhooon.booksearch`와 구분 |
| compileSdk / targetSdk | 37 / 37 | compileSdk는 원문 지정. targetSdk는 같은 값으로 맞춤 |
| minSdk | 26 | `java.time`을 desugaring 없이 사용 |
| JDK | 컴파일 대상 Java 17, Gradle daemon JVM 21 | 아래 "JDK 기준" |
| UI 언어 | 한국어 문자열 리소스 | 사용자 대상과 UI 가이드에 맞춤 |
| 화면 방향 | 고정하지 않음 | 태블릿·폴더블·회전 대응 |

### Gradle 모듈

여러 개발자가 기능 단위로 작업한다는 평가 전제에 맞춰 모듈로 경계를 나눕니다.

| 모듈 | 책임 | 의존 |
| --- | --- | --- |
| `app` | Application, Metro 앱 그래프, NavDisplay·탭·백스택 조립 | 모든 feature, core |
| `core:model` | 도서·검색 조건·정렬·가격 필터 모델, ID·가격·필터·정렬 순수 규칙 | 없음 |
| `core:network` | Retrofit 서비스·DTO, OkHttp(이미지 기본 클라이언트, 인증 interceptor를 더한 API 클라이언트) | core:model |
| `core:database` | Room DB·Entity·DAO, 스키마 export(KSP는 이 모듈에서만 실행) | core:model |
| `core:data` | Soil key(InfiniteQuery·Subscription·Mutation), DTO·Entity→`Book` 변환, FavoriteWriter, 오류 분류 | core:model, core:network·core:database(`implementation`) |
| `core:ui` | 공통 도서 카드, 표지(Coil), 가격·날짜 표시, 상태 UI, 테마 | core:model |
| `feature:search` | 검색 탭 Root·Presenter·Screen | core:model, core:data, core:ui |
| `feature:favorites` | 즐겨찾기 탭 Root·Presenter·Screen | core:model, core:data, core:ui |
| `feature:detail` | 상세 Root·Presenter·Screen | core:model, core:data, core:ui |
| `core:testing` | Presenter 테스트 실행 도구(Molecule·Turbine 연결, Main dispatcher 규칙), Fake `FavoriteWriter`, 테스트 도서 | 테스트 의존성으로만 사용 |

앱 코드 모듈 9개와 테스트 전용 `core:testing`을 합쳐 10개입니다. `core:data`가 원격·로컬 모듈을 `implementation`으로 감싸 feature는 Soil key만 보며, Room KSP는 `core:database`에서만 돕니다. `core:designsystem`, `core:common`은 만들지 않습니다.

공통 설정은 `build-logic` 포함 빌드의 convention plugin(ID 접두 `booksearch.`)으로 모으며, 사용자 기존 저장소의 구성(includeBuild, `kotlin-dsl`, version catalog 공유, `pluginRegister` 패턴)을 따릅니다. Room 설정은 `core:database` build 파일에 직접 적습니다.

feature끼리는 서로 참조하지 않습니다. feature는 `onBookClick(book)` 같은 콜백만 노출하고, 어떤 NavKey로 이동할지는 `app`이 정합니다.

### 의존성 버전

지정 빌드 조합(Gradle 9.4.1, AGP 9.2.1, Kotlin 2.4.0, KSP 2.3.10, compileSdk 37)에서 `./gradlew assembleDebug testDebugUnitTest`가 성공하는 버전으로 고정했습니다. 참고 조합은 DroidKaigi 2026(`300a81bc`)이며, Android 전용이므로 JetBrains Compose Multiplatform 대신 AndroidX 아티팩트를 씁니다.

| 라이브러리 | 고정 버전 |
| --- | --- |
| Kotlin | 2.4.0 (원문 지정) |
| KSP | 2.3.10 (원문 지정) |
| Metro | 1.4.5 |
| Soil | 1.0.0-alpha15 |
| Compose BOM / runtime retain | 2026.09.00 / 1.12.1 |
| material3-adaptive-navigation-suite | 1.4.0 |
| Navigation 3 | 1.2.0 |
| Coil 3 | 3.6.3 |
| Timber | 5.0.1 |
| AndroidX App Startup | 1.2.0 |
| Retrofit / OkHttp | 3.0.0 / 5.5.0 |
| Room | 2.8.5 |
| Robolectric | 4.17 |
| kotlinx.coroutines / serialization | 1.11.0 / 1.11.0 |
| Molecule / Turbine | 2.2.0 / 1.2.1 |

### JDK 기준

원문은 JDK 17을 지정합니다. [AGP 9.2 공식 호환표](https://developer.android.com/build/releases/agp-9-2-0-release-notes)는 JDK 17·Gradle 9.4.1·API 37을 지원하지만, [Metro의 플러그인·컴파일러](https://zacsweers.github.io/metro/latest/changelog/#0120)는 실행 JVM 21 이상을 요구합니다. 현재 Metro 1.4.5를 엄격한 JVM 17로 실행하면 의존성 해석 단계에서 실패하므로, 원문의 JDK 17 실행 조건은 미충족입니다. Java 컴파일 대상 17과 빌드 실행 JVM 21은 별개입니다.

- `gradle/gradle-daemon-jvm.properties`에 `toolchainVersion=21`을 둡니다. JDK 17로 `./gradlew`를 실행해도 Gradle이 JDK 21 daemon을 찾거나 내려받아 실행합니다(`./gradlew --version`: `Launcher JVM: 17.0.10`, `Daemon JVM: Compatible with Java 21`).
- 앱과 라이브러리 모듈의 `sourceCompatibility`·`targetCompatibility`와 Kotlin `jvmTarget`은 17입니다. 순수 Kotlin 모듈은 `jvmToolchain(17)`으로 컴파일합니다.
- Android 단위 테스트는 daemon과 같은 JDK 21에서 실행됩니다(11절).

## 3. API 키와 빌드 구성

- Android SDK Platform 37을 설치하고 `ANDROID_HOME` 또는 `local.properties`의 `sdk.dir`로 SDK 위치를 설정해야 합니다. ZIP에는 개인 기기의 SDK 경로를 포함하지 않습니다. SDK 위치가 없는 명령줄 환경에서는 빌드가 실패합니다. Android Studio는 프로젝트를 열 때 `sdk.dir`를 생성할 수 있지만, 최종 ZIP의 새 프로젝트 열기·Sync는 아직 검증하지 않았습니다.
- 키 이름은 `local.properties`의 `KAKAO_REST_API_KEY`입니다.
- `app/build.gradle.kts`가 `local.properties`를 읽어 `BuildConfig.KAKAO_REST_API_KEY`로 주입합니다(`buildFeatures.buildConfig = true`).
- 키가 없어도 빌드는 성공합니다. 빈 문자열을 주입하고, 검색 시 "API 키가 설정되지 않았습니다" 상태를 보여 줍니다. 즐겨찾기는 키 없이 동작합니다.
- 키는 인증 interceptor에서만 `Authorization: KakaoAK {key}` 헤더로 붙입니다. 로그·예외 메시지·테스트 fixture에 키를 남기지 않습니다. HTTP 로그는 debug 빌드에서 BASIC 수준만 남기고 `Authorization` 헤더를 가립니다.
- App Startup의 `TimberInitializer`가 debug 빌드에서만 `Timber.DebugTree`를 심습니다. OkHttp 로그(`OkHttp` 태그)와 즐겨찾기 쓰기 실패를 Timber로 보냅니다. `Application`은 앱 그래프와 Coil 연결만 맡습니다. 선택 이유는 [ARCHITECTURE](ARCHITECTURE.md) 6절에 있습니다.
- `local.properties`는 Git에서 제외합니다. 제출 ZIP에는 사용자가 확인한 뒤 포함합니다.

## 4. Kakao 책 검색 API 계약

출처: [Kakao 책 검색 개발 가이드](https://developers.kakao.com/docs/ko/daum-search/dev-guide#search-book).

- 요청: `GET https://dapi.kakao.com/v3/search/book`
- 헤더: `Authorization: KakaoAK ${REST_API_KEY}`

| 파라미터 | 계약 | 이 앱의 사용 |
| --- | --- | --- |
| `query` | 필수 | 앞뒤 공백을 제거한 확정 검색어 |
| `sort` | `accuracy`(기본) / `latest`(발간일순) | 사용자 선택값 |
| `page` | 1~50, 기본 1 | 1부터 증가. 50을 넘겨 요청하지 않음 |
| `size` | 1~50, 기본 10 | **항상 20을 명시** |
| `target` | title / isbn / publisher / person | 사용하지 않음(전체 대상 검색) |

응답의 `meta`에는 `total_count`, `pageable_count`, `is_end`가 있습니다. `documents`의 필드는 다음과 같습니다.

| 필드 | 타입 | 처리 |
| --- | --- | --- |
| `title` | String | 비어 있으면 "제목 없음" 표시 |
| `contents` | String | 도서 소개 요약(API가 짧게 자름) |
| `url` | String | 대체 ID의 재료로 사용 |
| `isbn` | String | ISBN10과 ISBN13이 공백으로 구분. 한쪽만 있거나 빈 값일 수 있음 |
| `datetime` | String(ISO 8601) | 출간일. 파싱에 실패하면 정보 없음 |
| `authors` / `translators` | String[] | 비어 있을 수 있음 |
| `publisher` | String | 비어 있을 수 있음 |
| `price` / `sale_price` | Int | 정가 / 판매가. 0 이하 정가의 의미는 API 문서로 단정하지 않으며 표시·필터 정책은 아래 5절 적용 |
| `thumbnail` | String | 비어 있으면 대체 표지 |
| `status` | String | 판매 상태(예: 정상판매, 품절, 절판). 상세에서만 표시 |

- DTO는 모든 필드에 기본값을 두고 `ignoreUnknownKeys = true`로 역직렬화해, 필드 누락으로 목록 전체가 실패하지 않게 합니다.
- 종료 판정은 `meta.is_end == true` 또는 마지막 page가 50인 경우이며, 문서 개수로 추정하지 않습니다.
- 이 앱의 `InfiniteQueryKey`는 주입된 Retrofit 서비스를 호출해 페이지 번호에 해당하는 `SearchPage`를 반환합니다. 실패는 페이지 번호와 원인을 담은 `SearchPageFetchException`으로 전파하고, 오류 분류는 원인 예외를 기준으로 합니다. `CancellationException`은 감싸지 않고 그대로 전파합니다.

## 5. 도서 모델과 ID

`Book`은 화면 계약 모델이며 DTO나 Room Entity와 분리합니다. 필드는 `id`, `isbn10`, `isbn13`, `title`, `authors`, `translators`, `publisher`, `publishedDate(LocalDate?)`, `price(Int?)`, `salePrice(Int?)`, `thumbnailUrl`, `contents`, `url`, `status`입니다.

`Book.price`는 API·Room·직렬화의 원본 정가를 보존합니다. 표시는 양수만 반환하는 `availablePrice`를 사용하고, 필터는 같은 양수 정가 기준을 적용합니다. **정가가 0 이하이거나 없으면 "가격 정보 없음", 가격 필터 적용 시 제외, 필터 미적용 시 도서 유지**가 이 앱의 방어적 정책입니다. 기존 Room 데이터도 같은 규칙을 적용하며 스키마 변경은 없습니다. 판매가의 기존 정책(음수는 정보 없음, 0은 표시 가능)은 유지합니다.

ID 규칙은 `core:model`의 순수 함수 하나로 구현하고 단위 테스트합니다.

1. `isbn`을 공백으로 나눈 뒤 각 토큰에서 숫자와 `X`만 남깁니다.
2. 길이가 13인 토큰이 있으면 `isbn13:{값}`을 ID로 씁니다.
3. 없으면 길이 10인 토큰으로 `isbn10:{값}`을 씁니다.
4. ISBN이 없으면 `url:{url}`을 씁니다. url도 비어 있으면 `fallback:{title}|{publisher}|{datetime}`을 씁니다.

같은 책이 응답마다 ISBN13·ISBN10 한쪽으로만 오면 다른 ID가 되며, 이 한계는 README에 적습니다. 제목만으로 ID를 만들지 않습니다.

## 6. 화면 사양

UI 가이드를 기준으로 Material 3 기본 컴포넌트로 구성합니다.

### 공통 도서 카드

- 구성: 표지 썸네일, 제목(최대 2줄), 저자(쉼표로 연결), 출판사, 출간일(`yyyy.MM.dd`), 가격, 즐겨찾기 하트 버튼
- 가격: 정가를 기본으로 표시합니다. 판매가가 유효하고 정가보다 낮으면 할인 판매가를 함께 표시합니다. 가격 필터 기준(정가)과 맞추기 위해서입니다. 정가가 0 이하이거나 없으면 "가격 정보 없음"으로 표시합니다.
- 숫자는 `#,###원` 형식입니다. 저자·출판사·출간일이 비면 "정보 없음"으로 채워 카드 높이를 유지합니다.
- 하트 버튼은 해당 도서의 저장이 진행되는 동안만 비활성화하고, 다른 조작과 이동은 막지 않습니다. 하트 외 카드 영역을 누르면 상세로 이동합니다.

### 검색 탭

- 검색창 placeholder는 "검색어를 입력하세요"이고, 검색 버튼이나 IME로 확정합니다. 아래에 정렬 라벨("정확도순" / "발간일순")과 정렬 버튼을 두며, 정렬을 바꾸면 확정 검색어로 1페이지부터 다시 검색합니다.
- 상태별 화면: 검색 전 "도서를 검색해 보세요", 최초 로딩은 전체 로딩, 결과 없음 "검색 결과가 없습니다", 최초 오류는 메시지와 재시도 버튼, 추가 로딩은 하단 로딩, 추가 오류는 기존 목록 유지와 하단 재시도 버튼.
- 빈 검색어를 확정하면 진행 중인 요청과 결과를 정리하고 검색 전 상태로 돌아갑니다.
- 확정할 때 키보드를 내립니다. 같은 검색어·정렬을 다시 확정하면 오류 상태일 때만 재시도합니다. 당겨서 새로고침은 없습니다.
- 마지막 아이템 근처(남은 아이템 5개 이하)가 보이면 다음 페이지를 요청합니다. 보이는 아이템 수에 최소 문턱은 두지 않습니다.
- 여러 페이지를 합칠 때 같은 ID가 다시 나오면 처음 나온 항목만 남깁니다. 목록 `key`가 도서 ID라 중복은 크래시로 이어집니다. 다음 페이지 번호와 종료 여부는 중복 제거 전의 응답 기준으로 계산합니다.
- 검색 결과의 `staleTime`은 5분(`onConfigureOptions`)입니다. 그 안에는 탭 전환·상세 복귀 시 같은 조건의 캐시를 재사용하며, 5분이 지난 쿼리를 다시 구독하면 재조회할 수 있습니다.
- 검색 루트가 백스택에 있는 동안 확정 조건의 쿼리를 retain한 참조로 붙잡고, 조건 변경·검색어 비움·엔트리 은퇴 때 놓습니다(`SearchQueryRetentionTest`). Soil은 구독이 끊기고 `keepAliveTime`(기본 5초)이 지나면 성공 상태만 남기므로, 붙잡지 않으면 추가 로딩이 실패한 목록이 다른 탭에 5초 넘게 머문 뒤 전체 오류로 바뀝니다.
- 검색 결과 ID(`SearchBooksQueryId`)는 정규화한 검색어와 정렬로 만듭니다. Presenter는 `rememberInfiniteQuery`를 `key(queryKey.id)` 그룹 안에서 호출합니다. Soil compose의 기본 수집이 key 없는 `collectAsState`라서, 그룹을 나누지 않으면 조건이 바뀐 첫 프레임에 이전 결과가 보입니다.

### 즐겨찾기 탭

- 검색창 placeholder는 "제목 또는 저자 검색"이고, 검색 버튼이나 IME로 확정합니다.
- 로컬 검색 규칙: 앞뒤 공백을 제거한 질의어가 제목 또는 저자 중 하나에 부분 일치하면 표시합니다. 영문은 대소문자를 구분하지 않습니다. 빈 질의어는 전체를 표시합니다.
- 상단에 정렬 라벨("제목 오름차순" / "제목 내림차순"), 필터 버튼, 정렬 버튼을 둡니다. 기본은 제목 오름차순입니다.
- 정렬 규칙: 제목을 앞뒤 공백 제거 후 `java.text.Collator(Locale.KOREAN)`으로 비교합니다. 제목이 같으면 ID로 비교해 순서를 고정합니다. SQLite 기본 정렬은 쓰지 않습니다.
- 가격 필터는 바텀시트에서 최소·최대 정가를 입력하고 "적용"으로 확정합니다. 세부 규칙은 [REQUIREMENTS](REQUIREMENTS.md)의 제품 정책 2번을 따릅니다. 입력은 숫자 키패드·숫자만·최대 9자리(Int 범위)입니다. 적용 중이면 필터 버튼에 표시하고 "초기화"를 제공합니다.
- 이 탭에서 해제한 카드는 Room 반영과 동시에 목록에서 사라집니다. 실행 취소는 없습니다.
- 검색·정렬·필터는 Room에서 읽은 전체 즐겨찾기 목록을 메모리에서 순수 함수로 처리합니다. 즐겨찾기는 많아야 수백 건이라고 보고 한국어 정렬과 테스트 용이성을 우선했습니다. 수천 건 이상이면 DB 질의로 옮겨야 하며 README 한계에 적습니다.
- 빈 상태: 즐겨찾기가 없으면 "즐겨찾기한 도서가 없습니다", 조건에 맞는 도서가 없으면 "조건에 맞는 도서가 없습니다".

### 상세 화면

- 상단 앱바에 뒤로 가기, 제목, 즐겨찾기 하트를 둡니다. 1-pane 상세에서는 탐색 UI를 숨깁니다(UI 가이드와 동일, 10절).
- 본문에는 표지(큰 이미지), 제목, 저자, 역자(있을 때만), 출판사, 출간일, ISBN, 정상가, 할인가, 판매 상태(정상판매가 아닐 때만), 책 소개(`contents`)를 표시합니다.
- 할인가가 없거나 정가와 같으면 할인가 줄은 "할인 없음"으로 표시합니다.
- 넓은 화면에서는 본문 최대 너비를 제한하고 가운데 정렬합니다.

### 공통 표현

- Material 3 기본 테마로 시스템 라이트/다크를 따르고, dynamic color는 쓰지 않습니다.
- 표지와 하트 버튼에 `contentDescription`을 붙이며, 하트는 상태에 따라 "즐겨찾기 추가" / "즐겨찾기 해제"로 읽힙니다.
- 즐겨찾기 쓰기가 실패하면 스낵바로 알립니다. 성공은 하트 상태 변화로 충분하므로 메시지를 띄우지 않습니다.

### 저장·복원하는 화면 상태

| 상태 | 저장 방식 | 시스템 프로세스 재생성 후 |
| --- | --- | --- |
| 검색 입력값, 확정 검색어, 정렬 | Presenter의 저장 데이터(`rememberSerializable`) | 확정 검색어가 있으면 1페이지부터 다시 검색 |
| 즐겨찾기 입력값, 확정 검색어, 정렬, 가격 필터 | Presenter의 저장 데이터(`rememberSerializable`) | Room 목록에 그대로 다시 적용 |
| 탭과 상세 엔트리 | `rememberNavBackStack` | 그대로 복원. 상세는 스냅샷으로 즉시 표시 |
| 목록 스크롤 위치 | 조회 조건(확정 검색어·정렬, 즐겨찾기는 가격 필터 포함)을 `rememberSaveable`의 입력으로 두고 `LazyGridState.Saver`로 첫 표시 항목의 index·offset 저장. 조건 변경 시 맨 위에서 시작하고 조건이 같으면 위치 유지 | 위치 값은 복원하되 검색 결과 재조회로 이전 페이지 전체·동일 도서 위치는 보장하지 않음 |
| 검색 결과 페이지, 진행 중 쓰기 ID | Soil 캐시 / Presenter의 retain 데이터 | 소멸 |

### 화면 상태 소유

세 화면은 상태를 출처별로 나눕니다. 서버 상태는 Soil, 즐겨찾기는 Room, 화면에서만 쓰는 클라이언트 상태는 Presenter가 공식 문서의 저장·retain 분리 방식대로 직접 둡니다. 별도 Store 클래스는 두지 않습니다. 이벤트 처리가 Presenter 한 파일에 보여야 재시도·다음 페이지처럼 쿼리를 쓰는 흐름을 파일을 오가지 않고 읽을 수 있기 때문입니다.

| 구성 | 수단 | 내용 |
| --- | --- | --- |
| 저장 데이터 | `rememberSerializable`로 `@Serializable` 묶음 하나 | 검색: 입력값, 확정 검색어, 정렬. 즐겨찾기: 입력값, 확정 검색어, 정렬, 최소·최대 정가 |
| retain 데이터 | `private` `RetainObserver`로 감싼 값 | `retainFavoriteToggler(key)`: 요청 scope·Mutation 참조를 접수 명령 완료까지 유지하고, 엔트리 은퇴 시 신규 요청부터 차단. `retainSearchQueryHolder()`는 엔트리 은퇴 시 쿼리 참조 해제 |

- Presenter의 `eventSink`가 화면의 단일 진입점이며, 이벤트를 그 자리에서 처리합니다. InfiniteQuery 객체와 Room 즐겨찾기 ID처럼 컴포지션마다 바뀌는 값은 `eventSink` 람다가 그 컴포지션의 값을 읽고, retain 값에는 보관하지 않습니다.
- `FavoriteToggler`는 받은 scope와 `mutate`로 명령을 전송합니다. 진행·실패 상태는 화면별로 소유하고, Mutation 참조는 해당 화면이 접수한 모든 요청의 완료까지 유지합니다.
- 저장 데이터는 retain 데이터로 바꾸지 않고, retain 데이터는 프로세스 재생성 뒤 새로 만듭니다.

### NavKey

모든 키는 `@Serializable`이며 `rememberNavBackStack`으로 저장·복원합니다.

- `SearchNavKey`, `FavoritesNavKey`: 탭 루트
- `BookDetailNavKey(book: Book)`: 상세

상세에 필요한 필드가 `Book`과 같으므로 별도 스냅샷 타입 없이 `Book`을 그대로 담습니다. 엔트리는 `entry<BookDetailNavKey>(clazzContentKey = { it.book.id })`로 등록해, 같은 도서의 상세는 같은 retain 저장소를 씁니다.

### 상세에 스냅샷을 넣는 이유

Kakao API에는 단건 조회가 없어, bookId만 저장하면 ISBN 없는 도서는 재조회할 수 없고 불안정한 네트워크에서 프로세스가 재생성되면 상세가 곧바로 오류가 됩니다. 그래서 카드를 누른 시점의 스냅샷을 NavKey에 넣습니다. 도서 한 권은 수 KB 이하이고 쌓이는 상세도 소수라 saved state 크기 위험이 낮습니다. 검색 결과 목록은 saved state에 넣지 않습니다.

- 표시 데이터는 Room에 같은 ID가 있으면 Room 값, 없으면 스냅샷입니다. 즐겨찾기 여부는 항상 Room 기준이며, 해제해도 화면은 스냅샷으로 유지됩니다.
- 상세는 네트워크를 호출하지 않으므로 오프라인과 프로세스 재생성 후에도 바로 표시됩니다. 대가는 가격 같은 정보가 진입 시점 값으로 고정된다는 점이며, README 한계에 적습니다.

### 백스택 규칙

DroidKaigi 2026의 탭 처리에는 순서 역전([#344](https://github.com/DroidKaigi/conference-app-2026/issues/344))과 상세 중복 진입([#226](https://github.com/DroidKaigi/conference-app-2026/issues/226)) 이슈가 열려 있어, 아래 규칙을 직접 구현해 테스트합니다.

백스택은 하나만 둡니다. 형태는 항상 `[비활성 탭 루트(있으면), 활성 탭 루트, 상세…]`이고 시작은 `[SearchNavKey]`입니다.

| 동작 | 결과 |
| --- | --- |
| 다른 탭 선택 | 현재 탭 위의 상세를 모두 pop하고, 대상 탭 루트를 맨 위로 옮김(없으면 추가). 두 탭 루트는 pop하지 않으므로 목록 상태가 retain으로 유지됨 |
| 같은 탭 재선택 | 상세가 있으면 탭 루트까지 pop. 루트에서는 아무것도 하지 않음 |
| 카드 선택(1-pane) | 맨 위가 상세면 무시하고, 아니면 상세를 push. 상세에는 다른 도서로 가는 경로가 없으므로, 두 카드를 동시에 누르거나 전환 중에 다른 카드를 눌러도 상세는 하나만 쌓임 |
| 카드 선택(2-pane) | 맨 위가 같은 도서의 상세면 무시. 다른 도서의 상세면 그 상세를 pop하고 새 상세를 push(교체). 상세가 없으면 push. 어느 경우에도 상세는 하나만 쌓임 |
| 뒤로 가기(상세) | 상세 pop |
| 뒤로 가기(즐겨찾기 루트) | 검색 탭 선택과 같은 동작(즐겨찾기 루트는 유지) |
| 뒤로 가기(검색 루트) | 앱 종료(Activity finish) |

탭을 바꾸면 이전 탭의 상세는 정리됩니다. 탭별 독립 상세 스택보다 단순하고 retain 정리 시점이 명확합니다.

엔트리 decorator(saveable·retain)는 백스택 전체에 적용하고, `NavDisplay`에는 활성 탭 루트부터의 엔트리만 넘깁니다. 비활성 탭 루트는 pop되지 않아 retain 값이 유지되고(`BookSearchNavHostTest`), 뒤로 가기 미리보기에 다른 탭이 비치지 않습니다. 엔트리가 하나뿐이면 `NavDisplay`가 뒤로 가기를 받지 않으므로 즐겨찾기 루트만 `BackHandler`로 처리합니다.

Navigation 3 1.2.0에는 retain decorator가 없어, `compose-runtime-retain` 1.12.1의 `RetainedValuesStoreRegistry`(`retainRetainedValuesStoreRegistry`, `LocalRetainedValuesStoreProvider`, `clearChild`)로 직접 만듭니다(`RetainedValuesStoreNavEntryDecorator.kt`). pop된 엔트리의 저장소는 `clearChild`로 정리합니다.

## 8. 즐겨찾기 쓰기 경로

### 확인한 Soil 사실

Soil 1.0.0-alpha15 소스 기준입니다.

- `MutationRef.mutate`는 랑데부 채널로 명령을 보냅니다. actor가 앞 명령을 처리 중이면 send가 대기하고, 그동안 호출 코루틴이 취소되면 명령은 전달되지 않습니다.
- actor는 명령을 하나씩 처리하므로 같은 Mutation key 안에서는 순서가 보장됩니다.
- 마지막 구독자가 사라지면 `keepAliveTime`(기본 5초) 뒤 Mutation의 scope가 취소됩니다. Mutation 안에서 직접 DB에 쓰면 화면 pop 후 완료 보장은 약 5초뿐입니다.
- Subscription의 Flow는 replay 없이 `shareIn(WhileSubscribed(keepAliveTime))`으로 공유됩니다. 구독자가 없는 keepAliveTime 동안에도 Flow를 수집하지만 그 사이 값은 버리고, 그 안에 다시 구독하면 이미 도는 Flow에 붙어 새 값이 오지 않습니다.

### 최종 쓰기 동작

- 살아 있는 화면의 `FavoriteToggler`가 도서 ID를 pending에 추가한 시점을 접수로 봅니다. 같은 화면에서 같은 도서의 쓰기가 진행 중이면 연타를 무시합니다. Mutation 전송은 즉시 시작합니다.
- 모든 화면은 `MutationId("favorite")`를 공유합니다. 하나의 actor가 앞선 writer 결과를 기다린 뒤 다음 명령을 처리해 실제 UI 경로의 저장·해제 접수 순서를 유지합니다. 진행·오류 표시는 각 화면의 Toggler가 소유합니다.
- 엔트리가 pop되면 신규 요청을 차단하고 요청 Job에 `complete()`를 호출합니다. 이미 접수한 요청이 Mutation 전달·쓰기 결과 수신까지 끝나면 마지막 완료 콜백에서 참조를 닫습니다. 요청 scope는 `FavoriteWriter`의 앱 scope와 별개입니다.
- `FavoriteWriter.write(FavoriteCommand)`는 `ApplicationCoroutineScope`(`SupervisorJob` + `Dispatchers.Default`)의 `async`와 하나의 Mutex로 Room 쓰기를 실행합니다. writer의 임의 동시 호출까지 전역 FIFO를 보장하는 API는 아닙니다.
- 실패하면 pending을 정리하고 로그를 남깁니다. 화면이 열려 있으면 스낵바용 실패 상태를 표시하며, 닫힌 화면의 실패를 새 화면에 전달하지 않습니다. 취소 예외는 일반 실패로 바꾸지 않고 재전파합니다.
- `FavoriteMutationLifecycleTest`는 실제 retain 저장소·Soil·writer와 Fake DAO를 연결해 화면 종료 중 대기 명령 완료, 같은 도서 명령 순서, 연타·종료 후 신규 요청 차단, 현재·닫힌 화면의 실패 처리를 확인합니다. 프로세스 종료 시 미완료 쓰기 보장은 범위 밖입니다.

### 읽기 경로

- `FavoriteIdsSubscriptionKey`: `Flow<Set<String>>`, 검색 카드 하트
- `FavoriteBooksSubscriptionKey`: `Flow<List<Book>>`, 즐겨찾기 탭
- `FavoriteBookSubscriptionKey(id)`: `Flow<Book?>`, 상세
- 세 key 모두 `keepAliveTime = 0`으로 둡니다. 화면이 구독을 놓으면 Room Flow 수집도 멈추고, 다시 구독하면 Room이 현재 값부터 내보냅니다. 기본값이면 상세에 들어간 직후 저장하고 돌아온 목록이 저장 전 하트로 남습니다(`FavoriteSubscriptionKeysTest`, TC-71). 비용은 돌아올 때마다 Room을 한 번 다시 조회하는 것입니다.

Room Entity는 `id`(PK), 분리된 `isbn10`·`isbn13`, 그 밖의 도서 필드, `savedAt`으로 구성합니다. 원본 `isbn` 문자열은 보관하지 않습니다. 목록 필드는 TypeConverter로 저장합니다. DB 버전은 1이고 `exportSchema = true`로 스키마 JSON을 저장소에 둡니다.

## 9. 네트워크 오류와 재시도

- OkHttp 타임아웃은 연결 10초, 읽기 15초입니다.
- 오류 분류와 처리는 다음과 같습니다.

| 원인 | 분류 | 자동 재시도 | 화면 안내 |
| --- | --- | --- | --- |
| `IOException`(타임아웃 포함) | 네트워크 | 함 | 연결 상태 확인 후 재시도 |
| HTTP 5xx, 429 | 일시 오류 | 함 | 잠시 후 재시도 |
| HTTP 401, 403 | 인증 | 안 함 | API 키 확인 안내 |
| 기타 4xx | 요청 | 안 함 | 일반 오류 |
| 직렬화 실패 | 응답 형식 | 안 함 | 일반 오류 |
| `CancellationException` | 취소 | 안 함 | 표시하지 않고 재전파 |

- 자동 재시도는 Soil의 `QueryOptions.shouldRetry: (Throwable) -> Boolean`을 씁니다. key의 `onConfigureOptions`에서 `options.copy(shouldRetry = { error -> error.toErrorKind().isRetryable })`로 지정하고, 횟수와 간격은 Soil 기본값을 씁니다. 키 미설정(`MissingApiKeyException`)은 `IOException` 하위 타입이지만 재시도하지 않습니다.
- 최초 로딩 실패는 `refresh()`합니다. 기존 데이터가 있는 오류는 `SearchPageFetchException.page`를 다음 페이지 값과 비교해, 추가 로딩 실패일 때만 해당 페이지를 다시 요청하고 기존 페이지 재조회 실패는 `refresh()`합니다. `SearchRetryTest`로 첫 페이지·여러 페이지 재조회와 추가 로딩을 각각 확인합니다.
- `AndroidNetworkConnectivity`는 네트워크 복구 2초 뒤 활성 실패 쿼리에 resume을 요청합니다. 실제 조회는 미조회·무효화·stale 조건을 추가로 만족할 때만 실행하며, 오류 버튼으로 직접 재시도할 수 있습니다. 기존 [QA](QA.md) TC-42의 자동 요청 0건은 당시 관찰 결과이며 현재 APK의 재연결 경로는 미검증입니다. 상세 조건은 [아키텍처](ARCHITECTURE.md#검색-캐시의-수명과-재조회) 참고. 오류 분류와 화면 문구는 일대일 대응하지 않으며 HTTP 5xx·429는 같은 안내를 사용합니다.
- 이미지용 클라이언트는 인증 interceptor 없는 기본 `OkHttpClient`이고, API 클라이언트는 `newBuilder()`로 interceptor만 더합니다.

## 10. 적응형 레이아웃

- 탐색 UI는 `NavigationSuiteScaffold`를 씁니다. compact에서는 하단 바, medium·expanded에서는 내비게이션 레일입니다. 1-pane에서 상세가 맨 위면 `layoutType = NavigationSuiteType.None`으로 탐색 UI를 숨깁니다. 2-pane에서는 목록이 함께 보이므로 레일을 유지합니다.
- 목록은 `LazyVerticalGrid(GridCells.Adaptive(minSize = 360.dp))`로 구성합니다. compact에서는 1열이고 넓은 창에서는 2~3열이 됩니다. 검색창과 필터 영역은 최대 너비를 제한합니다.
- 회전·창 크기 변경 후에도 입력값, 확정 조건, 스크롤 위치, 상세 엔트리가 유지됩니다.

### list-detail 2-pane

2-pane은 제품 정책 5번의 선택 항목이며, 1-pane 동작과 백스택 형태는 그대로 두고 장면 구성만 더합니다.

- 조건: 창 너비가 expanded(840dp 이상)일 때만 2-pane입니다. 판단은 `currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)`입니다.
- 배치: 왼쪽에 활성 탭 루트(목록, 고정 400dp·1열), 오른쪽에 상세를 둡니다. 상세가 없으면 "도서를 선택하세요" 안내를 보여 줍니다.
- 구현: 앱의 `ListDetailSceneStrategy`가 엔트리 metadata의 목록·상세 역할을 보고, 넘긴 엔트리가 `[목록]` 또는 `[목록, 상세]`이면 두 칸 장면을 만듭니다. 그 밖과 1-pane에서는 `null`(기본 단일 장면)입니다.
- 직접 구현하는 이유: Material의 `material3-adaptive-navigation3`(1.3.0, 1.4.0-alpha02)는 `navigation3-ui` 1.0.0 기준으로 빌드되어 이 앱의 1.2.0 `SceneStrategy`(`SceneStrategyScope` 수신 함수)와 맞지 않습니다. 버전을 낮추지 않고 필요한 부분만 만듭니다.
- 장면 key는 목록 엔트리의 contentKey입니다. 상세를 열고 닫거나 교체해도 오른쪽 칸만 바뀌고, 탭을 바꿀 때만 장면 전환 애니메이션이 일어납니다.
- 카드 선택: 다른 도서를 누르면 상세를 교체하고(7절 표), 교체된 상세의 retain 값은 은퇴합니다. 선택된 카드는 2-pane에서만 강조합니다.
- 뒤로 가기: `[목록, 상세]` 장면의 `previousEntries`는 `[목록]`입니다. 뒤로 가기는 상세를 닫고 안내 칸을 보여 주며, 그다음은 1-pane과 같습니다.
- 크기 변경: 백스택은 그대로이고 장면만 다시 계산합니다. 상세가 열린 채 좁아지면 상세가 전체 화면이 되고 탐색 UI가 숨겨지며, 넓어지면 목록과 나란히 보입니다.
- 테스트: `BookSearchBackStackTest`(교체 규칙), `BookSearchNavHostTest`(넓은 창 qualifier에서 두 칸·레일·retain 은퇴·안내 칸·크기 변경), 실기기 태블릿 수동 확인.

## 11. 테스트 환경

| 대상 | 도구 | 실행 위치 |
| --- | --- | --- |
| ID·가격·필터·정렬·날짜 규칙 | JUnit4 + kotlin.test | JVM 단위 테스트 |
| Retrofit API | OkHttp MockWebServer | JVM 단위 테스트 |
| Soil key와 페이징 | soil-query-test(`TestSwrClientPlus`, Fake key) + coroutines-test | JVM 단위 테스트 |
| Presenter | Molecule + Turbine + coroutines-test | JVM 단위 테스트 |
| Room | Robolectric + in-memory DB | JVM 단위 테스트 |
| Compose 내비게이션·적응형·retain 수명 | Robolectric Compose UI 테스트 | JVM 단위 테스트 |
| 프로세스 재생성·폴더블·실제 API | 실기기 QA | 기기·절차·확인 한계를 기록 |

Robolectric을 쓰는 7개 모듈의 `robolectric.properties`는 현재 `sdk=36`을 사용합니다. SDK 36 이상은 JDK 21에서만 실행되고, 테스트 JVM에 `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED`가 없으면 `IllegalAccessException ... does not export jdk.internal.access`로 실패하므로 convention plugin의 `unitTests.all`에서 넣습니다.

실행 명령은 `./gradlew testDebugUnitTest test`(Android 모듈과 순수 Kotlin `core:model`)입니다.

## 12. 제출 패키지

- ZIP 기본 파일명은 `book-search-android.zip`이며, 내부 APK 이름은 `app-debug.apk`입니다.
- ZIP에는 `.git`과 `local.properties`를 포함하고, 모든 `build/`, `.gradle/`, `.idea/`, `.kotlin/` 디렉터리를 제외합니다. 생성 스크립트는 `scripts/make-submission-zip.sh`입니다.
- APK는 사용자 키로 빌드한 `./gradlew assembleDebug` 산출물입니다. ZIP을 새 디렉터리에 풀어 `assembleDebug`가 다시 성공하는지 확인합니다.
- 메일 발송, 실제 키 확인, 최종 첨부는 사용자가 합니다.

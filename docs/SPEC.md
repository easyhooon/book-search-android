# 구현 사양

기준일: 2026-10-07. 구현 착수 전에 남아 있던 암묵지를 이 문서에서 확정합니다. 과제 원문은 의문점을 스스로 합리적으로 판단해 진행하도록 안내합니다. 이에 따라 원문과 UI 가이드에 근거가 없는 세부사항은 아래 기본안으로 정하고 이유를 남깁니다. 구현 중 검증 결과로 바뀌면 이 문서를 먼저 갱신합니다.

요구사항 체크는 [REQUIREMENTS](REQUIREMENTS.md), 구조와 상태 소유권은 [ARCHITECTURE](ARCHITECTURE.md), 일정·위험·참고 코드는 [STRATEGY](STRATEGY.md)를 기준으로 봅니다. 이 문서는 구현자가 바로 코드로 옮길 수 있는 계약만 다룹니다.

## 1. 원문 요구 요약

회사명과 연락처 없이 기능·제출 조건만 요약합니다.

- 화면: 하단 탭 2개. 탭1은 원격 검색 리스트, 탭2는 즐겨찾기 리스트입니다. 도서 상세 화면이 있습니다.
- 검색 리스트: 카드에 제목·저자·출판사·출간일·가격 등과 이미지를 표시합니다. 질의어 검색, 정확도순/발간일순, 1페이지 20개 페이징, 카드별 즐겨찾기 토글, 로컬 저장, 카드 선택 시 상세 이동을 지원합니다.
- 즐겨찾기 리스트: 같은 카드 구성, 로컬 데이터 안의 질의어 검색, 제목 오름차순/내림차순 정렬, 금액 필터, 토글, 상세 이동을 지원합니다.
- 상세: 이미지, 제목, 각종 정보, 즐겨찾기 토글을 제공합니다.
- 품질: 지하철처럼 불안정한 네트워크에서도 쓸 수 있어야 하고, 태블릿·폴더블 사용자를 고려합니다.
- 평가 관점: 대규모 앱의 한 기능이고 여러 개발자가 함께 작업한다는 전제에서의 구조, 고려한 상황과 판단 근거, 고려했지만 구현하지 않은 것의 기록, 저장소 안에서의 인수인계 가능성을 봅니다.
- 제출: 프로젝트 압축 파일(.git과 local.properties 포함, build·.gradle 제외), 실행 가능한 APK(debug 허용), README가 필요합니다. 시연 영상(3분 이내)은 선택입니다. 파일명 규칙은 없습니다. README에는 빌드 방법, 사용 프레임워크, 구조, 주요 구현 포인트를 적습니다. AI를 활용했다면 활용 작업, 검증 방법, 직접 판단해 바꾼 부분도 적습니다.
- UI 가이드는 참고용이며 세부 디자인은 자유입니다.

## 2. 프로젝트 설정

| 항목 | 결정 | 이유 |
| --- | --- | --- |
| applicationId / namespace 접두 | `io.github.easyhooon.booksearch` | 회사명을 넣지 않고, 참고 저장소의 `com.easyhooon.booksearch`와 구분 |
| compileSdk / targetSdk | 37 / 37 | compileSdk는 원문 지정. targetSdk는 같은 값으로 맞춤 |
| minSdk | 26 | `java.time`을 desugaring 없이 사용. 현재 사용 기기 분포에서 제외 범위가 작음 |
| JDK | 17 toolchain | 원문 지정 |
| UI 언어 | 한국어 문자열 리소스 | 사용자 대상과 UI 가이드에 맞춤 |
| 화면 방향 | 고정하지 않음 | 태블릿·폴더블·회전 대응 |

### Gradle 모듈

여러 개발자가 기능 단위로 나누어 작업한다는 평가 전제에 맞춰 Gradle 모듈로 경계를 나눕니다. 공통 설정은 `build-logic` 포함 빌드의 작은 convention plugin으로 모읍니다.

| 모듈 | 책임 | 의존 |
| --- | --- | --- |
| `app` | Application, Metro 앱 그래프, NavDisplay·탭·백스택 조립 | 모든 feature, core |
| `core:model` | 도서·검색 조건·정렬·가격 필터 모델, ID·가격·필터·정렬 순수 규칙 | 없음 |
| `core:network` | Retrofit 서비스·DTO, OkHttp(이미지 기본 클라이언트, 인증 interceptor를 더한 API 클라이언트) | core:model |
| `core:database` | Room DB·Entity·DAO, 스키마 export(KSP는 이 모듈에서만 실행) | core:model |
| `core:data` | Soil key(Query·InfiniteQuery·Subscription·Mutation), DTO·Entity→`Book` 변환, FavoriteWriter, 오류 분류 | core:model, core:network·core:database(`implementation`) |
| `core:ui` | 공통 도서 카드, 표지(Coil), 가격·날짜 표시, 상태 UI, 테마 | core:model |
| `feature:search` | 검색 탭 Root·Presenter·Screen | core:model, core:data, core:ui |
| `feature:favorites` | 즐겨찾기 탭 Root·Presenter·Screen | core:model, core:data, core:ui |
| `feature:detail` | 상세 Root·Presenter·Screen | core:model, core:data, core:ui |

모두 9개입니다. 처음에는 `core:data` 하나에 원격·로컬을 함께 두려 했으나, Repository가 없는 구조에서는 feature가 Soil key만 보도록 Retrofit·Room 타입을 `core:data` 뒤로 숨기는 편이 경계가 선명하고, Room KSP 재실행 범위도 줄어들어 2026-10-07에 나눴습니다. `core:designsystem`, `core:common`은 만들지 않습니다(Material 3 기본 테마, 공용 코드 없음).

convention plugin은 사용자 기존 저장소의 `build-logic` 구성(includeBuild, `kotlin-dsl`, version catalog 공유, `pluginRegister` 패턴)을 따르고, Circuit·Firebase·detekt 관련 플러그인은 두지 않습니다. 플러그인 ID 접두는 `booksearch.`입니다. Room은 `core:database` 한 곳에서만 쓰므로 해당 build 파일에 직접 적습니다.

feature끼리는 서로 참조하지 않습니다. feature는 `onBookClick(book)` 같은 콜백만 노출하고, 어떤 NavKey로 이동할지는 `app`이 정합니다. AGP 9 환경에서 convention plugin 구성이 막히면 실패 로그를 기록한 뒤, 모듈 경계는 유지하고 각 모듈 build 파일에 설정을 직접 적는 방식으로 후퇴합니다.

### 의존성 버전의 출발점

참고 조합은 DroidKaigi 2026 기준 커밋 `300a81bc`입니다. 이 저장소의 원격 main과 같은 커밋이며 2026-10-07에 확인했습니다. 이 버전들은 **후보**이며, 지정 빌드 조합에서 실제 빌드가 성공한 뒤 고정합니다.

| 라이브러리 | 후보 |
| --- | --- |
| Kotlin | 2.4.0 (원문 지정) |
| KSP | 2.3.10 (원문 지정. DroidKaigi는 2.3.9이며 KSP 2.3.x는 Kotlin 버전과 분리됨) |
| Metro | 1.4.5 (1.2.0부터 Kotlin 2.4.0 기준 빌드. DroidKaigi는 1.2.1) |
| Soil | 1.0.0-alpha15 |
| Compose BOM / runtime retain | 2026.09.00 / 1.12.1 (BOM에 포함된 정식판. DroidKaigi는 CMP 1.12.0-alpha03) |
| Navigation 3 | 1.2.0 (DroidKaigi는 1.1.1) |
| Coil 3 | 3.6.3 |
| Timber | 5.0.1 (사용자 선택. 2021-08 이후 새 릴리스 없음) |
| AndroidX App Startup | 1.2.0 (사용자 선택. Timber 초기화 등록) |
| Retrofit / OkHttp | 3.0.0 / 5.5.0 |
| Room | 2.8.5 |
| Robolectric | 4.17 |
| kotlinx.coroutines / serialization | 1.11.0 / 1.11.0 |
| Molecule / Turbine | 2.2.0 / 1.2.1 |

DroidKaigi는 JetBrains Compose Multiplatform 아티팩트를 씁니다. 이 앱은 Android 전용이므로 같은 기능을 제공하는 AndroidX 아티팩트를 우선 찾고, 없거나 맞지 않을 때만 대안을 기록합니다. Retrofit, OkHttp, Room, Robolectric 버전은 공식 릴리스에서 확인합니다.

## 3. API 키와 빌드 구성

- 키 이름은 `local.properties`의 `KAKAO_REST_API_KEY`로 정합니다.
- `app/build.gradle.kts`에서 `local.properties`를 읽어 `BuildConfig.KAKAO_REST_API_KEY`로 주입합니다. AGP 8부터 BuildConfig 생성이 기본으로 꺼져 있으므로 `buildFeatures.buildConfig = true`를 설정합니다.
- 키가 없어도 빌드는 성공해야 합니다. 이때는 빈 문자열을 주입하고, 런타임 검색 시 "API 키가 설정되지 않았습니다" 상태를 보여 줍니다. 즐겨찾기 기능은 키 없이도 동작합니다.
- 키는 인증 interceptor에서만 `Authorization: KakaoAK {key}` 헤더로 붙입니다. 로그·예외 메시지·테스트 fixture에 키를 남기지 않습니다. HTTP 로그는 debug 빌드에서 BASIC 수준만 남기고 `Authorization` 헤더를 가립니다.
- 로그는 Timber로 남깁니다. App Startup의 `TimberInitializer`가 debug 빌드에서만 `Timber.DebugTree`를 심고, release에는 Tree를 심지 않습니다. `Application`은 앱 그래프와 Coil 연결만 맡습니다. OkHttp 로그(`OkHttp` 태그)와 즐겨찾기 쓰기 실패를 Timber로 보냅니다. 선택 이유는 [아키텍처](ARCHITECTURE.md) 6절에 적습니다.
- `local.properties`는 계속 Git에서 제외합니다. 제출 ZIP에는 사용자가 확인한 뒤 포함합니다.

## 4. Kakao 책 검색 API 계약

출처: [Kakao 책 검색 개발 가이드](https://developers.kakao.com/docs/ko/daum-search/dev-guide#search-book). 2026-10-07에 확인했습니다.

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
| `contents` | String | 도서 소개 요약. API가 짧게 잘라 제공하므로 전문을 기대하지 않음 |
| `url` | String | 대체 ID의 재료로 사용 |
| `isbn` | String | ISBN10과 ISBN13이 공백으로 구분되어 오며, 한쪽만 있거나 빈 값일 수 있음 |
| `datetime` | String(ISO 8601) | 출간일. 파싱에 실패하면 정보 없음 |
| `authors` / `translators` | String[] | 비어 있을 수 있음 |
| `publisher` | String | 비어 있을 수 있음 |
| `price` / `sale_price` | Int | 정가 / 판매가. 음수·누락은 정보 없음, 0은 유효 |
| `thumbnail` | String | 비어 있으면 대체 표지 |
| `status` | String | 판매 상태(예: 정상판매, 품절, 절판). 상세에서만 표시 |

- DTO는 모든 필드에 기본값을 두고 `ignoreUnknownKeys = true`로 역직렬화합니다. 필드 누락 때문에 목록 전체가 실패하지 않게 하기 위해서입니다.
- 종료 판정은 `meta.is_end == true`이거나 마지막으로 받은 page가 50인 경우입니다. 받은 문서 개수로 종료 여부를 추정하지 않습니다.
- Soil core의 fetch 계약은 `suspend` 함수일 뿐이라 HTTP 클라이언트를 가리지 않습니다(`QueryKey.fetch: suspend QueryReceiver.() -> T`). 공식 receiver 모듈은 Ktor용만 있지만 필수가 아니며, 이 앱은 Metro로 주입한 Retrofit 서비스를 key 생성자로 받아 fetch에서 호출합니다. Retrofit의 `HttpException`·`IOException`은 그대로 Soil로 전파됩니다.
- 단건 조회 API는 없습니다. 상세는 네트워크 재조회 없이 7절의 스냅샷으로 구성합니다.

## 5. 도서 모델과 ID

`Book`은 화면 계약 모델이며 DTO나 Room Entity와 분리합니다. 필드는 `id`, `isbn10`, `isbn13`, `title`, `authors`, `translators`, `publisher`, `publishedDate(LocalDate?)`, `price(Int?)`, `salePrice(Int?)`, `thumbnailUrl`, `contents`, `url`, `status`입니다.

ID 규칙은 `core:model`의 순수 함수 하나로 구현하고 단위 테스트합니다.

1. `isbn`을 공백으로 나눈 뒤 각 토큰에서 숫자와 `X`만 남깁니다.
2. 길이가 13인 토큰이 있으면 `isbn13:{값}`을 ID로 씁니다.
3. 없으면 길이 10인 토큰으로 `isbn10:{값}`을 씁니다.
4. ISBN이 없으면 `url:{url}`을 씁니다. url도 비어 있으면 `fallback:{title}|{publisher}|{datetime}`을 씁니다.

같은 책이 한 응답에서는 ISBN13으로, 다른 응답에서는 ISBN10으로만 오면 서로 다른 ID가 됩니다. 이 충돌 가능성은 받아들이고 README에 한계로 적습니다. 제목만으로 ID를 만들지 않습니다.

## 6. 화면 사양

UI 가이드를 기준으로 하되 세부 디자인은 Material 3 기본 컴포넌트로 단순하게 구성합니다.

### 공통 도서 카드

- 구성: 표지 썸네일, 제목(최대 2줄), 저자(쉼표로 연결), 출판사, 출간일(`yyyy.MM.dd`), 가격, 즐겨찾기 하트 버튼
- 가격: 정가를 기본으로 표시합니다. 판매가가 유효하고 정가보다 낮으면 할인 판매가를 함께 표시합니다. 가격 필터가 정가 기준이므로, 카드에 보이는 기본 가격과 필터 기준을 일치시키기 위한 결정입니다. 가격 정보가 없으면 "가격 정보 없음"으로 표시합니다.
- 숫자는 `#,###원` 형식으로 씁니다. 저자·출판사·출간일이 비면 해당 줄을 숨기지 않고 "정보 없음"으로 채워 카드 높이를 일정하게 유지합니다.
- 하트 버튼은 해당 도서의 저장이 진행되는 동안만 비활성화합니다. 다른 카드 조작과 화면 이동은 막지 않습니다.
- 카드 전체를 누르면 상세로 이동합니다. 하트 버튼 영역은 이동하지 않습니다.

### 검색 탭

- 상단 검색창의 placeholder는 "검색어를 입력하세요"입니다. 검색 버튼 또는 키보드 검색(IME) 실행 시 입력을 확정합니다.
- 검색창 아래에 현재 정렬 라벨("정확도순" / "발간일순")과 정렬 변경 버튼을 둡니다. 정렬을 바꾸면 현재 확정된 검색어로 1페이지부터 다시 검색합니다.
- 상태별 화면은 다음과 같습니다.
  - 검색 전: "도서를 검색해 보세요"
  - 최초 로딩: 전체 로딩 표시
  - 결과 없음: "검색 결과가 없습니다"
  - 최초 오류: 오류 메시지와 재시도 버튼
  - 추가 로딩: 목록 하단 로딩 표시
  - 추가 오류: 기존 목록을 유지하고 하단에 재시도 버튼
- 빈 검색어를 확정하면 진행 중인 요청과 결과를 정리하고 검색 전 상태로 돌아갑니다.
- 확정할 때 키보드를 내립니다. 직전과 같은 검색어·정렬을 다시 확정하면 새로 요청하지 않고, 오류 상태일 때만 재시도합니다. 당겨서 새로고침은 구현하지 않습니다.
- 마지막 아이템 근처(남은 아이템 5개 이하)가 보이면 다음 페이지를 요청합니다. 보이는 아이템 수에 최소 문턱은 두지 않습니다.
- 여러 페이지를 합칠 때 같은 ID가 다시 나오면 처음 나온 항목만 남깁니다. 목록의 `key`는 도서 ID이므로, 중복을 제거하지 않으면 Lazy 목록이 크래시합니다. 다음 페이지 번호와 종료 여부는 중복 제거 전의 응답 기준으로 계산합니다.
- 검색 결과 Query의 `staleTime`은 5분으로 둡니다. 탭 전환이나 상세에서 돌아올 때 같은 조건을 다시 요청하지 않도록 하기 위해서입니다. 정확한 옵션 이름은 Soil 버전에서 확인합니다.
- 검색 루트가 백스택에 있는 동안에는 확정 조건의 쿼리를 Soil에 붙여 둡니다. Soil은 구독이 모두 끊기고 `keepAliveTime`(기본 5초)이 지나면 쿼리를 내리면서 성공 상태만 캐시에 남깁니다. 그래서 추가 로딩이 실패한 채 즐겨찾기 탭에 5초 넘게 머물다 돌아오면 목록이 지워지고 전체 오류가 보였습니다(실기기 QA에서 발견). 검색 Presenter가 엔트리 범위로 retain한 참조로 쿼리를 붙잡고, 조건이 바뀌거나 검색어를 비우거나 엔트리가 은퇴하면 놓습니다. `SearchQueryRetentionTest`로 확인합니다.

### 즐겨찾기 탭

- 상단 검색창의 placeholder는 "제목 또는 저자 검색"입니다. 검색 버튼이나 IME로 확정합니다.
- 로컬 검색 규칙: 앞뒤 공백을 제거한 질의어가 제목 또는 저자 중 하나에 부분 일치하면 표시합니다. 영문은 대소문자를 구분하지 않습니다. 빈 질의어는 전체를 표시합니다.
- 상단에 현재 정렬 라벨("제목 오름차순" / "제목 내림차순")과 필터 버튼, 정렬 버튼을 둡니다. 기본 정렬은 제목 오름차순입니다.
- 정렬 규칙: 제목을 앞뒤 공백 제거 후 `java.text.Collator(Locale.KOREAN)`으로 비교합니다. 제목이 같으면 ID로 비교해 순서를 고정합니다. SQLite 기본 정렬은 쓰지 않습니다.
- 가격 필터는 바텀시트에서 최소·최대 정가를 입력하고 "적용"으로 확정합니다. 세부 규칙은 [REQUIREMENTS](REQUIREMENTS.md)의 제품 정책 2번을 따릅니다. 입력 칸은 숫자 키패드를 띄우고 숫자만 받으며, 최대 9자리로 제한해 Int 범위를 넘지 않게 합니다. 필터가 적용 중이면 필터 버튼에 표시를 하고 "초기화"를 제공합니다.
- 이 탭에서 하트를 눌러 즐겨찾기를 해제하면, 그 카드는 Room 반영과 동시에 목록에서 사라집니다. 실행 취소는 구현하지 않습니다.
- 검색·정렬·필터는 Room에서 읽은 전체 즐겨찾기 목록을 메모리에서 순수 함수로 처리합니다. 개인 즐겨찾기는 많아야 수백 건이라고 보고, 한국어 정렬과 테스트 용이성을 우선했습니다. 수천 건 이상을 다루려면 DB 질의로 옮겨야 하며, 이 점을 README 한계에 적습니다.
- 빈 상태는 두 가지로 나눕니다. 즐겨찾기가 하나도 없으면 "즐겨찾기한 도서가 없습니다", 조건에 맞는 도서가 없으면 "조건에 맞는 도서가 없습니다"를 표시합니다.

### 상세 화면

- 상단 앱바에 뒤로 가기, 제목, 즐겨찾기 하트를 둡니다. 상세에서는 하단 탭바를 숨깁니다(UI 가이드와 동일).
- 본문에는 표지(큰 이미지), 제목, 저자, 역자(있을 때만), 출판사, 출간일, ISBN, 정상가, 할인가, 판매 상태(정상판매가 아닐 때만), 책 소개(`contents`)를 표시합니다.
- 할인가가 없거나 정가와 같으면 할인가 줄은 "할인 없음"으로 표시합니다.
- 넓은 화면에서는 본문 최대 너비를 제한하고 가운데 정렬합니다.

### 공통 표현

- 테마는 Material 3 기본 테마입니다. 시스템 설정에 따라 라이트/다크를 따르고, 화면 캡처의 일관성을 위해 dynamic color는 쓰지 않습니다.
- 표지 이미지와 하트 버튼에는 `contentDescription`을 붙입니다. 하트 버튼은 현재 상태에 따라 "즐겨찾기 추가" 또는 "즐겨찾기 해제"로 읽히게 합니다.
- 즐겨찾기 쓰기가 실패하면 스낵바로 알립니다. 성공했을 때는 하트 상태가 바뀌는 것으로 충분하므로 별도 메시지를 띄우지 않습니다.

### 저장·복원하는 화면 상태

| 상태 | 저장 방식 | 시스템 프로세스 재생성 후 |
| --- | --- | --- |
| 검색 입력값, 확정 검색어, 정렬 | `rememberSaveable` | 확정 검색어가 있으면 1페이지부터 다시 검색 |
| 즐겨찾기 입력값, 확정 검색어, 정렬, 가격 필터 | `rememberSaveable` | Room 목록에 그대로 다시 적용 |
| 탭과 상세 엔트리 | `rememberNavBackStack` | 그대로 복원. 상세는 스냅샷으로 즉시 표시 |
| 목록 스크롤 위치 | 조회 조건(확정 검색어·정렬, 즐겨찾기는 가격 필터 포함)을 key로 한 `rememberSaveable(saver = LazyGridState.Saver)`. 조건이 바뀌면 맨 위에서 시작하고, 하트 해제처럼 조건이 같으면 위치 유지 | 검색 탭은 1페이지 범위까지만 의미가 있음 |
| 검색 결과 페이지, 진행 중 쓰기 ID | Soil 캐시 / retain | 소멸 |

## 7. 내비게이션과 상세 데이터

### NavKey

모든 키는 `@Serializable`이며 `rememberNavBackStack`으로 저장·복원합니다.

- `SearchNavKey`, `FavoritesNavKey`: 탭 루트
- `BookDetailNavKey(book: BookSnapshot)`: 상세

`BookSnapshot`은 상세를 그리는 데 필요한 도서 필드를 담은 직렬화 가능 모델입니다. 엔트리의 `contentKey`는 `book.id`로 지정해, 같은 도서의 상세는 같은 retain 저장소를 씁니다.

### 상세에 스냅샷을 넣는 이유

이전 문서는 "bookId만 저장하고 미저장 도서는 재조회"를 정책으로 두었습니다. 그러나 Kakao API에는 단건 조회가 없습니다. 재조회하려면 ISBN 검색을 이용해야 하고, ISBN이 없는 도서는 재조회 자체가 불가능합니다. 또한 지하철 같은 불안정한 네트워크에서 프로세스가 재생성되면 상세가 곧바로 오류 화면이 됩니다.

그래서 카드를 누른 시점의 도서 스냅샷을 NavKey에 넣습니다. 도서 한 권의 텍스트는 수 KB 이하이고 백스택에 쌓이는 상세도 소수이므로 saved state 크기 위험이 낮습니다. 검색 결과 목록 전체는 여전히 saved state에 넣지 않습니다.

- 상세의 표시 데이터: Room에 같은 ID가 있으면 Room 값을, 없으면 NavKey의 스냅샷을 씁니다. 즐겨찾기 여부는 항상 Room 기준입니다.
- 상세에서 즐겨찾기를 해제하면 Room 행이 사라지지만, 화면은 스냅샷으로 계속 같은 정보를 보여 줍니다. 해제 후 화면이 비는 문제를 이렇게 해결합니다.
- 상세는 네트워크를 호출하지 않으므로 오프라인과 프로세스 재생성 후에도 바로 표시됩니다. 대가는 가격 같은 정보가 진입 시점 값으로 고정된다는 점이며, README 한계에 적습니다.

### 백스택 규칙

DroidKaigi 2026의 탭 처리(`remove` + `add`)에는 같은 탭 재선택 시 백스택 순서가 뒤집히는 버그([DroidKaigi/conference-app-2026#344](https://github.com/DroidKaigi/conference-app-2026/issues/344))와 상세 중복 진입 버그([DroidKaigi/conference-app-2026#226](https://github.com/DroidKaigi/conference-app-2026/issues/226))가 열려 있습니다. 따라서 코드를 그대로 옮기지 않고 아래 규칙을 직접 구현해 테스트합니다.

백스택은 하나만 둡니다. 형태는 항상 `[비활성 탭 루트(있으면), 활성 탭 루트, 상세…]`이고 시작은 `[SearchNavKey]`입니다.

| 동작 | 결과 |
| --- | --- |
| 다른 탭 선택 | 현재 탭 위의 상세를 모두 pop하고, 대상 탭 루트를 맨 위로 옮김(없으면 추가). 두 탭 루트는 pop하지 않으므로 목록 상태가 retain으로 유지됨 |
| 같은 탭 재선택 | 상세가 있으면 탭 루트까지 pop. 루트에서는 아무것도 하지 않음 |
| 카드 선택 | 맨 위가 상세면 무시하고, 아니면 상세를 push. 상세에는 다른 도서로 가는 경로가 없으므로, 두 카드를 동시에 누르거나 전환 중에 다른 카드를 눌러도 상세는 하나만 쌓임 |
| 뒤로 가기(상세) | 상세 pop |
| 뒤로 가기(즐겨찾기 루트) | 검색 탭 선택과 같은 동작(즐겨찾기 루트는 유지) |
| 뒤로 가기(검색 루트) | 앱 종료(Activity finish) |

탭을 바꾸면 이전 탭의 상세는 정리됩니다. 탭마다 독립된 상세 스택을 유지하는 방식보다 단순하고, retain 정리 시점이 명확하기 때문입니다. 탭 루트를 옮길 때 retain decorator가 이를 pop으로 처리하지 않는지는 Compose 테스트로 확인합니다.

## 8. 즐겨찾기 쓰기 경로

### 확인한 Soil 사실

Soil 1.0.0-alpha15(원격 main `000c624`와 동일, 2026-10-07 확인)의 소스에서 확인한 내용입니다.

- `MutationRef.mutate`는 랑데부 채널로 명령을 보냅니다. actor가 앞 명령을 처리 중이면 send가 대기하고, 대기 중에 호출 코루틴이 취소되면 그 명령은 전달되지 않습니다.
- actor는 명령을 하나씩 처리하므로 같은 Mutation key 안에서는 순서가 보장됩니다.
- 마지막 구독자가 사라지면 `keepAliveTime`(기본 5초) 뒤 Mutation의 scope가 취소됩니다. 따라서 Mutation 안에서 직접 DB 쓰기를 하면, 화면 pop 이후 완료가 보장되는 시간은 약 5초뿐입니다.

### 결정

- `FavoriteWriter`는 Metro 앱 그래프의 싱글턴입니다. `setFavorite(book: Book, favorite: Boolean)`는 앱 수준 `ApplicationScope`에서 `async { mutex.withLock { upsert 또는 delete } }.await()`로 실행합니다. `ApplicationScope`는 `SupervisorJob` 기반이며 로깅용 예외 처리기를 둡니다. `GlobalScope`는 쓰지 않습니다.
- Soil Mutation key의 `mutate`는 이 `setFavorite`를 호출합니다. 화면이 pop되어 호출 코루틴이 취소되어도, 이미 시작한 DB 쓰기는 앱 scope에서 끝까지 실행됩니다.
- 하나의 `Mutex`가 모든 즐겨찾기 쓰기를 직렬화합니다. 화면이 달라도 도서별 명령 순서가 유지됩니다. 명령은 토글이 아니라 목표 상태이므로 같은 명령이 반복되어도 결과가 같습니다.
- Mutation key는 화면별로 다른 ID를 씁니다. DroidKaigi 2026의 extraTag 방식을 따른 것으로, 한 화면의 Mutation 상태가 다른 화면에 섞이지 않게 합니다.
- Presenter는 진행 중인 도서 ID 집합을 retain 상태로 들고 있다가 해당 카드의 버튼만 비활성화합니다. 실패하면 스낵바로 알리고, 상태는 Room 값을 그대로 유지합니다.
- 알려진 한계가 하나 있습니다. 같은 화면에서 A 도서 쓰기가 진행 중일 때 B 도서를 누르고 수 밀리초 안에 화면을 떠나면, B 명령이 actor에 전달되기 전에 취소될 수 있습니다. 실제 쓰기 시간이 짧아 발생 구간이 작으므로 테스트로 동작을 기록하고 README 한계에 적습니다.
- 프로세스가 종료되면 아직 끝나지 않은 쓰기의 완료는 보장하지 않습니다.

### 읽기 경로

- `FavoriteIdsSubscriptionKey`: `Flow<Set<String>>`. 검색 카드의 하트 표시에 씁니다.
- `FavoriteBooksSubscriptionKey`: `Flow<List<Book>>`. 즐겨찾기 탭에 씁니다.
- `FavoriteBookSubscriptionKey(id)`: `Flow<Book?>`. 상세에 씁니다.

Room Entity는 `id`(PK), 원본 `isbn`, 5절의 도서 필드, `savedAt`으로 구성합니다. 목록 필드는 TypeConverter로 저장합니다. DB 버전은 1이고 `exportSchema = true`로 스키마 JSON을 저장소에 둡니다.

## 9. 네트워크 오류와 재시도

- OkHttp 타임아웃은 연결 10초, 읽기 15초로 둡니다.
- 오류 분류와 처리는 다음과 같습니다.

| 원인 | 분류 | 자동 재시도 | 화면 안내 |
| --- | --- | --- | --- |
| `IOException`(타임아웃 포함) | 네트워크 | 함 | 연결 상태 확인 후 재시도 |
| HTTP 5xx, 429 | 일시 오류 | 함 | 잠시 후 재시도 |
| HTTP 401, 403 | 인증 | 안 함 | API 키 확인 안내 |
| 기타 4xx | 요청 | 안 함 | 일반 오류 |
| 직렬화 실패 | 응답 형식 | 안 함 | 일반 오류 |
| `CancellationException` | 취소 | 안 함 | 표시하지 않고 재전파 |

- 자동 재시도는 Soil의 retry 옵션에 위 조건을 판정하는 함수를 넘겨 구현합니다. 횟수와 간격은 Soil 기본값에서 시작합니다.
- 화면의 재시도 버튼은 같은 key의 같은 페이지를 다시 요청합니다.
- 이미지용 클라이언트는 인증 interceptor가 없는 기본 `OkHttpClient`를 쓰고, API 클라이언트는 여기에 `newBuilder()`로 인증 interceptor만 더해 만듭니다.

## 10. 적응형 레이아웃

- 탐색 UI는 `NavigationSuiteScaffold`를 씁니다. compact에서는 하단 바, medium·expanded에서는 내비게이션 레일로 바뀝니다. 상세에서는 탐색 UI를 숨깁니다.
- 목록은 `LazyVerticalGrid(GridCells.Adaptive(minSize = 360.dp))`로 구성합니다. compact에서는 1열이고 넓은 창에서는 2~3열이 됩니다. 검색창과 필터 영역은 최대 너비를 제한합니다.
- list-detail 2-pane은 구현하지 않습니다(제품 정책 5번). 이유와 영향은 README에 적습니다.
- 회전·창 크기 변경 후에도 입력값, 확정 조건, 스크롤 위치, 상세 엔트리가 유지되는지 확인합니다.

## 11. 테스트 환경

| 대상 | 도구 | 실행 위치 |
| --- | --- | --- |
| ID·가격·필터·정렬·날짜 규칙 | JUnit4 + kotlin.test | JVM 단위 테스트 |
| Retrofit API | OkHttp MockWebServer | JVM 단위 테스트 |
| Soil key와 페이징 | soil-query-test(`TestSwrClientPlus`, Fake key) + coroutines-test | JVM 단위 테스트 |
| Presenter | Molecule + Turbine + coroutines-test | JVM 단위 테스트 |
| Room | Robolectric + in-memory DB | JVM 단위 테스트 |
| Compose 내비게이션·적응형·retain 수명 | Robolectric Compose UI 테스트 | JVM 단위 테스트 |
| 프로세스 재생성·폴더블·실제 API | 에뮬레이터 수동 확인 | 기록으로 남김 |

Robolectric이 SDK 37을 지원하지 않으면 테스트에서만 `@Config(sdk = [지원 최대 버전])`을 쓰고, 이 제약을 기록합니다. 실행 명령은 기본적으로 `./gradlew testDebugUnitTest`입니다. 실제 task 이름은 프로젝트 생성 후 확인합니다.

## 12. 제출 패키지

- 원문에 파일명 규칙이 없으므로 `book-search-android.zip`, `book-search-android-debug.apk`로 합니다.
- ZIP에는 `.git`과 `local.properties`를 포함하고, 모든 `build/`, `.gradle/`, `.idea/`, `.kotlin/` 디렉터리를 제외합니다. ZIP 생성 스크립트를 저장소에 두고, 내용 점검 결과를 기록합니다.
- APK는 `./gradlew assembleDebug` 산출물을 씁니다. 사용자의 키가 들어간 상태로 빌드합니다.
- ZIP을 새 디렉터리에 풀어 `./gradlew assembleDebug`가 다시 성공하는지 확인합니다.
- 메일 발송, 실제 키 확인, 최종 첨부는 사용자가 합니다.

## 13. 구현 중 검증으로 확정할 항목

아래 항목은 결정이 아니라 사실 확인이 필요합니다. 결과를 이 문서와 README에 반영합니다.

1. 지정 빌드 조합에서 2절의 의존성 후보가 함께 빌드되는지
2. AndroidX에서 retain decorator에 필요한 `RetainedValuesStoreRegistry` 계열 API를 제공하는 버전
3. Soil 재시도 옵션에서 재시도 여부를 판정하는 함수의 정확한 이름과 형태
4. Robolectric의 지원 SDK 최댓값
5. `NavigationSuiteScaffold`와 Navigation 3를 함께 쓸 때 상세에서 탐색 UI를 숨기는 방법

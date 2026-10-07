# 아키텍처

기준일: 2026-10-08. 구현을 마친 구조를 설명합니다(커밋 `71cdf67` 기준). 자동 테스트로 확인한 범위와 아직 수동으로 확인하지 않은 범위는 10절과 [REQUIREMENTS](REQUIREMENTS.md)의 검증 기록에 나눠 적습니다. 전체 요구사항은 [REQUIREMENTS](REQUIREMENTS.md), 화면·API·백스택 등 코드로 옮길 세부 계약은 [SPEC](SPEC.md), 일정·참고 코드·상세 검증 시나리오는 [STRATEGY](STRATEGY.md), 판단 과정은 [AI_USAGE](AI_USAGE.md)에 기록합니다.

## 1. 구성과 선택 상태

| 영역 | 기술 | 상태와 역할 |
| --- | --- | --- |
| 화면 | Jetpack Compose | 필수. 입력·표시·사용자 이벤트 전달 |
| 화면 모델 | Root + 얇은 @Composable Presenter | 현재 권장안. Root의 데이터 관찰·boundary, Presenter의 준비된 데이터·로컬 상태·mutation |
| 수명·이동 | retain + Navigation 3 | 사용자 선택. 엔트리별 유지·제거 및 백스택 |
| 데이터 접근 | Soil typed-key | 사용자 선택. 원격 Query / InfiniteQuery, 로컬 Subscription / Mutation |
| 페이지네이션 | Soil InfiniteQuery | 사용자 선택. 요청 key와 페이지 메타데이터 연결 |
| 원격 통신 | Retrofit + OkHttp + kotlinx.serialization | 사용자 선택. 요청·응답·직렬화 |
| 로컬 저장 | Room + Flow | 사용자 선택. 즐겨찾기의 단일 상태 기준 |
| 의존성 연결 | Metro | 사용자 선택. 필요한 객체와 최소 그래프 조립 |
| 표지 이미지 | Coil | 사용자 선택. Coil 3.6.3의 coil-compose + coil-network-okhttp |
| 로그 | Timber | 사용자 선택. debug 빌드에서만 출력 |

Android 전용입니다. KMP source set, expect/actual, 플랫폼 추상화, Circuit은 도입하지 않습니다. 참고 앱의 전체 DI 그래프나 프로젝트 구성을 복제하지 않습니다. 기술 수가 많다고 요구사항 충족이나 좋은 설계가 보장되지는 않으므로, 각 선택이 맡는 책임을 테스트로 확인했습니다(7절).

## 2. 의존성과 데이터 흐름

```text
Screen Root
  → Soil Query / InfiniteQuery → Retrofit → OkHttp → Kakao API
  → Soil Subscription → Room DAO Flow
  → 로딩·오류 boundary와 준비된 데이터
      → 얇은 Composable Presenter → Compose Screen
          → 사용자 명령 → Soil Mutation → Room DAO 쓰기

Navigation 3 + retain: 엔트리 상태의 유지·제거
Metro: API, DB, key의 의존성, 이미지 로더 등 객체 조립
Coil: Screen의 표지 요청 → 이미지 전용 네트워크 설정과 캐시
```

Presenter는 UI 모델을 만들고 이벤트를 데이터 명령으로 연결합니다. 화면은 네트워크 DTO나 Room Entity를 직접 조작하지 않습니다. typed-key가 이미 담당하는 호출을 그대로 전달하는 Repository는 추가하지 않습니다. 이는 이번 프로젝트에서 선택한 책임 분리이며, Soil이 모든 앱에서 Repository를 금지한다는 뜻은 아닙니다. 도서 ID 정규화·가격 정책 같은 독립적인 규칙은 일반 Kotlin 함수/타입으로 분리할 수 있습니다. 순수 Kotlin 테스트만을 위해 전체 상태를 가진 StateFlow 객체를 추가로 중복 도입하지 않습니다.

로컬은 선택한 Subscription/Mutation→Room으로 진행하며, Room의 단일 상태 기준과 구독 수명을 검증합니다. DroidKaigi 2026의 Root가 query/subscription과 로딩·오류 boundary를 조립하고 Presenter가 준비된 데이터·로컬 상태·mutation을 다루는 패턴을 얇게 적용하는 안을 권장합니다. 모든 읽기 책임을 Presenter에 몰아넣거나 참고 앱의 전체 프레임워크를 가져오지 않습니다.

Metro는 의존성을 연결하는 도구이며, 화면 상태나 데이터의 단일 기준을 대신하지 않습니다. 앱 수준에서 재사용하는 API·DB·이미지 로더와 화면 엔트리의 수명을 구분합니다. 그래프는 앱 그래프 하나(`AppGraph`, `AppScope`)와 화면별 `GraphExtension` 세 개(`SearchScreenGraph`, `FavoritesScreenGraph`, `BookDetailScreenGraph`)입니다. `AppGraph`가 각 화면 그래프의 `Factory`를 구현하고, 앱 그래프는 Retrofit·OkHttp·Room·`SwrCachePlus`·Coil `ImageLoader`·`FavoriteWriter`·`ApplicationCoroutineScope`를 싱글턴으로 둡니다.

### Metro factory와 retain 연결

DroidKaigi 2026의 [TimetableScreenGraph](https://github.com/DroidKaigi/conference-app-2026/blob/main/feature/sessions/src/commonMain/kotlin/io/github/droidkaigi/confsched/feature/sessions/timetable/TimetableScreenGraph.kt)와 [TimetableNavEntryProvider](https://github.com/DroidKaigi/conference-app-2026/blob/main/feature/sessions/src/commonMain/kotlin/io/github/droidkaigi/confsched/feature/sessions/timetable/TimetableNavEntryProvider.kt)를 참고했습니다. 선언한 `@GraphExtension.Factory`의 구현은 Metro 컴파일러가 생성하고, 내비게이션 엔트리에서는 factory로 만든 그래프를 retain으로 유지합니다.

```kotlin
entry<BookDetailNavKey>(clazzContentKey = { it.book.id }) { key ->
    val graph = retain { graphFactory.createBookDetailScreenGraph() }
    BookDetailScreenRoot(context = graph.bookDetailScreenContext, snapshot = key.book, onBack = onBack)
}
```

화면 그래프는 그 화면이 쓰는 Soil key와 화면별 Mutation key(`favoriteMutationKeyFactory.create("search")` 등)를 담은 `XxxScreenContext` 하나를 노출합니다. 엔트리가 백스택에 있는 동안 같은 key 인스턴스를 쓰고, 엔트리가 pop되면 그래프도 함께 버려집니다.

2025 예제의 커스텀 KSP 생성기는 이미 있는 factory를 감싼 remember 편의 함수를 만드는 별도 장치였으며, factory 구현 자체를 생성한 것이 아닙니다. 이번에는 자체 코드 생성기·컴파일러 플러그인을 만들지 않았습니다. Metro 컴파일러 플러그인은 모든 모듈에, Room용 KSP는 `core:database`에만 적용했고 두 가지가 함께 빌드되는 것을 확인했습니다.

### 요구사항과 현재 제품 정책의 경계

로컬 저장·금액 필터·불안정한 네트워크 대응·태블릿/폴더블 지원은 이미 명시된 요구입니다. 상세한 UX 선택 다섯 가지와 구현 계약은 [요구사항 문서](REQUIREMENTS.md)의 분류를 따릅니다. 특히 태블릿·폴더블 대응은 필수이고 list-detail 2-pane은 선택입니다. 아래 ID·페이징·쓰기 순서·크기 변경 상태 보존 설명은 구현·검증 책임이며 사용자 재승인 요청이 아닙니다. 다섯 제품 정책은 사용자 지시에 따라 도서 서비스 사례와 기술 지침을 참고해 정리했으며, [전략 문서](STRATEGY.md)에서 관찰 사실과 설계 선택을 구분합니다.

## 3. 상태 소유권

| 상태 | 소유·관찰 기준 | 복원과 주의점 |
| --- | --- | --- |
| 검색어, 정렬, 가격 필터, 선택 탭 | Presenter의 입력 상태 + 최소 saveable 값 | 시스템 프로세스 재생성 후 조건 복원·재조회 |
| 확정 검색 조건 | query/sort를 포함한 Soil key | 편집 중 입력과 분리. 다른 검색 결과 혼입 방지 |
| 검색 페이지·원격 로딩·오류 | Soil의 해당 key 상태 | 메모리 캐시가 기본 범위. 프로세스 종료 후 유지 보장 없음 |
| 즐겨찾기 도서·선택 여부 | Room, Subscription으로 Flow 관찰 | 화면 간 단일 상태 기준. 재실행·오프라인 복원 |
| 저장/삭제 진행·오류 | 해당 Mutation/명령 상태 | 완료 전 성공 표시 금지. Room 관찰 결과로 확정 |
| 선택한 상세 경로 | 도서 한 권(`Book`)을 담은 저장 가능한 내비게이션 키 `BookDetailNavKey` | 전체 목록은 백스택에 넣지 않음. 표시 데이터는 Room 값 우선, 없으면 스냅샷 |
| 화면 엔트리에서 유지할 객체 | retain 엔트리 범위(화면 그래프, `FavoriteToggler`) | pop 시 정리. 프로세스 종료 저장과는 별개 |
| 표지 이미지 | Coil 이미지 로더·캐시 | 캐시 미존재·실패 시 대체 UI |

[사용자의 retain 글(2025-10-21)](https://velog.io/@mraz3068/Compose-Retain-API)도 객체 유지와 프로세스 종료 복원을 구분하는 참고 자료입니다. 당시 preview 용어나 버전을 현재 API 계약으로 그대로 사용하지 않고, 선택한 버전의 문서와 실제 호스트로 확인합니다.

검색 결과의 즐겨찾기 표시는 원격 도서와 Room의 즐겨찾기 ID를 결합합니다. Soil 캐시에 별도의 즐겨찾기 boolean을 두고 DB와 경쟁시키지 않습니다.

상세는 진입 시점의 도서 스냅샷을 내비게이션 키에 담습니다. Kakao API에는 단건 조회가 없고, ISBN이 없는 도서는 다시 찾을 수 없기 때문입니다. 표시는 Room 값을 우선하고, 없으면 스냅샷을 씁니다. 즐겨찾기 여부는 항상 Room을 기준으로 합니다. 그래서 상세에서 즐겨찾기를 해제해도 화면이 비지 않고, 프로세스 재생성 후에도 네트워크 없이 상세가 다시 표시됩니다. 자동 즐겨찾기 저장은 하지 않습니다. 전체 결과를 saveable로 저장하지 않고, 강제 종료 뒤 콜드 스타트에 같은 복원을 보장하지 않습니다. retain은 이 문제를 자동 해결하지 않습니다.

### retain 엔트리 수명

Navigation 3 1.2.0에는 retain용 엔트리 decorator가 없어 `RetainedValuesStoreNavEntryDecorator`를 직접 만들었습니다.

- `retainRetainedValuesStoreRegistry()`로 레지스트리를 만들고, 엔트리를 그릴 때 `registry.LocalRetainedValuesStoreProvider(entry.contentKey)`로 감쌉니다. 엔트리마다 retain 저장소가 따로 생깁니다.
- 엔트리가 백스택에서 빠지면(`onPop`) `registry.clearChild(contentKey)`로 그 저장소를 비웁니다. 이때 retain한 객체의 `RetainObserver.onRetired`가 불립니다.
- `BookSearchNavHost`는 saveable·retain decorator를 백스택 전체에 적용하고, `NavDisplay`에는 활성 탭 루트부터의 엔트리만 넘깁니다. 비활성 탭 루트는 백스택에 남아 있으므로 pop되지 않고 retain 값이 유지됩니다.
- 창 너비가 840dp 이상이면 `ListDetailSceneStrategy`가 `[목록 루트, 상세]`를 한 장면에 나란히 그립니다. 엔트리 metadata로 목록·상세 역할을 표시하고, 장면 key를 목록 엔트리로 두어 상세를 열고 닫아도 목록을 다시 그리지 않습니다. 다른 도서로 상세를 바꾸면 이전 상세 엔트리가 pop되어 그 retain 값만 정리됩니다([SPEC](SPEC.md) 10절).

`FavoriteToggler`는 retain으로 유지되는 화면별 즐겨찾기 진행 상태입니다. 진행 중인 도서 ID와 실패 여부를 Compose 상태로 들고, `onRetired`·`onUnused`에서 자기 scope만 취소합니다. 이미 시작한 DB 쓰기는 앱 범위의 `FavoriteWriter`가 끝까지 실행하므로, 쓰기 도중 상세를 닫아도 쓰기는 완료되고 실패 알림만 사라집니다.

### 구성 변경과 프로세스 재생성 대응

retain은 구성 변경(회전, 다크 모드, 창 크기 변경)에서 객체를 유지할 뿐이고, 프로세스가 죽으면 남지 않습니다. 그래서 상태마다 성격에 맞는 수단을 따로 썼습니다. Manifest에 `configChanges`를 선언해 Activity 재생성을 막는 방식은 쓰지 않았습니다. 재생성 경로를 그대로 두어야 회전 확인이 프로세스 재생성 복원과 같은 코드를 지나기 때문입니다.

| 상태 | 수단 | 구성 변경 | 프로세스 재생성 |
| --- | --- | --- | --- |
| 백스택(탭 루트, 상세) | `rememberNavBackStack` + `@Serializable` NavKey. 상세 키에는 진입 시점의 `Book` 스냅샷을 담음 | 유지 | 복원. 상세는 네트워크 없이 다시 표시 |
| 엔트리별 화면 상태 | `rememberSaveableStateHolderNavEntryDecorator`로 엔트리마다 saveable 범위를 나눔 | 유지 | 복원 |
| 검색어 입력·확정 검색어·정렬, 즐겨찾기 검색어·정렬·가격 범위 | Presenter의 `rememberSaveable` | 유지 | 복원. 검색 결과는 조건으로 다시 요청 |
| 가격 필터 시트 열림 여부와 입력 중인 금액 | 화면의 `rememberSaveable` | 유지 | 복원 |
| 목록 스크롤 위치 | 조회 조건을 key로 둔 `rememberSaveable(..., saver = LazyGridState.Saver)` | 유지 | 복원. 조건이 바뀌면 맨 위에서 시작 |
| 화면 그래프, 즐겨찾기 진행 상태(`FavoriteToggler`), 검색 쿼리 고정(`SearchQueryHolder`) | `retain` + 직접 만든 `RetainedValuesStoreNavEntryDecorator` | 같은 객체 유지 | 남지 않음. 새로 만듦 |
| 검색 결과 페이지 | 앱 그래프 싱글턴의 Soil `SwrCachePlus` 메모리 캐시(`staleTime` 5분) | 유지. 다시 요청하지 않음 | 남지 않음. 조건으로 다시 요청 |
| 즐겨찾기 | Room(단일 기준), Subscription으로 관찰 | 유지 | 디스크에서 그대로 읽음 |
| 진행 중인 즐겨찾기 쓰기 | 앱 범위 `FavoriteWriter`(`ApplicationScope` + `Mutex`) | 화면이 사라져도 끝까지 실행 | 미완료 쓰기는 보장하지 않음 |
| 탐색 UI 형태(하단 바/레일), 그리드 열 수, 1-pane/2-pane | `currentWindowAdaptiveInfo()`와 `GridCells.Adaptive`를 매 구성마다 다시 계산. 2-pane 여부에 따라 `NavDisplay`의 장면 전략만 바꾸고 백스택은 그대로 둠 | 새 크기에 맞춰 바뀜. 열린 상세는 유지 | 같음 |
| 키보드 표시 중 레이아웃 | Manifest `windowSoftInputMode="adjustResize"` | 해당 없음 | 해당 없음 |

자동 테스트는 `BookSearchNavHostTest`의 "구성 변경으로 Activity가 다시 만들어져도 백스택과 retain 값이 유지된다"입니다. 실기기 확인은 [QA](QA.md)의 TC-41·68(회전, 다크 모드, 창 크기 변경)과 TC-54·70(`am kill` 후 복원)에 있습니다.

## 4. 핵심 동작

### 검색과 다음 페이지

1. 검색 버튼/IME 제출로 입력을 확정하고 정규화한 query/sort key를 만듭니다.
2. InfiniteQuery의 첫 요청은 page=1, size=20입니다.
3. 응답의 도서 목록과 `meta.is_end` 등 페이지 메타데이터를 보존합니다.
4. 성공한 페이지 뒤에만 다음 페이지로 진행합니다. is_end=true면 20개가 반환되어도 종료합니다.
5. 추가 로딩 실패 시 기존 목록을 보존하고 동일 조건·페이지 재시도를 제공합니다.
6. ISBN 기반 중복 제거 후 목록 길이로 다음 페이지나 종료 여부를 다시 계산하지 않습니다.

Query ID는 정규화한 검색어와 정렬로 만들고, Presenter는 `rememberInfiniteQuery`를 `key(queryKey.id)` 그룹 안에서 호출합니다. Soil compose의 기본 수집이 key 없는 `collectAsState`라서, 그룹을 나누지 않으면 조건이 바뀐 첫 프레임에 이전 조건의 결과가 보였기 때문입니다. 다음 페이지 요청은 현재 상태가 성공이고 진행 중인 요청이 없으며 다음 페이지 값이 있을 때만 보냅니다. 별도의 커스텀 페이지 상태는 두지 않았습니다.

정렬 변경, 빈 검색어 초기화, 중복 ID, is_end, 추가 로딩 실패 후 재시도는 `SearchPresenterTest`와 `SearchBooksQueryKeyTest`로 확인했습니다. 검색어를 빠르게 바꾸거나, 첫 페이지를 받기 전에 정렬을 바꾸거나 빈 검색어로 초기화해 늦은 응답이 도착하는 경우도 `SearchPresenterTest`로 재현해 확인했습니다.

### 즐겨찾기 쓰기와 관찰

아래는 선택한 Subscription/Mutation 구성의 흐름입니다.

1. 화면이 `setFavorite(book, favorite)`처럼 도서와 목표 즐겨찾기 상태를 명령으로 전달합니다. 저장(upsert)에는 도서 정보 전체가 필요하므로 ID만 넘기지 않습니다.
2. Mutation이 앱 그래프 싱글턴 `FavoriteWriter`를 호출하고, `FavoriteWriter`가 Room에 저장/삭제합니다. 해당 도서 버튼만 비활성화하고 명령 순서를 보장합니다.
3. Subscription이 Room Flow 변경을 관찰합니다.
4. 검색·즐겨찾기·상세가 동일한 확정 상태를 반영합니다.
5. 쓰기 실패 시 성공 안내를 하지 않고 기존 확정 상태를 유지합니다.

화면 이동은 허용하고 짧은 DB 쓰기는 화면 밖의 명시적으로 관리되는 scope에서 완료합니다. Soil Mutation의 scope는 마지막 구독자가 사라지고 keepAliveTime(기본 5초)이 지나면 취소됩니다. 그래서 실제 쓰기는 Metro 앱 그래프가 소유한 `ApplicationScope`(SupervisorJob)에서 `async`로 시작하고, 하나의 `Mutex`로 직렬화합니다. Mutation은 그 결과를 기다리기만 합니다. 이 범위의 소유자·종료·오류 처리와 명령 순서를 테스트합니다. 프로세스 종료 시 미완료 쓰기까지 완료된다고 보장하지 않습니다. 세부 근거와 알려진 한계는 [SPEC](SPEC.md) 8절에 있습니다.

### Retrofit 연결과 재시도 경계

공식 모듈 목록에는 Ktor receiver가 있지만 Retrofit 전용 모듈은 없습니다. 범용 InfiniteQuery key의 suspend fetch에서 주입된 Retrofit suspend API를 호출하는 것으로 충분했고, 전용 모듈이나 커스텀 어댑터는 만들지 않았습니다. 지정 버전 조합에서 빌드와 테스트가 통과했습니다.

기본 재시도 정책의 `Retryable` / `canRetry` 조건에 일반 Retrofit `HttpException`이나 `IOException`이 자동으로 들어간다고 가정하지 않습니다. key의 `onConfigureOptions`에서 `shouldRetry = { error -> error.toErrorKind().isRetryable }`를 지정해 네트워크 오류·HTTP 5xx·429만 재시도하고, 키 미설정과 4xx는 재시도하지 않습니다. 취소 예외는 그대로 전파합니다. Ktor receiver도 클라이언트 주입을 위한 기능이며 이 오류 정책을 자동 해결하는 근거는 아닙니다.

### 취소와 Result 변환의 경계

Query/InfiniteQuery의 fetch와 Mutation은 성공 시 T를 반환하고 실패는 던지는 계약으로 둡니다. 확인한 Soil의 [QueryCommand](https://github.com/soil-kt/soil/blob/main/soil-query-core/src/commonMain/kotlin/soil/query/QueryCommand.kt)와 [MutationCommand](https://github.com/soil-kt/soil/blob/main/soil-query-core/src/commonMain/kotlin/soil/query/MutationCommand.kt)는 재시도와 실패 상태 변환을 맡고 CancellationException은 재전파합니다. 여기서 `Result.failure`를 T로 반환하면 바깥 Soil 상태가 성공으로 처리될 수 있으므로, 모든 호출을 runSuspendCatching으로 감싸지 않습니다.

Subscription은 Flow<T>를 반환합니다. upstream 실패를 데이터인 Result.failure로 미리 바꾸지 않고, Flow의 취소 투명성과 Soil 구독 파이프라인의 retryWhen/catch 경계를 유지합니다.

Reed의 runSuspendCatching은 취소 예외를 다시 던지지만 다른 Throwable은 Error까지 포착합니다. Soil 바깥의 수동 일회성 작업에서 Result 계약이 실제로 필요할 때만 사용 여부를 검토하고, Exception과 Throwable 중 포착 범위를 의도적으로 선택합니다. 이번 구현에는 그런 작업이 없어 이 helper를 도입하지 않았습니다. Soil 밖에서 예외를 직접 다루는 곳은 `FavoriteToggler` 하나이며, `CancellationException`은 다시 던지고 `Exception`만 실패로 표시합니다.

필수 확인은 취소가 오류 UI·재시도로 바뀌지 않는지, 일반 작업 실패가 Soil 오류 상태에 도달하는지, 명시한 Retryable/shouldRetry 조건만 재시도되는지입니다. 참고 helper도 개별적으로 검토합니다. DroidKaigi 기준 커밋의 일부 영속 캐시 get/put wrapper는 일반 runCatching으로 실패를 버리거나 null로 바꾸므로 취소 전파까지 보장한다고 가정하지 않습니다. 나중에 재사용한다면 저장·읽기 중 취소가 null/성공으로 바뀌지 않는 테스트를 먼저 추가합니다. 현재 이 영속 캐시 wrapper는 도입하지 않습니다. 정확한 동작은 선택한 버전의 코드와 테스트로 다시 확인합니다.

## 5. 이미지와 인증 경계

[Coil 3](https://coil-kt.github.io/coil/) 3.6.3의 coil-compose와 coil-network-okhttp를 씁니다. 앱 그래프의 `ImageLoader`는 인증 interceptor가 없는 기본 OkHttp 클라이언트로 만들고, API 클라이언트는 그 기본 클라이언트에 `KakaoAuthInterceptor`를 더한 별도 인스턴스입니다.

- 이미지 요청에 Kakao REST API 키나 API 전용 Authorization interceptor를 전달하지 않습니다.
- API용 OkHttp 설정과 이미지용 네트워크 설정을 분리합니다. 이름이나 qualifier로 혼동을 막는 구성을 검토합니다.
- 필요한 경우 안전한 연결 풀 등 자원을 공유할 수 있지만, API 인증 interceptor가 포함된 클라이언트를 그대로 복사하지 않습니다.
- 이미지 캐시는 즐겨찾기 텍스트 영속성과 별개입니다. URL 저장만으로 오프라인 표지 표시가 보장되지는 않습니다.
- 표지 없음·로딩·실패 대체 UI를 제공합니다. 이미지 실패로 목록이나 상세의 나머지 정보가 사라지지 않게 합니다.

## 6. 오류와 오프라인 범위

기본 범위는 온라인 검색 + 세션 내 결과 유지 + 재시도, 그리고 Room 기반 오프라인 즐겨찾기·상세입니다. 최초 검색 오류, 추가 로딩 오류, DB 쓰기 오류, 이미지 오류를 별도로 표현합니다. 취소 예외를 일반 네트워크 오류로 바꾸지 않습니다.

### 로그

로그는 [Timber](https://github.com/JakeWharton/timber)로 남깁니다(사용자 선택). 이유는 다음과 같습니다.

- 출력 여부를 한 곳에서 정합니다. `TimberInitializer`가 debug 빌드에서만 `DebugTree`를 심으므로, release에서는 호출 지점마다 `BuildConfig.DEBUG`를 확인하지 않아도 아무것도 출력되지 않습니다. 라이브러리 모듈은 app의 `BuildConfig`를 볼 수 없으므로 이 방식이 특히 맞습니다.
- 인증 경계와 함께 관리합니다. OkHttp 로그도 Timber로 보내므로, `Authorization` 헤더 가림과 debug 전용 출력이라는 두 가지 규칙이 같은 경로를 지납니다.
- 테스트에 영향이 없습니다. JVM 단위 테스트에서는 Tree를 심지 않으므로 `android.util.Log`를 호출하지 않으며, 로그 때문에 Robolectric이 필요해지지 않습니다.
- 태그를 호출 클래스에서 자동으로 만들고, 포맷 인자와 예외를 함께 넘길 수 있어 `Log` 직접 호출보다 호출 지점이 짧습니다.

초기화는 [App Startup](https://developer.android.com/topic/libraries/app-startup)의 `Initializer`로 등록합니다(사용자 선택, Reed와 같은 방식).

- `Application`은 앱 그래프와 Coil 연결만 맡고, 라이브러리 초기화는 `initializer` 패키지의 클래스 하나에 하나씩 둡니다. 초기화가 늘어도 `Application.onCreate`가 커지지 않고, 순서는 `dependencies()`로 표현합니다.
- `InitializationProvider`가 `Application.onCreate`보다 먼저 실행하므로, 그 뒤의 어떤 코드가 남긴 로그도 빠지지 않습니다.
- 병합 매니페스트에는 emoji2·lifecycle·OkHttp가 이미 같은 `InitializationProvider`를 등록하고 있습니다. 이 앱은 그 provider에 `meta-data` 한 줄을 더할 뿐이라 ContentProvider가 늘지 않습니다.

2021-08의 5.0.1 이후 새 릴리스는 없습니다. API가 작고 안정적이라 이 앱의 사용 범위에서는 문제로 보지 않습니다. 남기는 로그는 HTTP 요청 요약과 즐겨찾기 쓰기 실패이며, API 키나 요청 헤더 값은 남기지 않습니다.

원격 검색 결과의 영속 캐시는 추가 범위입니다. DroidKaigi의 `PersistedQueryKey`는 별도 구현이므로 Soil 도입만으로 자동 디스크 캐시를 기대하지 않습니다. 나중에 도입하면 만료·무효화·용량·오래된 정보 표시까지 함께 설계합니다.

## 7. 테스트와 계층 대응

테스트 코드 작성은 사용자가 추가한 필수 요구입니다. 작성, 실행, 통과는 각각 구분합니다.

| 대상 | 제안 도구·경계 | 확인할 내용 |
| --- | --- | --- |
| 독립 비즈니스 규칙 | 일반 Kotlin 단위 테스트 | ID, 가격 경계, 필터·정렬 규칙 |
| 원격 API | Retrofit + MockWebServer | query/sort/page/size, 응답 변환, 오류 분류·재시도 조건, 취소 보존, 인증 헤더 범위 |
| Soil 데이터 접근 | Fake key/데이터 소스, 제어 가능한 코루틴 | key 격리, is_end, 중복 append, 실패 후 재시도, 취소 |
| 로컬 저장 | Room 통합 테스트 | 저장·삭제·Flow 관찰·DB 재열기 후 복원 |
| Presenter | Molecule + Turbine + coroutines-test(`core:testing`) | Fake 의존성으로 상태 방출·이벤트·경합 확인 |
| 실제 호스트와 UI | Compose/내비게이션·기기 통합 검증 | retain 수명, pop 정리, 회전·크기 변화·프로세스 복원 |

Presenter 테스트는 의존성이 허용하면 Compose runtime/compiler 기반 로컬 JVM에서 실행할 수 있으며 Android UI·기기가 항상 필요한 것은 아닙니다. 그러나 Molecule의 모델 방출 테스트는 실제 호스트의 retained store나 백스택 수명을 증명하지 않습니다. 앱의 수명 통합 검증은 따로 수행합니다.

테스트를 위해 Repository를 새로 만들지 않았습니다. Presenter 테스트는 Fake `FavoriteWriter`와 테스트 전용 Soil key를 `SwrClientPlus`에 연결해 실행합니다. 공통 실행 도구는 테스트 전용 `core:testing` 모듈에 모았습니다. 실제 호스트의 retain 수명은 `BookSearchNavHostTest`가 Robolectric 위에서 `ActivityScenario`로 앱 NavHost를 띄워 확인합니다.

## 8. 코드 경계와 확장

경계는 `app` 조립, `feature:search` / `feature:favorites` / `feature:detail`, 공통 `core:model` / `core:network` / `core:database` / `core:data` / `core:ui`의 9개 Gradle 모듈이고, 테스트 전용 `core:testing`이 더해져 모두 10개입니다. feature는 `core:data`의 Soil key와 `Book`만 보고, Retrofit·Room 타입은 `core:data` 뒤에 숨깁니다. 여러 개발자가 기능 단위로 나누어 작업한다는 과제 전제를 빌드 수준의 경계로 보여 주기 위한 결정입니다. 공통 빌드 설정은 `build-logic`의 작은 convention plugin으로 모읍니다. 모듈별 책임과 실패 시 후퇴 방법은 [SPEC](SPEC.md) 2절에 있습니다.

기능은 다른 기능의 구현을 직접 참조하지 않고 내비게이션 조립은 app에서 담당합니다. 탭과 백스택 규칙(재선택, 상세 중복 방지, 뒤로 가기)은 [SPEC](SPEC.md) 7절을 따르며, 참고 앱의 탭 전환 코드를 그대로 복사하지 않습니다. 데이터 key와 UI 모델의 계약을 공유해 검색·로컬 저장을 나누어 작업할 수 있게 합니다. 참조 프로젝트의 규모를 맞추기 위한 모듈이나 범용 플랫폼 계층은 추가하지 않습니다.

## 9. 선택 이유와 비용

- Retrofit: 더 익숙한 Android REST 구현 경험을 활용합니다. Ktor도 가능한 대안이지만 현재 선택은 아닙니다.
- Soil: typed-key 데이터 접근과 InfiniteQuery를 학습합니다. key/provider 수명, 취소·무효화·테스트 설정에 대한 이해 비용을 감수합니다.
- Room: 즐겨찾기의 영속성과 관찰 기준을 명확하게 둡니다. 스키마·빌드 플러그인·마이그레이션 책임이 생깁니다.
- retain + Navigation 3: 사용자의 학습 목표를 유지합니다. 엔트리 수명과 프로세스 복원을 나누어 설계해야 합니다.
- Metro: 객체 연결을 명시적으로 구성합니다. [공식 프로젝트](https://github.com/ZacSweers/metro) 기준으로 Kotlin 2.4.0을 지원하는 1.4.5를 썼습니다. 이 버전의 Gradle 플러그인이 JVM 21 이상에서만 실행되어 Gradle daemon을 JDK 21로 고정하는 비용이 생겼습니다(10절).
- Coil: 이미지 로딩을 맡깁니다. 캐시 정책과 API 인증 경계를 별도로 관리해야 합니다.
- Timber: debug 전용 로그 출력을 한 곳에서 정합니다. 의존성 하나가 늘고, 2021년 이후 릴리스가 없다는 점을 감수합니다(6절).

### 공식 철학과 기존 경험을 적용하는 범위

[Soil 공식 동기](https://docs.soil-kt.com/guide/what-is-soil#motivation)는 Compose-first 데이터 접근, 서버 상태, stale-while-revalidate에 초점을 둡니다. ViewModel의 유용성을 부정하지 않으며 모든 앱의 Repository 제거를 요구하는 근거도 아닙니다. 사용자가 이 철학에 공감한다고 밝혔지만, 어떤 문장에 어떤 의미로 동의했는지 더 구체적으로 추정하지 않습니다.

[사용자의 적용 글(2025-09-30)](https://velog.io/@mraz3068/Soil-Apply)은 QueryKey가 데이터 접근 책임을 맡는 사례입니다. 호출 책임 자체가 사라진 것은 아니며, 주입한 클라이언트의 서비스 설정도 남습니다. 당시 Retrofit에서 Ktor로 바꾼 것은 그 구현의 선택이므로 이번 Retrofit 선택을 취소하는 이유가 아닙니다. 사용자는 DroidKaigi 2026을 참고해 당시의 여러 의문이 해소되었다고 설명했습니다. 로컬 Mutation/Subscription을 되돌린 경험은 현재 선택을 다시 뒤집는 조건으로 삼지 않고, Room 단일 상태 기준과 구독 수명·반영 순서 테스트에 활용합니다. 관찰한 느린 동기화의 원인도 확정된 Soil 결함으로 표현하지 않습니다.

[사용자의 DroidKaigi 비교 글(2026-08-17)](https://velog.io/@mraz3068/Same-Soil-Different-Architecture-DroidKaigi-2025-2026-App-Comparison)은 Root의 query/subscription·boundary 조립과 Presenter의 준비된 데이터·로컬 상태·mutation 책임 분리를 참고할 자료입니다. 이를 무조건 계층 수를 줄이거나 Presenter를 단순 전달 계층으로 만들라는 규칙으로 해석하지 않습니다.

## 10. 빌드 조합과 검증 결과

지정된 Gradle 9.4.1 / AGP 9.2.1 / Kotlin 2.4.0 / KSP 2.3.10 / compileSdk 37을 그대로 썼습니다. 의존성 버전은 [SPEC](SPEC.md) 2절에 고정했습니다.

JDK 17 지정은 앱 바이트코드와 컴파일 기준으로 지켰습니다(`sourceCompatibility`·`targetCompatibility`·`jvmTarget` 17). Metro 1.4.5의 Gradle 플러그인이 실행 JVM 21 이상을 요구해 JDK 17로는 Gradle 설정 단계부터 실패하므로, `gradle/gradle-daemon-jvm.properties`로 Gradle daemon만 JDK 21로 고정했습니다(2026-10-07 사용자 결정). JDK 17로 `./gradlew`를 실행해도 Gradle이 JDK 21 daemon을 찾거나 내려받습니다. 자세한 근거는 [SPEC](SPEC.md) 2절 "JDK 기준"에 있습니다.

2026-10-08, 커밋 `71cdf67`에서 실행한 결과입니다.

| 명령 | 결과 |
| --- | --- |
| `./gradlew assembleDebug testDebugUnitTest test --continue` | 성공. 테스트 110개 실행, 실패 0, 오류 0, 건너뜀 0 |

`testDebugUnitTest`는 Android 모듈의 테스트(87개)만 실행하고, 순수 JVM 모듈인 `core:model`의 테스트(23개)는 `test` 작업으로 실행됩니다. 두 작업을 함께 지정해야 전체가 실행됩니다.

환경은 macOS 26.3.1, Launcher JVM 17.0.10, Gradle daemon JVM 21(Temurin)입니다. 테스트 클래스별 대응과 실기기 시나리오 결과는 [QA](QA.md)에, 요구사항별 검증 상태는 [REQUIREMENTS](REQUIREMENTS.md)의 검증 기록에 있습니다.

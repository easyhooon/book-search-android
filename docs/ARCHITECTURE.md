# 아키텍처

기준일: 2026-10-07. **구현 전 설계 문서**입니다. 아래 구성은 선택한 방향과 현재 권장안을 설명하며, 앱 구현·의존성 호환·테스트 통과를 뜻하지 않습니다. 전체 요구사항은 [REQUIREMENTS](REQUIREMENTS.md), 화면·API·백스택 등 코드로 옮길 세부 계약은 [SPEC](SPEC.md), 일정·참고 코드·상세 검증 시나리오는 [STRATEGY](STRATEGY.md), 판단 과정은 [AI_USAGE](AI_USAGE.md)에 기록합니다.

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
| 표지 이미지 | Coil | 사용자 선택. Coil 3와 coil-compose + coil-network-okhttp는 현재 권장 구성 |

Android 전용입니다. KMP source set, expect/actual, 플랫폼 추상화, Circuit은 도입하지 않습니다. 참고 앱의 전체 DI 그래프나 프로젝트 구성을 복제하지 않습니다. 기술 수가 많다고 요구사항 충족이나 좋은 설계가 보장되지는 않으므로, 첫 구현에서 빌드와 작은 기능 흐름을 먼저 검증합니다.

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

Metro는 의존성을 연결하는 도구이며, 화면 상태나 데이터의 단일 기준을 대신하지 않습니다. 앱 수준에서 재사용하는 API·DB·이미지 로더와 화면 엔트리의 수명을 구분합니다. 구체적인 scope·provider API·그래프 수는 구현 전 검증하며, 앱 하나에 필요한 최소 구성으로 시작합니다.

### Metro factory와 retain 연결

DroidKaigi 2026의 [TimetableScreenGraph](https://github.com/DroidKaigi/conference-app-2026/blob/main/feature/sessions/src/commonMain/kotlin/io/github/droidkaigi/confsched/feature/sessions/timetable/TimetableScreenGraph.kt)와 [TimetableNavEntryProvider](https://github.com/DroidKaigi/conference-app-2026/blob/main/feature/sessions/src/commonMain/kotlin/io/github/droidkaigi/confsched/feature/sessions/timetable/TimetableNavEntryProvider.kt)를 참고합니다. 선언한 `@GraphExtension.Factory`의 구현은 Metro 컴파일러가 생성하고, 내비게이션 엔트리에서는 factory.createGraph 결과를 retain으로 유지하는 패턴입니다. 이 프로젝트도 선언한 최소 factory와 retain/Nav3의 연결을 검토합니다.

2025 예제의 커스텀 KSP 생성기는 이미 있는 factory를 감싼 remember 편의 함수를 만드는 별도 장치였으며, factory 구현 자체를 생성한 것이 아닙니다. 이번에는 자체 코드 생성기·컴파일러 플러그인을 만들지 않습니다. Metro의 기존 컴파일러 플러그인과 Room용 KSP는 역할과 호환성을 별도로 확인합니다.

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
| 선택한 상세 경로 | 도서 한 권의 스냅샷을 담은 저장 가능한 내비게이션 키 | 전체 목록은 백스택에 넣지 않음. 표시 데이터는 Room 값 우선, 없으면 스냅샷 |
| 화면 엔트리에서 유지할 객체 | retain 엔트리 범위 | pop 시 정리. 프로세스 종료 저장과는 별개 |
| 표지 이미지 | Coil 이미지 로더·캐시 | 캐시 미존재·실패 시 대체 UI |

[사용자의 retain 글(2025-10-21)](https://velog.io/@mraz3068/Compose-Retain-API)도 객체 유지와 프로세스 종료 복원을 구분하는 참고 자료입니다. 당시 preview 용어나 버전을 현재 API 계약으로 그대로 사용하지 않고, 선택한 버전의 문서와 실제 호스트로 확인합니다.

검색 결과의 즐겨찾기 표시는 원격 도서와 Room의 즐겨찾기 ID를 결합합니다. Soil 캐시에 별도의 즐겨찾기 boolean을 두고 DB와 경쟁시키지 않습니다.

상세는 진입 시점의 도서 스냅샷을 내비게이션 키에 담습니다. Kakao API에는 단건 조회가 없고, ISBN이 없는 도서는 다시 찾을 수 없기 때문입니다. 표시는 Room 값을 우선하고, 없으면 스냅샷을 씁니다. 즐겨찾기 여부는 항상 Room을 기준으로 합니다. 그래서 상세에서 즐겨찾기를 해제해도 화면이 비지 않고, 프로세스 재생성 후에도 네트워크 없이 상세가 다시 표시됩니다. 자동 즐겨찾기 저장은 하지 않습니다. 전체 결과를 saveable로 저장하지 않고, 강제 종료 뒤 콜드 스타트에 같은 복원을 보장하지 않습니다. retain은 이 문제를 자동 해결하지 않습니다.

## 4. 핵심 동작

### 검색과 다음 페이지

1. 검색 버튼/IME 제출로 입력을 확정하고 정규화한 query/sort key를 만듭니다.
2. InfiniteQuery의 첫 요청은 page=1, size=20입니다.
3. 응답의 도서 목록과 `meta.is_end` 등 페이지 메타데이터를 보존합니다.
4. 성공한 페이지 뒤에만 다음 페이지로 진행합니다. is_end=true면 20개가 반환되어도 종료합니다.
5. 추가 로딩 실패 시 기존 목록을 보존하고 동일 조건·페이지 재시도를 제공합니다.
6. ISBN 기반 중복 제거 후 목록 길이로 다음 페이지나 종료 여부를 다시 계산하지 않습니다.

key 변경, 빈 검색어 초기화, 중복 append, 응답 역전, 취소·재시도는 실제 Soil 구성으로 검증합니다. 라이브러리 이름만으로 자동 보장을 주장하지 않습니다. 필요한 추가 방어는 넣되 별도의 커스텀 페이지 상태를 무조건 병행하지 않습니다.

### 즐겨찾기 쓰기와 관찰

아래는 선택한 Subscription/Mutation 구성의 흐름입니다.

1. 화면이 `setFavorite(book, favorite)`처럼 도서와 목표 즐겨찾기 상태를 명령으로 전달합니다. 저장(upsert)에는 도서 정보 전체가 필요하므로 ID만 넘기지 않습니다.
2. Mutation이 앱 그래프 싱글턴 `FavoriteWriter`를 호출하고, `FavoriteWriter`가 Room에 저장/삭제합니다. 해당 도서 버튼만 비활성화하고 명령 순서를 보장합니다.
3. Subscription이 Room Flow 변경을 관찰합니다.
4. 검색·즐겨찾기·상세가 동일한 확정 상태를 반영합니다.
5. 쓰기 실패 시 성공 안내를 하지 않고 기존 확정 상태를 유지합니다.

화면 이동은 허용하고 짧은 DB 쓰기는 화면 밖의 명시적으로 관리되는 scope에서 완료합니다. Soil Mutation의 scope는 마지막 구독자가 사라지고 keepAliveTime(기본 5초)이 지나면 취소됩니다. 그래서 실제 쓰기는 Metro 앱 그래프가 소유한 `ApplicationScope`(SupervisorJob)에서 `async`로 시작하고, 하나의 `Mutex`로 직렬화합니다. Mutation은 그 결과를 기다리기만 합니다. 이 범위의 소유자·종료·오류 처리와 명령 순서를 테스트합니다. 프로세스 종료 시 미완료 쓰기까지 완료된다고 보장하지 않습니다. 세부 근거와 알려진 한계는 [SPEC](SPEC.md) 8절에 있습니다.

### Retrofit 연결과 재시도 경계

현재 확인한 공식 모듈 목록에는 Ktor receiver가 있지만 Retrofit 전용 모듈은 없습니다. 범용 Query key의 suspend fetch에서 주입된 Retrofit suspend API를 호출할 수 있으므로 전용 모듈이나 커스텀 어댑터가 반드시 필요한 것은 아닙니다. 이는 API 형태에 근거한 연결 가능성이며, 지정 버전으로 실행한 호환성 증거는 아직 없습니다.

기본 재시도 정책의 `Retryable` / `canRetry` 조건에 일반 Retrofit `HttpException`이나 `IOException`이 자동으로 들어간다고 가정하지 않습니다. 명시적인 `shouldRetry` 또는 재시도 대상 예외 매핑을 적용하고, 취소 예외는 그대로 전파합니다. Ktor receiver도 클라이언트 주입을 위한 기능이며 이 오류 정책을 자동 해결하는 근거는 아닙니다.

### 취소와 Result 변환의 경계

Query/InfiniteQuery의 fetch와 Mutation은 성공 시 T를 반환하고 실패는 던지는 계약으로 둡니다. 확인한 Soil의 [QueryCommand](https://github.com/soil-kt/soil/blob/main/soil-query-core/src/commonMain/kotlin/soil/query/QueryCommand.kt)와 [MutationCommand](https://github.com/soil-kt/soil/blob/main/soil-query-core/src/commonMain/kotlin/soil/query/MutationCommand.kt)는 재시도와 실패 상태 변환을 맡고 CancellationException은 재전파합니다. 여기서 `Result.failure`를 T로 반환하면 바깥 Soil 상태가 성공으로 처리될 수 있으므로, 모든 호출을 runSuspendCatching으로 감싸지 않습니다.

Subscription은 Flow<T>를 반환합니다. upstream 실패를 데이터인 Result.failure로 미리 바꾸지 않고, Flow의 취소 투명성과 Soil 구독 파이프라인의 retryWhen/catch 경계를 유지합니다.

Reed의 runSuspendCatching은 취소 예외를 다시 던지지만 다른 Throwable은 Error까지 포착합니다. Soil 바깥의 수동 일회성 작업에서 Result 계약이 실제로 필요할 때만 사용 여부를 검토하고, Exception과 Throwable 중 포착 범위를 의도적으로 선택합니다. 아직 이 helper를 구현하거나 채택한 것은 아닙니다.

필수 확인은 취소가 오류 UI·재시도로 바뀌지 않는지, 일반 작업 실패가 Soil 오류 상태에 도달하는지, 명시한 Retryable/shouldRetry 조건만 재시도되는지입니다. 참고 helper도 개별적으로 검토합니다. DroidKaigi 기준 커밋의 일부 영속 캐시 get/put wrapper는 일반 runCatching으로 실패를 버리거나 null로 바꾸므로 취소 전파까지 보장한다고 가정하지 않습니다. 나중에 재사용한다면 저장·읽기 중 취소가 null/성공으로 바뀌지 않는 테스트를 먼저 추가합니다. 현재 이 영속 캐시 wrapper는 도입하지 않습니다. 정확한 동작은 선택한 버전의 코드와 테스트로 다시 확인합니다.

## 5. 이미지와 인증 경계

현재 권장안은 [Coil 3](https://coil-kt.github.io/coil/)의 coil-compose와 coil-network-okhttp 조합입니다. 정확한 버전은 초기 호환성 검증 후 고정합니다.

- 이미지 요청에 Kakao REST API 키나 API 전용 Authorization interceptor를 전달하지 않습니다.
- API용 OkHttp 설정과 이미지용 네트워크 설정을 분리합니다. 이름이나 qualifier로 혼동을 막는 구성을 검토합니다.
- 필요한 경우 안전한 연결 풀 등 자원을 공유할 수 있지만, API 인증 interceptor가 포함된 클라이언트를 그대로 복사하지 않습니다.
- 이미지 캐시는 즐겨찾기 텍스트 영속성과 별개입니다. URL 저장만으로 오프라인 표지 표시가 보장되지는 않습니다.
- 표지 없음·로딩·실패 대체 UI를 제공합니다. 이미지 실패로 목록이나 상세의 나머지 정보가 사라지지 않게 합니다.

## 6. 오류와 오프라인 범위

기본 범위는 온라인 검색 + 세션 내 결과 유지 + 재시도, 그리고 Room 기반 오프라인 즐겨찾기·상세입니다. 최초 검색 오류, 추가 로딩 오류, DB 쓰기 오류, 이미지 오류를 별도로 표현합니다. 취소 예외를 일반 네트워크 오류로 바꾸지 않습니다.

원격 검색 결과의 영속 캐시는 추가 범위입니다. DroidKaigi의 `PersistedQueryKey`는 별도 구현이므로 Soil 도입만으로 자동 디스크 캐시를 기대하지 않습니다. 나중에 도입하면 만료·무효화·용량·오래된 정보 표시까지 함께 설계합니다.

## 7. 테스트와 계층 대응

테스트 코드 작성은 사용자가 추가한 필수 요구입니다. 작성, 실행, 통과는 각각 구분합니다.

| 대상 | 제안 도구·경계 | 확인할 내용 |
| --- | --- | --- |
| 독립 비즈니스 규칙 | 일반 Kotlin 단위 테스트 | ID, 가격 경계, 필터·정렬 규칙 |
| 원격 API | Retrofit + MockWebServer | query/sort/page/size, 응답 변환, 오류 분류·재시도 조건, 취소 보존, 인증 헤더 범위 |
| Soil 데이터 접근 | Fake key/데이터 소스, 제어 가능한 코루틴 | key 격리, is_end, 중복 append, 실패 후 재시도, 취소 |
| 로컬 저장 | Room 통합 테스트 | 저장·삭제·Flow 관찰·DB 재열기 후 복원 |
| Presenter | Molecule + Turbine + coroutines-test 권장 | Fake 의존성으로 상태 방출·이벤트·경합 확인 |
| 실제 호스트와 UI | Compose/내비게이션·기기 통합 검증 | retain 수명, pop 정리, 회전·크기 변화·프로세스 복원 |

Presenter 테스트는 의존성이 허용하면 Compose runtime/compiler 기반 로컬 JVM에서 실행할 수 있으며 Android UI·기기가 항상 필요한 것은 아닙니다. 그러나 Molecule의 모델 방출 테스트는 실제 호스트의 retained store나 백스택 수명을 증명하지 않습니다. 앱의 수명 통합 검증은 따로 수행합니다.

테스트를 위해 불필요한 Repository를 새로 만들지 않습니다. Soil 버전에서 지원하는 provider/key 주입 방법을 확인해 Fake를 연결하고, 가능하지 않은 가정은 구현으로 검증하기 전 확정하지 않습니다.

## 8. 코드 경계와 확장

경계는 `app` 조립, `feature:search` / `feature:favorites` / `feature:detail`, 공통 `core:model` / `core:data` / `core:ui`의 7개 Gradle 모듈입니다. 여러 개발자가 기능 단위로 나누어 작업한다는 과제 전제를 빌드 수준의 경계로 보여 주기 위한 결정입니다. 공통 빌드 설정은 `build-logic`의 작은 convention plugin으로 모읍니다. 모듈별 책임과 실패 시 후퇴 방법은 [SPEC](SPEC.md) 2절에 있습니다.

기능은 다른 기능의 구현을 직접 참조하지 않고 내비게이션 조립은 app에서 담당합니다. 탭과 백스택 규칙(재선택, 상세 중복 방지, 뒤로 가기)은 [SPEC](SPEC.md) 7절을 따르며, 참고 앱의 탭 전환 코드를 그대로 복사하지 않습니다. 데이터 key와 UI 모델의 계약을 공유해 검색·로컬 저장을 나누어 작업할 수 있게 합니다. 참조 프로젝트의 규모를 맞추기 위한 모듈이나 범용 플랫폼 계층은 추가하지 않습니다.

## 9. 선택 이유와 비용

- Retrofit: 더 익숙한 Android REST 구현 경험을 활용합니다. Ktor도 가능한 대안이지만 현재 선택은 아닙니다.
- Soil: typed-key 데이터 접근과 InfiniteQuery를 학습합니다. key/provider 수명, 취소·무효화·테스트 설정에 대한 이해 비용을 감수합니다.
- Room: 즐겨찾기의 영속성과 관찰 기준을 명확하게 둡니다. 스키마·빌드 플러그인·마이그레이션 책임이 생깁니다.
- retain + Navigation 3: 사용자의 학습 목표를 유지합니다. 엔트리 수명과 프로세스 복원을 나누어 설계해야 합니다.
- Metro: 객체 연결을 명시적으로 구성합니다. [공식 프로젝트](https://github.com/ZacSweers/metro)를 기준으로 Kotlin 2.4.0과 컴파일러 플러그인 호환성을 먼저 확인하고 최소 그래프만 도입합니다.
- Coil: 이미지 로딩을 맡깁니다. 캐시 정책과 API 인증 경계를 별도로 관리해야 합니다.

### 공식 철학과 기존 경험을 적용하는 범위

[Soil 공식 동기](https://docs.soil-kt.com/guide/what-is-soil#motivation)는 Compose-first 데이터 접근, 서버 상태, stale-while-revalidate에 초점을 둡니다. ViewModel의 유용성을 부정하지 않으며 모든 앱의 Repository 제거를 요구하는 근거도 아닙니다. 사용자가 이 철학에 공감한다고 밝혔지만, 어떤 문장에 어떤 의미로 동의했는지 더 구체적으로 추정하지 않습니다.

[사용자의 적용 글(2025-09-30)](https://velog.io/@mraz3068/Soil-Apply)은 QueryKey가 데이터 접근 책임을 맡는 사례입니다. 호출 책임 자체가 사라진 것은 아니며, 주입한 클라이언트의 서비스 설정도 남습니다. 당시 Retrofit에서 Ktor로 바꾼 것은 그 구현의 선택이므로 이번 Retrofit 선택을 취소하는 이유가 아닙니다. 사용자는 DroidKaigi 2026을 참고해 당시의 여러 의문이 해소되었다고 설명했습니다. 로컬 Mutation/Subscription을 되돌린 경험은 현재 선택을 다시 뒤집는 조건으로 삼지 않고, Room 단일 상태 기준과 구독 수명·반영 순서 테스트에 활용합니다. 관찰한 느린 동기화의 원인도 확정된 Soil 결함으로 표현하지 않습니다.

[사용자의 DroidKaigi 비교 글(2026-08-17)](https://velog.io/@mraz3068/Same-Soil-Different-Architecture-DroidKaigi-2025-2026-App-Comparison)은 Root의 query/subscription·boundary 조립과 Presenter의 준비된 데이터·로컬 상태·mutation 책임 분리를 참고할 자료입니다. 이를 무조건 계층 수를 줄이거나 Presenter를 단순 전달 계층으로 만들라는 규칙으로 해석하지 않습니다.

## 10. 첫 구현에서 확인할 것

지정된 Gradle 9.4.1 / AGP 9.2.1 / Kotlin 2.4.0 / KSP 사용 시 2.3.10 / JDK 17 / compileSdk 37 요구는 유지합니다. Metro 컴파일러 플러그인, Compose compiler/runtime, Soil, Room/KSP, Retrofit/직렬화, Coil의 정확한 의존성 버전은 공식 호환 정보를 확인한 뒤 고정합니다. 충돌 시 요구 버전을 임의로 바꾸지 않습니다.

첫 검증은 최소 Metro 조립 → 검색 한 페이지 → Room 한 건 저장·관찰 → retain/Nav3 상세 진입·복귀입니다. 이 경로의 빌드와 테스트가 확인된 뒤 기능을 확장합니다. 현재 문서에는 실제 빌드 성공이나 테스트 결과가 없습니다.

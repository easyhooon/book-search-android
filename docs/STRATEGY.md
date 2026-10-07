# 구현 전략

작성일: 2026-10-07 / 일정 기준: 한국 표준시(KST)

이 문서는 구현 전 계획입니다. 참조 코드에서 확인한 사실, 사용자 선택, 설계 권장안, 미정 사항을 구분합니다. 현재 앱 구현·빌드·테스트 완료를 의미하지 않습니다. 원문이 재량에 맡긴 세부 동작은 [구현 사양](SPEC.md)에서 기본안으로 확정했습니다.

## 1. 목표와 우선순위

3일 안에 검색 → 상세 → 즐겨찾기 → 재실행 후 복원까지 이어지는 작은 앱을 완성합니다. 마감은 **2026-10-10(토) 14:30 KST**입니다.

우선순위는 다음과 같습니다.

1. 지정 환경에서 재현 가능한 빌드
2. 요구된 검색·즐겨찾기·상세 기능
3. 요청 경합, 저장 실패, 오프라인, 화면 크기 변화에 대한 일관된 동작
4. 설계 판단을 설명하는 문서와 실제 검증 기록
5. 시간 여유가 있으면 추가 캐시·시연 영상·확장 실험

retain 학습은 사용자 선택입니다. 일정이 촉박하다는 이유로 ViewModel로 전면 교체하는 것을 기본 해법으로 삼지 않습니다. 대신 retain 적용 범위를 작게 하고 수명·복원 검증을 앞당깁니다.

## 2. 결정 현황

| 항목 | 상태 | 방향과 확인할 점 |
| --- | --- | --- |
| Kotlin / Coroutines / Flow / Compose | 필수 | 요구 기술 유지 |
| retain + Navigation 3 | 사용자 선택 | 화면 단위 상태 소유권과 제거 시 정리 검증 |
| Circuit | 사용자 선택: 미도입 | Reed는 구현 패턴 참고 자료로만 사용 |
| 플랫폼 구성 | 사용자 선택: Android 전용 | KMP source set, expect/actual, 플랫폼 추상화 미도입 |
| Presenter 형태 | 현재 권장안 | DroidKaigi 방식의 얇은 @Composable Presenter. 최종 형태와 테스트 의존성은 구현 검증 후 확정 |
| 테스트 코드 작성 | 사용자 추가 필수 | 핵심 단위·경합·페이지네이션, Room 통합, 필수 Compose UI 테스트. 실제 실행 증거는 별도 기록 |
| Room | 사용자 선택 | 즐겨찾기 영속 저장. 원 과제의 지정 라이브러리는 아님 |
| Room Flow 단일 상태 기준 | 사용자 선택 | 목록·상세의 즐겨찾기 상태 불일치 방지 |
| Retrofit + OkHttp + kotlinx.serialization | 사용자 선택 | 익숙한 Android REST 구현 경험을 활용. 버전은 검증 후 고정 |
| Soil typed-key / Repository 생략 | 사용자 선택 | 원격 InfiniteQuery, 로컬 Subscription→Room Flow / Mutation→Room 쓰기. 단순 위임 Repository 미도입 |
| Soil InfiniteQuery | 사용자 선택 | query/sort key, size=20, is_end 기반 페이지 계약 검증 |
| Metro | 사용자 선택 | 최소 DI 그래프. Kotlin 2.4.0과 컴파일러 플러그인 호환 먼저 검증 |
| Coil | 사용자 선택 | Coil 3 + coil-compose/coil-network-okhttp 권장. API 인증 설정과 이미지 요청 분리 |
| 가격 필터 | 현재 제품 정책 | 정가 최소·최대 금액, 양끝 포함, 적용 버튼. 누락·음수와 0원 구분 |
| 모듈 구성 | 결정 | app, core:model/network/database/data/ui, feature:search/favorites/detail의 9개. 모듈 수 자체를 목표로 삼지 않음 |
| 검색 결과 디스크 캐시 | 후순위 | 필수 기능 완료 전에는 도입하지 않음 |
| 사용자 retain PoC | 참고 정보 | 사용자가 공유한 경험. 코드는 확인하지 못했으므로 구현 근거로 쓰지 않음 |
| 세부 동작 기본안 | 결정 | 원문의 재량 영역은 [SPEC](SPEC.md)에 기본안으로 확정. 구현 중 사실 확인이 필요한 항목은 SPEC 13절에 분리 |

### 원문 요구, 제품 선택, 구현 계약의 구분

로컬 저장·금액 필터·불안정한 네트워크 대응·태블릿/폴더블은 원문에 명시된 요구입니다. 이를 암묵적 선택이나 사용자 재승인 항목으로 다루지 않습니다. 원문이 정하지 않은 제품 선택은 검색 실행 시점, 가격 필터 세부, 저장 중 버튼/pop 정책, 프로세스 종료 후 복원 범위, list-detail 2-pane 여부의 다섯 가지입니다. 사용자의 도서 앱 사례 참고 방향에 따라 현재 정책을 정했으며, 아래 근거와 한계를 구분합니다.

확정 query에 기반한 페이지네이션, ISBN 정규화·대체 ID, 쓰기 순서, 크기 변경 상태 보존은 구현 계약과 테스트로 구체화합니다. 이 기술 항목들을 별도 제품 승인 질문으로 늘리지 않습니다. 원문과 설계 구체화의 체크리스트는 [REQUIREMENTS.md](REQUIREMENTS.md)를 기준으로 봅니다.

### 도서 서비스 사례를 참고한 제품 정책

현재 정책의 상세 체크리스트는 [REQUIREMENTS.md](REQUIREMENTS.md)를 기준으로 합니다. 검색 버튼/IME 확정, 정가 범위의 적용 버튼, 도서별 저장 중 버튼 제한과 화면 이동 허용, 최소 상태와 상세 스냅샷 복원, 안정적인 1-pane 우선으로 정리했습니다.

근거는 확인한 범위에만 적용합니다.

- [교보문고 웹 검색](https://search.kyobobook.co.kr/search?keyword=Search&pbcm=Search): 가격 범위 입력과 적용 UI를 참고했습니다. 교보의 정가/판매가 기준까지 확인한 것은 아닙니다. 정가 선택과 0원·가격 없음 규칙은 이 앱의 정책이며 [Kakao 가격 필드 명세](https://developers.kakao.com/docs/ko/daum-search/dev-guide#search-book)를 바탕으로 구분합니다.
- [리디 공식 위시리스트 안내](https://ridihelp.ridibooks.com/support/solutions/articles/154000207731): 상세에서 관심 도서를 추가하고 MY에서 확인하는 사용자 흐름을 참고합니다. 내부 DB 쓰기 scope·순서·pop 처리를 알려 주는 근거는 아닙니다.
- 검색 버튼/IME 방식은 일관된 확정 검색을 위한 설계 선택입니다. 현재 유명 앱의 실제 검색 트리거를 직접 검증했다고 주장하지 않습니다.
- 복원 범위는 [Android 상태 저장 지침](https://developer.android.com/develop/ui/compose/state-saving)에 맞춘 기술 정책입니다. 다른 도서 앱의 프로세스 종료 동작을 확인한 결과가 아닙니다.
- 대화면 대응과 2-pane 분리는 [Android list-detail 가이드](https://developer.android.com/develop/ui/compose/layouts/adaptive/list-detail)를 참고합니다. 2-pane은 가능한 패턴이며 이번 과제의 필수 레이아웃으로 해석하지 않습니다.

### 첫 번째 중단 조건: 빌드 조합

요청값은 Gradle **9.4.1**, AGP **9.2.1**, Kotlin **2.4.0**, KSP 사용 시 **2.3.10**, JDK **17**, compileSdk **37**입니다. 최신 안정 Android Studio를 사용합니다.

이 문서는 이 조합을 호환된다고 보증하지 않습니다. 구현 시작 시 다음을 먼저 확인합니다.

1. 요청된 정확한 버전의 배포 여부와 공식 호환 범위를 확인합니다. 최신 릴리스 페이지가 다른 버전을 가리키더라도 요청값을 덮어쓰지 않습니다.
2. AGP의 Kotlin 통합 방식, Kotlin/Compose 컴파일러 플러그인, KSP/Room 조합, Kotlin 2.4.0과 Metro 컴파일러 플러그인 호환성을 함께 확인합니다. 참고 프로젝트의 플러그인 선언을 그대로 복사하지 않습니다.
3. Navigation 3 / retain / Compose / Soil / Room / Retrofit / OkHttp / 직렬화 / Metro / Coil의 실제 사용할 버전을 고정합니다. 참조 앱이 alpha/beta를 썼다는 이유로 동일 버전을 선택하지 않습니다.
4. 빈 Compose 화면과 최소 Room 모델까지 추가한 작은 수직 단면으로 Wrapper 빌드를 실행합니다.
5. 충돌하면 실패 단계, 공식 근거, 가능한 대안을 기록하고 버전 변경 여부를 결정합니다. 다른 버전으로 몰래 빌드한 뒤 요구값을 만족했다고 쓰지 않습니다.

산출물은 실제 `java -version`, `./gradlew --version`, 의존성 버전, `./gradlew assembleDebug` 결과입니다. API 키 없이 가능한 컴파일 검증과 실제 API 통신 검증을 구분합니다.

## 3. 최소 아키텍처

선택한 기술의 역할·데이터 흐름·상태 소유권·테스트 경계는 별도 [ARCHITECTURE.md](ARCHITECTURE.md)에 정리합니다. 이 문서에서는 구현 순서와 위험·참고 코드에 집중합니다.

### 책임 구분

다음 경계를 Gradle 모듈로 표현합니다. 모듈별 의존 방향은 [SPEC](SPEC.md) 2절에 있습니다.

- `app`: 앱 조립, 탭, Navigation 3 백스택, 의존성 연결
- `feature/search`: 입력·정렬·검색 결과·페이지 로딩 상태
- `feature/favorites`: 로컬 검색·정렬·가격 필터·목록 상태
- `feature/detail`: 도서 정보와 즐겨찾기 명령
- `core/model`: UI/DB/네트워크 DTO와 분리한 도서 및 검색 조건 모델
- `core/data`: 데이터 접근 계약과 구현, 네트워크·Room 경계. Soil typed-key로 책임을 표현하고 단순 위임 Repository는 생략
- `core/ui`: 여러 화면에서 실제로 공유하는 카드·상태 UI

feature 간 직접 구현 참조를 피합니다. 앱이 내비게이션을 조립하고, 기능은 결정된 데이터 접근 계약을 통해 데이터를 읽습니다. 네트워크 DTO나 Room Entity가 그대로 화면 계약이 되지 않도록 변환 경계를 둡니다. 단순 위임뿐인 UseCase, 기능마다 동일한 추상화, 과도한 DI/KSP 설정은 추가하지 않습니다.

공통 계약은 Soil key로 표현합니다. 검색 InfiniteQuery key(query, sort), 즐겨찾기 ID 집합·목록·단건 Subscription key, `setFavorite(book, favorite)` Mutation key입니다. 실제 타입 이름은 구현 시 정합니다.

### Soil typed-key와 데이터 접근 책임

사용자가 작은 Soil 데이터 계층을 선택했습니다. 검색은 typed Query/InfiniteQuery key에서 Retrofit 호출로 연결합니다. 로컬은 Subscription에서 Room Flow를 노출하고 Mutation에서 Room에 쓰는 구성으로 진행합니다. DroidKaigi의 typed-key 패턴을 참고하되 실제 사용 API와 버전은 구현 전 확인합니다.

이미 데이터 접근 책임을 가진 key를 단순히 전달하는 Repository는 추가하지 않습니다. 즐겨찾기의 확정 상태는 Room에 두고, Soil의 메모리 캐시를 별도 진실 공급원으로 만들지 않습니다. 일반 Repository는 검토한 대안으로 기록하되 현재 구현 계획에는 포함하지 않습니다.

학습 가치도 고려한 선택이며 provider 범위, 취소·키 변경, 재시도·무효화, 테스트에서의 의존성 교체를 먼저 확인합니다. 이 선택이 기존 화면·코루틴 수명 계약을 대신하지는 않습니다. DroidKaigi의 영속 캐시는 별도 `PersistedQueryKey` 래퍼 구현을 포함하므로 Soil 사용만으로 디스크 캐시가 자동 제공된다고 가정하지 않습니다. 검색 결과 디스크 캐시는 계속 후순위입니다. KMP와 Circuit은 도입하지 않습니다. Metro는 선택했지만 DroidKaigi의 전체 DI 인프라를 복제하지 않고 최소 그래프로 구성합니다.

### 과거 경험을 반영한 로컬 검증

[기존 Soil 적용 기록(2025-09-30)](https://velog.io/@mraz3068/Soil-Apply)에는 Room을 Soil Mutation/Subscription으로 감싼 뒤 로컬 Repository로 되돌린 경험이 있습니다. Flow·캐시 책임 중복에 대한 우려와 관찰한 느린 반영이 기록되어 있으나, 이를 Soil 자체의 결함이나 필연적인 이중 캐시로 단정하지 않습니다.

사용자는 DroidKaigi 2026이 이전 글의 여러 의문을 해소한 참고 구조라고 설명했습니다. 이에 따라 선택한 원격 InfiniteQuery와 로컬 Subscription/Mutation→Room 구성을 유지합니다. 과거 경험은 저장 완료부터 화면 반영까지의 순서·지연, 중복 관찰, 구독 수명·재구독·화면 이동·오류 테스트에 반영합니다. 직접 Room Flow와의 비교를 필수 관문으로 만들거나 Repository 재도입을 요구하지 않습니다.

공식 철학과 사용자 글의 의미, Retrofit 연결·재시도 경계는 [ARCHITECTURE.md](ARCHITECTURE.md)에 정리합니다.

### 여러 사람이 확장하는 기준

- 검색 API/페이지네이션 담당과 즐겨찾기 저장 담당이 같은 화면 파일을 수정하지 않도록 계약을 분리합니다.
- 목록 카드 모델, ID 규칙, 오류 타입을 먼저 합의합니다.
- Soil provider/key 경계에서 Fake key 또는 Fake 데이터 소스로 의존성을 교체해 feature 상태 전이를 테스트할 수 있게 합니다. 구체적인 교체 API는 선택한 Soil 버전에서 확인합니다.
- 새 기능을 위해 DB·네트워크·UI 전역 상태를 동시에 수정해야 한다면 경계를 다시 검토합니다.
- 모듈은 위 9개로 시작합니다. 기능이 늘면 feature 모듈을 추가하고, core를 더 쪼개는 일은 실제 충돌이 생긴 뒤에 합니다.

## 4. retain, 내비게이션, 코루틴 수명

### 유지와 복원을 분리

retain으로 보존할 객체와 프로세스 종료 후 복원할 값을 분리합니다.

| 데이터 | 소유 위치 | 시스템 프로세스 재생성 후 |
| --- | --- | --- |
| 검색/즐겨찾기 상태 소유자 | 내비게이션 엔트리 범위의 retain | 새로 생성 |
| 진행 중 검색 작업·메모리 결과 | 상태 소유자 범위 | 소멸. 조건으로 재요청 |
| 검색어·정렬·가격 필터·선택 탭 | saveable 최소 상태 | 복원 후 재조회·재계산 |
| 상세 도서 스냅샷 | 저장 가능한 내비게이션 키 | 네트워크 없이 다시 표시. Room 값이 있으면 우선 |
| 즐겨찾기 도서 정보 | Room | 로컬 복원 |
| 표지 이미지 | 이미지 로더 캐시, 없으면 대체 UI | 캐시 존재를 보장하지 않음 |

검색 결과 전체나 거대한 도서 목록을 saved state에 넣지 않습니다. 시스템 프로세스 재생성 시 조건·탭·상세 엔트리를 복원하고 검색은 재조회합니다. 상세는 도서 한 권의 스냅샷을 키에 담아 재조회 없이 표시합니다. Kakao API에는 단건 조회가 없기 때문입니다. 복원을 위해 자동으로 즐겨찾기에 저장하지 않습니다. DB에 저장된 도서는 네트워크 없이 복원합니다. 강제 종료 후 콜드 스타트에도 같은 상태 복원을 보장하는 것은 아닙니다.

### Navigation 3 연결

DroidKaigi 예제에서 가져올 것은 엔트리별 retain 저장소와 제거 시 정리하는 작은 패턴입니다. `entry.contentKey` 기준으로 상태를 나누고 엔트리가 pop되면 해당 자원을 해제하는 방향을 검토합니다. saveable 상태 보관은 별도 책임입니다.

- 서로 다른 상세 엔트리가 동일한 상태 객체를 공유하지 않도록 키를 설계합니다.
- 상세에서 목록으로 돌아올 때 목록 상태를 유지합니다.
- 엔트리를 완전히 제거하면 불필요한 Job·참조를 정리합니다.
- 화면 크기 변화로 엔트리 콘텐츠의 composition 위치가 달라질 때도 상태가 유지되는지 확인합니다.
- 탭 전환 정책은 [SPEC](SPEC.md) 7절의 백스택 규칙을 따릅니다. 두 탭 루트를 백스택에 유지해 입력·정렬·스크롤 상태를 보존하고, 탭을 전환하면 이전 탭의 상세는 정리합니다.

### 코루틴 소유권

`retain { channel }`로 채널을 유지했다고 `LaunchedEffect` 소비자가 시작한 작업까지 유지되는 것은 아닙니다. 소비자가 composition에서 벗어나면 해당 작업의 수명도 영향을 받습니다.

구현 시 각 작업에 대해 “누가 시작하고, 누가 취소하고, 언제 끝나는가”를 정합니다.

- 검색: Soil query key 범위. 새 조건이 확정되면 다른 key를 구독하므로 이전 결과가 섞이지 않음을 테스트로 확인
- 즐겨찾기 쓰기: 앱 그래프 싱글턴 `FavoriteWriter`가 `ApplicationScope`에서 Mutex로 직렬화. 화면 pop 이후에도 시작한 쓰기는 완료
- 이벤트: 순서가 중요한 명령은 직렬화. UI 이벤트를 무조건 동시 실행하지 않음
- 취소: `CancellationException`을 일반 실패로 바꾸지 않고 재전파
- 전달: `trySend` 반환값을 무시하지 않음. 중요한 저장 명령은 유실 가능한 UI 이벤트만으로 처리하지 않음

즐겨찾기의 짧은 DB 쓰기는 화면 pop과 독립적인 명시적 scope에서 완료합니다. 해당 도서 버튼만 처리 중 비활성화하고 화면 이동은 허용하며, 책별 명령 순서를 지킵니다. 이 scope의 소유자·종료·오류 전달을 정의하고 `GlobalScope`로 숨기지 않습니다. 프로세스가 종료되면 미완료 쓰기까지 완료된다고 보장하지 않으며 확정 DB 상태만 읽습니다.

## 5. 도서와 즐겨찾기의 상태 기준

### ID와 로컬 모델

검색 결과와 즐겨찾기를 같은 ID로 연결해야 합니다. ISBN13 우선, 없으면 ISBN10, 그다음 url, 마지막으로 제목·출판사·출간일 조합을 씁니다. 규칙과 한계는 [SPEC](SPEC.md) 5절에 있습니다. title만 ID로 삼지 않습니다.

최소 저장 정보는 ID, 원본 ISBN, 제목, 저자, 출판사, 출판일, 정가/판매가, 표지 URL, 필요한 상세 정보, 즐겨찾기 여부 또는 저장 시각입니다. Entity의 필드·DB 스키마는 구현 시 확정합니다.

Room Flow를 즐겨찾기 상태의 단일 기준으로 둡니다. 원격 검색 결과와 DB 즐겨찾기 ID 집합을 결합해 카드 상태를 만들고 상세도 동일한 DB 상태를 관찰합니다. 한 화면에서만 boolean을 뒤집고 다른 화면과 별도로 유지하지 않습니다.

### 빠른 토글과 실패

현재 정책은 해당 도서 버튼의 저장 중 비활성화와 책별 명령 순서 보장입니다. 다른 조작과 화면 이동은 허용하고 상태 반영은 DB 성공에 맞춥니다. 낙관적 UI는 현재 기본안으로 채택하지 않습니다.

- `toggle` 대신 `setFavorite(book, favorite)`처럼 목표 상태가 명시적인 명령을 씁니다.
- 모든 즐겨찾기 쓰기를 하나의 Mutex로 직렬화합니다.
- 성공 메시지는 저장 완료 뒤에 표시합니다.
- 저장 실패 시 기존 확정 상태를 유지하고 다시 시도할 수 있게 합니다.
- 즐겨찾기를 해제해도 상세는 내비게이션 키의 스냅샷으로 같은 정보를 계속 표시합니다.

### 로컬 검색·정렬·필터

정가 최소·최대 금액을 입력하고 적용 버튼으로 확정합니다. 양끝 경계값은 포함하고 빈칸은 해당 방향 제한 없음으로 처리합니다. 최소 > 최대는 적용을 막고 입력 오류로 안내합니다.

누락·음수 가격은 정보 없음으로 분류하고 범위 미설정 시 포함, 범위 설정 시 제외합니다. 0원은 유효한 가격입니다. 이 정책을 UI와 테스트에 동일하게 반영합니다. title 정렬은 대소문자·한글·동일 제목의 tie-breaker까지 정의하고, SQLite 기본 정렬이 자연스러운 한국어 사전순과 같다고 가정하지 않습니다.

## 6. 검색과 페이지네이션

### 네트워크 클라이언트 선택

사용자가 Ktor보다 Retrofit 사용 경험이 많다는 점을 고려해 **Retrofit을 선택**했습니다. 익숙한 Android REST 구현 방식으로 일정 내 안정적으로 검증하기 위한 판단입니다. OkHttp 기반 통신과 kotlinx.serialization 컨버터 구성도 사용자가 선택했습니다. 실제 버전은 호환성 확인 후 확정합니다. Ktor도 테스트 가능한 대안이지만, 참고 저장소의 클라이언트를 그대로 재사용하기 위해 선택하지는 않습니다.

API 계층은 MockWebServer로 요청 파라미터·인증 헤더 구성·응답 변환·HTTP 오류를 확인하는 안을 권장합니다. Presenter 테스트는 네트워크 서버 대신 교체 가능한 Fake 의존성으로 모델 방출과 상태 전이를 검증합니다. Soil provider/key가 사용하는 데이터 의존성을 Fake key 또는 Fake 데이터 소스로 교체하며, 테스트를 위해 별도 Repository를 추가하지 않습니다. 실제 API 키를 테스트 코드·fixture·로그에 넣지 않습니다. API 계층 테스트와 실제 서비스 통신 확인은 구분합니다.

Retrofit은 현재 확인한 Soil 공식 모듈 목록의 전용 통합 모듈이 아닙니다. 범용 `buildQueryKey`의 `suspend QueryReceiver.() -> T` fetch 안에서 주입한 Retrofit suspend 함수를 호출하는 방식이며, 이를 위해 별도의 커스텀 어댑터가 필수인 것은 아닙니다. 실제 버전 조합의 컴파일·실행은 아직 검증하지 않았습니다. Ktor receiver도 HttpClient 주입 도구이며 예외를 자동 변환해 주는 것으로 가정하지 않습니다.

기본 재시도는 Soil의 `Retryable` 및 `canRetry` 조건을 사용하므로, Retrofit의 일반 `HttpException`이나 `IOException`이 자동 재시도된다고 쓰지 않습니다. `shouldRetry`를 명시하거나 재시도 가능한 오류를 매핑하는 정책을 선택해 테스트합니다. `CancellationException`은 재전파합니다. 인증 실패·일시적 실패·취소·수동 재시도의 동작을 나누어 확인합니다.

Query/Mutation의 실패·취소 처리와 runSuspendCatching의 사용 경계는 [아키텍처의 취소·Result 설명](ARCHITECTURE.md)에 따릅니다. Soil 반환값에 Result.failure를 넣어 실패를 성공 데이터로 만들지 않습니다. helper 자체를 일괄 도입하지 않고 취소·운영 오류·재시도 정책을 각각 테스트합니다.

### API 계약

[공식 책 검색 문서](https://developers.kakao.com/docs/ko/daum-search/dev-guide#search-book)를 기준으로 `GET /v3/search/book`에 query, sort, page, size를 전달합니다. sort는 accuracy 또는 latest이며 latest는 발간일순입니다. 이 앱은 size=20, page=1부터 시작합니다. 종료 여부는 `meta.is_end`를 사용합니다. 미디어 검색의 recency나 자체 API의 start/maxResults 규칙을 가져오지 않습니다.

### 입력과 확정 조건 분리

- `inputQuery`: 사용자가 현재 편집 중인 값
- `activeCriteria`: 현재 결과를 만든 검색어·정렬의 불변 스냅샷
- query key: 확정 query/sort를 포함하는 검색 정체성. 늦은 응답은 key가 다르므로 현재 화면에 섞이지 않아야 하며, 테스트로 확인합니다. 테스트가 실패할 때만 보조 방어를 추가합니다.

검색 버튼 또는 키보드 IME 제출 시 검색어를 확정합니다. 로컬 검색도 같은 확정 방식을 적용합니다. append는 편집 중인 입력이 아니라 activeCriteria를 사용합니다.

새 검색/정렬은 다른 query/sort key로 처리합니다. 이전 관찰·요청의 취소와 늦은 응답 격리가 선택한 Soil 구성에서 어떻게 동작하는지 테스트합니다. 라이브러리와 별도의 페이지 상태를 중복 관리하지 않습니다. 빈 검색어는 key를 구독하지 않는 "검색 전" 상태로 명시적으로 전이하여 이전 결과를 남기지 않습니다.

### InfiniteQuery 페이지 계약과 검증할 불변식

InfiniteQuery에서 확정 query/sort를 key에 포함하고 size=20을 고정합니다. 페이지 응답의 문서 목록과 meta.is_end 등 필요한 메타데이터를 함께 보존합니다. 최초/추가 로딩·오류를 UI에서 분리하며, 다음 불변식은 라이브러리가 자동 보장한다고 주장하지 않고 실제 구성의 테스트로 확인합니다.

1. 같은 활성 query key에서 중복 append가 발생하지 않도록 합니다.
2. 성공한 페이지에 대해서만 nextPage를 증가시킵니다.
3. 실패하면 동일 조건·동일 페이지를 재시도합니다.
4. 응답의 is_end가 true면 개수가 20개여도 끝냅니다.
5. ID 중복 제거는 표시 결과에만 적용합니다. 중복 제거 후 개수로 페이지 번호나 종료 여부를 계산하지 않습니다.
6. append 오류에서는 기존 목록을 유지하고 하단에 재시도 UI를 둡니다.
7. 새 검색에서는 이전 append 오류·푸터·종료 상태를 함께 초기화합니다.
8. 화면을 채우지 못한 결과에서도 다음 페이지가 있으면 로드할 수 있어야 합니다. 보이는 아이템 개수에 임의의 최소 문턱을 두지 않습니다.
9. API 상한과 종료 플래그를 함께 방어하여 무한 요청을 막습니다. 비정상 빈 응답·중복 페이지에 대한 정지/수동 재시도 정책도 테스트합니다.

### 검토했으나 선택하지 않은 대안

Paging 3는 로드 상태·취소·재시도 구조가 표준화되어 있다는 장점이 있습니다. 커스텀 구현은 작은 API 계약과 retain 상태 모델을 직접 이해하고 테스트하기 쉽지만, 경합과 종료 판정을 직접 책임져야 합니다.

커스텀 구현은 초기 검토안이었고, 현재 선택은 Soil InfiniteQuery입니다. Day 1에 key 격리, 페이지 종료, 실패 후 동일 페이지 재시도를 검증합니다. Paging 3나 일반 Repository로 전환할 필요가 생기면 이유를 기록하고 별도로 결정합니다. 참고 코드의 pageSize=10 또는 loadSize 산술식을 복사하지 않습니다.

## 7. 네트워크, 오프라인, 적응형 화면

### 최소 오프라인 범위

- 즐겨찾기 목록·검색·정렬·가격 필터·저장 도서 상세는 Room만으로 동작
- 온라인 검색에서 이미 받은 결과는 같은 세션의 오류 시 유지
- 최초 오류와 추가 로딩 오류를 분리하고 재시도 제공
- 연결 여부 사전 체크만 믿지 않고 실제 요청 실패를 처리
- 타임아웃·인증 실패·일시적 서버 실패 등을 사용자가 이해할 수 있게 구분
- 표지는 실패/캐시 없음 상태의 대체 UI 제공

검색 결과 전체의 디스크 캐시, 자동 갱신, TTL, 캐시 크기 제한은 후순위입니다. 이를 도입한다면 오래된 데이터 표시·조회 조건별 저장·DB 마이그레이션·삭제 정책까지 묶어서 설계합니다. “오프라인 지원”이라는 한 줄로 미구현 범위를 감추지 않습니다.

### 화면 크기

태블릿·폴더블 대응은 원문 필수입니다. 세로 고정을 제거하고 창 크기를 기준으로 구성합니다. 최소 목표는 compact에서 한 화면 흐름, 넓은 화면에서 읽기 쉬운 너비·여백·탐색 배치입니다. 현재 정책은 큰 화면에서도 안정적인 1-pane을 우선하고 list-detail 2-pane은 후순위로 두는 것입니다.

태블릿·폴더블 지원을 단순히 카드 열 수 증가로 끝내지 않습니다. 회전/분할 화면/펼침·접힘 중 입력·목록·상세 선택 유지, 키보드와 스크롤, 큰 글꼴, 힌지에 가려지는 주요 조작을 확인합니다. 실제 기기가 없으면 에뮬레이터로 확인한 범위와 미확인 범위를 구분해 적습니다.

## 8. 참고 저장소에서 가져올 것과 버릴 것

참고 기준 커밋은 아래에 고정합니다. 읽은 코드의 패턴을 판단 자료로 사용하며, 대량 복사 전에 라이선스와 출처 표기 의무를 확인합니다.

### easyhooon/BookSearch

[기준 커밋 2533f396](https://github.com/easyhooon/BookSearch/tree/2533f396a1e1e70fed37531c1d92edd8b2a69d63)

- 활용: 도서 검색의 기능 흐름, Compose Presenter, Room/Flow 즐겨찾기 구조. 참고 코드의 네트워크 구현은 Ktor지만 이번 앱은 Retrofit으로 구현
- README의 Retrofit/ViewModel 설명과 실제 Ktor/Presenter 구현이 달라 README만으로 기술을 판단하지 않음
- [SearchBooksQuery.kt](https://github.com/easyhooon/BookSearch/blob/2533f396a1e1e70fed37531c1d92edd8b2a69d63/core/data/src/main/kotlin/com/easyhooon/booksearch/core/data/query/SearchBooksQuery.kt): meta.isEnd를 버리고 결과 개수 및 chunks.size 기반으로 다음 페이지를 판단하는 부분을 개선
- [DetailPresenter.kt](https://github.com/easyhooon/BookSearch/blob/2533f396a1e1e70fed37531c1d92edd8b2a69d63/feature/detail/src/main/kotlin/com/easyhooon/booksearch/feature/detail/presenter/DetailPresenter.kt): 저장 성공 전 낙관적 상태/메시지, 병렬 쓰기, DB 상태 관찰·롤백 부재를 답습하지 않음
- [Manifest](https://github.com/easyhooon/BookSearch/blob/2533f396a1e1e70fed37531c1d92edd8b2a69d63/feature/main/src/main/AndroidManifest.xml): 세로 고정과 휴대폰 중심 화면은 이번 조건에 맞게 변경
- 로컬 제목 검색·정렬은 참고하되 가격이 0인지 여부만 거르는 구현을 그대로 가격 필터로 채택하지 않음
- 확인 범위에서 테스트/CI 및 영속 검색 캐시가 확인되지 않음. 이번 검증을 대신하는 근거로 사용하지 않음
- Rin/Soil 등 프레임워크 전체를 복제하지 않고 필요한 개념만 재구성

### easyhooon/KakaoMediaSearchApp2

[기준 커밋 ba5ae0af](https://github.com/easyhooon/KakaoMediaSearchApp2/tree/ba5ae0af89532f1ac912d2892fc989ca29f4fd63)

- 활용: SavedStateHandle 입력 복원, debounce/combine/flatMapLatest, Paging cachedIn, load-state UI라는 문제 해결 방식
- 이번 retain 설계에서는 상태 저장 위치와 scope를 다시 정함. ViewModel 코드를 통째로 이식하지 않음
- 주의: blank query를 flatMapLatest 앞에서 제외하면 이전 결과가 남을 수 있음
- pageSize=10 및 loadSize 계산은 도서 API 20개 계약에 그대로 적용하지 않음
- broad Exception wrapper로 취소까지 오류 처리하지 않음
- 미디어 검색 앱이므로 책 검색 DTO·정렬 값·필터 요구의 근거가 아님

### YAPP-Github/Reed-Android

[기준 커밋 74a8ce90](https://github.com/YAPP-Github/Reed-Android/tree/74a8ce9009260859f0a2e468ac1d6db928e62f50)

- 활용: BookSearchPresenter의 최초/추가 로딩 분리, 하단 재시도, 성공 후 페이지 증가, 취소 예외 재전파
- Circuit 라이브러리는 도입하지 않음. 이 저장소는 상태 전이·오류 처리 패턴을 참고하는 자료로만 활용
- 개선: 이전 검색 Job 경합, append가 편집 중 입력을 사용하는 문제, 푸터 상태 초기화 누락, 중복 제거 부재
- InfinityLazyColumn의 보이는 아이템 수 ≤6에서 자동 로드를 막는 조건을 그대로 쓰지 않음
- 자체 API의 start/maxResults/lastPage는 Kakao의 page/size/meta.is_end와 다른 계약

### DroidKaigi/conference-app-2026

[기준 커밋 300a81bc](https://github.com/DroidKaigi/conference-app-2026/tree/300a81bc575598c37b06814ed9e81099c162b9e5)

- 활용: RetainNavEntryDecorator의 entry.contentKey별 저장소와 onPop clearChild, KaigiNavDisplay의 별도 saveable decorator, 저장 가능한 KaigiBackStack 키
- 참고 문서: 기준 커밋의 `docs/navigation-retain-entry-decorator.md`. 엔트리 콘텐츠가 다른 composition으로 옮겨질 때 상태가 사라지는 위험 설명
- SearchScreenPresenter의 retain query만으로 프로세스 종료 복원이 되는 것은 아님
- 화면 그래프의 @GraphExtension.Factory 구현은 Metro 컴파일러가 생성하고 NavEntryProvider에서 생성 결과를 retain하는 패턴을 참고. 2025의 커스텀 KSP remember 편의 함수 생성기까지 복제하지 않음
- 취소 처리는 helper별로 확인. 같은 기준 커밋에서 ScreenChannel과 PersistedTimetableReader는 취소를 재전파하지만, PersistedQueryKey의 suspend FileStorage get/put을 감싼 일반 runCatching 경로는 취소를 별도로 걸러내지 않음. 참고 코드 전체가 취소 안전하다고 가정하지 않으며, 미채택 영속 캐시 wrapper도 그대로 복사하지 않음
- ScreenChannel의 retained channel + LaunchedEffect 소비 구조에서 실행 중 작업 소유권, 이벤트 동시 실행, trySend 실패를 별도로 확인
- Metro는 필요한 최소 그래프에 사용하되 DroidKaigi의 전체 그래프·KMP 인프라는 복제하지 않음. Soil은 작은 typed-key 범위로 적용하며 Metro 컴파일러 플러그인과 Room용 KSP를 구분
- Android 전용으로 구현하므로 KMP 프로젝트 설정·source set·expect/actual·플랫폼 추상화를 가져오지 않음. retain/Navigation 3 소유권과 Presenter·테스트 패턴만 선택적으로 참고
- 참조 코드의 alpha/beta 버전 사용을 이 프로젝트의 호환성 보증으로 해석하지 않음
- 탭 전환 코드(`NavigatorEffect`의 SelectTab 처리)는 그대로 쓰지 않음. 기준 시점에 열린 이슈 [DroidKaigi/conference-app-2026#344](https://github.com/DroidKaigi/conference-app-2026/issues/344)(같은 탭을 재선택하면 백스택 순서가 뒤집히고, 이후 뒤로 가기로 앱 종료)와 [DroidKaigi/conference-app-2026#226](https://github.com/DroidKaigi/conference-app-2026/issues/226)(상세 중복 push)가 있음. 규칙은 [SPEC](SPEC.md) 7절에서 직접 정하고 테스트함
- 저장소 규모: 2026-10-07 기준 2026은 스타 83·기여자 54로, 2025(228·109)와 2024(491·133)보다 작음. 앱 전체의 완성도가 아니라 특정 패턴(retain decorator, Root/Presenter, Soil typed-key, Metro factory + retain)만 참고하므로 영향은 제한적이라고 판단함. 이 패턴들은 main의 여러 feature 모듈에 적용되어 있음. Kotlin 2.4.0 기준의 참고 버전 조합은 2026이 유일함
- 2026과 2025 모두 즐겨찾기를 Room이 아닌 저장소에 둠. Soil Mutation/Subscription과 Room의 연결은 참고 사례 없이 이 앱이 직접 확장하는 부분이므로 테스트로 검증함

### DroidKaigi/conference-app-2025

[기준 커밋 07b46e65](https://github.com/DroidKaigi/conference-app-2025/tree/07b46e6585ea6bdafe8a52142d1dd456fddda387)

- 확인: Metro 0.5.2, Soil 1.0.0-alpha12를 실제로 사용함. 즐겨찾기는 DataStore 기반이며 `buildMutationKey`로 만든 전역 Mutation key 하나로 토글함
- 차이: Compose retain 대신 Rin의 `rememberRetained`, Navigation 3 alpha05, Ktor + Ktorfit, Kotlin 2.2.0을 씀. 화면 컨텍스트용 커스텀 KSP 생성기가 있음
- 활용: Soil typed-key·Mutation key·Presenter 패턴이 2026과 같은 방향인지 교차 확인하는 용도로만 씀. 버전 조합, retain 수명, 내비게이션 근거로는 쓰지 않음

### 기존 retain PoC

사용자가 Navigation 3와 오류 처리를 상당 부분 시험했다고 공유한 코드입니다. 원격 접근이 되지 않아 내부 구현은 확인하지 못했습니다. 구현 근거로 쓰지 않으며, 이 앱의 수명 검증은 자체 테스트로 수행합니다.

사용자의 [retain 글(2025-10-21)](https://velog.io/@mraz3068/Compose-Retain-API)은 유지·프로세스 복원의 차이를 참고하되 당시 preview 설명을 현재 버전 보증으로 사용하지 않습니다. factory와 수명 연결의 구체적인 경계는 [아키텍처](ARCHITECTURE.md)에 정리합니다.

## 9. 실행 계획과 제출 버퍼

구현 착수는 2026-10-07(수) 저녁입니다. 아래 시간은 실행 제안입니다. 시작 시각이 밀리면 선택 기능을 줄이고, 빌드·핵심 기능·검증 시간을 보호합니다.

### 10월 7일(수) 저녁: 빌드 조합

1. 지정 버전의 Wrapper·AGP·Kotlin·KSP·JDK 17 조합으로 빈 프로젝트와 9개 모듈 골격 빌드
2. Metro·Compose·Navigation 3·retain·Soil·Room·Retrofit·Coil 후보 버전을 추가하며 `assembleDebug` 확인
3. 최소 retain + Navigation 3 화면 전환과 retain decorator 연결

종료 기준: 지정 조합의 실제 빌드 결과가 있습니다. 충돌이 있으면 실패 로그·근거·대안을 기록합니다.

### 10월 8일(목): 검색과 즐겨찾기 수직 단면

1. 순수 규칙: ID, 가격 표시·필터, 정렬, 날짜. 단위 테스트를 같이 작성
2. Retrofit API와 MockWebServer 테스트, 오류 분류
3. 검색 InfiniteQuery key와 페이징 테스트(마지막 20개 + is_end, 실패 후 같은 페이지, 중복 ID)
4. 검색 화면 Root·Presenter·Screen과 Presenter 테스트
5. Room DB·DAO·`FavoriteWriter`·Subscription/Mutation key와 Room 통합 테스트
6. 즐겨찾기 화면의 로컬 검색·정렬·가격 필터

종료 기준: 검색 → 즐겨찾기 저장 → 즐겨찾기 탭 확인이 실제 API로 동작합니다.

### 10월 9일(금): 상세, 적응형, 수명 검증

1. 상세 화면과 스냅샷 NavKey, 백스택 규칙과 테스트
2. NavigationSuiteScaffold와 적응형 그리드, 창 크기별 Compose 테스트
3. retain 유지·pop 정리, 쓰기 중 pop, 프로세스 재생성 수동 확인
4. 오류·빈 상태·키 미설정 상태 점검
5. README·REQUIREMENTS 검증 기록, 제출 ZIP/APK 후보 생성과 별도 디렉터리 빌드 확인

목표: 금요일 밤까지 제출 후보를 고정합니다. 테스트 명령은 실제 프로젝트의 task 이름을 확인한 뒤 기록합니다. 존재와 실행 여부를 확인하지 않은 task를 통과했다고 쓰지 않습니다.

### 10월 10일(토): 제출 전 버퍼

- 09:30~11:00: 치명적 결함 수정만 허용, 변경된 경로 재검증
- 11:00~12:00: 최종 ZIP 해제 빌드, APK 설치, .git/local.properties/제외 디렉터리 점검
- 12:00~13:00: README·민감정보 최종 확인, 필요하면 3분 이내 영상
- **13:30 이전 사용자 제출 완료 목표**, 14:30 마감까지 전송/재업로드 여유 확보

업로드와 실제 키 전달은 사용자가 최종 확인합니다. 빌드 실패나 핵심 데이터 손실이 남아 있으면 영상·검색 캐시·추가 애니메이션보다 먼저 해결합니다.

## 10. 테스트 설계

구현 후 [요구사항 체크리스트](REQUIREMENTS.md)의 R-ID를 하나씩 검증합니다. 같은 문서의 기록란에 구현 상태·실제 검증 결과·테스트 이름/명령·환경·증거·미충족 사항을 남기며, 실행 증거 없이 체크하지 않습니다.

테스트 코드 작성은 사용자가 추가한 필수 요구사항입니다. 원 과제의 명시 조건과 구분하되, 구현 완료 기준에 포함합니다. 10월 8일의 핵심 단위·상태 경합·페이징·Room 통합 테스트, 10월 9일의 Compose 내비게이션·적응형 화면·retain 수명 테스트와 실제 실행·회귀 검증 시간을 확보합니다. 선택 기능을 줄이더라도 이 범위를 먼저 보호합니다.

테스트 작성과 실행 성공은 별개입니다. 명령·환경·실제 결과를 남기고, 실행이 막히면 코드 작성 상태와 미실행 사유를 구분합니다. 프로세스 종료·폴더블 등 자동화 범위 밖의 확인은 수동 검증으로 보완하되 자동 테스트 통과로 표현하지 않습니다. 현재는 문서 단계이며 테스트 코드는 아직 없습니다.

### 상태 관리와 테스트 가능성

사용자는 ViewModel 테스트에서 구성에 따라 viewModelScope, Main 디스패처, SavedStateHandle 등의 준비가 추가될 수 있다는 점을 지적했습니다. retain + Navigation 3 사용과 Circuit 미도입은 확정했으며, 현재 권장안은 DroidKaigi 방식의 얇은 @Composable Presenter입니다. 이는 변경 불가능한 결정이 아니며, 실제 구현과 검증 후 확정합니다. 순수 Kotlin 형태를 만들기 위해 별도의 전체 StateFlow 상태 소유자를 중복으로 둘 필요는 없습니다.

테스트는 다음 층으로 나누는 방향을 권장합니다.

- 순수 비즈니스 규칙·ID·가격 정책·분리 가능한 페이지네이션 로직: 일반 Kotlin 단위 테스트
- Presenter의 모델 방출·이벤트·비동기 상태 전이: 로컬 JVM에서 Molecule + Turbine + kotlinx-coroutines-test를 사용하는 안 검토. Compose runtime/compiler가 필요하지만, 의존성이 허용하면 Android UI나 기기가 본질적으로 필요한 것은 아님
- retain 유지·제거, 백스택, 구성 변경, 프로세스 종료 복원: 실제 호스트와 연결한 별도 통합 검증. UI와 기기 동작도 이 범위에서 확인

[Molecule 공식 문서](https://github.com/cashapp/molecule)는 Composable에서 Flow/StateFlow 모델을 만들고 실행하는 방식을 설명합니다. 구체적인 테스트 라이브러리·버전은 아직 도입하지 않았습니다. Molecule 실행 환경에 실제 호스트와 동등한 retained store가 제공된다고 가정하지 않습니다. 기본 저장소가 forgetful하게 동작하는 구성도 고려해야 하므로, 모델 방출 테스트 통과만으로 호스트의 retain 수명을 검증했다고 판단하지 않습니다.

이는 앞으로 확인할 테스트 설계입니다. retain의 보편적 우위나 이미 실행한 테스트의 성공을 뜻하지 않으며, 동시성·수명 검증 책임도 그대로 남습니다.

| 위험 | 테스트 입력 | 기대 결과 |
| --- | --- | --- |
| 오래된 응답 덮어쓰기 | A 요청 지연 중 B 검색, B 먼저 완료 | B 결과 유지 |
| 검색 초기화 | 결과 표시 중 검색어 전체 삭제 | 이전 요청 취소, 이전 결과 정리 |
| 정렬 경합 | append 중 정렬 교체 | 새 조건 첫 페이지, 이전 append 무시 |
| 페이지 종료 오판 | 20개 + is_end=true | 다음 요청 없음 |
| 재시도 오류 | 2페이지 실패 후 재시도 | 다시 2페이지 요청, 성공 시에만 증가 |
| 중복 로딩 | 스크롤 이벤트 연속 발생 | 같은 페이지 동시 요청 없음 |
| 중복 데이터 | 페이지에 기존 ISBN 반복 | 카드 중복 없음, 페이지 계산 정상 |
| 적은 항목 | 화면에 6개 이하이나 다음 페이지 존재 | 다음 로드 가능 |
| 가격 정책 | 경계값/0원/가격 없음/역전 범위 | 문서화된 정책과 일치 |
| 원격 오류 분류 | HTTP 오류·I/O 실패·취소를 주입 | 명시한 재시도 조건·횟수 준수, 취소 재시도 없음 |
| 저장 실패 | Fake DAO 예외 | 성공 표시 없음, 확정 상태 유지 |
| 연타·뒤로 가기 | 빠른 저장/해제 및 pop | 명령 정책대로 최종 DB 상태 확정 |
| 네트워크 끊김 | 검색 append 실패, 즐겨찾기 이동 | 기존 결과 유지, 로컬 기능 사용 가능 |
| 재시작 | 저장 후 앱 종료·재실행 | 저장 도서와 상세 복원 |
| retain 수명 | 회전, 크기 변경, 상세 진입·복귀, pop | 유지할 상태 유지, 제거된 엔트리 작업 정리 |
| 프로세스 복원 | 백그라운드 프로세스 종료 뒤 복귀 | 최소 상태 복원, 상세는 스냅샷으로 즉시 표시, 검색은 재조회 |

빠른 단위 테스트는 상태 전이·검색 경합·ID/가격 정책에 집중합니다. Room 통합 테스트는 관찰·저장 실패·재실행 영속성 경계를 다룹니다. Compose/기기 테스트는 탭·상세·크기 변화 흐름을 검증합니다. Activity 재생성만 확인하고 프로세스 종료 복원까지 검증했다고 쓰지 않습니다.

## 11. 트레이드오프와 미구현 기록

구현하면서 아래 표를 실제 결과로 갱신합니다.

| 선택 | 기대 효과 | 비용·위험 | 대안/미구현 |
| --- | --- | --- | --- |
| retain + Navigation 3 | 선택한 학습 목표와 화면별 상태 소유 | 수명·정리·복원 설계 책임 | ViewModel은 대안 비교만 기록 |
| Room Flow | 영속성과 화면 간 일관성 | 스키마·KSP·마이그레이션 고려 | 파일/DataStore는 도서 검색 모델에서 제약 |
| Soil InfiniteQuery | typed-key 기반 데이터 접근과 페이징을 함께 학습 | key·provider·취소·페이지 계약 검증 필요 | 커스텀 페이징 / Paging 3는 미채택 대안 |
| 저장 성공 후 상태 반영 권장 | 실패 시 롤백 복잡도 감소 | 반응 지연, 처리 중 UI 필요 | 낙관적 UI는 직렬화·롤백 비용 |
| 원격 결과 메모리 유지 | 오류에서도 현재 목록 유지 | 프로세스 종료 시 손실 | 영속 검색 캐시는 후순위 |
| 9개 Gradle 모듈 | 기능별 협업·의존 방향을 빌드로 강제 | 초기 설정 비용, AGP 9 convention plugin 위험 | 단일 모듈 + 패키지 경계 |
| 기본 적응형 레이아웃 우선 | 넓이/회전 대응을 빨리 검증 | 풍부한 2-pane UX는 제한 | 시간 확보 시 list-detail 확장 |

검토했으나 구현하지 않은 기능은 이유와 사용자 영향까지 적습니다. “시간 부족”만 적기보다 어떤 위험을 줄이기 위해 무엇을 우선했는지 설명합니다.

## 12. AI 활용을 검증 가능한 작업으로 남기기

AI 활용이 허용된 만큼 조사·구현·테스트 제안·리뷰·문서화까지 적극 활용합니다. 다만 구현 여부나 테스트 성공은 실제 결과로만 기록합니다.

- AI 작업은 기능 단위보다 작게 나눕니다. API 계약, 상태 전이, 실패 주입 테스트, UI 검토처럼 입출력과 완료 기준이 분명한 단위가 좋습니다.
- 각 작업에는 요구사항, 변경하지 않을 계약, 확인할 경계값을 줍니다.
- AI의 출력에서 근거와 추측을 구분하고, 실제 코드와 테스트로 대조합니다.
- 개발자는 채택·수정·기각을 결정합니다. 이번 retain 선택처럼 AI 제안과 다른 판단을 한 경우에도 그 이유를 남깁니다.
- 결과는 작은 커밋, 검증 명령, 실제 로그 요약으로 연결합니다. 검증되지 않은 “완료” 표현을 제거합니다.
- 원본 대화 전체나 민감정보 대신 간결한 의사결정 기록을 유지합니다.

구현 단계에서는 API/수명 계약 조사, 경합·저장 실패 테스트, 코드 리뷰, 요구사항 대조·회귀 테스트·문서 불일치 탐지에 활용합니다. 이 항목은 예정된 방식이며, 현재 구현이나 테스트가 수행되었다는 뜻은 아닙니다.

실제 retain 결정 사례와 앞으로 사용할 기록 양식은 [AI_USAGE.md](AI_USAGE.md)에 정리했습니다.

### 저장소 작업 방식

문서와 작업 설명은 한국어로 작성합니다. 이번 과제에서는 이슈를 생성하지 않고 [요구사항 체크리스트](REQUIREMENTS.md)로 진행 상태를 관리합니다. 변경은 작은 기능·수정 단위 커밋으로 남기며, 문서만 바꾸는 커밋에는 Markdown만 포함합니다. 2026-10-07 사용자 요청에 따라 설계·문서화에 이어 앱 구현도 같은 AI 작업 세션(Claude Code, Claude Opus 5.5)이 담당합니다. 사용자는 결정과 검토, 실제 키 설정과 제출을 맡습니다.

PR 사용 여부는 필요와 요청에 따라 결정합니다. 이슈 생략을 PR 전면 금지로 확대하지 않습니다. 커밋·PR에 자동 `Generated-by-Claude` 문구나 AI `Co-authored-by` 트레일러를 넣지 않되, 실제 작성자·출처·라이선스 정보는 보존합니다. 기존 이력도 이를 이유로 재작성하지 않습니다.

AI 활용 자체는 README와 AI_USAGE에 사실대로 남깁니다. 수행한 작업과 예정된 작업, 테스트의 미실행·실패·통과를 구분합니다. 자세한 작업 기준은 [AGENTS.md](../AGENTS.md)를 따릅니다.

## 13. 구현 시 참고할 공식 문서

- [Kakao 책 검색 REST API](https://developers.kakao.com/docs/ko/daum-search/dev-guide#search-book): 요청·응답 계약
- [Compose state lifespans](https://developer.android.com/develop/ui/compose/state-lifespans): remember/retain/saveable 상태 수명 구분
- [Navigation 3 상태 저장](https://developer.android.com/guide/navigation/navigation-3/save-state): 내비게이션 상태 복원
- [Room](https://developer.android.com/training/data-storage/room): 로컬 데이터베이스
- [적응형 Compose UI](https://developer.android.com/develop/ui/compose/layouts/adaptive/get-started-with-adaptive-apps): 창 크기와 화면 구성
- [Android Gradle Plugin 릴리스](https://developer.android.com/build/releases/gradle-plugin): 구현 시 요청된 버전의 릴리스 노트로 이동해 확인
- [Gradle 호환성](https://docs.gradle.org/current/userguide/compatibility.html): 구현 시 사용 버전에 맞는 문서로 확인

공식 문서는 변경될 수 있습니다. 링크가 현재 최신 버전을 보여주는 것과 과제의 지정 버전을 만족하는 것은 별개입니다. 최종 README에는 실제 사용·검증한 버전과 검증 시점을 남깁니다.

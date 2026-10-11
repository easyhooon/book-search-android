# Maestro E2E 검증 범위

기준 main: `0773b9ea62e4fe0736b4ab203c008e5de989abe1`. feature/maestro-e2e 변경입니다. fixture 회귀 검증과 기존 QA 재검증을 구분합니다. 기존 QA를 모두 재검증했다고 주장하지 않습니다.

총 YAML 33개(정상 흐름32개+의도적 실패1개)의 문법을 Maestro 자체 파서로 확인했습니다. 정상 흐름32개는 휴대폰·AVD에서 각각 실제 실행했고 경합은 시간겹침까지 확인했습니다. 기존 QA45행의 원절차 전체를 새로 수행했다는 뜻은 아닙니다.

## 확인 상태

- 공식 Maestro CLI 2.11.0, SM-S936N에서 실행. 검색/정렬/상세/빈 검색, 즐겨찾기/로컬 검색/필터/상세 해제, 마지막 20개 페이지 종료, 서버 오류/재시도 흐름 통과.
- 기존 자동 테스트 145개 --rerun-tasks 새 실행, 실패/오류/skip 0, fixture 계약 테스트 3개 통과. e2e APK와 lintDebug 및 시트 변경 관련 테스트/lint 성공. 268 tasks executed; 이전 캐시 재사용 결과와 구분.
- negative-control은 존재하지 않는 문구를 찾는 틀린 assertion을 검출(exit 1). 실제 앱 결함을 주입하여 검출한 결과는 아님.
- 로컬 stdio MCP 등록 및 initialize/tools/list/list_devices 실응답 확인. Codex 실행 세션의 도구 재로딩과 별개. 로컬 설정은 저장소에 넣지 않음.
- 기본 fixture는 `.e2e` 패키지와 가짜 키 사용. 승인된 실제 API smoke만 로컬 키 사용. 기존 앱/제출 APK/ZIP/원본 키 파일은 변경하지 않음. 비행기 모드·회전·다크 설정은 smoke에서 변경 후 복원 비교 true, 별도 앱 종료·reverse 없음 확인 후 휴대폰 분리 안내. 이후 휴대폰 미사용.

## 기존 adb QA와의 차이

기존 QA는 adb 입력으로 화면을 조작하고 AI가 screenshot/uiautomator 텍스트/요청 로그/PID를 관찰해 판정했습니다. 이번 흐름은 YAML에 선택자·순서·대기·명시적 assertVisible/assertNotVisible을 저장해 같은 절차를 다시 실행하며 JUnit에 성공/실패를 남깁니다. adb도 같은 방식으로 자동화할 수 있지만 선택자·동기화·판정 코드를 직접 구성해야 합니다. 경합 구간은 uiautomator의 현재 선택자 좌표에서 얻은 빠른 adb 입력과 Maestro assertion을 함께 사용하며, 순수 YAML 조작만으로 겹침을 보장했다고 주장하지 않습니다. MCP는 에이전트와 Maestro를 연결하는 수단이며 assertion이나 테스트 커버리지 그 자체가 아닙니다. [공식 MCP 문서](https://docs.maestro.dev/get-started/maestro-mcp)

프로세스 복원은 HOME 후 background am kill과 Launcher MAIN/LAUNCHER 재실행, PID 소멸/변경으로 확인합니다. [Android killApp 공식 예제](https://docs.maestro.dev/reference/commands-available/killapp)는 같은 background am kill 경로와 stopApp:false 재실행을 설명합니다. force-stop/swipe/data-clear를 이 증거와 혼동하지 않습니다. [inputText 공식 제약](https://docs.maestro.dev/reference/commands-available/inputtext)에 따라 Android Unicode inputText를 사용하지 않습니다. 이미 한국어 상태인 삼성 자판의 ㅋ·ㅗ·ㅌ·ㅡ·ㄹ·ㄹ·ㅣ·ㄴ 선택자 클릭으로 코틀린 입력을 검증했습니다. clipboard·IME 설치/설정 변경은 없음. 다른 IME 또는 실제 한글 Kakao 응답 검증은 아님.


## 이번 실행의 세부 증거

| 구분 | 실행 결과 | Git 제외 증거 경로 e2e/results/ |
|---|---|---|
| 휴대폰 fixture | 01-search,02-favorites,03-pages,04-error,restore-setup/assert 통과 | 20261011-230546 / 231052 / 231514 |
| 휴대폰 실제 API | smoke 6개 통과. HTTP 시도 6건·200 응답 2건 | 20261011-234516-real; 비행기 모드1·default network none. 오프라인 4건은 클라이언트 시도이며 실제 서버 수신 아님 |
| 한글 입력 | korean-samsung 통과. 입력창/요청의 코틀린 일치 | 20261011-235234 |
| AVD 적응형 | adaptive 11회 invocation 통과, 전환 중 요청1회 | 20261011-234341; 가상 CLOSED/HALF_OPENED/OPENED·1/2-pane·회전·다크·시트 편집·1920×1200/240dpi 뷰포트; 설정 복원 true |
| 전용 AVD 빈 키 | no-key setup/assert 통과, API요청0건, fixture APK 재설치 | 20261012-002651; 실제 검색 제출→키안내→Room 목록·상세·삭제. 빈키 변형앱과 기존저장DB 유지 |
| 전용 AVD 누락 필드 | missing 통과 | 20261012-002345: 제목/URL만 있는 DTO의 목록·상세에서 정보/가격 대체 표시 |
| 전용 AVD 경합 | race 준비·assert 통과, 실제 시간 겹침 확인 | 20261012-002345/race-events.json: slow 시작26.319/완료40.324초, new 시작28.098초. 이전 응답 완료 뒤에도 New 유지. 현재 UI 선택자 좌표를 이용한 빠른 adb 입력+Maestro assertion |
| AVD 경계·오류 | 05-boundaries,06-navigation,첫 페이지 재시도 setup/recover,추가 페이지 재시도 setup/recover 통과 | 20261012-000156; 재시도 전 요청 증가 없음, 복구 후 정확히1회, page1 재요청 없음 |

실제 API smoke의 연결 복구 흐름은 다시 시도 버튼이 보일 때만 누릅니다. 자동 재연결이 먼저 완료할 수 있으므로 수동 재시도 클릭을 무조건 실행했다고 주장하지 않습니다. API smoke ASCII 검색은 Kotlin/Android이며 fixture 통과는 Kakao 통과를 대체하지 않습니다. Fold AVD의 가상 posture와 태블릿 뷰포트는 물리 힌지나 실물 태블릿 검증이 아닙니다.

전용 `BookSearch_E2E` AVD는 기존 API37 이미지를 공유해 별도 이름/port5556/작업폴더 데이터로 생성했습니다. RAM1536MB·2코어·데이터 설정2GB, snapshot 없이 실행했습니다. 검증 후 별도 앱·reverse 정리를 확인하고 이 전용 AVD만 종료·생성 데이터를 제거해 자원을 회수했습니다. 기존 Necto AVD와 앱은 조작하지 않았습니다.

## R-ID별 확인 경로

| R-ID | 요구 | 이번 실행 E2E | 기존 자동 테스트 또는 검토 | 남은 범위 |
|---|---|---|---|---|
| R-001 | Kotlin, Kotlin Coroutines, Flow, Jetpack Compose 사용 | 해당 없음 | 빌드/도구 설정 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-002 | Gradle 9.4.1 | 해당 없음 | 빌드/도구 설정 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-003 | Android Gradle Plugin 9.2.1 | 해당 없음 | 빌드/도구 설정 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-004 | Kotlin 2.4.0 | 해당 없음 | 빌드/도구 설정 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-005 | KSP를 사용하는 경우 2.3.10 | 해당 없음 | 빌드/도구 설정 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-006 | JDK 17 | 해당 없음 | 빌드/도구 설정 | Metro/Robolectric 실행 JDK21 필요. JDK17/무설명 기본 빌드 조건 미충족 유지; 별도 manual-DI PR 범위 |
| R-007 | compileSdk 37 | 해당 없음 | 빌드/도구 설정 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-008 | **추가 검증**: 최신 안정 Android Studio에서 열기 및 빌드 확인 | 해당 없음 | 빌드/도구 설정 | 기존 Studio/ZIP 검증 유지; 새 PC·무캐시·Studio 이번 미실행 |
| R-009 | Gradle Wrapper 파일과 실행 권한 포함 | 해당 없음 | 빌드/도구 설정 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-010 | 별도 설명 없이도 재현 가능한 `./gradlew assembleDebug` 성공 | 해당 없음 | 빌드/도구 설정 | Metro/Robolectric 실행 JDK21 필요. JDK17/무설명 기본 빌드 조건 미충족 유지; 별도 manual-DI PR 범위 |
| R-011 | **추가 검증**: 요청 버전 조합의 호환성 확인. 충돌 시 임의 변경하지 않고 실패 로그·공식 근거·대안 기록 | 해당 없음 | 빌드/도구 설정 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-012 | 하단 탭 2개: 검색 탭과 즐겨찾기 탭 제공 | 01-search | 소스·문서/제출 검토 | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-013 | Kakao 책 검색 API에 검색어 전달 | 01-search · smoke-* (실 API) · korean-samsung | KakaoBookServiceTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-014 | 정확도순 / 최신순 선택. API 값은 `accuracy` / `latest`, 최신순 의미는 발간일순 | 01-search | 소스·문서/제출 검토 | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-015 | 페이지당 20개 요청 및 다음 페이지 로드 | 03-pages | SearchBooksQueryKeyTest / SearchPresenterTest / SearchRetryTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-016 | 공통 도서 카드에 표지, 제목, 저자, 출판사, 출판일, 가격 표시 | missing · 01-search · smoke-* (실 API) | 소스·문서/제출 검토 | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-017 | 카드에서 즐겨찾기 추가·해제 | 02-favorites · 06-navigation | FavoriteMutationKeyTest / FavoriteMutationLifecycleTest / FavoriteTogglerTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-018 | 카드를 선택하면 상세 화면으로 이동 | 01-search · smoke-* (실 API) | 소스·문서/제출 검토 | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-019 | 검색·정렬 변경 후 이전 조건의 결과가 섞이지 않음 | race/race-assert · 01-search | SearchBooksQueryKeyTest / SearchPresenterTest / SearchRetryTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-020 | 검색 결과 없음 / 최초 로딩 / 최초 오류 / 추가 로딩 / 추가 오류 구분 | 01-search · error/more-error setup/recover | SearchBooksQueryKeyTest / SearchPresenterTest / SearchRetryTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-021 | 도서 정보를 로컬에 영속 저장 | no-key-setup/assert · 02-favorites · restore | FavoriteBookDaoTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-022 | 즐겨찾기 목록 안에서 로컬 검색 | 02-favorites | FavoriteListRulesTest / PriceFilterTest / FavoritesScreenTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-023 | 제목 오름차순 / 내림차순 정렬 | 02-favorites | FavoriteListRulesTest / PriceFilterTest / FavoritesScreenTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-024 | 가격 조건 필터 | 02-favorites · 05-boundaries | FavoriteListRulesTest / PriceFilterTest / FavoritesScreenTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-025 | 즐겨찾기 목록에서 상세로 이동 | 02-favorites · 06-navigation | 소스·문서/제출 검토 | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-026 | 앱 종료 및 재실행 후 즐겨찾기 유지 | restore | FavoriteBookDaoTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-027 | 검색·즐겨찾기·상세에서 동일 도서의 즐겨찾기 상태 일치 | 02-favorites · 06-navigation | FavoriteMutationKeyTest / FavoriteMutationLifecycleTest / FavoriteTogglerTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-028 | 저장/삭제 실패 시 성공으로 표시하지 않음 | 해당 없음 | FavoriteMutationKeyTest / FavoriteMutationLifecycleTest / FavoriteTogglerTest | 지연·실패·pop·도서별 순서는 기존 실제 Soil/retain+Fake DAO 테스트. 기기에서 쓰기 실패/지연 미주입, 미완료 process-kill 쓰기 보장 없음 |
| R-029 | 도서 표지와 제목 등 도서 정보 표시 | missing · 01-search · smoke-* (실 API) | BookMapperTest / BookDetailPresenterTest / BookDetailPriceTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-030 | 즐겨찾기 상태 표시 및 변경 | 02-favorites | FavoriteMutationKeyTest / FavoriteMutationLifecycleTest / FavoriteTogglerTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-031 | 목록과 상세의 즐겨찾기 변경 결과 일치 | 02-favorites | FavoriteMutationKeyTest / FavoriteMutationLifecycleTest / FavoriteTogglerTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-032 | 저장된 도서는 네트워크 없이도 로컬 정보로 상세 표시 | no-key-setup/assert · restore | BookMapperTest / BookDetailPresenterTest / BookDetailPriceTest | snapshot/Room·fixture 실패와 실제 검색 단절은 별개; 이번 비행기 모드에서 모든 상세 조합 미반복 |
| R-033 | 누락된 표지·저자·날짜·가격 등으로 화면이 깨지지 않음 | missing · 05-boundaries | BookMapperTest / BookDetailPresenterTest / BookDetailPriceTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-034 | 불안정한 네트워크에서도 앱 사용 가능 | error/more-error setup/recover · smoke-* (실 API) | 소스·문서/제출 검토 | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-035 | 태블릿·폴더블 대응 | adaptive-* | BookSearchNavHostTest / BookSearchBackStackTest / SearchStateRestorationTest / FavoritesStateLifecycleTest | AVD 가상 접힘/태블릿 뷰포트. 물리 힌지·실물 태블릿 없음 |
| R-036 | 대규모 앱의 기능 중 하나라는 전제로 구조 설계 | 해당 없음 | 소스·문서/제출 검토 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-037 | 여러 개발자가 확장할 수 있는 책임·의존성 경계 설명 | 해당 없음 | 소스·문서/제출 검토 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-038 | 구현한 설계의 선택 이유와 단점 설명 | 해당 없음 | 소스·문서/제출 검토 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-039 | 검토했으나 구현하지 않은 대안과 미구현 범위 설명 | 해당 없음 | 소스·문서/제출 검토 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-040 | 오류 후 재시도 가능, 추가 로딩 오류가 기존 목록을 제거하지 않음 | error/more-error setup/recover · smoke-* (실 API) | 소스·문서/제출 검토 | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-041 | 창 크기 변화 중 입력·목록·선택 상태를 보존하는 동작 검증 | adaptive-* · smoke-* (실 API) | BookSearchNavHostTest / BookSearchBackStackTest / SearchStateRestorationTest / FavoritesStateLifecycleTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-042 | **사용자 선택**: retain + Navigation 3로 진행. 학습 목표를 유지하며 검증 | 해당 없음 | BookSearchNavHostTest / BookSearchBackStackTest / SearchStateRestorationTest / FavoritesStateLifecycleTest | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-043 | **사용자 선택**: Room으로 영속 저장. 원 과제에서 Room 라이브러리를 지정한 것은 아님 | 해당 없음 | FavoriteBookDaoTest | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-044 | **사용자 선택**: Room Flow를 즐겨찾기의 단일 상태 기준으로 사용 | 해당 없음 | FavoriteBookDaoTest | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-045 | **사용자 선택**: 원격 통신은 Retrofit + OkHttp + kotlinx.serialization 사용 | 해당 없음 | KakaoBookServiceTest | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-046 | **권장**: MockWebServer API 계층 테스트. 의존성 버전은 [구현 사양](SPEC.md) 2절 기준 | 해당 없음 | KakaoBookServiceTest | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-047 | **사용자 선택**: Soil typed-key + InfiniteQuery로 데이터 접근·페이지네이션 구성, 단순 위임 Repository 생략 | 해당 없음 | SearchBooksQueryKeyTest / SearchPresenterTest / SearchRetryTest | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-048 | **사용자 선택**: Metro 의존성 주입, Coil 3.6.3 이미지 로딩 | 해당 없음 | 소스·문서/제출 검토 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-049 | **설계 검증**: Metro/Kotlin 컴파일러 호환, API와 이미지 네트워크 인증 설정 분리. 구체적인 구조는 [아키텍처](ARCHITECTURE.md) 참고 | 해당 없음 | KakaoBookServiceTest | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-050 | **설계 검증**: query/sort별 key, size=20, 페이지 메타데이터·is_end 유지, 로딩·취소·재시도 동작을 테스트로 확인. 커스텀 페이지네이션과 Paging 3는 미채택 대안 | 03-pages | SearchBooksQueryKeyTest / SearchPresenterTest / SearchRetryTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-051 | **로컬 연결 검증**: 선택한 Soil Subscription→Room Flow / Mutation→Room 쓰기에서 Room 단일 상태 기준, 반영 순서·지연, 구독 수명·재구독을 테스트 | 해당 없음 | FavoriteMutationKeyTest / FavoriteMutationLifecycleTest / FavoriteTogglerTest | 지연·실패·pop·도서별 순서는 기존 실제 Soil/retain+Fake DAO 테스트. 기기에서 쓰기 실패/지연 미주입, 미완료 process-kill 쓰기 보장 없음 |
| R-052 | **오류 정책 검증**: Retrofit 예외의 재시도 가능 여부를 명시적으로 분류하고 CancellationException을 보존 | 해당 없음 | ErrorKindTest | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-053 | **결정**: `app`, `core:model`, `core:network`, `core:database`, `core:data`, `core:ui`, `feature:search`, `feature:favorites`, `feature:detail`의 9개 모듈로 경계를 나누고, 그 이상의 모듈 증설은 피하기 | 해당 없음 | 소스·문서/제출 검토 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-054 | **현재 제품 정책**: 시스템 프로세스 재생성 시 query/sort/filter/탭과 상세 도서 스냅샷을 복원하고 검색은 재조회. 결과 목록 전체를 saveable에 넣지 않음 | restore | BookSearchNavHostTest / BookSearchBackStackTest / SearchStateRestorationTest / FavoritesStateLifecycleTest | kill에서 검색어·미저장 상세·Room 확인. sort/filter/입력/탭 전체 조합은 기존 복원 테스트 |
| R-055 | 핵심 단위 테스트: ID·가격 정책, 검색 상태 전이, 요청 역전·취소·초기화 경합 | race/race-assert | BookIdentityTest / BookPricePolicyTest / SearchPresenterTest | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-056 | 페이지네이션 테스트: 마지막 20개 + is_end, 실패한 동일 페이지 재시도, 중복 요청·ISBN 중복 | 03-pages | SearchBooksQueryKeyTest / SearchPresenterTest / SearchRetryTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-057 | Room 통합 테스트: 저장·삭제·Flow 관찰, DB 재열기 후 영속성. 쓰기 실패는 Fake/실패 주입 테스트로 별도 확인 | 해당 없음 | FavoriteBookDaoTest | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-058 | 필수 Compose UI 테스트: 탭·상세 이동과 복귀, 창 크기별 UI, retain 상태 유지·엔트리 제거 경로 | 해당 없음 | BookSearchNavHostTest / BookSearchBackStackTest / SearchStateRestorationTest / FavoritesStateLifecycleTest | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-059 | 회전·폴더블·프로세스 종료 등 자동화가 제한된 검증은 기기/에뮬레이터 수동 확인으로 보완하고 자동 테스트와 구분 | adaptive-* · smoke-* (실 API) | 소스·문서/제출 검토 | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-060 | 테스트별 실제 실행 명령·환경·결과 기록. 작성만으로 통과 처리하지 않으며, 미실행·실패는 이유와 함께 별도 표시 | 해당 없음 | 소스·문서/제출 검토 | 기존 자동 테스트/검토 경로; Maestro 전체 대체 아님 |
| R-061 | 검색어 빠른 변경, 정렬 변경, 빈 검색어로 초기화 | race/race-assert · 01-search · korean-samsung | SearchBooksQueryKeyTest / SearchPresenterTest / SearchRetryTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-062 | 마지막 페이지가 20개여도 `meta.is_end`에 따라 요청 종료 | 03-pages | SearchBooksQueryKeyTest / SearchPresenterTest / SearchRetryTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-063 | 추가 로딩 실패 후 같은 페이지 재시도, 중복 트리거 차단 | error/more-error setup/recover | SearchBooksQueryKeyTest / SearchPresenterTest / SearchRetryTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-064 | 페이지 간 동일 ISBN 중복 제거 | 03-pages | SearchBooksQueryKeyTest / SearchPresenterTest / SearchRetryTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-065 | 가격 경계값, 0 이하/가격 없음, 잘못된 범위, 필터 결과 0개 | 05-boundaries | FavoriteListRulesTest / PriceFilterTest / FavoritesScreenTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-066 | 오프라인 즐겨찾기·상세 및 앱 재시작 후 복원 | restore | 소스·문서/제출 검토 | snapshot/Room·fixture 실패와 실제 검색 단절은 별개; 이번 비행기 모드에서 모든 상세 조합 미반복 |
| R-067 | Room 쓰기 실패, 즐겨찾기 연타, 쓰기 중 뒤로 가기 | 해당 없음 | FavoriteMutationKeyTest / FavoriteMutationLifecycleTest / FavoriteTogglerTest | 지연·실패·pop·도서별 순서는 기존 실제 Soil/retain+Fake DAO 테스트. 기기에서 쓰기 실패/지연 미주입, 미완료 process-kill 쓰기 보장 없음 |
| R-068 | 화면 회전, 창 크기 변경, 폴더블 펼침·접힘 | adaptive-* · smoke-* (실 API) | BookSearchNavHostTest / BookSearchBackStackTest / SearchStateRestorationTest / FavoritesStateLifecycleTest | AVD 가상 접힘/태블릿 뷰포트. 물리 힌지·실물 태블릿 없음 |
| R-069 | 상세 진입·복귀 시 상태 유지, 백스택 제거 시 작업 정리 | 06-navigation · adaptive-* | BookSearchNavHostTest / BookSearchBackStackTest / SearchStateRestorationTest / FavoritesStateLifecycleTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |
| R-070 | 시스템 프로세스 재생성 후 최소 상태 복원, 저장·미저장 도서 모두 상세를 네트워크 없이 다시 표시 | restore | BookSearchNavHostTest / BookSearchBackStackTest / SearchStateRestorationTest / FavoritesStateLifecycleTest | kill에서 검색어·미저장 상세·Room 확인. sort/filter/입력/탭 전체 조합은 기존 복원 테스트 |
| R-071 | 저장 중 화면 pop 이후 쓰기 완료·도서별 순서 확인. 프로세스 종료 시 미완료 쓰기 보장과 구분 | 해당 없음 | FavoriteMutationKeyTest / FavoriteMutationLifecycleTest / FavoriteTogglerTest | 지연·실패·pop·도서별 순서는 기존 실제 Soil/retain+Fake DAO 테스트. 기기에서 쓰기 실패/지연 미주입, 미완료 process-kill 쓰기 보장 없음 |
| R-072 | 소스 코드 ZIP | 해당 없음 | 소스·문서/제출 검토 | 기존 제출 문서/ZIP 검사. 이번 PR은 제출 패키지 재생성 없음 |
| R-073 | ZIP 안에 `.git` 포함 | 해당 없음 | 소스·문서/제출 검토 | 기존 제출 문서/ZIP 검사. 이번 PR은 제출 패키지 재생성 없음 |
| R-074 | API 키를 `local.properties`에서 읽도록 구성하고, 제출 ZIP에도 해당 파일 포함 | 해당 없음 | 소스·문서/제출 검토 | 기존 제출 문서/ZIP 검사. 이번 PR은 제출 패키지 재생성 없음 |
| R-075 | 제출에 사용한 API 키를 전형 종료 전까지 폐기하지 않음 | 해당 없음 | 소스·문서/제출 검토 | 최종 키 수명·Git 인증 설정·제출 대상 사용자 확인 |
| R-076 | ZIP에서 모든 `build/`, `.gradle/` 제외 | 해당 없음 | 소스·문서/제출 검토 | 기존 제출 문서/ZIP 검사. 이번 PR은 제출 패키지 재생성 없음 |
| R-077 | APK 포함. Debug APK 허용 | 해당 없음 | 소스·문서/제출 검토 | 기존 제출 문서/ZIP 검사. 이번 PR은 제출 패키지 재생성 없음 |
| R-078 | README 포함: 빌드 방법, 사용 프레임워크, 구조, 주요 구현 포인트(원문 요구), 선택 이유·실제 검증 결과·한계, AI 활용 시 활용 작업·검증 방법·직접 판단해 바꾼 부분 | 해당 없음 | 소스·문서/제출 검토 | 기존 제출 문서/ZIP 검사. 이번 PR은 제출 패키지 재생성 없음 |
| R-079 | **선택**: 3분 이내 시연 영상 | 해당 없음 | 소스·문서/제출 검토 | 기존 제출 문서/ZIP 검사. 이번 PR은 제출 패키지 재생성 없음 |
| R-080 | GitHub 저장소명·설명·문서 등 저장소 콘텐츠에 회사명 미포함 | 해당 없음 | 소스·문서/제출 검토 | 기존 제출 문서/ZIP 검사. 이번 PR은 제출 패키지 재생성 없음 |
| R-081 | 실제 API 키가 Git 현재 파일·전체 이력·문서·로그에 없는지 확인 | 해당 없음 | 소스·문서/제출 검토 | 기존 제출 문서/ZIP 검사. 이번 PR은 제출 패키지 재생성 없음 |
| R-082 | ZIP에 포함될 `.git/config` 및 remote URL에 자격증명이 없는지 사용자 확인 | 해당 없음 | 소스·문서/제출 검토 | 최종 키 수명·Git 인증 설정·제출 대상 사용자 확인 |
| R-083 | 최종 키 설정·민감정보 포함 여부·제출 대상은 사용자가 직접 확인 | 해당 없음 | 소스·문서/제출 검토 | 최종 키 수명·Git 인증 설정·제출 대상 사용자 확인 |
| R-084 | 압축을 새 디렉터리에 풀고 빌드 및 APK 설치 확인 | 해당 없음 | 소스·문서/제출 검토 | 기존 Studio/ZIP 검증 유지; 새 PC·무캐시·Studio 이번 미실행 |
| R-085 | 탭·백스택 규칙 준수: 같은 탭 재선택 시 루트로 복귀하고 백스택 순서가 뒤집히지 않음, 상세 위에 상세가 쌓이지 않음(두 카드 동시 터치 포함), 즐겨찾기 루트에서 뒤로 가기 시 검색 탭으로 이동 | 06-navigation · adaptive-* | BookSearchNavHostTest / BookSearchBackStackTest / SearchStateRestorationTest / FavoritesStateLifecycleTest | 순차 탐색은 Maestro; 두 카드 실제 동시 탭은 기존 BackStack/NavHost 테스트·과거 QA |
| R-086 | API 키가 없어도 빌드가 성공하고, 검색 화면에서 키 미설정을 안내하며 즐겨찾기는 동작 | no-key-setup/assert | 소스·문서/제출 검토 | 빈 키 변형앱의 실제 검색 제출·Room 목록/상세/삭제 통과, HTTP요청0건 |
| R-087 | 상세에서 즐겨찾기를 해제해도 표시 중인 도서 정보가 사라지지 않음 | no-key-setup/assert · 02-favorites | BookMapperTest / BookDetailPresenterTest / BookDetailPriceTest | 이번 흐름의 assertion만 확인; 원 TC 전체 반복 아님 |

## QA 45행 대응

| TC | R-ID | 기기 절차 | 작성된 흐름 / 남는 범위 |
|---|---|---|---|
| 01 | R-012 | 앱을 새로 설치하고 첫 실행 | 01-search |
| 02 | R-013, R-016 | "kotlin" 입력 후 키보드 검색 키로 확정 | 01-search/korean-samsung/smoke-online; 검색 버튼 사용, IME submit 기존 테스트 |
| 03 | R-014, R-019 | 정렬 메뉴에서 발간일순 선택 | 01-search |
| 04 | R-015, R-062 | 결과 목록을 끝까지 연속 스크롤 | 03-pages |
| 05 | R-020 | 결과가 없는 검색어("qzxqzxqzx987") 확정 | 01-search fixture empty |
| 06 | R-061 | 검색어를 지운 뒤 빈 상태로 확정 | 01-search |
| 19 | R-019, R-061 | "android" 확정 직후 "java"로 바꿔 확정 | race/race-assert: 빠른 adb 입력+Maestro 판정; 시간겹침 확인. 기존 SearchPresenterTest |
| 20 | R-020, R-034 | 비행기 모드에서 새 검색어("swift") 확정 | smoke-offline 실제 단절/04-error 503 |
| 40 | R-040, R-063 | 온라인에서 재시도 → 오프라인 전환 → 끝까지 스크롤 → 오류 뒤 더 스크롤 | more-error setup/recover 503; 실제 단절 페이징 과거 QA |
| 40b | R-040, R-069 | 추가 로딩 실패 상태에서 즐겨찾기 탭으로 10초 이동 후 복귀 → 온라인에서 재시도 | more-error setup/recover; 탭10초 체류 미반복 |
| 42 | R-034, R-040 | 오프라인에서 새 검색어("compose") 오류 → 연결 복구 후 12초 대기 → 재시도 | error setup/recover/smoke-retry; 원12초 조건 미반복 |
| 86 | R-086 | `local.properties`가 없는 별도 worktree로 빌드·설치 → 검색 → 즐겨찾기 탭과 상세 | no-key-setup/assert 통과; 제출앱 대신 동일.e2e ID의 빈키 변형앱 사용 |
| 07 | R-017, R-021 | 검색 결과 카드 3개의 하트 누르기 | 02-favorites/06-navigation |
| 10 | R-021, R-023 | 2개를 더 추가한 뒤 즐겨찾기 탭 열기 | 02-favorites |
| 12 | R-023 | 정렬을 제목 내림차순으로 변경 | 02-favorites |
| 12b | R-023, R-041 | 목록을 아래로 스크롤한 뒤 정렬 변경 | 기존 FavoritesScreenTest; 스크롤 후 정렬 미반복 |
| 11 | R-022 | 로컬 검색: 제목 "action", 저자 "jemerov", 일치 없음 "sebastian", 검색어 지우기 | 02-favorites 제목 검색; 저자·대소문자·빈결과 기존 테스트 |
| 13 | R-024, R-065 | 가격 필터: 36000~40000 적용 → 최소 50000·최대 40000 입력 → 최소 70000만 적용 → 초기화 | 05-boundaries/02-favorites |
| 27 | R-027 | 즐겨찾기 목록에서 하트 해제 → 검색 탭 확인 | 02-favorites/06-navigation |
| 67 | R-067 | 검색 카드의 하트를 한 번의 셸 호출로 6회 연속 탭 | 06-navigation 순차토글; 같은 셸6회 연타는 과거 QA/기존 테스트 |
| 26 | R-026 | `am force-stop` 후 재실행 → 즐겨찾기 탭 | restore background kill; force-stop 원절차 과거 QA |
| 08/09 | R-018, R-029, R-030, R-031, R-087 | 저장한 도서 상세 열기 → 상세에서 하트 해제 → 뒤로 | 01-search/02-favorites |
| 25 | R-025 | 즐겨찾기 목록에서 카드 선택 | 02-favorites/06-navigation |
| 71 | R-071, R-027 | 미저장 도서 상세에서 하트를 누르고 같은 셸 호출에서 바로 뒤로 가기. 상세 진입 약 1초 뒤 실행해 저장·해제를 번갈아 4회, 하트 후 2초 뒤 뒤로 2회. 즐겨찾기 목록의 상세에서도 해제 후 바로 뒤로 | 06-navigation save/back; 진행중 write 보장은 기존 FavoriteMutationLifecycleTest |
| 18 | R-085 | 검색 → 즐겨찾기 탭 → 즐겨찾기 탭 재선택 → 뒤로 → 검색 루트에서 뒤로 | 06-navigation; 검색루트 종료 기존 NavHostTest |
| 85 | R-085, R-069 | 서로 다른 카드 2개를 거의 동시에 탭(백그라운드 탭 + 즉시 탭) → 뒤로 | 기존 BackStack/NavHost 테스트; 이번 실제 동시 터치 미주입 |
| 33 | R-033 | 누락 필드 및 정가 0 이하·null 도서의 카드·상세 확인 → 즐겨찾기 저장 → 금액 필터 적용·초기화 | missing/05-boundaries 통과; 원가격null/즐겨찾기필터전체조합 기존 테스트 |
| 35 | R-035 | 세로(800dp)에서 검색 → 상세 | adaptive-narrow/detail |
| 90 | R-035 | 가로에서 앱 데이터를 지우고 실행 → "kotlin" 검색 → 첫 카드 선택 | adaptive-wide; 원가로clearState순서와 다름 |
| 91 | R-035, R-085 | 상세가 열린 채 다른 카드 선택 | adaptive-wide 상세교체 |
| 92 | R-085 | 뒤로 / 같은 카드 두 번 선택 후 뒤로 / 서로 다른 카드 3개를 차례로 선택 후 뒤로 | adaptive-wide/06-navigation; 원전체반복 기존 테스트 |
| 93 | R-025, R-027 | 검색 카드 2개 저장 → 즐겨찾기 탭에서 카드 선택 → 오른쪽 상세에서 하트 해제 | 02-favorites/adaptive-wide; 즐겨찾기2-pane해제 기존 테스트 |
| 94 | R-041, R-068 | 2-pane에서 상세를 연 채 세로 회전 → 다시 가로 회전 | adaptive-rotate |
| 68d | R-035, R-068 | 펼친 화면에서 "compose" 결과를 스크롤 → 접힘(state 0) → 펼침(state 3) | adaptive-narrow/wide 가상state0/2; 기기별state번호 다름 |
| 68e | R-041, R-068 | 상세를 연 채 접힘 → 펼침 → 뒤로 | adaptive-detail/wide |
| 68f | R-041, R-068 | 즐겨찾기 가격 필터 시트에 "300"을 입력하는 중(키보드 열림) 접힘 → 펼침 | adaptive-sheet-setup/sheet |
| 68g | R-068 | 검색 결과에서 중간 접힘(HALF_FOLDED, state 2) → 펼침 | adaptive-detail 가상 HALF_OPENED |
| 68h | R-068 | 기기 상태 강제값을 해제하고 손으로 접었다 펴기 | 물리 폴더블 없음; 손으로접기 미실행 |
| 32 | R-032, R-066 | 비행기 모드에서 즐겨찾기 탭의 도서 상세 열기 | restore Room/snapshot; 실제비행기모드 이TC 미반복 |
| 70a | R-070 | 비행기 모드에서 검색 결과(미저장) 도서 상세 열기 | restore-assert 미저장snapshot; 실제비행기모드 이TC 미반복 |
| 70b | R-054, R-070 | 미저장 도서 상세(비행기 모드) → 홈 → `am kill` → 재실행 | restore setup/assert 503 실패조건; 실제단절 아님 |
| 54 | R-054 | TC-70b에 이어 뒤로 → 즐겨찾기 탭 | restore-assert; sort/filter/tab 전체조합 기존 복원테스트 |
| 41 | R-041, R-068 | 검색 목록을 스크롤한 상태로 가로 회전 | adaptive-rotate/smoke-rotate; 깊은scroll 기존 테스트 |
| 68a | R-068 | 상세에서 회전 후 뒤로 / 가격 필터 시트에 "300" 입력 중 회전 | adaptive-rotate/sheet; 시트열린회전 기존 테스트 |
| 68b | (추가 확인) | 다크 모드 전환 | adaptive-wide dark/smoke-theme |

## 재현과 제한

최종 자동 테스트 명령: `JAVA_HOME=/path/to/jdk21 ./gradlew testDebugUnitTest :core:model:test --rerun-tasks -Pkotlin.compiler.execution.strategy=in-process -Pkotlin.compiler.runViaBuildToolsApi=false --offline`. APK·lint는 `:app:assembleDebug :app:assembleE2e :app:assembleE2eNoKey lintDebug`와 관련 app/favorites 테스트로 확인했습니다. fixture는 `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s e2e/support -p 'test_*.py'` 3개 통과. 현재 추적/후보 파일287개에서 로컬 실제키 일치0건, worktree에는 SDK만 설정. GitHub Actions workflow0개로 원격 CI는 미구성입니다.

[실행 안내](../e2e/README.md). runner는 명시 기기·별도 .e2e 앱만 사용하며 기존 reverse 규칙이 있으면 중단합니다. 자신이 만든 tcp:8877만 finally 제거하고 localhost 서버를 종료합니다. adaptive는 별도 --allow-emulator-config로 승인된 AVD만 사용하고 설정 원본/복원 비교를 저장합니다. smoke는 별도 실제키·설정 승인 플래그가 필요합니다.

restore는 HOME→background am kill→PID 소멸→Launcher MAIN/LAUNCHER 재실행→새 PID를 요구합니다. force-stop/swipe/data-clear 복원이나 미완료 쓰기의 kill 내구성은 보장하지 않습니다. negative-control은 정확한 nonexistent 문구가 JUnit failure에 있어야 검출로 인정하므로 연결 실패를 정상 대조로 계산하지 않습니다.

물리 힌지 TC-68h·실물 태블릿·Gboard 한글·실제 두 카드 동시 터치·기기 쓰기 오류/지연·비행기 모드에서 저장/미저장 상세 모든 조합은 이번 실행 밖입니다. 해당 계약의 기존 자동 테스트와 과거 수동 QA는 이번 Maestro 통과로 바꾸지 않습니다. 원본 표지의 오프라인 저장도 구현 범위 밖입니다. 최종 키 유효기간·제출 ZIP·Git 인증 설정은 사용자 확인 사항입니다.

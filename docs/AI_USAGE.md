# AI 활용 기록

기준일: 2026-10-08

## 역할 분담

| 단계 | 도구 | AI가 맡은 일 |
| --- | --- | --- |
| 설계 | Codex | 개발자와 협의하며 요구사항을 기능·환경·제출 체크리스트로 정리, 참고 코드와 Kakao API·Android 문서 조사, 위험 식별(상태 수명, 검색 경합, 페이지 종료, 저장 실패, 오프라인), 아키텍처·제품 정책 문서 초안. 코드는 작성하지 않음 |
| 사양 확정 | Claude Code (Claude Opus 5.5) | 설계 문서의 암묵지 검토, 원문 재량 영역의 기본안을 [SPEC](SPEC.md)으로 정리, 모듈 분리·Soil–Retrofit 연결 확인 |
| 구현·검증 | Claude Code (Claude Opus 5.5) | 개발자의 명시적 요청으로 맡음. 지정 버전 호환성 확인과 멀티 모듈 Gradle 구성, 9개 모듈의 코드와 테스트, 요구사항 기반 실기기 QA(adb 조작), 시연 영상 녹화·편집, 제출 ZIP 스크립트, 검증 문서·README·이 문서 작성 |

개발자는 기술 선택과 제품 정책 결정, AI 결과 검토와 수정 지시, 실제 키 설정과 최종 제출 패키지 확인을 맡았습니다. 실제 폴더블 기기 확인은 아직 하지 않았습니다.

작업 원칙은 다음과 같습니다.

- AI 결과에는 근거(공식 문서, 확인한 코드 위치, diff, 실행한 명령과 결과)를 붙이고, 개발자가 요구사항·동작·유지보수 관점에서 검토해 채택·수정·기각합니다.
- 기능 단위로 구현하고 단위마다 빌드·테스트를 실행한 뒤 커밋합니다. SPEC의 기본안이 바뀌면 SPEC을 먼저 고칩니다.
- 작성한 테스트, 실행해 통과한 테스트, 미실행 항목을 구분하고 오류 원인은 실제 로그와 대조합니다.
- 실제 API 키나 비공개 메시지를 넣은 프롬프트, 원본 대화 기록은 제출 자료에 넣지 않습니다.

## 의사결정 기록

### ADR-001: retain + Navigation 3

- 결정: 화면 상태 수명은 retain + Navigation 3로 관리하고 ViewModel 중심 구조로 전환하지 않습니다.
- 이유: AI는 일정과 익숙한 구현 경로를 들어 ViewModel을 제안했습니다. 개발자는 기존 retain PoC에서 Navigation 3와 오류 처리를 시험한 경험과 retain 학습 목표를 근거로 retain을 택했습니다. 대신 엔트리별 수명, 코루틴 취소, pop 정리, saveable 최소 상태, 프로세스 복원을 먼저 검증하도록 순서를 바꿨습니다.
- 객체 유지와 프로세스 복원, Metro가 생성하는 factory와 자체 KSP 편의 함수 생성기를 구분했습니다. Metro factory와 retain/Nav3는 최소한으로 연결하고 자체 생성기는 추가하지 않습니다.
- 검증: retain 값을 Navigation 3 엔트리에 묶는 decorator를 구현했습니다. 엔트리 제거 시 정리와 구성 변경 시 유지는 `BookSearchNavHostTest`로, 프로세스 재생성 복원은 실기기 QA(TC-54, TC-70b)로 확인했습니다.

### ADR-002: Retrofit

- 결정: 원격 통신은 Retrofit + OkHttp + kotlinx.serialization, 로컬 저장은 Room으로 구성합니다.
- 이유: 참고 도서 검색 프로젝트는 Ktor를 쓰지만, 개발자는 Retrofit 경험이 더 많아 제한된 일정에서 구현·검증에 집중하기로 했습니다. Retrofit이 항상 낫다거나 Ktor로 테스트할 수 없다는 판단은 아닙니다.
- 대안: Ktor.
- 검증: AI가 권장한 MockWebServer 테스트(`KakaoBookServiceTest`)로 인증 헤더·검색 파라미터, 알 수 없는 필드 무시, HTTP 오류 전파, 키 누락 시 요청 차단을 확인했습니다. Presenter 테스트는 key factory에 Fake API를 연결합니다.

### ADR-003: Soil typed-key와 InfiniteQuery

- 결정: Query/InfiniteQuery → Retrofit, Subscription → Room Flow, Mutation → Room 쓰기의 typed-key 구성을 씁니다. 단순 위임 Repository는 두지 않고, 즐겨찾기 상태는 Room을 단일 기준으로 삼습니다.
- 이유: 개발자가 학습 가치를 고려해 선택했습니다. 개발자의 과거 Soil 적용에서 로컬을 Repository로 되돌린 경험이 있어 AI가 로컬 연결 비교를 제안했지만, 개발자는 DroidKaigi 2026이 당시의 의문을 해소한 참고 구조라고 판단해 현재 구성을 유지했습니다. 과거 경험은 단일 상태 기준·구독 수명·반영 순서 테스트에 반영했습니다([ARCHITECTURE](ARCHITECTURE.md) 9절).
- 오류 처리: fetch와 Mutation은 T 반환·예외 전파 계약을 지키고, `runSuspendCatching`을 기본 래퍼로 쓰지 않습니다. 취소는 실패로 알리지 않습니다(`FavoriteTogglerTest`).
- 대안: 일반 Repository, 커스텀 페이징, Paging 3.
- 검증: `SearchPresenterTest`, `FavoriteSubscriptionKeysTest`.

### ADR-004: Metro와 Coil

- 결정: 개발자가 Metro와 Coil을 선택했고, AI는 최소 Metro 그래프와 Coil 3(coil-compose + coil-network-okhttp) 구성을 권장했습니다. 이미지 요청에는 API 인증 interceptor를 전달하지 않습니다.
- 결과: Metro 1.4.5와 Coil 3.6.3으로 빌드했고, 이미지 로더는 인증 interceptor가 없는 클라이언트를 씁니다. 구조는 [ARCHITECTURE](ARCHITECTURE.md)에 정리했습니다.

### ADR-005: 원문 재량 영역의 기본안

- 배경: 원문은 의문점을 합리적으로 판단하라고 하고 UI 세부를 자유로 둡니다. 재량 영역은 명시한 기본안으로, 나머지는 구현 중 확인할 사실로 나누고, 필수 요구·제품 정책·구현 계약도 분리했습니다.
- AI 작업: 원문 요약, UI 가이드, Kakao API 문서, Soil 소스(1.0.0-alpha15), DroidKaigi 2025·2026 기준 커밋을 대조해 [SPEC](SPEC.md)을 작성했습니다.
- 정책 변경: Kakao API에 단건 조회가 없어, 복원 정책을 "bookId 저장 후 재조회"에서 "상세 진입 시점의 도서 스냅샷을 내비게이션 키에 저장"으로 바꿨습니다. Soil Mutation scope가 keepAliveTime 뒤 취소되므로 즐겨찾기 쓰기를 앱 scope와 Mutex로 옮겼습니다.
- 제품 정책 근거: 공개 도서 서비스 UI와 Android 공식 지침을 참고했습니다. 가격 범위 입력·적용 UI와 위시리스트 흐름은 공개 자료로 확인했지만, 검색 트리거·내부 DB scope·프로세스 복원은 확인했다고 기록하지 않습니다. 정가 기준, 저장 버튼·화면 이동, 최소 상태 복원, 1-pane 우선은 이 앱이 선택한 설계입니다.
- 참고 저장소: DroidKaigi 2026은 패턴만 참고하고, 열린 탭 관련 버그 이슈(#344, #226) 때문에 탭 전환 코드는 복사하지 않고 규칙을 직접 정했습니다. 2025는 Metro·Soil 사용의 교차 확인에만 씁니다.
- 검증: 즐겨찾기 쓰기의 앱 scope와 Mutex는 `FavoriteWriterTest`(화면 취소 후 완료, 직렬 실행)로 확인했습니다.

### ADR-006: 모듈 분리와 Soil·Retrofit 연결

- AI 확인(개발자 질문: Soil이 Retrofit을 지원하는지, `core:data`를 나눌지): Soil의 fetch 계약은 `suspend` 함수뿐이고 core 의존성에 HTTP 클라이언트가 없으며, Ktor는 선택 receiver 모듈에만 있습니다. KMP 공통 코드에서 Retrofit을 못 쓰는 것은 Retrofit의 플랫폼 제약입니다.
- 결정: Retrofit을 유지하고 key 생성자에 Retrofit 서비스를 Metro로 주입합니다. 개발자 제안대로 `core:network`·`core:database`를 분리해 9개 모듈로 구성했습니다. build-logic은 개발자의 기존 저장소 구성을 따르되 Circuit·Firebase·detekt 관련 플러그인은 뺐습니다.
- 버전: Maven Central·Google Maven에서 조회한 Metro 1.4.5, Compose BOM 2026.09.00(runtime-retain 1.12.1 포함), Navigation 3 1.2.0, Room 2.8.5, Retrofit 3.0.0, OkHttp 5.5.0, Coil 3.6.3, Robolectric 4.17로 빌드에 성공해 [SPEC](SPEC.md) 2절에 고정했습니다.

### ADR-007: 구현 중 개발자 판단

| 주제 | AI 보고·제안 | 개발자 결정 | 근거·결과 |
| --- | --- | --- | --- |
| JDK | Metro 1.4.5 Gradle 플러그인이 JVM 21 이상을 요구함을 실패 로그로 보고. Metro를 낮추면 Kotlin 2.4.0과 맞지 않고, DI를 바꾸면 사용자 선택이 바뀜 | 지정 버전 유지, Gradle daemon만 JDK 21. 앱 바이트코드는 17 | [ARCHITECTURE](ARCHITECTURE.md) 10절, REQUIREMENTS R-006(조건부) |
| Timber 초기화 | Application에서 직접 Tree 등록 | 이전 프로젝트의 App Startup initializer 방식 | debug 빌드에서만 Tree 등록 |
| UI 조정 | 기기 확인 중 필터 시트를 닫으면 키보드가 다시 뜨고, 목록 카드가 붙어 보임 | 시트·메뉴를 열기 전 포커스 해제, 카드 사이 간격 추가 | - |
| 상세 중복 진입 | 흔치 않은 경우로 보고 우선순위를 낮춤 | 두 손가락 동시 탭으로 상세가 두 번 쌓인 이전 경험을 근거로 대응 요청 | Compose 테스트로 동시 탭 확인 |
| QA | 자동 테스트와 일부 기기 확인으로 완료 판단 | 요구사항 기반 QA 시나리오를 실기기에서 전부 실행해 통과시킬 것 | [QA](QA.md) |
| 2-pane | 선택 항목이라 미구현 | 남은 일정에 구현하고 태블릿 실기기 확인과 시연 영상 추가 요청 | [SPEC](SPEC.md) 10절, [QA](QA.md) TC-90~94, `demo-tablet.mp4` |
| 화면 상태 구조 | Presenter 안에 흩어진 `rememberSaveable`·retain 값과 공식 retain 문서의 저장·retain 데이터 분리 기준을 정리해 보고 | TanStack Query + Zustand처럼 서버 상태(Soil·Room)와 화면 Store를 나누고, Store는 공식 문서 방식대로 구성 | [ARCHITECTURE](ARCHITECTURE.md) "화면 Store", `SearchStateRestorationTest`, `FavoritesStoreLifecycleTest` |

## 단계 요약

| 단계 | AI가 한 일 | 개발자 판단 | 증거 |
| --- | --- | --- | --- |
| 설계 | 요구사항 정리, 참고 코드·API 조사, 위험 식별, 문서 초안 | retain + Nav3, Retrofit·Room, Soil, Metro·Coil 선택 | [REQUIREMENTS](REQUIREMENTS.md), [ARCHITECTURE](ARCHITECTURE.md) 9절, ADR-001~004 |
| 사양 확정 | 원문·API·Soil·DroidKaigi 대조로 기본안 작성, 모듈·버전 확인 | 구현을 AI에 맡김, Retrofit 유지, 9개 모듈 | [SPEC](SPEC.md), ADR-005·006 |
| 구현 | 빌드 구성, 9개 모듈 코드와 테스트, 기능 단위 빌드·테스트 | daemon JDK 21, App Startup, UI 조정, 중복 진입 대응 | ADR-007, [ARCHITECTURE](ARCHITECTURE.md) 10절 |
| 실기기 QA | 요구사항 기반 시나리오 작성, adb로 실행, 회귀 테스트 추가 | 시나리오 전부 실기기 실행·통과 요구 | [QA](QA.md) |
| 2-pane | list-detail 구현, 태블릿 실기기 QA, 시연 영상 | 남은 일정에 구현 요청 | [SPEC](SPEC.md) 10절, [QA](QA.md) TC-90~94 |
| 제출 준비 | R-ID별 검증 기록, README, 시연 영상, 제출 ZIP 스크립트 | ZIP에 `.git`·`local.properties`·APK·영상 포함, 최종 패키지 직접 확인 | [REQUIREMENTS](REQUIREMENTS.md) 9절, R-072~077·084 |

제출 전 AI가 실행한 실기기 QA로 문제를 찾았고, 회귀 테스트(`SearchQueryRetentionTest`, `FavoritesScreenTest`)를 추가했습니다. 자동 테스트 결과와 QA 결과는 [README](../README.md) 검증 결과 절과 [QA](QA.md)에 있습니다.

## 참고 코드와 보안

참고 저장소와 기준 커밋은 [ARCHITECTURE](ARCHITECTURE.md) 9절에 기록했습니다. 개념 참고와 코드 복사는 구분하며, 코드를 가져올 경우 라이선스와 출처 표기 조건을 먼저 확인하고 반영 범위를 남깁니다.

API 키, 인증정보, 개인 이메일, 원본 비공개 메시지를 문서나 Git에 넣지 않습니다. 제출용 local.properties는 Git에서 제외하고 사용자가 최종 패키지와 전달 대상을 확인합니다.

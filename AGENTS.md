# 저장소 작업 지침

## 범위와 기준

- 문서와 작업 설명은 한국어로 작성합니다.
- 작업 전 [요구사항](docs/REQUIREMENTS.md), [구현 전략](docs/STRATEGY.md), [아키텍처](docs/ARCHITECTURE.md), 현재 코드를 확인합니다. 필수 요구사항, 사용자 선택, 권장안, 미정 사항을 구분합니다.
- 역할은 설계·문서화·조사·리뷰입니다. 앱 구현은 사용자의 Claude Opus 5.5가 담당합니다. 이 설계·리뷰 작업에서는 앱 코드, 빌드 설정, 의존성을 추가하거나 변경하지 않습니다. 역할 변경은 사용자의 명시적 요청이 있을 때만 반영합니다.
- retain + Navigation 3는 사용자 선택이며 Circuit 라이브러리는 도입하지 않습니다. 임의로 ViewModel 중심 구조로 전환하지 않습니다. 현재 권장안은 DroidKaigi 방식의 얇은 @Composable Presenter이며, 변경 불가능한 결정은 아닙니다. 순수 Kotlin 테스트만을 위해 별도의 전체 StateFlow 상태 소유자를 중복 도입하지 않습니다. 원격 통신은 Retrofit + OkHttp + kotlinx.serialization, 로컬 저장은 Room으로 선택했습니다. Soil typed-key + InfiniteQuery도 선택했으며 단순 위임 Repository는 추가하지 않습니다. Query/InfiniteQuery는 Retrofit에 연결합니다. 로컬은 Subscription→Room Flow / Mutation→Room 쓰기로 진행하며, 즐겨찾기의 단일 상태 기준은 Room입니다. DroidKaigi 2026을 참고해 과거 우려를 해소한다는 사용자 판단을 따릅니다. 과거 롤백 경험은 수명·반영 순서 테스트에 활용하고, 이를 이유로 직접 Flow 비교를 필수화하거나 Repository를 재도입하지 않습니다.
- Metro와 Coil은 사용자 선택입니다. Coil 3는 현재 권장 세대이며 버전은 미고정입니다. 최소 Metro 그래프를 사용하고, 이미지 클라이언트에는 Kakao API 인증 interceptor를 전달하지 않습니다. Kotlin 2.4.0과 Metro 컴파일러 호환성을 먼저 확인합니다.
- Metro의 선언형 factory와 retain/Nav3 연결을 최소 구성으로 사용합니다. 자체 KSP 편의 함수 생성기나 자체 컴파일러 플러그인을 추가하지 않습니다. Metro가 생성하는 factory 구현과 Room용 KSP는 구분합니다.
- Android 전용으로 구성합니다. KMP source set, expect/actual, 플랫폼 추상화를 추가하지 않습니다. DroidKaigi에서는 retain/Navigation 3 소유권과 Presenter·테스트 패턴만 필요한 범위에서 참고합니다.
- 지정 빌드 버전은 구현 시작 시 호환성을 먼저 검증합니다. 충돌을 숨기거나 요청 버전을 임의로 바꾸지 않습니다.

## 작업과 변경 기록

- 이번 과제에서는 이슈를 생성하지 않습니다. 진행 상태는 요구사항 체크리스트와 문서로 관리합니다.
- 커밋은 검토 가능한 작은 기능·수정 단위로 나눕니다. 문서만 요청된 커밋에는 Markdown 파일만 포함합니다.
- PR은 필요와 요청에 따라 사용합니다. 이슈 생략을 모든 PR의 금지로 해석하지 않습니다.
- 커밋 메시지나 PR 본문에 자동 `Generated-by-Claude` 문구나 AI를 공동 작성자로 표시하는 `Co-authored-by` 트레일러를 추가하지 않습니다.
- 실제 작성자·출처·라이선스 표기는 보존합니다. 자동 표기를 줄이기 위해 기존 이력을 재작성하거나 실제 기여를 숨기지 않습니다.
- AI 활용 범위, 개발자 판단, 근거와 검증 결과는 README 및 [AI_USAGE.md](docs/AI_USAGE.md)에 사실대로 반영합니다.

## 검증과 보안

- 사용자가 테스트 코드 작성을 필수로 요청했습니다. 별도 구현 요청 이후 핵심 단위·상태 경합·페이지네이션 테스트, Room 통합 테스트, 필수 Compose 내비게이션·적응형 화면·retain 수명 검증을 3일 구현 범위에 포함합니다. 원 과제의 명시 조건과 이 추가 요구를 구분합니다.
- 계획한 테스트, 작성한 테스트, 실행한 테스트를 구분합니다. 실행하지 않은 빌드·테스트를 성공했다고 기록하지 않습니다.
- 변경한 요구사항과 관련 경계값을 확인하고, 실제 실행 명령·결과·남은 제약을 기록합니다.
- Soil fetch/Mutation은 T 반환·예외 전파 계약을 유지합니다. runSuspendCatching으로 무조건 감싸 Result.failure를 성공 데이터로 만들지 않습니다. Subscription의 Flow 취소 투명성도 유지하며, helper는 Soil 밖에서 Result 계약이 필요한 경우에만 검토합니다.
- API 키, 인증정보, 개인 이메일, 비공개 원문, local.properties를 Git에 넣지 않습니다. 비공개 저장소에도 동일하게 적용합니다.
- 제출용 ZIP의 local.properties 포함은 Git 추적과 별도 절차입니다. 실제 키 설정 및 최종 제출 패키지는 사용자가 확인합니다.

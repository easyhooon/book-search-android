# 요구사항 체크리스트

기준일: 2026-10-08. 체크는 확인 방법을 실제로 실행해 통과한 항목에만 했습니다. 근거와 남은 조건은 9절에 있습니다.

- **원문 필수**: 제공된 과제에 명시된 조건
- **사용자 추가 필수**: 사용자가 원 과제 조건에 추가한 완료 기준
- **선택 결정**: 사용자가 선택한 기술 방향
- **권장**: 필수 조건을 안정적으로 달성하기 위한 제안
- **제품 선택**: 원문이 정하지 않은 사용자 경험의 세부 선택
- **구현 계약·품질 기준**: 명시 요구를 충족하기 위해 구체화한 동작·검증 기준. 별도 사용자 재승인 항목이 아님
- **미정**: 제품 선택 또는 아직 검증하지 않은 기술 세부사항

## 체크와 증거 기록 규칙

- 확인 방법을 실제로 실행하고 증거를 남긴 항목만 체크합니다. 코드 작성 완료와 검증 완료를 구분하며, 테스트 파일이나 문서가 있다는 이유로 체크하지 않습니다.
- 구현 상태: 미착수 / 진행 / 구현 완료 / 해당 없음. 검증 상태: 미검증 / 통과 / 실패 / 차단 / 해당 없음.
- 9절에 증거(검증 커밋·환경·명령·테스트 이름과 결과)와 미충족 사항(남은 조건·차단 원인)을 적습니다. 관련 코드가 바뀌면 영향받는 항목을 재검증합니다.
- 원문 필수는 임의로 해당 없음 처리하지 않습니다. R-ID는 재배치하지 않고 새 항목은 새 번호로 추가합니다.

각 항목을 코드로 옮길 때의 세부 계약(ID, 화면 문구, 백스택, 오류 분류 등)은 [구현 사양](SPEC.md)을 따릅니다.

## 1. 환경과 빌드: 원문 필수

- [x] **R-001** Kotlin, Kotlin Coroutines, Flow, Jetpack Compose 사용
- [x] **R-002** Gradle 9.4.1
- [x] **R-003** Android Gradle Plugin 9.2.1
- [x] **R-004** Kotlin 2.4.0
- [x] **R-005** KSP를 사용하는 경우 2.3.10
- [ ] **R-006** JDK 17
- [x] **R-007** compileSdk 37
- [ ] **R-008** 최신 안정 Android Studio에서 열기 및 빌드 확인
- [x] **R-009** Gradle Wrapper 파일과 실행 권한 포함
- [x] **R-010** 별도 설명 없이도 재현 가능한 `./gradlew assembleDebug` 성공
- [x] **R-011** 요청 버전 조합의 호환성 확인. 충돌 시 임의 변경하지 않고 실패 로그·공식 근거·대안 기록

minSdk 26, targetSdk 37과 빌드에 성공한 라이브러리 버전은 [구현 사양](SPEC.md) 2절에 고정했습니다.

## 2. 원격 도서 검색

### 원문 필수

- [x] **R-012** 하단 탭 2개: 검색 탭과 즐겨찾기 탭 제공
- [x] **R-013** Kakao 책 검색 API에 검색어 전달
- [x] **R-014** 정확도순 / 최신순 선택. API 값은 `accuracy` / `latest`, 최신순 의미는 발간일순
- [x] **R-015** 페이지당 20개 요청 및 다음 페이지 로드
- [x] **R-016** 공통 도서 카드에 표지, 제목, 저자, 출판사, 출판일, 가격 표시
- [x] **R-017** 카드에서 즐겨찾기 추가·해제
- [x] **R-018** 카드를 선택하면 상세 화면으로 이동

### 구현 계약·품질 기준

- [x] **R-019** 검색·정렬 변경 후 이전 조건의 결과가 섞이지 않음
- [x] **R-020** 검색 결과 없음 / 최초 로딩 / 최초 오류 / 추가 로딩 / 추가 오류 구분

API 계약 근거: [Kakao 책 검색 REST API](https://developers.kakao.com/docs/ko/daum-search/dev-guide#search-book).

## 3. 즐겨찾기

### 원문 필수

- [x] **R-021** 도서 정보를 로컬에 영속 저장
- [x] **R-022** 즐겨찾기 목록 안에서 로컬 검색
- [x] **R-023** 제목 오름차순 / 내림차순 정렬
- [x] **R-024** 가격 조건 필터
- [x] **R-025** 즐겨찾기 목록에서 상세로 이동
### 구현 계약·품질 기준

- [x] **R-026** 앱 종료 및 재실행 후 즐겨찾기 유지
- [x] **R-027** 검색·즐겨찾기·상세에서 동일 도서의 즐겨찾기 상태 일치
- [x] **R-028** 저장/삭제 실패 시 성공으로 표시하지 않음

### 가격 필터와 검색·정렬 구현 규칙

- 가격 필터는 6절 정책 2를 따릅니다. 최소 > 최대는 적용을 막고 안내하며, 범위 미설정 시 정보 없음도 포함합니다.
- 로컬 검색은 제목 또는 저자의 부분 일치(영문 대소문자 무시)이며 검색 버튼/IME로 확정합니다. 로컬 검색 기능 자체는 원문 필수이고 세부 UX는 선택입니다.
- 정렬은 앞뒤 공백을 제거한 제목을 `Collator(Locale.KOREAN)`으로 비교하고, 같은 제목은 ID로 순서를 고정합니다.

## 4. 상세 화면

### 원문 필수

- [x] **R-029** 도서 표지와 제목 등 도서 정보 표시
- [x] **R-030** 즐겨찾기 상태 표시 및 변경
### 구현 계약·품질 기준

- [x] **R-031** 목록과 상세의 즐겨찾기 변경 결과 일치
- [x] **R-032** 저장된 도서는 네트워크 없이도 로컬 정보로 상세 표시
- [ ] **R-033** 누락된 표지·저자·날짜·가격 등으로 화면이 깨지지 않음
- [x] **R-087** 상세에서 즐겨찾기를 해제해도 표시 중인 도서 정보가 사라지지 않음

오프라인에서 텍스트 정보는 DB로 보장하고, 이미지 캐시가 없으면 대체 이미지를 보여 줍니다. 원본 표지까지 보장하려면 이미지 파일 저장을 추가 설계해야 합니다.

## 5. 품질과 확장성

### 원문에 명시된 요구

- [x] **R-034** 불안정한 네트워크에서도 앱 사용 가능
- [ ] **R-035** 태블릿·폴더블 대응
- [x] **R-036** 대규모 앱의 기능 중 하나라는 전제로 구조 설계
- [x] **R-037** 여러 개발자가 확장할 수 있는 책임·의존성 경계 설명
- [x] **R-038** 구현한 설계의 선택 이유와 단점 설명
- [x] **R-039** 검토했으나 구현하지 않은 대안과 미구현 범위 설명

### 요구를 구체화한 품질 기준과 선택 범위

- [x] **R-040** 오류 후 재시도 가능, 추가 로딩 오류가 기존 목록을 제거하지 않음
- [x] **R-041** 창 크기 변화 중 입력·목록·선택 상태를 보존하는 동작 검증
- [x] **R-085** 탭·백스택 규칙 준수: 같은 탭 재선택 시 루트로 복귀하고 백스택 순서가 뒤집히지 않음, 상세 위에 상세가 쌓이지 않음(두 카드 동시 터치 포함), 즐겨찾기 루트에서 뒤로 가기 시 검색 탭으로 이동
- [x] **R-086** API 키가 없어도 빌드가 성공하고, 검색 화면에서 키 미설정을 안내하며 즐겨찾기는 동작

태블릿·폴더블 대응은 원문 필수이고 list-detail 2-pane은 선택입니다(6절 정책 5, [구현 사양](SPEC.md) 10절).

오프라인 즐겨찾기·상세는 기본 범위입니다. 원격 검색 결과를 디스크에 보관해 재시작 후 제공하는 기능은 필수가 아니며, 범위는 온라인 검색, 메모리 내 기존 결과 유지, 오류/재시도, 로컬 즐겨찾기입니다.

## 6. 선택 결정과 권장 설계

- [x] **R-042** **사용자 선택**: retain + Navigation 3로 진행. 학습 목표를 유지하며 검증
- [x] **R-043** **사용자 선택**: Room으로 영속 저장. 원 과제에서 Room 라이브러리를 지정한 것은 아님
- [x] **R-044** **사용자 선택**: Room Flow를 즐겨찾기의 단일 상태 기준으로 사용
- [x] **R-045** **사용자 선택**: 원격 통신은 Retrofit + OkHttp + kotlinx.serialization 사용
- [x] **R-046** **권장**: MockWebServer API 계층 테스트. 구체적인 의존성 버전은 검증 후 확정
- [x] **R-047** **사용자 선택**: Soil typed-key + InfiniteQuery로 데이터 접근·페이지네이션 구성, 단순 위임 Repository 생략
- [x] **R-048** **사용자 선택**: Metro 의존성 주입, Coil 이미지 로딩. Coil 3는 현재 권장안
- [x] **R-049** **설계 검증**: Metro/Kotlin 컴파일러 호환, API와 이미지 네트워크 인증 설정 분리. 구체적인 구조는 [아키텍처](ARCHITECTURE.md) 참고
- [x] **R-050** **설계 검증**: query/sort별 key, size=20, 페이지 메타데이터·is_end 유지, 로딩·취소·재시도 동작을 테스트로 확인. 커스텀 페이지네이션과 Paging 3는 미채택 대안
- [x] **R-051** **로컬 연결 검증**: 선택한 Soil Subscription→Room Flow / Mutation→Room 쓰기에서 Room 단일 상태 기준, 반영 순서·지연, 구독 수명·재구독을 테스트. 과거 우려를 근거로 Repository를 재도입하지 않음
- [x] **R-052** **오류 정책 검증**: Retrofit 예외의 재시도 가능 여부를 명시적으로 분류하고 CancellationException을 보존
- [x] **R-053** **결정**: `app`, `core:model`, `core:network`, `core:database`, `core:data`, `core:ui`, `feature:search`, `feature:favorites`, `feature:detail`의 9개 모듈로 경계를 나누고, 그 이상의 모듈 증설은 피하기
- [x] **R-054** **현재 제품 정책**: 시스템 프로세스 재생성 시 query/sort/filter/탭과 상세 도서 스냅샷을 복원하고 검색은 재조회. 결과 목록 전체를 saveable에 넣지 않음

### 제품 선택 5개의 현재 정책

사용자의 도서 앱 사례 참고 방향에 따라 정했습니다. 모두 특정 앱에서 직접 관찰한 것은 아니며, 확인된 UI 사례와 이 앱의 기술 정책을 구분합니다. 근거와 한계는 [ARCHITECTURE](ARCHITECTURE.md) 9절에 있습니다.

1. 검색 실행 시점: 검색 버튼 또는 키보드 IME 제출로 확정합니다. 로컬 검색에도 같은 방식을 적용합니다.
2. 가격 필터: 정가 최소·최대 금액, 양끝 포함, 적용 버튼. 빈칸은 제한 없음, 최소 > 최대는 적용 금지. 누락·음수는 정보 없음, 0원은 유효하며 범위 적용 시 정보 없음은 제외합니다.
3. 저장 중 버튼/pop: 해당 도서의 저장 버튼만 비활성화하고 화면 이동은 허용합니다. 짧은 DB 쓰기는 명시적으로 관리되는 화면 외부 scope에서 책별 순서를 지켜 완료합니다. 프로세스가 죽으면 미완료 쓰기의 완료를 보장하지 않습니다.
4. 복원: 시스템 프로세스 재생성 시 조건·탭과 상세 엔트리를 복원합니다. 상세는 진입 시점의 도서 스냅샷을 내비게이션 키에 담아 네트워크 없이 다시 표시합니다(Kakao API에 단건 조회가 없음, [구현 사양](SPEC.md) 7절). 검색 결과는 조건으로 재조회하며, 복원을 위해 즐겨찾기에 자동 저장하지 않습니다. 강제 종료 뒤 콜드 스타트에 같은 복원을 보장하지 않습니다.
5. 큰 화면: 안정적인 1-pane과 크기 변경 대응을 우선하고, expanded 너비(840dp 이상)에서는 목록과 상세를 나란히 보여 주는 2-pane을 제공합니다.

로컬 저장, 가격 필터 기능 자체, 불안정한 네트워크 대응, 태블릿·폴더블 지원은 원문 필수입니다. 확정 검색 조건 기반 페이징, ISBN 정규화·대체 ID, DB 쓰기 순서, 크기 변경 중 상태 보존은 구현 계약·테스트로 정리하며 사용자에게 다시 승인받을 사항으로 늘리지 않습니다.

## 7. 우선 검증 시나리오

### 사용자 추가 필수: 테스트 코드

원 과제의 명시 조건과 별개로 사용자가 요청한 필수 완료 기준입니다. 작성·실행 결과는 [QA](QA.md)의 자동 테스트 절에 있습니다.

- [x] **R-055** 핵심 단위 테스트: ID·가격 정책, 검색 상태 전이, 요청 역전·취소·초기화 경합
- [x] **R-056** 페이지네이션 테스트: 마지막 20개 + is_end, 실패한 동일 페이지 재시도, 중복 요청·ISBN 중복
- [x] **R-057** Room 통합 테스트: 저장·삭제·Flow 관찰, DB 재열기 후 영속성. 쓰기 실패는 Fake/실패 주입 테스트로 별도 확인
- [x] **R-058** 필수 Compose UI 테스트: 탭·상세 이동과 복귀, 창 크기별 UI, retain 상태 유지·엔트리 제거 경로
- [x] **R-059** 회전·폴더블·프로세스 종료 등 자동화가 제한된 검증은 기기/에뮬레이터 수동 확인으로 보완하고 자동 테스트와 구분
- [x] **R-060** 테스트별 실제 실행 명령·환경·결과 기록. 작성만으로 통과 처리하지 않으며, 미실행·실패는 이유와 함께 별도 표시

기기나 실행 환경이 없어 검증이 막히면 테스트 작성 상태와 실행 차단 상태를 나누어 기록합니다.

- [x] **R-061** 검색어 빠른 변경, 정렬 변경, 빈 검색어로 초기화
- [x] **R-062** 마지막 페이지가 20개여도 `meta.is_end`에 따라 요청 종료
- [x] **R-063** 추가 로딩 실패 후 같은 페이지 재시도, 중복 트리거 차단
- [x] **R-064** 페이지 간 동일 ISBN 중복 제거
- [x] **R-065** 가격 경계값, 0원/가격 없음, 잘못된 범위, 필터 결과 0개
- [x] **R-066** 오프라인 즐겨찾기·상세 및 앱 재시작 후 복원
- [x] **R-067** Room 쓰기 실패, 즐겨찾기 연타, 쓰기 중 뒤로 가기
- [ ] **R-068** 화면 회전, 창 크기 변경, 폴더블 펼침·접힘
- [x] **R-069** 상세 진입·복귀 시 상태 유지, 백스택 제거 시 작업 정리
- [x] **R-070** 시스템 프로세스 재생성 후 최소 상태 복원, 저장·미저장 도서 모두 상세를 네트워크 없이 다시 표시
- [x] **R-071** 저장 중 화면 pop 이후 쓰기 완료·도서별 순서 확인. 프로세스 종료 시 미완료 쓰기 보장과 구분

## 8. 제출: 원문 필수와 추가 검증

마감: **2026-10-10(토) 14:30 KST**. 아래는 제출 준비 체크입니다. 제출 자체는 사용자가 합니다.

- [x] **R-072** 소스 코드 ZIP
- [x] **R-073** ZIP 안에 `.git` 포함
- [x] **R-074** API 키를 `local.properties`에서 읽도록 구성하고, 제출 ZIP에도 해당 파일 포함
- [ ] **R-075** 제출에 사용한 API 키를 전형 종료 전까지 폐기하지 않음
- [x] **R-076** ZIP에서 모든 `build/`, `.gradle/` 제외
- [x] **R-077** APK 포함. Debug APK 허용
- [ ] **R-078** README 포함: 빌드 방법, 사용 프레임워크, 구조, 주요 구현 포인트(원문 요구), 선택 이유·실제 검증 결과·한계, AI 활용 시 활용 작업·검증 방법·직접 판단해 바꾼 부분
- [x] **R-079** **선택**: 3분 이내 시연 영상
- [x] **R-080** GitHub 저장소명·설명·문서 등 저장소 콘텐츠에 회사명 미포함

### 추가 보안·재현성 검증

- [x] **R-081** 실제 API 키가 Git 현재 파일·전체 이력·문서·로그에 없는지 확인
- [x] **R-082** ZIP에 포함될 `.git/config` 및 remote URL에 자격증명이 없는지 사용자 확인
- [ ] **R-083** 최종 키 설정·민감정보 포함 여부·제출 대상은 사용자가 직접 확인
- [x] **R-084** 압축을 새 디렉터리에 풀고 빌드 및 APK 설치 확인

local.properties는 Git에서 제외하고 제출 ZIP에만 넣습니다. `git archive`로는 `.git`과 무시된 local.properties가 들어가지 않으므로 제출 형식을 따로 검증합니다.

## 9. 항목별 구현·검증 기록

공통 증거는 아래 두 가지이며 표에서는 이름만 적습니다.

- **자동 테스트**: 2026-10-08, 커밋 `71cdf67`, macOS 26.3.1, Launcher JVM 17.0.10, Gradle daemon JVM 21. `./gradlew assembleDebug testDebugUnitTest test --continue` 성공, 110개 통과(실패·오류·건너뜀 0). 표의 테스트 클래스 이름은 이 실행에 포함됩니다.
- **TC-NN**: [QA](QA.md)의 실기기 시나리오. SM-S936N, Android 17(API 37), 커밋 `22855b3` debug 빌드, 2026-10-07~08. 이후 커밋 `71cdf67`은 테스트만 추가해 앱 동작이 같습니다.

"부분 통과"·"조건부"는 요구 전체를 확인하지 못해 체크하지 않은 항목입니다.

| 항목 | 구현 상태 | 검증 상태 | 실제 증거 | 미충족 사항 |
| --- | --- | --- | --- | --- |
| R-001 | 구현 완료 | 통과 | 빌드 성공. Coroutines·Flow(Room Flow, Soil), Compose 화면 | 없음 |
| R-002 | 구현 완료 | 통과 | Wrapper `gradle-9.4.1-bin.zip`, 자동 빌드 | 없음 |
| R-003 | 구현 완료 | 통과 | `libs.versions.toml` AGP 9.2.1, 자동 빌드 | 없음 |
| R-004 | 구현 완료 | 통과 | `libs.versions.toml` Kotlin 2.4.0, 자동 빌드 | 없음 |
| R-005 | 구현 완료 | 통과 | KSP 2.3.10(Room 컴파일러), 자동 빌드 | 없음 |
| R-006 | 구현 완료 | 조건부 | 컴파일 기준 17(`JavaVersion.VERSION_17`, `JvmTarget.JVM_17`), Launcher JVM 17로 실행 | Metro 1.4.5 Gradle 플러그인 요구로 Gradle daemon만 JDK 21(사용자 결정, [ARCHITECTURE](ARCHITECTURE.md) 10절). "JDK 17 전용"과 달라 미체크 |
| R-007 | 구현 완료 | 통과 | `compileSdk = "37"`, 자동 빌드 | 없음 |
| R-008 | 구현 완료 | 미검증 | 명령행 빌드만 확인 | Android Studio에서 열어 Sync·실행하는 확인은 사용자가 수행 |
| R-009 | 구현 완료 | 통과 | `git ls-files -s`: `gradlew` 100755, `gradle-wrapper.jar` 추적. 키 없는 worktree에서 `./gradlew :app:installDebug` 성공(TC-86) | 없음 |
| R-010 | 구현 완료 | 통과 | 빌드 산출물·키 없는 새 worktree에서 Wrapper로 빌드·설치(TC-86), 자동 빌드 | ZIP 재빌드는 R-084 |
| R-011 | 구현 완료 | 통과 | 지정 버전 그대로 빌드. Metro의 JVM 21 요구는 실패 로그·공식 근거·대안을 [SPEC](SPEC.md) 2절, [ARCHITECTURE](ARCHITECTURE.md) 10절에 기록 | 없음 |
| R-012 | 구현 완료 | 통과 | TC-01, `BookSearchNavHostTest` | 없음 |
| R-013 | 구현 완료 | 통과 | TC-02(`query=kotlin` 요청 로그), `KakaoBookServiceTest` | 없음 |
| R-014 | 구현 완료 | 통과 | TC-03(`sort=latest`, 출간일 내림차순), `SearchPresenterTest` 정렬 변경 | 없음 |
| R-015 | 구현 완료 | 통과 | TC-04(page 2·3·4 순차 요청), `SearchPresenterTest`, `SearchPagingTest` | 없음 |
| R-016 | 구현 완료 | 통과 | TC-02 카드 필드, `BookFormattersTest` | 없음 |
| R-017 | 구현 완료 | 통과 | TC-07, TC-27, `SearchPresenterTest` 하트 | 없음 |
| R-018 | 구현 완료 | 통과 | TC-08, `BookSearchNavHostTest` | 없음 |
| R-019 | 구현 완료 | 통과 | `SearchPresenterTest` 늦은 응답 3건(검색어·정렬·빈 검색어), TC-03·19 | 기기에서는 늦은 응답 경합 재현 못 함(TC-19) |
| R-020 | 구현 완료 | 통과 | TC-05(결과 없음), TC-20(최초 오류), TC-04(추가 로딩), TC-40(추가 오류), `SearchPresenterTest` | 없음 |
| R-021 | 구현 완료 | 통과 | TC-07·10, `FavoriteBookDaoTest` | 없음 |
| R-022 | 구현 완료 | 통과 | TC-11, `FavoritesPresenterTest`, `FavoriteListRulesTest` | 없음 |
| R-023 | 구현 완료 | 통과 | TC-10·12·12b, `FavoritesPresenterTest`, `FavoriteListRulesTest` | 없음 |
| R-024 | 구현 완료 | 통과 | TC-13, `PriceFilterTest`, `FavoritesPresenterTest` | 없음 |
| R-025 | 구현 완료 | 통과 | TC-25 | 없음 |
| R-026 | 구현 완료 | 통과 | TC-26(`am force-stop` 후 재실행), `FavoriteBookDaoTest` 파일 DB 재열기 | 없음 |
| R-027 | 구현 완료 | 통과 | TC-09·27·67·71, `FavoriteSubscriptionKeysTest` | 없음 |
| R-028 | 구현 완료 | 통과 | `FavoriteWriterTest`(실패 전파), `FavoriteTogglerTest`, 세 Presenter 테스트의 쓰기 실패 | 기기 Room 쓰기 실패는 Fake 주입으로만 확인 |
| R-029 | 구현 완료 | 통과 | TC-08, `BookDetailPresenterTest` | 없음 |
| R-030 | 구현 완료 | 통과 | TC-08·09·71, `BookDetailPresenterTest` | 없음 |
| R-031 | 구현 완료 | 통과 | TC-09(상세 해제 후 목록 반영) | 없음 |
| R-032 | 구현 완료 | 통과 | TC-32(비행기 모드) | 없음 |
| R-033 | 구현 완료 | 부분 통과 | `BookMapperTest`(누락·음수 가격, 빈 문자열), `BookFormattersTest`(빈 값 null), 표지 없음 대체 이미지 | 필드가 빠진 실제 도서를 기기에서 확인 못 함 |
| R-034 | 구현 완료 | 통과 | TC-20·40·40b·42·32·70a | 재연결 시 자동 재요청 없음, 재시도 버튼으로 복구([SPEC](SPEC.md) 9절) |
| R-035 | 구현 완료 | 부분 통과 | TC-35(1600x2560·320dpi, 레일·다열 그리드), TC-68c(창 크기 변경), 태블릿 SM-X710 2-pane TC-90~94, `BookSearchNavHostTest` 좁은/넓은 창·2-pane | 폴더블 기기·AVD 접힘·펼침 미실행 |
| R-036 | 구현 완료 | 통과 | 9개 모듈 구성과 빌드, [ARCHITECTURE](ARCHITECTURE.md) 8절 | 없음 |
| R-037 | 구현 완료 | 통과 | [ARCHITECTURE](ARCHITECTURE.md) 8절, [SPEC](SPEC.md) 2절 모듈 책임 | 없음 |
| R-038 | 구현 완료 | 통과 | [ARCHITECTURE](ARCHITECTURE.md) 9절(선택 이유와 비용) | 없음 |
| R-039 | 구현 완료 | 통과 | [ARCHITECTURE](ARCHITECTURE.md) 9절 미채택 대안(ViewModel, Repository, Paging 3) | 없음 |
| R-040 | 구현 완료 | 통과 | TC-40·40b·42, `SearchPresenterTest` 추가 오류 후 재시도 | 없음 |
| R-041 | 구현 완료 | 통과 | TC-41·68a·68c·94, `BookSearchNavHostTest` 구성 변경·2-pane↔1-pane, `FavoritesScreenTest` | 없음 |
| R-042 | 구현 완료 | 통과 | `RetainedValuesStoreNavEntryDecorator`, `BookSearchNavHostTest` retain 수명 4건 | 없음 |
| R-043 | 구현 완료 | 통과 | `FavoriteBookDaoTest`, TC-26 | 없음 |
| R-044 | 구현 완료 | 통과 | `FavoriteSubscriptionKeysTest`, TC-27 | 없음 |
| R-045 | 구현 완료 | 통과 | `KakaoBookServiceTest` | 없음 |
| R-046 | 구현 완료 | 통과 | `KakaoBookServiceTest`(MockWebServer 4건) | 없음 |
| R-047 | 구현 완료 | 통과 | `SearchBooksQueryKeyTest`, `SearchPresenterTest`. Repository 없음 | 없음 |
| R-048 | 구현 완료 | 통과 | Metro 1.4.5 그래프 빌드, Coil 3.6.3 표지 표시(TC-02) | 없음 |
| R-049 | 구현 완료 | 통과 | Kotlin 2.4.0 + Metro 1.4.5 빌드. 이미지 로더는 인증 interceptor 없는 클라이언트(`AppGraph.provideImageLoader`), interceptor는 API 클라이언트에만(`NetworkFactory`). 로그는 Authorization을 가림 | 없음 |
| R-050 | 구현 완료 | 통과 | `SearchBooksQueryKeyTest`, `SearchPagingTest`, `SearchPresenterTest` | 없음 |
| R-051 | 구현 완료 | 통과 | `FavoriteSubscriptionKeysTest`, `FavoriteWriterTest`, Presenter 테스트 | 없음 |
| R-052 | 구현 완료 | 통과 | `ErrorKindTest`, `KakaoBookServiceTest`, `FavoriteTogglerTest`(취소를 실패로 알리지 않음) | 없음 |
| R-053 | 구현 완료 | 통과 | `settings.gradle.kts`의 9개 모듈 + 테스트 전용 `core:testing` | 없음 |
| R-054 | 구현 완료 | 통과 | TC-54·70b(`am kill` 후 복원) | 실제 메모리 압박으로 인한 종료는 만들지 않음 |
| R-055 | 구현 완료 | 통과 | `BookIdentityTest`, `PriceFilterTest`, `SearchPresenterTest` 경합 3건 | 없음 |
| R-056 | 구현 완료 | 통과 | `SearchPagingTest`, `SearchPresenterTest`(20개 마지막 페이지, 같은 페이지 재시도, 중복 ID) | 없음 |
| R-057 | 구현 완료 | 통과 | `FavoriteBookDaoTest` 4건(파일 DB 재열기 포함), 쓰기 실패는 `FavoriteWriterTest`·Fake 주입 | 없음 |
| R-058 | 구현 완료 | 통과 | `BookSearchNavHostTest` 9건, `FavoritesScreenTest`, `SearchQueryRetentionTest` | 없음 |
| R-059 | 구현 완료 | 통과 | [QA](QA.md)에 실기기 시나리오와 자동 테스트를 나눠 기록 | 폴더블 고유 상태는 미실행(R-035, R-068) |
| R-060 | 구현 완료 | 통과 | [QA](QA.md) 자동 테스트 절, [ARCHITECTURE](ARCHITECTURE.md) 10절 | 없음 |
| R-061 | 구현 완료 | 통과 | `SearchPresenterTest`, TC-06·19 | 없음 |
| R-062 | 구현 완료 | 통과 | `SearchPresenterTest`(20개 + is_end), `SearchPagingTest` | 없음 |
| R-063 | 구현 완료 | 통과 | `SearchPresenterTest`, TC-40(오류 뒤 스크롤 요청 0건)·40b | 없음 |
| R-064 | 구현 완료 | 통과 | `SearchPagingTest`, `SearchPresenterTest` 중복 ID | 없음 |
| R-065 | 구현 완료 | 통과 | `PriceFilterTest`, `PriceInputTest`, `FavoritesPresenterTest`, TC-13 | 없음 |
| R-066 | 구현 완료 | 통과 | TC-26·32·70a·70b | 없음 |
| R-067 | 구현 완료 | 통과 | 쓰기 실패: Fake 주입. 연타: TC-67, `FavoriteTogglerTest`. 쓰기 중 뒤로: TC-71, `FavoriteWriterTest` | 기기 Room 쓰기 실패는 미재현 |
| R-068 | 구현 완료 | 부분 통과 | 회전 TC-41·68a·94(태블릿 2-pane↔1-pane), 창 크기 변경 TC-68c, 다크 모드 TC-68b | 폴더블 접힘·펼침 미실행 |
| R-069 | 구현 완료 | 통과 | `BookSearchNavHostTest` retain 은퇴 3건, `SearchQueryRetentionTest`, TC-40b·85 | 없음 |
| R-070 | 구현 완료 | 통과 | TC-70a·70b(미저장 도서), TC-32(저장 도서) | 없음 |
| R-071 | 구현 완료 | 통과 | `FavoriteWriterTest` 화면 취소 후 완료·직렬 실행, TC-71 | 프로세스 종료 시 미완료 쓰기는 보장하지 않음(정책) |
| R-072 | 구현 완료 | 통과 | 2026-10-08 `scripts/make-submission-zip.sh`로 `book-search-android.zip`(약 24.8MiB: APK 13.1MiB, 영상 2개 5.4MiB, `.git` 3.3MiB) 생성. 깨끗한 작업 트리·키 있을 때만 생성 | 제출 직전 마지막 커밋으로 재생성. 메일 첨부 한도 초과로 Drive 링크 전달, 공유 권한은 제출자 확인 |
| R-073 | 구현 완료 | 통과 | 압축 푼 폴더에서 `git log` 최신 커밋, `git status` 변경 없음, `git fsck` 오류 없음 | 없음 |
| R-074 | 구현 완료 | 통과 | 키는 `local.properties`에서 읽고 `.gitignore`로 제외, ZIP 루트에 포함. 키 값은 `local.properties`·APK에만 있고 `.git` 객체 0건. ZIP의 파일에는 `sdk.dir` 없음(`ANDROID_HOME`만으로 빌드 확인) | 없음 |
| R-075 | 해당 없음 | 미검증 | 없음 | 사용자 확인 |
| R-076 | 구현 완료 | 통과 | `unzip -l`과 압축 푼 폴더에서 `build/`·`.gradle/`·`.kotlin/`·`.idea/` 0건 | 없음 |
| R-077 | 구현 완료 | 통과 | ZIP 루트 `app-debug.apk`를 실기기에 설치, 검색 200 응답·결과 표시 | 없음 |
| R-078 | 진행 | 미검증 | README 갱신 | 최종 제출 전 사용자 검토 |
| R-079 | 구현 완료 | 통과 | `demo.mp4` 1분 42초(`ffprobe` 101.7초), 720x1560, 2026-10-08 SM-S936N, 커밋 `71cdf67`: 검색·페이지네이션·정렬, 즐겨찾기 추가·해제·정렬·검색·가격 필터, 오프라인 오류·재시도, 회전, 다크 모드. `demo-tablet.mp4` 31초, 1280x746, SM-X710, 커밋 `bad3cd5`: 2-pane | 저장소 제외(`.gitignore`), 제출 ZIP 루트에 포함 |
| R-080 | 구현 완료 | 통과 | 2026-10-08 회사명 변형(국문·영문·띄어쓰기·법인명·옛 법인명·도메인)으로 추적 파일·파일명·전체 이력·커밋 메시지·브랜치·태그·`local.properties` 검색 0건. 저장소명 `book-search-android`, 설명·홈페이지·토픽 없음. 검색어는 문서에 적지 않음 | 스크린샷·영상은 텍스트 검색 대상 외 |
| R-081 | 해당 없음 | 통과 | 2026-10-08 키 값으로 `git log -p --all`·`git grep`·문서 검색 0건(값 미출력). OkHttp 로그는 Authorization을 가림 | 제출 직전 재확인 |
| R-082 | 해당 없음 | 통과 | 2026-10-08 제출 ZIP의 `.git`: remote는 자격증명 없는 HTTPS, credential·token·extraheader 없음, hooks는 기본 sample, 토큰 형식·URL 내 자격증명 패턴 0건 | 작성자 이메일은 Git 메타데이터로 남음(자격증명 아님) |
| R-083 | 해당 없음 | 미검증 | 없음 | 사용자 확인 |
| R-084 | 구현 완료 | 통과 | 새 디렉터리에 압축 해제 후 Launcher JVM 17로 `./gradlew --no-build-cache --no-configuration-cache assembleDebug` 성공(작업 271개 실행). ZIP의 APK를 SM-S936N에 설치·검색 | 없음 |
| R-085 | 구현 완료 | 통과 | `BookSearchBackStackTest` 7건(2-pane 상세 교체 포함), `BookSearchNavHostTest`(두 손가락 동시 탭 포함), TC-18·85·92 | 없음 |
| R-086 | 구현 완료 | 통과 | TC-86(키 없는 worktree 빌드·설치, 키 미설정 안내, 요청 0건, 즐겨찾기 정상), `KakaoBookServiceTest` 키 누락 | 없음 |
| R-087 | 구현 완료 | 통과 | TC-09, `BookDetailPresenterTest` | 없음 |

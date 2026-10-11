# 요구사항 체크리스트

이 브랜치는 수동 DI 구성으로 실제 JDK 17에서 APK 빌드와 자동 테스트 149개를 통과했습니다. Robolectric SDK 35 결과이며, 이 브랜치의 API 36·37 실기기·Studio ZIP 가져오기·새 PC·동적 재구성 측정은 미실행입니다. main의 Metro·JDK 21·SDK 36 및 Galaxy·Studio·ZIP 기록은 참고로 구분합니다.

- **원문 필수**: 제공된 과제에 명시된 조건
- **사용자 추가 필수**: 사용자가 원 과제 조건에 추가한 완료 기준
- **선택 결정**: 사용자가 선택한 기술 방향
- **권장**: 필수 조건을 안정적으로 달성하기 위한 제안
- **제품 선택**: 원문이 정하지 않은 사용자 경험의 세부 선택
- **구현 계약·품질 기준**: 명시 요구를 충족하기 위해 구체화한 동작·검증 기준. 별도 사용자 재승인 항목이 아님
- **미정**: 제품 선택 또는 아직 검증하지 않은 기술 세부사항

## 체크와 증거 기록 규칙

- 체크는 기록된 검증 근거로 해당 요구사항을 충족한다는 뜻이며, 최신 APK에서 모든 기기 시나리오를 다시 실행했다는 뜻은 아닙니다. 코드·테스트 파일의 존재만으로 체크하지 않습니다.
- 기존에 실제 실행한 기기 QA는 관련 동작이 유지되는지 소스로 대조해 활용합니다. 동작이 바뀐 부분은 현재 자동 테스트·기기 결과가 확인한 범위만 인정하고, 미재실행 항목과 검증 한계는 따로 기록합니다.
- 구현 상태: 미착수 / 진행 / 구현 완료 / 해당 없음. 확인 상태: 소스 확인 / 자동 테스트 통과 / 기기 확인 / 통과 / 부분 확인 / 최종 재검증 대기 / 미검증 / 실패 / 조건부 / 해당 없음.
- 9절에 코드·테스트·QA 시나리오와 남은 조건을 적습니다. 최종 실행 환경·명령·결과는 [QA](QA.md)와 함께 확인하며, 관련 코드가 바뀌면 영향받는 항목을 재검증합니다.
- 원문 필수는 임의로 해당 없음 처리하지 않습니다. R-ID는 재배치하지 않고 새 항목은 새 번호로 추가합니다.

각 항목을 코드로 옮길 때의 세부 계약(ID, 화면 문구, 백스택, 오류 분류 등)은 [구현 사양](SPEC.md)을 따릅니다.

## 1. 환경과 빌드: 원문 필수와 추가 검증

- [x] **R-001** Kotlin, Kotlin Coroutines, Flow, Jetpack Compose 사용
- [x] **R-002** Gradle 9.4.1
- [x] **R-003** Android Gradle Plugin 9.2.1
- [x] **R-004** Kotlin 2.4.0
- [x] **R-005** KSP를 사용하는 경우 2.3.10
- [x] **R-006** JDK 17
- [x] **R-007** compileSdk 37
- [ ] **R-008** **추가 검증**: 최신 안정 Android Studio에서 열기 및 빌드 확인
- [x] **R-009** Gradle Wrapper 파일과 실행 권한 포함
- [x] **R-010** 별도 설명 없이도 재현 가능한 `./gradlew assembleDebug` 성공
- [x] **R-011** **추가 검증**: 요청 버전 조합의 호환성 확인. 충돌 시 임의 변경하지 않고 실패 로그·공식 근거·대안 기록

minSdk 26, targetSdk 37과 라이브러리 버전은 [구현 사양](SPEC.md) 2절 기준입니다.

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
- [ ] **R-025** 즐겨찾기 목록에서 상세로 이동

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
- [x] **R-033** 누락된 표지·저자·날짜·가격 등으로 화면이 깨지지 않음
- [x] **R-087** 상세에서 즐겨찾기를 해제해도 표시 중인 도서 정보가 사라지지 않음

오프라인에서 텍스트 정보는 DB로 보장하고, 이미지 캐시가 없으면 대체 이미지를 보여 줍니다. 원본 표지까지 보장하려면 이미지 파일 저장을 추가 설계해야 합니다.

## 5. 품질과 확장성

### 원문에 명시된 요구

- [x] **R-034** 불안정한 네트워크에서도 앱 사용 가능
- [x] **R-035** 태블릿·폴더블 대응
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
- [x] **R-046** **권장**: MockWebServer API 계층 테스트. 의존성 버전은 [구현 사양](SPEC.md) 2절 기준
- [x] **R-047** **사용자 선택**: Soil typed-key + InfiniteQuery로 데이터 접근·페이지네이션 구성, 단순 위임 Repository 생략
- [x] **R-048** **사용자 선택**: 이 비교 브랜치에서는 수동 DI, Coil 이미지 로딩. main의 Metro 선택에 대한 명시적 비교 예외
- [x] **R-049** **설계 검증**: 수동 객체 조립·JDK 17 빌드, API와 이미지 네트워크 인증 설정 분리. 구체적인 구조는 [아키텍처](ARCHITECTURE.md) 참고
- [x] **R-050** **설계 검증**: query/sort별 key, size=20, 페이지 메타데이터·is_end 유지, 로딩·취소·재시도 동작을 테스트로 확인. 커스텀 페이지네이션과 Paging 3는 미채택 대안
- [x] **R-051** **로컬 연결 검증**: 선택한 Soil Subscription→Room Flow / Mutation→Room 쓰기에서 Room 단일 상태 기준, 반영 순서·지연, 구독 수명·재구독을 테스트
- [x] **R-052** **오류 정책 검증**: Retrofit 예외의 재시도 가능 여부를 명시적으로 분류하고 CancellationException을 보존
- [x] **R-053** **결정**: `app`, `core:model`, `core:network`, `core:database`, `core:data`, `core:ui`, `feature:search`, `feature:favorites`, `feature:detail`의 9개 모듈로 경계를 나누고, 그 이상의 모듈 증설은 피하기
- [x] **R-054** **현재 제품 정책**: 시스템 프로세스 재생성 시 query/sort/filter/탭과 상세 도서 스냅샷을 복원하고 검색은 재조회. 결과 목록 전체를 saveable에 넣지 않음

### 제품 선택 5개의 현재 정책

도서 앱의 UI 사례를 참고한 제품 정책입니다. 직접 확인한 외부 UI 사례와 이 앱의 기술 정책을 구분합니다. 근거와 한계는 [ARCHITECTURE](ARCHITECTURE.md) 9절에 있습니다.

1. 검색 실행 시점: 검색 버튼 또는 키보드 IME 제출로 확정합니다. 로컬 검색에도 같은 방식을 적용합니다.
2. 가격 필터: 정가 최소·최대 금액, 양끝 포함, 적용 버튼. 빈칸은 제한 없음, 최소 > 최대는 적용 금지. 0 이하·누락 정가는 정보 없음으로 표시하고 범위 적용 시 제외하며, 미적용 시 도서를 유지합니다. API가 0의 의미를 보장한다고 보지 않는 방어적 앱 정책입니다.
3. 저장 중 버튼/pop: 해당 화면에서 쓰기 중인 도서의 버튼만 비활성화하고 이동은 허용합니다. UI에서 접수한 명령은 공통 Mutation actor로 순서대로 처리하며, 화면이 닫히면 신규 접수를 막고 기존 요청 완료 뒤 참조를 정리합니다. 프로세스 종료 시 미완료 쓰기는 보장하지 않습니다.
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
- [x] **R-065** 가격 경계값, 0 이하/가격 없음, 잘못된 범위, 필터 결과 0개
- [x] **R-066** 오프라인 즐겨찾기·상세 및 앱 재시작 후 복원
- [x] **R-067** Room 쓰기 실패, 즐겨찾기 연타, 쓰기 중 뒤로 가기
- [x] **R-068** 화면 회전, 창 크기 변경, 폴더블 펼침·접힘
- [x] **R-069** 상세 진입·복귀 시 상태 유지, 백스택 제거 시 작업 정리
- [x] **R-070** 시스템 프로세스 재생성 후 최소 상태 복원, 저장·미저장 도서 모두 상세를 네트워크 없이 다시 표시
- [x] **R-071** 저장 중 화면 pop 이후 쓰기 완료·도서별 순서 확인. 프로세스 종료 시 미완료 쓰기 보장과 구분

## 8. 제출: 원문 필수와 추가 검증

마감: **2026-10-10(토) 14:30 KST**. 아래는 제출 준비 체크입니다. 제출 자체는 사용자가 합니다.

- [ ] **R-072** 소스 코드 ZIP
- [ ] **R-073** ZIP 안에 `.git` 포함
- [ ] **R-074** API 키를 `local.properties`에서 읽도록 구성하고, 제출 ZIP에도 해당 파일 포함
- [ ] **R-075** 제출에 사용한 API 키를 전형 종료 전까지 폐기하지 않음
- [ ] **R-076** ZIP에서 모든 `build/`, `.gradle/` 제외
- [ ] **R-077** APK 포함. Debug APK 허용
- [x] **R-078** README 포함: 빌드 방법, 사용 프레임워크, 구조, 주요 구현 포인트(원문 요구), 선택 이유·실제 검증 결과·한계, AI 활용 시 활용 작업·검증 방법·직접 판단해 바꾼 부분
- [ ] **R-079** **선택**: 3분 이내 시연 영상
- [ ] **R-080** GitHub 저장소명·설명·문서 등 저장소 콘텐츠에 회사명 미포함

### 추가 보안·재현성 검증

- [ ] **R-081** 실제 API 키가 Git 현재 파일·전체 이력·문서·로그에 없는지 확인
- [ ] **R-082** ZIP에 포함될 `.git/config` 및 remote URL에 자격증명이 없는지 사용자 확인
- [ ] **R-083** 최종 키 설정·민감정보 포함 여부·제출 대상은 사용자가 직접 확인
- [ ] **R-084** 압축을 새 디렉터리에 풀고 빌드 및 APK 설치 확인

local.properties는 Git에서 제외하고 제출 ZIP에만 넣습니다. `git archive`로는 `.git`과 무시된 local.properties가 들어가지 않으므로 제출 형식을 따로 검증합니다.

## 9. 항목별 구현·검증 상태

아래 자동 테스트 판정은 이 브랜치의 149개 새 실행 결과입니다. TC-NN은 재현 절차이며, main의 기기 화면·영상·패키지 결과는 해당 행에 참고로 표시합니다. 이 브랜치의 기기·패키지 통과 근거로 사용하지 않습니다.

- **소스 확인:** 설정·구조·문서 또는 구현을 읽어 확인한 범위입니다. 실제 기기 동작까지 통과했다는 뜻은 아닙니다.
- **자동 테스트 통과:** daemon Oracle JDK 17.0.10, 테스트 작업 9개 모두 JVM 17. Robolectric SDK 35에서 main의 145개와 DI 4개를 새 실행해 실패·오류·건너뜀 0. 명령·로그·XML 30개 집계는 [QA](QA.md)에 기록.
- **main 참고:** main APK의 기기·Studio·ZIP 결과입니다. 이 브랜치의 실제 기기 동작은 별도로 검증해야 합니다.
- **최종 재검증 대기·미검증:** 이 브랜치에서 실행하지 않은 기기·패키지 검증입니다. 자동 저장상태 복원 테스트는 실제 프로세스 종료를 재현하지 않습니다.

| 항목 | 구현 상태 | 확인 상태 | 소스·테스트·시나리오 | 남은 조건·제한 |
| --- | --- | --- | --- | --- |
| R-001 | 구현 완료 | 소스 확인 | Kotlin·Coroutines·Flow(Room·Soil), Compose 사용 | 없음 |
| R-002 | 구현 완료 | 소스 확인 | Wrapper `gradle-9.4.1-bin.zip` | 최종 실행 결과는 R-010 |
| R-003 | 구현 완료 | 소스 확인 | `libs.versions.toml` AGP 9.2.1 | 최종 실행 결과는 R-010 |
| R-004 | 구현 완료 | 소스 확인 | `libs.versions.toml` Kotlin 2.4.0 | 최종 실행 결과는 R-010 |
| R-005 | 구현 완료 | 소스 확인 | KSP 2.3.10, Room 컴파일러 적용 | 최종 실행 결과는 R-010 |
| R-006 | 구현 완료 | 통과 | 외부 JVM 검증 init script로 daemon Oracle JDK 17.0.10과 테스트 작업 9개의 JVM 17 확인 | Robolectric SDK 35. API 36·37 실기기 검증과 구분 |
| R-007 | 구현 완료 | 소스 확인 | `compileSdk = "37"` | 최종 실행 결과는 R-010 |
| R-008 | 구현 완료 | 최종 재검증 대기 | main 참고: Android Studio Rabbit 1에서 ZIP SDK 경로 자동 생성·Sync·IDE 빌드 성공 | 이 브랜치 ZIP의 Studio 검증·새 PC 환경은 미실행 |
| R-009 | 구현 완료 | 소스 확인 | 실행 권한을 가진 `gradlew`, 추적된 `gradle-wrapper.jar`와 Wrapper 설정 | 최종 압축 해제 환경의 실행은 R-084 |
| R-010 | 구현 완료 | 통과 | 이 브랜치에서 정확히 `./gradlew assembleDebug` 성공. 별도 전체 빌드·149개 테스트도 성공 | 설치된 SDK 37·JDK 17과 의존성 캐시 사용. 새 PC 및 ZIP 재현은 미실행 |
| R-011 | 구현 완료 | 통과 | 지정 Gradle·AGP·Kotlin·KSP·compileSdk 유지. 수동 DI·JDK 17 빌드·149개 테스트 통과 | Robolectric은 SDK 35. 환경별 범위는 QA |
| R-012 | 구현 완료 | 자동 테스트 통과 | `BookSearchNavHostTest`: 탭·내비게이션 호스트 검증. main 참고: Galaxy 첫 실행 화면 | 이 브랜치 최초 실행 실기기 확인은 미실행 |
| R-013 | 구현 완료 | 자동 테스트 통과 | `KakaoBookServiceTest`: MockWebServer 요청 파라미터·응답 계약. main 참고: Galaxy kotlin 검색 | 이 브랜치 실제 Kakao 서버·기기 확인은 미실행 |
| R-014 | 구현 완료 | 자동 테스트 통과 | TC-03(`sort=latest`, 출간일 내림차순), `SearchPresenterTest` 정렬 변경 | 정렬 요청·상태 전이는 자동 테스트. 실제 최신순 전환은 미실행 |
| R-015 | 구현 완료 | 자동 테스트 통과 | TC-04(page 2·3·4 순차 요청), `SearchPresenterTest`, `SearchPagingTest` | size=20·종료·추가 요청 경계는 자동 테스트. 실기기 페이지네이션은 미실행 |
| R-016 | 구현 완료 | 소스 확인 | 공통 BookCard 구성. main 참고: Galaxy 카드 필드·표지 확인 | 현재 APK 카드의 실기기 확인 미실행. 정가 자동 테스트는 R-033 |
| R-017 | 구현 완료 | 자동 테스트 통과 | TC-07·27, `SearchPresenterTest`, `ManualAppGraphTest` | 기기 추가·해제 결과는 main 참고 |
| R-018 | 구현 완료 | 자동 테스트 통과 | `BookSearchNavHostTest`: 상세 내비게이션·retain. main 참고: Galaxy 검색 카드에서 상세 진입 | 실제 카드 터치 경로의 이 브랜치 기기 확인은 미실행 |
| R-019 | 구현 완료 | 자동 테스트 통과 | `SearchPresenterTest` 늦은 응답 경합(검색어·정렬·빈 검색어), TC-03·19 | 늦은 응답 경합은 Fake API를 사용한 자동 테스트 범위 |
| R-020 | 구현 완료 | 자동 테스트 통과 | TC-05(결과 없음), TC-20(최초 오류), TC-04(추가 로딩), TC-40(추가 오류), `SearchPresenterTest` | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-021 | 구현 완료 | 자동 테스트 통과 | TC-07·10, `FavoriteBookDaoTest` | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-022 | 구현 완료 | 자동 테스트 통과 | `FavoritesPresenterTest`, `FavoriteListRulesTest`: 제목·저자·일치 없음 경계 | main의 Galaxy 제목 검색은 참고 |
| R-023 | 구현 완료 | 자동 테스트 통과 | `FavoritesPresenterTest`, `FavoriteListRulesTest`: 제목 정렬 | main의 Galaxy 오름차순·내림차순 확인은 참고 |
| R-024 | 구현 완료 | 자동 테스트 통과 | `PriceFilterTest`, `FavoritesPresenterTest`, `PriceInputTest`: 금액 경계·잘못된 범위·빈 범위 | main의 Galaxy 최대 금액·동등 경계·정가0 확인은 참고 |
| R-025 | 구현 완료 | 미검증 | main 참고: Galaxy 즐겨찾기 목록에서 정가 정보 없음 도서의 상세 진입 | 이 브랜치 실제 목록 카드 터치 경로 미실행 |
| R-026 | 구현 완료 | 최종 재검증 대기 | TC-26(`am force-stop` 후 재실행), `FavoriteBookDaoTest` 파일 DB 재열기 | 최종 검증 필요 |
| R-027 | 구현 완료 | 자동 테스트 통과 | `FavoriteSubscriptionKeysTest`, `ManualAppGraphTest`: 세 화면의 같은 저장 데이터 읽기 | 실제 화면 하트 동기화의 기기 결과는 main 참고 |
| R-028 | 구현 완료 | 자동 테스트 통과 | `FavoriteMutationKeyTest`(실패 전파), `FavoriteTogglerTest`, 세 Presenter 테스트의 쓰기 실패 | 쓰기 실패는 자동 테스트의 Fake 주입이며 실제 기기 Room 장애는 미실행 |
| R-029 | 구현 완료 | 부분 확인 | `BookDetailPresenterTest`, `BookDetailPriceTest`: 상세 상태·가격. main 참고: Galaxy 상세 필드 표시 | 이 브랜치 전체 필드·표지 기기 확인 미실행 |
| R-030 | 구현 완료 | 자동 테스트 통과 | `BookDetailPresenterTest`, `ManualAppGraphTest`; TC-08·09·71 | main의 상세 저장 기기 결과는 참고 |
| R-031 | 구현 완료 | 자동 테스트 통과 | `ManualAppGraphTest`, `FavoriteSubscriptionKeysTest`: 상세 수정·목록 삭제 후 세 화면 데이터 일치 | 이 브랜치 실제 화면 반영의 기기 확인 미실행 |
| R-032 | 구현 완료 | 최종 재검증 대기 | TC-32(비행기 모드) | 최종 검증 필요 |
| R-033 | 구현 완료 | 부분 확인 | 모델·mapper·Room·카드·상세 정가 테스트 통과. main 참고: Galaxy 정가0 표시·필터 | 다른 누락 필드와 현재 APK의 기기 검증 미실행 |
| R-034 | 구현 완료 | 최종 재검증 대기 | TC-20·40·40b·42·32·70a | 조건부 재연결 재조회·버튼 재시도 제공. 현재 APK의 네트워크 오류·복구 기기 검증은 미실행([SPEC](SPEC.md) 9절) |
| R-035 | 구현 완료 | 최종 재검증 대기 | 태블릿 SM-X236N TC-35(세로 레일)·TC-90~94(가로 2-pane), Galaxy Z Fold2 TC-68d~68h(접힘·펼침·중간 접힘), `BookSearchNavHostTest` 좁은/넓은 창·2-pane | Fold2 안쪽 화면은 compact 너비라 폴더블 2-pane은 없음. 테이블톱 전용 배치 없음 |
| R-036 | 구현 완료 | 소스 확인 | 앱 코드 9개 모듈과 테스트 전용 `core:testing`, [ARCHITECTURE](ARCHITECTURE.md) | 없음 |
| R-037 | 구현 완료 | 소스 확인 | [ARCHITECTURE](ARCHITECTURE.md) 8절, [SPEC](SPEC.md) 2절 모듈 책임 | 없음 |
| R-038 | 구현 완료 | 소스 확인 | [ARCHITECTURE](ARCHITECTURE.md) 9절(선택 이유와 비용) | 없음 |
| R-039 | 구현 완료 | 소스 확인 | [ARCHITECTURE](ARCHITECTURE.md) 9절 미채택 대안(ViewModel, Repository, Paging 3) | 없음 |
| R-040 | 구현 완료 | 자동 테스트 통과 | `SearchPresenterTest`, 실제 key factory·Soil·Presenter를 연결한 `SearchRetryTest`; TC-40·40b·42 | 재시도 통합 경로는 Fake API 기반. 기기 네트워크 오류 재현은 별도 |
| R-041 | 구현 완료 | 통과 | 기존 TC-41·68a·68e·68f·94의 회전·크기 변경·입력 유지. 현재 `BookSearchNavHostTest` 구성 변경·2-pane 전환, `FavoritesScreenTest`·복원 모사 테스트 통과 | 최신 APK의 동일 기기 시나리오는 미재실행. 검색 프로세스 재생성 뒤 이전 페이지 전체·동일 도서 위치는 보장하지 않음 |
| R-042 | 구현 완료 | 소스 확인 | `RetainedValuesStoreNavEntryDecorator`, 화면별 retained graph | 자동 수명 검증은 R-069 |
| R-043 | 구현 완료 | 소스 확인 | Room DB·Entity·DAO 구성 | 영속성 실행 검증은 R-057 |
| R-044 | 구현 완료 | 소스 확인 | Room DAO Flow→Soil Subscription 경로 | 구독·반영 실행 검증은 R-051 |
| R-045 | 구현 완료 | 소스 확인 | Retrofit·OkHttp·kotlinx.serialization 서비스 구성 | API 실행 검증은 R-046 |
| R-046 | 구현 완료 | 자동 테스트 통과 | `KakaoBookServiceTest`(MockWebServer) | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-047 | 구현 완료 | 소스 확인 | Soil typed-key·InfiniteQuery 구성. 단순 위임 Repository 없음 | 페이지네이션 실행 검증은 R-050 |
| R-048 | 구현 완료 | 자동 테스트 통과 | AppGraph·화면 Factory 계약을 유지한 수동 DI, Coil 3.6.3. `ManualAppGraphTest` 4개 | SWR·ImageLoader 및 화면 context 공유 범위를 검증. 실제 이미지 로드 기기 검증은 별도 |
| R-049 | 구현 완료 | 통과 | JDK 17 APK 빌드, `ManualAppGraphTest`·`KakaoBookServiceTest` 통과. 이미지 기본 클라이언트와 API 인증 클라이언트 분리는 소스 확인 | 기기 이미지 요청의 네트워크 추적은 미실행 |
| R-050 | 구현 완료 | 자동 테스트 통과 | `SearchBooksQueryKeyTest`, `SearchPagingTest`, `SearchPresenterTest`, `SearchRetryTest` | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-051 | 구현 완료 | 자동 테스트 통과 | `FavoriteSubscriptionKeysTest`: in-memory 실제 Room 구독. `ManualAppGraphTest`: 실제 그래프·Room 연결. Writer·Presenter·수명 테스트 | 저장 경합·지연·실패 격리는 `FavoriteMutationLifecycleTest`의 Fake DAO 기반. 실제 기기 장애 재현과 구분 |
| R-052 | 구현 완료 | 자동 테스트 통과 | `ErrorKindTest`, `SearchBooksQueryKeyTest`(실패 페이지·원인 보존, 취소 예외 동일 인스턴스), `FavoriteTogglerTest`(취소 전파) | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-053 | 구현 완료 | 소스 확인 | `settings.gradle.kts`의 9개 모듈 + 테스트 전용 `core:testing` | 없음 |
| R-054 | 구현 완료 | 최종 재검증 대기 | 내비게이션 키의 상세 스냅샷, saveable 최소 상태; TC-54·70b | 실제 메모리 압박으로 인한 종료는 재현하지 않음. 최종 기기 확인 필요 |
| R-055 | 구현 완료 | 자동 테스트 통과 | `BookIdentityTest`, `PriceFilterTest`, `SearchPresenterTest` | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-056 | 구현 완료 | 자동 테스트 통과 | `SearchPagingTest`, `SearchPresenterTest`, `SearchRetryTest`(재조회 실패와 추가 로딩 실패를 구분) | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-057 | 구현 완료 | 자동 테스트 통과 | `FavoriteBookDaoTest`: 저장·삭제·Flow·파일 DB 재열기·원본 정가 보존. 쓰기 실패는 `FavoriteMutationKeyTest`·Fake 주입 | Fake 쓰기 실패와 기기 Room 실패를 구분 |
| R-058 | 구현 완료 | 자동 테스트 통과 | `BookSearchNavHostTest`, `FavoritesScreenTest`, `SearchQueryRetentionTest` | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-059 | 구현 완료 | 최종 재검증 대기 | [QA](QA.md)의 자동 검증 범위와 실기기 시나리오; TC-41·68a~68h·70b | 최종 기기 재검증 대기. 실제 메모리 압박 종료는 재현하지 않음 |
| R-060 | 구현 완료 | 자동 테스트 통과 | [QA](QA.md)의 실행 환경·명령·결과와 범위 제한 | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-061 | 구현 완료 | 자동 테스트 통과 | `SearchPresenterTest`, TC-06·19 | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-062 | 구현 완료 | 자동 테스트 통과 | `SearchPresenterTest`(20개 + is_end), `SearchPagingTest` | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-063 | 구현 완료 | 자동 테스트 통과 | `SearchPresenterTest`, TC-40(오류 뒤 스크롤 요청 0건)·40b | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-064 | 구현 완료 | 자동 테스트 통과 | `SearchPagingTest`, `SearchPresenterTest` 중복 ID | 기기 결과는 main 참고이며 이 브랜치는 미실행 |
| R-065 | 구현 완료 | 자동 테스트 통과 | BookPricePolicyTest·PriceFilterTest·FavoriteBookDaoTest·PriceLineTest·BookDetailPriceTest·PriceInputTest·FavoritesPresenterTest | main의 Galaxy 최대 금액·동등 경계·정가0 확인은 참고 |
| R-066 | 구현 완료 | 최종 재검증 대기 | TC-26·32·70a·70b | 최종 검증 필요 |
| R-067 | 구현 완료 | 자동 테스트 통과 | `FavoriteTogglerTest`, `FavoriteMutationLifecycleTest`: 저장 실패·연타·화면 종료 중 접수 명령 처리; TC-67·71 | Room 지연·실패는 Fake DAO 기반으로 주입. 실제 기기 장애 재현과 구분 |
| R-068 | 구현 완료 | 최종 재검증 대기 | 회전 TC-41·68a·94(태블릿 2-pane↔1-pane), 폴더블 접힘·펼침·중간 접힘 TC-68d~68g(`cmd device_state`), 손으로 접고 펴기 TC-68h, 다크 모드 TC-68b | 기존 main 참고 절차. 이 브랜치 기기 검증은 미실행 |
| R-069 | 구현 완료 | 자동 테스트 통과 | `BookSearchNavHostTest`, `SearchQueryRetentionTest`; TC-40b·85 | retain 정리·구성 변경과 검색 상태 수명 자동 검증 |
| R-070 | 구현 완료 | 최종 재검증 대기 | TC-70a·70b(미저장 도서), TC-32(저장 도서) | 최종 검증 필요 |
| R-071 | 구현 완료 | 자동 테스트 통과 | `FavoriteMutationLifecycleTest`: 실제 retain·Soil·Mutation key + Fake DAO로 명령 완료·순서·실패 격리·참조 해제 검증 | 프로세스 종료 시 미완료 쓰기는 보장하지 않음 |
| R-072 | 구현 완료 | 미검증 | main 참고: 제출 커밋의 소스·문서를 포함한 ZIP 생성. CRC·압축 해제 해시 대조 통과 | 이 브랜치 최종 ZIP·영상·설치 검증은 미실행 |
| R-073 | 구현 완료 | 미검증 | main 참고: ZIP의 .git 포함 및 원본과 압축 해제 파일 해시 일치 | 이 브랜치 최종 ZIP·영상·설치 검증은 미실행 |
| R-074 | 구현 완료 | 미검증 | main 참고: Git에서 제외한 local.properties를 ZIP에 포함. API 키 포함·sdk.dir 제외 확인, 동일 APK의 검색 동작 확인 | 이 브랜치 최종 ZIP·영상·설치 검증은 미실행 |
| R-075 | 진행 | 미검증 | 없음 | 사용자 확인 |
| R-076 | 구현 완료 | 미검증 | main 참고: ZIP에 build/·.gradle/ 등 제외 대상 디렉터리가 없음을 확인 | 이 브랜치 최종 ZIP·영상·설치 검증은 미실행 |
| R-077 | 구현 완료 | 미검증 | main 참고: ZIP의 app-debug.apk와 Galaxy 설치·실행 APK의 해시 일치 | 이 브랜치 최종 ZIP·영상·설치 검증은 미실행 |
| R-078 | 구현 완료 | 소스 확인 | README에 빌드 전제·명령, 사용 기술 및 라이브러리, 구조·구현 포인트·선택 비용·검증 범위·한계, AI 활용 3항목 확인 | 없음 |
| R-079 | 구현 완료 | 미검증 | main 참고: 휴대폰 47.5초·태블릿 30.675초, 합계 78.175초. ZIP 포함 및 영상 해시 확인 | 이 브랜치 최종 ZIP·영상·설치 검증은 미실행 |
| R-080 | 구현 완료 | 최종 재검증 대기 | 저장소명·설명·추적 파일·이력의 텍스트 검사 대상 정의 | 최종 문서·이력 재확인 필요. 스크린샷·영상은 별도 확인 |
| R-081 | 해당 없음 | 최종 재검증 대기 | Git·문서·로그 민감정보 확인 절차. Authorization 로그 마스킹 | 최종 제출 직전 재확인. 실제 키 값은 기록하지 않음 |
| R-082 | 구현 완료 | 미검증 | main 참고: 원격 URL의 내장 자격증명·쿼리·fragment 없음 확인 | 이 브랜치의 최종 패키지 Git 설정 확인은 별도 |
| R-083 | 해당 없음 | 미검증 | 없음 | 사용자 확인 |
| R-084 | 구현 완료 | 미검증 | main 참고: 새 디렉터리에 ZIP을 풀어 SDK 경로 자동 생성·Studio Sync·APK 빌드 성공. 포함 APK는 Galaxy 검증 APK와 해시 동일 | 이 브랜치 최종 ZIP·영상·설치 검증은 미실행 |
| R-085 | 구현 완료 | 자동 테스트 통과 | `BookSearchBackStackTest`, `BookSearchNavHostTest`의 동시 터치·2-pane 상세 교체; TC-18·85·92 | 동시 터치·백스택·2-pane 교체 자동 검증. 실제 기기 범위는 QA에 기록 |
| R-086 | 구현 완료 | 통과 | 기존 TC-86의 키 없는 빌드·설치·설정 안내·즐겨찾기 확인. 현재 빈 키 BuildConfig 경로 유지, `KakaoBookServiceTest` 요청 차단과 `ErrorKindTest` 키 누락 분류·재시도 제외 통과 | 최신 APK의 키 없는 빌드·설치·전체 UI 시나리오는 미재실행 |
| R-087 | 구현 완료 | 자동 테스트 통과 | TC-09, `BookDetailPresenterTest` | BookDetailPresenterTest 통과. 상세 해제 후 화면 유지의 기기 확인은 미실행 |

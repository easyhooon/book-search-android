# book-search-android

Kakao 책 검색 API로 도서를 찾고, 관심 도서를 기기에 저장하는 Android 앱입니다.

- **검색**: 검색어 확정, 정확도순·발간일순 정렬, 20개 단위 페이지네이션
- **즐겨찾기**: 로컬 저장·검색, 제목 오름차순·내림차순, 정가 범위 필터
- **상세**: 두 목록에서 공통으로 사용하며, 도서 정보와 즐겨찾기 상태 표시·변경

## 주요 설계와 구현

- **검색 상태**: Soil InfiniteQuery가 검색어·정렬별 캐시와 로딩·오류 상태를 맡습니다. 앱은 `is_end`·페이지 상한에 따른 종료와 중복 ID 제거를 맡고, 네트워크 오류·HTTP 5xx·429를 자동 재시도하도록 설정합니다. 실패 페이지를 보존해 기존 페이지 재조회 실패는 다시 조회하고, 추가 로딩 실패는 해당 페이지만 재시도합니다.
- **즐겨찾기**: Room을 상태의 단일 기준으로 삼아 세 화면이 같은 저장 결과를 관찰합니다. 하나의 Mutation actor가 UI에서 접수한 명령을 순서대로 처리하고, `FavoriteWriter`가 앱 범위에서 Room에 씁니다. 화면을 닫으면 새 요청을 막고, 이미 접수한 요청의 완료 후 참조를 정리합니다. 진행·오류 표시는 화면별로 유지합니다.
- **화면 수명**: Navigation 3 엔트리에 retain 저장소를 연결해 화면 전환·구성 변경 중 객체를 유지하고 pop 시 정리합니다. 입력·정렬·필터와 상세 스냅샷은 별도로 저장해 시스템 프로세스 재생성에 대응합니다. 이 연결을 위한 decorator와 수명 테스트를 직접 관리합니다.
- **협업 경계**: 화면은 Root·Presenter·Screen으로 나누고, 원격·로컬 구현은 `core:data` 뒤에 둡니다. Metro가 객체 조립을 생성하지만 화면 그래프와 Soil key의 수명을 맞추는 책임은 앱에 남습니다.
- **화면·네트워크**: 창 크기·자세에 따라 하단 바·레일을 전환하고, 840dp 이상에서는 목록과 상세를 나란히 표시합니다. 저장한 도서의 텍스트는 오프라인에서도 열리며, 표지는 캐시가 없으면 대체 이미지를 표시합니다. API 인증 interceptor는 이미지 요청에 전달하지 않습니다.

선택 이유·비용과 데이터 흐름은 [아키텍처](docs/ARCHITECTURE.md), 세부 동작은 [구현 사양](docs/SPEC.md)에 있습니다.

## 검증 범위와 상태

- **자동 테스트**: 140개 통과(실패·오류·건너뜀 0), APK 빌드 성공. 상세 범위는 [QA](docs/QA.md) 참고.
- **Android Studio**: ZIP을 새 폴더에 풀어 수동 SDK·JDK 설정 없이 SDK 경로 자동 생성, Sync·APK 빌드 성공. 설치된 SDK 37·JDK 21과 캐시를 사용한 환경이며 새 PC의 JDK 자동 설치는 미검증.
- **실기기 QA**: Galaxy SM-S936N에 최종 APK 설치 후 검색·상세, 즐겨찾기 추가·해제·동기화, 제목 정렬·로컬 검색·가격 필터 확인. API 정가 0의 정보 없음 표시와 필터 제외 확인. 네트워크 오류·페이지네이션·태블릿·폴더블은 현재 APK로 재실행하지 않음.
- **검증 경계**: 검색은 실제 Soil·key factory·Presenter와 Fake API, 저장은 실제 retain·Soil·Writer와 Fake DAO로 경합·수명 확인. 절차·판정 기준·미실행 항목은 [QA](docs/QA.md)에 정리.

## 빌드 방법

Android SDK Platform 37이 설치되어 있어야 합니다. 명령줄 빌드는 `ANDROID_HOME` 또는 `local.properties`의 `sdk.dir`로 SDK를 찾습니다. 제출 ZIP에는 개인 기기의 `sdk.dir`를 넣지 않으므로, SDK 환경이 설정된 머신에서 실행해야 합니다.

| 항목 | 버전 |
| --- | --- |
| Gradle / AGP | 9.4.1(Wrapper 포함) / 9.2.1 |
| Kotlin / KSP | 2.4.0 / 2.3.10 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 |
| Gradle 실행 JDK(daemon) | 21 |
| 순수 JVM 모듈(`core:model`) toolchain | 17 |
| Android Java·Kotlin 컴파일 대상 | 17 |

**JDK 조건 차이:** Metro 1.4.5의 플러그인·컴파일러가 JVM 21을 요구해 원문의 JDK 17 실행 조건은 미충족입니다. `gradle/gradle-daemon-jvm.properties`가 daemon 21을 선택하며, 컴파일 대상 17과 빌드 실행 JDK는 별개입니다. 근거는 [구현 사양](docs/SPEC.md) 2절에 있습니다.

1. 프로젝트 루트의 `local.properties`에 Kakao REST API 키를 설정합니다. 제출 ZIP에는 이 파일이 포함되며 Git에서는 제외됩니다.

   ```properties
   KAKAO_REST_API_KEY=발급받은_REST_API_키
   ```

2. 빌드·설치합니다.

   ```sh
   ./gradlew assembleDebug
   ./gradlew :app:installDebug
   ```

3. 테스트합니다. 순수 JVM 모듈 `core:model`을 포함하려면 `test`도 지정합니다.

   ```sh
   ./gradlew testDebugUnitTest test --continue
   ```

키가 없어도 빌드할 수 있습니다. 검색은 키 설정 안내를 표시하고 요청을 보내지 않으며, 로컬 즐겨찾기는 사용할 수 있습니다.

### 제출 파일

ZIP에는 제출 커밋의 소스·문서, `.git`, 개인 SDK 경로를 제외한 `local.properties`, `app-debug.apk`, 시연 영상 2개를 포함하고 `build/`·`.gradle/` 등은 제외합니다. 구성·CRC·압축 해제 파일 해시와 Studio 빌드 확인 완료. 포함 APK는 Galaxy에서 검증한 APK와 동일합니다.

`app-debug.apk`는 `adb install app-debug.apk`로 설치할 수 있습니다. 제출 영상은 최종 APK의 휴대폰 시연 `demo.mp4`(47.5초)와 기존 빌드의 태블릿 2-pane 시연 `demo-tablet.mp4`(30.675초)입니다. 합계 78.175초이며, 태블릿 영상은 현재 코드의 검증 근거로 사용하지 않습니다.

## 사용 기술 및 라이브러리

| 영역 | 선택 |
| --- | --- |
| UI | Jetpack Compose(BOM 2026.09.00), Material 3, Material 3 Adaptive Navigation Suite |
| 비동기 처리 | Kotlin Coroutines, Flow |
| 화면 이동·상태 수명 | Navigation 3 1.2.0, Compose `retain` |
| 비동기 상태 관리 | Soil 1.0.0-alpha15 |
| HTTP 통신·직렬화 | Retrofit 3.0.0, OkHttp 5.5.0, kotlinx.serialization |
| 로컬 저장 | Room 2.8.5 |
| 의존성 주입 | Metro 1.4.5 |
| 이미지 / 로그 | Coil 3.6.3 / Timber 5.0.1(debug 빌드에서만 Tree 등록) |
| 테스트 | JUnit 4, Robolectric 4.17, Compose UI Test, Molecule, Turbine, MockWebServer |

Soil은 비동기 데이터의 조회·캐싱·갱신과 로딩·오류 상태를 관리합니다. 이 앱은 **InfiniteQuery → Retrofit 검색**, **Subscription → Room Flow**, **Mutation → Room 쓰기**를 연결하며, 독립적인 단건 Query는 사용하지 않습니다. HTTP 통신은 Retrofit·OkHttp가 담당합니다.

검색 캐시는 정규화한 검색어·정렬별로 나뉘며, 마지막 성공(추가 페이지 포함) 후 5분이 지나면 오래된 상태(stale)로 판단합니다. 시간 경과만으로 요청하지 않고 재구독 등 조건을 만족할 때 기존 페이지를 재조회합니다. 메모리 캐시의 수명·재시도·재연결 조건은 [아키텍처](docs/ARCHITECTURE.md#검색-캐시의-수명과-재조회) 참고.

## 구조

- `app`: 의존성 조립, 탭·백스택, retain decorator, 2-pane 장면
- `feature:search`·`favorites`·`detail`: 엔트리 등록과 데이터 연결(Root), 화면 상태·이벤트(Presenter), UI(Screen). feature끼리는 참조하지 않습니다.
- `core:model`·`network`·`database`·`data`·`ui`: 순수 규칙, 통신, 저장, 데이터 연결, 공통 UI
- `core:testing`·`build-logic`: 테스트 도구와 공통 빌드 설정

원격 검색 상태는 Soil, 로컬 즐겨찾기 데이터는 Room, 화면 전용 상태는 Presenter가 관리합니다. `core:data`의 key와 writer가 데이터 계층을 이루며, 호출을 전달만 하는 Repository·UseCase는 추가하지 않았습니다.

## 알려진 제한

- **저장 수명:** 화면에서 접수한 요청은 화면을 닫아도 완료하지만, 프로세스 종료 시 미완료 쓰기까지 보장하지는 않습니다.
- **오프라인 검색:** 검색 결과는 메모리 캐시만 사용하며, 프로세스 종료 후에는 다시 요청합니다. 재연결에 따른 자동 재조회에는 조건이 있으며, 오류 화면에서 직접 재시도할 수 있습니다.
- **데이터 규모·식별:** 즐겨찾기는 수백 건을 가정해 전체 목록을 메모리에서 검색·정렬합니다. 대량 데이터 성능은 측정하지 않았습니다. 같은 책의 ISBN10·ISBN13이 응답마다 한쪽만 오면 서로 다른 ID가 될 수 있습니다.
- **폴더블·이미지:** 목록·상세 배치는 창 너비를 기준으로 하며 힌지 자세별 테이블톱 배치는 없습니다. Fold2 안쪽 화면은 compact 너비라 2-pane이 나타나지 않습니다. 오프라인 표지 표시는 이미지 캐시 유무에 따릅니다.
- **가격 정책:** 0 이하·누락 정가는 "가격 정보 없음"으로 표시합니다. 가격 필터 적용 시 제외하고, 미적용 시 도서를 유지합니다. API 값의 의미를 단정하지 않는 방어적 앱 정책입니다.

## AI 활용

### 활용한 작업

| 도구 | 맡은 작업 |
| --- | --- |
| Codex | 요구사항·자료 조사, 설계 초안, 코드·문서 검토. 개발자 승인에 따른 앱 코드·회귀 테스트 구현 및 실행, 빌드·문서 정리 |
| Claude Code(Claude Opus 5.5) | 사양 구체화, 빌드 설정, 초기 앱 코드·테스트, adb 실기기 QA, 시연 영상·제출 스크립트 |

### 검증 방법

공식 문서·라이브러리 소스와 코드 대조, 빌드·자동 테스트·실기기 시나리오로 검증. 개발자가 요구사항·동작·유지보수 관점에서 채택·수정 여부를 판단하고, AI가 승인된 코드 수정·테스트 수행. 실행 범위와 미실행 항목은 [QA](docs/QA.md)에 기록.

### 직접 판단해 바꾼 부분

| AI 제안·참고 구현 | 개발자 판단 |
| --- | --- |
| ViewModel 중심 구조 | retain + Navigation 3 선택, 엔트리 수명·정리·복원 검증 추가 |
| 참고 코드의 Ktor | Android 전용 과제의 HTTP 통신·테스트를 Retrofit·OkHttp로 충족하므로 Ktor 추가 이점이 없다고 판단. 더 익숙한 Retrofit 선택, Soil key에 서비스 주입 |
| 원격·로컬 연결 구조 | 통신·저장 구현을 별도 모듈로 분리 |
| 화면·기기 검증 범위 | 동시 탭 대응과 태블릿·실제 폴더블 검증 포함 |
| 가격 정보 처리 | 0 이하 정가는 정보 없음, 금액 필터 적용 시 제외·미적용 시 도서 유지 |

Metro 유지로 daemon JVM 21을 선택한 판단과 그 제약을 포함한 상세 기록은 [AI 활용 기록](docs/AI_USAGE.md)에 있습니다.

## 문서

[요구사항과 검증 상태](docs/REQUIREMENTS.md) · [구현 사양](docs/SPEC.md) · [아키텍처와 선택 이유](docs/ARCHITECTURE.md) · [QA](docs/QA.md) · [AI 활용 기록](docs/AI_USAGE.md) · [작업 지침](AGENTS.md)

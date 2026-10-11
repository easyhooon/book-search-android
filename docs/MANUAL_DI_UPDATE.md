# 수동 DI 비교 브랜치 최신화 검증

기존 Draft PR #1의 `codex/manual-di-jdk17`에 main `6f80ef0c568626bcdb08d6a81d5229e214805774`를 merge 방식으로 반영했습니다. Writer 제거, Mutation·retain 수명 회귀 테스트, 문서 갱신, QA 중복 이미지와 미사용 다이어그램 PNG 정리를 포함합니다. main으로 이 비교안을 merge하지는 않습니다.

공개 사용자용 README 정리 커밋 `510d7a6`도 추가 merge로 반영했습니다. 이 후속 변경은 README와 검증 문서뿐이며, 아래 JVM 17 검증 이후 운영 코드·빌드 설정·테스트 소스는 변경되지 않았습니다.

## 유지한 비교 범위

- Metro plugin·annotation 대신 `ManualAppGraph`가 객체를 조립합니다. AppGraph·화면 Graph·Factory 계약, retain·Navigation 3·Soil 경계를 유지합니다.
- 앱 그래프가 Room DB와 DAO를 공유하고 세 화면의 Mutation factory에 DAO를 직접 전달합니다. FavoriteWriter·전용 application scope·Mutex는 없습니다.
- 모든 화면의 `MutationId("favorite")`와 Toggler 완료 대기·참조 유지 계약은 main과 동일합니다. actor 자체 취소 시 독립적으로 쓰기를 완료하거나 프로세스 종료 중 미완료 쓰기를 끝내는 보장은 없습니다.
- Gradle daemon 기준과 Java/Kotlin target은 17입니다. Robolectric은 SDK 35, compileSdk·targetSdk는 37입니다. SDK 35 JVM 검증은 API 36·37 실기기 검증과 다릅니다.

## 새 실행 결과

설치된 Oracle JDK 17.0.10과 기존 Android SDK·의존성 캐시를 사용했습니다. 저장소 밖의 init script가 Gradle 실행 JVM `17.0.10`과 테스트 작업 9개 모두 Java launcher `17`임을 출력했습니다.

```sh
./gradlew assembleDebug testDebugUnitTest test lintDebug --continue --offline --rerun-tasks \
  --init-script "$JVM_VERIFICATION_INIT"
./gradlew assembleDebug
```

첫 실행은 1분 56초, 507 tasks 모두 실행해 성공했습니다. 테스트 XML 30개를 집계해 **149 tests, 실패·오류·skip 0**을 확인했습니다. 별도 옵션 없는 `assembleDebug`도 성공했습니다. Lint 오류 0개, app의 `GradleDependency`·`DataExtractionRules` 경고 2개가 남습니다.

| 범위 | 새 실행 결과 |
| --- | --- |
| main과 동일한 기존 테스트 소스 | 145개 통과. 이 브랜치에서는 Robolectric SDK 35로 실행 |
| `FavoriteMutationKeyTest` | 5개 통과: 순서·실패·대기자 취소·actor 자체 취소 경계 |
| `FavoriteMutationLifecycleTest` | 8개 통과: retain·pop·대기 명령·서로 다른 작업 시간의 최종 상태 |
| `FavoriteSubscriptionKeysTest` | 5개 통과: 실제 Room·운영 Mutation key·구독 반영 |
| `ManualAppGraphTest` | 추가 4개 통과: 앱/화면 공유 범위·공통 ID·세 화면의 동일 Room 데이터 읽기/쓰기 |

main의 결함 주입 결과는 [Writer 제거 검증](WRITER_REMOVAL.md)에 있습니다. 이번 수동 DI 최신화에서는 동일한 회귀 테스트를 JVM 17로 실행했으며 결함 주입 자체는 다시 실행하지 않았습니다.

## 확인 범위와 한계

운영 코드의 Metro import·plugin이 없고 Toggler·retain decorator·검색 QueryHolder가 최신 main과 동일함을 확인했습니다. 미사용 PNG 참조와 깨진 로컬 Markdown 링크는 없습니다. API 키 설정 파일은 Git에서 제외합니다.

실기기 설치·API 36/37 QA, 새 ZIP·Android Studio 가져오기, 새 PC·캐시 없는 의존성 다운로드, 동적 재구성 측정은 실행하지 않았습니다. 제출 APK·ZIP·기기는 변경하지 않았습니다.

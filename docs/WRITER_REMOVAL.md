# 즐겨찾기 Writer 제거 검증

## 변경 범위

기준은 `main`의 `49e8a74ce72ad8feb2f916d90286585dff42f55b`이며, 변경 브랜치는 `refactor/remove-favorite-writer`입니다. 제출 커밋 `f9f6b2290199b354eb2cb79f998ef4147b063b17` 이후 기준 커밋까지의 변경은 QA 이미지 정리입니다. 기존 제출 APK·ZIP·기기 QA 결과와 이 브랜치의 검증 결과는 구분합니다.

`FavoriteWriter`와 `ApplicationCoroutineScope`를 제거했습니다. `DefaultFavoriteMutationKeyFactory`가 Room DAO를 직접 호출하며, 화면들이 공유하는 `MutationId("favorite")`가 쓰기 순서를 직렬화합니다. `FavoriteToggler`의 retain 수명, 명령 접수, 완료 후 참조 해제 동작은 유지합니다. 별도 scope나 Mutex를 다른 운영 클래스에 옮기지 않았습니다. 테스트 fake는 실제 역할에 맞게 `FakeFavoriteMutation`으로 이름을 변경했습니다.

## 보장과 경계

| 동작 | 실행한 근거 | 결과 |
| --- | --- | --- |
| 여러 화면의 저장→해제→저장 순서 | 실제 Soil actor + 운영 Mutation key + 지연 fake DAO | 순서와 최종 도서 데이터 확인 |
| 느린 저장 뒤 다른 화면의 빠른 해제 | 실제 retain·Soil + 운영 key + 지연 fake DAO | 마지막 해제 의도대로 최종 미저장 상태 확인 |
| 같은 프레임의 명령 접수와 pop, 대기 명령, 마지막 화면 제거 | 실제 retain·Soil 수명 테스트 | 대기 명령 완료·pending 해제·Mutation 참조 해제 확인 |
| 컴포지션 이탈과 복귀 | 실제 retain 수명 테스트 | 동일 객체와 진행 중 상태 유지 확인 |
| Room 반영 | 실제 메모리 Room + 운영 Mutation key·Subscription | ID·목록 전체 필드·상세 null·구독 재진입 확인 |
| DAO 실패와 후속 쓰기 | 실제 key·actor + 실패 fake DAO | 예외 타입·메시지 전파, 실패 후 다음 쓰기 완료 확인 |
| 대기 호출자 취소 | actor 참조를 유지한 실제 Soil 테스트 | 이미 시작한 쓰기는 완료 |
| actor scope 자체 취소 | 실제 Soil + 중단 가능한 fake DAO | DAO도 취소됨. 별도 application scope의 독립 완료 보장은 제거됨 |

일반적인 화면 pop은 actor scope 자체 취소와 다릅니다. `FavoriteToggler`가 접수한 작업을 마칠 때까지 참조를 유지하는 범위를 검증했습니다. 프로세스 종료 중 미완료 쓰기의 내구성, 강제 종료 후 쓰기 완료는 보장하지 않습니다. 이 브랜치에서는 기기 조작, 프로세스 종료 QA, Android Studio 재가져오기, 제출 패키지 재생성을 실행하지 않았습니다.

## 실행 결과

환경은 JDK Temurin 21.0.11, Gradle daemon 기준 21, Java/Kotlin target 17입니다. 모든 아래 실행은 캐시된 의존성을 사용한 `--offline` 실행입니다.

| 실행 | 결과 |
| --- | --- |
| 변경 전 `./gradlew testDebugUnitTest test --continue --offline --rerun-tasks` | 29 suite, 140 tests, 실패·오류·skip 0 |
| 변경 후 `./gradlew :core:data:testDebugUnitTest :core:ui:testDebugUnitTest --offline --rerun-tasks` | 성공 |
| 최종 `./gradlew assembleDebug testDebugUnitTest test lintDebug --continue --offline --rerun-tasks` | 503 tasks 실행, 성공. 29 suite, 145 tests, 실패·오류·skip 0 |

145개는 기존 Writer 테스트 3개를 Mutation 경계 테스트 5개로 교체하고, retain 수명 테스트 3개를 추가한 수입니다. Lint 오류는 0개이며 app의 `GradleDependency`, `DataExtractionRules` 경고 2개가 남습니다. 테스트 개수 자체를 코드 커버리지 또는 기기 동작 검증으로 해석하지 않습니다.

## 결함 주입 검증

다음 운영 코드 변경을 하나씩 임시 적용해 관련 테스트 실패를 확인한 뒤 원복했습니다. 결함 코드는 커밋하지 않았으며, 위 최종 전체 검사는 원복된 코드에서 실행했습니다.

| 임시 결함 | 검출한 사실 |
| --- | --- |
| DAO 저장을 무시 | Mutation 경계 검사 실패. 실제 Room 목록 구독에도 도서가 나타나지 않아 실패 |
| pop에서 `job.complete()` 대신 `job.cancel()` | 진행 중·대기 명령과 화면 간 순서 관련 수명 테스트 4개 실패 |
| 화면별 서로 다른 Mutation ID 사용 | 느린 저장·빠른 해제 검사에서 최종 DB 상태가 미저장이 아닌 저장으로 남아 실패. retain 재진입 동일 객체 검사도 실패 |

같은 지연시간을 사용한 경우 actor 분리의 순서 결함 검출이 충분하지 않아, 최종 상태를 확인하는 서로 다른 작업 시간 사례를 포함했습니다. 결함 주입은 위 세 가지 변경에 한정한 수동 검사이며 포괄적인 mutation score를 산출하지 않았습니다.

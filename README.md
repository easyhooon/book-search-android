# book-search-android

Kakao 책 검색 API로 도서를 찾고, 관심 도서를 기기에 저장하는 Android 앱 과제 저장소입니다.

## 현재 상태

2026-10-07 기준 **요구사항과 구현 전략만 정리한 상태**입니다. 앱 코드, Gradle Wrapper, APK, 테스트 실행 결과는 아직 없습니다. 아래 명령은 구현 후 충족해야 할 빌드 기준이며, 현재 실행 가능한 명령이 아닙니다.

```sh
./gradlew assembleDebug
```

## 구현 방향

- Kotlin, Coroutines, Flow, Jetpack Compose
- Android 전용 구성. KMP 프로젝트 설정은 도입하지 않음
- 검색 / 즐겨찾기 하단 탭 2개와 공통 도서 카드·상세 화면
- Navigation 3 + retain 기반 화면 상태 관리 (Circuit 미사용)
- Room + Flow를 통한 즐겨찾기 영속 저장과 화면 간 상태 동기화
- Retrofit + OkHttp + kotlinx.serialization 원격 통신
- Soil typed-key와 InfiniteQuery 기반 데이터 접근·페이지네이션. 단순 위임 Repository는 생략
- Metro 의존성 주입, Coil 이미지 로딩 (Coil 3 권장)
- 불안정한 네트워크, 화면 회전, 태블릿·폴더블 대응

retain은 결과와 무관하게 직접 익히려는 선택입니다. 프로세스 종료 복원, 코루틴 소유권, 화면 제거 시 정리를 별도 설계하고 검증합니다.

## 문서

1. [구현 전략과 3일 일정](docs/STRATEGY.md): 설계 결정, 참고 코드의 활용 범위, 위험과 검증 순서
2. [요구사항 체크리스트](docs/REQUIREMENTS.md): 필수 요구사항과 추가 권장사항 구분
3. [AI 활용 기록](docs/AI_USAGE.md): 실제 도움받은 범위와 아직 수행하지 않은 작업
4. [아키텍처](docs/ARCHITECTURE.md): 기술별 역할, 데이터 흐름, 상태 소유권, 테스트 경계
5. [저장소 작업 지침](AGENTS.md): 문서·커밋·검증·보안 기준

## 빌드 기준과 주의사항

요청된 기준은 Gradle 9.4.1, AGP 9.2.1, Kotlin 2.4.0, KSP 사용 시 2.3.10, JDK 17, compileSdk 37, 최신 안정 Android Studio입니다. **이 조합의 실제 호환성과 빌드는 아직 검증하지 않았습니다.** 구현 첫 단계에서 확인하고, 충돌 시 근거와 대안을 기록한 뒤 버전 변경 여부를 결정합니다.

API 키는 local.properties에서 읽으며 실제 키와 해당 파일은 Git에 커밋하지 않습니다. 제출용 ZIP에는 local.properties를 포함하고 제출 키는 전형 종료 전까지 폐기하지 않습니다. 실제 키 설정 및 최종 제출 파일 준비는 사용자가 직접 확인합니다. GitHub 저장소명·설명·문서에는 회사명을 넣지 않습니다.

목표 제출 시각: **2026-10-10(토) 14:30 KST**. 최종 실행 방법, 검증 결과, 알려진 제한은 구현 완료 후 이 README에 갱신합니다.

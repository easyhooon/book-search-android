# 로컬 Maestro 회귀 테스트

[요구사항 87개·QA 45행 대응표와 실제 결과](../docs/MAESTRO_E2E.md). Maestro CLI 2.11.0, Python 3.9+, adb와 승인된 잠금 해제 테스트 기기를 사용합니다. 문법 통과와 UI assertion 통과는 구분합니다.

```sh
# Metro 컴파일러 실행에는 JDK 21이 필요합니다.
JAVA_HOME=/path/to/jdk21 ./gradlew :app:assembleE2e :app:assembleE2eNoKey testDebugUnitTest :core:model:test lintDebug -Pkotlin.compiler.execution.strategy=in-process -Pkotlin.compiler.runViaBuildToolsApi=false
python3 e2e/run.py --device DEVICE_ID --allow-e2e-install-and-reset
```

기본 runner는 가짜 키·localhost fixture만 사용합니다. 별도 `.e2e` 앱과 Maestro 드라이버 설치, `.e2e` 데이터 초기화, HOME/background am kill/Launcher 재실행을 수행합니다. 기존 앱 데이터는 건드리지 않습니다. port 8877 기존 reverse가 있으면 중단하고 자신이 만든 규칙만 finally 제거합니다. fixture는 outbound 요청을 하지 않으며 인증·size/sort/page·요청 횟수·경합 시작/완료 시각을 확인합니다. JUnit·화면·명령·요청·PID와 실패 결과는 Git 제외 `e2e/results/`에 남깁니다.

경합 runner는 Maestro로 slow 입력을 준비한 뒤 현재 UI hierarchy의 선택자 좌표를 읽어 critical 입력을 한 adb shell 호출로 보냅니다. 앱 HTTP 타임아웃 안에서 두 요청을 겹치게 하고 이전 응답 완료 뒤 Maestro assertion으로 새 결과 유지와 서버 시간 기록을 확인합니다. 순수 YAML만 사용하는 흐름과 구분합니다.

일부 흐름은 `--only 05-boundaries 06-navigation retry more-error race missing no-key`처럼 선택할 수 있습니다. negative-control은 의도적으로 존재하지 않는 문구의 assertion 실패를 요구합니다. 실제 앱 결함을 주입한 검증은 아닙니다.

## AVD 적응형 화면

```sh
python3 e2e/run.py --device emulator-5554 --allow-e2e-install-and-reset --allow-emulator-config --only adaptive
```

기존 AVD를 사용합니다. 가상 접힘·화면 크기·밀도·회전·다크를 변경하고 원본값과 복원값을 비교합니다. 가상 접힘과 태블릿 뷰포트는 물리 힌지·실물 태블릿을 대체하지 않습니다. 테스트 동안 다른 작업이 같은 AVD의 전면 앱을 변경하면 실패할 수 있으므로 기기를 독점 사용합니다.

## 실제 API smoke

로컬 키 사용·실제 API 호출·비행기 모드/회전/다크 변경을 승인받은 뒤 실행합니다. 일반 fixture 회귀와 독립이며 기존 앱 대신 `.e2e`만 사용합니다. 먼저 fixture APK를 별도 경로에 복사하고 실제 smoke APK를 만듭니다. 경로와 APK/키 파일은 Git에 추가하지 않습니다.

```sh
cp app/build/outputs/apk/e2e/app-e2e.apk /tmp/fixture.apk
JAVA_HOME=/path/to/jdk21 ./gradlew :app:assembleE2e -PbookSearchE2eRealApi=true -Pkotlin.compiler.execution.strategy=in-process -Pkotlin.compiler.runViaBuildToolsApi=false
cp app/build/outputs/apk/e2e/app-e2e.apk /tmp/real-smoke.apk
python3 e2e/smoke.py --device DEVICE_ID --real-apk /tmp/real-smoke.apk --fixture-apk /tmp/fixture.apk --allow-real-api-and-phone-settings
```

smoke는 동일 PID의 연속 logcat에서 GET 시도/HTTP200을 기록하며 6건 초과를 감지하면 별도 앱을 중단합니다. 오프라인 GET 로그는 서버 수신 증거가 아닙니다. finally에서 설정 복원·앱 종료·fixture 재설치를 수행하고 오류를 기록합니다. smoke 후 실제 키가 포함된 임시 APK를 삭제하고 기본 fixture APK를 다시 빌드합니다.

한글 `korean-samsung`은 이미 한국어로 설정된 삼성 키보드의 자판 선택자를 누릅니다. Android Unicode inputText나 clipboard·새 IME 설치를 사용하지 않으며 Gboard용 공통 흐름이 아닙니다.

## MCP

로컬 stdio 등록 예시입니다. OAuth/Cloud 로그인은 필요하지 않습니다. 등록 후 새 Codex 세션의 tool 호출로 연결을 확인합니다. MCP 연결 자체는 테스트 assertion을 대신하지 않습니다.

```toml
[mcp_servers.maestro]
command = "/absolute/path/to/maestro"
args = ["mcp"]
[mcp_servers.maestro.env]
JAVA_HOME = "/absolute/path/to/jdk21"
MAESTRO_CLI_NO_ANALYTICS = "1"
MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED = "true"
```

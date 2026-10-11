#!/usr/bin/env python3
"""Opt-in real API, approved phone appearance/airplane tests. Always restore fixture APK/settings."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import threading
import time
import zipfile

ROOT = Path(__file__).resolve().parents[1]
APP = "io.github.easyhooon.booksearch.e2e"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--device", required=True)
    parser.add_argument("--real-apk", type=Path, required=True)
    parser.add_argument("--fixture-apk", type=Path, required=True)
    parser.add_argument("--allow-real-api-and-phone-settings", action="store_true", required=True)
    args = parser.parse_args()
    with zipfile.ZipFile(args.real_apk) as archive:
        if any(b"http://127.0.0.1:8877/" in archive.read(n) for n in archive.namelist() if n.endswith(".dex")):
            raise RuntimeError("real APK contains fixture endpoint; refusing mixed evidence")
    out = ROOT / "e2e/results" / (time.strftime("%Y%m%d-%H%M%S") + "-real")
    out.mkdir(parents=True)
    adb = ["adb", "-s", args.device]
    env = dict(os.environ, MAESTRO_CLI_NO_ANALYTICS="1", MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED="true")
    outcomes = {}
    recorder = None
    reader = None
    attempts = []
    responses = []
    budget_exceeded = threading.Event()
    measured_pid = None

    def device(*cmd):
        return subprocess.check_output(adb + list(cmd), text=True).strip()

    def snapshot():
        return dict(airplane=device("shell", "settings", "get", "global", "airplane_mode_on"),
                    rotation=device("shell", "settings", "get", "system", "user_rotation"),
                    auto_rotate=device("shell", "settings", "get", "system", "accelerometer_rotation"),
                    night=device("shell", "cmd", "uimode", "night"))

    if "showing=false" not in device("shell", "dumpsys", "window", "policy"):
        raise RuntimeError("unlock approved phone first")
    if "tcp:8877" in device("reverse", "--list"):
        raise RuntimeError("fixture reverse exists; refusing real smoke")
    original = snapshot()
    (out / "original-settings.json").write_text(json.dumps(original, indent=2))

    def flow(name):
        folder = out / name
        folder.mkdir()
        with (folder / "console.log").open("w") as log:
            result = subprocess.run([str(Path.home() / ".maestro/bin/maestro"), "--device", args.device, "test",
                                     "--format", "JUNIT", "--output", str(folder / "report.xml"),
                                     "--test-output-dir", str(folder / "artifacts"),
                                     "--debug-output", str(folder / "debug"),
                                     str(ROOT / f"e2e/flows/{name}.yaml")], env=env, stdout=log, stderr=subprocess.STDOUT)
        outcomes[name] = result.returncode
        print(f"{name}: exit {result.returncode}", flush=True)
        if result.returncode:
            raise RuntimeError(f"smoke assertion failed: {name}")
        if measured_pid and device("shell", "pidof", APP) != measured_pid:
            raise RuntimeError("process changed; request measurement is incomplete")
        if budget_exceeded.is_set():
            raise RuntimeError("real API attempt budget exceeded")

    try:
        device("install", "-r", str(args.real_apk))
        flow("smoke-launch")
        measured_pid = device("shell", "pidof", APP)
        recorder = subprocess.Popen(adb + ["logcat", f"--pid={measured_pid}", "-v", "threadtime", "-T", "1"],
                                    stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True)
        def collect():
            for line in recorder.stdout:
                if "--> GET https://dapi.kakao.com/v3/search/book" in line:
                    attempts.append(line.strip())
                    if len(attempts) > 6:
                        budget_exceeded.set()
                        device("shell", "am", "force-stop", APP)
                        return
                elif "<-- 200 https://dapi.kakao.com/v3/search/book" in line:
                    responses.append(line.strip())
        reader = threading.Thread(target=collect, daemon=True)
        reader.start()
        flow("smoke-online")
        flow("smoke-rotate")
        device("shell", "cmd", "uimode", "night", "yes")
        flow("smoke-theme")
        device("shell", "cmd", "uimode", "night", "no")
        device("shell", "cmd", "connectivity", "airplane-mode", "enable")
        time.sleep(3)
        active = re.search(r"Active default network: (\S+)", device("shell", "dumpsys", "connectivity"))
        outcomes["airplane_state"] = device("shell", "settings", "get", "global", "airplane_mode_on")
        outcomes["default_network_after_airplane"] = active.group(1) if active else "unknown"
        if active and active.group(1).lower() in ("none", "null", "-1"):
            flow("smoke-offline")
            device("shell", "cmd", "connectivity", "airplane-mode", "disable")
            time.sleep(5)
            flow("smoke-retry")
        else:
            outcomes["real_disconnect"] = "BLOCKED: airplane mode retained default network; WiFi not changed"
            print(outcomes["real_disconnect"], flush=True)
        time.sleep(.2)
        if not 1 <= len(attempts) <= 6:
            raise RuntimeError("real API request budget exceeded or request evidence missing")
    finally:
        if recorder is not None:
            recorder.terminate()
            recorder.wait(timeout=5)
            reader.join(timeout=2)
        outcomes["recorded_api_attempts"] = len(attempts)
        outcomes["recorded_http200"] = len(responses)
        (out / "api-requests.txt").write_text("\n".join(attempts) + "\n")
        (out / "api-http200.txt").write_text("\n".join(responses) + "\n")
        errors = []
        for cmd in (("shell", "cmd", "connectivity", "airplane-mode", "enable" if original["airplane"] == "1" else "disable"),
                    ("shell", "settings", "put", "system", "user_rotation", original["rotation"]),
                    ("shell", "settings", "put", "system", "accelerometer_rotation", original["auto_rotate"]),
                    ("shell", "cmd", "uimode", "night", original["night"].split(":", 1)[1].strip()),
                    ("shell", "am", "force-stop", APP),
                    ("install", "-r", str(args.fixture_apk))):
            try:
                device(*cmd)
            except Exception as error:
                errors.append(str(error))
        restored = snapshot()
        outcomes["settings_restored"] = restored == original
        outcomes["cleanup_errors"] = errors
        (out / "restored-settings.json").write_text(json.dumps(restored, indent=2))
        (out / "summary.json").write_text(json.dumps(outcomes, indent=2))
        print(f"Results: {out}", flush=True)
        if errors or restored != original:
            raise RuntimeError("smoke cleanup/restoration failed")


if __name__ == "__main__":
    main()

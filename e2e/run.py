#!/usr/bin/env python3
"""Explicit device, isolated app only. No real API or global device settings."""
import argparse
import json
import os
import re
from pathlib import Path
import subprocess
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
APP = "io.github.easyhooon.booksearch.e2e"
URL = "http://127.0.0.1:8877"


def control(path, data=None):
    request = urllib.request.Request(URL + path,
                                     data=json.dumps(data).encode() if data is not None else None,
                                     headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=3) as response:
        return json.load(response)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--device", required=True)
    parser.add_argument("--allow-e2e-install-and-reset", action="store_true", required=True)
    parser.add_argument("--maestro", default=str(Path.home() / ".maestro/bin/maestro"))
    parser.add_argument("--only", nargs="*")
    parser.add_argument("--allow-emulator-config", action="store_true")
    args = parser.parse_args()
    out = ROOT / "e2e/results" / time.strftime("%Y%m%d-%H%M%S")
    out.mkdir(parents=True)
    env = dict(os.environ, MAESTRO_CLI_NO_ANALYTICS="1", MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED="true")
    adb = ["adb", "-s", args.device]

    def device(*cmd):
        return subprocess.check_output(adb + list(cmd), text=True).strip()

    if device("get-state") != "device":
        raise RuntimeError("device is not authorized")
    policy = device("shell", "dumpsys", "window", "policy")
    keyguard = re.search(r"KeyguardServiceDelegate\s+showing=(true|false)", policy)
    if keyguard is None or keyguard.group(1) != "false":
        raise RuntimeError("unlock the test device first; no security settings will be changed")
    apk = ROOT / "app/build/outputs/apk/e2e/app-e2e.apk"
    if not apk.is_file():
        raise RuntimeError("build :app:assembleE2e first")
    with zipfile.ZipFile(apk) as archive:
        if not any(b"http://127.0.0.1:8877/" in archive.read(name)
                   for name in archive.namelist() if name.endswith(".dex")):
            raise RuntimeError("fixture APK endpoint missing; rebuild without bookSearchE2eRealApi")
    # Never replace an existing reverse rule; cleanup removes only the rule created here.
    if "tcp:8877" in device("reverse", "--list"):
        raise RuntimeError("port 8877 already forwarded; refusing to modify existing rule")
    server = None
    forwarded = False
    outcomes = {}
    try:
        try:
            control("/__health")
        except Exception:
            pass
        else:
            raise RuntimeError("port 8877 already in use; refusing existing fixture")
        server = subprocess.Popen([sys.executable, str(ROOT / "e2e/support/fixture_server.py")],
                                  stdout=(out / "fixture.log").open("w"), stderr=subprocess.STDOUT)
        for _ in range(30):
            try:
                control("/__health")
                break
            except Exception:
                time.sleep(.1)
        else:
            raise RuntimeError("fixture did not start")
        device("install", "-r", str(apk))
        device("reverse", "tcp:8877", "tcp:8877")
        forwarded = True

        def flow(name, negative=False):
            run_name = name
            occurrence = 2
            while run_name in outcomes:
                run_name = f"{name}-{occurrence}"
                occurrence += 1
            folder = out / run_name
            folder.mkdir()
            with (folder / "console.log").open("w") as log:
                result = subprocess.run([args.maestro, "--device", args.device, "test",
                                         "--format", "JUNIT", "--output", str(folder / "report.xml"),
                                         "--test-output-dir", str(folder / "artifacts"),
                                         "--debug-output", str(folder / "debug"),
                                         str(ROOT / f"e2e/flows/{name}.yaml")],
                                        env=env, stdout=log, stderr=subprocess.STDOUT)
            outcomes[run_name] = dict(exit_code=result.returncode, expected_failure=negative)
            print(f"{run_name}: exit {result.returncode}", flush=True)
            if (result.returncode == 0) == negative:
                raise RuntimeError(f"unexpected result in {name}; see {folder}")
            if negative:
                report = ET.parse(folder / "report.xml")
                failures = report.findall(".//failure")
                if not any("THIS_TEXT_MUST_NEVER_EXIST_7dc229" in
                           (failure.get("message", "") + (failure.text or "")) for failure in failures):
                    raise RuntimeError("negative control failed for a different reason")

        names = args.only or ["01-search", "02-favorites", "03-pages", "04-error", "05-boundaries",
                              "06-navigation", "retry", "more-error", "race", "missing", "restore", "no-key"]
        for name in names:
            control("/__reset", {})
            if name == "adaptive":
                if not args.allow_emulator_config or not args.device.startswith("emulator-"):
                    raise RuntimeError("adaptive requires an approved emulator and --allow-emulator-config")
                if "mOverrideState=Optional.empty" not in device("shell", "dumpsys", "device_state"):
                    raise RuntimeError("existing device-state override; refusing to replace it")
                original = dict(size=device("shell", "wm", "size"), density=device("shell", "wm", "density"),
                                rotation=device("shell", "settings", "get", "system", "user_rotation"),
                                auto_rotate=device("shell", "settings", "get", "system", "accelerometer_rotation"),
                                night=device("shell", "cmd", "uimode", "night"),
                                state=device("shell", "cmd", "device_state", "print-state"))
                (out / "emulator-original.json").write_text(json.dumps(original, indent=2))
                def posture(state):
                    device("shell", "cmd", "device_state", "state", state)
                    time.sleep(1)  # Wait for cover/inner display handoff before dismissing swipe lock.
                    for _ in range(6):
                        device("shell", "input", "keyevent", "KEYCODE_WAKEUP")
                        # Standard request only dismisses an unsecured AVD swipe lock.
                        device("shell", "wm", "dismiss-keyguard")
                        time.sleep(.5)
                        if "showing=false" in device("shell", "dumpsys", "window", "policy"):
                            # Fold policy has per-posture rotation; lock after the handoff.
                            device("shell", "wm", "user-rotation", "lock", "0")
                            time.sleep(.5)
                            return
                    raise RuntimeError("AVD display handoff remains locked; no security settings changed")
                try:
                    # The existing AVD sensor may be landscape despite user_rotation=0.
                    device("shell", "settings", "put", "system", "accelerometer_rotation", "0")
                    device("shell", "settings", "put", "system", "user_rotation", "0")
                    posture("2")
                    flow("adaptive-setup")
                    flow("adaptive-wide")
                    posture("0")
                    flow("adaptive-narrow")
                    posture("1")
                    flow("adaptive-detail")
                    posture("2")
                    flow("adaptive-wide")
                    flow("adaptive-rotate")
                    device("shell", "cmd", "uimode", "night", "yes")
                    flow("adaptive-wide")
                    device("shell", "cmd", "uimode", "night", "no")
                    flow("adaptive-sheet-setup")
                    posture("0")
                    flow("adaptive-sheet")
                    posture("2")
                    flow("adaptive-sheet")
                    device("shell", "wm", "size", "1920x1200")
                    device("shell", "wm", "density", "240")
                    flow("adaptive-tablet")
                finally:
                    device("shell", "cmd", "device_state", "state", "reset")
                    for _ in range(20):
                        if device("shell", "wm", "size") == original["size"]:
                            break
                        time.sleep(.2)
                    for key, command in (("size", "size"), ("density", "density")):
                        override = re.search(r"Override .*?: (.+)", original[key])
                        device("shell", "wm", command, override.group(1) if override else "reset")
                    device("shell", "settings", "put", "system", "user_rotation", original["rotation"])
                    device("shell", "settings", "put", "system", "accelerometer_rotation", original["auto_rotate"])
                    device("shell", "cmd", "uimode", "night", original["night"].split(":", 1)[1].strip())
                    time.sleep(.5)
                    restored = dict(size=device("shell", "wm", "size"), density=device("shell", "wm", "density"),
                                    rotation=device("shell", "settings", "get", "system", "user_rotation"),
                                    auto_rotate=device("shell", "settings", "get", "system", "accelerometer_rotation"),
                                    night=device("shell", "cmd", "uimode", "night"),
                                    state=device("shell", "cmd", "device_state", "print-state"))
                    outcomes["emulator_restored"] = restored == original
                    (out / "emulator-restored.json").write_text(json.dumps(restored, indent=2))
                    if restored != original:
                        raise RuntimeError("emulator restoration mismatch")
            elif name == "no-key":
                no_key_apk = ROOT / "app/build/outputs/apk/e2eNoKey/app-e2eNoKey.apk"
                if not no_key_apk.is_file():
                    raise RuntimeError("build :app:assembleE2eNoKey first")
                flow("no-key-setup")
                control("/__reset", {})
                try:
                    device("install", "-r", str(no_key_apk))
                    flow("no-key-assert")
                    assert control("/__requests") == [], "missing-key variant made an API request"
                finally:
                    device("install", "-r", str(apk))
                    outcomes["fixture_apk_restored"] = True
            elif name in ("retry", "more-error"):
                query = "fixture" if name == "retry" else "pages:2"
                prefix = "error" if name == "retry" else "more-error"
                control("/__fail", {"queries": [query]})
                flow(f"{prefix}-setup")
                before = control("/__requests")
                time.sleep(2)
                assert control("/__requests") == before, "duplicate error-trigger requests"
                control("/__fail", {"queries": []})
                flow(f"{prefix}-recover")
                after = control("/__requests")
                assert len(after) == len(before) + 1, after
                if name == "more-error":
                    assert after[-1]["page"] == 2 and sum(r["page"] == 1 for r in after) == 1, after
            elif name == "race":
                control("/__delay", {"queries": {"slow": 14}})
                flow("race")
                # Maestro's accessibility settle waits can outlast OkHttp's 15s timeout.
                # Resolve coordinates from THIS screen, then inject the critical sequence
                # in one adb shell call. Maestro still owns setup and final assertions.
                hierarchy_path = f"/data/local/tmp/booksearch-e2e-race-{os.getpid()}.xml"
                try:
                    device("shell", "uiautomator", "dump", hierarchy_path)
                    hierarchy = ET.fromstring(device("shell", "cat", hierarchy_path))
                    def bounds(tag):
                        nodes = [n for n in hierarchy.iter("node") if n.get("resource-id") == tag]
                        if len(nodes) != 1:
                            raise RuntimeError(f"race requires one visible {tag}")
                        return list(map(int, re.findall(r"\d+", nodes[0].get("bounds", ""))))
                    field = bounds("search-field")
                    submit = bounds("query-submit")
                    clear = bounds("query-clear")
                    def tap(rect):
                        return f"input tap {(rect[0] + rect[2]) // 2} {(rect[1] + rect[3]) // 2}"
                    focus = [field[0] + 40, field[1], field[0] + 40, field[3]]
                    actions = [tap(submit), tap(clear), tap(focus), "input text new", tap(submit)]
                    (out / "race-input.json").write_text(json.dumps(dict(
                        method="UI-derived coordinates; single adb shell sequence", commands=actions), indent=2))
                    device("shell", "; ".join(actions))
                finally:
                    device("shell", "rm", "-f", hierarchy_path)
                # Verify the winner again after the old HTTP response actually completes.
                deadline = time.monotonic() + 20
                while not (control("/__events") and control("/__events")[0]["completed"]):
                    if time.monotonic() > deadline:
                        raise RuntimeError("old response did not complete")
                    time.sleep(.5)
                flow("race-assert")
            elif name == "restore":
                flow("restore-setup")
                before = device("shell", "pidof", APP)
                if not before:
                    raise RuntimeError("missing process before kill")
                device("shell", "input", "keyevent", "KEYCODE_HOME")
                time.sleep(2)  # Activity must save state before background kill.
                device("shell", "am", "kill", APP)
                def pid():
                    result = subprocess.run(adb + ["shell", "pidof", APP], capture_output=True, text=True)
                    if result.returncode not in (0, 1):
                        raise RuntimeError("pidof transport error")
                    return result.stdout.strip()

                for _ in range(30):
                    if not pid():
                        break
                    time.sleep(.2)
                else:
                    raise RuntimeError("am kill did not terminate background process")
                # Reject all remote searches, without changing WiFi/mobile settings.
                control("/__fail", {"queries": ["fixture"]})
                device("shell", "am", "start", "-a", "android.intent.action.MAIN",
                       "-c", "android.intent.category.LAUNCHER", "-f", "0x10200000",
                       "-n", f"{APP}/io.github.easyhooon.booksearch.MainActivity")
                for _ in range(30):
                    after = pid()
                    if after:
                        break
                    time.sleep(.2)
                if not after or before == after:
                    raise RuntimeError("new process not proven")
                outcomes["process"] = dict(before_pid=before, after_pid=after, command="HOME; am kill; am start")
                flow("restore-assert")
            else:
                if name not in ("01-search", "02-favorites", "03-pages", "04-error", "05-boundaries", "06-navigation", "missing", "korean-samsung"):
                    raise RuntimeError("unknown flow")
                flow(name)
            requests = control("/__requests")
            (out / f"{name}-requests.json").write_text(json.dumps(requests, indent=2))
            if name == "03-pages":
                assert requests == [dict(query="pages", sort="accuracy", page=p, size=20) for p in (1, 2)], requests
            if name == "01-search":
                assert any(r["sort"] == "latest" and r["page"] == 1 for r in requests), requests
            if name == "02-favorites":
                assert len(requests) == 1, requests  # local actions must not search API.
            if name == "04-error":
                assert len(requests) >= 2 and all(r["query"] == "servererror" for r in requests), requests
            if name == "adaptive":
                assert len(requests) == 1, requests  # window/theme transitions must not refetch.
            if name == "race":
                assert [r["query"] for r in requests] == ["slow", "new"], requests
                events = control("/__events")
                (out / "race-events.json").write_text(json.dumps(events, indent=2))
                assert events[0]["completed"] and events[1]["started"] < events[0]["completed"], events
            if name == "korean-samsung":
                assert requests == [dict(query="코틀린", sort="accuracy", page=1, size=20)], requests
        flow("negative-control", negative=True)
    finally:
        try:
            if forwarded:
                device("reverse", "--remove", "tcp:8877")
                outcomes["reverse_cleanup"] = "removed"
        except Exception as error:
            outcomes["reverse_cleanup"] = f"FAILED: {error}"
            raise
        finally:
            if server is not None:
                server.terminate()
                server.wait(timeout=5)
            (out / "summary.json").write_text(json.dumps(outcomes, indent=2))
            print(f"Results: {out}", flush=True)


if __name__ == "__main__":
    main()

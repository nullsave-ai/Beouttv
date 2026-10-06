#!/usr/bin/env python3
import os
import sys
import time
import subprocess

PROJECT_DIR = "/storage/emulated/0/CodeOnTheGoProjects/Beouttv"
TARGET_RELEASE = os.path.join(PROJECT_DIR, "app/build/outputs/apk/release/app-release.apk")
TARGET_DEBUG = os.path.join(PROJECT_DIR, "app/build/outputs/apk/debug/app-debug.apk")
SCRIPT = os.path.join(PROJECT_DIR, "package_universal_apk.py")

last_mtimes = {
    TARGET_RELEASE: os.path.getmtime(TARGET_RELEASE) if os.path.exists(TARGET_RELEASE) else 0,
    TARGET_DEBUG: os.path.getmtime(TARGET_DEBUG) if os.path.exists(TARGET_DEBUG) else 0,
}

print(f"[*] Watching for updates on BeoutTV APK builds (Release & Debug)...", flush=True)

while True:
    try:
        time.sleep(2)
        for target in [TARGET_RELEASE, TARGET_DEBUG]:
            if os.path.exists(target):
                current_mtime = os.path.getmtime(target)
                if current_mtime > last_mtimes.get(target, 0):
                    print(f"[*] Detected new APK build at {target}! Waiting for write completion...", flush=True)
                    prev_size = -1
                    for _ in range(30):
                        time.sleep(1)
                        cur_size = os.path.getsize(target)
                        if cur_size > 0 and cur_size == prev_size:
                            break
                        prev_size = cur_size
                    print(f"[*] Running Universal Packaging & Signing Pipeline on {target}...", flush=True)
                    res = subprocess.run(["python3", SCRIPT, target])
                    last_mtimes[target] = os.path.getmtime(target)
                    if res.returncode == 0:
                        print("[DONE] Universal APK ready at /storage/emulated/0/Download/BeoutTV-Universal-v3.0.apk", flush=True)
                    else:
                        print("[!] Packaging pipeline failed, see output above.", flush=True)
    except KeyboardInterrupt:
        break
    except Exception as e:
        print(f"[!] Watcher error: {e}", flush=True)
        time.sleep(5)

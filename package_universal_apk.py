#!/usr/bin/env python3
import os
import sys
import subprocess
import shutil
import zipfile
import struct

PROJECT_DIR = "/storage/emulated/0/CodeOnTheGoProjects/Beouttv"
RELEASE_APK = os.path.join(PROJECT_DIR, "app/build/outputs/apk/release/app-release.apk")
DEBUG_APK = os.path.join(PROJECT_DIR, "app/build/outputs/apk/debug/app-debug.apk")
KEYSTORE = os.path.join(PROJECT_DIR, "release.keystore")
KEYSTORE_PASS = "pass:rschqrIo"
KEY_ALIAS = "Jm8bvOq3oo4vBF"
OUT_DOWNLOAD_DIR = "/storage/emulated/0/Download"
FINAL_APK = os.path.join(OUT_DOWNLOAD_DIR, "BeoutTV-Universal-v3.0.apk")
FINAL_APK_AR = os.path.join(OUT_DOWNLOAD_DIR, "BeoutTV-Universal-v3.0 الاصدار الثالث للهواتف والشاشات.apk")
TEMP_ALIGNED_APK = os.path.join(PROJECT_DIR, "app/build/outputs/apk/release/temp-aligned.apk")

def zipalign(in_apk, out_apk, alignment=4):
    print("[*] Running 4-Byte Zipalign on APK...")
    with open(in_apk, "rb") as fin:
        data = fin.read()

    eocd_pos = data.rfind(b"\x50\x4b\x05\x06")
    if eocd_pos == -1:
        raise ValueError("Invalid ZIP file (EOCD not found)")
        
    disk_num, cd_disk, disk_entries, total_entries, cd_size, cd_offset, comment_len = struct.unpack(
        "<HHHHIIH", data[eocd_pos+4:eocd_pos+22]
    )
    
    cd_entries = []
    pos = cd_offset
    for _ in range(total_entries):
        (sig, v_made, v_needed, flags, comp_method, mod_time, mod_date, crc32, 
         comp_size, uncomp_size, fn_len, extra_len, comm_len, disk_start, 
         int_attr, ext_attr, local_offset) = struct.unpack("<IHHHHHHIIIHHHHHII", data[pos:pos+46])
        fn = data[pos+46 : pos+46+fn_len]
        extra = data[pos+46+fn_len : pos+46+fn_len+extra_len]
        comment = data[pos+46+fn_len+extra_len : pos+46+fn_len+extra_len+comm_len]
        pos += 46 + fn_len + extra_len + comm_len
        cd_entries.append({
            "v_made": v_made, "v_needed": v_needed, "flags": flags, "comp_method": comp_method,
            "mod_time": mod_time, "mod_date": mod_date, "crc32": crc32, "comp_size": comp_size,
            "uncomp_size": uncomp_size, "fn": fn, "extra": extra, "comment": comment,
            "int_attr": int_attr, "ext_attr": ext_attr, "local_offset": local_offset
        })

    out = bytearray()
    new_cd_entries = []

    for entry in cd_entries:
        loc = entry["local_offset"]
        sig, v_needed, flags, comp_method, mod_time, mod_date, crc32, comp_size, uncomp_size, fn_len, extra_len = struct.unpack(
            "<IHHHHHIIIHH", data[loc:loc+30]
        )
        fn = data[loc+30 : loc+30+fn_len]
        raw_payload = data[loc+30+fn_len+extra_len : loc+30+fn_len+extra_len+comp_size]

        new_loc = len(out)
        header_len = 30 + len(fn)
        new_extra = bytearray()

        if comp_method == 0:  # Stored -> Align to 4 bytes
            current_payload_offset = new_loc + header_len
            remainder = current_payload_offset % alignment
            if remainder != 0:
                new_extra = bytearray(alignment - remainder)

        out.extend(struct.pack(
            "<IHHHHHIIIHH", sig, v_needed, flags, comp_method, mod_time, mod_date, 
            crc32, comp_size, uncomp_size, len(fn), len(new_extra)
        ))
        out.extend(fn)
        out.extend(new_extra)
        out.extend(raw_payload)

        new_entry = dict(entry)
        new_entry["local_offset"] = new_loc
        new_entry["extra"] = bytes(new_extra)
        new_cd_entries.append(new_entry)

    new_cd_offset = len(out)
    for entry in new_cd_entries:
        out.extend(struct.pack(
            "<IHHHHHHIIIHHHHHII",
            0x02014b50, entry["v_made"], entry["v_needed"], entry["flags"], entry["comp_method"],
            entry["mod_time"], entry["mod_date"], entry["crc32"], entry["comp_size"],
            entry["uncomp_size"], len(entry["fn"]), len(entry["extra"]), len(entry["comment"]),
            0, entry["int_attr"], entry["ext_attr"], entry["local_offset"]
        ))
        out.extend(entry["fn"])
        out.extend(entry["extra"])
        out.extend(entry["comment"])

    new_cd_size = len(out) - new_cd_offset
    out.extend(struct.pack(
        "<IHHHHIIH", 0x06054b50, 0, 0, len(new_cd_entries), len(new_cd_entries), 
        new_cd_size, new_cd_offset, 0
    ))

    with open(out_apk, "wb") as fout:
        fout.write(out)
    print("    [+] Zipalign completed successfully.")

def main():
    source_apk = None
    if len(sys.argv) > 1 and os.path.exists(sys.argv[1]):
        source_apk = sys.argv[1]
    elif os.path.exists(RELEASE_APK) and os.path.exists(DEBUG_APK):
        source_apk = RELEASE_APK if os.path.getmtime(RELEASE_APK) >= os.path.getmtime(DEBUG_APK) else DEBUG_APK
    elif os.path.exists(RELEASE_APK):
        source_apk = RELEASE_APK
    elif os.path.exists(DEBUG_APK):
        source_apk = DEBUG_APK
    else:
        print("[!] Error: No APK found in release or debug outputs!")
        print("[!] Please click 'Build APK' or 'Run' inside Code on the Go first.")
        sys.exit(1)

    print(f"[*] Found source APK: {source_apk} ({os.path.getsize(source_apk) / (1024*1024):.2f} MB)")
    
    # 1. Strip lib/ and META-INF from source APK if any
    print("[*] Ensuring Architecture-Neutral (Universal) compatibility...")
    temp_unstripped = os.path.join(PROJECT_DIR, "app/build/outputs/apk/release/temp-unstripped.apk")
    os.makedirs(os.path.dirname(temp_unstripped), exist_ok=True)
    shutil.copy2(source_apk, temp_unstripped)
    
    subprocess.run([
        "zip", "-d", temp_unstripped,
        "lib/*", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA",
        "META-INF/MANIFEST.MF", "META-INF/com/**", "META-INF/*.textproto", "META-INF/*.properties"
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    
    # 2. 4-Byte Zipalign
    zipalign(temp_unstripped, TEMP_ALIGNED_APK, alignment=4)
    if os.path.exists(temp_unstripped):
        os.remove(temp_unstripped)

    # 3. apksigner sign with v1 + v2 + v3 + v4
    print("[*] Signing APK with official release.keystore (v1+v2+v3)...")
    os.makedirs(OUT_DOWNLOAD_DIR, exist_ok=True)
    if os.path.exists(FINAL_APK):
        os.remove(FINAL_APK)
    
    sign_cmd = [
        "apksigner", "sign",
        "--ks", KEYSTORE,
        "--ks-pass", KEYSTORE_PASS,
        "--ks-key-alias", KEY_ALIAS,
        "--key-pass", KEYSTORE_PASS,
        "--min-sdk-version", "21",
        "--v1-signing-enabled", "true",
        "--v2-signing-enabled", "true",
        "--v3-signing-enabled", "true",
        "--out", FINAL_APK,
        TEMP_ALIGNED_APK
    ]
    
    result = subprocess.run(sign_cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print("[!] apksigner failed:")
        print(result.stderr)
        sys.exit(1)

    if os.path.exists(TEMP_ALIGNED_APK):
        os.remove(TEMP_ALIGNED_APK)

    # 4. Verify
    print("[*] Verifying signed APK...")
    verify_cmd = ["apksigner", "verify", "--verbose", "--min-sdk-version", "21", FINAL_APK]
    verify_res = subprocess.run(verify_cmd, capture_output=True, text=True)
    print(verify_res.stdout)
    
    final_size_mb = os.path.getsize(FINAL_APK) / (1024 * 1024)
    shutil.copy2(FINAL_APK, FINAL_APK_AR)
    print(f"[SUCCESS] Ready-to-use Universal APK generated at:\n  --> {FINAL_APK} ({final_size_mb:.2f} MB)")
    print(f"  --> {FINAL_APK_AR}")

if __name__ == "__main__":
    main()

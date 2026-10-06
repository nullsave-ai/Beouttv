#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
BeoutTV Channels Converter & Admin Panel Exporter
=================================================
Converts raw channels database (e.g. ostora_channels.json or any custom playlist)
into the exact Firebase RTDB / Admin Panel format expected by BeoutTV.
"""

import json
import os
import sys

INPUT_JSON = "/storage/emulated/0/Srz/ostora_channels.json"
OUTPUT_FIREBASE_JSON = "/storage/emulated/0/CodeOnTheGoProjects/Beouttv/channels_firebase_import.json"

def convert_to_beouttv_schema(input_file=INPUT_JSON, output_file=OUTPUT_FIREBASE_JSON):
    if not os.path.exists(input_file):
        print(f"[-] Input file not found: {input_file}")
        return

    print(f"[*] Reading channels from {input_file}...")
    with open(input_file, "r", encoding="utf-8") as f:
        data = json.load(f)

    raw_channels = data.get("channels", [])
    print(f"[+] Loaded {len(raw_channels)} raw channels.")

    beouttv_channels = []
    for idx, ch in enumerate(raw_channels, 1):
        # Format into BeoutTV expected schema
        category_name = ch.get("group_name") or ch.get("server_name") or "عام"
        
        agent = ch.get("agent")
        if not agent or "ExoPlayer" in agent:
            agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36"
        
        entry = {
            "id": str(ch.get("id", idx)),
            "name": ch.get("name", "Channel"),
            "category": category_name,
            "logoUrl": ch.get("image") or "",
            "streamUrl": ch.get("source_url") or ch.get("source_raw") or "",
            "groupId": str(ch.get("group_id") or ""),
            "serverId": str(ch.get("server_id") or ""),
            "status": 1,
            "userAgent": agent,
            "referer": "https://ostora.tv/",
            "isLive": True
        }
        beouttv_channels.append(entry)

    print(f"[*] Writing {len(beouttv_channels)} channels to {output_file}...")
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(beouttv_channels, f, ensure_ascii=False, indent=2)

    file_size_kb = os.path.getsize(output_file) / 1024
    print(f"[✓] Done! Saved {output_file} ({file_size_kb:.1f} KB)")
    print("\n[!] Instructions to update your App Control Panel / Firebase:")
    print("    1. Go to Firebase Console -> Realtime Database -> Node 'channels'")
    print(f"    2. Import '{output_file}'")
    print("    OR host this JSON file on your server/gateway endpoint.")

if __name__ == "__main__":
    convert_to_beouttv_schema()

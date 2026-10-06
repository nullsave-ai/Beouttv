#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
BeoutTV Automated Firebase Uploader
===================================
Authenticates via Service Account RSA JWT and updates the 'channels' node in Firebase RTDB.
"""

import json
import time
import base64
import urllib.request
import urllib.parse
from Crypto.PublicKey import RSA
from Crypto.Signature import pkcs1_15
from Crypto.Hash import SHA256

SA_FILE = "/storage/emulated/0/CodeOnTheGoProjects/Beouttv/digite-bda04-firebase-adminsdk-lpwzc-ebd442be66.json"
CHANNELS_JSON_FILE = "/storage/emulated/0/CodeOnTheGoProjects/Beouttv/channels_firebase_import.json"
RTDB_BASE = "https://digite-bda04-default-rtdb.firebaseio.com"

def get_google_access_token():
    with open(SA_FILE, "r", encoding="utf-8") as f:
        sa = json.load(f)

    now = int(time.time())
    header = {"alg": "RS256", "typ": "JWT"}
    payload = {
        "iss": sa["client_email"],
        "sub": sa["client_email"],
        "aud": sa["token_uri"],
        "iat": now,
        "exp": now + 3600,
        "scope": "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/firebase.database"
    }

    def b64url(b):
        return base64.urlsafe_b64encode(b).decode().rstrip("=")

    header_b64 = b64url(json.dumps(header).encode())
    payload_b64 = b64url(json.dumps(payload).encode())
    to_sign = f"{header_b64}.{payload_b64}".encode()

    key = RSA.import_key(sa["private_key"])
    h = SHA256.new(to_sign)
    sig = pkcs1_15.new(key).sign(h)
    jwt_token = f"{header_b64}.{payload_b64}.{b64url(sig)}"

    data = urllib.parse.urlencode({
        "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "assertion": jwt_token
    }).encode()

    req = urllib.request.Request(sa["token_uri"], data=data)
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode())
        return res["access_token"]

def upload_channels_to_firebase():
    print("[*] Authenticating with Firebase Service Account...")
    token = get_google_access_token()
    print("[+] Access token acquired.")

    print(f"[*] Reading channels from {CHANNELS_JSON_FILE}...")
    with open(CHANNELS_JSON_FILE, "r", encoding="utf-8") as f:
        channels_data = json.load(f)

    print(f"[+] Total channels to upload: {len(channels_data)}")

    # Update /channels node via HTTP PUT
    target_url = f"{RTDB_BASE}/channels.json?access_token={token}"
    json_bytes = json.dumps(channels_data, ensure_ascii=False).encode("utf-8")

    print("[*] Uploading channels array to Firebase Realtime Database...")
    req = urllib.request.Request(
        target_url,
        data=json_bytes,
        headers={"Content-Type": "application/json"},
        method="PUT"
    )

    with urllib.request.urlopen(req, timeout=60) as resp:
        res_text = resp.read().decode("utf-8")
        print("[+] Upload response received.")
        res_json = json.loads(res_text)
        if isinstance(res_json, list):
            print(f"[✓] SUCCESS! Uploaded {len(res_json)} channels to Firebase Realtime Database.")
        elif isinstance(res_json, dict):
            print(f"[✓] SUCCESS! Uploaded channels dictionary to Firebase Realtime Database ({len(res_json)} entries).")

if __name__ == "__main__":
    upload_channels_to_firebase()

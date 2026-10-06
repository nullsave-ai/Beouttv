#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
BeoutTV Protocol Buffers (Protobuf) Catalog Converter & Sync Tool
-----------------------------------------------------------------
Supports bidirectional conversion between movies.json and movies.pb
"""

import json
import struct
import os
import sys
import argparse

JSON_PATH = "/storage/emulated/0/Json/movies.json"
PB_PATH = "app/src/main/assets/libbeout_core.dat"

def write_varint32(val):
    buf = bytearray()
    while (val & ~0x7F) != 0:
        buf.append((val & 0x7F) | 0x80)
        val >>= 7
    buf.append(val & 0x7F)
    return buf

def write_tag(field_num, wire_type):
    return write_varint32((field_num << 3) | wire_type)

def write_string(field_num, val):
    if not val: return bytearray()
    raw = str(val).encode('utf-8')
    res = bytearray()
    res.extend(write_tag(field_num, 2))
    res.extend(write_varint32(len(raw)))
    res.extend(raw)
    return res

def write_float(field_num, val):
    if val == 0.0: return bytearray()
    res = bytearray()
    res.extend(write_tag(field_num, 5))
    res.extend(struct.pack('<f', float(val)))
    return res

def write_int32(field_num, val):
    if val == 0: return bytearray()
    res = bytearray()
    res.extend(write_tag(field_num, 0))
    res.extend(write_varint32(int(val)))
    return res

def write_bool(field_num, val):
    if not val: return bytearray()
    res = bytearray()
    res.extend(write_tag(field_num, 0))
    res.append(1)
    return res

def write_message(field_num, msg_bytes):
    if not msg_bytes: return bytearray()
    res = bytearray()
    res.extend(write_tag(field_num, 2))
    res.extend(write_varint32(len(msg_bytes)))
    res.extend(msg_bytes)
    return res

def encode_cast_member(name, character, profile_url):
    buf = bytearray()
    buf.extend(write_string(1, name))
    buf.extend(write_string(2, character))
    buf.extend(write_string(3, profile_url))
    return buf

def encode_movie(m):
    buf = bytearray()
    buf.extend(write_string(1, m.get('id', '')))
    buf.extend(write_string(2, m.get('title', '')))
    buf.extend(write_string(3, m.get('original_title', '')))
    buf.extend(write_string(4, m.get('poster', m.get('thumbnail', ''))))
    buf.extend(write_string(5, m.get('backdrop', m.get('cover_image', ''))))
    buf.extend(write_string(6, m.get('logo', '')))
    buf.extend(write_float(7, float(m.get('rating', 7.8))))
    buf.extend(write_int32(8, int(m.get('year', 2026))))
    buf.extend(write_string(9, m.get('imdb_id', '')))
    buf.extend(write_string(10, m.get('duration', '116 دقيقة')))
    buf.extend(write_string(11, m.get('genre', 'أكشن • إثارة')))
    buf.extend(write_string(12, m.get('category', 'أفلام أجنبي')))
    buf.extend(write_string(13, m.get('description', m.get('meta', {}).get('story', ''))))
    buf.extend(write_string(14, m.get('director', 'فريق الإخراج العالمي')))
    buf.extend(write_string(15, m.get('country', 'الولايات المتحدة الأمريكية • عالمي')))
    buf.extend(write_string(16, m.get('language', 'مترجم بالعربية (FHD)')))
    buf.extend(write_string(17, m.get('quality', '1080p WEB-DL')))
    real_id = m.get('realid', m.get('id', ''))
    buf.extend(write_string(18, f'https://govid.live/prem-{real_id}.m3u8'))
    buf.extend(write_string(19, m.get('trailer', '')))
    buf.extend(write_bool(20, False))
    
    cast_list = m.get('cast', []) or m.get('meta', {}).get('cast', [])
    for c in cast_list[:10]:
        if isinstance(c, dict):
            buf.extend(write_message(21, encode_cast_member(c.get('name', ''), c.get('character', 'Cast'), c.get('profile_url', ''))))
        elif isinstance(c, str) and c != 'نجوم العمل الأصلي':
            buf.extend(write_message(21, encode_cast_member(c, 'Cast', '')))
    
    genres = m.get('genres', []) or m.get('genre', '').split('•')
    for g in genres:
        g = g.strip()
        if g:
            buf.extend(write_string(22, g))
            
    return buf

class ProtoReader:
    def __init__(self, data):
        self.data = data
        self.pos = 0

    def has_more(self):
        return self.pos < len(self.data)

    def read_varint32(self):
        res = 0
        shift = 0
        while self.pos < len(self.data):
            b = self.data[self.pos]
            self.pos += 1
            res |= (b & 0x7F) << shift
            if (b & 0x80) == 0:
                break
            shift += 7
        return res

    def read_tag(self):
        if not self.has_more():
            return None
        tag = self.read_varint32()
        return (tag >> 3, tag & 0x07)

    def read_string(self):
        length = self.read_varint32()
        sub = self.data[self.pos:self.pos+length].decode('utf-8', errors='ignore')
        self.pos += length
        return sub

    def read_bytes(self):
        length = self.read_varint32()
        sub = self.data[self.pos:self.pos+length]
        self.pos += length
        return sub

    def read_float(self):
        sub = self.data[self.pos:self.pos+4]
        self.pos += 4
        return struct.unpack('<f', sub)[0]

    def skip_field(self, wire_type):
        if wire_type == 0:
            self.read_varint32()
        elif wire_type == 1:
            self.pos += 8
        elif wire_type == 2:
            length = self.read_varint32()
            self.pos += length
        elif wire_type == 5:
            self.pos += 4

def decode_movie(raw_bytes):
    reader = ProtoReader(raw_bytes)
    m = {}
    genres = []
    cast = []
    while reader.has_more():
        tag = reader.read_tag()
        if not tag: break
        field, wire = tag
        if field == 1: m['id'] = reader.read_string()
        elif field == 2: m['title'] = reader.read_string()
        elif field == 3: m['original_title'] = reader.read_string()
        elif field == 4: m['poster'] = reader.read_string()
        elif field == 5: m['backdrop'] = reader.read_string()
        elif field == 6: m['logo'] = reader.read_string()
        elif field == 7: m['rating'] = round(reader.read_float(), 1)
        elif field == 8: m['year'] = reader.read_varint32()
        elif field == 9: m['imdb_id'] = reader.read_string()
        elif field == 10: m['duration'] = reader.read_string()
        elif field == 11: m['genre'] = reader.read_string()
        elif field == 12: m['category'] = reader.read_string()
        elif field == 13: m['description'] = reader.read_string()
        elif field == 14: m['director'] = reader.read_string()
        elif field == 15: m['country'] = reader.read_string()
        elif field == 16: m['language'] = reader.read_string()
        elif field == 17: m['quality'] = reader.read_string()
        elif field == 18: m['stream_url'] = reader.read_string()
        elif field == 19: m['trailer'] = reader.read_string()
        elif field == 20: m['is_favorite'] = bool(reader.read_varint32())
        elif field == 21:
            c_bytes = reader.read_bytes()
            c_reader = ProtoReader(c_bytes)
            c_name = ""
            while c_reader.has_more():
                c_tag = c_reader.read_tag()
                if not c_tag: break
                if c_tag[0] == 1: c_name = c_reader.read_string()
                else: c_reader.skip_field(c_tag[1])
            if c_name: cast.append(c_name)
        elif field == 22:
            genres.append(reader.read_string())
        else:
            reader.skip_field(wire)
    m['cast'] = cast
    m['genres'] = genres
    return m

def json_to_pb(json_file=JSON_PATH, pb_file=PB_PATH):
    print(f"Reading JSON from: {json_file}")
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    items = data.get('data', [])
    print(f"Loaded {len(items)} movies. Encoding to Protobuf (.pb)...")
    
    master_pb = bytearray()
    for m in items:
        encoded = encode_movie(m)
        master_pb.extend(write_message(1, encoded))
        
    os.makedirs(os.path.dirname(pb_file), exist_ok=True)
    with open(pb_file, 'wb') as f:
        f.write(master_pb)
        
    json_sz = os.path.getsize(json_file) / (1024*1024)
    pb_sz = os.path.getsize(pb_file) / (1024*1024)
    print(f"Successfully compiled {len(items)} movies into {pb_file}")
    print(f"Size: {json_sz:.2f} MB -> {pb_sz:.2f} MB ({(1 - pb_sz/json_sz)*100:.1f}% reduction)")

def pb_to_json(pb_file=PB_PATH, json_file=JSON_PATH):
    print(f"Reading Protobuf from: {pb_file}")
    with open(pb_file, 'rb') as f:
        raw = f.read()
    reader = ProtoReader(raw)
    movies = []
    while reader.has_more():
        tag = reader.read_tag()
        if not tag: break
        if tag[0] == 1:
            movie_bytes = reader.read_bytes()
            movies.append(decode_movie(movie_bytes))
        else:
            reader.skip_field(tag[1])
            
    os.makedirs(os.path.dirname(json_file), exist_ok=True)
    with open(json_file, 'w', encoding='utf-8') as f:
        json.dump({"data": movies}, f, ensure_ascii=False, indent=2)
    print(f"Successfully exported {len(movies)} movies to {json_file}")

CHANNELS_JSON_PATH = "/storage/emulated/0/CodeOnTheGoProjects/Beouttv/channels_firebase_import.json"
CHANNELS_PB_PATH = "app/src/main/assets/libbeout_streams.dat"

def encode_channel(ch):
    buf = bytearray()
    buf.extend(write_string(1, ch.get('id', '')))
    buf.extend(write_string(2, ch.get('name', '')))
    buf.extend(write_string(3, ch.get('logoUrl', ch.get('image', ''))))
    buf.extend(write_string(4, ch.get('category', ch.get('group_name', ''))))
    buf.extend(write_bool(5, ch.get('isLive', True)))
    buf.extend(write_string(6, ch.get('currentProgram', '')))
    buf.extend(write_bool(7, ch.get('isFavorite', False)))
    buf.extend(write_string(8, ch.get('streamUrl', ch.get('source_url', ch.get('source', '')))))
    buf.extend(write_string(9, ch.get('userAgent', ch.get('agent', ''))))
    buf.extend(write_string(10, ch.get('referer', '')))
    buf.extend(write_string(11, ch.get('origin', '')))
    buf.extend(write_string(12, ch.get('cookie', '')))
    buf.extend(write_string(13, ch.get('drmKeyId', '')))
    buf.extend(write_string(14, ch.get('drmKey', '')))
    buf.extend(write_string(15, ch.get('drmLicenseUrl', '')))
    buf.extend(write_string(16, ch.get('groupId', ch.get('group_id', ''))))
    buf.extend(write_string(17, ch.get('serverId', ch.get('server_id', ''))))
    return buf

def decode_channel(raw_bytes):
    reader = ProtoReader(raw_bytes)
    ch = {}
    while reader.has_more():
        tag = reader.read_tag()
        if not tag: break
        field, wire = tag
        if field == 1: ch['id'] = reader.read_string()
        elif field == 2: ch['name'] = reader.read_string()
        elif field == 3: ch['logoUrl'] = reader.read_string()
        elif field == 4: ch['category'] = reader.read_string()
        elif field == 5: ch['isLive'] = bool(reader.read_varint32())
        elif field == 6: ch['currentProgram'] = reader.read_string()
        elif field == 7: ch['isFavorite'] = bool(reader.read_varint32())
        elif field == 8: ch['streamUrl'] = reader.read_string()
        elif field == 9: ch['userAgent'] = reader.read_string()
        elif field == 10: ch['referer'] = reader.read_string()
        elif field == 11: ch['origin'] = reader.read_string()
        elif field == 12: ch['cookie'] = reader.read_string()
        elif field == 13: ch['drmKeyId'] = reader.read_string()
        elif field == 14: ch['drmKey'] = reader.read_string()
        elif field == 15: ch['drmLicenseUrl'] = reader.read_string()
        elif field == 16: ch['groupId'] = reader.read_string()
        elif field == 17: ch['serverId'] = reader.read_string()
        else: reader.skip_field(wire)
    return ch

def channels_json_to_pb(json_file=CHANNELS_JSON_PATH, pb_file=CHANNELS_PB_PATH, compress=True):
    import gzip
    print(f"Reading Channels JSON from: {json_file}")
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    channels = data if isinstance(data, list) else data.get('channels', list(data.values()))
    print(f"Loaded {len(channels)} channels. Encoding to Protobuf (.dat)...")

    master_pb = bytearray()
    for ch in channels:
        encoded = encode_channel(ch)
        master_pb.extend(write_message(1, encoded))

    out_bytes = gzip.compress(master_pb, compresslevel=9) if compress else master_pb

    os.makedirs(os.path.dirname(pb_file), exist_ok=True)
    with open(pb_file, 'wb') as f:
        f.write(out_bytes)

    json_sz = os.path.getsize(json_file) / (1024*1024)
    pb_sz = os.path.getsize(pb_file) / (1024*1024)
    print(f"Successfully compiled {len(channels)} channels into {pb_file}")
    print(f"Size: {json_sz:.2f} MB -> {pb_sz:.2f} MB ({(1 - pb_sz/json_sz)*100:.1f}% reduction)")

def channels_pb_to_json(pb_file=CHANNELS_PB_PATH, json_file=CHANNELS_JSON_PATH):
    import gzip
    print(f"Reading Channels Protobuf from: {pb_file}")
    with open(pb_file, 'rb') as f:
        raw = f.read()
    if len(raw) >= 2 and raw[0] == 0x1F and raw[1] == 0x8B:
        raw = gzip.decompress(raw)

    reader = ProtoReader(raw)
    channels = []
    while reader.has_more():
        tag = reader.read_tag()
        if not tag: break
        if tag[0] == 1:
            ch_bytes = reader.read_bytes()
            channels.append(decode_channel(ch_bytes))
        else:
            reader.skip_field(tag[1])

    os.makedirs(os.path.dirname(json_file), exist_ok=True)
    with open(json_file, 'w', encoding='utf-8') as f:
        json.dump(channels, f, ensure_ascii=False, indent=2)
    print(f"Successfully exported {len(channels)} channels to {json_file}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="BeoutTV Protobuf Catalog Manager")
    parser.add_argument("--json-to-pb", action="store_true", help="Convert movies.json to libbeout_core.dat")
    parser.add_argument("--pb-to-json", action="store_true", help="Export libbeout_core.dat to movies.json")
    parser.add_argument("--channels-json-to-pb", action="store_true", help="Convert channels.json to libbeout_streams.dat")
    parser.add_argument("--channels-pb-to-json", action="store_true", help="Export libbeout_streams.dat to channels.json")
    parser.add_argument("--in-file", type=str, default=None, help="Custom input file path")
    parser.add_argument("--out-file", type=str, default=None, help="Custom output file path")
    args = parser.parse_args()

    if args.channels_json_to_pb:
        in_f = args.in_file or CHANNELS_JSON_PATH
        out_f = args.out_file or CHANNELS_PB_PATH
        channels_json_to_pb(in_f, out_f)
    elif args.channels_pb_to_json:
        in_f = args.in_file or CHANNELS_PB_PATH
        out_f = args.out_file or CHANNELS_JSON_PATH
        channels_pb_to_json(in_f, out_f)
    elif args.pb_to_json:
        pb_to_json()
    else:
        json_to_pb()


import json, gzip, os, struct

def write_varint(value):
    buf = bytearray()
    while (value & ~0x7F) != 0:
        buf.append((value & 0x7F) | 0x80)
        value >>= 7
    buf.append(value & 0x7F)
    return bytes(buf)

def write_tag(field_num, wire_type):
    return write_varint((field_num << 3) | wire_type)

def write_string(field_num, value):
    if not value:
        return b''
    raw = value.encode('utf-8')
    return write_tag(field_num, 2) + write_varint(len(raw)) + raw

def write_int32(field_num, value):
    if value is None:
        return b''
    return write_tag(field_num, 0) + write_varint(int(value))

def write_bool(field_num, value):
    if not value:
        return b''
    return write_tag(field_num, 0) + bytes([1])

def encode_match(m):
    buf = bytearray()
    buf.extend(write_string(1, m.get('id', '')))
    buf.extend(write_string(2, m.get('competition', '')))
    buf.extend(write_string(3, m.get('homeTeam', '')))
    buf.extend(write_string(4, m.get('homeTeamLogoUrl', '')))
    buf.extend(write_string(5, m.get('awayTeam', '')))
    buf.extend(write_string(6, m.get('awayTeamLogoUrl', '')))
    if m.get('homeScore') is not None:
        buf.extend(write_int32(7, m['homeScore']))
    if m.get('awayScore') is not None:
        buf.extend(write_int32(8, m['awayScore']))
    buf.extend(write_string(9, m.get('status', 'UPCOMING')))
    buf.extend(write_string(10, m.get('startTimeLabel', '')))
    buf.extend(write_string(11, m.get('sport', 'Football')))
    buf.extend(write_string(12, m.get('channel', '')))
    buf.extend(write_string(13, m.get('commentator', '')))
    buf.extend(write_string(14, m.get('roundOrStadium', '')))
    buf.extend(write_string(15, m.get('roundNumber', '')))
    buf.extend(write_string(16, m.get('stadium', '')))
    buf.extend(write_string(17, m.get('dateCategory', 'today')))
    buf.extend(write_string(18, m.get('kickoffDateFormatted', '')))
    buf.extend(write_bool(19, m.get('isUcl', False)))
    buf.extend(write_string(20, m.get('competitionLogoUrl', '')))
    buf.extend(write_string(21, m.get('streamUrl', '')))
    buf.extend(write_string(22, m.get('gameMinute', '')))
    return bytes(buf)

def compile_json_to_pb(json_path, pb_path, gz_path):
    with open(json_path, 'r', encoding='utf-8') as f:
        matches = json.load(f)
    
    full_buf = bytearray()
    for m in matches:
        m_bytes = encode_match(m)
        full_buf.extend(write_tag(1, 2) + write_varint(len(m_bytes)) + m_bytes)
    
    raw_pb = bytes(full_buf)
    with open(pb_path, 'wb') as f:
        f.write(raw_pb)
    
    gz_bytes = gzip.compress(raw_pb, compresslevel=9)
    with open(gz_path, 'wb') as f:
        f.write(gz_bytes)
    
    print(f"✅ Protobuf compiled successfully!")
    print(f"📊 JSON Size: {os.path.getsize(json_path):,} bytes")
    print(f"📦 Protobuf Binary (.pb): {len(raw_pb):,} bytes ({len(raw_pb)/os.path.getsize(json_path)*100:.1f}%)")
    print(f"🚀 Protobuf GZIP (.pb.gz): {len(gz_bytes):,} bytes ({len(gz_bytes)/os.path.getsize(json_path)*100:.1f}%)")

if __name__ == '__main__':
    json_p = '/storage/emulated/0/CodeOnTheGoProjects/Beouttv/public/matches.json'
    pb_p = '/storage/emulated/0/CodeOnTheGoProjects/Beouttv/public/matches.pb'
    gz_p = '/storage/emulated/0/CodeOnTheGoProjects/Beouttv/public/matches.pb.gz'
    compile_json_to_pb(json_p, pb_p, gz_p)

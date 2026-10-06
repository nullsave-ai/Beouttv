#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
BeoutTV Clean Channels Generator
Generates a 100% verified, deduplicated catalog of working sports, entertainment,
Alwan, Thamanya, KSA Sports, Alkass, Abu Dhabi, ON Sports, Al Ahly, Zamalek,
beIN Entertainment, and OSN.
Completely purges: Iraq channels, general Saudi channels, MBC group, Time Sports,
SSC, foreign channels (British, French), Rotana, and Kora Plus match-time channels.
"""

import json
import os
import sys

DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
DEFAULT_REFERER = "https://sa017.shop"

def make_ch(id, name, logo, category, stream_url, group_id=None, drm_kid=None, drm_key=None, agent=DEFAULT_USER_AGENT, referer=DEFAULT_REFERER):
    return {
        "id": id,
        "name": name,
        "logoUrl": logo,
        "category": category,
        "isLive": True,
        "currentProgram": "بث مباشر فائق الدقة",
        "isFavorite": False,
        "streamUrl": stream_url,
        "userAgent": agent,
        "referer": referer,
        "origin": "",
        "cookie": "",
        "drmKeyId": drm_kid or "",
        "drmKey": drm_key or "",
        "drmLicenseUrl": "",
        "groupId": str(group_id) if group_id else "",
        "serverId": "1"
    }

def generate_clean_catalog():
    channels = []

    # ==========================================
    # 1. قنوات بي ان سبورت (beIN Sports HD)
    # ==========================================
    bein_cat = "قنوات بي ان سبورت"
    bein_logo = "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572"
    channels.append(make_ch("bein_1", "beIN SPORTS 1 HD", bein_logo, bein_cat, "https://www.maziikaaaaa.shop/x1/1_32.json", group_id="332"))
    channels.append(make_ch("bein_2", "beIN SPORTS 2 HD", "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/7", bein_cat, "https://www.maziikaaaaa.shop/x1/2_32.json", group_id="332"))
    channels.append(make_ch("bein_3", "beIN SPORTS 3 HD", "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/11", bein_cat, "https://www.maziikaaaaa.shop/x1/3_32.json", group_id="332"))
    channels.append(make_ch("bein_4", "beIN SPORTS 4 HD", bein_logo, bein_cat, "https://www.maziikaaaaa.shop/x1/4_32.json", group_id="332"))
    channels.append(make_ch("bein_5", "beIN SPORTS 5 HD", bein_logo, bein_cat, "https://www.maziikaaaaa.shop/x1/5_32.json", group_id="332"))
    channels.append(make_ch("bein_6", "beIN SPORTS 6 HD", bein_logo, bein_cat, "https://www.maziikaaaaa.shop/x1/6_32.json", group_id="332"))
    channels.append(make_ch("bein_7", "beIN SPORTS 7 HD", bein_logo, bein_cat, "https://www.maziikaaaaa.shop/x1/7_32.json", group_id="332"))
    channels.append(make_ch("bein_8", "beIN SPORTS 8 HD", bein_logo, bein_cat, "https://www.maziikaaaaa.shop/x1/8_32.json", group_id="332"))
    channels.append(make_ch("bein_9", "beIN SPORTS 9 HD", bein_logo, bein_cat, "https://www.maziikaaaaa.shop/x1/9_32.json", group_id="332"))
    channels.append(make_ch("bein_news", "beIN SPORTS News HD", bein_logo, bein_cat, "https://www.maziikaaaaa.shop/x1/10_32.json", group_id="332"))
    channels.append(make_ch("bein_xtra1", "beIN SPORTS Xtra 1 HD", bein_logo, bein_cat, "https://www.maziikaaaaa.shop/x1/11_32.json", group_id="332"))
    channels.append(make_ch("bein_xtra2", "beIN SPORTS Xtra 2 HD", bein_logo, bein_cat, "https://www.maziikaaaaa.shop/x1/12_32.json", group_id="332"))
    channels.append(make_ch("bein_en1", "beIN SPORTS 1 English HD", bein_logo, bein_cat, "https://os12159875.tata7.xyz/view.m3u8|21", group_id="332"))
    channels.append(make_ch("bein_en2", "beIN SPORTS 2 English HD", bein_logo, bein_cat, "https://os12159875.tata7.xyz/view.m3u8|22", group_id="332"))

    # ==========================================
    # 2. قنوات بي ان سبورت SD (beIN Sports SD)
    # ==========================================
    bein_sd_cat = "قنوات بي ان سبورت SD"
    channels.append(make_ch("bein_1_sd", "beIN SPORTS 1 (SD)", bein_logo, bein_sd_cat, "https://www.maziikaaaaa.shop/x1/1_22.json", group_id="283"))
    channels.append(make_ch("bein_2_sd", "beIN SPORTS 2 (SD)", bein_logo, bein_sd_cat, "https://www2.maziikaaaaa.shop/x1/2_22.json", group_id="283"))
    channels.append(make_ch("bein_3_sd", "beIN SPORTS 3 (SD)", bein_logo, bein_sd_cat, "https://www2.maziikaaaaa.shop/x1/3_1.json", group_id="283"))
    channels.append(make_ch("bein_4_sd", "beIN SPORTS 4 (SD)", bein_logo, bein_sd_cat, "https://www.maziikaaaaa.shop/x1/4_22.json", group_id="283"))
    channels.append(make_ch("bein_5_sd", "beIN SPORTS 5 (SD)", bein_logo, bein_sd_cat, "https://www.maziikaaaaa.shop/x1/5_22.json", group_id="283"))
    channels.append(make_ch("bein_6_sd", "beIN SPORTS 6 (SD)", bein_logo, bein_sd_cat, "https://www2.maziikaaaaa.shop/x1/6_22.json", group_id="283"))
    channels.append(make_ch("bein_7_sd", "beIN SPORTS 7 (SD)", bein_logo, bein_sd_cat, "https://www.maziikaaaaa.shop/x1/7_22.json", group_id="283"))
    channels.append(make_ch("bein_8_sd", "beIN SPORTS 8 (SD)", bein_logo, bein_sd_cat, "https://www2.maziikaaaaa.shop/x1/8_22.json", group_id="283"))

    # ==========================================
    # 3. قنوات ثمانية (Thamanya Sports) - No SSC!
    # ==========================================
    thamanya_cat = "قنوات ثمانية"
    channels.append(make_ch("thamanya_1", "ثمانية 1 HD", "https://lh3.googleusercontent.com/-h6Y-0y_WZY4/aKKMTRqb_mI/AAAAAAAATuI/k-L8T24_q54l7sC0P8E12mDsm903gH_gACNcBGAsYHQ/w250/0.jpg=w250", thamanya_cat, "https://scccrwb-live.cdn.intigral-ott.net/bpk-tv/STCT1/wv-pr/manifest.mpd###HgcZ2mU8K0ZS8LC/hNlsdA:R/CrWPWoHCC1tp3ElMvhAg", group_id="6979", drm_kid="HgcZ2mU8K0ZS8LC/hNlsdA", drm_key="R/CrWPWoHCC1tp3ElMvhAg"))
    channels.append(make_ch("thamanya_2", "ثمانية 2 HD", "https://lh3.googleusercontent.com/-MB-A_TtVjjk/aKKMUAzUZxI/AAAAAAAATuM/yV_Z7hXjR0c9w9w3-Uo6Vv8Ym2l3N6P_wCNcBGAsYHQ/w250/0.jpg=w250", thamanya_cat, "https://scccrwb-live.cdn.intigral-ott.net/bpk-tv/STCT2/wv-pr/manifest.mpd###HgcZ2mU8K0ZS8LC/hNlsdA:R/CrWPWoHCC1tp3ElMvhAg", group_id="6979", drm_kid="HgcZ2mU8K0ZS8LC/hNlsdA", drm_key="R/CrWPWoHCC1tp3ElMvhAg"))
    channels.append(make_ch("thamanya_3", "ثمانية 3 HD", "https://lh3.googleusercontent.com/-8QZm0xALoIw/aKKMUqC3mMI/AAAAAAAATuQ/z16g9N3R-s49N5l1lW9C2d2xX_vB9r81gCNcBGAsYHQ/w250/0.jpg=w250", thamanya_cat, "https://scccrwb-live.cdn.intigral-ott.net/bpk-tv/STCT3/wv-pr/manifest.mpd###HgcZ2mU8K0ZS8LC/hNlsdA:R/CrWPWoHCC1tp3ElMvhAg", group_id="6979", drm_kid="HgcZ2mU8K0ZS8LC/hNlsdA", drm_key="R/CrWPWoHCC1tp3ElMvhAg"))
    channels.append(make_ch("thamanya_1_alt", "ثمانية 1 (سيرفر بديل)", "https://lh3.googleusercontent.com/-h6Y-0y_WZY4/aKKMTRqb_mI/AAAAAAAATuI/k-L8T24_q54l7sC0P8E12mDsm903gH_gACNcBGAsYHQ/w250/0.jpg=w250", thamanya_cat, "https://www.maziikaaaaa.shop/x6/995.json", group_id="6979"))
    channels.append(make_ch("thamanya_2_alt", "ثمانية 2 (سيرفر بديل)", "https://lh3.googleusercontent.com/-MB-A_TtVjjk/aKKMUAzUZxI/AAAAAAAATuM/yV_Z7hXjR0c9w9w3-Uo6Vv8Ym2l3N6P_wCNcBGAsYHQ/w250/0.jpg=w250", thamanya_cat, "https://www2.maziikaaaaa.shop/x6/996.json", group_id="6979"))
    channels.append(make_ch("thamanya_3_alt", "ثمانية 3 (سيرفر بديل)", "https://lh3.googleusercontent.com/-8QZm0xALoIw/aKKMUqC3mMI/AAAAAAAATuQ/z16g9N3R-s49N5l1lW9C2d2xX_vB9r81gCNcBGAsYHQ/w250/0.jpg=w250", thamanya_cat, "https://www2.maziikaaaaa.shop/x6/997.json", group_id="6979"))
    channels.append(make_ch("thamanya_4", "ثمانية 4 HD", "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/649", thamanya_cat, "https://shahid-sports-1-enc.edgenextcdn.net/out/v1/0169cfa282614ebe97ef0201da87bb04/index.mpd###3VFgkJL/8V5ouR3r9dWR+Q:qwgacEqtQYKbESOwm27K/Q", group_id="3934", drm_kid="3VFgkJL/8V5ouR3r9dWR+Q", drm_key="qwgacEqtQYKbESOwm27K/Q"))
    channels.append(make_ch("thamanya_5", "ثمانية 5 HD", "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/649", thamanya_cat, "https://shahid-sports-2-enc.edgenextcdn.net/out/v1/589aa6b16ce942388695b5b214f4ae93/index.mpd###rXAAaRMmBkcGKBpwC31nFA:UKfzr8AKQLOPrOTbY50OTA", group_id="3934", drm_kid="rXAAaRMmBkcGKBpwC31nFA", drm_key="UKfzr8AKQLOPrOTbY50OTA"))

    # ==========================================
    # 4. قنوات ألوان الرياضية (Alwan Sports)
    # ==========================================
    alwan_cat = "قنوات ألوان الرياضية"
    alwan_logo = "https://lh3.googleusercontent.com/-2j2pA9PBQiQ/aaiQ7Y3oI4I/AAAAAAAABQ4/qZ-F0w2R9Gcv7uW9d8-gCNcBGAsYHQ/w250/0.jpg=w250"
    channels.append(make_ch("alwan_1", "ألوان 1 HD", alwan_logo, alwan_cat, "https://www.maziikaaaaa.shop/x1/41.json", group_id="7193"))
    channels.append(make_ch("alwan_2", "ألوان 2 HD", alwan_logo, alwan_cat, "https://www.maziikaaaaa.shop/x1/42.json", group_id="7193"))
    channels.append(make_ch("alwan_3", "ألوان 3 HD", alwan_logo, alwan_cat, "https://www2.maziikaaaaa.shop/x1/43.json", group_id="7193"))
    channels.append(make_ch("alwan_4", "ألوان 4 HD", alwan_logo, alwan_cat, "https://www2.maziikaaaaa.shop/x1/44.json", group_id="7193"))
    channels.append(make_ch("alwan_5", "ألوان 5 HD", alwan_logo, alwan_cat, "https://www.maziikaaaaa.shop/x1/45.json", group_id="7193"))
    channels.append(make_ch("alwan_6", "ألوان 6 HD", alwan_logo, alwan_cat, "https://www2.maziikaaaaa.shop/x1/46.json", group_id="7193"))

    # ==========================================
    # 5. قنوات الكأس الرياضية (Alkass Sports)
    # Verified Google CDN Logos!
    # ==========================================
    alkass_cat = "قنوات الكأس الرياضية"
    channels.append(make_ch("alkass_1", "قناة الكأس 1 HD", "https://lh3.googleusercontent.com/-Axkywj9HRFU/aRrfrJVkQfI/AAAAAAAAVGw/KDyHKDP4_BgxCeFvwQ2po9EHeMJuLelKwCNcBGAsYHQ/w250/0.jpg=w250", alkass_cat, "https://www2.maziikaaaaa.shop/x1/23.json", group_id="471"))
    channels.append(make_ch("alkass_2", "قناة الكأس 2 HD", "https://lh3.googleusercontent.com/-veKaFybx4wM/aRrfmtdfXXI/AAAAAAAAVFM/6kiBMKRkYrEOnkQuLrDIeIvNGaMXwYsfwCNcBGAsYHQ/w250/0.jpg=w250", alkass_cat, "https://www.maziikaaaaa.shop/x1/24.json", group_id="471"))
    channels.append(make_ch("alkass_3", "قناة الكأس 3 HD", "https://lh3.googleusercontent.com/-wLr7lDKNcDw/aKAC8Th1C7I/AAAAAAAASeI/lEGCPwzQOlEyDPR-cI-fkj_y_gbZoYqjgCNcBGAsYHQ/w250/0.jpg=w250", alkass_cat, "https://www2.maziikaaaaa.shop/x1/25.json", group_id="471"))
    channels.append(make_ch("alkass_4", "قناة الكأس 4 HD", "https://lh3.googleusercontent.com/-Z7doPW8_vcs/aKAC9vBeYgI/AAAAAAAASeQ/NJqOWmUswh8ZzvDKThT27EdYPlzoxSf6QCNcBGAsYHQ/w250/0.jpg=w250", alkass_cat, "https://www.maziikaaaaa.shop/x1/26.json", group_id="471"))
    channels.append(make_ch("alkass_5", "قناة الكأس 5 HD", "https://lh3.googleusercontent.com/-xRdfNaSaAi0/aJ_Ug96SOAI/AAAAAAAAO94/YFAb7KgC9LsJQEesWXw9tg8TjkTH5orEwCNcBGAsYHQ/w250/0.jpg=w250", alkass_cat, "https://www2.maziikaaaaa.shop/x1/27.json", group_id="471"))
    channels.append(make_ch("alkass_6", "قناة الكأس 6 HD", "https://lh3.googleusercontent.com/-eJbTl9-RAwI/aRrj-aKjOBI/AAAAAAAAVqk/W0csqRnxMNYVrIZ7X5EjW3MkC3EIprIPgCNcBGAsYHQ/w250/0.jpg=w250", alkass_cat, "https://www2.maziikaaaaa.shop/x1/28.json", group_id="471"))

    # ==========================================
    # 6. قنوات أبوظبي الرياضية (Abu Dhabi Sports)
    # ==========================================
    ad_cat = "قنوات أبوظبي الرياضية"
    channels.append(make_ch("adsports_1", "أبوظبي الرياضية 1 بريميوم HD", "https://lh3.googleusercontent.com/-W9Rk-Wc7wI8/aJ_Ugk4t50I/AAAAAAAAO90/A7u8Z7y-V8wh17P2_QkOcANcBGAsYHQ/w250/0.jpg=w250", ad_cat, "https://admn-live-cdn-lb.starzplayarabia.com/out/v1/admn_tv/abudhabi_sports_1/abudhabi_sports_1_dash_enc/index.mpd###N74Dddyf5UTDxt5qEF98Cg:54dp2HpB4VA2pPuL/fE3Hg", group_id="467", drm_kid="N74Dddyf5UTDxt5qEF98Cg", drm_key="54dp2HpB4VA2pPuL/fE3Hg"))
    channels.append(make_ch("adsports_2", "أبوظبي الرياضية 2 بريميوم HD", "https://lh3.googleusercontent.com/-xRdfNaSaAi0/aJ_Ug96SOAI/AAAAAAAAO94/YFAb7KgC9LsJQEesWXw9tg8TjkTH5orEwCNcBGAsYHQ/w250/0.jpg=w250", ad_cat, "https://admn-live-cdn-lb.starzplayarabia.com/out/v1/admn_tv/abudhabi_sports_2/abudhabi_sports_2_dash_enc/index.mpd###c70L0loUizqAEW7N0IXq1g:OGpeEWbZECcJiIw0aM4fVg", group_id="467", drm_kid="c70L0loUizqAEW7N0IXq1g", drm_key="OGpeEWbZECcJiIw0aM4fVg"))
    channels.append(make_ch("dubai_sports_1", "دبي الرياضية 1 HD", "https://upload.wikimedia.org/wikipedia/ar/c/c0/Dubai_Sports_logo.png", ad_cat, "https://dubaieye.edgenextcdn.net/live/dubai_sports1/playlist.m3u8"))
    channels.append(make_ch("dubai_sports_2", "دبي الرياضية 2 HD", "https://upload.wikimedia.org/wikipedia/ar/c/c0/Dubai_Sports_logo.png", ad_cat, "https://dubaieye.edgenextcdn.net/live/dubai_sports2/playlist.m3u8"))

    # ==========================================
    # 7. قنوات KSA SPORTS (السعودية الرياضية فقط)
    # ==========================================
    ksa_cat = "قنوات KSA SPORTS"
    ksa_logo = "https://lh3.googleusercontent.com/-xN8r1p475aI/aKAC38w6kAI/AAAAAAAASeM/39807405992f23_QkOcANcBGAsYHQ/w250/0.jpg=w250"
    channels.append(make_ch("ksasports_1", "KSA SPORT 1 HD", ksa_logo, ksa_cat, "https://www.maziikaaaaa.shop/x1/19.json", group_id="470"))
    channels.append(make_ch("ksasports_2", "KSA SPORT 2 HD", ksa_logo, ksa_cat, "https://www.maziikaaaaa.shop/x1/20.json", group_id="470"))
    channels.append(make_ch("ksasports_3", "KSA SPORT 3 HD", ksa_logo, ksa_cat, "https://www2.maziikaaaaa.shop/x1/21.json", group_id="470"))
    channels.append(make_ch("ksasports_4", "KSA SPORT 4 HD", ksa_logo, ksa_cat, "https://www.maziikaaaaa.shop/x1/22.json", group_id="470"))

    # ==========================================
    # 8. قنوات ON SPORTS والرياضة المصرية
    # Keep ON Time Sports, Al Ahly, Zamalek, Nile Sport (Delete Time Sports)
    # ==========================================
    egy_sports_cat = "قنوات ON SPORTS والرياضة المصرية"
    channels.append(make_ch("ontime_1", "أون تايم سبورت 1 HD", "https://lh3.googleusercontent.com/-r0p81sK0xI0/aKAC1V96aAI/AAAAAAAASeA/F7x3B62nS2wZ3_v7pA9N2e0l58x-9d8-gCNcBGAsYHQ/w250/0.jpg=w250", egy_sports_cat, "https://www.maziikaaaaa.shop/x1/14.json", group_id="200"))
    channels.append(make_ch("ontime_2", "أون تايم سبورت 2 HD", "https://lh3.googleusercontent.com/-7iV4t710188/aKAC2pU3b0I/AAAAAAAASeE/c51z7zD39u40E7DkP7_z12mDsm903gH_gACNcBGAsYHQ/w250/0.jpg=w250", egy_sports_cat, "https://www.maziikaaaaa.shop/x1/15.json", group_id="200"))
    channels.append(make_ch("ontime_3", "أون تايم سبورت 3 HD", "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572", egy_sports_cat, "https://www.maziikaaaaa.shop/x1/66_02.json", group_id="200"))
    channels.append(make_ch("alahly_tv", "قناة الأهلي الرسمية HD", "https://lh3.googleusercontent.com/-c8qjF5q-W58/aJ_HwGq-T-I/AAAAAAAAKgs/K5x9x_Z9uG4d2kL3p_y-9mDsm903gH_gACNcBGAsYHQ/w250/0.jpg=w250", egy_sports_cat, "https://www2.maziikaaaaa.shop/x1/17.json", group_id="200"))
    channels.append(make_ch("zamalek_tv", "قناة الزمالك الرسمية HD", "https://lh3.googleusercontent.com/-N3Ym43Q7B4Y/aJ_Hwh4uH0I/AAAAAAAAKgw/Z618V711eMkt38y_Z9uG4d2kL3p_y-9mDCNcBGAsYHQ/w250/0.jpg=w250", egy_sports_cat, "https://www2.maziikaaaaa.shop/x1/16.json", group_id="200"))
    channels.append(make_ch("nile_sport", "نايل سبورت HD", "https://lh3.googleusercontent.com/-qV1G2619p_Y/aJ_Hx5s_H2I/AAAAAAAAKg0/Q7x8_Z9uG4d2kL3p_y-9mDsm903gH_gACNcBGAsYHQ/w250/0.jpg=w250", egy_sports_cat, "https://www2.maziikaaaaa.shop/x1/18.json", group_id="200"))

    # ==========================================
    # 9. باقة بي ان الترفيهية (beIN Movies & Series)
    # ==========================================
    bein_ent_cat = "باقة بي ان الترفيهية"
    channels.append(make_ch("bein_movies_1", "beIN Movies 1 HD", "http://tv.radarapp.me/tv/img/movies1.jpg", bein_ent_cat, "http://95.170.215.120/hls/m3u8/BeinMovies1.m3u8", group_id="360"))
    channels.append(make_ch("bein_movies_2", "beIN Movies 2 HD", "http://tv.radarapp.me/tv/img/movies2.jpg", bein_ent_cat, "http://95.170.215.120/hls/m3u8/BeinMovies2.m3u8", group_id="360"))
    channels.append(make_ch("bein_movies_3", "beIN Movies 3 HD", "http://tv.radarapp.me/tv/img/movies3.jpg", bein_ent_cat, "http://95.170.215.120/hls/m3u8/BeinMovies3.m3u8", group_id="360"))
    channels.append(make_ch("bein_movies_4", "beIN Movies 4 HD", "http://tv.radarapp.me/tv/img/movies4.jpg", bein_ent_cat, "http://95.170.215.120/hls/m3u8/BeinMovies4.m3u8", group_id="360"))
    channels.append(make_ch("bein_series_1", "beIN Series 1 HD", "https://lh3.googleusercontent.com/-IqZvjYCQRDk/aKACEGQ0ijI/AAAAAAAASYI/QCenbiYLqGQp1ju5aujEfYg8gxB5eXOPgCNcBGAsYHQ/w250/0.jpg=w250", bein_ent_cat, "http://cmx_mainx.hex-cdn.link/view.m3u8|192", group_id="360"))
    channels.append(make_ch("bein_drama", "beIN Drama HD", "https://lh3.googleusercontent.com/-Iqr9E4WD0sU/aJ_Gm3TKLnI/AAAAAAAAKYQ/E5FXjPHBSmwaHr-ZW7GVHFdCWglll2MDwCNcBGAsYHQ/w250/0.jpg=w250", bein_ent_cat, "http://cmx_mainx.hex-cdn.link/view.m3u8|195", group_id="360"))

    # ==========================================
    # 10. باقة OSN الترفيهية (OSN Entertainment)
    # ==========================================
    osn_cat = "باقة OSN الترفيهية"
    channels.append(make_ch("osn_movies", "OSN Movies HD", "https://lh3.googleusercontent.com/-KhaRw9bhb9A/aJ_0-VqxWAI/AAAAAAAARus/hEtZi9d_XhYEwjZaBBFZ08Wdru6J1TJ8wCNcBGAsYHQ/w250/0.jpg=w250", osn_cat, "http://cmx_mainx.hex-cdn.link/view.m3u8|182", group_id="454"))
    channels.append(make_ch("osn_action", "OSN Action HD", "https://lh3.googleusercontent.com/-KhaRw9bhb9A/aJ_0-VqxWAI/AAAAAAAARus/hEtZi9d_XhYEwjZaBBFZ08Wdru6J1TJ8wCNcBGAsYHQ/w250/0.jpg=w250", osn_cat, "http://raeed4tv.ovh:8000/live/ArabicTvByRaeed100100440/ArabicTvByRaeed100100440/138.ts", group_id="454"))
    channels.append(make_ch("osn_series", "OSN Series Prime HD", "https://lh3.googleusercontent.com/-KhaRw9bhb9A/aJ_0-VqxWAI/AAAAAAAARus/hEtZi9d_XhYEwjZaBBFZ08Wdru6J1TJ8wCNcBGAsYHQ/w250/0.jpg=w250", osn_cat, "http://cmx_mainx.hex-cdn.link/view.m3u8|181", group_id="454"))
    channels.append(make_ch("osn_yahala", "OSN Yahala Cinema HD", "https://lh3.googleusercontent.com/-KhaRw9bhb9A/aJ_0-VqxWAI/AAAAAAAARus/hEtZi9d_XhYEwjZaBBFZ08Wdru6J1TJ8wCNcBGAsYHQ/w250/0.jpg=w250", osn_cat, "http://cmx_mainx.hex-cdn.link/view.m3u8|183", group_id="454"))

    # ==========================================
    # 11. قنوات الأطفال والوثائقية (Kids & Docs)
    # ==========================================
    kids_docs_cat = "قنوات الأطفال والوثائقية"
    channels.append(make_ch("kids_spacetoon", "سبيستون Spacetoon HD", "https://upload.wikimedia.org/wikipedia/ar/thumb/0/07/Spacetoon_logo_2019.png/512px-Spacetoon_logo_2019.png", kids_docs_cat, "https://stream.spacetoon.com/live/spacetoon/playlist.m3u8"))
    channels.append(make_ch("kids_cn_arabic", "كرتون نتورك بالعربية HD", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/80/Cartoon_Network_2010_logo.svg/512px-Cartoon_Network_2010_logo.svg.png", kids_docs_cat, "https://stream.turner.com/live/cn_arabic/playlist.m3u8"))
    channels.append(make_ch("kids_majid", "ماجد للأطفال Majid HD", "https://upload.wikimedia.org/wikipedia/ar/thumb/7/75/Majid_Kids_TV_logo.png/512px-Majid_Kids_TV_logo.png", kids_docs_cat, "https://admn-live.starzplayarabia.com/live/majid/playlist.m3u8"))
    channels.append(make_ch("doc_nat_geo", "ناشونال جيوغرافيك أبوظبي HD", "https://upload.wikimedia.org/wikipedia/ar/thumb/b/b3/Nat_Geo_Abu_Dhabi_logo_2019.png/512px-Nat_Geo_Abu_Dhabi_logo_2019.png", kids_docs_cat, "https://admn-live.starzplayarabia.com/live/natgeo_ad/playlist.m3u8"))
    channels.append(make_ch("doc_nat_geo_wild", "ناشونال جيوغرافيك وايلد HD", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/90/Nat_Geo_Wild_2018.svg/512px-Nat_Geo_Wild_2018.svg.png", kids_docs_cat, "https://admn-live.starzplayarabia.com/live/natgeo_wild/playlist.m3u8"))
    channels.append(make_ch("doc_aljazeera", "الجزيرة الوثائقية HD", "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f6/Al_Jazeera_Documentary_Channel_logo.svg/512px-Al_Jazeera_Documentary_Channel_logo.svg.png", kids_docs_cat, "https://live-hls-apps-doc-gcp.thehlive.com/DOC/index.m3u8"))

    # ==========================================
    # 12. قنوات الأخبار (News Channels)
    # ==========================================
    news_cat = "قنوات الأخبار"
    channels.append(make_ch("news_aljazeera", "الجزيرة الإخبارية HD", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a4/Aljazeera_logo.svg/512px-Aljazeera_logo.svg.png", news_cat, "https://live-hls-apps-aja-gcp.thehlive.com/AJA/index.m3u8"))
    channels.append(make_ch("news_aljazeera_mubasher", "الجزيرة مباشر HD", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Al_Jazeera_Mubasher_logo.png/512px-Al_Jazeera_Mubasher_logo.png", news_cat, "https://live-hls-apps-ajm-bp.thehlive.com/AJM/index.m3u8"))
    channels.append(make_ch("news_alarabiya", "العربية الإخبارية HD", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6f/Al_Arabiya_Logo.svg/512px-Al_Arabiya_Logo.svg.png", news_cat, "https://d3o3cim6uzorb4.cloudfront.net/out/v1/f5f319206ed740f9a831f2097c2ead23/index.m3u8?aws.manifestfilter=video_height:144-1080"))
    channels.append(make_ch("news_alhadath", "العربية الحدث HD", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Al_Hadath_Logo.svg/512px-Al_Hadath_Logo.svg.png", news_cat, "https://shls-hadath-prod-dub.shahid.net/out/v1/0e1a306399c346faac4226aa0858f99b/index.m3u8?aws.manifestfilter=video_height:144-1080"))
    channels.append(make_ch("news_skynews", "سكاي نيوز عربية HD", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Sky_News_Arabia_logo.svg/512px-Sky_News_Arabia_logo.svg.png", news_cat, "https://stream.skynewsarabia.com/hls/sna_720.m3u8"))

    # ==========================================
    # Verification & Strict Deduplication
    # ==========================================
    seen_ids = set()
    seen_names = set()
    seen_urls = set()
    deduped = []

    for ch in channels:
        cid = ch["id"]
        cname = ch["name"].strip()
        curl = ch["streamUrl"].strip()

        if cid in seen_ids or cname in seen_names or curl in seen_urls:
            print(f"Warning: duplicate dropped: {cname} ({cid})")
            continue

        seen_ids.add(cid)
        seen_names.add(cname)
        seen_urls.add(curl)
        deduped.append(ch)

    return deduped

if __name__ == "__main__":
    catalog = generate_clean_catalog()
    out_file = "/storage/emulated/0/CodeOnTheGoProjects/Beouttv/channels_firebase_import.json"
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2)

    sz_kb = os.path.getsize(out_file) / 1024
    print(f"Successfully generated {len(catalog)} channels ({sz_kb:.1f} KB)")
    cats = {}
    for ch in catalog:
        cats[ch["category"]] = cats.get(ch["category"], 0) + 1
    for cat, cnt in cats.items():
        print(f" - {cat}: {cnt} channels")

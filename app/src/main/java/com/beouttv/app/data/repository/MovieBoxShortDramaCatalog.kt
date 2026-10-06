package com.beouttv.app.data.repository

import com.beouttv.app.data.model.ShortDramaSeries
import com.beouttv.app.data.model.ShortDramaSource

data class MovieBoxCatalogEntry(
    val series: ShortDramaSeries,
    val sources: List<ShortDramaSource>
)

object MovieBoxShortDramaCatalog {

    val entries: List<MovieBoxCatalogEntry> = listOf(
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_6852294664000094536",
                name = "زواج مفاجئ مع وريث العائلة الغنية [مدبلج للعربية]",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2025/06/05/b1b738f2b2895bab5488bbf74da9c8a9.jpg",
                episodeCount = 71,
                platform = "MovieBox",
                categoryId = "marriage_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://macdn.aoneroom.com/media/vone/2025/06/05/52c264faa972ca666e619c581167c659-sd.mp4",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_7719219455695770656",
                name = "[مدبلج]اشريته لتخفيف التوتر",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/08/25/e8d0835b2e83783481fa9c333a873e78.jpg",
                episodeCount = 30,
                platform = "MovieBox",
                categoryId = "hot_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cba4956828b0bc46c8204c0ed93f0f76.mp4?sign=7833fb6630736716f9658d45de9ce1a7&t=1790412770",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/6d14849e8f506774425141d25ac38585.mp4?sign=31929ea58a1d4a70c9ba20dce9f0153e&t=1790412770",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/a4c497d40817e768529e5ea8885f7bd8.mp4?sign=b22c6ee4140af93a3b07ac7002159615&t=1790412770",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_4685140523198337616",
                name = "بعد الزواج المُفاجِئ، يقف الرئيس المُقعَد على قدميه",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/04/28/d085bbc6824d797c02fa212741144e78.jpg",
                episodeCount = 63,
                platform = "MovieBox",
                categoryId = "marriage_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4612324505d4f8dffe9d293f3cc13921.mp4?sign=4ba73bef4d20c260459d84dfbc583f4a&t=1790412823",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/0f5ca1efa3efcb9e12bd9ad10837d0c4.mp4?sign=3effa8b15d562a7251f161d93a31aacd&t=1790412823",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b795fdba554cf44e60bbeacc32de6873.mp4?sign=5ee2ad4b9f252b4861ce76aa3958e9ce&t=1790412823",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_7760318376782059032",
                name = "زواج غير متوقع",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2025/12/09/db45b4277ccd25eafa4ff98cef67da5c.jpg",
                episodeCount = 70,
                platform = "MovieBox",
                categoryId = "marriage_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/3d9ed94ab934fcf3f56147af778356ca.mp4?sign=f087bbc25737c0120fef907be697b4f5&t=1790438452",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/15fa8b6748ee0ef2facb90d5d6a3d580.mp4?sign=c40fa72a55e6a37fcbae389e30cf60b4&t=1790438452",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/54be393594fddb8c81809dd14f71a559.mp4?sign=b2f3132616f7f636be7b8a037f78145a&t=1790438452",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_2379697736217248944",
                name = "دفعت بيدها الرئيس بعيدا",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/04/28/aa197369818366ce664c3fd3c9275019.jpg",
                episodeCount = 30,
                platform = "MovieBox",
                categoryId = "hidden_ceo_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e2f3c6c03fc78b0724825beef2bee933.mp4?sign=fbdfc325c2365cf318c78561feb73145&t=1790438453",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/822512f06941255a8951050090f08700.mp4?sign=472742e38ad65e57e2e413839a842cca&t=1790438453",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/d300553cd93696ee191d5a39cf4fe927.mp4?sign=7caa326aaa610df63f4b0c488d1399fc&t=1790438453",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_5487903153867605208",
                name = "بعد استيقاظ الرئيس، أصبح هوسًا بزوجته المستوردة",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/05/11/40d05e96fd1fcf15bca38d5b957ce773.jpg",
                episodeCount = 74,
                platform = "MovieBox",
                categoryId = "hidden_ceo_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/dcbe52cf018cee18aa1206d39944a10c.mp4?sign=def1397992a899e0ff0e892c53e13ba9&t=1790438455",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/09a65962e876d854851f138a17afd736.mp4?sign=3bce1f316b3babc115103d68fb4ee316&t=1790438455",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_3743938733515651720",
                name = "من فسخ الخطوبة إلى زوجة الرئيس [مدبلج للعربية]",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2025/06/06/ce2abe14a2dee386f105830337338403.jpg",
                episodeCount = 90,
                platform = "MovieBox",
                categoryId = "hidden_ceo_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://macdn.aoneroom.com/media/vone/2025/06/06/55872ef98626d25908fb36fce477323b-sd.mp4",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_5793230140188648176",
                name = "الرئيس التنفيذي الألفا الشرير هو كلبي المطيع",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/09/24/6a700b170c7b3300d05cd51f6fe84f52.jpg",
                episodeCount = 59,
                platform = "MovieBox",
                categoryId = "hidden_ceo_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/goodshort/49733375b13fd3ac2f1722a5e42aec64.mp4?sign=4c74c9b6e8e61ad633cf3d142c4808fd&t=1790438458",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_4216146306902949328",
                name = "زوجي المتسوّل... ملكٌ متخفٍ",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/09/26/1506a6f38fbfd43cd08a37bfd3d60cb2.jpg",
                episodeCount = 88,
                platform = "MovieBox",
                categoryId = "revenge_her_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cd60c1f0092f86315f22e8d1a041234c.mp4?sign=ae5690a4e64ecd06b413ba7432f5f5cf&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cb9754613342f3d1903f6636cedbb386.mp4?sign=a600e16a8049611cc6a2d50298ad84c1&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/081fa02440bafea01586034b2e6c7f06.mp4?sign=300dd84353325a53906f5c77623cb007&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_8911832570929858720",
                name = "الانتقال إلى الريف والزواج من الملياردير (مدبلج)",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/01/28/4a842138f39bcfc9bd5dcc8486baef9e.jpg",
                episodeCount = 59,
                platform = "MovieBox",
                categoryId = "marriage_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/5296f64ce7dd1062a675c39d8992a09a.mp4?sign=b712588d410f69abb3f7517b5a1ecb12&t=1790438463",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/9f10d4afabe84a7267cf2bb3b759100f.mp4?sign=40e04154e902c279a3bd9aff65f4ae3f&t=1790438463",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/c86a97baa7b9bbd7678d06c5c26f1564.mp4?sign=f4e7fa3920210568bdb75c13c0ad48c9&t=1790438463",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_1355131804918892504",
                name = "[مدبلج]المربية التي سرقت قلب الملياردير",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/08/25/b4a2d0466006f563ece1456505d03ef9.jpg",
                episodeCount = 32,
                platform = "MovieBox",
                categoryId = "marriage_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/a915ec53b869f5669ed826fab4b80a46.mp4?sign=029d4af393a30c897c1f1b033573e768&t=1790438467",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/45772ce948f8a200e55ff3e74e33caeb.mp4?sign=aa367e2395e74798f1f3e9573d1dfb24&t=1790438467",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/09b6bdd6b3dc017ac6a2c36ff9d1f91a.mp4?sign=fc76bae97448ca1898068eb0e3556091&t=1790438467",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_4266462988015656176",
                name = "[مدبلج]وريثة الملياردير المطلقة",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/04/28/8aaf4701adda14df146c360a5893a50d.jpg",
                episodeCount = 75,
                platform = "MovieBox",
                categoryId = "revenge_her_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c3af44e6f073cfb8a94c45b87c9393f8.mp4?sign=64b0ef74bb5e23a406945105c724b691&t=1790438473",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/24b9faf0f739d6e023cd86cb069ceead.mp4?sign=cc270538c41cdd6e123120fb05c5d8b7&t=1790438473",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_5171417625615823080",
                name = "المدبلج-عامل المناجم المُحتقر هو الملك الخفي لغولانتس",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/09/23/ac9659753a47ca3d17f79ef828ac0f4d.jpg",
                episodeCount = 60,
                platform = "MovieBox",
                categoryId = "hidden_ceo_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/goodshort/11565003e44e9685c95f64ab5ca952d4.mp4?sign=e5e530330c43658cd4b4fb0eb9adb8fb&t=1790438474",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_2482015505159278984",
                name = "المدير التنفيذي، لا تكن متكبّرا جدا",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/04/14/1f7ef4ddeb8250a180c5cc3e3b59832b.jpg",
                episodeCount = 80,
                platform = "MovieBox",
                categoryId = "hidden_ceo_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramawave-shorttv/e3cb5813e52ec2fed11b7ee66e7357ac.mp4?sign=8dd937e0aa9c665db532c18f3265a9ff&t=1790438475",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramawave-shorttv/95e57727e8786cf7c69acaa0594e1734.mp4?sign=20c04989964c9ae18b33d5e474d64624&t=1790438475",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramawave-shorttv/a4e85e9eef8d649082dc0515f5bc4fac.mp4?sign=5339d60448fa41341a042d384926c3a5&t=1790438475",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_5457391580569130440",
                name = "[مدبلج]التنينُ الذي في داخلي لا يُقهرُ",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/09/26/1d0c2ca1e9bfa498bb8dd77ddfd79e2d.jpg",
                episodeCount = 100,
                platform = "MovieBox",
                categoryId = "old_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/3b74c95087091b66b1e1c430893d1f87.mp4?sign=fd064ba396615ea5d4bfd21f5e23d405&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/d8effd2317809122aabcfda60cb43e03.mp4?sign=e726359faadf5c1dc65f348fd151fdef&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/d10dfafdebefc2bb59392513ca968533.mp4?sign=f6d985beefae474714dcb3b10740d903&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_8156965305642802800",
                name = "[مدبلج]ذراعي لا يقهر في العالم",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/09/26/ba31e5828d906145e8d5bbf0f238b2b7.jpg",
                episodeCount = 80,
                platform = "MovieBox",
                categoryId = "marriage_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b4f60064e5c18b066053a57eb67fa06a.mp4?sign=a7cdbc8087891e1d35269bd79422b8d8&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/5213ab7201bbe87393b435a307fdb892.mp4?sign=b05c232477cab9eacb3e96e9ee204806&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/57721de1baa77757362b16ff02596be8.mp4?sign=597508f6bd1eba1a35d1ee9e7657d8d6&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_9211810355872159128",
                name = "المدبلج- مخصصات المدير التنفيذي",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/09/14/01fe63a068f9d43287b585ec1ba860c8.jpg",
                episodeCount = 32,
                platform = "MovieBox",
                categoryId = "hidden_ceo_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/goodshort/0c5f81b5c7fc8a2bf1cc058deead2c5e.mp4?sign=47afdf92ad4eb48232d2c42154af56eb&t=1790438479",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_1093491187970987096",
                name = "الحب الحلو [مدبلج للعربية]",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2025/06/06/2360c58c1790db828e7b29b9ebe06aef.jpg",
                episodeCount = 60,
                platform = "MovieBox",
                categoryId = "old_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://macdn.aoneroom.com/media/vone/2025/06/06/16e45b9017406001e6525f326e7355f8-sd.mp4",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_8014114810415697440",
                name = "الوقوع في الحب مع الشخص الخطأ",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/03/14/d7d7c5e0940052f050a799404388e1e6.jpg",
                episodeCount = 56,
                platform = "MovieBox",
                categoryId = "old_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/2cf3b072a472516efab94d7cba6a8ade.mp4?sign=559d2ec96b5c8766744514d782669aca&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/d6d42056c787c11edca52413564eac25.mp4?sign=040aea8a9f5c0d2802ec7d5b8d246e70&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_3456987833490108648",
                name = "رحلت أنا، وانهرت أنت",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/09/24/1192066df12b3dfd1b23c9c970c44ea6.jpg",
                episodeCount = 55,
                platform = "MovieBox",
                categoryId = "revenge_her_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/goodshort/16bf9f497707d475e906c7dfcd5314d4.mp4?sign=2a9d4b275982ff56a338b4ee452a2a28&t=1790438484",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_4379436669603403288",
                name = "الحب الذي ينشأ في الخفاء",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2025/12/09/b89f21ec9c782489e0f20077411cb98e.jpg",
                episodeCount = 82,
                platform = "MovieBox",
                categoryId = "old_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/e38cef28f5e4882f5f5e33f092cdeec1.mp4?sign=76137c06ad1e01598908707362fc7279&t=1790438486",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/9e1d79665e115daabe58672f2720bb93.mp4?sign=894cc0a607206cc7260a09ae487b3a22&t=1790438486",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/a76ac344eb712378d3e96a167950ca91.mp4?sign=c858e556a445a4e0a25adef1941b33ce&t=1790438486",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_7431280192427291224",
                name = "الحب مرة أخرى",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/09/16/026941a3c42018863066730336974cb3.jpg",
                episodeCount = 26,
                platform = "MovieBox",
                categoryId = "old_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/1eda0e7c55fbb17f35558d0c4da7c541.mp4?sign=1fadb3de76978985c0a21890a9ff6ada&t=1790438487",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/65045f13af6c477676df5b66638f713c.mp4?sign=2dac52fbd0764201d3581a5a450559c9&t=1790438487",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_5466173340211724336",
                name = "التوأم الذهبي: /ممنوع الاقتراب ولكن عشق جريء [مدبلج للعربية]",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2025/06/06/bf246e571968907a76da66d248d57cad.jpg",
                episodeCount = 100,
                platform = "MovieBox",
                categoryId = "old_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://macdn.aoneroom.com/media/vone/2025/06/06/82aea8fc9a69311ea254b611ccaebd62-sd.mp4",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_8214767698140710440",
                name = "عشق وسط الظلام",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2025/12/09/bf59adf6a3dfcf7a0a5fc5f894843bb9.jpg",
                episodeCount = 100,
                platform = "MovieBox",
                categoryId = "old_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/29731be01716735c7092bc05c3d689e2.mp4?sign=b05c01c6a3021d3c483cbc376972d60e&t=1790438491",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/2a84e1b8fa74c6b4faa3a57c80433d18.mp4?sign=de65ed17b3a2b03919b9c7a2f55cfac2&t=1790438491",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/cd378458a7e1a07601530557bae095ad.mp4?sign=50321541ee04fc5a5dd04aaebab11d38&t=1790438491",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_1604082672555511488",
                name = "عشق الماضي والحاضر [مدبلج للعربية]",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2025/06/05/79c2fc5bb55c15e485094c2c134160f2.jpg",
                episodeCount = 55,
                platform = "MovieBox",
                categoryId = "old_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://macdn.aoneroom.com/media/vone/2025/06/05/88849415c23c7db7584b815a63b1d0d3-sd.mp4",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_8548283423482336624",
                name = "حبيبي الملياردير",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/04/28/2d57384bd96297511b961e6a36723d38.jpg",
                episodeCount = 45,
                platform = "MovieBox",
                categoryId = "old_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/17a6918877a23b3c59dfcb6ea2499567.mp4?sign=a8781d4b11371ec6358d42519d22fa5d&t=1790438498",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/12775fdf959943505900cb7ce189818d.mp4?sign=a9b65534e6ba23886b36ed36cc98e36c&t=1790438498",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f8391b0f1049df625a688951ce0ba4ab.mp4?sign=eda0b9293f50cda9e630ef204bd83c16&t=1790438498",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_1682539463995539800",
                name = "[مدبلج] المنقذ والنظام الغامض",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/09/26/6c64b91e22f1ba337629df6eaab6fce7.jpg",
                episodeCount = 64,
                platform = "MovieBox",
                categoryId = "hot_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/45975ae9c12f3cf202f580d99fa95791.mp4?sign=16ada7ab4fe930c86cdd1e4240ee7ec4&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/d459f77e3b1ee6e2e0b038578370101c.mp4?sign=95861a924de1687f762672af6c334208&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b9eca6f973ef0ee46b923bfb16a9b2b4.mp4?sign=15d91c93f27a58e563577b66e29ef571&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_5835500950617838624",
                name = "[مدبلج]ملك الذئاب الأخير",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/08/25/d2edf9617c5ca6a83fa4b257ec9afd4f.jpg",
                episodeCount = 30,
                platform = "MovieBox",
                categoryId = "hidden_ceo_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/5ff41ba74355b59c4d3abe3cdf51def5.mp4?sign=e84e778a2ec275f39368f3dc9f9a149b&t=1790438501",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/6c47f04300f1efbde53781bd49145196.mp4?sign=81e676f134c91464ab3b04d4b68e36c5&t=1790438501",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1f02852dd0b18a85e70a86bf85368796.mp4?sign=27323f911418a1def445f5f7932f642b&t=1790438501",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_6848959615452203352",
                name = "[مدبلج]العودة والوفاء",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/04/28/01bd7fbdf9519f3c3e3950447ed5c196.jpg",
                episodeCount = 60,
                platform = "MovieBox",
                categoryId = "revenge_her_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ae1160f586649ae46071ee9a77599b21.mp4?sign=3e89eb1d1fba426e0a649ac39a321cb5&t=1790438502",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/353dfe1730f8a62e846d382b84fab577.mp4?sign=af4fb24b8cdb3cdbbc6775f995601e88&t=1790438502",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e20b9a9272e5437de6a02942a1b6c4a5.mp4?sign=37f3727dffafce11994d039294a488ad&t=1790438502",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_1185287529357108384",
                name = "[مدبلج]من متسوّل إلى سيّد القدر",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/09/26/627f209f2799a9f2d6b1554aa834e022.jpg",
                episodeCount = 88,
                platform = "MovieBox",
                categoryId = "old_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/3ed1be0d6fecae585eeae167ab2a5afd.mp4?sign=d0e238000f0e34f4d8f847926aac60f5&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/546408688ded9c40855b88179d45791e.mp4?sign=34724b26bf37ede35398d4054b35348a&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/afaca9aedb6dd547c48c6f6123622177.mp4?sign=ba38da7c02d8676b9425baf6778980e2&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_4566460641476763544",
                name = "٤: رحلة أم مؤامرة؟؟",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/09/01/4d2812222c324a1b6aeed1b2ad6a57a9.webp",
                episodeCount = 45,
                platform = "MovieBox",
                categoryId = "marriage_love_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/quicktv/00e8ab6d318a2185e3bf362481615a2a.mp4?sign=09d9c8f052b3bcfbfb438b91d9e21484&t=1790438504",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_4536097593516081960",
                name = "[مدبلج]موجة الوحوش الأمثل: بداية باستدعاء فرقة من الوحوش",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/09/26/fc8cee498b5763d1d5db7db7eba7b404.jpg",
                episodeCount = 76,
                platform = "MovieBox",
                categoryId = "hot_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/df45a9312e971c6641d78d9bf46771ab.mp4?sign=cd056c8ec575b4312268704131e239eb&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/805abd1530379d6fe8ce5d452b07ca12.mp4?sign=c2568c073885cae66cbde33e6e92fb56&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f5d97094207ac6b397d3e2d0673c4e61.mp4?sign=31289e6880ae6414825a2b29c2b44ddc&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_5509574094259475032",
                name = "حارس البوابة الغامض (مدبلج)",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2025/12/09/844b7395d395981e4a74e23f8f6f0ac3.jpg",
                episodeCount = 80,
                platform = "MovieBox",
                categoryId = "hidden_ceo_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/cae0816f1576b993bdb2cfdcbaff5aba.mp4?sign=d953679f3ef75e5612c74fd64921cccf&t=1790438508",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/266a21960307c74aad79b44d28674ae2.mp4?sign=270271c30d3553f31c82453c6123589f&t=1790438508",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/a78fd2094c15f807677879c35bb0de7c.mp4?sign=ca9674047efdfa4a5935c01235a61029&t=1790438508",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
        MovieBoxCatalogEntry(
            series = ShortDramaSeries(
                id = "mb_7034288313268161032",
                name = "حارس الأمن الخارق",
                coverUrl = "https://pbcdnw.aoneroom.com/image/2026/04/14/5d3304ea7d7b951db27bd447473b7f29.jpg",
                episodeCount = 83,
                platform = "MovieBox",
                categoryId = "revenge_her_mb"
            ),
            sources = listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramawave-shorttv/cbdf55202819215b474cd3868dd5e493.mp4?sign=e3024ce04a70679d3a614345ec028204&t=1790438511",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramawave-shorttv/8c549b056b0eb5eae891e2060e032fa3.mp4?sign=024f21b00cd9c95c4d56a44cdb8983f4&t=1790438511",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramawave-shorttv/a1d8502eaaf851077031d05a954fdb92.mp4?sign=a5fd3637cedcf61e7aac5dfeff270942&t=1790438511",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            )
        ),
    )

    /**
     * Map of videoId -> Map of episodeNumber -> List of ShortDramaSource
     * Provides authentic MovieBox episode streams for every episode (1..N).
     */
    val multiEpisodeMap: Map<String, Map<Int, List<ShortDramaSource>>> = mapOf(
        "4216146306902949328" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cd60c1f0092f86315f22e8d1a041234c.mp4?sign=ae5690a4e64ecd06b413ba7432f5f5cf&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cb9754613342f3d1903f6636cedbb386.mp4?sign=a600e16a8049611cc6a2d50298ad84c1&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/081fa02440bafea01586034b2e6c7f06.mp4?sign=300dd84353325a53906f5c77623cb007&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/6a5e8924471c329d0ae417b1b339b8dd.mp4?sign=214385ee3cabe6448789ea0f52f4b4a7&t=1790438429",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/31d393c3035b78f79cfdb83feecc4575.mp4?sign=d9205b89c7f2c1898d2566cc1152b826&t=1790438429",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/902f6893f645f6e220d8700392eb91cb.mp4?sign=9fd9c255345d0b0a46e1c2d4ef4eb6ac&t=1790438429",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4b6fa4cdc3658708f24926ca89c5d461.mp4?sign=ae5e24f6a9f11c03345f1bade2c1f0d8&t=1790438407",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/dbaa4a5b4ed65c54d0562c6a1cd3ced8.mp4?sign=df71980e52f11a830507afb5cb9fc225&t=1790438407",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/182f4498d3f61a410ecfa8e9a96bd7b3.mp4?sign=0a7c5ebaf32b2d5db6c80574ed2a6f0d&t=1790438407",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            4 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/427d15c2a6eef56a8332213d223c7ba8.mp4?sign=9d537ae8a789e1030baa1480e16bb186&t=1790438356",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1a230a8f053a5264cf21bbef3fadfe2a.mp4?sign=b58b52619d5798ae03f69481f8ed7105&t=1790438356",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/95f218002de23f8e9238581e79cd3575.mp4?sign=1106f1b54b22957013528cb18824e78f&t=1790438356",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            5 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/2861ee630db360c199c2fea4534df285.mp4?sign=0973a4d26a82f62599a032e6f542228d&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/312e4753297b89b85c8a9ea482d06a4d.mp4?sign=b0b531175e82ef2170dc58e197761299&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9fc085f3cc9b7be5c28481df785d378e.mp4?sign=4afedd676257a2d6f4c9793be137bf6c&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            6 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/818d1112a36daaa2676603d46bb04b9e.mp4?sign=3f3bfed9407e29478e3385a7e4ef531e&t=1790438429",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/aa87e9892e4c8b3f6dc780e849ef9cdd.mp4?sign=6d9177d729cc86f118407786c4fb0855&t=1790438429",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/705234371527986554426413e79242e8.mp4?sign=1c8f49af7d583a0e7454a9153e4540a4&t=1790438429",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            7 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/5fe0df13cfd5b771350cf5f6d0da6861.mp4?sign=2fa36b294c698a9c22cc23c47c278748&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ead5466389ed1be4f6c878f55f804797.mp4?sign=385c27c2ec39ad42aee071bf87106763&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/46fc1a15105af64dbdec3b93a52a1e31.mp4?sign=4dd346d933c139b55b0bd7ce5b1fbda2&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            8 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/585c36c64e69851974aafb7653b25131.mp4?sign=794d4ebcd9f7e9be5130a746d6bde903&t=1790438356",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9ec4c4f2e25c9c1791c04721883417fa.mp4?sign=e664349e85b28161a10165e1d0ff7dc6&t=1790438356",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/176722b691112e35d737a541e6c4f28e.mp4?sign=986abc233fc7535101038209941de94e&t=1790438356",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "mb_4216146306902949328" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cd60c1f0092f86315f22e8d1a041234c.mp4?sign=ae5690a4e64ecd06b413ba7432f5f5cf&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cb9754613342f3d1903f6636cedbb386.mp4?sign=a600e16a8049611cc6a2d50298ad84c1&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/081fa02440bafea01586034b2e6c7f06.mp4?sign=300dd84353325a53906f5c77623cb007&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/6a5e8924471c329d0ae417b1b339b8dd.mp4?sign=214385ee3cabe6448789ea0f52f4b4a7&t=1790438429",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/31d393c3035b78f79cfdb83feecc4575.mp4?sign=d9205b89c7f2c1898d2566cc1152b826&t=1790438429",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/902f6893f645f6e220d8700392eb91cb.mp4?sign=9fd9c255345d0b0a46e1c2d4ef4eb6ac&t=1790438429",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4b6fa4cdc3658708f24926ca89c5d461.mp4?sign=ae5e24f6a9f11c03345f1bade2c1f0d8&t=1790438407",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/dbaa4a5b4ed65c54d0562c6a1cd3ced8.mp4?sign=df71980e52f11a830507afb5cb9fc225&t=1790438407",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/182f4498d3f61a410ecfa8e9a96bd7b3.mp4?sign=0a7c5ebaf32b2d5db6c80574ed2a6f0d&t=1790438407",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            4 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/427d15c2a6eef56a8332213d223c7ba8.mp4?sign=9d537ae8a789e1030baa1480e16bb186&t=1790438356",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1a230a8f053a5264cf21bbef3fadfe2a.mp4?sign=b58b52619d5798ae03f69481f8ed7105&t=1790438356",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/95f218002de23f8e9238581e79cd3575.mp4?sign=1106f1b54b22957013528cb18824e78f&t=1790438356",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            5 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/2861ee630db360c199c2fea4534df285.mp4?sign=0973a4d26a82f62599a032e6f542228d&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/312e4753297b89b85c8a9ea482d06a4d.mp4?sign=b0b531175e82ef2170dc58e197761299&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9fc085f3cc9b7be5c28481df785d378e.mp4?sign=4afedd676257a2d6f4c9793be137bf6c&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            6 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/818d1112a36daaa2676603d46bb04b9e.mp4?sign=3f3bfed9407e29478e3385a7e4ef531e&t=1790438429",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/aa87e9892e4c8b3f6dc780e849ef9cdd.mp4?sign=6d9177d729cc86f118407786c4fb0855&t=1790438429",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/705234371527986554426413e79242e8.mp4?sign=1c8f49af7d583a0e7454a9153e4540a4&t=1790438429",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            7 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/5fe0df13cfd5b771350cf5f6d0da6861.mp4?sign=2fa36b294c698a9c22cc23c47c278748&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ead5466389ed1be4f6c878f55f804797.mp4?sign=385c27c2ec39ad42aee071bf87106763&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/46fc1a15105af64dbdec3b93a52a1e31.mp4?sign=4dd346d933c139b55b0bd7ce5b1fbda2&t=1790438461",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            8 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/585c36c64e69851974aafb7653b25131.mp4?sign=794d4ebcd9f7e9be5130a746d6bde903&t=1790438356",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9ec4c4f2e25c9c1791c04721883417fa.mp4?sign=e664349e85b28161a10165e1d0ff7dc6&t=1790438356",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/176722b691112e35d737a541e6c4f28e.mp4?sign=986abc233fc7535101038209941de94e&t=1790438356",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "5457391580569130440" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/3b74c95087091b66b1e1c430893d1f87.mp4?sign=fd064ba396615ea5d4bfd21f5e23d405&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/d8effd2317809122aabcfda60cb43e03.mp4?sign=e726359faadf5c1dc65f348fd151fdef&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/d10dfafdebefc2bb59392513ca968533.mp4?sign=f6d985beefae474714dcb3b10740d903&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/73f1927d1b4d93e69f8cb2e972647892.mp4?sign=d736b28f03c859b01a43aec5a95222ef&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c58f79715613a406de3c64236692d654.mp4?sign=f27a6cde40abb2e0ad2325ff672e60ba&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e31d2a2c6b3c299790880bcb1f4a5b2d.mp4?sign=b00c4b7fb3038fd7eb6fca8b2941fa87&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b4f7e42c642475e3e12fdf433411f9a7.mp4?sign=538220cee9a5acd0192d7176f6156f0b&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e6ed76543f5977e1048c40acee5d4c5d.mp4?sign=d1405d76ff75d4d03d4739ef3799d808&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4a29fab9aff3d89bf5d06c2a92a422a4.mp4?sign=a8faa9b940cb773241c09cb0e9cf792f&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            4 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/db5327672dea52fbcd7c74b464922d9e.mp4?sign=68dd60b3caa078ce4bd494c3de44e450&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ea301fe6bcfa4bfc79327a47b8277c9c.mp4?sign=9ab69553a71a54f22ed918cdb8be3d2a&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9fe892d15c80a7ab1d9a3465952aa429.mp4?sign=420183caefdc3b72d7480c26dc633db8&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            5 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9bf4d1a6f0207f9fa29f67868f1c6a52.mp4?sign=fbc71bdf4f187d042fb871ede5fedf30&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/7b574450fda9e4623ade257b19fc7ff3.mp4?sign=f3c83d434c58a1f0dcd1cb13bd1560f1&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9e6c820f900a5d8636fe1e0613ff5799.mp4?sign=2780e5a6c84da1089443c1265a82054c&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            6 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/7721effd221ef365d5e2e182b8a9f149.mp4?sign=202f514be2cac5a9168e63976fef066b&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9f7d64a04c25bf6ba5f4c8a9c9c904dc.mp4?sign=606d14fb64f4ae2c9514a70bd1ab19a6&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4446c23e39d61a304af54f3447e5f8d6.mp4?sign=e0a1af1f7dd83a601b74d203e09c1789&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            7 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4acc84ff7cd15ea8ffb9812dc9ead9e9.mp4?sign=af44bae01de091d1be53dee310ef2592&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/8323d694668c429205c0ac36acd44afc.mp4?sign=ae3aab879c18a667626ea4d8d17d26be&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ff2a48c4f4b029b514c27483e8f4bf1e.mp4?sign=31e8e449175a9e6a6e10135a36ec4837&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            8 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/582e22f3b2a17a911a39082216d480b3.mp4?sign=148bb0df6926c57f0bcd458c9e504f73&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ed3b1e15f788431dca2a4d800a765321.mp4?sign=b50789925e264d18ee5e7e6c7bb25bbc&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e4c82172aff36fb97a8211fc3eaa5c12.mp4?sign=593f638e4375909971c154200e9dfa35&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            9 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/35a9e33e4c44ae88911371321276a360.mp4?sign=ec04eac85b8a51a53ca6578df7c25d21&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e251bf4259b43f66d4b335cbc4c674cc.mp4?sign=8657805e1e2f5afefe73dcca41345d99&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/eb1fe289250733e425d324515d33a11a.mp4?sign=7c88bcc870fa6e59b4b2c74466f25820&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            10 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b9058092afb23877981c4ae62a284cd2.mp4?sign=e57048720370ce120256423d7bef5b8d&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4af69adf59aa11582a2ea1b84cf4f7c8.mp4?sign=45743b304bec35ffdc9218d62dda54be&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4efac04a3e57658a7ee76772b9912918.mp4?sign=26ab897466fe87dea2f04a95c84ba246&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "mb_5457391580569130440" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/3b74c95087091b66b1e1c430893d1f87.mp4?sign=fd064ba396615ea5d4bfd21f5e23d405&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/d8effd2317809122aabcfda60cb43e03.mp4?sign=e726359faadf5c1dc65f348fd151fdef&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/d10dfafdebefc2bb59392513ca968533.mp4?sign=f6d985beefae474714dcb3b10740d903&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/73f1927d1b4d93e69f8cb2e972647892.mp4?sign=d736b28f03c859b01a43aec5a95222ef&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c58f79715613a406de3c64236692d654.mp4?sign=f27a6cde40abb2e0ad2325ff672e60ba&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e31d2a2c6b3c299790880bcb1f4a5b2d.mp4?sign=b00c4b7fb3038fd7eb6fca8b2941fa87&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b4f7e42c642475e3e12fdf433411f9a7.mp4?sign=538220cee9a5acd0192d7176f6156f0b&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e6ed76543f5977e1048c40acee5d4c5d.mp4?sign=d1405d76ff75d4d03d4739ef3799d808&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4a29fab9aff3d89bf5d06c2a92a422a4.mp4?sign=a8faa9b940cb773241c09cb0e9cf792f&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            4 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/db5327672dea52fbcd7c74b464922d9e.mp4?sign=68dd60b3caa078ce4bd494c3de44e450&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ea301fe6bcfa4bfc79327a47b8277c9c.mp4?sign=9ab69553a71a54f22ed918cdb8be3d2a&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9fe892d15c80a7ab1d9a3465952aa429.mp4?sign=420183caefdc3b72d7480c26dc633db8&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            5 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9bf4d1a6f0207f9fa29f67868f1c6a52.mp4?sign=fbc71bdf4f187d042fb871ede5fedf30&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/7b574450fda9e4623ade257b19fc7ff3.mp4?sign=f3c83d434c58a1f0dcd1cb13bd1560f1&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9e6c820f900a5d8636fe1e0613ff5799.mp4?sign=2780e5a6c84da1089443c1265a82054c&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            6 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/7721effd221ef365d5e2e182b8a9f149.mp4?sign=202f514be2cac5a9168e63976fef066b&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9f7d64a04c25bf6ba5f4c8a9c9c904dc.mp4?sign=606d14fb64f4ae2c9514a70bd1ab19a6&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4446c23e39d61a304af54f3447e5f8d6.mp4?sign=e0a1af1f7dd83a601b74d203e09c1789&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            7 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4acc84ff7cd15ea8ffb9812dc9ead9e9.mp4?sign=af44bae01de091d1be53dee310ef2592&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/8323d694668c429205c0ac36acd44afc.mp4?sign=ae3aab879c18a667626ea4d8d17d26be&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ff2a48c4f4b029b514c27483e8f4bf1e.mp4?sign=31e8e449175a9e6a6e10135a36ec4837&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            8 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/582e22f3b2a17a911a39082216d480b3.mp4?sign=148bb0df6926c57f0bcd458c9e504f73&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ed3b1e15f788431dca2a4d800a765321.mp4?sign=b50789925e264d18ee5e7e6c7bb25bbc&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e4c82172aff36fb97a8211fc3eaa5c12.mp4?sign=593f638e4375909971c154200e9dfa35&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            9 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/35a9e33e4c44ae88911371321276a360.mp4?sign=ec04eac85b8a51a53ca6578df7c25d21&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e251bf4259b43f66d4b335cbc4c674cc.mp4?sign=8657805e1e2f5afefe73dcca41345d99&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/eb1fe289250733e425d324515d33a11a.mp4?sign=7c88bcc870fa6e59b4b2c74466f25820&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            10 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b9058092afb23877981c4ae62a284cd2.mp4?sign=e57048720370ce120256423d7bef5b8d&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4af69adf59aa11582a2ea1b84cf4f7c8.mp4?sign=45743b304bec35ffdc9218d62dda54be&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/4efac04a3e57658a7ee76772b9912918.mp4?sign=26ab897466fe87dea2f04a95c84ba246&t=1790438476",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "8156965305642802800" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b4f60064e5c18b066053a57eb67fa06a.mp4?sign=a7cdbc8087891e1d35269bd79422b8d8&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/5213ab7201bbe87393b435a307fdb892.mp4?sign=b05c232477cab9eacb3e96e9ee204806&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/57721de1baa77757362b16ff02596be8.mp4?sign=597508f6bd1eba1a35d1ee9e7657d8d6&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1159506b77c1c6749adbed256a0a7774.mp4?sign=0db92aa2cb851af867c8e7829bf3d0db&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/7a43f2e9bc9a4d696d5f59307a4334fc.mp4?sign=2e315b9e741a7b66d31c121b2757acf0&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/a3714d9cefbdae9cae92abfd022f8e9e.mp4?sign=c4d3e86c36f776f13e1f5cec81abc699&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/0f2ebd577fbd7e784f5aab43935b5c61.mp4?sign=dc6e3a8dadef65250a6d878c0e934cf4&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/fb248bfe0f4265965e95ea0e9ef38997.mp4?sign=5edc34b425f3259d83855627467a2423&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/06d6a0a151ec8391d2fe21d691281b32.mp4?sign=693e4c00f8fabdb289445b449c8790f3&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            4 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/8f538c4b147a8a57307f329468d31494.mp4?sign=22e2db905277207982c53570f2286613&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/dbd21de6be69ecc6a78d53afa2dbd664.mp4?sign=84c7f50604b7abdc54f5bee5b9da6f27&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f40f6e8f2f244ab0227f3fec65f3637e.mp4?sign=bf6d504bad9379b3bbc49b5e9aba95c0&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            5 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/913e6a31f4b9572a5546d260f624372c.mp4?sign=93688454ec34d304030c4e40a8cd38ca&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/aa6a26b1c812406bf79808304184b838.mp4?sign=45baa159fdb96fa89d8e5fef8f02ad39&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/68b135cec0c20368511c41434fe16a9a.mp4?sign=e3f75194574ce961bb118b290cdf5f9f&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            6 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ccc45ccc270d637f3b12a97e635df323.mp4?sign=9ed7e4807705270b31a10fa8021225e7&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/fd7d90054ebb0be4dbb4ac8efed63641.mp4?sign=49d18c3ec49a3031e3f3b88bfd162020&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1c4709943397f6ae4dc348c7cfa439e2.mp4?sign=553c996ca5162efbf9e2ccc88f74b76b&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            7 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/fa2e60722110e4f2c3c4eacef7446070.mp4?sign=fcf3a3aab44cfd823c817c0be20a7bed&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f32cf6029f24489bb6e31705d40a45f7.mp4?sign=27255ef288c10ade12cca86218d10137&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e67b75969448c578a0845e3308720ef4.mp4?sign=b7af1bd8044e88a56b40207af62d04ac&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            8 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ef4ef89d8bc7b8a7f5cb8afb9e4d8fb1.mp4?sign=40d2b532f04e86240bd012ce71f48507&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9e9ca20790e2b0b2dc7cb73ccadd307c.mp4?sign=465d5b72f9b9d726369f4ff95441073d&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/db1ab12ac7d8f90f606d5fd778e8fdbc.mp4?sign=b8bd59da1b0061713da5fb55f80e9f6b&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "mb_8156965305642802800" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b4f60064e5c18b066053a57eb67fa06a.mp4?sign=a7cdbc8087891e1d35269bd79422b8d8&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/5213ab7201bbe87393b435a307fdb892.mp4?sign=b05c232477cab9eacb3e96e9ee204806&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/57721de1baa77757362b16ff02596be8.mp4?sign=597508f6bd1eba1a35d1ee9e7657d8d6&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1159506b77c1c6749adbed256a0a7774.mp4?sign=0db92aa2cb851af867c8e7829bf3d0db&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/7a43f2e9bc9a4d696d5f59307a4334fc.mp4?sign=2e315b9e741a7b66d31c121b2757acf0&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/a3714d9cefbdae9cae92abfd022f8e9e.mp4?sign=c4d3e86c36f776f13e1f5cec81abc699&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/0f2ebd577fbd7e784f5aab43935b5c61.mp4?sign=dc6e3a8dadef65250a6d878c0e934cf4&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/fb248bfe0f4265965e95ea0e9ef38997.mp4?sign=5edc34b425f3259d83855627467a2423&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/06d6a0a151ec8391d2fe21d691281b32.mp4?sign=693e4c00f8fabdb289445b449c8790f3&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            4 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/8f538c4b147a8a57307f329468d31494.mp4?sign=22e2db905277207982c53570f2286613&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/dbd21de6be69ecc6a78d53afa2dbd664.mp4?sign=84c7f50604b7abdc54f5bee5b9da6f27&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f40f6e8f2f244ab0227f3fec65f3637e.mp4?sign=bf6d504bad9379b3bbc49b5e9aba95c0&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            5 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/913e6a31f4b9572a5546d260f624372c.mp4?sign=93688454ec34d304030c4e40a8cd38ca&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/aa6a26b1c812406bf79808304184b838.mp4?sign=45baa159fdb96fa89d8e5fef8f02ad39&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/68b135cec0c20368511c41434fe16a9a.mp4?sign=e3f75194574ce961bb118b290cdf5f9f&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            6 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ccc45ccc270d637f3b12a97e635df323.mp4?sign=9ed7e4807705270b31a10fa8021225e7&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/fd7d90054ebb0be4dbb4ac8efed63641.mp4?sign=49d18c3ec49a3031e3f3b88bfd162020&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1c4709943397f6ae4dc348c7cfa439e2.mp4?sign=553c996ca5162efbf9e2ccc88f74b76b&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            7 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/fa2e60722110e4f2c3c4eacef7446070.mp4?sign=fcf3a3aab44cfd823c817c0be20a7bed&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f32cf6029f24489bb6e31705d40a45f7.mp4?sign=27255ef288c10ade12cca86218d10137&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e67b75969448c578a0845e3308720ef4.mp4?sign=b7af1bd8044e88a56b40207af62d04ac&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            8 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ef4ef89d8bc7b8a7f5cb8afb9e4d8fb1.mp4?sign=40d2b532f04e86240bd012ce71f48507&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9e9ca20790e2b0b2dc7cb73ccadd307c.mp4?sign=465d5b72f9b9d726369f4ff95441073d&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/db1ab12ac7d8f90f606d5fd778e8fdbc.mp4?sign=b8bd59da1b0061713da5fb55f80e9f6b&t=1790438477",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "8014114810415697440" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/2cf3b072a472516efab94d7cba6a8ade.mp4?sign=559d2ec96b5c8766744514d782669aca&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/d6d42056c787c11edca52413564eac25.mp4?sign=040aea8a9f5c0d2802ec7d5b8d246e70&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/2cf3b072a472516efab94d7cba6a8ade.mp4?sign=559d2ec96b5c8766744514d782669aca&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/d6d42056c787c11edca52413564eac25.mp4?sign=040aea8a9f5c0d2802ec7d5b8d246e70&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/2cf3b072a472516efab94d7cba6a8ade.mp4?sign=559d2ec96b5c8766744514d782669aca&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/d6d42056c787c11edca52413564eac25.mp4?sign=040aea8a9f5c0d2802ec7d5b8d246e70&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "mb_8014114810415697440" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/2cf3b072a472516efab94d7cba6a8ade.mp4?sign=559d2ec96b5c8766744514d782669aca&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/d6d42056c787c11edca52413564eac25.mp4?sign=040aea8a9f5c0d2802ec7d5b8d246e70&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/2cf3b072a472516efab94d7cba6a8ade.mp4?sign=559d2ec96b5c8766744514d782669aca&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/d6d42056c787c11edca52413564eac25.mp4?sign=040aea8a9f5c0d2802ec7d5b8d246e70&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/2cf3b072a472516efab94d7cba6a8ade.mp4?sign=559d2ec96b5c8766744514d782669aca&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/dramabox/d6d42056c787c11edca52413564eac25.mp4?sign=040aea8a9f5c0d2802ec7d5b8d246e70&t=1790438482",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "1682539463995539800" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/45975ae9c12f3cf202f580d99fa95791.mp4?sign=16ada7ab4fe930c86cdd1e4240ee7ec4&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/d459f77e3b1ee6e2e0b038578370101c.mp4?sign=95861a924de1687f762672af6c334208&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b9eca6f973ef0ee46b923bfb16a9b2b4.mp4?sign=15d91c93f27a58e563577b66e29ef571&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1ac9f7bf3a00db0826bce71858a4022b.mp4?sign=5f55ee63572bbeec005d4dbed12bf938&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/eb036157608d2230d09d59fe03eefa46.mp4?sign=f43b75e0cc1fbdcfa2301bbeb6c0c21f&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ef6a2ce32523448602a70c5c4854e049.mp4?sign=d0352cf4a6cc41069978fe105381dccb&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/687c871bee11b6e71fe903489b00db63.mp4?sign=31b7114dafb77c65408810fe6c10c415&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/6257d6187598ceb7f0628f0022512473.mp4?sign=8a3c36b9e8bddbfcfafc88668471a735&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/8f260e03c850fb733bbd32c585c5fa69.mp4?sign=460cb15bc27b1fedb6b2cd485713e583&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            4 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/d83c35aa6419a64505c63276339c178e.mp4?sign=b37f08cc198d39ce3d0e60ccb410c1c1&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/33e337f93f5d1e755c591da989d377bc.mp4?sign=3eb7437e577fe2edcc2dd0399f5b0a0a&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/7847de70c5761eef76994a776dc27172.mp4?sign=26358ad2a7f864f3631a1a943e91b3ce&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            5 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/82e3475cd9b91ab87b8125364a0baf47.mp4?sign=2db2bcab88c6db74f97bad54e1855ecb&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1cdfaaa8bf831237a528c236cb002316.mp4?sign=f07a62c7411011aa0f148b9f2a8dbf3b&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/7a6d95b4818775a3d384b7f1410c9413.mp4?sign=6995772a9d60c1b7c60f2ad3ad4141ae&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            6 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/8acbd79cd916c6a706ff6fe505af1b4e.mp4?sign=0098014e499cc55c36b983691777d463&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/fa13f79044afc6d2a7e093cc4cf5c04a.mp4?sign=8f004fcc5b6c98046f7f1abdf488fc9c&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/0a42c2176bd2b4e4c5b6875605243e5f.mp4?sign=0154500cae0043dd7476c73393403890&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            7 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ef12282c103ef6e0b5f250c8e1a14ee3.mp4?sign=7207498d10dcea60fdb02f4be64a382e&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/33a2ab4a53060c95aeb6fa866039cc70.mp4?sign=27c23fdd6c447bd5e516b1e37fb8ee8a&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/08424aaae8be8c098789e57c17fe0984.mp4?sign=5cb22f57b7f05e73751a4ae7d831e8ea&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            8 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/72f9e1f706d0a02b45384e5812a723d5.mp4?sign=04b4a8c30921aa06e4603efa4eb08d02&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c75c5be4fce5afd4348a6ac9da4d2d2d.mp4?sign=195549b3decb659c93b1d642989238bc&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/06f42a49f5b1d97d83d17628f7e29b71.mp4?sign=9c695861d6ed6c573a6902d5172aa216&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            9 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/0510a2b4b2aad31e9eaba418e100fad4.mp4?sign=de4e17b9bade6f264e7ec6f783ed7b7f&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/28d2d9bd2b1a21dc5142ab59e6901220.mp4?sign=1d2c327139e1b4b38f711691bfd7b9c8&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f602c72619c77720856716bd69e5e3c2.mp4?sign=8b6ac37f764fd38c3ae2f7a013a170a8&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            10 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/a8e3ac1da247987ec3356738059e4851.mp4?sign=738ca4fbdc302ad7f2aebcc1dd212be1&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/abb40c6d93a524204bfba9aebe83069e.mp4?sign=703f4a2ebeafe2d7396706c8a742dd9a&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/8befd8ce1d696cf86faba8e4cb9f2068.mp4?sign=27b30d42ab38f6a4d1e03f58f17c6dc3&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            11 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9c9e68f661c05f0b81689e6f3ea9490f.mp4?sign=d0916dc70659c4cd77a406d8e75903b2&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f42894c9ad74c6da3e3302451ff4d10b.mp4?sign=9ab7b4726ffaf3f2a9e8ac5f7f553c1c&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/186001f3991fa86f5f467cc46667c58e.mp4?sign=0fc68c423ccdb99782bdbbd26eaf292e&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            12 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/2521749a8650d94b267cfb8fe398f9ac.mp4?sign=add84f739f8686611f03662112c321bd&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/dccc477c42e5a20bcc0a3b4cc9217bd2.mp4?sign=21cca33df142e6396b09090432313c03&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/57e2307e2a7aa85e9d24433500889dba.mp4?sign=9a2e02b218dbf9bea035704073c3ea9a&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            13 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/63e92f1014fc100cdec33a2f15807b05.mp4?sign=05e5bcde97edb94f50ebdc8d4e60fbd2&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b7e91bff77cd4f3cdda725264a087e08.mp4?sign=8204d68cdd8183c81fd327992d42fe3e&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/18d8948cfb09e46416d3c4fcab6c4cd8.mp4?sign=c169fa03ed2bc6e5bd0fc32628233a8a&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "mb_1682539463995539800" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/45975ae9c12f3cf202f580d99fa95791.mp4?sign=16ada7ab4fe930c86cdd1e4240ee7ec4&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/d459f77e3b1ee6e2e0b038578370101c.mp4?sign=95861a924de1687f762672af6c334208&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b9eca6f973ef0ee46b923bfb16a9b2b4.mp4?sign=15d91c93f27a58e563577b66e29ef571&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1ac9f7bf3a00db0826bce71858a4022b.mp4?sign=5f55ee63572bbeec005d4dbed12bf938&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/eb036157608d2230d09d59fe03eefa46.mp4?sign=f43b75e0cc1fbdcfa2301bbeb6c0c21f&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ef6a2ce32523448602a70c5c4854e049.mp4?sign=d0352cf4a6cc41069978fe105381dccb&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/687c871bee11b6e71fe903489b00db63.mp4?sign=31b7114dafb77c65408810fe6c10c415&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/6257d6187598ceb7f0628f0022512473.mp4?sign=8a3c36b9e8bddbfcfafc88668471a735&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/8f260e03c850fb733bbd32c585c5fa69.mp4?sign=460cb15bc27b1fedb6b2cd485713e583&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            4 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/d83c35aa6419a64505c63276339c178e.mp4?sign=b37f08cc198d39ce3d0e60ccb410c1c1&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/33e337f93f5d1e755c591da989d377bc.mp4?sign=3eb7437e577fe2edcc2dd0399f5b0a0a&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/7847de70c5761eef76994a776dc27172.mp4?sign=26358ad2a7f864f3631a1a943e91b3ce&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            5 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/82e3475cd9b91ab87b8125364a0baf47.mp4?sign=2db2bcab88c6db74f97bad54e1855ecb&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1cdfaaa8bf831237a528c236cb002316.mp4?sign=f07a62c7411011aa0f148b9f2a8dbf3b&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/7a6d95b4818775a3d384b7f1410c9413.mp4?sign=6995772a9d60c1b7c60f2ad3ad4141ae&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            6 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/8acbd79cd916c6a706ff6fe505af1b4e.mp4?sign=0098014e499cc55c36b983691777d463&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/fa13f79044afc6d2a7e093cc4cf5c04a.mp4?sign=8f004fcc5b6c98046f7f1abdf488fc9c&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/0a42c2176bd2b4e4c5b6875605243e5f.mp4?sign=0154500cae0043dd7476c73393403890&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            7 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ef12282c103ef6e0b5f250c8e1a14ee3.mp4?sign=7207498d10dcea60fdb02f4be64a382e&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/33a2ab4a53060c95aeb6fa866039cc70.mp4?sign=27c23fdd6c447bd5e516b1e37fb8ee8a&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/08424aaae8be8c098789e57c17fe0984.mp4?sign=5cb22f57b7f05e73751a4ae7d831e8ea&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            8 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/72f9e1f706d0a02b45384e5812a723d5.mp4?sign=04b4a8c30921aa06e4603efa4eb08d02&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c75c5be4fce5afd4348a6ac9da4d2d2d.mp4?sign=195549b3decb659c93b1d642989238bc&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/06f42a49f5b1d97d83d17628f7e29b71.mp4?sign=9c695861d6ed6c573a6902d5172aa216&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            9 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/0510a2b4b2aad31e9eaba418e100fad4.mp4?sign=de4e17b9bade6f264e7ec6f783ed7b7f&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/28d2d9bd2b1a21dc5142ab59e6901220.mp4?sign=1d2c327139e1b4b38f711691bfd7b9c8&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f602c72619c77720856716bd69e5e3c2.mp4?sign=8b6ac37f764fd38c3ae2f7a013a170a8&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            10 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/a8e3ac1da247987ec3356738059e4851.mp4?sign=738ca4fbdc302ad7f2aebcc1dd212be1&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/abb40c6d93a524204bfba9aebe83069e.mp4?sign=703f4a2ebeafe2d7396706c8a742dd9a&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/8befd8ce1d696cf86faba8e4cb9f2068.mp4?sign=27b30d42ab38f6a4d1e03f58f17c6dc3&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            11 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9c9e68f661c05f0b81689e6f3ea9490f.mp4?sign=d0916dc70659c4cd77a406d8e75903b2&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f42894c9ad74c6da3e3302451ff4d10b.mp4?sign=9ab7b4726ffaf3f2a9e8ac5f7f553c1c&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/186001f3991fa86f5f467cc46667c58e.mp4?sign=0fc68c423ccdb99782bdbbd26eaf292e&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            12 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/2521749a8650d94b267cfb8fe398f9ac.mp4?sign=add84f739f8686611f03662112c321bd&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/dccc477c42e5a20bcc0a3b4cc9217bd2.mp4?sign=21cca33df142e6396b09090432313c03&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/57e2307e2a7aa85e9d24433500889dba.mp4?sign=9a2e02b218dbf9bea035704073c3ea9a&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            13 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/63e92f1014fc100cdec33a2f15807b05.mp4?sign=05e5bcde97edb94f50ebdc8d4e60fbd2&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b7e91bff77cd4f3cdda725264a087e08.mp4?sign=8204d68cdd8183c81fd327992d42fe3e&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/18d8948cfb09e46416d3c4fcab6c4cd8.mp4?sign=c169fa03ed2bc6e5bd0fc32628233a8a&t=1790438500",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "1185287529357108384" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/3ed1be0d6fecae585eeae167ab2a5afd.mp4?sign=d0e238000f0e34f4d8f847926aac60f5&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/546408688ded9c40855b88179d45791e.mp4?sign=34724b26bf37ede35398d4054b35348a&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/afaca9aedb6dd547c48c6f6123622177.mp4?sign=ba38da7c02d8676b9425baf6778980e2&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/6589f8da2cd3a992bf8da0d7179fe187.mp4?sign=f7238f8c0a790a55ff170c9b7221c4a7&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/bf9dd3e576178a29388e72c1c0969e13.mp4?sign=c44255e1266f897786128c8175bc1c8d&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1c085feb906e0b9d3f2757b3c67e5194.mp4?sign=e1e822925255845e2f9d80d181cd17ae&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/06f0c62f1a0314d105199554897dda67.mp4?sign=ab42b2d3d82f10fc633c1b370364ee5d&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cb88f34cffee954020321f6a3a38b0c3.mp4?sign=a68d53d2a481cd5adb74fba06f68110b&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c47c275598ade0bf2d00665914817893.mp4?sign=ab83d10659136343335f51652226fb50&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            4 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/5927d54ae84576260af5f75267a75508.mp4?sign=650bf29e85f7c1b5d66f046695005d81&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e9db32db7da18f1f7a183c5b47c71c03.mp4?sign=5847dab7e6108b67635bd392a187791d&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/78ffd89d5cc6704b32d2f58ccaced45c.mp4?sign=345e581e28c5708fb594e2993c7359f8&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            5 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f6d82728d109b05d2828a4ff2f4fb90a.mp4?sign=9e98ded1b1485af8928eaed078b103cd&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/60cd99df020120ab5edf686873770b74.mp4?sign=f3f8096132af6e53aabdac74fca0c3aa&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/56b982cb15458f027cdc2a418657c6f2.mp4?sign=9983a5b0791f8efd83310c049b39292c&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            6 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cffcab2555115e81517e4e1a6cd4814e.mp4?sign=4140fa367c499f7228b3145dd93cf08e&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/630d13641f949f8941e46257c20dd0ee.mp4?sign=159698ccc8d4d673915ab0cf984ba0aa&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/97f9f79e72e68fe8398469bbc210bb9d.mp4?sign=598bfc57835df25cda03207a9d9e2be6&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            7 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b430a902fffeec4017e12599bd637485.mp4?sign=9f6623595158bba680ece47d10ffc35b&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/3ce51de9b2c26ed6f8ebc8855da1582f.mp4?sign=8691da8ee2b55d019da6e5b1b1fbed93&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f9700ed2249a839892928dc5ac77244d.mp4?sign=7ec83d88a48151ef2557f7b8a41812fd&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            8 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e67b9909b8da9d6c0b94e76b35a66d04.mp4?sign=aa1acd5d0aa94cc221af5ffcdf17dba1&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f5bd833a1c7fcff44624093443eaad13.mp4?sign=39c5141a3ff10b76eb904cee0cf8cdb7&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/60bb6d28f48feabe091c7ff5f47a8f51.mp4?sign=74333917517fa36f0a57c19b953b97d6&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "mb_1185287529357108384" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/3ed1be0d6fecae585eeae167ab2a5afd.mp4?sign=d0e238000f0e34f4d8f847926aac60f5&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/546408688ded9c40855b88179d45791e.mp4?sign=34724b26bf37ede35398d4054b35348a&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/afaca9aedb6dd547c48c6f6123622177.mp4?sign=ba38da7c02d8676b9425baf6778980e2&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/6589f8da2cd3a992bf8da0d7179fe187.mp4?sign=f7238f8c0a790a55ff170c9b7221c4a7&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/bf9dd3e576178a29388e72c1c0969e13.mp4?sign=c44255e1266f897786128c8175bc1c8d&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1c085feb906e0b9d3f2757b3c67e5194.mp4?sign=e1e822925255845e2f9d80d181cd17ae&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/06f0c62f1a0314d105199554897dda67.mp4?sign=ab42b2d3d82f10fc633c1b370364ee5d&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cb88f34cffee954020321f6a3a38b0c3.mp4?sign=a68d53d2a481cd5adb74fba06f68110b&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c47c275598ade0bf2d00665914817893.mp4?sign=ab83d10659136343335f51652226fb50&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            4 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/5927d54ae84576260af5f75267a75508.mp4?sign=650bf29e85f7c1b5d66f046695005d81&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e9db32db7da18f1f7a183c5b47c71c03.mp4?sign=5847dab7e6108b67635bd392a187791d&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/78ffd89d5cc6704b32d2f58ccaced45c.mp4?sign=345e581e28c5708fb594e2993c7359f8&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            5 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f6d82728d109b05d2828a4ff2f4fb90a.mp4?sign=9e98ded1b1485af8928eaed078b103cd&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/60cd99df020120ab5edf686873770b74.mp4?sign=f3f8096132af6e53aabdac74fca0c3aa&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/56b982cb15458f027cdc2a418657c6f2.mp4?sign=9983a5b0791f8efd83310c049b39292c&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            6 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cffcab2555115e81517e4e1a6cd4814e.mp4?sign=4140fa367c499f7228b3145dd93cf08e&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/630d13641f949f8941e46257c20dd0ee.mp4?sign=159698ccc8d4d673915ab0cf984ba0aa&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/97f9f79e72e68fe8398469bbc210bb9d.mp4?sign=598bfc57835df25cda03207a9d9e2be6&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            7 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b430a902fffeec4017e12599bd637485.mp4?sign=9f6623595158bba680ece47d10ffc35b&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/3ce51de9b2c26ed6f8ebc8855da1582f.mp4?sign=8691da8ee2b55d019da6e5b1b1fbed93&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f9700ed2249a839892928dc5ac77244d.mp4?sign=7ec83d88a48151ef2557f7b8a41812fd&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            8 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e67b9909b8da9d6c0b94e76b35a66d04.mp4?sign=aa1acd5d0aa94cc221af5ffcdf17dba1&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f5bd833a1c7fcff44624093443eaad13.mp4?sign=39c5141a3ff10b76eb904cee0cf8cdb7&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/60bb6d28f48feabe091c7ff5f47a8f51.mp4?sign=74333917517fa36f0a57c19b953b97d6&t=1790438503",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "4566460641476763544" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/quicktv/00e8ab6d318a2185e3bf362481615a2a.mp4?sign=09d9c8f052b3bcfbfb438b91d9e21484&t=1790438504",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/quicktv/0d705ca1541bcd60ba0ce882b210fd36.mp4?sign=8e7712f48228c23c8b9cea65b73224d5&t=1790438504",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "mb_4566460641476763544" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/quicktv/00e8ab6d318a2185e3bf362481615a2a.mp4?sign=09d9c8f052b3bcfbfb438b91d9e21484&t=1790438504",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/quicktv/0d705ca1541bcd60ba0ce882b210fd36.mp4?sign=8e7712f48228c23c8b9cea65b73224d5&t=1790438504",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "4536097593516081960" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/df45a9312e971c6641d78d9bf46771ab.mp4?sign=cd056c8ec575b4312268704131e239eb&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/805abd1530379d6fe8ce5d452b07ca12.mp4?sign=c2568c073885cae66cbde33e6e92fb56&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f5d97094207ac6b397d3e2d0673c4e61.mp4?sign=31289e6880ae6414825a2b29c2b44ddc&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/3065d604d2568419f9fcd10edb3e5e77.mp4?sign=69480646fd686123ea1b34caceec23bb&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/41e27b77cbfc43e52ba153fc9ce26919.mp4?sign=713f4e286d7b67b7c272aef4ad89689b&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9889f0574f0603613d5c3ca38d49e2f7.mp4?sign=4389ef458157687bd9c6065c34c38aa9&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/bb3ffd561e021aa8de50c89f99845a72.mp4?sign=289c7fe96b536f309033035cdd625fe0&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c7db9d53f2a5189c2091b7dab1a50aa5.mp4?sign=b8f5f5d0e94d5ff104a7165b61296a9d&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/379ad91c4fb9519c1930d35ae1795377.mp4?sign=cbe2dbca5846b5ec0081a653e1a4e0aa&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            4 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/19e20fe72b40d5e92338142922f90c10.mp4?sign=4e5f7ebb32670c907dcea5197349abb8&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ce66c74cf6c4f6079fd5f0914a4b5dd4.mp4?sign=b0e52c8a76a43970721034547b5e9565&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/db800e60e1fcc210396094d0efe2c3ef.mp4?sign=ff7ab3baf73d849f147b6394c3011a89&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            5 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e5f97c0cb3fde43e6f1796473454897f.mp4?sign=fbab63a62cdfd415b0850b7594782f65&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/da104a18b28e644cf7a1bf522768917a.mp4?sign=dce130dbafcee089e5bab9611070b7f5&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cc7367d8cddfabdaf3f06a2b69452e69.mp4?sign=87a0a6e5dab10411ccd1254959b82af1&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            6 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/dcc7bcfb83a86088c284f00302867f33.mp4?sign=3f9b1c5a4f344c3d1f7e50083e3599ac&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/93cf16ed0e02fbe3714d11b73e67d392.mp4?sign=bdc9e737e009c8bfb88efae6bdaeb126&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/507c1aafa5be4bb6250a456e39fbe21c.mp4?sign=853ea77af9de1c020160464eb7b1d411&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            7 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/968bd1116dbede4f67e956fd76236aef.mp4?sign=21f26b379471cbd67da88307e80764f7&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/bbe85e742322537ff141d9adef14b589.mp4?sign=962984ab0b6f25809b9718130bcb19eb&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c117e9eafa7275908a924d7778c978a2.mp4?sign=6e3b80767aaed66d28f53200828487ec&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            8 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c0cc7229711abe1b9a80822c4bb80898.mp4?sign=cf05b3438920e384cebff7d8b673ab73&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/757132439b1810e6a2427c66aecd6346.mp4?sign=7d7aa541f4c3f7ac0d50a27e0b6d3753&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/5ec05b2288367a213f7b41c749ac154e.mp4?sign=c28f8f18c52b1ec02cc1813895f52e51&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            9 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/0258479f4ba5447b5083b23122b8469a.mp4?sign=4a9ceec3eb13dcd21e3b57aaabab504e&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b3d23640ebb8087fca0b0417ede7265a.mp4?sign=61004cbab7b5df47c4bac834e44427c5&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1b7f2e1e79ecc16c333a808e42f5dba1.mp4?sign=a3e2b6d66fff22b9644012a9e82dd6a5&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            10 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/37dba6e4578b27ea31affd724f3bf701.mp4?sign=b12b3d39da0d5a2068009d02caaeee34&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/67b1d16ac66397c47a042183e2bd8e6f.mp4?sign=dfbf08a2860e0cb3cb5b9229cb402929&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/eb366b3ecb0cb6f8f55fdcd4c0c8c4d2.mp4?sign=f9cf22953d8ac771a5f31b1c4eaed853&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            11 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/dcb91fb6e3163b666c8970c2b14e029f.mp4?sign=2984b3134ab740e5587efaf0654ba4cc&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e6254e7d3f05ca1fdf198a7da84b05ef.mp4?sign=eb9eef902920deccdb716acfe5872930&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/2039e968289ee0aa9cd700b6a398dc0b.mp4?sign=99fa7a8b843b0e38d82c7d8c0cb6d687&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
        "mb_4536097593516081960" to mapOf(
            1 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/df45a9312e971c6641d78d9bf46771ab.mp4?sign=cd056c8ec575b4312268704131e239eb&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/805abd1530379d6fe8ce5d452b07ca12.mp4?sign=c2568c073885cae66cbde33e6e92fb56&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/f5d97094207ac6b397d3e2d0673c4e61.mp4?sign=31289e6880ae6414825a2b29c2b44ddc&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            2 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/3065d604d2568419f9fcd10edb3e5e77.mp4?sign=69480646fd686123ea1b34caceec23bb&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/41e27b77cbfc43e52ba153fc9ce26919.mp4?sign=713f4e286d7b67b7c272aef4ad89689b&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/9889f0574f0603613d5c3ca38d49e2f7.mp4?sign=4389ef458157687bd9c6065c34c38aa9&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            3 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/bb3ffd561e021aa8de50c89f99845a72.mp4?sign=289c7fe96b536f309033035cdd625fe0&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c7db9d53f2a5189c2091b7dab1a50aa5.mp4?sign=b8f5f5d0e94d5ff104a7165b61296a9d&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/379ad91c4fb9519c1930d35ae1795377.mp4?sign=cbe2dbca5846b5ec0081a653e1a4e0aa&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            4 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/19e20fe72b40d5e92338142922f90c10.mp4?sign=4e5f7ebb32670c907dcea5197349abb8&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/ce66c74cf6c4f6079fd5f0914a4b5dd4.mp4?sign=b0e52c8a76a43970721034547b5e9565&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/db800e60e1fcc210396094d0efe2c3ef.mp4?sign=ff7ab3baf73d849f147b6394c3011a89&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            5 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e5f97c0cb3fde43e6f1796473454897f.mp4?sign=fbab63a62cdfd415b0850b7594782f65&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/da104a18b28e644cf7a1bf522768917a.mp4?sign=dce130dbafcee089e5bab9611070b7f5&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/cc7367d8cddfabdaf3f06a2b69452e69.mp4?sign=87a0a6e5dab10411ccd1254959b82af1&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            6 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/dcc7bcfb83a86088c284f00302867f33.mp4?sign=3f9b1c5a4f344c3d1f7e50083e3599ac&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/93cf16ed0e02fbe3714d11b73e67d392.mp4?sign=bdc9e737e009c8bfb88efae6bdaeb126&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/507c1aafa5be4bb6250a456e39fbe21c.mp4?sign=853ea77af9de1c020160464eb7b1d411&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            7 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/968bd1116dbede4f67e956fd76236aef.mp4?sign=21f26b379471cbd67da88307e80764f7&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/bbe85e742322537ff141d9adef14b589.mp4?sign=962984ab0b6f25809b9718130bcb19eb&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c117e9eafa7275908a924d7778c978a2.mp4?sign=6e3b80767aaed66d28f53200828487ec&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            8 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/c0cc7229711abe1b9a80822c4bb80898.mp4?sign=cf05b3438920e384cebff7d8b673ab73&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/757132439b1810e6a2427c66aecd6346.mp4?sign=7d7aa541f4c3f7ac0d50a27e0b6d3753&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/5ec05b2288367a213f7b41c749ac154e.mp4?sign=c28f8f18c52b1ec02cc1813895f52e51&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            9 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/0258479f4ba5447b5083b23122b8469a.mp4?sign=4a9ceec3eb13dcd21e3b57aaabab504e&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/b3d23640ebb8087fca0b0417ede7265a.mp4?sign=61004cbab7b5df47c4bac834e44427c5&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/1b7f2e1e79ecc16c333a808e42f5dba1.mp4?sign=a3e2b6d66fff22b9644012a9e82dd6a5&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            10 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/37dba6e4578b27ea31affd724f3bf701.mp4?sign=b12b3d39da0d5a2068009d02caaeee34&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/67b1d16ac66397c47a042183e2bd8e6f.mp4?sign=dfbf08a2860e0cb3cb5b9229cb402929&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/eb366b3ecb0cb6f8f55fdcd4c0c8c4d2.mp4?sign=f9cf22953d8ac771a5f31b1c4eaed853&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
            11 to listOf(
                ShortDramaSource(
                    id = "mb_fhd",
                    name = "سيرفر MovieBox الأصلي (FHD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/dcb91fb6e3163b666c8970c2b14e029f.mp4?sign=2984b3134ab740e5587efaf0654ba4cc&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "1080p FHD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_hd",
                    name = "سيرفر MovieBox السريع (HD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/e6254e7d3f05ca1fdf198a7da84b05ef.mp4?sign=eb9eef902920deccdb716acfe5872930&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "720p HD",
                    isDirectCdn = true
                ),
                ShortDramaSource(
                    id = "mb_sd",
                    name = "سيرفر MovieBox الموفر (SD)",
                    streamUrl = "https://bcdn.hakunaymatata.com/shortTv/2039e968289ee0aa9cd700b6a398dc0b.mp4?sign=99fa7a8b843b0e38d82c7d8c0cb6d687&t=1790438506",
                    userAgent = "okhttp/4.12.0",
                    quality = "480p SD",
                    isDirectCdn = true
                ),
            ),
        ),
    )

    val postIdMap: Map<String, String> = mapOf(
        "mb_6852294664000094536" to "3114306973282582800",
        "6852294664000094536" to "3114306973282582800",
        "mb_7719219455695770656" to "1409676377749704664",
        "7719219455695770656" to "1409676377749704664",
        "mb_4685140523198337616" to "2921585444896802568",
        "4685140523198337616" to "2921585444896802568",
        "mb_7760318376782059032" to "5427172294827345456",
        "7760318376782059032" to "5427172294827345456",
        "mb_2379697736217248944" to "5156307643979516072",
        "2379697736217248944" to "5156307643979516072",
        "mb_5487903153867605208" to "8215677153170576096",
        "5487903153867605208" to "8215677153170576096",
        "mb_3743938733515651720" to "3876794922523149520",
        "3743938733515651720" to "3876794922523149520",
        "mb_5793230140188648176" to "1969674056551089048",
        "5793230140188648176" to "1969674056551089048",
        "mb_4216146306902949328" to "8242364373772257992",
        "4216146306902949328" to "8242364373772257992",
        "mb_8911832570929858720" to "7751170504463368480",
        "8911832570929858720" to "7751170504463368480",
        "mb_1355131804918892504" to "8295178830696825488",
        "1355131804918892504" to "8295178830696825488",
        "mb_4266462988015656176" to "6481178473519368360",
        "4266462988015656176" to "6481178473519368360",
        "mb_5171417625615823080" to "2167516674159695496",
        "5171417625615823080" to "2167516674159695496",
        "mb_2482015505159278984" to "1540534484620269008",
        "2482015505159278984" to "1540534484620269008",
        "mb_5457391580569130440" to "1676056743438187624",
        "5457391580569130440" to "1676056743438187624",
        "mb_8156965305642802800" to "2561242768634953864",
        "8156965305642802800" to "2561242768634953864",
        "mb_9211810355872159128" to "8968615975994253040",
        "9211810355872159128" to "8968615975994253040",
        "mb_1093491187970987096" to "7344487470761230464",
        "1093491187970987096" to "7344487470761230464",
        "mb_8014114810415697440" to "5242254792885420480",
        "8014114810415697440" to "5242254792885420480",
        "mb_3456987833490108648" to "7477576400825205656",
        "3456987833490108648" to "7477576400825205656",
        "mb_4379436669603403288" to "287916408137306192",
        "4379436669603403288" to "287916408137306192",
        "mb_7431280192427291224" to "6728718650557582368",
        "7431280192427291224" to "6728718650557582368",
        "mb_5466173340211724336" to "6628102044073313488",
        "5466173340211724336" to "6628102044073313488",
        "mb_8214767698140710440" to "97592319758814784",
        "8214767698140710440" to "97592319758814784",
        "mb_1604082672555511488" to "730384344845632720",
        "1604082672555511488" to "730384344845632720",
        "mb_8548283423482336624" to "6580562230042316952",
        "8548283423482336624" to "6580562230042316952",
        "mb_1682539463995539800" to "8645104487910228096",
        "1682539463995539800" to "8645104487910228096",
        "mb_5835500950617838624" to "3578071637398279296",
        "5835500950617838624" to "3578071637398279296",
        "mb_6848959615452203352" to "5304787893217643136",
        "6848959615452203352" to "5304787893217643136",
        "mb_1185287529357108384" to "633596575004119944",
        "1185287529357108384" to "633596575004119944",
        "mb_4566460641476763544" to "2637051123252740232",
        "4566460641476763544" to "2637051123252740232",
        "mb_4536097593516081960" to "2246017182998093672",
        "4536097593516081960" to "2246017182998093672",
        "mb_5509574094259475032" to "8867007616464179280",
        "5509574094259475032" to "8867007616464179280",
        "mb_7034288313268161032" to "8560164162016632160",
        "7034288313268161032" to "8560164162016632160",
    )

    fun getAllSeries(): List<ShortDramaSeries> = entries.map { it.series }

    fun getSourcesMap(): Map<String, List<ShortDramaSource>> = entries.associate {
        it.series.id.removePrefix("mb_") to it.sources
    }

    /**
     * Returns authentic MovieBox CDN streams for a specific episode of a series.
     */
    fun getSourcesForEpisode(videoId: String, episodeNumber: Int): List<ShortDramaSource>? {
        val clean = videoId.removePrefix("mb_")
        val epMap = multiEpisodeMap[clean] ?: multiEpisodeMap["mb_" + clean]
        if (epMap != null && epMap.containsKey(episodeNumber)) {
            return epMap[episodeNumber]
        }
        return if (episodeNumber == 1) getSourcesMap()[clean] else null
    }

    fun getPostId(videoId: String): String? = postIdMap[videoId]
}

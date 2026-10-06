const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const publicDir = '/storage/emulated/0/CodeOnTheGoProjects/Beouttv/public';
const matchesFile = path.join(publicDir, 'matches.json');

const BLOCKED_KEYWORDS = [
    'تحت 21', 'تحت 19', 'تحت 20', 'تحت 23', 'تحت 17', 'تحت 18', 'تحت 16', 'تحت 15',
    'u21', 'u19', 'u20', 'u23', 'u17', 'u18', 'u-21', 'u-19', 'u-20',
    'دوري أبطال أوروبا للشباب', 'للشباب', 'youth league',
    'كأس الاتحاد الإنجليزي - الجولات التمهيدية', 'الجولات التمهيدية',
    'التصفيات الأولية', 'التصفيات التمهيدية', 'preliminary', 'qualifying rounds',
    'الدرجة الثانية', 'الدرجة الثالثة', 'الدرجة الرابعة', 'الدرجة الخامسة',
    '2nd division', '3rd division', 'division 2', 'division 3', 'serie b', 'serie c',
    'segunda', 'liga 2', 'liga 3', '2. bundesliga', '3. liga', 'ligue 2', 'championship',
    'سيدات', 'نساء', 'women', 'female', 'w-league', 'feminin',
    'الهواة', 'amateur', 'الرديف', 'reserve', 'شبيبة', 'دوري الشباب',
    'الدوري الثاني', 'الدوري الثالث', 'b division', 'المناطق',
    'next pro', 'برايم بي', 'primera b', 'primera c', 'pro league b',
    'الدنماركي الدرجة الأولى', 'السويدي - الدرجة الأولى', 'النرويجي - الدرجة الأولى',
    'النمساوي الدرجة الأولى', 'الهولندي - الدرجة الأولى', 'الروسي الدرجة الأولى',
    'التركي الدرجة الأولى', 'الإماراتي الدرجة الأولى', 'الأرجنتيني - برايم',
    'الأوكراني الدرجة الأولى', 'الصيني - الدرجة الأولى', 'المكسيكي الدرجة',
    'كرة السلة', 'basketball', 'كرة اليد', 'handball', 'كرة الطائرة', 'volleyball',
    'كرة المضرب', 'tennis', 'تنس', 'سلة', 'كرة الماء', 'بيسبول', 'baseball', 'هوكي', 'hockey'
];

const MAJOR_KEYWORDS = [
    'أبطال أوروبا', 'champions league', 'الدوري الإنجليزي', 'premier league',
    'الدوري الإسباني', 'la liga', 'الدوري الإيطالي', 'serie a',
    'الدوري الألماني', 'bundesliga', 'الدوري الفرنسي', 'ligue 1',
    'روشن', 'السعودي', 'saudi', 'العراق', 'iraq', 'المصري', 'egypt',
    'أدنوك', 'الإماراتي', 'uae', 'قطر', 'qatar', 'المغربي', 'botola', 'morocco',
    'التونسي', 'tunisia', 'الجزائري', 'algeria', 'أبطال آسيا', 'afc',
    'أبطال إفريقيا', 'caf', 'الدوري الأوروبي', 'europa league',
    'المؤتمر', 'conference league', 'كأس العالم', 'world cup',
    'كأس الاتحاد', 'fa cup', 'كأس ملك', 'copa del rey', 'كأس إيطاليا', 'coppa italia',
    'كأس ألمانيا', 'dfb', 'كأس فرنسا', 'coupe de france',
    'كأس خادم الحرمين', 'كأس السوبر', 'super cup', 'supercup', 'supercopa',
    'كأس الرابطة', 'carabao', 'efl cup', 'كأس البرتغال', 'الدوري البرتغالي', 'liga portugal',
    'الدوري الهولندي', 'eredivisie', 'الدوري التركي', 'super lig', 'süper lig',
    'الدوري البلجيكي', 'الدوري البرازيلي', 'brasileirao', 'brasileirão',
    'الدوري الأرجنتيني الممتاز', 'الدوري الأمريكي الممتاز', 'mls', 'كوبا ليبرتادوريس', 'libertadores',
    'كوبا سود أمريكانا', 'sudamericana', 'كأس أمم', 'nations league',
    'الدوري الكويتي', 'الدوري الأردني', 'الدوري العماني', 'الدوري البحريني',
    'الدوري الأسكتلندي', 'الدوري السويسري', 'الدوري النمساوي الممتاز', 'الدوري اليوناني',
    'الدوري الروسي الممتاز', 'الدوري الدنماركي الممتاز', 'الدوري السويدي الممتاز', 'الدوري النرويجي الممتاز',
    'المباريات الودية', 'ودية للأندية', 'ودية دولية', 'friendly'
];

function isMajorMatch(compName) {
    const compLower = (compName || '').toLowerCase();
    for (const b of BLOCKED_KEYWORDS) {
        if (compLower.includes(b.toLowerCase())) return false;
    }
    for (const m of MAJOR_KEYWORDS) {
        if (compLower.includes(m.toLowerCase())) return true;
    }
    return false;
}

function formatDateTo365(d) {
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    return `${day}/${month}/${year}`;
}

async function fetchLive365Games(dateOffsetDays, dateCategory) {
    try {
        const d = new Date();
        d.setDate(d.getDate() + dateOffsetDays);
        const dateStr = formatDateTo365(d);

        const url = `https://webws.365scores.com/web/games/allscores/?appTypeId=5&langId=27&timezoneName=Asia/Baghdad&userCountryId=114&sports=1&startDate=${dateStr}&endDate=${dateStr}&showOdds=true`;
        console.log(`🌐 Fetching live scores for [${dateCategory} - ${dateStr}] from official 365Scores WebWS (Strict Football sports=1)...`);

        const res = await fetch(url, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } });
        if (!res.ok) {
            console.error(`HTTP error ${res.status} for ${dateCategory}`);
            return [];
        }

        const data = await res.json();
        const rawGames = data.games || [];
        const comps = {};
        (data.competitions || []).forEach(c => { comps[c.id] = c; });

        console.log(`📥 Received ${rawGames.length} raw matches from 365Scores for ${dateCategory}.`);

        const parsed = [];
        for (const g of rawGames) {
            if (g.sportId && g.sportId !== 1) continue; // Strict Football Only!
            if (g.competitionId === 6297 || g.competitionId === 7856) continue; // Strict exclude UEFA Youth & FA Cup Prelims
            const comp = comps[g.competitionId] || {};
            const compName = comp.name || 'كرة القدم';
            if (!isMajorMatch(compName)) continue;

            const home = g.homeCompetitor || {};
            const away = g.awayCompetitor || {};
            const homeName = home.name || 'الفريق';
            const awayName = away.name || 'الفريق';

            const hLow = homeName.toLowerCase();
            const aLow = awayName.toLowerCase();
            if (hLow.includes('تحت 19') || hLow.includes('u19') || hLow.includes('تحت 21') || hLow.includes('u21') ||
                aLow.includes('تحت 19') || aLow.includes('u19') || aLow.includes('تحت 21') || aLow.includes('u21')) continue;

            // Status: statusGroup 3 = LIVE, 4 = FINISHED, 2 = UPCOMING
            const statusGroup = g.statusGroup;
            const statusText = g.statusText || '';
            const gameTimeDisplay = (g.gameTimeDisplay || '').trim();
            const gameTime = g.gameTime;

            const isHalftime = statusText.includes('استراحة') || statusText.includes('بين الشوطين') || statusText === 'شوط' || statusText.toLowerCase().includes('half') || statusText.toLowerCase().includes('ht');
            const isExtraTime = statusText.includes('إضافي') || statusText.toLowerCase().includes('et');
            const isPenalties = statusText.includes('ترجيح') || statusText.toLowerCase().includes('pen');

            let status = 'UPCOMING';
            if (statusGroup === 3 || gameTimeDisplay.includes("'") || statusText.includes('الشوط الأول') || statusText.includes('الشوط الثاني') || isHalftime || isExtraTime || isPenalties) {
                status = 'LIVE';
            } else if (statusGroup === 4 || statusText.includes('انتهت') || statusText.toLowerCase().includes('ft')) {
                status = 'FINISHED';
            }

            const homeScore = (home.score !== undefined && home.score >= 0) ? Math.floor(home.score) : null;
            const awayScore = (away.score !== undefined && away.score >= 0) ? Math.floor(away.score) : null;

            // Formatted 24h start time
            let timeLabel = statusText;
            if (g.startTime) {
                try {
                    const startD = new Date(g.startTime);
                    const hh = String(startD.getHours()).padStart(2, '0');
                    const mm = String(startD.getMinutes()).padStart(2, '0');
                    timeLabel = `${hh}:${mm}`;
                } catch (_) {}
            }

            // Real-time dynamic game minute / status label
            let formattedMinute = null;
            if (isHalftime) {
                formattedMinute = 'استراحة';
            } else if (isPenalties) {
                formattedMinute = 'ركلات ترجيح';
            } else if (isExtraTime) {
                formattedMinute = gameTimeDisplay ? `إضافي ${gameTimeDisplay}` : 'شوط إضافي';
            } else if (gameTimeDisplay) {
                formattedMinute = gameTimeDisplay;
            } else if (gameTime && gameTime > 0) {
                formattedMinute = `${Math.floor(gameTime)}'`;
            } else if (status === 'LIVE') {
                formattedMinute = statusText || 'مباشر';
            }

            // Real Round Number & Stage formatting (e.g. "الجولة 3", "نصف النهائي", "دور المجموعات")
            let formattedRound = '';
            if (g.roundNum && g.roundNum > 0) {
                const prefix = (g.roundName && g.roundName.trim().length > 0) ? g.roundName.trim() : 'الجولة';
                formattedRound = `${prefix} ${g.roundNum}`;
            } else if (g.stageName && g.stageName.trim().length > 0) {
                formattedRound = g.stageName.trim();
            } else if (g.roundName && g.roundName.trim().length > 0 && g.roundName.trim() !== 'الجولة' && g.roundName.trim() !== 'جولة') {
                formattedRound = g.roundName.trim();
            }

            const isUcl = (g.competitionId === 572 || compName.includes('أبطال أوروبا') || compName.includes('Champions League')) &&
                          !compName.includes('شباب') && !compName.includes('19');
            const compLogo = g.competitionId ? `https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitions/${g.competitionId}` : '';
            const homeLogo = home.id ? `https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitors/${home.id}` : '';
            const awayLogo = away.id ? `https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitors/${away.id}` : '';

            parsed.push({
                id: 'm_' + g.id,
                competition: compName,
                homeTeam: homeName,
                homeTeamLogoUrl: homeLogo,
                awayTeam: awayName,
                awayTeamLogoUrl: awayLogo,
                homeScore: homeScore,
                awayScore: awayScore,
                status: status,
                startTimeLabel: timeLabel,
                sport: 'Football',
                channel: isUcl ? 'beIN Sports 1 HD' : (compName.includes('روشن') ? 'SSC 1 HD' : ''),
                commentator: '',
                roundOrStadium: formattedRound,
                roundNumber: formattedRound,
                dateCategory: dateCategory,
                isUcl: isUcl,
                competitionLogoUrl: compLogo,
                streamUrl: '',
                gameMinute: formattedMinute
            });
        }

        return parsed;
    } catch (e) {
        console.error(`Error fetching live matches for ${dateCategory}:`, e);
        return [];
    }
}

async function syncAll() {
    console.log('⚽ Starting Direct Real-Time Sync from 365Scores WebWS...');
    const todayMatches = await fetchLive365Games(0, 'today');
    const tomorrowMatches = await fetchLive365Games(1, 'tomorrow');
    const allMatches = [...todayMatches, ...tomorrowMatches];

    console.log(`✅ Filtered ${allMatches.length} Tier-1 matches (${todayMatches.length} Today, ${tomorrowMatches.length} Tomorrow).`);

    fs.writeFileSync(matchesFile, JSON.stringify(allMatches, null, 2), 'utf8');
    console.log(`💾 Saved to ${matchesFile}`);

    // Compile to Protobuf binary (.pb.gz) for ultra-fast CDN delivery
    try {
        console.log('⚡ Compiling to Google Protocol Buffers binary (matches.pb.gz)...');
        execSync('python3 /storage/emulated/0/CodeOnTheGoProjects/Beouttv/tools/matches_pb_compiler.py', { stdio: 'inherit' });
    } catch (err) {
        console.error('Protobuf compilation error:', err);
    }
}

syncAll().catch(console.error);

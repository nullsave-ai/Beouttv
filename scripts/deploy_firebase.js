const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const zlib = require('zlib');

const keyPath = '/storage/emulated/0/CodeOnTheGoProjects/Beouttv/digite-bda04-firebase-adminsdk-lpwzc-ebd442be66.json';
if (!fs.existsSync(keyPath)) {
    console.error('Error: Service account key not found at:', keyPath);
    process.exit(1);
}

const sa = JSON.parse(fs.readFileSync(keyPath, 'utf8'));

function createJwt() {
    const now = Math.floor(Date.now() / 1000);
    const header = { alg: 'RS256', typ: 'JWT' };
    const payload = {
        iss: sa.client_email,
        sub: sa.client_email,
        aud: 'https://oauth2.googleapis.com/token',
        iat: now,
        exp: now + 3600,
        scope: 'https://www.googleapis.com/auth/firebase.hosting https://www.googleapis.com/auth/firebase.database https://www.googleapis.com/auth/cloud-platform'
    };
    
    const encode = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url');
    const unsignedToken = encode(header) + '.' + encode(payload);
    
    const signer = crypto.createSign('RSA-SHA256');
    signer.update(unsignedToken);
    const signature = signer.sign(sa.private_key, 'base64url');
    return unsignedToken + '.' + signature;
}

async function getAccessToken() {
    const jwt = createJwt();
    const res = await fetch('https://oauth2.googleapis.com/token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
            grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            assertion: jwt
        })
    });
    const data = await res.json();
    return data.access_token;
}

async function deploy() {
    console.log('🚀 Starting BeoutTV Firebase Cloud Deployment...');
    const token = await getAccessToken();
    const siteId = 'digite-bda04';
    const publicDir = '/storage/emulated/0/CodeOnTheGoProjects/Beouttv/public';

    // 1. Sync to Firebase Realtime Database
    try {
        console.log('📡 Syncing Gateway & Channels to Firebase Realtime Database...');
        if (fs.existsSync(path.join(publicDir, 'gateway.json'))) {
            const gatewayData = JSON.parse(fs.readFileSync(path.join(publicDir, 'gateway.json'), 'utf8'));
            await fetch('https://digite-bda04-default-rtdb.firebaseio.com/gateway.json', {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
                body: JSON.stringify(gatewayData)
            });
        }
        console.log('✅ Gateway synced to Realtime Database!');
    } catch (e) {
        console.error('RTDB sync error:', e);
    }

    // 2. Deploy to Firebase Hosting
    console.log('🌐 Deploying to Firebase Hosting (https://' + siteId + '.web.app)...');
    const createVerRes = await fetch(`https://firebasehosting.googleapis.com/v1beta1/sites/${siteId}/versions`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            config: {
                headers: [
                    {
                        glob: '**',
                        headers: {
                            'Access-Control-Allow-Origin': '*',
                            'Cache-Control': 'no-cache, no-store, must-revalidate'
                        }
                    }
                ]
            }
        })
    });
    const verData = await createVerRes.json();
    if (!verData.name) {
        console.error('Version creation error:', verData);
        return;
    }
    const verId = verData.name.split('/').pop();

    const files = fs.readdirSync(publicDir);
    const fileHashes = {};
    const gzippedBuffers = {};

    for (const file of files) {
        if (file === 'channels.json') continue; // Never expose raw channels JSON on public CDN
        const fullPath = path.join(publicDir, file);
        if (fs.statSync(fullPath).isFile()) {
            const rawBuf = fs.readFileSync(fullPath);
            const gzBuf = zlib.gzipSync(rawBuf);
            const hash = crypto.createHash('sha256').update(gzBuf).digest('hex');
            fileHashes['/' + file] = hash;
            gzippedBuffers[hash] = gzBuf;
        }
    }

    const popRes = await fetch(`https://firebasehosting.googleapis.com/v1beta1/sites/${siteId}/versions/${verId}:populateFiles`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ files: fileHashes })
    });
    const popData = await popRes.json();
    const requiredUploads = popData.uploadRequiredHashes || [];
    const uploadUrl = popData.uploadUrl;

    for (const hash of requiredUploads) {
        await fetch(`${uploadUrl}/${hash}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/octet-stream'
            },
            body: gzippedBuffers[hash]
        });
    }

    // Finalize
    await fetch(`https://firebasehosting.googleapis.com/v1beta1/sites/${siteId}/versions/${verId}?update_mask=status`, {
        method: 'PATCH',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ status: 'FINALIZED' })
    });

    // Release
    const relRes = await fetch(`https://firebasehosting.googleapis.com/v1beta1/sites/${siteId}/releases?versionName=sites/${siteId}/versions/${verId}`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    });
    const relData = await relRes.json();

    console.log('====================================================');
    console.log('🎉 تم النشر والرفع بنجاح على استضافة Google Firebase!');
    console.log('🌐 الموقع ولوحة التحكم: https://' + siteId + '.web.app/admin.html');
    console.log('📦 مسار بوابة الإعدادات: https://' + siteId + '.web.app/gateway.json');
    console.log('📺 مسار بيانات القنوات: https://' + siteId + '.web.app/channels.json');
    console.log('⚽ مسار جدول المباريات: https://' + siteId + '.web.app/matches.json');
    console.log('====================================================');
}

deploy().catch(console.error);

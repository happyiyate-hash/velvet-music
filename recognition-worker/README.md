# Velvet Recognition Worker

This is the server-side bridge for Velvet's real song recognition feature.

## Endpoints

- `POST /v1/recognition/audio` — identifies recorded/background music through AudD.
- `POST /v1/recognition/hum` — identifies humming/singing through an ACRCloud project configured for cover/humming recognition.

Both accept a multipart field named `audio`.

## Secrets

Configure these with Cloudflare Worker secrets. Do **not** commit them and do **not** put them in the Android APK:

```text
AUDD_API_TOKEN
ACRCLOUD_HOST
ACRCLOUD_ACCESS_KEY
ACRCLOUD_ACCESS_SECRET
```

ACRCloud's project must have its music bucket and humming/cover-song recognition enabled. AudD handles normal microphone/background-song fingerprint recognition.

## Deploy

```bash
npm install
npx wrangler login
npx wrangler secret put AUDD_API_TOKEN
npx wrangler secret put ACRCLOUD_HOST
npx wrangler secret put ACRCLOUD_ACCESS_KEY
npx wrangler secret put ACRCLOUD_ACCESS_SECRET
npx wrangler deploy
```

The deployed Worker URL becomes Velvet's `VELVET_RECOGNITION_BASE_URL` value, for example:

```text
VELVET_RECOGNITION_BASE_URL=https://velvet-recognition.<your-account>.workers.dev/
```

The Android client already sends real WAV microphone captures to this contract. The provider credentials remain server-side.

interface Env {
  AUDD_API_TOKEN: string
  ACRCLOUD_HOST: string
  ACRCLOUD_ACCESS_KEY: string
  ACRCLOUD_ACCESS_SECRET: string
}

type RecognitionResponse = {
  success: boolean
  confidence: number
  song?: {
    id: string
    title: string
    artist: string
    album: string
    artworkUrl: string | null
    durationMs: number
    isrc: string | null
    spotifyUrl: string | null
    appleMusicUrl: string | null
    youtubeMusicUrl: string | null
    audiomackUrl: string | null
  }
  error?: string
}

const JSON_HEADERS = { "content-type": "application/json; charset=utf-8" }

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const cors = {
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "POST, OPTIONS",
      "access-control-allow-headers": "content-type",
    }

    if (request.method === "OPTIONS") return new Response(null, { headers: cors })
    if (request.method !== "POST") return json({ success: false, confidence: 0, error: "POST required" }, 405, cors)

    const path = new URL(request.url).pathname
    const form = await request.formData()
    const audio = form.get("audio")
    if (!(audio instanceof File)) {
      return json({ success: false, confidence: 0, error: "Missing audio file" }, 400, cors)
    }

    // Keep the mobile capture bounded. The Android client normally sends ~256 KB for 8s WAV.
    if (audio.size > 5_000_000) {
      return json({ success: false, confidence: 0, error: "Audio file is too large" }, 413, cors)
    }

    try {
      const result = path.endsWith("/hum")
        ? await recognizeHumming(audio, env)
        : path.endsWith("/audio")
          ? await recognizeAmbient(audio, env)
          : { success: false, confidence: 0, error: "Unknown recognition endpoint" } satisfies RecognitionResponse

      return json(result, result.success ? 200 : 422, cors)
    } catch (error) {
      const message = error instanceof Error ? error.message : "Recognition provider failed"
      return json({ success: false, confidence: 0, error: message }, 502, cors)
    }
  },
}

async function recognizeAmbient(audio: File, env: Env): Promise<RecognitionResponse> {
  if (!env.AUDD_API_TOKEN) throw new Error("AUDD_API_TOKEN is not configured")

  const body = new FormData()
  body.append("api_token", env.AUDD_API_TOKEN)
  body.append("return", "apple_music,spotify")
  body.append("file", audio, audio.name || "velvet-capture.wav")

  const response = await fetch("https://api.audd.io/", { method: "POST", body })
  const data = await response.json() as any
  if (!response.ok || data.status !== "success") {
    return { success: false, confidence: 0, error: data.error?.error_message || "AudD did not recognize the audio" }
  }
  if (!data.result) return { success: false, confidence: 0, error: "No song match found" }

  const result = data.result
  const title = String(result.title || "").trim()
  const artist = String(result.artist || "").trim()
  if (!title || !artist) return { success: false, confidence: 0, error: "Recognition returned incomplete metadata" }

  return {
    success: true,
    confidence: 100,
    song: {
      id: result.isrc || `${artist}:${title}`,
      title,
      artist,
      album: String(result.album || "Unknown Album"),
      artworkUrl: result.apple_music?.artwork?.url || null,
      durationMs: Number(result.apple_music?.durationInMillis || 0),
      isrc: result.isrc || null,
      spotifyUrl: result.spotify?.external_urls?.spotify || null,
      appleMusicUrl: result.apple_music?.url || null,
      youtubeMusicUrl: `https://music.youtube.com/search?q=${encodeURIComponent(`${artist} ${title}`)}`,
      audiomackUrl: `https://audiomack.com/search?q=${encodeURIComponent(`${artist} ${title}`)}`,
    },
  }
}

async function recognizeHumming(audio: File, env: Env): Promise<RecognitionResponse> {
  if (!env.ACRCLOUD_HOST || !env.ACRCLOUD_ACCESS_KEY || !env.ACRCLOUD_ACCESS_SECRET) {
    throw new Error("ACRCLOUD_HOST, ACRCLOUD_ACCESS_KEY and ACRCLOUD_ACCESS_SECRET are required")
  }

  const httpMethod = "POST"
  const httpUri = "/v1/identify"
  const dataType = "audio"
  const signatureVersion = "1"
  const timestamp = Math.floor(Date.now() / 1000).toString()
  const stringToSign = [httpMethod, httpUri, env.ACRCLOUD_ACCESS_KEY, dataType, signatureVersion, timestamp].join("\n")
  const signature = await hmacSha1Base64(env.ACRCLOUD_ACCESS_SECRET, stringToSign)

  const body = new FormData()
  body.append("access_key", env.ACRCLOUD_ACCESS_KEY)
  body.append("sample_bytes", String(audio.size))
  body.append("timestamp", timestamp)
  body.append("signature", signature)
  body.append("data_type", dataType)
  body.append("signature_version", signatureVersion)
  body.append("sample", audio, audio.name || "velvet-hum.wav")

  const host = env.ACRCLOUD_HOST.replace(/^https?:\/\//, "").replace(/\/$/, "")
  const response = await fetch(`https://${host}${httpUri}`, { method: httpMethod, body })
  const data = await response.json() as any

  if (!response.ok || data?.status?.code !== 0) {
    return {
      success: false,
      confidence: 0,
      error: data?.status?.msg || "ACRCloud did not recognize the melody",
    }
  }

  const candidate = data?.metadata?.humming?.[0] || data?.metadata?.music?.[0]
  if (!candidate) return { success: false, confidence: 0, error: "No humming match found" }

  const title = String(candidate.title || "").trim()
  const artist = String(candidate.artists?.[0]?.name || candidate.artist || "").trim()
  if (!title || !artist) return { success: false, confidence: 0, error: "Recognition returned incomplete metadata" }

  const score = Number(candidate.score)
  const spotifyId = candidate.external_metadata?.spotify?.track?.id
  const spotifyUrl = spotifyId ? `https://open.spotify.com/track/${spotifyId}` : null
  const youtubeId = candidate.external_metadata?.youtube?.vid
  const youtubeUrl = youtubeId
    ? `https://music.youtube.com/watch?v=${youtubeId}`
    : `https://music.youtube.com/search?q=${encodeURIComponent(`${artist} ${title}`)}`

  return {
    success: true,
    confidence: Number.isFinite(score) ? Math.round(Math.max(0, Math.min(1, score)) * 100) : 0,
    song: {
      id: candidate.acrid || `${artist}:${title}`,
      title,
      artist,
      album: String(candidate.album?.name || "Unknown Album"),
      artworkUrl: null,
      durationMs: Number(candidate.duration_ms || 0),
      isrc: candidate.external_ids?.isrc || null,
      spotifyUrl,
      appleMusicUrl: null,
      youtubeMusicUrl: youtubeUrl,
      audiomackUrl: `https://audiomack.com/search?q=${encodeURIComponent(`${artist} ${title}`)}`,
    },
  }
}

async function hmacSha1Base64(secret: string, message: string): Promise<string> {
  const encoder = new TextEncoder()
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-1" },
    false,
    ["sign"],
  )
  const signature = await crypto.subtle.sign("HMAC", key, encoder.encode(message))
  let binary = ""
  for (const byte of new Uint8Array(signature)) binary += String.fromCharCode(byte)
  return btoa(binary)
}

function json(payload: unknown, status: number, extraHeaders: Record<string, string> = {}) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { ...JSON_HEADERS, ...extraHeaders },
  })
}

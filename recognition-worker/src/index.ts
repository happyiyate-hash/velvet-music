interface Env {
  AUDD_API_TOKEN: string
  ACRCLOUD_HOST: string
  ACRCLOUD_ACCESS_KEY: string
  ACRCLOUD_ACCESS_SECRET: string
}

type RecognitionSong = {
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
  soundcloudUrl: string | null
  boomplayUrl: string | null
}

type RecognitionResponse = {
  success: boolean
  confidence: number
  requestId?: string
  song?: RecognitionSong
  error?: string
}

type BatchProviderResult = RecognitionResponse

type BatchResponse = {
  success: boolean
  requestId: string
  results: { audd: BatchProviderResult; acrcloud: BatchProviderResult }
  error?: string
}

const JSON_HEADERS = { "content-type": "application/json; charset=utf-8" }
const PROVIDER_TIMEOUT_MS = 18_000
const MAX_AUDIO_BYTES = 5_000_000

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const cors = { "access-control-allow-origin": "*", "access-control-allow-methods": "GET, POST, OPTIONS", "access-control-allow-headers": "content-type" }
    const requestId = crypto.randomUUID()
    const startedAt = Date.now()
    const path = new URL(request.url).pathname
    log(requestId, "REQUEST_RECEIVED", { method: request.method, path })
    if (request.method === "OPTIONS") return new Response(null, { headers: cors })
    if (request.method === "GET" && path === "/health") return json({ ok: true, service: "velvet-recognition", requestId }, 200, cors, requestId)
    if (request.method !== "POST") return json({ success: false, confidence: 0, requestId, error: "POST required" }, 405, cors, requestId)
    if (!path.endsWith("/hum") && !path.endsWith("/audio") && !path.endsWith("/batch")) return json({ success: false, confidence: 0, requestId, error: "Unknown recognition endpoint" }, 404, cors, requestId)
    try {
      const form = await request.formData()
      const audio = form.get("audio")
      if (!(audio instanceof File)) return json({ success: false, confidence: 0, requestId, error: "Missing audio file" }, 400, cors, requestId)
      log(requestId, "AUDIO_VALIDATED", { bytes: audio.size, contentType: audio.type || "unknown", name: audio.name || "unknown" })
      if (audio.size > MAX_AUDIO_BYTES) return json({ success: false, confidence: 0, requestId, error: "Audio file is too large" }, 413, cors, requestId)
      if (path.endsWith("/batch")) {
        const result = await recognizeBatch(audio, env, requestId)
        log(requestId, "BATCH_RESPONSE_SENT", { elapsedMs: Date.now() - startedAt, auddSuccess: result.results.audd.success, acrcloudSuccess: result.results.acrcloud.success })
        return json(result, 200, cors, requestId)
      }
      const result = path.endsWith("/hum") ? await recognizeHumming(audio, env, requestId) : await recognizeAmbient(audio, env, requestId)
      const finalResult = { ...result, requestId }
      log(requestId, result.success ? "MATCH_FOUND" : "NO_MATCH", { elapsedMs: Date.now() - startedAt, error: result.error })
      return json(finalResult, result.success ? 200 : 422, cors, requestId)
    } catch (error) {
      const message = error instanceof Error ? error.message : "Recognition provider failed"
      log(requestId, "REQUEST_FAILED", { elapsedMs: Date.now() - startedAt, error: message })
      return json({ success: false, confidence: 0, requestId, error: message }, 502, cors, requestId)
    }
  },
}

async function recognizeBatch(audio: File, env: Env, requestId: string): Promise<BatchResponse> {
  const [audd, acrcloud] = await Promise.allSettled([
    recognizeAmbient(audio, env, requestId),
    recognizeHumming(audio, env, requestId),
  ])
  const auddResult = settledProviderResult(audd, "AudD", requestId)
  const acrcloudResult = settledProviderResult(acrcloud, "ACRCloud", requestId)
  return { success: auddResult.success || acrcloudResult.success, requestId, results: { audd: auddResult, acrcloud: acrcloudResult } }
}

function settledProviderResult(result: PromiseSettledResult<RecognitionResponse>, provider: string, requestId: string): BatchProviderResult {
  if (result.status === "fulfilled") return result.value
  const message = result.reason instanceof Error ? result.reason.message : `${provider} request failed`
  log(requestId, "PROVIDER_REQUEST_FAILED", { provider, error: message })
  return { success: false, confidence: 0, error: `${provider}: ${message}` }
}

async function recognizeAmbient(audio: File, env: Env, requestId: string): Promise<RecognitionResponse> {
  if (!env.AUDD_API_TOKEN) throw new Error("AUDD_API_TOKEN is not configured")
  const body = new FormData()
  body.append("api_token", env.AUDD_API_TOKEN)
  body.append("return", "apple_music,spotify")
  body.append("file", audio, audio.name || "velvet-capture.wav")
  log(requestId, "PROVIDER_REQUEST_STARTED", { provider: "AudD" })
  const started = Date.now()
  const response = await fetchWithTimeout("https://api.audd.io/", { method: "POST", body })
  const data = await readJsonSafely(response)
  log(requestId, "PROVIDER_RESPONSE_RECEIVED", { provider: "AudD", status: response.status, elapsedMs: Date.now() - started })
  if (!response.ok || data.status !== "success") return { success: false, confidence: 0, error: `AudD: ${data.error?.error_message || "recognition failed"}` }
  if (!data.result) return { success: false, confidence: 0, error: "AudD: no song match found" }
  const result = data.result
  const title = String(result.title || "").trim(); const artist = String(result.artist || "").trim()
  if (!title || !artist) return { success: false, confidence: 0, error: "AudD: recognition returned incomplete metadata" }
  return { success: true, confidence: 100, song: { id: result.isrc || `${artist}:${title}`, title, artist, album: String(result.album || "Unknown Album"), artworkUrl: result.apple_music?.artwork?.url || null, durationMs: Number(result.apple_music?.durationInMillis || 0), isrc: result.isrc || null, spotifyUrl: result.spotify?.external_urls?.spotify || null, appleMusicUrl: result.apple_music?.url || null, youtubeMusicUrl: `https://music.youtube.com/search?q=${encodeURIComponent(`${artist} ${title}`)}`, audiomackUrl: `https://audiomack.com/search?q=${encodeURIComponent(`${artist} ${title}`)}`, soundcloudUrl: `https://soundcloud.com/search?q=${encodeURIComponent(`${artist} ${title}`)}`, boomplayUrl: `https://www.boomplay.com/search/default-${encodeURIComponent(`${artist} ${title}`)}` } }
}

async function recognizeHumming(audio: File, env: Env, requestId: string): Promise<RecognitionResponse> {
  if (!env.ACRCLOUD_HOST || !env.ACRCLOUD_ACCESS_KEY || !env.ACRCLOUD_ACCESS_SECRET) throw new Error("ACRCLOUD_HOST, ACRCLOUD_ACCESS_KEY and ACRCLOUD_ACCESS_SECRET are required")
  const httpMethod = "POST", httpUri = "/v1/identify", dataType = "audio", signatureVersion = "1", timestamp = Math.floor(Date.now() / 1000).toString()
  const stringToSign = [httpMethod, httpUri, env.ACRCLOUD_ACCESS_KEY, dataType, signatureVersion, timestamp].join("\n")
  const signature = await hmacSha1Base64(env.ACRCLOUD_ACCESS_SECRET, stringToSign)
  const body = new FormData()
  body.append("access_key", env.ACRCLOUD_ACCESS_KEY); body.append("sample_bytes", String(audio.size)); body.append("timestamp", timestamp); body.append("signature", signature); body.append("data_type", dataType); body.append("signature_version", signatureVersion); body.append("sample", audio, audio.name || "velvet-hum.wav")
  const host = env.ACRCLOUD_HOST.replace(/^https?:\/\//, "").replace(/\/$/, "")
  log(requestId, "PROVIDER_REQUEST_STARTED", { provider: "ACRCloud" })
  const started = Date.now()
  const response = await fetchWithTimeout(`https://${host}${httpUri}`, { method: httpMethod, body })
  const data = await readJsonSafely(response)
  log(requestId, "PROVIDER_RESPONSE_RECEIVED", { provider: "ACRCloud", status: response.status, providerCode: data?.status?.code, providerMessage: data?.status?.msg, metadataKeys: data?.metadata ? Object.keys(data.metadata) : [], elapsedMs: Date.now() - started })
  if (!response.ok || Number(data?.status?.code) !== 0) return { success: false, confidence: 0, error: `ACRCloud: ${data?.status?.msg || `HTTP ${response.status}`}` }
  const candidate = data?.metadata?.humming?.[0] || data?.metadata?.music?.[0]
  if (!candidate) return { success: false, confidence: 0, error: "ACRCloud: processed audio but found no matching song" }
  const title = String(candidate.title || "").trim(); const artist = String(candidate.artists?.[0]?.name || candidate.artist || "").trim()
  if (!title || !artist) return { success: false, confidence: 0, error: "ACRCloud: recognition returned incomplete metadata" }
  const score = Number(candidate.score), spotifyId = candidate.external_metadata?.spotify?.track?.id, youtubeId = candidate.external_metadata?.youtube?.vid
  return { success: true, confidence: Number.isFinite(score) ? Math.round(Math.max(0, Math.min(1, score)) * 100) : 0, song: { id: candidate.acrid || `${artist}:${title}`, title, artist, album: String(candidate.album?.name || "Unknown Album"), artworkUrl: null, durationMs: Number(candidate.duration_ms || 0), isrc: candidate.external_ids?.isrc || null, spotifyUrl: spotifyId ? `https://open.spotify.com/track/${spotifyId}` : null, appleMusicUrl: null, youtubeMusicUrl: youtubeId ? `https://music.youtube.com/watch?v=${youtubeId}` : `https://music.youtube.com/search?q=${encodeURIComponent(`${artist} ${title}`)}`, audiomackUrl: `https://audiomack.com/search?q=${encodeURIComponent(`${artist} ${title}`)}`, soundcloudUrl: `https://soundcloud.com/search?q=${encodeURIComponent(`${artist} ${title}`)}`, boomplayUrl: `https://www.boomplay.com/search/default-${encodeURIComponent(`${artist} ${title}`)}` } }
}

async function fetchWithTimeout(input: RequestInfo | URL, init: RequestInit): Promise<Response> {
  const controller = new AbortController(); const timeout = setTimeout(() => controller.abort(), PROVIDER_TIMEOUT_MS)
  try { return await fetch(input, { ...init, signal: controller.signal }) } finally { clearTimeout(timeout) }
}
async function readJsonSafely(response: Response): Promise<any> { const text = await response.text(); try { return JSON.parse(text) } catch { return { error: { error_message: text.slice(0, 300) || `HTTP ${response.status}` } } } }
async function hmacSha1Base64(secret: string, message: string): Promise<string> { const encoder = new TextEncoder(); const key = await crypto.subtle.importKey("raw", encoder.encode(secret), { name: "HMAC", hash: "SHA-1" }, false, ["sign"]); const signature = await crypto.subtle.sign("HMAC", key, encoder.encode(message)); let binary = ""; for (const byte of new Uint8Array(signature)) binary += String.fromCharCode(byte); return btoa(binary) }
function log(requestId: string, stage: string, details: Record<string, unknown> = {}) { console.log(JSON.stringify({ service: "velvet-recognition", requestId, stage, ...details })) }
function json(payload: unknown, status: number, extraHeaders: Record<string, string> = {}, requestId?: string) { return new Response(JSON.stringify(payload), { status, headers: { ...JSON_HEADERS, ...extraHeaders, ...(requestId ? { "x-velvet-request-id": requestId } : {}) } }) }

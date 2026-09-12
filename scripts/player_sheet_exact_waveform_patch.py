from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
s = path.read_text(encoding="utf-8")

# The patch is intentionally idempotent because the PlayerSheet workflow may run again
# after other queue performance changes have already landed.
if "ExactAudioWaveformProgress(" in s:
    print("Exact waveform call is already applied; nothing to change.")
else:
    needle = """        if (progressComponentAlpha > 0f) {
            NowPlayingWaveformProgress(
"""
    replacement = """        if (progressComponentAlpha > 0f) {
            ExactAudioWaveformProgress(
                audioUri = track.contentUri,
"""
    if needle not in s:
        raise SystemExit("waveform call site not found")
    s = s.replace(needle, replacement, 1)
    path.write_text(s, encoding="utf-8")

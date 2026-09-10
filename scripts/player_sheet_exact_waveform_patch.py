from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
s = path.read_text(encoding="utf-8")

needle = """        if (progressComponentAlpha > 0f) {\n            NowPlayingWaveformProgress(\n"""
replacement = """        if (progressComponentAlpha > 0f) {\n            ExactAudioWaveformProgress(\n                audioUri = track.contentUri,\n"""
if needle not in s:
    raise SystemExit("waveform call site not found")
s = s.replace(needle, replacement, 1)
path.write_text(s, encoding="utf-8")

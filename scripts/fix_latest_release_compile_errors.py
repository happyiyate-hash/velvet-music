from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# 1) VelvetAudioEngine: avoid nullable ByteArray extension ambiguity and make the
#    null/empty guard explicit before using waveform.size.
engine = ROOT / "app/src/main/java/com/example/audio/VelvetAudioEngine.kt"
s = engine.read_text(encoding="utf-8")
s = s.replace("if (waveform.isNullOrEmpty()) return", "if (waveform == null || waveform.isEmpty()) return")
engine.write_text(s, encoding="utf-8")

# 2) PlayerSheet: ExactAudioWaveformProgress no longer exposes the three alpha
#    parameters. Keep the existing single waveform/progress component and remove
#    only the stale named arguments.
player = ROOT / "app/src/main/java/com/example/ui/PlayerSheet.kt"
s = player.read_text(encoding="utf-8")
s = s.replace("import androidx.compose.foundation.gestures.detectHorizontalDragGestures\n", "import androidx.compose.foundation.gestures.detectHorizontalDragGestures\nimport androidx.compose.foundation.gestures.detectDragGestures\n")
s = s.replace("                waveformAlpha = movingWaveformAlpha * progressComponentAlpha,\n                timestampAlpha = movingTimestampAlpha * progressComponentAlpha,\n                progressAlpha = progressComponentAlpha,\n", "")

# The queue row callback uses its index for visual reorder math. Use itemsIndexed
# rather than referring to an index that the items{} lambda does not provide.
old = '''                    items(\n                        items = orderedQueueItems,\n                        key = { it.id }\n                    ,\n                        contentType = { "track_row" }) { queueTrack ->'''
new = '''                    itemsIndexed(\n                        items = orderedQueueItems,\n                        key = { _, item -> item.id },\n                        contentType = { _, _ -> "track_row" }\n                    ) { queueIndex, queueTrack ->'''
if old not in s:
    raise SystemExit("Expected queue items block was not found")
s = s.replace(old, new, 1)

player.write_text(s, encoding="utf-8")
print("Latest release compile fixes applied.")

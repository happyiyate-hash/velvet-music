from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# 1) VelvetAudioEngine: make the nullable waveform guard explicit so the
#    compiler can safely accept subsequent waveform.size / iteration usage.
engine = ROOT / "app/src/main/java/com/example/audio/VelvetAudioEngine.kt"
s = engine.read_text(encoding="utf-8")
s = s.replace("if (waveform.isNullOrEmpty()) return", "if (waveform == null || waveform.isEmpty()) return")
engine.write_text(s, encoding="utf-8")

# 2) PlayerSheet: ExactAudioWaveformProgress exposes no waveform/timestamp/progress
#    alpha parameters. Remove only those stale arguments and keep one waveform.
player = ROOT / "app/src/main/java/com/example/ui/PlayerSheet.kt"
s = player.read_text(encoding="utf-8")
s = s.replace(
    "import androidx.compose.foundation.gestures.detectHorizontalDragGestures\n",
    "import androidx.compose.foundation.gestures.detectHorizontalDragGestures\nimport androidx.compose.foundation.gestures.detectDragGestures\n",
    1,
)
s = s.replace("import androidx.compose.foundation.gestures.detectDragGestures\nimport androidx.compose.foundation.gestures.detectDragGestures\n", "import androidx.compose.foundation.gestures.detectDragGestures\n")
s = s.replace("                waveformAlpha = movingWaveformAlpha * progressComponentAlpha,\n", "")
s = s.replace("                timestampAlpha = movingTimestampAlpha * progressComponentAlpha,\n", "")
s = s.replace("                progressAlpha = progressComponentAlpha,\n", "")

# The queue row uses queueIndex for visual reorder math. Convert the queue
# iteration to itemsIndexed so the index is actually supplied by Compose.
old = '''                    items(\n                        items = orderedQueueItems,\n                        key = { it.id }\n                    ,\n                        contentType = { "track_row" }) { queueTrack ->'''
new = '''                    itemsIndexed(\n                        items = orderedQueueItems,\n                        key = { _, item -> item.id },\n                        contentType = { _, _ -> "track_row" }\n                    ) { queueIndex, queueTrack ->'''
if old in s:
    s = s.replace(old, new, 1)
else:
    # Be idempotent if a previous run already converted this block.
    s = s.replace(
        '''                    items(\n                        items = orderedQueueItems,\n                        key = { it.id }\n                    ) { queueTrack ->''',
        new,
        1,
    )

player.write_text(s, encoding="utf-8")
print("Latest release compile fixes applied.")

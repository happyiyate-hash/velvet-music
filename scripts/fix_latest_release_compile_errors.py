from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

engine = ROOT / "app/src/main/java/com/example/audio/VelvetAudioEngine.kt"
s = engine.read_text(encoding="utf-8")
s = s.replace("if (waveform.isNullOrEmpty()) return", "if (waveform == null || waveform.isEmpty()) return")
engine.write_text(s, encoding="utf-8")

player = ROOT / "app/src/main/java/com/example/ui/PlayerSheet.kt"
s = player.read_text(encoding="utf-8")

if "import androidx.compose.foundation.gestures.detectDragGestures\n" not in s:
    anchor = "import androidx.compose.foundation.gestures.detectHorizontalDragGestures\n"
    if anchor in s:
        s = s.replace(anchor, anchor + "import androidx.compose.foundation.gestures.detectDragGestures\n", 1)

if "import androidx.compose.foundation.lazy.itemsIndexed\n" not in s:
    anchor = "import androidx.compose.foundation.lazy.items\n"
    if anchor in s:
        s = s.replace(anchor, anchor + "import androidx.compose.foundation.lazy.itemsIndexed\n", 1)

s = re.sub(r"^\s*waveformAlpha\s*=.*\n", "", s, flags=re.MULTILINE)
s = re.sub(r"^\s*timestampAlpha\s*=.*\n", "", s, flags=re.MULTILINE)
s = re.sub(r"^\s*progressAlpha\s*=.*\n", "", s, flags=re.MULTILINE)

pattern = re.compile(
    r"(?P<indent>\s*)items\(\s*"
    r"items\s*=\s*orderedQueueItems\s*,\s*"
    r"key\s*=\s*\{\s*it\.id\s*\}\s*,?\s*"
    r"contentType\s*=\s*\{\s*\"track_row\"\s*\}\s*\)\s*"
    r"\{\s*queueTrack\s*->",
    re.MULTILINE,
)
replacement = (
    r"\g<indent>itemsIndexed(\n"
    r"\g<indent>    items = orderedQueueItems,\n"
    r"\g<indent>    key = { _, item -> item.id },\n"
    r"\g<indent>    contentType = { _, _ -> \"track_row\" }\n"
    r"\g<indent>) { queueIndex, queueTrack ->"
)
s, count = pattern.subn(replacement, s, count=1)

if count == 0:
    simple = re.compile(
        r"(?P<indent>\s*)items\(\s*"
        r"items\s*=\s*orderedQueueItems\s*,\s*"
        r"key\s*=\s*\{\s*it\.id\s*\}\s*\)\s*"
        r"\{\s*queueTrack\s*->",
        re.MULTILINE,
    )
    s, count = simple.subn(replacement, s, count=1)

player.write_text(s, encoding="utf-8")
print(f"Latest release compile fixes applied; queue conversion count={count}.")

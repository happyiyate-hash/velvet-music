from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# Remove the obsolete second Stage-1 progress bar. The moving
# NowPlayingWaveformProgress instance is now the only expanded-state progress UI.
start_marker = "        // Sleek progress bar in Stage 1 below the title:\n"
end_marker = "        // 4. COMPACT CONTROLS IN STAGE 2"
if start_marker in text:
    start = text.index(start_marker)
    end = text.index(end_marker, start)
    text = text[:start] + text[end:]

if "// Sleek progress bar in Stage 1 below the title:" in text:
    raise SystemExit("obsolete Stage-1 progress bar still present")

path.write_text(text, encoding="utf-8")
print("PlayerSheet v4b cleanup applied")

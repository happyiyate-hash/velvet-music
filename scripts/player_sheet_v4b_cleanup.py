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

# v4b uses Modifier.zIndex() for the compact Up Next foreground layer.
# Keep this idempotent so the cleanup workflow can safely run more than once.
z_import = "import androidx.compose.ui.zIndex\n"
if z_import not in text:
    anchor = "import androidx.compose.ui.Modifier\n"
    if anchor not in text:
        raise SystemExit("PlayerSheet import anchor not found")
    text = text.replace(anchor, anchor + z_import, 1)

path.write_text(text, encoding="utf-8")
print("PlayerSheet v4b cleanup + zIndex import applied")

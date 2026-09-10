from pathlib import Path

p = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
s = p.read_text(encoding="utf-8")

# The restored reference background uses themeColors.bgBottom for the artwork dissolve.
# The previous experimental animated background state was removed, but five stale
# references remained in that fade. Replace only those stale references.
s = s.replace("animatedPlayerBackground.copy(", "themeColors.bgBottom.copy(")

# Remove any stale standalone names left by earlier background experiments.
s = s.replace("playerBackgroundTop", "themeColors.bgBottom")
s = s.replace("playerBackgroundBottom", "themeColors.bgBottom")
s = s.replace("animatedPlayerBackground", "themeColors.bgBottom")

# animateColorAsState is not part of the restored reference implementation.
s = s.replace("import androidx.compose.animation.animateColorAsState\n", "")
s = s.replace("import androidx.compose.animation.core.animateColorAsState\n", "")

if "animatedPlayerBackground" in s or "playerBackgroundTop" in s or "playerBackgroundBottom" in s:
    raise SystemExit("Stale PlayerSheet background references remain; refusing to commit.")

# Safety checks: never rewrite the file unless the restored reference background is present.
required = [
    "themeColors.bgTop",
    "themeColors.bgMidUpper",
    "themeColors.bgMidLower",
    "themeColors.bgBottom",
]
missing = [token for token in required if token not in s]
if missing:
    raise SystemExit(f"Reference background is missing: {missing}; refusing to commit.")

p.write_text(s, encoding="utf-8")
print("Fixed stale PlayerSheet background references while preserving the reference rendering.")

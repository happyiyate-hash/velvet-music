from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# v6 inserted the new fixed dissolve but could leave the previous artwork-bound dissolve
# behind it. Remove only the second artworkFadeAlpha block, keeping the fixed one.
marker = "if (artworkFadeAlpha > 0f) {"
positions = []
start = 0
while True:
    i = text.find(marker, start)
    if i < 0:
        break
    positions.append(i)
    start = i + len(marker)

if len(positions) > 1:
    i = positions[1]
    brace_start = text.find("{", i)
    depth = 0
    end = None
    for j in range(brace_start, len(text)):
        if text[j] == "{":
            depth += 1
        elif text[j] == "}":
            depth -= 1
            if depth == 0:
                end = j + 1
                break
    if end is None:
        raise SystemExit("Could not find the end of duplicate artwork fade block")
    # Also consume the whitespace/newline immediately after the duplicate block.
    while end < len(text) and text[end] in " \t":
        end += 1
    if end < len(text) and text[end] == "\n":
        end += 1
    text = text[:i] + text[end:]

path.write_text(text, encoding="utf-8")
print("PlayerSheet v7 cleanup applied: removed duplicate artwork dissolve block.")

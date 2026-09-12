from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

# Keep the PlayerSheet completely untouched. This repair only controls the main-app
# background and the bottom navigation surface.
app_path = ROOT / "app/src/main/java/com/example/ui/VelvetApp.kt"
app = app_path.read_text()

mesh_import = "import com.example.mesh.SoundCatchMeshBackground\n"
app = app.replace(mesh_import, "", 1)

mesh_block = '''        if (selectedTab != 0) {
            SoundCatchMeshBackground(
                dominantColor = VelvetOffBloodTop,
                secondaryColor = VelvetBloodPlum,
                audioTelemetry = telemetry,
                isPlaying = isPlaying,
                isSoundCatchEnabled = isSoundCatchEnabled
            )
        }

'''
app = app.replace(mesh_block, "", 1)

# The root already uses VelvetAshGrayDark. Make the Scaffold itself use the same
# solid color so every main page (Music/Home, Explore, Videos) has the same base.
app = app.replace(
    "            containerColor = Color.Transparent,\n",
    "            containerColor = VelvetAshGrayDark,\n",
    1,
)

app_path.write_text(app)

nav_path = ROOT / "app/src/main/java/com/example/ui/AshGlassComponents.kt"
nav = nav_path.read_text()

nav_import = "import com.example.ui.theme.VelvetBrightCrimson\n"
if "import com.example.ui.theme.VelvetAshGrayDark\n" not in nav:
    nav = nav.replace(
        nav_import,
        nav_import + "import com.example.ui.theme.VelvetAshGrayDark\n",
        1,
    )

old_nav_background = '''            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xB8231C28),
                        Color(0xC817121D),
                        Color(0xD80E0A14)
                    )
                )
            )
'''
new_nav_background = '''            // Use the exact main-app background as the navigation bar base.
            // Keep the existing glass/highlight layers below untouched.
            drawRect(color = VelvetAshGrayDark)
'''
nav = nav.replace(old_nav_background, new_nav_background, 1)

nav_path.write_text(nav)

#!/usr/bin/env python3
"""بيرتّب الملفات المسطّحة في الجذر على شكل مشروع Gradle/Android عادي وقت البناء."""
import os, re, shutil, sys

M = "app/src/main/"
BYNAME = {
    "app-build.gradle.kts": "app/build.gradle.kts",
    "phone-release.jks": "app/phone-release.jks",
    "AndroidManifest.xml": M + "AndroidManifest.xml",
    "strings.xml": M + "res/values/strings.xml",
    "colors.xml": M + "res/values/colors.xml",
    "themes.xml": M + "res/values/themes.xml",
    "ic_launcher.xml": M + "res/mipmap-anydpi-v26/ic_launcher.xml",
    "ic_launcher_round.xml": M + "res/mipmap-anydpi-v26/ic_launcher_round.xml",
    "ic_launcher_foreground.xml": M + "res/drawable/ic_launcher_foreground.xml",
    "ic_stat_phone.xml": M + "res/drawable/ic_stat_phone.xml",
}
KEEP = {"build.gradle.kts", "settings.gradle.kts", "gradle.properties", "README.md", "assemble.py", ".gitignore"}

count = 0
for f in sorted(os.listdir(".")):
    if not os.path.isfile(f) or f in KEEP:
        continue
    if f.endswith(".kt"):
        src = open(f, encoding="utf-8").read()
        m = re.search(r"^package\s+([\w.]+)", src, re.M)
        if not m:
            sys.exit("no package in " + f)
        dest = M + "java/" + m.group(1).replace(".", "/") + "/" + f
    elif f in BYNAME:
        dest = BYNAME[f]
    else:
        print("تجاهل:", f)
        continue
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    shutil.copy(f, dest)
    count += 1
print("تم ترتيب", count, "ملف")

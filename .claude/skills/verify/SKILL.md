---
name: verify
description: Verify TechWrap TV changes before handing them back. Runs the JVM unit tests, the debug build, and the English/Arabic string parity check; with "release", also builds the R8 release APK and confirms the FFmpeg audio renderer survived shrinking. Use after code changes, before saying work is done.
---

Run from the project root. On Windows use Git Bash. Commands can take several minutes, so use a 10-minute timeout.

1. `./gradlew testDebugUnitTest assembleDebug --console=plain -q`
   - On failure, read `app/build/test-results/testDebugUnitTest/*.xml` for the failing test and message. Report them and don't paper over them.
2. `python .claude/hooks/check_strings.py --all`
   - Every UI string must exist in both `res/values/strings.xml` and `res/values-ar/strings.xml` with the same placeholders. Add any missing Arabic translations.
3. Only if the user said `release` ($ARGUMENTS contains "release"), or the change touches Gradle, ProGuard, Media3/FFmpeg, reflection or Serializable models:
   - `./gradlew assembleRelease --console=plain -q`
   - Open `app/build/outputs/apk/release/app-release-unsigned.apk` (or the signed one) as a zip and confirm `classes*.dex` contains `FfmpegAudioRenderer` and that `lib/*/libffmpegJNI.so` exists. If either is missing, R8 stripped the decoder and MP2/AC-3 audio will be silent.

Report what passed and what failed, with the exact errors. State clearly what still needs testing on a real phone: no emulator is set up, so playback, picture-in-picture, subtitles and the right-to-left layout can't be verified here.

@echo off

::: clean, APK
call gradlew clean ^
 :app:assembleQuad5Release

pause

@echo off

::: clean, APK
call gradlew clean ^
 :app:publishQuad5ReleaseApk

pause

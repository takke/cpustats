@echo off

::: clean, APK
call gradlew clean ^
 :app:publishReleaseApk

pause

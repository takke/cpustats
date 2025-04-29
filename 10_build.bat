@echo off

::: clean, aab, APK
call gradlew clean ^
 test ^
 :app:bundlePublishRelease ^
 :app:publishReleaseApk

pause

@echo off

::: clean, aab, APK
call gradlew clean ^
 :app:bundlePublishRelease ^
 :app:publishReleaseApk

pause

@echo off

::: clean, aab, APK
call gradlew clean ^
 :app:bundlePublishQuad5Release ^
 :app:publishQuad5ReleaseApk

pause

@echo off

::: clean, aab, APK
call gradlew clean ^
 test ^
 :app:bundlePublishQuad5Release ^
 :app:publishQuad5ReleaseApk

pause

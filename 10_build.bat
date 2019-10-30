@echo off

::: clean, APK
call gradlew clean ^
 :app:bundlePublishQuad5Release ^
 :app:publishQuad5ReleaseApk

pause

#!/bin/sh
set -e

# clean, aab, APK
./gradlew clean \
  test \
  :app:bundlePublishRelease \
  :app:publishReleaseApk

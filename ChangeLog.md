Change Log
==========

v2.1.3 (2020.06.24)
-------------------
- Fix not reload settings after ConfigActivity closed
- Update libraries
  - AGP 3.5.1 -> 4.0.0
  - Gradle 5.4.1 -> 6.5
  - Kotlin 1.3.50 -> 1.3.72
  - AssertJ 3.16.1
  - BuildTools 28.0.3 -> 29.0.3
  - Preference 1.0.0 -> 1.1.1
  - Core 1.0.2 -> 1.3.0

v2.1.2 (2019.11.02)
-------------------
- setForeground more early at the first time run
- Fix NPE

v2.1.1 (2019.11.01)
-------------------
- Fix to stop notification by system after closing the main activity
(Call Service.startForeground when starting up the service from main activity)

v2.1.0 (2019.10.30)
-------------------
- Change notification icon color to Monochrome
- Set "Start on boot" by default
- Support Android 4.0 or later (Stop supporting Android 2.3 to 3.2)
- targetSdkVersion 15 -> 28
- Set icon mode as "1 Icon (sorted)" by default
- Migrate to AndroidX
- Update libraries
  - Support Library 25.3.1 -> 27.1.0
  - Gradle 4.1 -> 5.4.1
  - AGP 3.0.1 -> 3.5.1
  - Kotlin 1.3.0 -> 1.3.50
  - BuildTools 27.0.3 -> 28.0.3

v2.0.2 (2018.03.06)
-------------------
- Suppress error log outputs (care for frozen core)

v2.0.1 (2018.02.28)
-------------------
- Add landscape layout

v2.0.0 (2018.02.27)
-------------------
- Re-support Android 8.0 or later
- For Android 8.0 and later, use the frequency of each core as the CPU usages
- Show frequency of each core
- Add placeholder at ConfigActivity
- Add "dump feature" on long tap of app icon of AboutActivity
- Update build tools

v1.4.2 (2017.09.04)
-------------------
- Support Android 2.3 to 7.1 (exclude Android 1.6, and 8.0 or later)
- Update Support Library 23.1.0 -> 25.3.1
- Update build tools

v1.4.1 (2015.11.10)
-------------------
- Add "1 Icon (Unsorted)" and "1 Icon (Sorted)" mode
- Add french translation (by Belarrius)

v1.4.0 (2015.11.02)
-------------------
- Supports 6 or 8 cores (2 icons)
- More simple layout

v1.3.0 (2015.04.20)
-------------------
- Fix delaying interval on Android 5.1 devices
- Add About activity (and show CPU Info) (by Paul Verest)

v1.2.2 (2014.11.27)
-------------------
- Add czech translation (by Jaroslav Lichtblau)
- Add german translation (by Oliver Z.)
- Hide notification on lock-screen (Android 5.0)
- Reduce APK Size

v1.2.1 (2014.07.03)
-------------------
- Android Studio に移行
- proguard最適化

v1.2.0 (2013.01.13)
-------------------
- Android1.6以降対応(従来はAndroid2.2以降だった)
- 0.5秒設定追加
- スリープ復帰時にステータスバーがすぐに更新されるのを防ぐようにした

v1.1.0 (2012.10.22)
-------------------
- クアッドコアのレベルを5段階に変更
- CPUクロック周波数の表示に対応
- 設定でCPU使用率、クロック周波数の通知表示を変更できるようにした

v1.0.2 (2012.10.17)
-------------------
- 性能改善(整数演算化、キャッシュ追加、不要データの解析抑止など)
- バックグラウンド時も画面更新していたバグ修正
- デフォルト更新間隔を3秒から5秒に変更
- コア数変動時もできる限り算出するようにした

v1.0.1 (2012.10.16)
-------------------
- デュアルコアでもCPUコア数が1コアと認識される場合がある不具合を修正

v1.0 (2012.10.16)
-------------------
- 初版リリース

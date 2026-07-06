# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

CPU Stats — Android のステータスバーに CPU 使用率／クロック周波数を常駐通知として表示するアプリ (`jp.takke.cpustats`)。Google Play と F-Droid で配布されている。

- 言語 / ツールチェイン: Kotlin 2.4.0 / AGP 9.2.1 / Gradle 9.6.1 / JDK 11 (JVM toolchain 11)
- UI は **Jetpack Compose(Material3)**。Compose BOM で UI/Foundation/Material3 のバージョンを揃える。`kotlin-compose` Gradle プラグイン使用。
- minSdk 23 / compileSdk 37 / targetSdk 35(buildTools は AGP の自動選択に任せるため未指定)
- モジュール: `:app`, `:modules:quad5`

## よく使うコマンド

`gradlew`(bash / macOS)と `gradlew.bat`(Windows)を使う。macOS 用のワンライナーは `10_build.sh`、Windows 用は `*.bat` 系。

```bash
# ビルド + ユニットテスト(release AAB + APK を deployTo にコピーまで実施)
./gradlew clean test :app:bundlePublishRelease :app:publishReleaseApk

# デバッグ APK のみ(CI と同等)
./gradlew assembleDebug

# ユニットテスト全部
./gradlew test

# 特定のテストクラス/メソッドだけ
./gradlew :app:testDebugUnitTest --tests "jp.takke.cpustats.CpuNotificationDataDistributorTest"
./gradlew :app:testDebugUnitTest --tests "jp.takke.cpustats.CpuNotificationDataDistributorTest.distributeNotificationData_2icons"

# 署名済み Release APK / AAB(要 signing 設定, 下記参照)
./gradlew :app:publishReleaseApk       # APK を deployTo にコピー
./gradlew :app:bundlePublishRelease    # AAB を deployTo にコピー
./gradlew :app:publishAll              # 上記まとめて
```

テストは JUnit4 + Robolectric + AssertJ。`app/src/test/java` に置く。

## リポジトリのビルド設定

- **バージョン管理は root の `build.gradle.kts` の `ext { }` に集約**: `versionName`, `versionCode`, `compileSdkVersion`, `targetSdkVersion`, `minSdkVersion`, `apkNamePrefix`。リリース時はここだけ書き換えれば全モジュールに伝播する。
- `libs.versions.toml` にライブラリ / Kotlin / AGP のバージョンをまとめている。root の buildscript classpath もここを参照する。
- **Configuration Cache / Build Cache 有効**(`gradle.properties`)。`archivesName` は日時抜き(`CpuStats_<shortVersionName>`)で確定し、publish 系 Copy タスクの `rename`(Execution フェーズ)で `_yyyyMMdd_HHmm` を注入する方式(TwitPane と同方式)。**Configuration フェーズで `Date()` 等を評価しないこと**。rename のラムダは何もキャプチャしないこと。
- publish タスクは `androidComponents.onVariants` + `SingleArtifact.APK/BUNDLE` で生成している(旧 `applicationVariants` API は AGP 9 で廃止方向のため使用しない)。
- 署名情報とデプロイ先は `app/gradle.properties` の以下のプロパティで注入(存在すればビルドが読む):
  - `storeFile`, `storePassword`, `keyAlias`, `keyPassword`
  - `deployTo` — `publishReleaseApk` / `bundlePublishRelease` が成果物をコピーする先
- CI や公開環境で秘密を含む `app/gradle.properties` を上書きしたいときは `app/make_plain_gradle_properties.sh` を実行し `deployTo=` のみの空プロパティを生成する(`.github/workflows/main.yml` で実行しているのと同じ手順)。
- GitHub Actions は `assembleDebug` と `test` を master への push / PR で回している。

## アーキテクチャの全体像

CPU 状態の収集 → 通知アイコンへの割り当て → 通知描画、という 1 本の流れが `UsageUpdateService` に集約されている。処理をまたぐデータは `IntArray`(index=0 が全体平均、1 以降が各コア)で運ばれる。

- **`UsageUpdateService`**(foreground service, manifest で `foregroundServiceType="specialUse"` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` の用途申告)
  - 収集は `mServiceScope`(SupervisorJob + Dispatchers.Default)上の収集ループ(`startGatherLoop`)で行う。`delay(mConfig.intervalMs)` → `execTask()` の繰り返し。AlarmManager による keep-alive は廃止済み(FGS + START_STICKY で十分)。
  - 停止は `stopGatherLoop()` が `runBlocking { cancelAndJoin() }` で実行中の execTask 完了を待つ(停止直後の `cancelAll()` 後に通知が再表示されるのを防ぐため)。
  - `ACTION_SCREEN_ON/OFF` を受信して収集ループを停止/再開する(スリープ復帰後はステータスバーの並びが崩れないように 30 秒間は通知時刻を更新しない)。
  - IPC は AIDL ではなく **`LocalBinder`(Service インスタンス直接参照)+ `UsageUpdateCallback`(fun interface)**。`MainActivity` が bind して `onUsageUpdated(cpuUsages, freqs, minFreqs, maxFreqs, systemStats)` を受け取る。コールバックは収集スレッドから呼ばれるため受信側でメインスレッドへ切り替える。
  - Android 8.0+ からは foreground 起動要求のフラグ(`FOREGROUND_REQUEST`)を extras に載せてもらい、5 秒以内に `startForeground` を呼び出す設計。
  - CPU 以外の情報は `SystemStats`(data class)に集約して配信: バッテリー温度(`ACTION_BATTERY_CHANGED` の sticky broadcast、権限不要)とメモリ使用量(`ActivityManager.getMemoryInfo`)。メモリは変動が細かすぎるため通知の更新判定には含めない。
  - 全体使用率の履歴を `UsageHistoryBuffer`(容量 120 のリングバッファ)に毎サンプル記録し、`getUsageHistory()` で公開(スパークライン用)。
  - 履歴記録: `UsageLogDatabase`(SQLiteOpenHelper 直・シングルトン、Room 不使用)に 10 秒間隔の間引きで記録し、挿入時に 24h rolling 削除。設定(`RecordHistory`、デフォルト ON)で無効化可能。`HistoryActivity` が 1h/6h/24h の時系列グラフ(`view/HistoryGraphView`)を表示する。
  - 常駐状態は companion の `isResidentRunning`(@Volatile)で公開。`ACTION_STOP_RESIDENT` を intent action で受けると常駐停止する(クイック設定タイル用)。
- **`CpuInfoCollector`**(object)
  - コア数は `/sys/devices/system/cpu/cpu[0-9]` を数え(取れなければ `availableProcessors()`)、一度だけキャッシュ。
  - 周波数は `/sys/devices/system/cpu/cpuN/cpufreq/{scaling_cur_freq,cpuinfo_min_freq,cpuinfo_max_freq}` を読む。
  - CPU 使用率は `/proc/stat` の user/nice/system/idle/iowait/irq/softirq を差分計算する。**Android O(API 26) 以降は `/proc/stat` が読めない**ため `takeCpuUsageSnapshot()` は `null` を返し、`UsageUpdateService` が周波数比(`MyUtil.calcCpuUsagesByCoreFrequencies`)にフォールバックする。
- **`CpuNotificationDataDistributor`**(object)
  - `cpuUsages`(index=0 が平均、1 以降が各コア) を最大 2 個のアイコン用データ(`CpuNotificationData`)に分割する。
  - モードは 3 種類: `CORE_DISTRIBUTION_MODE_2ICONS`(デフォルト, 5 コア以上は 4+残り、6 コアは 3+3)/ `_1ICON_UNSORTED`(1 個目まで) / `_1ICON_SORTED`(降順)。
  - どのモードでも副アイコン側の `cpuUsages[0]` に全体平均をコピーして連続表示できるようにしている。
- **`NotificationPresenter`**
  - 通知チャンネル 2 本(CPU Usage / CPU Frequency)を必要時に作成。
  - 「使用率通知」= 最大 2 アイコン(ID=10, 11)、「周波数通知」= 1 アイコン(ID=20、本文にバッテリー温度も表示)。requestForeground=true の間は最初の通知を `startForeground` に流用する。
  - キーガード時は priority=MIN にしてロック画面から隠す。
  - `MyConfig` は保持せず、`updateNotifications()` / `cancelNotifications()` の引数で毎回受け取る。
- **`ResourceUtil` / `:modules:quad5`(QuadResource)**
  - コア数によりアイコンリソースを差し替える: 1 コア = `single*`、2 コア = `dual_*_*`、3 コア = `tri_*`、4 コア以上 = `quad_*`。
  - 各コアの使用率を「level5」(<5,<20,<40,<60,<80,else)にマップし、それを 4 桁の 5 進値にして `quad_XXXX.xml` へ引く仕組み。この 1500 件超のドロウアブルを別モジュール `:modules:quad5` に切り出している(`QuadResource.getIconIdForCpuUsageQuad` などが公開 API)。
- **UI 側(Jetpack Compose)**
  - 全画面 `ComponentActivity` + `setContent { CpuStatsTheme { ... } }`。XML レイアウトと Fragment は撤去済み。
  - テーマ: `ui/theme/CpuStatsTheme`(Material3 `ColorScheme`、DayNight 追従、Dynamic Color はデフォルト OFF)。application/activity のマニフェスト theme は `Theme.CpuStats(.NoActionBar)`(`Theme.AppCompat.DayNight` ベース、起動画面の一時色として使用)。
  - `MainActivity`: `LocalBinder` でサービスに bind →  `UsageUpdateCallback` で `MutableStateFlow<MainState>` を更新 → `collectAsState` で画面に反映。TopAppBar のオーバーフローメニューで Start/Stop/Settings/History/About/Exit。
  - `ConfigActivity`: `SharedPreferences` 直接読み書きの Compose 版 Settings 画面(`SwitchPreference` / `ListPreference` を自前実装)。`androidx.preference-ktx` 依存は撤去済み(素の `context.defaultSharedPreferences()` を使う)。
  - `AboutActivity`: Material3 + `LinkAnnotation.Url` によるクリック可能リンク。アイコン長押しで内部ダンプ表示。
  - `HistoryActivity`: `SingleChoiceSegmentedButtonRow` で 1h/6h/24h 切替、DB 読み出しは `Dispatchers.IO` + `LaunchedEffect`。
  - `ui/component/Sparkline`: 全体使用率の折れ線(Compose Canvas 版)。
  - `ui/component/HistoryGraph`: 履歴の時系列グラフ(Compose Canvas 版、記録間隔の 3 倍で欠測分断)。
  - `CpuTileService`: クイック設定タイル(Android 7.0+)。タップで常駐 ON/OFF、パネル表示中は flags=0 で bind してライブ表示(タイル表示だけで収集を始めない)。
  - `widget/CpuWidget` + `CpuWidgetReceiver`: ホーム画面ウィジェット(Glance)。表示は使用率アイコン + 「CPU N%」+ 周波数 + バッテリー温度。データは `widget/WidgetData`(@Volatile Snapshot)でプロセス内共有し、サービスの execTask ごとに `CpuWidget().updateAll(context)` で再描画(貼られていなければ no-op)。`updatePeriodMillis="0"` でシステム定期更新は無効、更新はサービス駆動。
  - `BootReceiver`: 起動時に `StartOnBoot`(デフォルト true)なら foreground service として `UsageUpdateService` を起動。
  - `NotificationPermissionUtil`: Android 13+ の `POST_NOTIFICATIONS` 許可導線(MainActivity 側は `rememberLauncherForActivityResult` で自前実装)。
- **設定は `MyConfig`(immutable data class)+ `MyConfig.load(context)`**: `updateIntervalSec`(小数秒可 → ms 変換)、`showUsageNotification`、`showFrequencyNotification`、`coreDistributionMode`。設定変更後は `ConfigActivity` から戻った時に `MainActivity` がサービスの `reloadSettings()` を呼び、サービス側で新しい `MyConfig` に差し替え + 不要な通知を消去する。

## 注意点 / 落とし穴

- `app/src/quad5/java/...` は空ディレクトリ。以前あった product flavor を撤去した名残(コミット `no edition flavor` 参照)。`quad5` 名を見かけたら、実体は `:modules:quad5` サブモジュール側にある。
- 通知 ID は `NotificationPresenter` に固定値(10 / 11 / 20)。追加時は既存 ID と衝突しないこと。
- `versionName` を変えたら `versionCode` の増加も忘れずに(root `build.gradle.kts`)。ChangeLog は `ChangeLog.md` に追記していく慣習。
- 通知アイコン(`freq_*` / `digit*` / `single*` 等)はダーク背景用の淡色モノクロ画像。アプリ内 UI で使う場合は `app:tint="?android:attr/textColorPrimary"` などテーマ連動の tint を掛けること(掛けないとライトモードで見えない)。
- Foreground Service タイプは `specialUse`。`dataSync` に戻すと Android 15+ で BOOT_COMPLETED からの起動不可 + 6 時間タイムアウトの問題が再発する。Play Console 提出時は specialUse の用途申告が必要。

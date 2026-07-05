# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

CPU Stats — Android のステータスバーに CPU 使用率／クロック周波数を常駐通知として表示するアプリ (`jp.takke.cpustats`)。Google Play と F-Droid で配布されている。

- 言語 / ツールチェイン: Kotlin 2.1.20 / AGP 8.9.2 / Gradle 8.14 / JDK 11 (JVM toolchain 11)
- minSdk 23 / compileSdk 35 / targetSdk 35 / buildTools 35.0.0
- モジュール: `:app`, `:modules:quad5`

## よく使うコマンド

`gradlew`(bash / macOS)と `gradlew.bat`(Windows)を使う。Windows 用のワンライナーが `10_build.bat` 系にまとめられている。

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

- **バージョン管理は root の `build.gradle.kts` の `ext { }` に集約**: `versionName`, `versionCode`, `compileSdkVersion`, `buildToolsVersion`, `targetSdkVersion`, `minSdkVersion`, `apkNamePrefix`。リリース時はここだけ書き換えれば全モジュールに伝播する。
- `libs.versions.toml` にライブラリ / Kotlin / AGP のバージョンをまとめている。
- 出力 APK 名は `CpuStats_<shortVersionName>_<yyyyMMdd_HHmm>` 形式(`app/build.gradle.kts` の `archivesBaseName` 参照)。
- 署名情報とデプロイ先は `app/gradle.properties` の以下のプロパティで注入(存在すればビルドが読む):
  - `storeFile`, `storePassword`, `keyAlias`, `keyPassword`
  - `deployTo` — `publishReleaseApk` / `bundlePublishRelease` が成果物をコピーする先
- CI や公開環境で秘密を含む `app/gradle.properties` を上書きしたいときは `app/make_plain_gradle_properties.sh` を実行し `deployTo=` のみの空プロパティを生成する(`.github/workflows/main.yml` で実行しているのと同じ手順)。
- GitHub Actions は `assembleDebug` と `test` を master への push / PR で回している。

## アーキテクチャの全体像

CPU 状態の収集 → 通知アイコンへの割り当て → 通知描画、という 1 本の流れが `UsageUpdateService` に集約されている。処理をまたぐデータは `IntArray`(index=0 が全体平均、1 以降が各コア)で運ばれる。

- **`UsageUpdateService`**(foreground service, manifest で `foregroundServiceType="dataSync"`)
  - `AlarmManager` で自身の `onStartCommand` を再スケジュールし、加えて内部で `GatherThread` を回して短周期(`MyConfig.intervalMs`)の収集を行う二段構え。
  - `ACTION_SCREEN_ON/OFF` を受信して収集を止める(スリープ復帰後はステータスバーの並びが崩れないように 30 秒間は通知時刻を更新しない)。
  - AIDL の `IUsageUpdateService` を実装。`MainActivity` が bind し、`IUsageUpdateCallback.updateUsage` 経由で `cpuUsages` / `freqs` / `minFreqs` / `maxFreqs` を配信する。
  - Android 8.0+ からは foreground 起動要求のフラグ(`FOREGROUND_REQUEST`)を extras に載せてもらい、5 秒以内に `startForeground` を呼び出す設計。
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
  - 「使用率通知」= 最大 2 アイコン(ID=10, 11)、「周波数通知」= 1 アイコン(ID=20)。requestForeground=true の間は最初の通知を `startForeground` に流用する。
  - キーガード時は priority=MIN にしてロック画面から隠す。
- **`ResourceUtil` / `:modules:quad5`(QuadResource)**
  - コア数によりアイコンリソースを差し替える: 1 コア = `single*`、2 コア = `dual_*_*`、3 コア = `tri_*`、4 コア以上 = `quad_*`。
  - 各コアの使用率を「level5」(<5,<20,<40,<60,<80,else)にマップし、それを 4 桁の 5 進値にして `quad_XXXX.xml` へ引く仕組み。この 1500 件超のドロウアブルを別モジュール `:modules:quad5` に切り出している(`QuadResource.getIconIdForCpuUsageQuad` などが公開 API)。
- **UI 側**
  - `MainActivity`: サービスに bind → コールバックで受け取った値を画面に描画。設定画面 (`ConfigActivity` / `ConfigFragment`)、`AboutActivity` を提供。
  - `BootReceiver`: 起動時に `StartOnBoot`(デフォルト true)なら foreground service として `UsageUpdateService` を起動。
  - `NotificationPermissionUtil`: Android 13+ の `POST_NOTIFICATIONS` 許可導線。
- **設定は `MyConfig.loadSettings` に集約**: `updateIntervalSec`(小数秒可 → ms 変換)、`showUsageNotification`、`showFrequencyNotification`、`coreDistributionMode`。設定変更後は `ConfigActivity` から戻った時に `MainActivity` が AIDL の `reloadSettings()` を呼び、サービス側で `MyConfig` を再ロード + 不要な通知を消去する。

## 注意点 / 落とし穴

- `app/src/quad5/java/...` は空ディレクトリ。以前あった product flavor を撤去した名残(コミット `no edition flavor` 参照)。`quad5` 名を見かけたら、実体は `:modules:quad5` サブモジュール側にある。
- `UsageUpdateService.mBinder` は `MyConfig` を再ロードするが、`NotificationPresenter` が保持している `mConfig` 参照は差し替えず同一インスタンスを再利用している(mutable なフィールドを更新する構造)。設定オブジェクトを差し替えるリファクタをする際は要注意。
- 通知 ID は `NotificationPresenter` に固定値(10 / 11 / 20)。追加時は既存 ID と衝突しないこと。
- `versionName` を変えたら `versionCode` の増加も忘れずに(root `build.gradle.kts`)。ChangeLog は `ChangeLog.md` に追記していく慣習。

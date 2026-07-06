# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\takke\AppData\Local\Android\android-studio\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

#-----------------------------------------------------------
# Room(Glance が WorkManager 経由で内部利用)
#
# Room はランタイムに <DatabaseName>_Impl クラスをリフレクションで解決するため、
# minify 時にこれらのクラスが削除されると
# "Failed to create an instance of androidx.work.impl.WorkDatabase" で起動失敗する。
#-----------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Database class * { *; }
-keep class androidx.room.RoomDatabase$Builder { *; }
-keepnames class androidx.work.impl.WorkDatabase_Impl
-dontwarn androidx.room.paging.**

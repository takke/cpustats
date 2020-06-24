package jp.takke.cpustats

import android.util.Log

object MyLog {

    var debugMode = BuildConfig.DEBUG

    fun v(msg: String) {
        if (debugMode) {
            Log.v(C.LOG_NAME, msg)
        }
    }

    fun v(msg: String, th: Throwable) {
        if (debugMode) {
            Log.v(C.LOG_NAME, msg, th)
        }
    }

    fun d(msg: String) {
        if (debugMode) {
            Log.d(C.LOG_NAME, msg)
        }

        dumpToExternalLogFile(Log.DEBUG, msg)
    }

    /**
     * msg 内の {elapsed} 部分を経過時刻に変換し出力する
     *
     * @param msg メッセージ
     * @param startTick 測定開始時刻[ms]
     */
    fun dWithElapsedTime(msg: String, startTick: Long) {

        if (debugMode) {
            d(msg.replace("{elapsed}", (System.currentTimeMillis() - startTick).toString() + ""))
        }
    }

    fun d(msg: String, th: Throwable) {

        if (debugMode) {
            Log.d(C.LOG_NAME, msg, th)
        }

        dumpToExternalLogFile(Log.DEBUG, msg)
        dumpToExternalLogFile(Log.DEBUG, Log.getStackTraceString(th))
    }

    fun i(msg: String) {
        Log.i(C.LOG_NAME, msg)

        dumpToExternalLogFile(Log.INFO, msg)
    }

    fun iWithElapsedTime(msg: String, startTick: Long) {

        i(msg.replace("{elapsed}", (System.currentTimeMillis() - startTick).toString() + ""))
    }

    fun i(msg: String, th: Throwable) {
        Log.i(C.LOG_NAME, msg, th)

        dumpToExternalLogFile(Log.INFO, msg)
        dumpToExternalLogFile(Log.INFO, Log.getStackTraceString(th))
    }

    fun w(msg: String) {
        Log.w(C.LOG_NAME, msg)

        dumpToExternalLogFile(Log.WARN, msg)
    }

    fun wWithElapsedTime(msg: String, startTick: Long) {

        if (debugMode) {
            w(msg.replace("{elapsed}", (System.currentTimeMillis() - startTick).toString() + ""))
        }
    }

    fun w(msg: String, th: Throwable) {
        Log.w(C.LOG_NAME, msg, th)

        dumpToExternalLogFile(Log.WARN, msg)
        dumpToExternalLogFile(Log.WARN, Log.getStackTraceString(th))
    }

    fun w(th: Throwable) {
        Log.w(C.LOG_NAME, th.message, th)

        dumpToExternalLogFile(Log.WARN, Log.getStackTraceString(th))
    }

    fun e(msg: String) {
        Log.e(C.LOG_NAME, msg)

        dumpToExternalLogFile(Log.ERROR, msg)
    }

    fun e(msg: String, th: Throwable) {
        Log.e(C.LOG_NAME, msg, th)

        dumpToExternalLogFile(Log.ERROR, msg)
        dumpToExternalLogFile(Log.ERROR, Log.getStackTraceString(th))
    }

    fun e(th: Throwable) {
        Log.e(C.LOG_NAME, th.message, th)

        dumpToExternalLogFile(Log.ERROR, Log.getStackTraceString(th))
    }

    /**
     * 外部ストレージ(通常はSDカード)にログを出力する
     *
     * @param error エラーレベル
     * @param msg メッセージ
     */
    @Suppress("UNUSED_PARAMETER")
    private fun dumpToExternalLogFile(error: Int, msg: String) {
        if (!debugMode) {
            return
        }

//      try {
//          // 保存先の決定
//          final File fout = TkUtil.getExternalStorageFile(C.EXTERNAL_FILE_DIRNAME, null);
//          if (fout == null) {
//              // メディア非マウントなど
//              return;
//          }
//          final String path = fout.getAbsolutePath() + "/" + C.EXTERNAL_LOG_FILENAME;
//
//          // ファイルに書き込む
//          final FileOutputStream out = new FileOutputStream(path, true);  // append
//
//          // 日付時刻
//          final SimpleDateFormat sdf = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss.SSS]");
//          out.write(sdf.format(new Date()).getBytes());
//
//          // エラーレベル
//          switch (error) {
//          case Log.INFO:  out.write("[INFO] ".getBytes());    break;
//          case Log.WARN:  out.write("[WARN] ".getBytes());    break;
//          case Log.ERROR: out.write("[ERROR] ".getBytes());   break;
//          case Log.DEBUG: out.write("[DEBUG] ".getBytes());   break;
//          default:
//              break;
//          }
//
//          // ログ本文
//          out.write(msg.getBytes("UTF-8"));
//          out.write("\n".getBytes());
//
//          out.flush();
//          out.close();
//
//      } catch (Exception e) {
////            Log.e(C.LOG_NAME, e.getMessage(), e);
//      }
    }

    /**
     * 外部ストレージのログファイルがある一定サイズ以上の場合に削除する
     *
     * 通常は起動時にチェックさせる
     */
    fun deleteBigExternalLogFile() {
        if (!debugMode) {
            return
        }

//      try {
//          // 保存先の決定
//          final File fout = TkUtil.getExternalStorageFile(C.EXTERNAL_FILE_DIRNAME, null);
//          if (fout == null) {
//              // メディア非マウントなど
//              return;
//          }
//          final String path = fout.getAbsolutePath() + "/" + C.EXTERNAL_LOG_FILENAME;
//
//          // チェック＆削除
//          final File file = new File(path);
//          final int MAXFILESIZE = 2 * 1024 * 1024;    // [MB]
//
//          Log.i(C.LOG_NAME, "external log size check, size[" + file.length() + "], limit[" + MAXFILESIZE + "]");
//
//          if (file.length() > MAXFILESIZE) {
//              file.delete();
//          }
//
//      } catch (Exception e) {
//          Log.e(C.LOG_NAME, e.getMessage(), e);
//      }
    }

}

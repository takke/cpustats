package jp.takke.cpustats;

import android.util.Log;

public class MyLog {
	
	public static boolean debugMode = false;
	
	public static void v(String msg) {
		if (debugMode) {
			Log.v(C.LOG_NAME, msg);
		}
	}

	public static void v(String msg, Throwable th) {
		if (debugMode) {
			Log.v(C.LOG_NAME, msg, th);
		}
	}

	
	public static void d(String msg) {
		if (debugMode) {
			Log.d(C.LOG_NAME, msg);
		}

		MyLog.dumpToExternalLogFile(Log.DEBUG, msg);
	}
	
	/**
	 * msg 内の {elapsed} 部分を経過時刻に変換し出力する
	 * 
	 * @param msg メッセージ
	 * @param startTick 測定開始時刻[ms]
	 */
	public static void dWithElapsedTime(String msg, long startTick) {
		
		if (debugMode) {
			d(msg.replace("{elapsed}", (System.currentTimeMillis() - startTick) + ""));
		}
	}
	
	public static void d(String msg, Throwable th) {
		
		if (debugMode) {
			Log.d(C.LOG_NAME, msg, th);
		}
		
		MyLog.dumpToExternalLogFile(Log.DEBUG, msg);
		MyLog.dumpToExternalLogFile(Log.DEBUG, Log.getStackTraceString(th));
	}

	
	public static void i(String msg) {
		Log.i(C.LOG_NAME, msg);

		MyLog.dumpToExternalLogFile(Log.INFO, msg);
	}

	public static void iWithElapsedTime(String msg, long startTick) {
		
		i(msg.replace("{elapsed}", (System.currentTimeMillis() - startTick) + ""));
	}
	
	public static void i(String msg, Throwable th) {
		Log.i(C.LOG_NAME, msg, th);

		MyLog.dumpToExternalLogFile(Log.INFO, msg);
		MyLog.dumpToExternalLogFile(Log.INFO, Log.getStackTraceString(th));
	}

	
	public static void w(String msg) {
		Log.w(C.LOG_NAME, msg);
		
		MyLog.dumpToExternalLogFile(Log.WARN, msg);
	}

	public static void wWithElapsedTime(String msg, long startTick) {
		
		if (debugMode) {
			w(msg.replace("{elapsed}", (System.currentTimeMillis() - startTick) + ""));
		}
	}
	
	public static void w(String msg, Throwable th) {
		Log.w(C.LOG_NAME, msg, th);

		MyLog.dumpToExternalLogFile(Log.WARN, msg);
		MyLog.dumpToExternalLogFile(Log.WARN, Log.getStackTraceString(th));
	}

	public static void w(Throwable th) {
		Log.w(C.LOG_NAME, th.getMessage(), th);

		MyLog.dumpToExternalLogFile(Log.WARN, Log.getStackTraceString(th));
	}

	
	public static void e(String msg) {
		Log.e(C.LOG_NAME, msg);
		
		MyLog.dumpToExternalLogFile(Log.ERROR, msg);
	}

	public static void e(String msg, Throwable th) {
		Log.e(C.LOG_NAME, msg, th);
		
		MyLog.dumpToExternalLogFile(Log.ERROR, msg);
		MyLog.dumpToExternalLogFile(Log.ERROR, Log.getStackTraceString(th));
	}

	public static void e(Throwable th) {
		Log.e(C.LOG_NAME, th.getMessage(), th);
		
		MyLog.dumpToExternalLogFile(Log.ERROR, Log.getStackTraceString(th));
	}

	/**
	 * 外部ストレージ(通常はSDカード)にログを出力する
	 * 
	 * @param error エラーレベル
	 * @param msg メッセージ
	 */
	private static void dumpToExternalLogFile(int error, String msg) {
		if (!debugMode) {
			return;
		}
			
//		try {
//			// 保存先の決定
//			final File fout = TkUtil.getExternalStorageFile(C.EXTERNAL_FILE_DIRNAME, null);
//			if (fout == null) {
//				// メディア非マウントなど
//				return;
//			}
//			final String path = fout.getAbsolutePath() + "/" + C.EXTERNAL_LOG_FILENAME;
//			
//			// ファイルに書き込む
//			final FileOutputStream out = new FileOutputStream(path, true);	// append
//
//			// 日付時刻
//			final SimpleDateFormat sdf = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss.SSS]");
//			out.write(sdf.format(new Date()).getBytes());
//			
//			// エラーレベル
//			switch (error) {
//			case Log.INFO:	out.write("[INFO] ".getBytes());	break;
//			case Log.WARN:	out.write("[WARN] ".getBytes());	break;
//			case Log.ERROR:	out.write("[ERROR] ".getBytes());	break;
//			case Log.DEBUG:	out.write("[DEBUG] ".getBytes());	break;
//			default:
//				break;
//			}
//
//			// ログ本文
//			out.write(msg.getBytes("UTF-8"));
//			out.write("\n".getBytes());
//			
//			out.flush();
//			out.close();
//			
//		} catch (Exception e) {
////			Log.e(C.LOG_NAME, e.getMessage(), e);
//		}
	}

	/**
	 * 外部ストレージのログファイルがある一定サイズ以上の場合に削除する
	 * 
	 * 通常は起動時にチェックさせる
	 */
	public static void deleteBigExternalLogFile() {
		if (!debugMode) {
			return;
		}
		
//		try {
//			// 保存先の決定
//			final File fout = TkUtil.getExternalStorageFile(C.EXTERNAL_FILE_DIRNAME, null);
//			if (fout == null) {
//				// メディア非マウントなど
//				return;
//			}
//			final String path = fout.getAbsolutePath() + "/" + C.EXTERNAL_LOG_FILENAME;
//			
//			// チェック＆削除
//			final File file = new File(path);
//			final int MAXFILESIZE = 2 * 1024 * 1024;	// [MB]
//			
//			Log.i(C.LOG_NAME, "external log size check, size[" + file.length() + "], limit[" + MAXFILESIZE + "]");
//			
//			if (file.length() > MAXFILESIZE) {
//				file.delete();
//			}
//			
//		} catch (Exception e) {
//			Log.e(C.LOG_NAME, e.getMessage(), e);
//		}
	}

}

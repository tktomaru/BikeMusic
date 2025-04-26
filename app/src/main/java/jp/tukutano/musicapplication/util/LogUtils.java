package jp.tukutano.musicapplication.util;

import android.util.Log;

public class LogUtils {
    // Utility メソッド例：呼び出し元のクラス名とメソッド名を自動取得してログ出力
    public static void logWithCaller(StackTraceElement[] sts, String message) {
        // スタックトレースの要素取得
        // [0]: Thread#getStackTrace
        // [1]: このメソッド(logWithCaller)
        // [2]: 呼び出し元メソッド ← これを使う
//        StackTraceElement[] sts = Thread.currentThread().getStackTrace();
        if (sts.length >= 3) {
            StackTraceElement caller = sts[2];
            String cls = caller.getClassName();
            String mthd = caller.getMethodName();
            Log.d("tktomaru", cls + " : " + mthd + "() : " + message);
        } else {
            Log.d("Unknown", message);
        }
    }
}

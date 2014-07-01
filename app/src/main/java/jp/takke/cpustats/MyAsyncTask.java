package jp.takke.cpustats;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import android.os.AsyncTask;

public abstract class MyAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    
    @SuppressWarnings("unchecked")
    public final AsyncTask<Params, Progress, Result> parallelExecute(Params... params) {
        
        if (android.os.Build.VERSION.SDK_INT >= 13) {
            // Android 3.2 以降ではデフォルトでシリアル実行されるので切り替えて並行実行する
            // @see http://daichan4649.hatenablog.jp/entry/20120125/1327467103
            
            // ※下記は Android 1.6 等の端末で実行不可なのでリフレクションで呼び出す
//          return super.executeOnExecutor(executor, params);
            
            try {
                final Field f = AsyncTask.class.getField("THREAD_POOL_EXECUTOR");
                final Executor executor = (Executor) f.get(null);
                
                final Method m = this.getClass().getMethod("executeOnExecutor", new Class[]{
                        Executor.class, Object[].class });
                
                return (AsyncTask<Params, Progress, Result>) m.invoke(this, executor, params);
        
            } catch (Exception e) {
                MyLog.e(e);
            }
        }
        
        return super.execute(params);
    }
}
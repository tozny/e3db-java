package com.tozny.e3db;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class PredefinedExecutors {
    static final Executor backgroundExecutor;
    static final Executor uiExecutor;

    static {
        backgroundExecutor = new ThreadPoolExecutor(1,
            Runtime.getRuntime().availableProcessors(),
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(10),
            new ThreadFactory() {
                private int threadCount = 1;

                @Override
                public Thread newThread(Runnable runnable) {
                    final Thread thread = java.util.concurrent.Executors.defaultThreadFactory().newThread(runnable);
                    thread.setDaemon(true);
                    thread.setName("E3DB background " + threadCount++);
                    return thread;
                }
            });

        if (Platform.isAndroid()) {
            // Post results to UI thread
            uiExecutor = new Executor() {
                private final Handler handler = new Handler(Looper.getMainLooper());

                @Override
                public void execute(Runnable runnable) {
                    handler.post(runnable);
                }
            };
        } else {
            // Post results to current thread (whatever that is)
            uiExecutor = new Executor() {
                @Override
                public void execute(Runnable runnable) {
                    runnable.run();
                }
            };
        }
    }

}

package com.geneaazul.gedcomanalyzer.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ThreadUtils {

    public static void sleepMillisAndThen(int millis, Runnable runnable) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        runnable.run();
    }

    public static void sleepSecondsAndThen(int seconds, Runnable runnable) {
        sleepMillisAndThen(seconds * 1000, runnable);
    }

}

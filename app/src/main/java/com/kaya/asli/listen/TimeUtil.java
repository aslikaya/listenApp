package com.kaya.asli.listen;

public class TimeUtil{
    
    private TimeUtil() {
    }

    public static String millisecondsToFormattedTime(long milliseconds) {

        final long millis = milliseconds % 1000;
        final long second = (milliseconds / 1000) % 60;
        final long minute = (milliseconds / (1000 * 60)) % 60;
        final long hour = (milliseconds / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d:%02d", hour, minute, second, millis);
    }
}

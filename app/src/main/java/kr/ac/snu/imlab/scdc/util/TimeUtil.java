package kr.ac.snu.imlab.scdc.util;

/**
 * Created by kilho on 15. 8. 7.
 */
public class TimeUtil {

  public static String getElapsedTimeUntilNow(long startTime) {
    if (startTime == -1) return null;

    long timeDelta = System.currentTimeMillis() - startTime;
    String elapsedTime = null;

    long secondsInMillis = 1000;
    long minutesInMillis = secondsInMillis * 60;
    long hoursInMillis = minutesInMillis * 60;
    long daysInMillis = hoursInMillis * 24;

    if (timeDelta < minutesInMillis) {
      elapsedTime = String.valueOf(timeDelta / secondsInMillis) + " 초";
    } else if (timeDelta < hoursInMillis) {
      elapsedTime = String.valueOf(timeDelta / minutesInMillis) + " 분";
    } else if (timeDelta < daysInMillis) {
      elapsedTime = String.valueOf(timeDelta / hoursInMillis) + " 시간";
    } else {
      elapsedTime = String.valueOf(timeDelta / daysInMillis) + " 시간";
    }

    return elapsedTime;
  }

}

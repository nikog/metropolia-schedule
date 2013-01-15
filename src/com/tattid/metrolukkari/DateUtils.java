package com.tattid.metrolukkari;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class DateUtils {
	public static String millisToLocalReadable(long millis) {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/Helsinki"));
		
		return sdf.format(calendar.getTime());
	}
	
	public static long unixTimeStringToMillis(String unixTime) {
		return Long.parseLong(unixTime) * 1000L;
	}
}

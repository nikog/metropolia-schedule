package com.nikog.metropolia.schedule;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class DateUtils {
	public static String timeMillisToLocalReadable(long millis) {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/Helsinki"));
		
		return sdf.format(calendar.getTime());
	}
	
	public static long unixTimeToTimeMillis(long unixTime) {
		return unixTime * 1000L;
	}
	
	public static long tomorrow() {
		return daysFromToday(1);
	}
	public static long daysFromToday(int days) {
		Calendar c = Calendar.getInstance();
		
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		// next day
		c.add(Calendar.DAY_OF_MONTH, days);
		
		return c.getTimeInMillis();
	}
	
	public static String getFullDay(long timeMillis) {
		SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
		
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeMillis);
		
		return sdf.format(c.getTime());
	}
	
	public static int getDay(long timeMillis) {
		SimpleDateFormat sdf = new SimpleDateFormat("d");
		
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeMillis);
		
		return Integer.parseInt(sdf.format(c.getTime()));
	}
}

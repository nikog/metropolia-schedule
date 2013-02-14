package com.nikog.metropolia.schedule;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class UpdateService extends IntentService {
	
	private int appWidgetId;
	
	public UpdateService() {
		super("MetroSchedIntentService");
		Log.d(WidgetProvider.TAG, "UpdateService launched");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(WidgetProvider.TAG, "Handling intent");
		
		// Get widgetId from extras
		Bundle extras = intent.getExtras();
		this.appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		
		List<Event> resultList = getEvents(3);
		long updateTimeMillis = 0;
		// Current time + 30min
		long minUpdateTimeMillis = System.currentTimeMillis() + 1800000L;
		
		if(resultList != null) {
			updateTimeMillis = resultList.get(0).getEnd();
		}

		// If next event is tomorrow, fetch new data
		if((resultList == null || resultList.get(0).getStart() > DateUtils.tomorrow())) {
			if(isConnected()) {
				// Get URI from shared preferences
				URL url = getURL(appWidgetId);
				
				// Fetch and parse data
				String jsonString = getJSONSchedule(url);
				parseJSONToDatabase(jsonString);
				
				resultList = getEvents(3);

				updateTimeMillis = resultList.get(0).getEnd();
			}
		}

		// Minimum update interval is 30min
		if(updateTimeMillis < minUpdateTimeMillis) {
			updateTimeMillis = minUpdateTimeMillis;
		}
		
		resultList = trimEvents(resultList);
		
		buildUpdate(appWidgetId, resultList);
		
		// Create timer for next update
		createAlarmTimer(this, appWidgetId, updateTimeMillis);
	}
	
	// Build and deploy update to widget
	public void buildUpdate(int widgetId, List<Event> resultList) {
		Log.d(WidgetProvider.TAG, "Building");
		RemoteViews view = new RemoteViews(getPackageName(), R.layout.widget);
		
		Intent intent = new Intent(this, WidgetProvider.class);
		intent.setAction(WidgetProvider.METRO_ACTION_CLICK);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		view.setOnClickPendingIntent(R.id.widgetlayout, pendingIntent);
		
		String start, end, roomId, subject;
		
		int[] textViews = WidgetProvider.TEXTVIEWS;
		
		Log.d(WidgetProvider.TAG, resultList.toString());
		
		int listSize = resultList.size();
		
		Event event;
		
		for(int i=0; i<textViews.length; i++) {
			
			if(i < resultList.size()) {
				event = resultList.get(i);
			} else {
				event = null;
			}
			
			if(event != null) {
				start = DateUtils.timeMillisToLocalReadable(event.getStart());
				end = DateUtils.timeMillisToLocalReadable(event.getEnd());
				roomId = event.getRoomId();
				subject = event.getSubject();
				
				view.setTextViewText(textViews[i], start + " - " + end + " " + roomId + " " + subject);
			} else {
				view.setTextViewText(textViews[i], " ");
			}
		}
		
		view.setTextViewText(R.id.date, DateUtils.getFullDay(resultList.get(0).getStart()));
		
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		
		manager.updateAppWidget(widgetId, view);
	}
	
	public URL getURL(int widgetId) {
		SharedPreferences prefs = getSharedPreferences(ConfigurationActivity.PREFS_NAME, 0);	
		String group = prefs.getString("group#" + widgetId, "");
		
		URL url = null;	
		
		try {
			url = new URL(WidgetProvider.URIString + group);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		Log.d(WidgetProvider.TAG, "Got URI " + url.toString());
		return url;
	}
	
	public boolean isConnected() {
		ConnectivityManager cm =
		        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		 
		NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
		
		return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
	}
	
	public String getJSONSchedule(URL url) {
		Log.d(WidgetProvider.TAG, "Fetching & parsing");
		HttpURLConnection urlConnection = null;

		String result = null;
		
		if(isConnected()) {
			
		}
		
		try {
			urlConnection = (HttpURLConnection) url.openConnection();
			
			if (urlConnection.getResponseCode() != 200) {
				throw new IOException("Service not available: " + urlConnection.getResponseMessage());
			}
			
			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			StringBuilder sb = new StringBuilder();
			String line = null;
			
			while((line = reader.readLine()) != null) {
				sb.append(line);
			}
			result = sb.toString();
			//Log.d(MetrolukkariWidget.TAG, result);
		} catch(ClientProtocolException e) {
			e.printStackTrace();
		} catch(IOException e) {
			Log.d(WidgetProvider.TAG, "IOException: " + e.getMessage());
			e.printStackTrace();
		} finally {
			urlConnection.disconnect();
		}
		
		return result;
	}
	
	public void parseJSONToDatabase(String jsonString) {
		// Parse JSON here
		try {
			JSONObject jObject = new JSONObject(jsonString);
			JSONArray jArray = jObject.getJSONArray("events");
			
			DBAdapter dataSource = new DBAdapter(getApplicationContext(), appWidgetId);
			dataSource.open();
			dataSource.deleteOld();
			
			long start, end;
			String subject, roomId;
			
			int day = 0;
			int prevDay = 0;
			int dayCount = 0;
			
			for(int i=0; i<jArray.length(); i++) {
				JSONObject item = jArray.getJSONObject(i);
				
				start = item.getLong("start");
				end = item.getLong("end");
				subject = item.getString("subject");
				roomId = item.getString("roomid");

				start = DateUtils.unixTimeToTimeMillis(start);
				end = DateUtils.unixTimeToTimeMillis(end);
				
				day = DateUtils.getDay(start);
				
				if(day != prevDay) {
					prevDay = day;
					dayCount++;
				}
				
				if(dayCount > 3) {
					break;
				} else {
					Log.d(WidgetProvider.TAG, "Storing: " + subject + " from " + start + " to " + end + " at " + roomId);
					dataSource.push(subject, start, end, roomId);
				}
			}
			
			dataSource.close();
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<Event> getEvents(int limit) {
		DBAdapter dataSource = new DBAdapter(getApplicationContext(), appWidgetId);
		
		dataSource.open();
		
		List<Event> events = dataSource.getUpcoming(limit);
		
		dataSource.close();
		
		//Log.d(MetrolukkariWidget.TAG, "Got events " + events);
		
		return events;
	}
	
	public List<Event> trimEvents(List<Event> events) {
		if(events.size() < 1) {
			return null;
		}
		int day = DateUtils.getDay(events.get(0).getStart());
		int tDay;
		
		Iterator<Event> i = events.iterator();
		
		while(i.hasNext()) {
			Event event = i.next();
			tDay = DateUtils.getDay(event.getStart());
			Log.d(WidgetProvider.TAG, "day " +tDay);
			
			if(tDay != day) {
				i.remove();
			}
		}
		return events;
	}
	
	public void createAlarmTimer(Context ctx, int widgetId, long time) {
		// Check for already running alarm timer
		Intent intent = new Intent(ctx, UpdateService.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, widgetId, intent, PendingIntent.FLAG_NO_CREATE);

		if (pendingIntent == null) {
			pendingIntent = getSyncPendingIntent(ctx, widgetId);
			
			Log.d(WidgetProvider.TAG, "Next update at " + DateUtils.timeMillisToLocalReadable(time));

			AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(ctx.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC, time, pendingIntent);
		} else {
			Log.d(WidgetProvider.TAG, "AlarmManager already running for widget instance #" + widgetId);
		}
	}
	
	// Returns synced pendingIndents for identifying
	public static PendingIntent getSyncPendingIntent(Context ctx, int widgetId) {
		Intent intentUpdate = new Intent(ctx, UpdateService.class);
		intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		PendingIntent pendingIntentUpdate = PendingIntent.getService(ctx, widgetId, intentUpdate, PendingIntent.FLAG_UPDATE_CURRENT);
		
		return pendingIntentUpdate;
	}
	
	public static void startIntentService(Context ctx, int widgetId) {
		Log.d(WidgetProvider.TAG, "Attempting to start service from provider");
		Intent serviceIntent = new Intent(ctx, UpdateService.class);
		serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		ctx.startService(serviceIntent);
		
	}

}

package com.tattid.metrolukkari;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class MetroSchedIntentService extends IntentService {
	
	private int appWidgetId;
	
	public MetroSchedIntentService() {
		super("MetroSchedIntentService");
		Log.d(MetrolukkariWidget.TAG, "UpdateService launched");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(MetrolukkariWidget.TAG, "Handling intent");
		
		// Get widgetId from extras
		Bundle extras = intent.getExtras();
		this.appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		
		// Get URI from shared preferences
		URL url = getURL(appWidgetId);
		
		// Fetch and parse data
		List<Map<String, String>> resultMapList = requestAndParse(url);
		
		long updateTimeMillis = Long.parseLong((resultMapList.get(0).get("end")));
		
		buildUpdate(appWidgetId, resultMapList);
		
		// Create timer for next update
		createAlarmTimer(this, appWidgetId, updateTimeMillis);
		
		// Update widget
		//AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
		//RemoteViews remoteView = new RemoteViews(getPackageName(), R.layout.widget);
		//widgetManager.updateAppWidget(appWidgetId, remoteView);
	}
	
	// Build and deploy update to widget
	public void buildUpdate(int widgetId, List<Map<String, String>> resultMapList) {
		Log.d(MetrolukkariWidget.TAG, "Building");
		RemoteViews view = new RemoteViews(getPackageName(), R.layout.widget);
		
		Intent intent = new Intent(this, MetrolukkariWidget.class);
		intent.setAction(MetrolukkariWidget.METRO_ACTION_CLICK);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		view.setOnClickPendingIntent(R.id.widgetlayout, pendingIntent);
		
		Iterator<Map<String, String>> i = resultMapList.iterator();
		Map<String, String> resultMap;
		
		String start, end;
		
		while(i.hasNext()) {
			resultMap = i.next();
			
			start = DateUtils.millisToLocalReadable(DateUtils.unixTimeStringToMillis(resultMap.get("start")));
			end = DateUtils.millisToLocalReadable(DateUtils.unixTimeStringToMillis(resultMap.get("end")));
			
			view.setTextViewText(R.id.class1, start + " - " + end + " " + resultMap.get("subject"));
		}
		
		view.setTextViewText(R.id.date, "Updating...");
		
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		
		manager.updateAppWidget(widgetId, view);
	}
	
	public URL getURL(int widgetId) {
		SharedPreferences prefs = getSharedPreferences(MetrolukkariConfig.PREFS_NAME, 0);	
		String group = prefs.getString("group#" + widgetId, "");
		
		URL url = null;	
		
		try {
			url = new URL(MetrolukkariWidget.URIString + group);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		Log.d(MetrolukkariWidget.TAG, "Got URI " + url.toString());
		return url;
	}
	
	public List<Map<String, String>> requestAndParse(URL url) {
		Log.d(MetrolukkariWidget.TAG, "Fetching & parsing");
		HttpURLConnection urlConnection = null;

		String result = null;
		
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
			Log.d(MetrolukkariWidget.TAG, result);
		} catch(ClientProtocolException e) {
			e.printStackTrace();
		} catch(IOException e) {
			Log.d(MetrolukkariWidget.TAG, "IOException: " + e.getMessage());
			e.printStackTrace();
		} finally {
			urlConnection.disconnect();
		}
		
		List<Map<String, String>> resultMapList = new ArrayList<Map<String, String>>();
		
		// Parse JSON here
		try {
			JSONObject jObject = new JSONObject(result);
			JSONArray jArray = jObject.getJSONArray("events");
			
			for(int i=0; i<1; i++) {
				JSONObject item = jArray.getJSONObject(i);
				Map<String, String> resultMap = new HashMap<String, String>();
				
				String start = item.getString("start");
				String end = item.getString("end");
				String subject = item.getString("subject");
				String roomId = item.getString("roomid");
				
				resultMap.put("start", start);
				resultMap.put("end", end);
				resultMap.put("subject", subject);
				resultMap.put("roomId", roomId);
				
				ScheduleDataSource dataSource = new ScheduleDataSource(getApplicationContext(), appWidgetId);
				
				dataSource.push(subject, Long.parseLong(start), Long.parseLong(end), roomId);
				
				Log.d(MetrolukkariWidget.TAG, resultMap.toString());
				
				resultMapList.add(resultMap);
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return resultMapList;
	}
	
	public void createAlarmTimer(Context ctx, int widgetId, long time) {
		// Check for already running alarm timer
		Intent intent = new Intent(ctx, MetroSchedIntentService.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, widgetId, intent, PendingIntent.FLAG_NO_CREATE);

		if (pendingIntent == null) {
			time = time * 1000;
			
			pendingIntent = getSyncPendingIntent(ctx, widgetId);

			AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(ctx.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC, time, pendingIntent);
		} else {
			Log.d(MetrolukkariWidget.TAG, "AlarmManager already running for widget instance #" + widgetId);
		}
	}
	
	// Returns synced pendingIndents for identifying
	public static PendingIntent getSyncPendingIntent(Context ctx, int widgetId) {
		Intent intentUpdate = new Intent(ctx, MetroSchedIntentService.class);
		intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		PendingIntent pendingIntentUpdate = PendingIntent.getService(ctx, widgetId, intentUpdate, PendingIntent.FLAG_UPDATE_CURRENT);
		
		return pendingIntentUpdate;
	}
	
	public static void startIntentService(Context ctx, int widgetId) {
		Log.d(MetrolukkariWidget.TAG, "Attempting to start service from provider");
		Intent serviceIntent = new Intent(ctx, MetroSchedIntentService.class);
		serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		ctx.startService(serviceIntent);
		
	}

}

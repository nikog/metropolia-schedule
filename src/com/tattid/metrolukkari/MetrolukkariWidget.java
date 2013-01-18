package com.tattid.metrolukkari;

import android.app.AlarmManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class MetrolukkariWidget extends AppWidgetProvider {
	final static String TAG = "MetLuk";

	public static final String METRO_ACTION_CLICK = "com.nikog.metrosched.widget.action.click";
	public static final String METRO_ACTION_REFRESH = "com.tattid.metrolukkari.widget.action.WIDGET_UPDATE";
	// Available textViews.
	public final static int[] TEXTVIEWS = { R.id.class1, R.id.class2, R.id.class3 };
	// The URL to get data from. Group name will be appended to the end.
	public static String URIString = "http://mob.metropolia.fi/lukkarit/rest/index.php?rt=index/groupSchedule/";

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		int widgetId;
		RemoteViews remoteView;

		// Looping in case of multiple widgets
		for (int i = 0; i < appWidgetIds.length; i++) {
			widgetId = appWidgetIds[i];
			
			SharedPreferences prefs = context.getSharedPreferences(MetrolukkariConfig.PREFS_NAME, 0);	
			String group = prefs.getString("group#" + widgetId, "");
			
			// Update only if group has been set in preferences
			if(!group.equals("")) {	
				MetroSchedIntentService.startIntentService(context, widgetId);
	
				// Update widget (useless ?)
				remoteView = new RemoteViews(context.getPackageName(), R.layout.widget);
				appWidgetManager.updateAppWidget(widgetId, remoteView);
			}
		}

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive: " + intent.getAction());

		// Check if the received broadcast contains our custom action
		if (intent.getAction().equals(METRO_ACTION_REFRESH)) {

			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.widget);

			// Get added extras from the intent
			Bundle extras = intent.getExtras();

			if (extras != null) {
				int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

				// Announce update
				Log.d(TAG, "Manual widget update on " + appWidgetId);
				remoteView.setTextViewText(R.id.date, "Updating...");

				appWidgetManager.updateAppWidget(appWidgetId, remoteView);
			}
		}

		super.onReceive(context, intent);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIdList) {
		super.onDeleted(context, appWidgetIdList);
		
		int widgetId;
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);

		for (int i = 0; i < appWidgetIdList.length; i++) {
			widgetId = appWidgetIdList[i];
			
			// Cancel upcoming updates
			alarmManager.cancel(MetroSchedIntentService.getSyncPendingIntent(context, widgetId));
			
			// Delete preferences
			SharedPreferences.Editor prefs = context.getSharedPreferences(MetrolukkariConfig.PREFS_NAME, 0).edit();	
			prefs.remove("group#" + widgetId);
			prefs.commit();
			
			// Drop SQLite table
			ScheduleDataSource dataSource = new ScheduleDataSource(context, widgetId);
			dataSource.open();
			dataSource.dropTable();
			dataSource.close();
			
			
			Log.d(TAG, "Deleting widget with id #" + widgetId);
		}
	}
	
	
}

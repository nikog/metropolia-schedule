package com.tattid.metrolukkari;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class MetrolukkariConfig extends PreferenceActivity {
	public static final String PREFS_NAME = "com.tattid.metrolukkari.metrolukkariconfig";

	public static String METRO_ACTION_REFRESH = "com.tattid.metrolukkari.widget.action.WIDGET_UPDATE";

	int widgetId;

	static String textColor = "white";
	static String group = "to10";

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		// Bail if intent didn't include widgetId
		widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}
		
		// Default result
		setResult(RESULT_CANCELED);
	}

	// For now, back button will accept the settings and create the widget
	@Override
	public void onBackPressed() {
		// Get default preferences from PreferenceActivity
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String group = prefs.getString("group", null);

		// Get actual per widget preferences and store
		prefs = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor prefsEditor = prefs.edit();
		prefsEditor.putString("group#" + widgetId, group);
		prefsEditor.commit();

		// Launch widget
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		setResult(RESULT_OK, resultValue);

		// Launch service
		startIntentService(getApplicationContext(), widgetId);
				
		finish();
	}
	
	public void startIntentService(Context ctx, int widgetId) {
		Log.d(MetrolukkariWidget.TAG, "Attempting to start service from config");
		Intent serviceIntent = new Intent(ctx, MetroSchedIntentService.class);
		serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		ctx.startService(serviceIntent);	
	}
}
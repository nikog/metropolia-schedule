package com.tattid.metrolukkari;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class MetrolukkariWidget extends AppWidgetProvider {
	final static String TAG = "MetLuk";
	public static String METRO_ACTION_REFRESH = "com.tattid.metrolukkari.widget.action.WIDGET_UPDATE";
	// Available textViews.
	final static int[] textViews = {R.id.class1, R.id.class2, R.id.class3};
	// The URL to get data from. Group name will be appended to the end.
	private static String URL = "https://lukkarit.metropolia.fi/mobile/lukkarit/t2_haku.php?lang=fi&sws%5B%5D=";
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIdList){
		
		final int N = appWidgetIdList.length;
		
		int appWidgetId;
		RemoteViews remoteView;
		
		// Looping in case of multiple widgets
		for (int i = 0; i < N; i++) {
			Log.d(TAG, "onUpdate widget #"+appWidgetIdList[i]);
			
			appWidgetId = appWidgetIdList[i];
			
			//Get the widget view for UI updates
			remoteView = new RemoteViews(context.getPackageName(), R.layout.widget);
			
			remoteView.setOnClickPendingIntent(R.id.widgetlayout, createPendingIntent(context, appWidgetId));
			
			remoteView.setTextViewText(R.id.date, "Tap here to update");

			// Update the widget
			appWidgetManager.updateAppWidget(appWidgetId, remoteView);
				
		}
		
		super.onUpdate(context, appWidgetManager, appWidgetIdList);
	}

	@Override
	public void onReceive(Context context, Intent intent) {		
		Log.d(TAG, "onReceive: "+intent.getAction());
		
		// Check if the received broadcast containts our custom action
		if(intent.getAction().equals(METRO_ACTION_REFRESH)) {
			
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.widget);
			
			// Get added extras from the intent
			Bundle extras = intent.getExtras();
			
			if(extras != null) {
				int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 
											    AppWidgetManager.INVALID_APPWIDGET_ID);
				
				// Announce update
				Log.d(TAG, "Manual widget update on "+appWidgetId);
				remoteView.setTextViewText(R.id.date, "Updating...");
				
				// Load preferences
				CharSequence[] results = MetrolukkariConfig.loadPref(context, appWidgetId);
				// Append group to end of URL
				StringBuffer sb = new StringBuffer(85);
				String URLwGroup;
				URLwGroup = sb.append(URL).append(results[0]).toString();
				
				// Attempt to parse the color
				int color;
				try {
					color = Color.parseColor((String) results[1]);
				} catch(Exception e) {
					Log.d(TAG, "Error parsing color. Defaulting to white");
					color = Color.WHITE;
				}
				
				// Update colors
				remoteView.setTextColor(R.id.date, color);
				for(int textViewId : textViews) {
					remoteView.setTextColor(textViewId, color);
				}
				
				remoteView.setOnClickPendingIntent(R.id.widgetlayout, createPendingIntent(context, appWidgetId));
				
				appWidgetManager.updateAppWidget(appWidgetId, remoteView);
				
				AsyncHttpRequest asyncRequest = new AsyncHttpRequest(context.getApplicationContext(), appWidgetId);

				asyncRequest.execute(URLwGroup);
			}			
		}
		super.onReceive(context, intent);
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIdList) {
		super.onDeleted(context, appWidgetIdList);
		
		final int N = appWidgetIdList.length;
		
		for (int i = 0; i < N; i++) {
			Log.d(TAG, "onDelete widget #"+appWidgetIdList[i]);
		}
	}
	
	// Generates pendingIntent to attach on the onClick listener of the layout
	public static PendingIntent createPendingIntent(Context context, int appWidgetId) {
		Intent intent = new Intent(context, MetrolukkariWidget.class);
		intent.setAction(METRO_ACTION_REFRESH);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 
													appWidgetId, 
													intent, 
													PendingIntent.FLAG_UPDATE_CURRENT);
		return pendingIntent;
	}
	
	// Enter updated widget data to remoteView
	// Doesn't actually update, this is done through 
	// widgetManager.updateAppWidget()
	private static void updateRemoteView(Context context, int appWidgetId, 
			AsyncResponse response, RemoteViews remoteView) {
		
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		
		// Check if http connection had errors
		if(!response.getErr()) {
			// Parse the responsestring to parsed list
			ArrayList<String> outputList = new ArrayList<String>();
			
			try {
				outputList = parseSite(response.getResponseStream());
			} catch(IOException e) {
				e.printStackTrace();
			}
			
			int j = 0;
			
			// Check if the list only has the title in it
			// ie. No classes today! 
			// (This probably cannot happen because if there are no classes on some day
			// that date will not be listed and the site will show the next date)
			if(!outputList.isEmpty()) {
				// Clear textviews because we have new content coming up
				for(int textViewId : textViews) {
					remoteView.setTextViewText(textViewId, "");
				}
				// Loop through strings returned from the parser
				// and add them to the corresponding textview
				// Current max amount is 4 strings/classes
				boolean dateFound = false;
				
				for(String tOutput : outputList) {
					if(!dateFound) {
						remoteView.setTextViewText(R.id.date, tOutput);
						dateFound = true;
					} else {
						// Just incase we don't try to update textviews that 
						// we have no new content for
						if(j < textViews.length) {
							// Set textview content 
							remoteView.setTextViewText(textViews[j], tOutput);
							j++;
						} else {
							break;
						}
					}
				}
			} else {
				// Website didn't return the data we wanted for some unknown reason
				// TODO: Use the errors returned by the site itself
				remoteView.setTextViewText(R.id.date, "Data not currently available");
				Log.d(TAG, "No data available");
			}
			
		} else {
			// Show returned error
			remoteView.setTextViewText(R.id.date, response.getResponseError());
		}
		remoteView.setOnClickPendingIntent(R.id.widgetlayout, createPendingIntent(context, appWidgetId));
		
		appWidgetManager.updateAppWidget(appWidgetId, remoteView);
	}
	
	// Parses <tr> fields and their subelements from a given HTML-string
	// Returns values in arraylist with date in first cell and classes in the rest
	private static ArrayList<String> parseSite(InputStream responseStream) throws IOException {

		Document doc = Jsoup.parse(responseStream, null, URL);
		
		Elements trList = null;
		
		String name = null;
		Element time = null;
		Element prevTime = null;
		Element date = null;
		Element location = null;
		
		//date = doc.select("table.cell_1 td.cell_2").first();
		trList = doc.select("table.cell_1 tbody tr");
		
		boolean first = true;

		ArrayList<String> outputList = new ArrayList<String>();
		
		Pattern regex = Pattern.compile("Opinnon nimi:(.*?)\\<");
		Matcher m;
		
		for(Element tr : trList) {
			// Check if current element contains date instead of class
			if(tr.select("td").first().hasClass("cell_2")) {
				// If this is the first time we find a date
				// the date will be taken for usage
				if(first == true) {
					date = tr.select("td.cell_2").first();
					outputList.add(date.html());
					first = false;
				// If this is a second time we find a date
				// we have already looped through todays classes
				} else {
					break;
				}
			} else {
				time = tr.select("td").get(0);
				location = tr.select("td").get(1);
				
				// Find course name with regex from some weird comment block
				// due to that being the only place with full course name
				m = regex.matcher(tr.html());
				if(m.find()) {
					name = m.group(1);
				}
				
				// Notice on classes that start at the same time
				String prefix = "";
				if(prevTime != null) {
					if(time.html().equals(prevTime.html())) {
						prefix = "! ";
					}
				}
				prevTime = time;
				
				StringBuilder sb = new StringBuilder();
				sb.append(prefix);
				sb.append(time.html().replace("&nbsp;", ""));
				sb.append("\t");
				sb.append(location.html());
				sb.append("\t");
				sb.append(name);
				
				// Append the values to a single string and add it to list
				outputList.add(sb.toString());
			}
		}
		
		return outputList;
	}

	// AsyncTask for creating asynchronous http requests
	private class AsyncHttpRequest extends AsyncTask<CharSequence, CharSequence, AsyncResponse> {
		private Context context;
		private int appWidgetId;
		
		// Context and appWidgetId are required to find the widget we want to update
		public AsyncHttpRequest(Context context, int appWidgetId) {
			super();
			this.context = context;
			this.appWidgetId = appWidgetId;
		}
		
		// Executed on asynctask.execute()
		// Contains some http and buffer mumbo jumbo
		@Override
		protected AsyncResponse doInBackground(CharSequence... uri) {
			Log.d(TAG, "Initiating AsyncHttpRequest");
			
			// Custom HttpClient for using custom certificate keystore
			// due to Metropolia's self signed certificates
			DefaultHttpClient httpclient = new CustomHttpClient(context);
			
			HttpResponse response;
			
			CharSequence responseString = null;
			
			InputStream responseStream = null;
			
			boolean error = false;
			
			BufferedReader rd = null;
			StringBuilder sb = new StringBuilder();
			
			try {			
				response = httpclient.execute(new HttpGet((String)uri[0]));
				
				responseStream = response.getEntity().getContent();
				/*
				rd = new BufferedReader(new InputStreamReader(
						response.getEntity().getContent()));
				CharSequence line = "";
				while((line = rd.readLine()) != null) {
					sb.append(line);
				}
				responseString = sb.toString();	
				*/		
			} catch(Exception e) {
				Log.d(TAG, "AsyncHttpRequest error: "+e.toString());
			} finally {
				if(rd != null) {
					try {
						rd.close();
					} catch(IOException e) {
						Log.d(TAG, e.toString());
					}
				}
			}
			
			return new AsyncResponse(responseStream, error);
		}
			
		// Done after doInBackground() finishes
		@Override
		protected void onPostExecute(AsyncResponse response) {
			Log.d(TAG, "AsyncHttpRequest complete");
			
			// Create remoteView from the data inside the supplied context
			RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.widget);
			
			// Enter updated info to remoteView
			updateRemoteView(context, appWidgetId, response, remoteView);
		}
	}
}

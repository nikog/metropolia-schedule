package com.tattid.metrolukkari;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.tattid.metrolukkari.R;

public class MetrolukkariConfig extends Activity {
	private static final String PREFS_NAME = "com.tattid.metrolukkari.metrolukkariconfig";
	
	public static String METRO_ACTION_REFRESH = "com.tattid.metrolukkari.widget.action.WIDGET_UPDATE";

	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	
	static String textColor = "white";
	static String group = "to10";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setResult(RESULT_CANCELED);
        
        setContentView(R.layout.config);
        
        findViewById(R.id.addButton).setOnClickListener(addWidget);
        
        ListView listView = (ListView) findViewById(R.id.list);
        
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        
        Map<String, String> datum = new HashMap<String, String>(2);
        datum.put("title", getString(R.string.config_groupid_title));
        datum.put("value", getString(R.string.config_groupid_subtitle));
        data.add(datum);
        
        datum = new HashMap<String, String>(2);
        datum.put("title", getString(R.string.config_textcolor_title));
        datum.put("value", getString(R.string.config_textcolor_subtitle));
        data.add(datum);
        
        SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2,
        											new String[] {"title", "value"},
        											new int[] {android.R.id.text1, android.R.id.text2});

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
        		final EditText input = new EditText(MetrolukkariConfig.this);
        		if(id == 0) {
        			input.setText(group);
	        		AlertDialog.Builder builder = new AlertDialog.Builder(MetrolukkariConfig.this)
	        			.setTitle(getString(R.string.config_groupid_title))
	        			.setView(input)
	        			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {					
							@Override
							public void onClick(DialogInterface dialog, int which) {
								MetrolukkariConfig.group = input.getText().toString();
							}
						}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// Canceled	
							}
						});
	        		AlertDialog dialog = builder.create();
	        		input.requestFocus();
	        		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	        		dialog.show();
        		} else if(id == 1) {
        			input.setText(textColor);
        			AlertDialog.Builder builder = new AlertDialog.Builder(MetrolukkariConfig.this)
        			.setTitle(getString(R.string.config_textcolor_title))
        			.setView(input)
        			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {					
						@Override
						public void onClick(DialogInterface dialog, int which) {
							MetrolukkariConfig.textColor = input.getText().toString();
						}
					}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Canceled	
						}
					});
        			AlertDialog dialog = builder.create();
	        		input.requestFocus();
	        		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	        		dialog.show();
        		}
        	}
        });
        
        
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null) {
        	mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if(mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
        	finish();
        }
    }
    
    private OnClickListener addWidget = new OnClickListener() {
    	public void onClick(View v) {
    		final Context context = MetrolukkariConfig.this;
    		
    		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    		
    		Intent resultValue = new Intent();
    		resultValue.setAction(METRO_ACTION_REFRESH);
    		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
    		
    		MetrolukkariConfig.savePref(context, mAppWidgetId, group, textColor);
    		setResult(RESULT_OK, resultValue);
    		context.sendBroadcast(resultValue);
    		finish();
    	}
    };
    
    static void savePref(Context context, int appWidgetId, String group, String tColor) {
    	SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
    	prefs.putString("group#" + appWidgetId, group);
    	prefs.putString("tcolor#" + appWidgetId, tColor);
    	prefs.commit();
    }
    static CharSequence[] loadPref(Context context, int appWidgetId) {
    	SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
    	CharSequence[] prefArray = {prefs.getString("group#" + appWidgetId, null),
    							prefs.getString("tcolor#" + appWidgetId, null)};
    	return prefArray;
    }
}
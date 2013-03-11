package com.nikog.metropolia.schedule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.os.Environment;
import android.util.Log;

public class Logger {
	public static void logCatToSDCard() {
		try {
			long time = System.currentTimeMillis();
			File filename = new File(Environment.getExternalStorageDirectory(), "asdlukkarilogcat_" + time);
			filename.createNewFile();
			
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("logcat");
			cmd.add("-d");
			cmd.add("-f");
			cmd.add(filename.getAbsolutePath());
			cmd.add("MetropoliaSchedule:D");
			
			Runtime.getRuntime().exec(cmd.toArray(new String[0]));
		} catch(IOException e) {
			Log.d(WidgetProvider.TAG, e.getMessage());
		}
	}
}

package com.tattid.metrolukkari;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {
	public int widgetId;
	
	public static final String TABLE_SCHEDULE = "schedule";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_START = "start";
	public static final String COLUMN_END = "end";
	public static final String COLUMN_SUBJECT = "subject";
	public static final String COLUMN_ROOMID = "roomid";
	
	private static final String DATABASE_NAME = "schedule.db";
	private static final int DATABASE_VERSION = 1;

	public MySQLiteHelper(Context context, int widgetId) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.widgetId = widgetId;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		Log.d(MetrolukkariWidget.TAG, "widgetId in SQLiteHelper is " + widgetId);
		String DATABASE_CREATE = "create table "
				+ TABLE_SCHEDULE + widgetId +"("
				+ COLUMN_ID + " integer primary key autoincrement, "
				+ COLUMN_START + " integer, "
				+ COLUMN_END + " integer, "
				+ COLUMN_SUBJECT + " text, "
				+ COLUMN_ROOMID + ");";
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int arg1, int arg2) {
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULE + widgetId);
		onCreate(database);
	}
	

}

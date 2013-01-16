package com.tattid.metrolukkari;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class ScheduleDataSource {
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	
	private int widgetId;
	
	public ScheduleDataSource(Context ctx, int widgetId) {
		this.widgetId = widgetId;
		dbHelper = new MySQLiteHelper(ctx, widgetId);
	}
	
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	
	public void close() {
		dbHelper.close();
	}
	
	public void deleteOld() {
		long time = System.currentTimeMillis();
		
		String query = "DELETE * FROM " + MySQLiteHelper.TABLE_SCHEDULE + widgetId
				+ " WHERE " + MySQLiteHelper.COLUMN_END + " < " + time;
		Cursor cursor = database.rawQuery(query, null);
	}
	
	public void push(String subject, long start, long end, String roomId) {
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_SUBJECT, subject);
		values.put(MySQLiteHelper.COLUMN_START, subject);
		values.put(MySQLiteHelper.COLUMN_END, subject);
		values.put(MySQLiteHelper.COLUMN_ROOMID, subject);
		
		database.insert(MySQLiteHelper.TABLE_SCHEDULE + widgetId, null, values);
	}
	
	public List<Event> getUpcoming(int limit) {
		long time = System.currentTimeMillis();
		
		String query = "SELECT * FROM " + MySQLiteHelper.TABLE_SCHEDULE + widgetId
				+ " WHERE " + MySQLiteHelper.COLUMN_END + " > "  + time
				+ " ORDER BY " + MySQLiteHelper.COLUMN_ID + " ASC LIMIT " + limit;
		Cursor cursor = database.rawQuery(query, null);
		
		List<Event> eventList = new ArrayList<Event>();
		
		cursor.moveToFirst();
		do {
			eventList.add(new Event(cursor.getString(0), cursor.getLong(1), cursor.getLong(2), cursor.getString(3)));
		} while(cursor.moveToNext());
				
		return eventList;
	}
	
	public Cursor getFirstRow() {
		String query = "SELECT * FROM " + MySQLiteHelper.TABLE_SCHEDULE 
				+ " ORDER BY " + MySQLiteHelper.COLUMN_ID + " ASC LIMIT 1";
		Cursor cursor = database.rawQuery(query, null);
		
		return cursor;
	}
	
	public long first() {
		long time = System.currentTimeMillis();
		
		String query = "SELECT * FROM " + MySQLiteHelper.TABLE_SCHEDULE 
				+ " WHERE " + MySQLiteHelper.COLUMN_END + " > "  + time
				+ " ORDER BY " + MySQLiteHelper.COLUMN_END + " ASC LIMIT 1";
		Cursor cursor = database.rawQuery(query, null);
		
		return cursor.getLong(1);
	}
	
	public long last() {
		String query = "SELECT * FROM " + MySQLiteHelper.TABLE_SCHEDULE 
				+ " ORDER BY " + MySQLiteHelper.COLUMN_END + " DESC LIMIT 1";
		Cursor cursor = database.rawQuery(query, null);
		
		return cursor.getLong(1);
	}
	
	public void dropTable() {
		database.execSQL("DROP TABLE IF EXISTS " + MySQLiteHelper.TABLE_SCHEDULE + widgetId);
	}
}

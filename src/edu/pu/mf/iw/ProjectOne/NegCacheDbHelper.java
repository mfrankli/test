package edu.pu.mf.iw.ProjectOne;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;

public class NegCacheDbHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "trust_data";
	public static final String DATABASE_CREATE = "create table if not exists neg_cache (_id integer primary key autoincrement, uuid text, num_fails int, last_failed bigint, last_succeeded bigint);";
	
	public NegCacheDbHelper(Context context) {
		super(context, DATABASE_NAME, null, 1);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		//database.execSQL("DROP TABLE IF EXISTS trust");
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS neg_cache");
		onCreate(db);
	}

}

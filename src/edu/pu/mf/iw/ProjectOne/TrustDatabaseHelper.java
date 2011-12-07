package edu.pu.mf.iw.ProjectOne;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;

public class TrustDatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "trust_data";
	public static final String DATABASE_CREATE = "create table trust (_id integer primary key autoincrement, pubkey text, distance integer, uuid text, readable_id text, source text, attestation text);";
	
	public TrustDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, 1);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		//database.execSQL("DROP TABLE IF EXISTS trust");
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS trust");
		onCreate(db);
	}

}

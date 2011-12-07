package edu.pu.mf.iw.ProjectOne;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class NegCacheDbAdapter {
	public static final String KEY_UUID = "uuid";
	public static final String KEY_FAILS = "num_fails";
	public static final String KEY_LAST_FAIL = "last_failed";
	public static final String KEY_LAST_SUCCEED = "last_succeeded";
	private static final String DATABASE_TABLE = "neg_cache";
	private Context context;
	private SQLiteDatabase database;
	private NegCacheDbHelper helper;
	
	public NegCacheDbAdapter(Context context) {
		this.context = context;
	}
	
	public NegCacheDbAdapter open() throws SQLException {
		helper = new NegCacheDbHelper(context);
		database = helper.getWritableDatabase();
		helper.onCreate(database);
		//database.execSQL("DROP TABLE IF EXISTS neg_cache");
		//helper.onCreate(database);
		//database.execSQL(TrustDatabaseHelper.DATABASE_CREATE);
		return this;
	}
	
	public boolean inTransaction() {
		return database.inTransaction();
	}
	
	public void close() {
		helper.close();
	}
	
	public void reset() {
		helper.onUpgrade(database, 0, 0);
	}
	
	public long createCacheEntry(String uuid, int numFails, long lastFail, long lastSucceed) {
		ContentValues initialValues = createContentValues(uuid, numFails, lastFail, lastSucceed);
		return database.insert(DATABASE_TABLE, null, initialValues);
	}
	
	public boolean updateCacheEntry(String uuid, int numFails, long lastFail, long lastSucceed) {
		ContentValues updateValues = new ContentValues();
		if (uuid == null) return false;
		if (numFails != -1) updateValues.put(KEY_FAILS, numFails);
		if (lastFail != -1) updateValues.put(KEY_LAST_FAIL, lastFail);
		if (lastSucceed != -1) updateValues.put(KEY_LAST_SUCCEED, lastSucceed);
		uuid = DatabaseUtils.sqlEscapeString(uuid);
		return database.update(DATABASE_TABLE, updateValues, KEY_UUID + "=" + uuid, null) > 0;
	}
	
	public boolean isOpen() {
		return database.isOpen();
	}
	
	public boolean deleteCacheEntry(String uuid) {
		return database.delete(DATABASE_TABLE, KEY_UUID + "=" + uuid, null) > 0;
	}
	
	public Cursor selectAllEntries() {
		String query = "select * from neg_cache";
		Cursor mCursor = database.rawQuery(query, new String[] {});
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	public Cursor selectEntryByUuid(String uuid) throws SQLException {
		String query = "select * from neg_cache where " + KEY_UUID + "=?";
		Cursor mCursor = database.rawQuery(query, new String[] {uuid});
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	private ContentValues createContentValues(String uuid, int numFails, long lastFail, long lastSucceed) {
		ContentValues values = new ContentValues();
		values.put(KEY_UUID, uuid);
		values.put(KEY_FAILS, numFails);
		values.put(KEY_LAST_FAIL, lastFail);
		values.put(KEY_LAST_SUCCEED, lastSucceed);
		return values;
	}
	
}

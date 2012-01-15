package edu.pu.mf.iw.ProjectOne;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class TrustDbAdapter {
	public static final String KEY_PUBKEY = "pubkey";
	public static final String KEY_DIST = "distance";
	public static final String KEY_UUID = "uuid";
	public static final String KEY_NAME = "readable_id";
	public static final String KEY_SOURCE = "source";
	public static final String KEY_ATTEST = "attestation";
	public static final String KEY_MAC = "macaddr";
	private static final String DATABASE_TABLE = "trust";
	public static final int TO_HEX = 0;
	public static final int FROM_HEX = 1;
	private Context context;
	private SQLiteDatabase database;
	private TrustDatabaseHelper helper;
	
	public TrustDbAdapter(Context context) {
		this.context = context;
	}
	
	public TrustDbAdapter open() throws SQLException {
		helper = new TrustDatabaseHelper(context);
		database = helper.getWritableDatabase();
		//database.execSQL("DROP TABLE IF EXISTS trust");
		//database.execSQL(TrustDatabaseHelper.DATABASE_CREATE);
		return this;
	}
	
	public void close() {
		helper.close();
	}
	
	public void reset() {
		helper.onUpgrade(database, 0, 0);
	}
	
	public long createTrustEntry(String pubKey, int distance, String uuid, String name, String source, String attest, String macAddr) {
		ContentValues initialValues = createContentValues(pubKey, distance, uuid, name, source, attest, macAddr);
		return database.insert(DATABASE_TABLE, null, initialValues);
	}
	
	public boolean updateTrustEntry(String pubKey, int distance, String uuid, String name, String source, String attest, String macAddr) {
		ContentValues updateValues = new ContentValues();
		if (distance != -1) updateValues.put(KEY_DIST, distance);
		if (uuid != null) updateValues.put(KEY_UUID, uuid);
		if (name != null) updateValues.put(KEY_NAME,  name);
		if (attest != null) updateValues.put(KEY_ATTEST, attest);
		if (source != null) updateValues.put(KEY_SOURCE, source);
		if (macAddr != null) updateValues.put(KEY_MAC, macAddr);
		pubKey = DatabaseUtils.sqlEscapeString(pubKey);
		uuid = DatabaseUtils.sqlEscapeString(uuid);
		return database.update(DATABASE_TABLE, updateValues, KEY_PUBKEY + "=" + pubKey + " OR " + KEY_UUID + "=" + uuid, null) > 0;
	}
	
	public boolean isOpen() {
		return database.isOpen();
	}
	
	public boolean deleteTrustEntry(String pubKey) {
		return database.delete(DATABASE_TABLE, KEY_PUBKEY + "=" + pubKey, null) > 0;
	}
	
	public Cursor selectAllEntries() {
		String query = "select * from trust";
		Cursor mCursor = database.rawQuery(query, new String[] {});
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	
	public Cursor selectEntryByKey(String pubKey) throws SQLException {
		String query = "select * from trust where " + KEY_PUBKEY + " = ?";
		Cursor mCursor = database.rawQuery(query, new String[] {pubKey});
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	public Cursor selectEntryByName(String name) throws SQLException {
		String query = "select * from trust where " + KEY_NAME + " = ?";
		Cursor mCursor = database.rawQuery(query, new String[] {name});
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	public Cursor selectEntryByUuid(String uuid) throws SQLException {
		String query = "select * from trust where " + KEY_UUID + " = ?";
		Cursor mCursor = database.rawQuery(query, new String[] {uuid});
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	private ContentValues createContentValues(String pubKey, int distance, String uuid, String name, String source, String attest, String macAddr) {
		ContentValues values = new ContentValues();
		values.put(KEY_PUBKEY, pubKey);
		values.put(KEY_DIST, distance);
		values.put(KEY_UUID, uuid);
		values.put(KEY_NAME, name);
		values.put(KEY_ATTEST, attest);
		values.put(KEY_MAC, macAddr);
		return values;
	}
	
}

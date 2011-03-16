package android.EZCart;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class History {
	
	public static final String TAG = "History.db";
	
	public static final String KEY_ITEM_ROWID = "_id";
	public static final String KEY_HISTORY_ITEM_NAME = "name";
	public static final String DATABASE_NAME = "History.db";
	public static final int DATABASE_VERSION = 1;
	public static final String HISTORY_TABLE_NAME = "history_table";
	
	private static final String CREATE_HISTORY_TABLE = "create table " + HISTORY_TABLE_NAME + " (" + KEY_ITEM_ROWID + " integer primary key autoincrement, "
    + KEY_HISTORY_ITEM_NAME + " text not null" + ");";
	
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_HISTORY_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS "+DATABASE_NAME);
			onCreate(db);
		}
		
	}
	
	public History (Context context) {
		this.mContext = context;
	}
	
	/**
	 * Opens writable database
	 * @return History database
	 * @throws SQLException
	 */
	public History open() throws SQLException {
		mHistoryDbHelper = new DatabaseHelper(mContext);
		mDb = mHistoryDbHelper.getWritableDatabase();
		return this;
	}
	
	/**
	 * Closes database
	 */
	public void close() {
		mHistoryDbHelper.close();
	}
	
	/**
	 * Adds item to history database
	 * @param name
	 * @return id of the newly inserted row in the table
	 */
	public long addItemToHistory (String name) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_HISTORY_ITEM_NAME, name);
		return mDb.insert(HISTORY_TABLE_NAME, null, initialValues);
	}
	
	public Cursor getItem (long id) {
		String[] columns = new String[] {KEY_ITEM_ROWID, KEY_HISTORY_ITEM_NAME};
		Cursor c = mDb.query(true, HISTORY_TABLE_NAME, columns, KEY_ITEM_ROWID + "=" + id, null, null, null, null, null);
		if (c!=null) c.moveToFirst();
		return c;
	}
	
	public boolean updateItem(long id, String name) {
		ContentValues values = new ContentValues();
		values.put(KEY_HISTORY_ITEM_NAME, name);
		return mDb.update(HISTORY_TABLE_NAME, values, KEY_ITEM_ROWID + "=" + id, null) > 0;
	}
	/**
	 * Gets all items from history and returns array list 
	 *  
	 * @return sorted ArrayList<String> with all items in history 
	 */
	public ArrayList<String> getHistory () {
		String[] columns = new String[] {KEY_HISTORY_ITEM_NAME};
		Cursor c = mDb.query(HISTORY_TABLE_NAME, columns, null, null, null, null, KEY_HISTORY_ITEM_NAME);
		ArrayList<String> result = new ArrayList<String>();
		c.moveToFirst();
		for (c.move(-1); c.moveToNext(); c.isAfterLast()) {
			result.add(c.getString(0));
		}
		return result;
	}
	/**
	 * Gets all items from history and returns cursor
	 * @return cursor with id and name columns of all items in history database 
	 */
	public Cursor getHistoryCursor() {
		String[] columns = new String[] {KEY_ITEM_ROWID, KEY_HISTORY_ITEM_NAME};
		return mDb.query(HISTORY_TABLE_NAME, columns, null, null, null, null, KEY_HISTORY_ITEM_NAME);
	}
	
	/**
	 * Checks if item with given name exists already
	 * @param name name of the item
	 * @return true if exists, false if it doesn't
	 */
	public boolean itemExists(String name) {
		ArrayList<String> list = getHistory();
		return list.contains(name);
	}
	/**
	 * Removes item from history
	 * @param id id of the item in database
	 * @return true if successful, false if not 
	 */
	public boolean removeFromHistory(long id) {
		return mDb.delete(HISTORY_TABLE_NAME, KEY_ITEM_ROWID + "=" + id, null) > 0;
	}
	
	/**
	 * Clears history
	 * @return true if successful, false if not
	 */
	public boolean clearHistory() {
		return mDb.delete(HISTORY_TABLE_NAME, null, null) > 0;
	}
	
	private final Context mContext;
	private DatabaseHelper mHistoryDbHelper;
	private SQLiteDatabase mDb;
}

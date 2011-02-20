package android.EZCart;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelper {
	
	public static final String TAG="EZCart.db";
	
	// used to get id of the list in list table
	private static final int ID_COLUMN_INDEX = 0;
	
	/* Constants for following created lists */
	public static final String KEY_LIST_ROWID = "_id";
	public static final String KEY_LIST_NAME = "name";
	public static final String KEY_LIST_TABLE_NAME = "table_name";
		
	/* Constants needed for items in the list */
	public static final String KEY_ITEM_NAME="name";
	public static final String KEY_ITEM_QUANTITY="quantity";
	public static final String KEY_ITEM_VALUE="value";
	public static final String KEY_ITEM_TOTAL_ITEM_VALUE="totalvalue";
	public static final String KEY_ITEM_ROWID="_id";
	public static final String KEY_ITEM_DONE="done";
	public static final String KEY_ITEM_TABLE_NAME = "tablename";
	public static final String DATABASE_NAME="EZCart.db";
	public static final int DATABASE_VERSION=2;
	
	/* This constant is used to notify that there was problem creating table in DB */
	public static final long NOTIFY_TABLE_CREATION_PROBLEM = -2;
	
	public static final String LIST_TABLE_NAME = "list_table_name";
	
	private static final String CREATE_LIST_TABLE ="create table " + LIST_TABLE_NAME + " (" + KEY_LIST_ROWID + " integer primary key autoincrement, "
    + KEY_LIST_NAME + " text not null, " + KEY_LIST_TABLE_NAME + " text not null "  + ");";

	public final int LIST_NAME_COLUMN = 1;
	public final int TABLE_NAME_COLUMN = 2;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
			
			DatabaseHelper(Context context) {
				super(context, DATABASE_NAME, null, DATABASE_VERSION);
			}
	
			@Override
			public void onCreate(SQLiteDatabase db) {
				db.execSQL(CREATE_LIST_TABLE);
								
			}
	
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
	                    + newVersion + ", which will destroy all old data");
				db.execSQL("DROP TABLE IF EXISTS "+DATABASE_NAME);
				onCreate(db);
			}
			
		}
	
	public DbHelper (Context context) {
		this.mContext = context;
		
	}
	
	public DbHelper open() throws SQLException {
		mDbHelper = new DatabaseHelper(mContext);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		mDbHelper.close();
	}
	/**
	 * Inserts name of the newly created list in the lists table and 
	 * creates new table in data base using _id from the list table for
	 * name of newly created table
	 * 
	 * @param name name of the list
	 * @return row id of the newly inserted row or -1 if it cannot be created
	 */
	public long createList (String name) {
		long newPosition = 1;
		Cursor c = getAllLists();
		if (c.getCount()>0) {
			c.moveToLast();
			newPosition = c.getLong(ID_COLUMN_INDEX) + 1;
		}
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_LIST_NAME, name);
		name = "ListId_" + newPosition; 		
		initialValues.put(KEY_LIST_TABLE_NAME, name);
		
		/* this creates table in the DB for list */
		try {
			mDb.execSQL(createSQLStatementAdd(name));
			return mDb.insert(LIST_TABLE_NAME, null, initialValues);
		} catch (SQLException e) {
			return NOTIFY_TABLE_CREATION_PROBLEM;
		}
		
	}
	
	public void setListName(long id, String name) {
		ContentValues values = new ContentValues();
		values.put(KEY_LIST_NAME, name);
		mDb.update(LIST_TABLE_NAME, values, KEY_LIST_ROWID + "=" + id , null);
	}
	/**
	 * This method deletes row in the LIST_TABLE_NAME of the selected id
	 * and deletes appropriate table from the DB  
	 * @param id row id of the selected list in the LIST_TABLE_NAMES
	 * @return id of the deleted row
	 */
	public long removeList (long id) {
		String[] columns = new String[] {KEY_LIST_ROWID, KEY_LIST_NAME, KEY_LIST_TABLE_NAME};
		Cursor c = mDb.query(true, LIST_TABLE_NAME, columns, KEY_LIST_ROWID + "=" + id, null, null, null, null, null);
		if (c!=null) {
			c.moveToFirst();
		}
		String tableName = c.getString(TABLE_NAME_COLUMN);
		mDb.execSQL(createSQLStatementRemove(tableName));
		return mDb.delete(LIST_TABLE_NAME, KEY_LIST_ROWID + "=" + id, null);
		
	}
	/**
	 * This method removes all lists from database
	 * It removes entries from list table and drops all other tables
	 * 
	 * @return number of affected tables
	 */
	public int removeAllLists () {
		int counter = 0;
		String[] columns = new String[] {KEY_LIST_ROWID, KEY_LIST_NAME, KEY_LIST_TABLE_NAME};
		Cursor c = mDb.query(LIST_TABLE_NAME, columns, null, null, null, null, null);
		c.moveToFirst();
		for (c.move(-1); c.moveToNext(); c.isAfterLast()) {
			String tableName = c.getString(TABLE_NAME_COLUMN);
			mDb.execSQL(createSQLStatementRemove(tableName));
			counter++;
		}
		if (counter>0) mDb.delete(LIST_TABLE_NAME, null, null);
		return counter;
	}
	
	public Cursor getList(long id) {
		String[] columns = new String[] {KEY_LIST_ROWID, KEY_LIST_NAME, KEY_LIST_TABLE_NAME};
		Cursor c = mDb.query(true, LIST_TABLE_NAME, columns, KEY_LIST_ROWID + "=" + id, null, null, null, null, null);
		if (c!=null) {
			c.moveToFirst();
		}
		return c;
	}
	
	/**
	 * Gets all lists from the LIST_TABLE_NAME
	 * @return cursor with all lists
	 */
	public Cursor getAllLists() {
		String[] columns = new String[] {KEY_LIST_ROWID, KEY_LIST_NAME, KEY_LIST_TABLE_NAME};
		Cursor c = mDb.query(LIST_TABLE_NAME, columns, null, null, null, null, null, null);
		if (c!=null) {
			c.moveToFirst();
		}
		return c;
	}
	
	
	
	/**
	 * This method checks if list table is empty
	 * @return true if yes, false if no
	 */
	public boolean isListsEmpty() {
		Cursor temp = getAllLists();
		return !temp.moveToLast();
	}
	/**
	 * Creates SQL statement for creating table to be used with
	 * execSQL.
	 * This is used for creating multiple lists, this creates table
	 * in existing database that is used store items in list;
	 * 
	 * @param tableName name of the table to be created in Database
	 * @return
	 */
	private String createSQLStatementAdd (String tableName) {
		return "create table "+tableName+" (_id integer primary key autoincrement, "
        + DbHelper.KEY_ITEM_NAME + " text not null, " + DbHelper.KEY_ITEM_VALUE + " double not null, " 
        + DbHelper.KEY_ITEM_QUANTITY + " integer not null, " + DbHelper.KEY_ITEM_TOTAL_ITEM_VALUE + " double not null, " + DbHelper.KEY_ITEM_DONE +" boolean not null );";
	}
	/**	private static final int LIST_NAME_COLUMN = 1;

	 * Creates SQL statement for creating table to be used with execSQL.
	 * This is used for creating multiple lists, this deletes table
	 * in existing database that is used store items in list;
	 * @param tableName
	 * @return
	 */
	private String createSQLStatementRemove (String tableName) {
		return "drop table "+tableName;
	}
	
	/**
	 * This method inserts item in the list 
	 * @param table name of the list
	 * @param name name of the item
	 * @param value price of the item
	 * @param quantity number of items
	 * @param totalItemValue total value of the item (value*quantity)
	 * @param done true if item is bought, false if item is to be bought
	 * @return row id of the newly inserted row
	 */
	public long addItem (String table, String name, double value, int quantity, double totalItemValue, boolean done) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_ITEM_NAME, name);
		initialValues.put(KEY_ITEM_VALUE, value);
		initialValues.put(KEY_ITEM_QUANTITY, quantity);
		initialValues.put(KEY_ITEM_TOTAL_ITEM_VALUE, totalItemValue);
		initialValues.put(KEY_ITEM_DONE, done);
		
		
		return mDb.insert(table, null, initialValues);
	}
	/**
	 * This gets item from the list
	 * @param listName name of the list
	 * @param id row id of the item
	 * @return cursor with all values of single item (name, price, quantity, total price, done)
	 */
	public Cursor getItem(String listName, long id) {
		String[] columns = new String[] {KEY_ITEM_ROWID, KEY_ITEM_NAME, KEY_ITEM_VALUE, KEY_ITEM_QUANTITY, KEY_ITEM_TOTAL_ITEM_VALUE, KEY_ITEM_DONE};
		Cursor c = mDb.query(true, listName, columns, KEY_ITEM_ROWID + "=" + id, null, null, null, null, null);
		if (c!=null) {
			c.moveToFirst();
		}
		return c;
	}
	
	/**
	 * This gets all items from the list
	 * @param listName name of the list
	 * @return cursor holding values of all items in the list (name, price, quantity, total price, done)
	 */
	public Cursor getAllItems(String listName) {
		listName = listName.replace(' ', '_');
		String[] columns = new String[] {KEY_ITEM_ROWID, KEY_ITEM_NAME, KEY_ITEM_VALUE, KEY_ITEM_QUANTITY, KEY_ITEM_TOTAL_ITEM_VALUE, KEY_ITEM_DONE};
		return mDb.query(listName, columns, null, null, null, null, null);
	}
	
	/**
	 * This updates item in a list
	 * @param listName name of the list
	 * @param id id of the item that is edited
	 * @param name name of the item
	 * @param value price of the item
	 * @param quantity number of items
	 * @param totalItemValue total price of added item
	 * @param done boolean true if bought, false if it is to be bought
	 * @return boolean true succeeded, false if not 
	 */
	public boolean updateItem(String listName, long id, String name, double value, int quantity, double totalItemValue, boolean done) {
		ContentValues args= new ContentValues();
		args.put(KEY_ITEM_NAME, name);
		args.put(KEY_ITEM_VALUE, value);
		args.put(KEY_ITEM_QUANTITY, quantity);
		args.put(KEY_ITEM_TOTAL_ITEM_VALUE, totalItemValue);
		args.put(KEY_ITEM_DONE, done);
		
		return mDb.update(listName, args, KEY_ITEM_ROWID+"="+id, null)>0;
	}
	
	public boolean updateDone(String listName, long id, boolean done) {
		ContentValues args= new ContentValues();
		args.put(KEY_ITEM_DONE, done);
		return mDb.update(listName, args, KEY_ITEM_ROWID+"="+id, null)>0;
	}
	
	/**
	 * This removes item from the list 
	 * @param listName name of the list
	 * @param id id of the item in the list
	 * @return boolean true succeeded, false if not
	 */
	public boolean removeItem(String listName, long id) {
		return mDb.delete(listName, KEY_ITEM_ROWID + "=" + id, null) > 0;
	}
	/**
	 * Removes all items from the list
	 * @param listName name of the list 
	 * @return boolean true succeeded, false if not
	 */
	public boolean clearList(String listName) {
		return mDb.delete(listName, null, null) > 0;
	}
	
	/**
	 * This method checks if list of items is empty
	 * @param listName table name to be checked
	 * @return true if empty, false of not
	 */
	public boolean isItemsEmpty(String listName) {
		Cursor c = getAllItems(listName);
		return !c.moveToLast();
	}
	
	/**
	 * This method checks if list with a given name already exists
	 * in the database.
	 * First it creates array list and then it checks for the index 
	 * of the given name.
	 *  
	 * @param listName Name of the list we are checking if it already exists
	 * @return boolean true if exists, false otherwise
	 */
	public boolean listExists(String listName) {
		Cursor c = getAllLists();
		c.moveToFirst();
		ArrayList<String> listOfNames = new ArrayList<String>();
		for (c.move(-1); c.moveToNext(); c.isAfterLast()) {
			String name = c.getString(LIST_NAME_COLUMN);
			listOfNames.add(name);
		}
		return listOfNames.indexOf(listName) > 0;
	}
	
	
	private final Context mContext;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
}

package android.EZCart;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class ListEdit extends ListActivity {
	/*
	 * Integer constants for activities
	 */
	public static final int ACTIVITY_CREATE=0;
	public static final int ACTIVITY_EDIT=1;
	//used to pass activity id to the item edit activity
	public static final String REQUEST_CODE="activity_id";
	/*
	 * Integer constants for menus
	 */
	private static final int INSERT_ID = Menu.FIRST;
	private static final int DELETE_ID = Menu.FIRST+1;
	private static final int DELETE_ALL_ID = Menu.FIRST+2;
	private static final int EDIT_ID=Menu.FIRST+3;
	/*
	 * Integer constants for data base columns 
	 */
	private static final int TOTAL_ITEM_VALUE_COLUMN = 4;
	private static final int DONE_COLUMN = 5;
	private static final int LIST_NAME = 1;
	private static final int LIST_TABLE_NAME = 2;
	/*
	 * String constants for dialogs
	 */
	private static final String DELETE_DIALOG_STRING = "Are you sure?";
	private static final String POSITIVE = "Yes";
	private static final String NEGATIVE = "No";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new DbHelper(this);
		mDbHelper.open();
		setContentView(R.layout.list_edit);
		getTableName();		
		fillData();
		registerForContextMenu(getListView());
	}
	
	private void getTableName() {
		Bundle extras = getIntent().getExtras();
		Long listRowId =  extras.getLong(DbHelper.KEY_LIST_ROWID);
		Cursor listName = mDbHelper.getList(listRowId);
		String tableName = listName.getString(LIST_TABLE_NAME);
		mTableName = tableName;
		String name = listName.getString(LIST_NAME);
		setTitle("EZ Cart - List: " + name);
	}

	private void fillData() {
		String tableName = mTableName;	
		Cursor list = mDbHelper.getAllItems(tableName);
		startManagingCursor(list);
		ViewBinder binder=new ViewBinder() {
			
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				CheckBox done = (CheckBox) view.findViewById(R.id.CheckBox01);
				if (columnIndex==DONE_COLUMN)	{
					switch (cursor.getInt(columnIndex)) {
					case 0:
						done.setChecked(false);
						return true;
					case 1:
						done.setChecked(true);
						return true;
					}
				}
				
				
				return false;
			}
		};
		/*
		 * This create two arrays, from and to. It maps where column form data base should be displayed
		 * 
		 *  Note: id of the check box needs to be included in the to array
		 */
    	String[] from = new String[] {DbHelper.KEY_ITEM_NAME, DbHelper.KEY_ITEM_VALUE, DbHelper.KEY_ITEM_QUANTITY, DbHelper.KEY_ITEM_TOTAL_ITEM_VALUE, DbHelper.KEY_ITEM_DONE};
    	
    	int[] to = new int[] {R.id.NameTextView, R.id.ItemValueTextView, R.id.QuantityTextView, R.id.TotalItemValueTextView, R.id.CheckBox01 };
    	
    	SimpleCursorAdapter shoppingList = new SimpleCursorAdapter(this, R.layout.item_row, list, from, to);
    	shoppingList.setViewBinder(binder);
    	setListAdapter(shoppingList);
    	calculateTotal();
    	
	
	}
	
	/*
     * Create options menu with  Insert and Remove all menu entries
     * 
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, INSERT_ID, 1, "Insert item");
		menu.add(0, DELETE_ALL_ID, 2, "Remove all items");
		return true;
	}
	/*
     * When menu entry is selected.
     * Insert item: add item
     * Delete all: start confirmation dialog and depending on the 
     * 				user input list is cleared or not
     * 
     * (non-Javadoc)
     * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
     */
	@Override
	public boolean onMenuItemSelected(int id, MenuItem item) {
		mDeleteId=item.getItemId();
		switch(item.getItemId()) {
		case INSERT_ID:
			addItem();
			return true;
		case DELETE_ALL_ID:
			deleteDialog();
			break;
		
		}
		return super.onMenuItemSelected(id, item);
	}
	
	/*
	 * removes all items from the database
	 * and refreshes list
	 */
	private boolean removeAll() {
			mDbHelper.clearList(mTableName);
			fillData();
			return true;
	}
	
	private void addItem() {
		Intent i=new Intent(this, ItemEdit.class);
		i.putExtra(DbHelper.KEY_ITEM_TABLE_NAME, mTableName);
		i.putExtra(REQUEST_CODE, ACTIVITY_CREATE);
		startActivityForResult(i, ACTIVITY_CREATE);
		refreshData();
	}
	
	/*
	 * This just refreshes list
	 * 
	 * (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		refreshData();
	}
	
	/*
	 * This method is used for refreshing list.
	 * It is used instead of fillData(); 
	 */
	

	private void refreshData() {
		Cursor c = mDbHelper.getAllItems(mTableName);
		c.moveToLast();
		c.requery();
		calculateTotal();
	}
	
	/*
	 * Just adds Edit and Delete item to the context menu
	 * 
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, EDIT_ID, 0, "Edit item");
		menu.add(0, DELETE_ID, 0, "Remove item");
	}
	
	/*
	 * What is done depending on the context menu selection
	 * In case of delete it pops out confirmation dialog first and then proceeds to delete 
	 * item from the list depending on the selection
	 * 
	 * (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		mDeleteId = item.getItemId();
		switch (item.getItemId()) {
		case DELETE_ID:
			mItemId = info.id; 
			deleteDialog();	
			return true;
		case EDIT_ID:
			Intent i = new Intent(this, ItemEdit.class);
			i.putExtra(DbHelper.KEY_ITEM_ROWID, info.id);
			i.putExtra(DbHelper.KEY_ITEM_TABLE_NAME, mTableName);
			i.putExtra(REQUEST_CODE, ACTIVITY_EDIT);
			startActivityForResult(i, ACTIVITY_EDIT);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	/*
	 * This method checks if the state of the clicked item and then updates the data
	 * base entry and refreshes data.
	 * It also recalculates sum of all checked items
	 * 
	 * Also point of interest, since there is a check box in the row_layout
	 * it's focusable property needs to be set to false (no flag is not enough!),
	 * because if it's not it will override this method
	 * This applies for all elements that can be focusable
	 * 
	 * 
	 * (non-Javadoc)
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Cursor c=(Cursor) l.getAdapter().getItem(position);
		int done=c.getInt(DONE_COLUMN);
		if (done==0) {
			mDbHelper.updateDone(mTableName, id, true);
			c.requery(); 
			calculateTotal();
		} else if (done==1) {
			mDbHelper.updateDone(mTableName, id, false);
			c.requery();
			calculateTotal();
		}
	}
	/*
	 * This method sums all checked items.
	 * It checks if item is checked (DONE_COLMN VALUE is 1 if it's checked) and ads 
	 * value of total item value column.
	 * NOTE: moveToFirst moves cursor to position 1, so offset is needed move(-1) since 
	 * it is 0 based notation (0, 1, 2...)
	 */
	private void calculateTotal() {
		Cursor allItems = mDbHelper.getAllItems(mTableName);
		double total=0;
		allItems.moveToFirst();
		for (allItems.move(-1); allItems.moveToNext(); allItems.isAfterLast()) {
			if (allItems.getInt(DONE_COLUMN)==1) {
				total+=allItems.getDouble(TOTAL_ITEM_VALUE_COLUMN);
			}
		}
		
		/* This line is needed because sometimes in world of computers 
		 * 116.52 + 34.00 is 150.5199999999999999... 
		 */
		total = (double) (Math.round(total*100))/100;
 		
		TextView tv=(TextView) findViewById(R.id.MainTotalTextView);
		tv.setText(Double.toString(total));
	}
	/*
	 * This method creates confirmation dialog for removing items
	 * from the list. It depends on 2 member variables, mDeleteId
	 * and mItemId so it can decide what action should be performed
	 * and on what. 
	 */
	private void deleteDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage(DELETE_DIALOG_STRING).setCancelable(false).setPositiveButton(POSITIVE, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (mDeleteId) {
				case DELETE_ALL_ID:
					removeAll();
					fillData();
					break;
				case DELETE_ID:	
					Cursor c = (Cursor) getListView().getAdapter().getItem(which);
					mDbHelper.removeItem(mTableName, mItemId);
					c.requery();
					break;
					
				}
				
			}
		}).setNegativeButton(NEGATIVE, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				dialog.cancel();
			}
		});
		AlertDialog alertDialog = dialog.create();
		alertDialog.show();
		
	}
	
	private int mDeleteId;
	private long mItemId;
	private String mTableName;
	private DbHelper mDbHelper;
	
}

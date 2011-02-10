



package android.EZCart;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;


public class EZCart extends ListActivity {
   
	private static final int ACTIVITY_EDIT=1;
	/*
	 * Integer constants for menus
	 */
	private static final int INSERT_ID = Menu.FIRST;
	private static final int DELETE_ALL_ID = Menu.FIRST+1;
	private static final int DELETE_ID = Menu.FIRST+2;
	/*
	 * String constants for dialogs
	 */
	private static final String DELETE_DIALOG_STRING = "Are you sure?";
	private static final String POSITIVE = "Yes";
	private static final String NEGATIVE = "No";
	
	private static final String LIST_CREATE_STRING = "Enter name of the new list";
	private static final String LIST_SAVE = "Save";
	private static final String LIST_CANCEL = "Cancel";
	/*
	 * String constants for menu entries
	 */
	private static final String MENU_CREATE = "Create list";
	private static final String MENU_CLEAR	= "Remove all lists";
	private static final String MENU_DELETE = "Remove";
	private static final int DELETE_GROUP = 1;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ez_cart);
        mDbHelper = new DbHelper(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
    }
    
    /*
     * This method gets lists from the list table in database
     * and creates simple cursor adapter so it can be displayed in 
     * list activity
     */
    private void fillData() {
		Cursor listsCursor = mDbHelper.getAllLists();
		startManagingCursor(listsCursor);
		
		/*
		 * This create two arrays, from and to. It maps where column form data base should be displayed
		 */
		String[] from = new String[] {DbHelper.KEY_LIST_NAME};
		int[] to = new int[] {R.id.ListRowText};
		
		SimpleCursorAdapter shoppingLists = new SimpleCursorAdapter(this, R.layout.list_row, listsCursor, from, to);
		setListAdapter(shoppingLists);
		
	}
    /*
     * This adds menu entries 
     * It has Insert and Remove all menu entries
     * 
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, INSERT_ID, 1, MENU_CREATE);
		menu.add(DELETE_GROUP, DELETE_ALL_ID, 2, MENU_CLEAR);
		
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
					mDbHelper.removeAllLists();
					fillData();
					break;
				case DELETE_ID:	
					Cursor c = (Cursor) getListView().getAdapter().getItem(which);
					mDbHelper.removeList(mItemId);
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
	
	/*
	 * Creates dialog that asks for the name of the new list
	 * and creates new list
	 */
	private void addItem() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		final EditText input =(EditText) new EditText(this);
		input.setHint(LIST_CREATE_STRING);
		input.setSingleLine(true);
		input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		
		dialog.setView(input)
			.setPositiveButton(LIST_SAVE, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String listName = input.getText().toString();
				if (listName.length()==0) {
					dialog.dismiss();
					notifyEmpty();
				}
				if (mDbHelper.listExists(listName) == false) {
					mDbHelper.createList(listName);
					fillData();
					dialog.dismiss();
				} else {
					notifyExists();
				}
			}

		}).
		setNegativeButton(LIST_CANCEL, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		dialog.show();
		
	}
	
	/*
	 * This method is just notifier if user has entered empty string
	 */
	private void notifyEmpty() {
		Toast.makeText(this, "You must enter name of the list.", Toast.LENGTH_SHORT).show();
	}
	
	/*
	 * This method notifies user that list with given name exists
	 */
	private void notifyExists() {
		Toast.makeText(this, "Sorry, but list with that name exists already.", Toast.LENGTH_SHORT).show();
		
	}
	
	/*
	 * Starts activity for editing lists
	 * 
	 * 	(non-Javadoc)
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, ListEdit.class);
		i.putExtra(DbHelper.KEY_LIST_ROWID, id);
		startActivityForResult(i, ACTIVITY_EDIT);
	}
	
	/*
	 * Creates context menu
	 * 
	 * 
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 0, MENU_DELETE);
	}




	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		mDeleteId = item.getItemId();
		switch (item.getItemId()) {
		case DELETE_ID:
			mItemId = info.id;
			deleteDialog();
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		mDbHelper.close();
	}



	private long mItemId;
	private int mDeleteId;
	private DbHelper mDbHelper;
}
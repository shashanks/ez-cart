/*
 * EZ Cart
 * Version: 0.40
 * License: GPLv3
 * Author: idiomatik.80@gmail.com (Nikola Trandafilovic)
 * 
 * 
 * This is simple program intended for keeping track of bought items.
 * It could be also used as simple check list for shopping with possibility to 
 * add prices later. It supports multiple lists in which it is possible to add
 * and remove items 
 * 
 * Also it was created out of need to keep track of expenses when
 * dealing with lots of items.
 * 
 * This program seemed to me as a great starting point to begin 
 * learning android and I hope that it would be useful for someone
 * who is just started developing applications for android. 
 * 
 * So, feel free to fire E-mail my way if you have questions or suggestions.
 *  
 */



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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import idiomatik.EZCart.R;


public class EZCart extends ListActivity {
   
	/*
    * Funny thing really... we don't need this... 
    */
	@SuppressWarnings("unused")
	private static final int ANSWER_TO_THE_ULTIMATE_QUESTION_OF_LIFE_THE_UNIVERSE_AND_EVERYTHING = 42;
	
	private static final int ACTIVITY_EDIT=1;
	/*
	 * Integer constants for menu id's
	 */
	private static final int INSERT_ID = Menu.FIRST;
	private static final int DELETE_ALL_ID = Menu.FIRST+1;
	private static final int DELETE_ID = Menu.FIRST+2;
	private static final int RENAME_ID = Menu.FIRST+3;
	private static final int EXPORT_ID = Menu.FIRST+4;
	/*
	 * String constants for dialogs
	 */
	private static final String CONFIRM_DIALOG_STRING = "Are you sure?";
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
	private static final String CONTEXT_MENU_DELETE = "Remove";
	private static final String CONTEXT_MENU_RENAME = "Rename list";
	private static final int HIDABLE_GROUP = 1;
	
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
		menu.add(HIDABLE_GROUP, DELETE_ALL_ID, 2, MENU_CLEAR);
		
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
		dialog.setMessage(CONFIRM_DIALOG_STRING).setCancelable(false).setPositiveButton(POSITIVE, new OnClickListener() {
			
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
				} else if (mDbHelper.listExists(listName) == false) {
					long control = mDbHelper.createList(listName);
					if (control==DbHelper.NOTIFY_TABLE_CREATION_PROBLEM) {
						notifyProblem();
					}
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
		Toast.makeText(this, "You must enter name of the list.", Toast.LENGTH_LONG).show();
	}
	
	/*
	 * This method notifies user that list with given name exists
	 */
	private void notifyExists() {
		Toast.makeText(this, "Sorry, but list with that name exists already.", Toast.LENGTH_LONG).show();
		
	}
	private void notifyProblem() {
		Toast.makeText(this, "There was problem creating list. Try diferent name for the list", Toast.LENGTH_LONG).show();
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
	 * It has menu entries for renaming and deleting lists
	 * 
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, RENAME_ID, 0, CONTEXT_MENU_RENAME);
		menu.add(0, DELETE_ID, 0, CONTEXT_MENU_DELETE);
		menu.add(0, EXPORT_ID, 0, "Export list");
	}



	/*
	 * Depending on the selection this method calls deleteDialog or renameDialog
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
			break;
		case RENAME_ID:
			mItemId = info.id;
			renameDialog();
			break;
		case EXPORT_ID:
			mItemId = info.id;
			exportDialog();
			break;
			
			
		}
		return super.onContextItemSelected(item);
	}
	
	/*
	 * This method creates dialog for renaming lists
	 */
	private void renameDialog() {
		Cursor list = mDbHelper.getList(mItemId);
		String oldName = list.getString(DbHelper.LIST_NAME_COLUMN);
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		final EditText input =(EditText) new EditText(this);
		input.setText(oldName);
		input.setSelectAllOnFocus(true);
		input.setSingleLine(true);
		input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		dialog.setView(input)
			.setPositiveButton(LIST_SAVE, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String listName = input.getText().toString();
				if (listName.length()==0) {
					dialog.dismiss();
				} else {
					mDbHelper.setListName(mItemId, listName);
					fillData();
					dialog.dismiss();
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
	 * Confirmation dialog for exporting lists
	 * executes exportList if positive button is clicked
	 */
	private void exportDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage(CONFIRM_DIALOG_STRING).setCancelable(false).setPositiveButton(POSITIVE, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				exportList();
			}
		}).setNegativeButton(NEGATIVE, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog alertDialog = dialog.create();
		alertDialog.show();
	}
	
	/*
	 * This exports list to sd card and reports where it was exported 
	 * if successful
	 */
	private void exportList() {
		Exporter ex= new Exporter();
		Cursor list = mDbHelper.getList(mItemId);
		Cursor items = mDbHelper.getAllItems(list.getString(DbHelper.TABLE_NAME_COLUMN));
		String fileLocation = ex.exportToFile(list, items);
		if (fileLocation!=null) {
			Toast.makeText(this, "File exported to " + fileLocation, Toast.LENGTH_LONG).show();
		} else if (fileLocation==null){
			Toast.makeText(this, "SD card is not accessible. You can not export list.", Toast.LENGTH_LONG).show();
		}
	}
	
	/*
	 * This method just closes database on exit
	 * 
	 * (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		mDbHelper.close();
	}
	
	/*
	 * This hides menu entries that are in the hidable group
	 *  when list is empty
	 * 
	 * (non-Javadoc)
	 * @see android.app.Activity#onMenuOpened(int, android.view.Menu)
	 */
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		ListAdapter listAdapter = getListAdapter();
		int count = listAdapter.getCount();
		if (count==0) {
			menu.setGroupVisible(HIDABLE_GROUP, false);
		} else if (count>0){
			menu.setGroupVisible(HIDABLE_GROUP, true);
		}
		return super.onMenuOpened(featureId, menu);
	}
	
	/*
	 * Instance variables
	 */
	private long mItemId;
	private int mDeleteId;
	private DbHelper mDbHelper;
}
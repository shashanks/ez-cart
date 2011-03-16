package android.EZCart;

import idiomatik.EZCart.R;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class HistoryEdit extends ListActivity{
	
	private static final int REMOVE_SELECTED_ID = Menu.FIRST;
	private static final int CLEAR_ID = Menu.FIRST+1;
	private static final int EDIT_ID = Menu.FIRST+2;
	
	private static final String REMOVE_SELECTED = "Remove selected items";
	private static final String CLEAR = "Clear history";
	private static final String EDIT = "Edit item";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHistory = new History(this);
		mHistory.open();
		setContentView(R.layout.history_edit);
		fillData();
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		registerForContextMenu(getListView());
	}
	
	/*
	 * Populates list
	 */
	private void fillData() {
		Cursor items = mHistory.getHistoryCursor();
		startManagingCursor(items);
		String[] from = new String[] {History.KEY_HISTORY_ITEM_NAME};
		int[] to = new int[] {android.R.id.text1};
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,android.R.layout.simple_list_item_checked, items, from, to);
		setListAdapter(adapter);
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		CheckedTextView text = (CheckedTextView) v;
		if (text.isChecked()) {
			text.setChecked(false);
		} else {
			text.setChecked(true);
		}
		
	}
	
	/*
	 * Creates menu entries for removing selected items and clearing history
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, REMOVE_SELECTED_ID, 0, REMOVE_SELECTED);
		menu.add(0, CLEAR_ID, 0, CLEAR);
		return super.onCreateOptionsMenu(menu);
	}
	/*
	 * Calls methods depending on selected menu entry
	 * 
	 * (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case REMOVE_SELECTED_ID:
			removeSelectedDialog();
			return true;
		case CLEAR_ID:
			clearHistoryDialog();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	/*
	 * Removes all items from history
	 */
	private void clearHistory() {
		mHistory.clearHistory();
		fillData();
	}
	
	/*
	 * Removes selected items from history
	 */
	private void removeSelected() {
		ListView l = this.getListView();
		long[] ids = l.getCheckItemIds();
		for (int i=0; i<ids.length; i++) {
			mHistory.removeFromHistory(ids[i]);
		}
		fillData();
		
	}
	
	
	/*
	 * What to do when menu entry in context menu is selected
	 * (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		mItemId = info.id;
		switch (item.getItemId()) {
		case EDIT_ID:
			renameDialog();
			return true;
		}
		return false;
	}
	
	/*
	 * Creates context menu with entry for editing items in history
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, EDIT_ID, 0, EDIT);
	}
	/*
	 * Creates rename dialog for items in history
	 */
	private void renameDialog() {
		Cursor item = mHistory.getItem(mItemId);
		String oldName = item.getString(1);
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		final EditText input =(EditText) new EditText(this);
		input.setText(oldName);
		input.setSelectAllOnFocus(true);
		input.setSingleLine(true);
		input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		dialog.setView(input)
			.setPositiveButton("Save", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String newName = input.getText().toString();
				if (newName.length()==0 && mHistory.itemExists(newName)) {
					dialog.dismiss();
				} else {
					mHistory.updateItem(mItemId, newName);
					fillData();
					dialog.dismiss();
				}
				
			}

		}).
		setNegativeButton("Cancel", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		dialog.show();
		
	}
	/*
	 * Creates confirmation dialog for removing selected items 
	 * in history
	 */
	private void removeSelectedDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("Remove selected items");
		dialog.setMessage("Are you sure?").setCancelable(false).setPositiveButton("Yes", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				removeSelected();
				dialog.dismiss();
				
			}
		}).setNegativeButton("No", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				dialog.cancel();
			}
		});
		AlertDialog alertDialog = dialog.create();
		alertDialog.show();
		
	}
	
	/*
	 * Creates confirmation dialog for clearing history
	 */
	private void clearHistoryDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("Clear history");
		dialog.setMessage("Are you sure?").setCancelable(false).setPositiveButton("Yes", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				clearHistory();
				dialog.dismiss();
				
			}
		}).setNegativeButton("No", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				dialog.cancel();
			}
		});
		AlertDialog alertDialog = dialog.create();
		alertDialog.show();
		
	}

	
	private long mItemId;
	private History mHistory;
}

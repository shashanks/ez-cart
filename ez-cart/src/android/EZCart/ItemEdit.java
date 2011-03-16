/*
 * ItemEdit.java
 * 
 * This activity is used for editing and creating items in
 * the list 
 */

package android.EZCart;

import idiomatik.EZCart.R;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class ItemEdit extends Activity{
	
	private static final int ACTIVITY_EDIT=1;
	
	private static final String EDIT_HISTORY = "Edit history";
	private static final int EDIT_HISTORY_ID = Menu.FIRST;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new DbHelper(this);
		mDbHelper.open();
		mHistory = new History(this);
		mHistory.open();
		setContentView(R.layout.item_edit);
		
		Bundle listName = getIntent().getExtras();
		
		mListName = listName.getString(DbHelper.KEY_ITEM_TABLE_NAME);
		mNameEditText = (AutoCompleteTextView) findViewById(R.id.NameEditText);
		mQuantityEditText = (EditText) findViewById(R.id.QuantityEditText);
		mQuantityEditText.setSelectAllOnFocus(true);
		mPriceEditText = (EditText) findViewById(R.id.PriceEditText);
		mPriceEditText.setSelectAllOnFocus(true);
		mDoneCheckBox = (CheckBox) findViewById(R.id.CheckBox01);
		
		Button confirmButton = (Button) findViewById(R.id.ConfirmButton);
		
		mRowId = savedInstanceState !=null ? savedInstanceState.getLong(DbHelper.KEY_ITEM_ROWID)
											: null;
		
		if (mRowId==null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras !=null ? extras.getLong(DbHelper.KEY_ITEM_ROWID) : null;
			if (mRowId==0) mRowId=null;
		}
		
		populateFields();
		confirmButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				setResult(RESULT_OK);
				saveState();
				finish();
			}
		});
		ArrayAdapter<String> adapter = new ArrayAdapter<String> (this, R.layout.history_list, mHistory.getHistory());
		mNameEditText.setAdapter(adapter);
	}
	/*
	 * This populates fields. if it comes back from suspended activity
	 * it reads from the temporary entry in other cases it reads regular 
	 * entry from the table
	 */
	private void populateFields() {
		Long id;
		if (mPaused) {
			id = mTempRowId;
		} else {
			id = mRowId;
		}
		if (id!=null) {
			Cursor item = mDbHelper.getItem(mListName, id);
			startManagingCursor(item);
			mNameEditText.setText(item.getString(item.getColumnIndexOrThrow(DbHelper.KEY_ITEM_NAME)));
			mQuantityEditText.setText(item.getString(item.getColumnIndexOrThrow(DbHelper.KEY_ITEM_QUANTITY)));
			mPriceEditText.setText(item.getString(item.getColumnIndexOrThrow(DbHelper.KEY_ITEM_VALUE)));
			boolean done = item.getInt(item.getColumnIndex(DbHelper.KEY_ITEM_DONE))==1;
			mDoneCheckBox.setChecked(done);
		}
	}
	
	/*
	 * This kills activity
	 * 
	 * (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		
	}
	
	/*
	 * This just sets mPaused field to true to be used in
	 * populateFields and onResume methods
	 * 
	 * (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		mPaused=true;
		
	}
	
	/*
	 * Saves current state in a new row of the current table
	 */
	
	private void saveTempState() {
		String name = mNameEditText.getText().toString();
		String quantityText = mQuantityEditText.getText().toString();
		double quantity = 1;
		if (quantityText.length()>0) {
			quantity = Double.valueOf(quantityText);
		}			
		String priceText = mPriceEditText.getText().toString();
		double price = 0;
		if (priceText.length()>0) {
			price = Double.valueOf(priceText);
		}
		double totalPrice =(double) (Math.round(quantity*price*100))/100;
		boolean done = mDoneCheckBox.isChecked();
		mTempRowId = mDbHelper.addItem(mListName, name, price, quantity, totalPrice, done);
		
	}
	
	/*
	 * On resume populates fields and calls removeTempEntry if it comes back
	 * from pause 
	 * 
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		ArrayAdapter<String> adapter = new ArrayAdapter<String> (this, R.layout.history_list, mHistory.getHistory());
		mNameEditText.setAdapter(adapter);
		populateFields();
		if (mPaused) removeTempEntry();
		
	}
	/*
	 * This removes temporary entry and resets mPaused field
	 */
	private void removeTempEntry() {
		mDbHelper.removeItem(mListName, mTempRowId);
		mPaused=false;
	}
	/*
	 * This saves temporary state to a new row in the
	 * table and sets mPaused to true
	 * (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mPaused=true;
		saveTempState();
		outState.putLong(DbHelper.KEY_ITEM_ROWID, mTempRowId);
	}
	
	
	/*
	 * This method saves current state to a new row of the table if mRowId is null
	 * or updates item if mRowId is not null
	 */
	private void saveState() {
		String name = mNameEditText.getText().toString();
		String quantityText = mQuantityEditText.getText().toString();
		double quantity = 1;
		if (quantityText.length()>0) {
			quantity = Double.valueOf(quantityText);
		}			
		String priceText = mPriceEditText.getText().toString();
		double price = 0;
		if (priceText.length()>0) {
			price = Double.valueOf(priceText);
		}
		double totalPrice =(double) (Math.round(quantity*price*100))/100;
		boolean done = mDoneCheckBox.isChecked();
		if (mRowId == null) {
			if (mHistory.itemExists(name)==false && name.length()!=0) mHistory.addItemToHistory(name);
			long id = mDbHelper.addItem(mListName, name, price, quantity, totalPrice, done);
			if (id > 0) {
				mRowId = id;
			}
		} else {
		  mDbHelper.updateItem(mListName, mRowId, name, price, quantity, totalPrice, done);
		  if (mHistory.itemExists(name)==false && name.length()!=0) mHistory.addItemToHistory(name);
		}
	
	}
	
	 /*
     * This adds menu for editing history
     * 
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, EDIT_HISTORY_ID, 0, EDIT_HISTORY);
		return true;
	}
    public boolean onMenuItemSelected(int id, MenuItem item) {
    	switch (item.getItemId()) {
    	case EDIT_HISTORY_ID:
    		editHistory();
    		return true;
    	}
    	return false;
    }
    
    private void editHistory() {
    	Intent i = new Intent(this, HistoryEdit.class);
    	startActivityForResult(i, ACTIVITY_EDIT);
    }
    
	/*
	 * Instance variables
	 */
	
	// used to track if activity was paused and should entries be deleted after
	// restore 
	private boolean mPaused = false;
	private Long mRowId;
	private Long mTempRowId;
	
	private String mListName;
	private AutoCompleteTextView mNameEditText;
	private EditText mQuantityEditText;
	private EditText mPriceEditText;
	private CheckBox mDoneCheckBox;
	private DbHelper mDbHelper;
	private History mHistory;
	
}

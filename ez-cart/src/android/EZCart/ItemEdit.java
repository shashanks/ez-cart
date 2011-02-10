package android.EZCart;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class ItemEdit extends Activity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new DbHelper(this);
		mDbHelper.open();
		setContentView(R.layout.item_edit);
		
		Bundle listName = getIntent().getExtras();
		mListName = listName.getString(DbHelper.KEY_ITEM_TABLE_NAME);
		
		mNameEditText = (EditText) findViewById(R.id.NameEditText);
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
	}
	
	private void populateFields() {
		if (mRowId!=null) {
			Cursor item = mDbHelper.getItem(mListName, mRowId);
			startManagingCursor(item);
			mNameEditText.setText(item.getString(item.getColumnIndexOrThrow(DbHelper.KEY_ITEM_NAME)));
			mQuantityEditText.setText(item.getString(item.getColumnIndexOrThrow(DbHelper.KEY_ITEM_QUANTITY)));
			mPriceEditText.setText(item.getString(item.getColumnIndexOrThrow(DbHelper.KEY_ITEM_VALUE)));
			boolean done = item.getInt(item.getColumnIndex(DbHelper.KEY_ITEM_DONE))==1;
			mDoneCheckBox.setChecked(done);
		}
	}
	
	
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		canceled = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveState();
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		populateFields();
		if (paused) {
			removeTempEntry();
		}
	}
	
	private void removeTempEntry() {
		mDbHelper.removeItem(mListName, mRowId);
		mRowId=null;
		paused=false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState();
		paused=true;
		outState.putLong(DbHelper.KEY_ITEM_ROWID, mRowId);
	}
	
	private void saveState() {
		String name = mNameEditText.getText().toString();
		String quantityText = mQuantityEditText.getText().toString();
		int quantity = 1;
		if (quantityText.length()>0) {
			quantity = Integer.valueOf(quantityText);
		}			
		String priceText = mPriceEditText.getText().toString();
		double price = 0;
		if (priceText.length()>0) {
			price = Double.valueOf(priceText);
		}
		double totalPrice = quantity*price;
		boolean done = mDoneCheckBox.isChecked();
		if (!canceled) {
			if (mRowId == null) {
				long id = mDbHelper.addItem(mListName, name, price, quantity, totalPrice, done);
				if (id > 0) {
					mRowId = id;
				}
			} else {
				mDbHelper.updateItem(mListName, mRowId, name, price, quantity, totalPrice, done);
			}
		}
	}

	private boolean canceled = false;
	private boolean paused = false;
	private Long mRowId;
	
	private String mListName;
	private EditText mNameEditText;
	private EditText mQuantityEditText;
	private EditText mPriceEditText;
	private CheckBox mDoneCheckBox;
	private DbHelper mDbHelper;
}

/*
 * Exporter.java
 * 
 * This class exports lists and items in list
 */

package android.EZCart;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import android.database.Cursor;
import android.os.Environment;



public class Exporter {

	// Integer constants for DB columns
	private final int NAME_COLUMN = 1;
	private final int PRICE_COLUMN = 2;
	private final int QUANTITY_COLUMN = 3;
	private final int TOTAL_COLUMN = 4;
	private final int DONE_COLUMN = 5;
	
	/**
	 * This method exports lists to external memory in 
	 * csv file.
	 * Path is /mnt/sdcard/EZCart/Exported/. File name is 
	 * List name + currentDate.csv 
	 * Values in file are separated by semicolon
	 * 
	 * @param list cursor with list
	 * @param items cursor with all items in the list
	 * @return String of file name and location if successful, null if not
	 */
	
	
	public String exportToFile(Cursor list, Cursor items) {		
		getStorageState();
		if (mExternalStorageWritable) {
			String listName = list.getString(DbHelper.LIST_NAME_COLUMN);
			items.moveToFirst();
			File path = new File("/mnt/sdcard/EZCart", "Exported");
			path.mkdirs();
			Date date = new Date();
			String currentDate =DateFormat.getDateInstance().format(date) + "_" + date.getHours() + "_" + date.getMinutes();
			String filename = listName + "_" + currentDate + ".csv";
			filename = filename.replace(", ", "_");
			filename = filename.replace(' ', '_');
			File file = new File(path, filename);
			try {
				file.createNewFile();
				FileWriter wr = new FileWriter(file);
				String lineDone = "";
				String lineNotDone = "";
				for (items.move(-1); items.moveToNext(); items.isAfterLast()) {
					if (items.getInt(DONE_COLUMN) == 1) {
						lineDone += items.getString(NAME_COLUMN) + ";" + items.getString(PRICE_COLUMN) + ";" + items.getString(QUANTITY_COLUMN) +
						";" + items.getString(TOTAL_COLUMN) + "\n";
					} else {
						lineNotDone += items.getString(NAME_COLUMN) + ";" + items.getString(PRICE_COLUMN) + ";" + items.getString(QUANTITY_COLUMN) +
						";" + items.getString(TOTAL_COLUMN) + "\n";
					}
					
				}
				currentDate.trim();
				wr.write(currentDate + "\n" +"Name" + ";" + "Price" + ";" + "Items" + ";" + "Total" + "\n");
				wr.write(lineDone);
				wr.write("\n" + "Theese items were not bought" + "\n\n");
				wr.write(lineNotDone);
				wr.flush();
				wr.close();
				return "sdcard/EZCart/Exported/" + filename;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

/*
 * This method checks if external storage is
 * writable
 */
private void getStorageState() {
	String state = Environment.getExternalStorageState();
	if (Environment.MEDIA_MOUNTED.equals(state)) {
		mExternalStorageWritable = true;
	} else {
		mExternalStorageWritable = false;
	}
}

	/*
	 * Instance variables
	 */
	private boolean mExternalStorageWritable = false;
}

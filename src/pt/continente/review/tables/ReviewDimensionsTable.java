package pt.continente.review.tables;

import java.util.ArrayList;
import java.util.List;

import pt.continente.review.common.Common;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class ReviewDimensionsTable {

	// Debugging tag
	private static final String TAG = "CntRev - ReviewDimensionsTable";

	/**
	 * Defines the internal exceptions that can be thrown by the class
	 */
	public static final class exceptions {
		public static final String DB_HELPER_ERROR = "Error opening DB helper";
		public static final String WRITABLE_DB_ERROR = "Error capturing a writable DB";
	}

	public static final String TABLE_NAME = "ReviewDimensions";
	public static final String COLUMN_REVIEW_ID = "review_id";
	public static final String COLUMN_DIMENSION_ID = "dimension_id";
	public static final int COLUMN_COUNT = 2;
	
	// Database fields
	private SQLiteDatabase database;
	private SQLiteHelper dbHelper;
	private String[] allColumns = {
			COLUMN_REVIEW_ID,
			COLUMN_DIMENSION_ID
			};
	
	
	
	
	public ReviewDimensionsTable(SQLiteHelper helper) throws Exception {
		try {
			dbHelper = helper;
		} catch (SQLException e) {
			Log.i(TAG, "DimensionsTable: error opening the DB helper - " + e.getMessage());
			throw new Exception(exceptions.DB_HELPER_ERROR);
		}
	}
	
	
	public void open() throws Exception {
		try {
			database = dbHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.i(TAG, "open: error getting writable database - " + e.getMessage());
			throw new Exception(exceptions.WRITABLE_DB_ERROR);
		}
	}	
	
	public void close() {
		database.close();
	}
	
	
	
	
	public List<Long> getAllItemsOfReview(long itemId) {

		List<Long> item = new ArrayList<Long>();
	
	    Cursor cursor = database.query(TABLE_NAME, allColumns, COLUMN_REVIEW_ID + "=" + itemId, null, null, null, null);
	    
    	cursor.moveToNext();
    	while (!cursor.isAfterLast()) {
    		item.add(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DIMENSION_ID)));
    	}
	    cursor.close();
	    return item;
	}

	
	/**
	 * @return
	 * the <b>id</b> generated by the table (should be added to the supplied Object)<br>
	 * <b>-1</b> if there is already a line for this pair of items<br>
	 * <b>-2</b> if there was a general error adding to the table
	 */
	public long addItem(long revId, long dimId) {
		
		Common.log(5, TAG, "addItem: entrou");

		if (findItem(revId, dimId) != -1) {
			Common.log(1, TAG, "addItem: an item for same content already exists");
			return -1;
		}

		
		ContentValues values = new ContentValues();
	    
	    values.put(COLUMN_REVIEW_ID, revId);
	    values.put(COLUMN_DIMENSION_ID, dimId);
		
		Common.log(5, TAG, "addItem: vai tentar carregar registo na db");
	    long newItemId = database.insert(TABLE_NAME, null, values);
	    if(newItemId == -1) {
	    	Common.log(1, TAG, "addItem: couldn't insert new Device into table");
			return -2;
	    }
	    
		Common.log(5, TAG, "addItem: vai sair");
	    return newItemId;
	}
	

	public int deleteAllItemsOfReview (long itemId) {
		int rowsAffected = database.delete(TABLE_NAME, COLUMN_REVIEW_ID + "=" + itemId, null);
		Common.log(5, TAG, "deleteDevice: deleted " + rowsAffected + " rows with deviceId " + itemId);
		return rowsAffected;
	}

	
	/**
	 * @return
	 * <i><b>int</b></i> with number of rows affected
	 * <b>-1</b> if failed to read number of rows prior to deleting
	 * <b>-2</b> if no rows where deleted
	 * <b>-3</b> if not all rows where deleted
	 */
	public int deleteAllItems () {
		int rowsAvailable = getNumberOfRows();
		Common.log(5, TAG, "deleteAllItems: rows available = " + rowsAvailable);
		
		if (rowsAvailable < 0) {
			Log.i(TAG, "deleteAllItems: failed to read number of rows");
			return -1;
		}
		int rowsAffected = database.delete(TABLE_NAME, "1", null);

		Common.log(5, TAG, "deleteAllItems: apagou " + rowsAffected + " linhas");
		if (rowsAffected == rowsAvailable) {
			Common.log(5, TAG, "deleteAllItems: deleted " + rowsAffected + " rows (all)");
			return rowsAffected;
		} else if (rowsAffected == 0) {
			Log.i(TAG, "deleteAllItems: could not delete any rows");
			return -2;
		} else {
			Log.i(TAG, "deleteAllItems: not all rows where deleted (only " + rowsAffected + " out of " + rowsAvailable);
			return -3;
		}
	}

	/**
	 * @return
	 * <i><b>int</b></i> with number of rows in table
	 * <b>-1</b> if failed to count rows
	 */
	private int getNumberOfRows() {
		Cursor cursor;
		int numLines;
		
		Common.log(5, TAG, "getNumberOfRows: entrou");
		
		try {
			cursor = database.query(TABLE_NAME, new String[] { COLUMN_DIMENSION_ID }, null, null, null, null, null);
		} catch (Exception e) {
			Log.i(TAG, "getNumberOfRows: error counting rows (1) - " + e.getMessage());
			return -1;
		}
		
		try {
	    	numLines = 0;
			cursor.moveToNext();
	    	while (!cursor.isAfterLast()) {
	    		numLines++;
	    		cursor.moveToNext();
	    	}
		} catch (Exception e) {
			Log.i(TAG, "getNumberOfRows: error counting rows (2) - " + e.getMessage());
			return -1;
		}

		try {
		    cursor.close();
		} catch (Exception e) {
			Log.i(TAG, "getNumberOfRows: error counting rows (3) - " + e.getMessage());
			return -1;
		}
		
		return numLines;
	}
	
	/**
	 * @return
	 * <b>1</b> if one line matches the input<br>
	 * <b>-1</b> if no lines match the input<br>
	 * <b>-2</b> if more than one line match the input<br>
	 */
	public long findItem(long revId, long dimId) {
		Common.log(5, TAG, "findItem: started");

	    Cursor cursor = database.query(TABLE_NAME, allColumns, COLUMN_REVIEW_ID + "=" + revId + " AND " + COLUMN_DIMENSION_ID + "=" + dimId, null, null, null, null);
	    
		int cursorRows = cursor.getCount();
		
		Common.log(5, TAG, "findItem: found " + cursorRows + " lines");
	    if (cursorRows <= 0) {
	    	Common.log(5, TAG, "findItem: line with revId '" + revId + "' and dimId '" + dimId + "' does not exist");
	    	return -1;
	    } else if (cursorRows > 1) {
	    	Log.i(TAG, "findItem: more than one line with revId '" + revId + "' and dimId '" + dimId + "' exists");
	    	return -2;
	    }
	    cursor.close();
		return 1;
	}
}

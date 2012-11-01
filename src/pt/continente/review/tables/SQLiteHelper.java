package pt.continente.review.tables;

import pt.continente.review.common.Common;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {
	
	// Debugging tag
	private static final String TAG = "CntRev - SQLiteHelper";
	
	// Database variables
	public static final String DATABASE_NAME = "cnt_reviews.db";
	public static final int DATABASE_VERSION = 4; // >= 1

	// Table creation sql statements
	private static final String CREATE_ARTICLES_TABLE = "create table " + ArticlesTable.TABLE_NAME + "("
			+ ArticlesTable.COLUMN_ARTICLE_ID + " long primary key not null, "
			+ ArticlesTable.COLUMN_ARTICLE_NAME + " text not null, "
			+ ArticlesTable.COLUMN_ARTICLE_DESCRIPTION + " text, "
			+ ArticlesTable.COLUMN_ARTICLE_EAN + " text, "
			+ ArticlesTable.COLUMN_ARTICLE_PRICE + " double, "
			+ ArticlesTable.COLUMN_ARTICLE_IMAGE_URL + " text, "
			+ ArticlesTable.COLUMN_ARTICLE_IMAGE + " blob, "
			+ ArticlesTable.COLUMN_ARTICLE_STRUCTURE_L1 + " int, "
			+ ArticlesTable.COLUMN_ARTICLE_STRUCTURE_L2 + " int, "
			+ ArticlesTable.COLUMN_ARTICLE_STRUCTURE_L3 + " int, "
			+ ArticlesTable.COLUMN_ARTICLE_STRUCTURE_L4 + " int"
			+ ");";
	
	private static final String CREATE_REVIEWS_TABLE = "create table " + ReviewsTable.TABLE_NAME + "("
			+ ReviewsTable.COLUMN_REVIEW_ID + " integer primary key autoincrement, "
			+ ReviewsTable.COLUMN_REVIEW_STATE + " text not null, "
			+ ReviewsTable.COLUMN_REVIEW_ARTICLE_ID + " text not null, "
			+ ReviewsTable.COLUMN_REVIEW_COMMENT + " text"
			+ ");";

	private static final String CREATE_REVIEW_IMAGES_TABLE = "create table " + ReviewImagesTable.TABLE_NAME + "("
			+ ReviewImagesTable.COLUMN_IMAGE_ID + " integer primary key autoincrement, "
			+ ReviewImagesTable.COLUMN_REVIEW_ID + " long, "
			+ ReviewImagesTable.COLUMN_REVIEW_IMAGE + " blob"
			+ ");";
	
	private static final String CREATE_DIMENSIONS_TABLE = "create table " + DimensionsTable.TABLE_NAME + "("
			+ DimensionsTable.COLUMN_DIMENSION_ID + " integer primary key, "
			+ DimensionsTable.COLUMN_DIMENSION_NAME + " text not null, "
			+ DimensionsTable.COLUMN_DIMENSION_LABEL + " text not null, "
			+ DimensionsTable.COLUMN_DIMENSION_MIN + " text not null, "
			+ DimensionsTable.COLUMN_DIMENSION_MED + " text, "
			+ DimensionsTable.COLUMN_DIMENSION_MAX + " text not null"
			+ ");";
	
	private static final String CREATE_REVIEW_DIMENSIONS_TABLE = "create table " + ReviewDimensionsTable.TABLE_NAME + "("
			+ ReviewDimensionsTable.COLUMN_REVIEW_ID + " integer not null, "
			+ ReviewDimensionsTable.COLUMN_DIMENSION_ID + " integer not null, "
			+ "PRIMARY KEY ("
			+ ReviewDimensionsTable.COLUMN_REVIEW_ID + ", "
			+ ReviewDimensionsTable.COLUMN_DIMENSION_ID + ""
			+ "));";
	
	
	
	public SQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		Common.log(5, TAG, "SQLiteHelper: executed the constructor");
	}

	@Override
	public void onCreate(SQLiteDatabase database) throws SQLException {
		database.execSQL(CREATE_ARTICLES_TABLE);
		database.execSQL(CREATE_REVIEWS_TABLE);
		database.execSQL(CREATE_REVIEW_IMAGES_TABLE);
		database.execSQL(CREATE_DIMENSIONS_TABLE);
		database.execSQL(CREATE_REVIEW_DIMENSIONS_TABLE);
		Common.log(5, TAG, "onCreate: created new tables");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Common.log(1, TAG, "onUpgrade: Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + ArticlesTable.TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + ReviewsTable.TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + ReviewImagesTable.TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + DimensionsTable.TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + ReviewDimensionsTable.TABLE_NAME);
		onCreate(db);
		Common.log(5, TAG, "onUpgrade: finished upgrade");
	}
}

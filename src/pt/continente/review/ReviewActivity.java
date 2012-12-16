package pt.continente.review;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import pt.continente.review.common.Article;
import pt.continente.review.common.Common;
import pt.continente.review.common.Dimension;
import pt.continente.review.common.HTTPRequest;
import pt.continente.review.common.Preferences;
import pt.continente.review.common.Review;
import pt.continente.review.common.ReviewDimension;
import pt.continente.review.tables.ArticlesTable;
import pt.continente.review.tables.DimensionsTable;
import pt.continente.review.tables.ReviewDimensionsTable;
import pt.continente.review.tables.ReviewsTable;
import pt.continente.review.tables.SQLiteHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bugsense.trace.BugSenseHandler;

public class ReviewActivity extends Activity {
	private static final String TAG = "CntRev - ReviewActivity";

	private static TextView articleNameTextView;
	private static Context context;
	private static Resources resources;

	private static long revId;
	public static Article article = null;
	public static Review review = null;
	private static List<Dimension> dimensions = null;
	public static List<ReviewDimension> reviewDimensions = null;

	private static ProgressDialog dialog;
	private static boolean isReviewReadOnly;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Common.log(5, TAG, "onCreate: started");
		BugSenseHandler.initAndStartSession(this, Common.bugSenseAppKey);
		setContentView(R.layout.activity_review);
		
		context = this;
		resources = getResources();

		articleNameTextView = (TextView) findViewById(R.id.articleName);

		/*
		 * Attempts to get either an Id of an existing review or the object of
		 * the article already retrieved for a new review
		 */
		revId = getIntent().getLongExtra("revId", -1);
		article = (Article) getIntent().getSerializableExtra("Article");
		if (article != null) {
			Bitmap artImg = (Bitmap) getIntent().getParcelableExtra("ArticleImage");
			if (artImg == null)
				Common.log(5, TAG, "onCreate: imagem obtida é nula");
			article.setImage(artImg);
		}
		
		/*
		 * Checks wether or not this review is to be presented as read only
		 */
		isReviewReadOnly = getIntent().getBooleanExtra("isReviewReadOnly", false);
		
		Common.log(5, TAG, "onCreate: finished");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.general_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Common.log(5, TAG, "onResume: started");

		dimensions = new ArrayList<Dimension>();
		reviewDimensions = new ArrayList<ReviewDimension>();

		/*
		 * Must have at least one valid source
		 */
		if (revId == -1 && article == null) {
			finish();
			Common.longToast(this, resources.getString(R.string.toast_generalUndefinedError) + "\n(" + resources.getString(R.string.toast_reviewNoValidInput) + ")");
			return;
		}
		
		/*
		 * If review is to be presented as read-only,
		 * HIDE SUBMIT BUTTON
		 */
		if(isReviewReadOnly) {
			Button butSubmit = (Button) findViewById(R.id.buttonSubmitReview);
			butSubmit.setVisibility(Button.GONE);
		}
		
		
		/*
		 * If the input to the activity was not a previous review and an article
		 * has been received,
		 * CHECK IF A PENDING REVIEW EXISTS FOR THIS ARTICLE
		 */
		if (revId == -1 && article != null) {
			Common.log(5, TAG, "onResume: will check if received article already has a pending review");

			SQLiteHelper dbHelper = new SQLiteHelper(this);
			ReviewsTable revTab;
			try {
				revTab = new ReviewsTable(dbHelper);
				revTab.open();
			} catch (Exception e) {
				shutdownWithError("onResume: ERROR could not open the table - " + e.getMessage(), resources.getString(R.string.toast_generalUndefinedError) + "\n(" + resources.getString(R.string.toast_reviewErrorAccessingTables) + ")");
				return;
			}
			long revIdTmp = revTab.findItemFromActive(article.getId());
			if (revIdTmp > 0) {
				revId = revIdTmp;
			}
			revTab.close();
		}

		/*
		 * If there is a pending review to show (from input or pending for
		 * the selected article
		 * SHOW EXISTING REVIEW
		 */
		if (revId != -1) {
			Common.log(5, TAG, "onResume: will recover existing review with Id '" + revId + "'");
			int result = getDataFromExistingReview();
			if (result < 0) {
				shutdownWithError("onResume: ERROR getting information from DB; cannot continue (revId = '" + revId + "', result = '" + result + "'", resources.getString(R.string.toast_generalUndefinedError) + "\n(" + resources.getString(R.string.toast_reviewErrorAccessingTables) + ")");
				return;
			} else {
				showReview();
			}
			
		/*
		 * Else, if there is no pending review for this article,
		 * CREATE A NEW REVIEW FOR THE ARTICLE (request data from server
		 * and build review when server answers) 
		 */
		} else if (article != null) {
			Common.log(5, TAG, "onResume: review for received article not present, will create new");

			String url = Common.httpVariables.DIMENSIONS_PREFIX + article.getId();
			Common.log(5, TAG, "onResume: will atempt to launch service to get content from url '" + url + "'");
			(new HTTPRequest(this, new httpRequestHandler(), url, HTTPRequest.requestTypes.GET_DIMENSIONS)).start();
			dialog = ProgressDialog.show(this, resources.getString(R.string.dialog_generalFetchingInformation), resources.getString(R.string.dialog_generalFetchingInformation) + "...");
		}

		Common.log(5, TAG, "onResume: finished");
	}

	@Override
	protected void onPause() {
		super.onPause();
		reviewSave();
	}

	private static void showReview() {
		Common.log(5, TAG, "showReview: started");

		if (review == null || article == null || dimensions == null || dimensions.isEmpty() || reviewDimensions == null || reviewDimensions.isEmpty()) {
			shutdownWithError("showReview: ERROR no valid source to update view; cannot continue", resources.getString(R.string.toast_generalUndefinedError) + "\n(" + resources.getString(R.string.toast_reviewMissingInformation) + ")");
			return;
		}

		TextView t = (TextView) ((Activity) context).findViewById(R.id.articleName);
		t.setText(article.getName());

		Common.log(5, TAG, "showReview: will set bitmap");
		Bitmap articleBitmap = article.getImage();
		if (articleBitmap == null) {
			Common.log(1, TAG, "showReview: ERROR article object did not contain image; will attempt to get from URL");
		} else {
			articleNameTextView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(resources, articleBitmap), null, null, null);
		}

		Common.log(5, TAG, "showReview: will draw dimensions");
		if (dimensions != null)
			drawDimensions();
	}

	private static void shutdownWithError(String debugText, String userText) {
		Common.log(1, TAG, debugText);
		((Activity) context).finish();
		Common.longToast(context, userText);
	}

	/**
	 * @param revId
	 *            ID of review from which data should be retrieved
	 * @return <b>0</b> if all objects (<b><i>review</i></b>,
	 *         <b><i>article</i></b> and <b><i>dimensions</i></b>) were properly
	 *         populated<br>
	 *         <b>-1</b> if an error occurred when opening a table (see Log to
	 *         check which one)<br>
	 *         <b>-2</b> if an error occurred when getting a specific object
	 *         from a table (see Log to check which one)<br>
	 *         <b>-3</b> if no dimensions were returned for current review<br>
	 *         <b>-4</b> if not all review dimensions were found in the
	 *         Dimensions table
	 */
	private int getDataFromExistingReview() {
		Common.log(5, TAG, "getDataFromExistingReview: started");
		SQLiteHelper dbHelper = new SQLiteHelper(this);

		/*
		 * Get Review from table
		 */
		ReviewsTable revTab;
		try {
			revTab = new ReviewsTable(dbHelper);
			revTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "getDataFromExistingReview: could not open the Reviews table - " + e.getMessage());
			return -1;
		}
		review = revTab.getItem(revId);
		revTab.close();
		if (review == null) {
			Common.log(1, TAG, "getDataFromExistingReview: could not get Review from DB");
			return -2;
		}
		Common.log(5, TAG, "getDataFromExistingReview: built Review with Id '" + review.getId() + "'");

		/*
		 * Get Article from table
		 */
		ArticlesTable artTab;
		try {
			artTab = new ArticlesTable(dbHelper);
			artTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "getDataFromExistingReview: could not open the Articles table - " + e.getMessage());
			return -1;
		}
		article = artTab.getItem(review.getArticleId());
		artTab.close();
		if (article == null) {
			Common.log(1, TAG, "getDataFromExistingReview: could not get Artcile from DB");
			return -2;
		}
		Common.log(5, TAG, "getDataFromExistingReview: built Article with Id '" + article.getId() + "'");

		/*
		 * Get ReviewDimensions from table
		 */
		ReviewDimensionsTable revDimTab;
		try {
			revDimTab = new ReviewDimensionsTable(dbHelper);
			revDimTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "getDataFromExistingReview: could not open the ReviewsDimensions table - " + e.getMessage());
			return -1;
		}
		reviewDimensions = revDimTab.getAllItemsOfReview(review.getId());
		revDimTab.close();
		if (reviewDimensions == null) {
			Common.log(1, TAG, "getDataFromExistingReview: could not get Reviews Dimensions from DB");
			return -2;
		}
		Common.log(5, TAG, "getDataFromExistingReview: got '" + reviewDimensions.size() + "' revDims from table");
		if (reviewDimensions.size() <= 0) {
			Common.log(1, TAG, "getDataFromExistingReview: could not find dimensions for this review");
			return -3;
		}

		/*
		 * Get Dimensions from table
		 */
		DimensionsTable dimTab;
		try {
			dimTab = new DimensionsTable(dbHelper);
			dimTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "getDataFromExistingReview: could not open the Dimensions table - " + e.getMessage());
			return -1;
		}
		Common.log(5, TAG, "getDataFromExistingReview: will fetch dimensions");
		for (ReviewDimension revDim : reviewDimensions) {
			Dimension dimTmp = dimTab.getItem(revDim.getDimId());
			if (dimTmp != null) {
				dimensions.add(dimTmp);
			} else {
				Common.log(3, TAG, "getDataFromExistingReview: could not add Dimension with Id '" + revDim + "'");
			}
		}
		dimTab.close();
		Common.log(5, TAG, "getDataFromExistingReview: got '" + dimensions.size() + "' dimensions from table");
		if (reviewDimensions.size() != dimensions.size())
			return -4;
		else
			return 0;
	}

	private static boolean addNewReview() {
		Common.log(5, TAG, "addNewReview: started");

		SQLiteDatabase database;
		SQLiteHelper dbHelper = new SQLiteHelper(context);
		try {
			database = dbHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Common.log(1, TAG, "addNewReview: error getting writable database - " + e.getMessage());
			return false;
		}

		database.beginTransaction();

		/*
		 * Add Article to Table
		 */
		ArticlesTable artTab;
		try {
			artTab = new ArticlesTable(dbHelper, database);
			artTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "addNewReview: could not open the Articles table - " + e.getMessage());
			database.endTransaction();
			database.close();
			return false;
		}
		Common.log(5, TAG, "addNewReview: will add article");
		long artResult = artTab.addItem(article);
		artTab.close();
		if (artResult <= 0) {
			if (artResult == -2) {
				Common.log(5, TAG, "addNewReview: did not add article because already exists - OK");
			} else {
				Common.log(1, TAG, "addNewReview: ERROR adding new article to table (error '" + artResult + "')");
				database.endTransaction();
				database.close();
				return false;
			}
		}
		Common.log(3, TAG, "addNewReview: created article '" + article.getName() + "'");

		/*
		 * Add new Review to Table
		 */
		ReviewsTable revTab;
		try {
			revTab = new ReviewsTable(dbHelper, database);
			revTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "addNewReview: could not open the Reviews table - " + e.getMessage());
			database.endTransaction();
			database.close();
			return false;
		}
		// Não verifica se já existe review para este artigo porque só chega
		// aqui se, no onResume, já verificou que não existe
		Common.log(5, TAG, "addNewReview: review is new - will add");
		Review revTmp = new Review(-1, Common.revStates.WORK_IN_PROGRESS, article.getId(), null);
		long revResult = revTab.addItem(revTmp);
		revTab.close();
		if (revResult <= 0) {
			Common.log(1, TAG, "addNewReview: error adding new review to table (error '" + revResult + "')");
			database.endTransaction();
			database.close();
			return false;
		}
		revTmp.setId(revResult);
		review = revTmp;
		Common.log(3, TAG, "addNewReview: created new review with ID '" + revResult + "'");

		/*
		 * Add Dimensions to Table
		 */
		DimensionsTable dimTab;
		try {
			dimTab = new DimensionsTable(dbHelper, database);
			dimTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "addNewReview: could not open the Dimensions table - " + e.getMessage());
			database.endTransaction();
			database.close();
			return false;
		}
		Common.log(5, TAG, "addNewReview: will add new dimensions");
		int errorCount = 0;
		for (Dimension dim : dimensions) {
			long dimResult = dimTab.addItem(dim);
			if (dimResult == -2) {
				Common.log(3, TAG, "addNewReview: dimension with ID '" + dim.getId() + "' already exists in the table and was not added");
			} else if (dimResult <= 0) {
				errorCount++;
				Common.log(1, TAG, "addNewReview: ERROR adding new dimension to table (error '" + dimResult + "')");
			}
		}
		dimTab.close();
		if (errorCount > 0) {
			Common.log(1, TAG, "addNewReview: could not add all the required dimensions ('" + errorCount + "' errors)");
			database.endTransaction();
			database.close();
			return false;
		}
		Common.log(3, TAG, "addNewReview: created all required Dimensions (" + dimensions.size() + ")");

		/*
		 * Add ReviewDimensions to Table
		 */
		ReviewDimensionsTable revDimTab;
		try {
			revDimTab = new ReviewDimensionsTable(dbHelper, database);
			revDimTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "addNewReview: could not open the ReviewDimensions table - " + e.getMessage());
			database.endTransaction();
			database.close();
			return false;
		}
		Common.log(5, TAG, "addNewReview: will add new dimensions");
		errorCount = 0;
		for (Dimension dim : dimensions) {
			ReviewDimension revDimTmp = new ReviewDimension(revResult, dim.getId(), -1);
			long revDimResult = revDimTab.addItem(revDimTmp);
			if (revDimResult == -1) {
				Common.log(3, TAG, "addNewReview: dimension with ID '" + dim.getId() + "' already exists in the table and was not added");
			} else if (revDimResult <= 0) {
				errorCount++;
				Common.log(1, TAG, "addNewReview: ERROR adding new reviewDimension to table (error '" + revDimResult + "')");
			}
		}
		reviewDimensions = revDimTab.getAllItemsOfReview(revResult);
		revDimTab.close();
		if (errorCount > 0) {
			Common.log(1, TAG, "addNewReview: could not add all the required ReviewDimensions ('" + errorCount + "' errors)");
			database.endTransaction();
			database.close();
			return false;
		}
		Common.log(3, TAG, "addNewReview: created all required ReviewDimensions (" + dimensions.size() + ")");

		database.setTransactionSuccessful();
		database.endTransaction();
		database.close();

		showReview();

		Common.log(5, TAG, "addNewReview: finished");
		return true;
	}

	private static void drawDimensions() {
		if (dimensions.size() <= 0) {
			Common.log(3, TAG, "addDimensions: skiped entire method since there are no dimensions");
			return;
		}

		LinearLayout linLay = (LinearLayout) ((Activity) context).findViewById(R.id.mainLinLay);

		LinearLayout.LayoutParams linLayParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		linLayParams.bottomMargin = Common.pixelsFromDPs(context, 40);

		LinearLayout.LayoutParams dimTxtParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);

		/*
		 * Build revDimValues HashMap to recover previous user choices
		 */
		HashMap<Long, Integer> revDimValues = new HashMap<Long, Integer>();
		for (ReviewDimension revDim : reviewDimensions) {
			revDimValues.put(revDim.getDimId(), revDim.getValue());
		}

		SeekBar testSeekExistence;
		for (Dimension dim : dimensions) {
			testSeekExistence = (SeekBar) ((Activity) context).findViewById((int) dim.getId());
			if (testSeekExistence != null)
				continue;

			LinearLayout newLinLay = new LinearLayout(context);
			newLinLay.setOrientation(LinearLayout.VERTICAL);
			newLinLay.setLayoutParams(linLayParams);

			TextView newLabel = new TextView(context);
			newLabel.setText(dim.getLabel());
			newLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			newLabel.setTypeface(null, Typeface.BOLD);

			SeekBar newBar = new SeekBar(context);
			newBar.setId((int) dim.getId());
			newBar.setMax(100);
			
			/*
			 * In read only, MAKE SEEKBAR NOT EDITABLE
			 * else, ADD onChangeListener
			 */
			if(isReviewReadOnly) {
				newBar.setEnabled(false);
			} else {
				newBar.setOnSeekBarChangeListener(((ReviewActivity) context).new seekBarChangeListener());
			}


			if (revDimValues.containsKey(dim.getId())) {
				int value = revDimValues.get(dim.getId());
				if (value >= 0 && value <= 100) {
					newBar.setProgress(value);
				}
			}

			LinearLayout newLinLayClass = new LinearLayout(context);
			newLinLayClass.setOrientation(LinearLayout.HORIZONTAL);

			TextView newMin = new TextView(context);
			newMin.setText(dim.getMin());
			newMin.setGravity(Gravity.LEFT);
			newMin.setLayoutParams(dimTxtParams);
			newLinLayClass.addView(newMin);

			TextView newMed = new TextView(context);
			if (dim.getMed() != null && dim.getMed() != "")
				newMed.setText(dim.getMed());
			else
				newMed.setText("");
			newMed.setGravity(Gravity.CENTER_HORIZONTAL);
			newMed.setLayoutParams(dimTxtParams);
			newLinLayClass.addView(newMed);

			TextView newMax = new TextView(context);
			newMax.setText(dim.getMax());
			newMax.setGravity(Gravity.RIGHT);
			newMax.setLayoutParams(dimTxtParams);
			newLinLayClass.addView(newMax);

			newLinLay.addView(newLabel);
			newLinLay.addView(newBar);
			newLinLay.addView(newLinLayClass);

			linLay.addView(newLinLay);
		}
	}

	private class seekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			/*
			 * In read only this method should not be reached but, if it is,
			 * EXIT METHOD
			 */
			if(isReviewReadOnly)
				return;

			int changedBar = seekBar.getId();
			for (ReviewDimension revDim : reviewDimensions) {
				if (revDim.getDimId() == changedBar) {
					revDim.setValue(progress);
				}
			}
		}

		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		public void onStopTrackingTouch(SeekBar seekBar) {
		}
	}

	private void reviewSave() {
		Common.log(5, TAG, "reviewSave: started");

		/*
		 * In read only this method should not be reached but, if it is,
		 * EXIT METHOD
		 */
		if(isReviewReadOnly)
			return;

		if (review == null || reviewDimensions == null || reviewDimensions.isEmpty()) {
			Common.log(5, TAG, "reviewSave: not enough data available to save the review");
			return;
		}

		SQLiteHelper dbHelper = new SQLiteHelper(this);

		/*
		 * Update Review in Table
		 */
		ReviewsTable revTab;
		try {
			revTab = new ReviewsTable(dbHelper);
			revTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "reviewSave: could not open the Reviews table - " + e.getMessage());
			return;
		}
		boolean revResult = revTab.updateItem(review);
		revTab.close();
		Common.log(5, TAG, "reviewSave: updated com sucesso o review com ID '" + review.getId() + "'");

		/*
		 * Update ReviewDimensions in Table
		 */
		ReviewDimensionsTable revDimTab;
		try {
			revDimTab = new ReviewDimensionsTable(dbHelper);
			revDimTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "reviewSave: could not open the ReviewDimensions table - " + e.getMessage());
			return;
		}
		int errorCount = 0;
		for (ReviewDimension revDim : reviewDimensions) {
			boolean revDimResult = revDimTab.updateItem(revDim);
			if (revDimResult) {
				Common.log(5, TAG, "reviewSave: alterado com sucesso o registo com revId '" + revDim.getRevId() + "' e dimId '" + revDim.getDimId() + "'");
			} else {
				errorCount++;
				Common.log(1, TAG, "reviewSave: ERROR adding new reviewDimension to table (error '" + revDimResult + "')");
			}
		}
		revDimTab.close();

		if (revResult && errorCount == 0) {
			Common.log(5, TAG, "reviewSave: todos os updates realizados com sucesso");
		}
		if (!revResult) {
			Common.log(1, TAG, "reviewSave: dimensões atualizadas com sucesso mas review não");
		} else {
			Common.log(1, TAG, "reviewSave: review atualizado com sucesso mas dimensões não");
		}

		Common.log(5, TAG, "reviewSave: finished");
	}

	/**
	 * Submissão de uma review consiste em 2 coisas:<br>
	 * 1 - Envio ao servidor central de uma ordem de PUT, com toda a informação<br>
	 * 2 - Actualização do estado na BD da app de <b>WIP</b> para <b>Completed</b>, se 1 for um sucesso
	 * 
	 * @param view
	 */
	public void reviewSubmit(View view) {
		
		/*
		 * In read only this method should not be reached but, if it is,
		 * EXIT METHOD
		 */
		if(isReviewReadOnly)
			return;
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String userName = sharedPref.getString("userName", null);
		String userEmail = sharedPref.getString("userEmail", null);

		if(userName == null || userName.equals("") || userEmail == null || userEmail.equals("")) {
			//Common.longToast(this, "Cannot submit, user name/email not defined");
			AlertDialog.Builder alertUserData = new AlertDialog.Builder(this);
			alertUserData.setTitle(resources.getString(R.string.dialog_reviewSubmitTitle));
			alertUserData.setMessage(resources.getString(R.string.dialog_reviewSubmitUserInfoError));
			alertUserData.setPositiveButton(resources.getString(R.string.button_generalContinue), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
		    		startActivity(new Intent(context, Preferences.class));
				}
			});
			alertUserData.setNegativeButton(resources.getString(R.string.button_generalReturn), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					return;
				}
			});
			alertUserData.show();
			return;
		}
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(resources.getString(R.string.dialog_reviewSubmitTitle));
		alert.setMessage(resources.getString(R.string.dialog_reviewSubmitConfirmationMessage));
		alert.setPositiveButton(resources.getString(R.string.button_generalContinue), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				review.setState(Common.revStates.COMPLETED);
				reviewSave();
				
				String url = Common.httpVariables.REVIEW_PREFIX;
				(new HTTPRequest(context, new httpRequestHandler(), url, HTTPRequest.requestTypes.SUBMIT_REVIEW)).start();
				Common.longToast(context, resources.getString(R.string.toast_reviewSubmitSuccess));
				
				finish();
			}
		});

		alert.setNegativeButton(resources.getString(R.string.button_generalReturn), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});
		alert.show();
	}

	public void reviewPhotos(View view) {
		Intent intent = new Intent(this, PhotosManagementActivity.class);
		intent.putExtra("revId", review.getId());
		// Send state of read only to also implement in next activity
		intent.putExtra("isReviewReadOnly", isReviewReadOnly);
		startActivity(intent);
	}

	public void reviewComment(View view) {
		Common.log(5, TAG, "reviewComment: started");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(R.string.label_reviewCommentTitle);
		alert.setMessage(R.string.label_reviewCommentMessage);
		
		final EditText input = new EditText(this);
//		PARA POR MULTILINHA TERA Q SER POR AQUI ... MAS ISTO AINDA NÃO FUNCIONA
//		input.setGravity(Gravity.TOP);
//		input.setInputType(InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);// | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
//		input.setLines(3);
		if (review.getComment() != null)
			input.setText(review.getComment());
		alert.setView(input);

		/*
		 * If review is to be presented as read-only,
		 * DO NOT ALLOW COMMENT TO BE EDITED
		 * else
		 * CREATE SAVE BUTTON
		 */
		if(isReviewReadOnly) {
			input.setEnabled(false);
		} else {
			alert.setPositiveButton(getResources().getString(R.string.button_generalSave), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input.getText().toString();
					Common.log(5, TAG, "reviewComment: capturou o input '" + value + "'");
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY,0);
					review.setComment(value);
				}
			});

			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
		}

		alert.setNegativeButton(getResources().getString(R.string.button_generalReturn), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY,0);
				// Canceled.
			}
		});

		alert.show();
		Common.log(5, TAG, "reviewComment: will exit");
	}

	static class httpRequestHandler extends Handler {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(android.os.Message msg) {
			Common.log(5, TAG, "httpRequestHandler: Comecei a tratar as dimensoes que recebi");
			String errorMsg = null;
			List<Dimension> newDims = null;

			switch (msg.what) {
			case HTTPRequest.responseOutputs.SUCCESS:
				newDims = (List<Dimension>) msg.obj;
				Iterator<Dimension> itr = newDims.iterator();
				while (itr.hasNext())
					Common.log(5, TAG, "httpRequestHandler: Dimensão recebida no ReviewActivity:" + ((Dimension) itr.next()).getLabel());
				break;
			case HTTPRequest.responseOutputs.FAILED_NO_NETWORK_CONNECTION_DETECTED:
				errorMsg = resources.getString(R.string.toast_generalNoConnection) + "\n" + resources.getString(R.string.toast_generalCannotContinue);
				break;
			case HTTPRequest.responseOutputs.FAILED_ERROR_ON_SUPPLIED_URL:
				errorMsg = resources.getString(R.string.toast_generalErrorSearchingArticle) + "\n(" + resources.getString(R.string.toast_errorInvalidURL) + ")";
				break;
			case HTTPRequest.responseOutputs.FAILED_QUERY_FROM_INTERNET:
				errorMsg = resources.getString(R.string.toast_generalErrorSearchingArticle) + "\n(" + resources.getString(R.string.toast_errorNoAnswerToWebRequest) + ")";
				break;
			case HTTPRequest.responseOutputs.FAILED_GETTING_VALID_RESPONSE_FROM_QUERY:
				errorMsg = resources.getString(R.string.toast_generalErrorSearchingArticle) + "\n(" + resources.getString(R.string.toast_errorEmptyAnswerFromWebRequest) + ")";
				break;
			case HTTPRequest.responseOutputs.FAILED_PROCESSING_RETURNED_OBJECT:
				errorMsg = resources.getString(R.string.toast_generalErrorSearchingArticle) + "\n(" + resources.getString(R.string.toast_errorIncompatibleObject) + ")";
				break;
			case HTTPRequest.responseOutputs.FAILED_OBJECT_NOT_FOUND:
				String serverErrorMsg = msg.getData().getString("errorMessage");
				errorMsg = resources.getString(R.string.toast_generalErrorSearchingArticle) + "\n(" + serverErrorMsg + ")";
				break;
			default:
				errorMsg = resources.getString(R.string.toast_generalUndefinedError) + "\n(" + resources.getString(R.string.toast_errorUndefinedErrorFromWebReques) + ")";
				break;
			}

			if (dialog != null && dialog.isShowing())
				dialog.dismiss();

			if (msg.what != HTTPRequest.responseOutputs.SUCCESS) {
				shutdownWithError("httpRequestHandler: ERROR - " + errorMsg, "Error obtaining information for Article review; cannot continue");
			} else if (newDims == null) {
				shutdownWithError("httpRequestHandler: ERROR - received dimensions where null", "Error obtaining information for Article review; cannot continue");
			} else {
				dimensions = newDims;
				addNewReview();
			}
		}
	}
}

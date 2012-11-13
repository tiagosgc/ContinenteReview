package pt.continente.review;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import pt.continente.review.common.Article;
import pt.continente.review.common.Common;
import pt.continente.review.common.Dimension;
import pt.continente.review.common.HTTPRequest;
import pt.continente.review.common.Review;
import pt.continente.review.common.ReviewDimension;
import pt.continente.review.getpictures.PhotosManagementActivity;
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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ReviewActivity extends Activity {
	private static final String TAG = "CntRev - ReviewActivity";

	private static ImageView imageView;
	private Context context = this;

	private static long revId;
	public Article article = null;
	private Review review = null;
	private List<Dimension> dimensions = null;
	private List<ReviewDimension> reviewDimensions = null;

	private ProgressDialog dialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Common.log(5, TAG, "onCreate: started");
		setContentView(R.layout.activity_review);

		imageView = (ImageView) findViewById(R.id.articleIcon);

		/*
		 * Attempts to get either an Id of an existing review or the object of
		 * the article already retrieved for a new review
		 */
		revId = (long) getIntent().getLongExtra("revId", -1);
		article = (Article) getIntent().getSerializableExtra("Article");
		if (article != null) {
			Bitmap artImg = (Bitmap) getIntent().getParcelableExtra("ArticleImage");
			if (artImg == null)
				Common.log(5, TAG, "onCreate: imagem obtida é nula");
			article.setImage(artImg);
		}
		Common.log(5, TAG, "onCreate: finished");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_article, menu);
		return true;
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
			Common.longToast(this, "Error retrieving the Article; cannot continue");
			return;
		}

		if (revId == -1 && article != null) {
			Common.log(5, TAG, "onResume: will check if received article already has a pending review");

			SQLiteHelper dbHelper = new SQLiteHelper(this);
			ReviewsTable revTab;
			try {
				revTab = new ReviewsTable(dbHelper);
				revTab.open();
			} catch (Exception e) {
				shutdownWithError("onResume: ERROR could not open the table - " + e.getMessage(), "Error accessing local tables; cannot continue");
				return;
			}
			long revIdTmp = revTab.findItemFromActive(article.getId());
			if (revIdTmp > 0) {
				revId = revIdTmp;
			}
			revTab.close();
		}

		if (revId != -1) {
			Common.log(5, TAG, "onResume: will recover existing review with Id '" + revId + "'");
			int result = getDataFromExistingReview();
			if (result < 0) {
				shutdownWithError("onResume: ERROR getting information from DB; cannot continue (revId = '" + revId + "', result = '" + result + "'", "Error getting information from DB; cannot continue");
				return;
			} else {
				showReview();
			}

		} else if (article != null) {
			Common.log(5, TAG, "onResume: review for received article not present, will create new");

			String url = Common.httpVariables.DIMENSIONS_PREFIX + article.getId();
			Common.log(5, TAG, "onResume: will atempt to launch service to get content from url '" + url + "'");
			//TODO Tiago:Quando lançar isto tenho que ter neste objecto tudo o que é para fazer submit
			//Article id:OK
			//Comentário:?
			//Fotos:?
			//Dimensões com o score preenchido:?
			(new HTTPRequest(this, new httpRequestHandler(this), url, HTTPRequest.requestTypes.GET_DIMENSIONS)).start();
			dialog = ProgressDialog.show(this, "A obter informação", "a consultar...");
		}

		Common.log(5, TAG, "onResume: finished");
	}

	@Override
	protected void onPause() {
		super.onPause();
		reviewSave();
	}

	private void showReview() {
		Common.log(5, TAG, "showReview: started");

		if (review == null || article == null || dimensions == null || dimensions.isEmpty() || reviewDimensions == null || reviewDimensions.isEmpty()) {
			shutdownWithError("showReview: ERROR no valid source to update view; cannot continue", "Error getting data to present; cannot continue");
			return;
		}

		TextView t = (TextView) findViewById(R.id.articleName);
		t.setText(article.getName());

		Common.log(5, TAG, "showReview: will set bitmap");
		Bitmap productBitmap = article.getImage();
		if (productBitmap == null) {
			Common.log(1, TAG, "showReview: ERROR article object did not contain image; will attempt to get from URL");
		} else {
			imageView.setImageBitmap(productBitmap);
		}

		Common.log(5, TAG, "showReview: will draw dimensions");
		if (dimensions != null)
			drawDimensions();
	}

	private void shutdownWithError(String debugText, String userText) {
		Common.log(1, TAG, debugText);
		finish();
		Common.longToast(this, userText);
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

	private boolean addNewReview() {
		Common.log(5, TAG, "addNewReview: started");

		SQLiteDatabase database;
		SQLiteHelper dbHelper = new SQLiteHelper(this);
		try {
			database = dbHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.i(TAG, "addNewReview: error getting writable database - " + e.getMessage());
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
			//FIXME para o Fred: Este addItem retorna -2 quando se tenta adicionar a segunda dimensão
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

	private void drawDimensions() {
		if (dimensions.size() <= 0) {
			Common.log(3, TAG, "addDimensions: skiped entire method since there are no dimensions");
			return;
		}

		LinearLayout linLay = (LinearLayout) this.findViewById(R.id.mainLinLay);

		LinearLayout.LayoutParams linLayParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		linLayParams.bottomMargin = Common.pixelsFromDPs(this, 40);

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
			testSeekExistence = (SeekBar) findViewById((int) dim.getId());
			if (testSeekExistence != null)
				continue;

			LinearLayout newLinLay = new LinearLayout(this);
			newLinLay.setOrientation(LinearLayout.VERTICAL);
			newLinLay.setLayoutParams(linLayParams);

			TextView newLabel = new TextView(this);
			newLabel.setText(dim.getLabel());
			newLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			newLabel.setTypeface(null, Typeface.BOLD);

			SeekBar newBar = new SeekBar(this);
			newBar.setId((int) dim.getId());
			newBar.setMax(100);
			newBar.setOnSeekBarChangeListener(new seekBarChangeListener());

			if (revDimValues.containsKey(dim.getId())) {
				int value = revDimValues.get(dim.getId());
				if (value >= 0 && value <= 100) {
					newBar.setProgress(value);
				}
			}

			LinearLayout newLinLayClass = new LinearLayout(this);
			newLinLayClass.setOrientation(LinearLayout.HORIZONTAL);

			TextView newMin = new TextView(this);
			newMin.setText(dim.getMin());
			newMin.setGravity(Gravity.LEFT);
			newMin.setLayoutParams(dimTxtParams);
			newLinLayClass.addView(newMin);

			TextView newMed = new TextView(this);
			if (dim.getMed() != null && dim.getMed() != "")
				newMed.setText(dim.getMed());
			else
				newMed.setText("");
			newMed.setGravity(Gravity.CENTER_HORIZONTAL);
			newMed.setLayoutParams(dimTxtParams);
			newLinLayClass.addView(newMed);

			TextView newMax = new TextView(this);
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
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Submeter Review");
		alert.setMessage("Tem a certeza que pretende submeter este Review?");
		ReviewActivity ra = this;
		alert.setPositiveButton("Submeter", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				review.setState(Common.revStates.COMPLETED);
				reviewSave();
				
				String url = Common.httpVariables.REVIEW_PREFIX;
				(new HTTPRequest(context, new httpRequestHandler((ReviewActivity) context), url, HTTPRequest.requestTypes.SUBMIT_REVIEW)).start();
				Common.longToast(context, "Submission not yet implemented; just changed the state to COMPLETED");
				
				//
				finish();
			}
		});

		alert.setNegativeButton("Voltar", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});
		alert.show();
	}

	public void reviewPhotos(View view) {
		//FIXME para o Fred:O Tiago encontrou um bug. 
		/* Se clickares no botão de tirar fotos e te arrependeres sem tirar nenhuma foto, o botão de back não funciona
		 * Mais: Mesmo no momento de optar pela app que vai "tirar fotos", já não consigo fazer back
		 */
		Intent intent = new Intent(this, PhotosManagementActivity.class);
		intent.putExtra("revId", review.getId());
		startActivity(intent);
	}

	public void reviewComment(View view) {
		Common.log(5, TAG, "reviewComment: started");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(R.string.label_reviewCommentTitle);
		alert.setMessage(R.string.label_reviewCommentMessage);

		final EditText input = new EditText(this);
		if (review.getComment() != null)
			input.setText(review.getComment());
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				Common.log(5, TAG, "reviewComment: capturou o input '" + value + "'");
				review.setComment(value);
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});

		alert.show();
		Common.log(5, TAG, "reviewComment: will exit");
	}

	static class httpRequestHandler extends Handler {
		WeakReference<ReviewActivity> outerClass;

		httpRequestHandler(ReviewActivity outerClass) {
			this.outerClass = new WeakReference<ReviewActivity>(outerClass);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(android.os.Message msg) {
			Common.log(5, TAG, "httpRequestHandler: Comecei a tratar as dimensoes que recebi");
			ReviewActivity outerClassLocalObj = outerClass.get();
			String errorMsg = null;
			List<Dimension> newDims = null;

			switch (msg.what) {
			case HTTPRequest.responseOutputs.SUCCESS:
				newDims = (List<Dimension>) msg.obj;
				
				Iterator<Dimension> itr = newDims.iterator();
				while (itr.hasNext())
				{
					Common.log(5, TAG, "httpRequestHandler: Dimensão recebida no ReviewActivity:" + ((Dimension) itr.next()).getLabel());
				}
				break;
			case HTTPRequest.responseOutputs.FAILED_NO_NETWORK_CONNECTION_DETECTED:
				errorMsg = "No network connection was detected; cannot continue";
				break;
			case HTTPRequest.responseOutputs.FAILED_ERROR_ON_SUPPLIED_URL:
				errorMsg = "Supplied value was not valid";
				break;
			case HTTPRequest.responseOutputs.FAILED_QUERY_FROM_INTERNET:
				errorMsg = "No answer from internet (connection or server down)";
				break;
			case HTTPRequest.responseOutputs.FAILED_GETTING_VALID_RESPONSE_FROM_QUERY:
				errorMsg = "Query return was empty";
				break;
			case HTTPRequest.responseOutputs.FAILED_PROCESSING_RETURNED_OBJECT:
				errorMsg = "Query was invalid (not compatible with expected result)";
				break;
			case HTTPRequest.responseOutputs.FAILED_OBJECT_NOT_FOUND:
				String serverErrorMsg = msg.getData().getString("errorMessage");
				errorMsg = "Could not find dimensions for this article (" + serverErrorMsg + ")";
				break;
			default:
				errorMsg = "Undefined error when retrieving data from the internet";
				break;
			}

			if (outerClassLocalObj.dialog != null && outerClassLocalObj.dialog.isShowing())
				outerClassLocalObj.dialog.dismiss();

			if (msg.what != HTTPRequest.responseOutputs.SUCCESS) {
				outerClassLocalObj.shutdownWithError("httpRequestHandler: ERROR - " + errorMsg, "Error obtaining information for Article review; cannot continue");
			} else if (newDims == null) {
				outerClassLocalObj.shutdownWithError("httpRequestHandler: ERROR - received dimensions where null", "Error obtaining information for Article review; cannot continue");
			} else {
				outerClassLocalObj.dimensions = newDims;
				outerClassLocalObj.addNewReview();
			}
		}
	}
}

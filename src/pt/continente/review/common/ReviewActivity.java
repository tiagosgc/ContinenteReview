package pt.continente.review.common;

import java.lang.ref.WeakReference;
import java.util.List;

import pt.continente.review.R;
import pt.continente.review.getpictures.PhotosManagementActivity;
import pt.continente.review.tables.ArticlesTable;
import pt.continente.review.tables.DimensionsTable;
import pt.continente.review.tables.ReviewDimensionsTable;
import pt.continente.review.tables.ReviewsTable;
import pt.continente.review.tables.SQLiteHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
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

	private static long revId;
	private Article article = null;
	private Review review = null; 
	private List<Dimension> dimensions = null;

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
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_article, menu);
		return true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
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
					shutdownWithError(
							"onResume: ERROR could not open the table - " + e.getMessage(),
							"Error accessing local tables; cannot continue");
					return;
				}
		    	long revIdTmp = revTab.findItem(article.getId());
		    	if (revIdTmp > 0) {
					revId = revIdTmp;
		    	}
		    	revTab.close();
		}
		
		if (revId != -1) {
			Common.log(5, TAG, "onResume: will create recover existing review");
			int result = getDataFromExistingReview(revId);
			if (result < 0) {
				shutdownWithError(
						"onResume: ERROR getting information from DB; cannot continue (revId = '" + revId + "', result = '" + result + "'",
						"Error getting information from DB; cannot continue");
				return;
			} else {
				showReview();
			}
			
			
		} else if (article != null) {
			Common.log(5, TAG, "onResume: review for received article not present, will create new");

	    	String url = Common.httpVariables.DIMENSIONS_PREFIX + article.getId();
			Common.log(5, TAG, "onResume: will atempt to launch service to get content from url '" + url + "'");
			(new HTTPRequest(new httpRequestHandler(this), url, HTTPRequest.requestTypes.GET_DIMENSIONS)).start();
			dialog = ProgressDialog.show(this, "A obter informação", "a consultar...");
		}
	}
	
	
	private void showReview() {
		Common.log(5, TAG, "onCreate: will set text info in activity");
		
		if (review == null || article == null || dimensions == null) {
			shutdownWithError(
					"showReview: ERROR no valid source to update view; cannot continue",
					"Error getting data to present; cannot continue");
			return;
		}
		
		TextView t = (TextView) findViewById(R.id.articleName);
		t.setText(article.getName());

		Common.log(5, TAG, "showReview: will set bitmap");
		Bitmap productBitmap = article.getImage();
		if(productBitmap == null) {
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
	 * @param revId ID of review from which data should be retrieved
	 * @return
	 * <b>0</b> if all objects (<b><i>review</i></b>, <b><i>article</i></b> and <b><i>dimensions</i></b>) were properly populated<br>
	 * <b>-1</b> if an error occurred when opening a table (see Log to check which one)<br>
	 * <b>-2</b> if an error occurred when getting a specific object from a table (see Log to check which one)<br>
	 * <b>-3</b> if no dimensions were returned for current review<br>
	 * <b>-4</b> if not all review dimensions were found in the Dimensions table
	 */
	private int getDataFromExistingReview(long revId) {
		Common.log(5, TAG, "getDataFromRevId: started");
    	SQLiteHelper dbHelper = new SQLiteHelper(this);
    	
    	ReviewsTable revTab;
    	try {
			revTab = new ReviewsTable(dbHelper);
			revTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "getDataFromRevId: could not open the Reviews table - " + e.getMessage());
			return -1;
		}
    	review = revTab.getItem(revId);
    	revTab.close();
    	if (review == null) {
    		Common.log(1, TAG, "getDataFromRevId: could not get Review from DB");
    		return -2;
    	}
    	Common.log(5, TAG, "getDataFromRevId: built Review with Id '" + review.getId() + "'");
    	
    	ArticlesTable artTab;
    	try {
			artTab = new ArticlesTable(dbHelper);
			artTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "getDataFromRevId: could not open the Articles table - " + e.getMessage());
			return -1;
		}
    	article = artTab.getItem(review.getArticleId());
    	artTab.close();
    	if (article == null) {
    		Common.log(1, TAG, "getDataFromRevId: could not get Artcile from DB");
    		return -2;
    	}
    	Common.log(5, TAG, "getDataFromRevId: built Article with Id '" + article.getId() + "'");
    	
    	ReviewDimensionsTable revDimTab;
    	try {
    		revDimTab = new ReviewDimensionsTable(dbHelper);
    		revDimTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "getDataFromRevId: could not open the ReviewsDimensions table - " + e.getMessage());
			return -1;
		}
    	List<Long> revDims = revDimTab.getAllItemsOfReview(review.getId());
    	revDimTab.close();
    	if (revDims == null) {
    		Common.log(1, TAG, "getDataFromRevId: could not get Reviews Dimensions from DB");
			return -2;
    	}
    	Common.log(5, TAG, "getDataFromRevId: got '" + revDims.size() + "' revDims from table");
    	if (revDims.size() <= 0) {
    		Common.log(1, TAG, "getDataFromRevId: could not find dimensions for this review");
    		return -3;
    	}
    	
    	DimensionsTable dimTab;
    	try {
    		dimTab = new DimensionsTable(dbHelper);
    		dimTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "getDataFromRevId: could not open the Dimensions table - " + e.getMessage());
			return -1;
		}
    	for(long revDim : revDims) {
    		Dimension dimTmp = dimTab.getItem(revDim);
    		if (dimTmp != null) {
    			dimensions.add(dimTmp);
    		} else {
    			Common.log(3, TAG, "getDataFromRevId: could not add Dimension with Id '" + revDim + "'");
    		}
    	}
    	Common.log(5, TAG, "getDataFromRevId: got '" + dimensions.size() + "' dimensions from table");
    	if (revDims.size() != dimensions.size())
    		return -4;
    	else
    		return 0;
	}
	
	
	private boolean addNewReview() {
		Common.log(5, TAG, "addNewReview: started");
		
    	SQLiteHelper dbHelper = new SQLiteHelper(this);
    	
    	/*
    	 *  Add Article to Table
    	 */
    	ArticlesTable artTab;
    	try {
			artTab = new ArticlesTable(dbHelper);
			artTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "addNewReview: could not open the table - " + e.getMessage());
			return false;
		}
		Common.log(5, TAG, "addNewReview: will add article");
		//TODO capturar erro na adição à tabela
    	artTab.addItem(article);
    	artTab.close();
        Common.log(3, TAG, "addNewReview: created article '" + article.getName() + "'");
        
    	/*
    	 *  Add new Review to Table
    	 */
    	ReviewsTable revTab;
    	try {
			revTab = new ReviewsTable(dbHelper);
			revTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "addNewReview: could not open the table - " + e.getMessage());
			return false;
		}
    	// Não verifica se já existe review para este artigo porque só chega aqui se, no onResume, já verificou que não existe
		Common.log(5, TAG, "addNewReview: review is new - will add");
    	Review revTmp = new Review(-1, Common.revStates.WORK_IN_PROGRESS, article.getId(), null);
		//TODO capturar erro na adição à tabela
    	long revTmpId = revTab.addItem(revTmp);
        Common.log(3, TAG, "addNewReview: created new review with ID '" + revTmpId + "'");
    	revTab.close();

		//TODO adicionar dimensions e rev dimensions às tabelas
    	
    	
    	
    	
		Common.log(5, TAG, "addNewReview: finished");
		return true;
	}

	private void drawDimensions() {
		if (dimensions.size() <= 0) {
			Common.log(3, TAG, "addDimensions: skiped entire method since there are no dimensions");
			return;
		}
		
		LinearLayout linLay = (LinearLayout) this.findViewById(R.id.mainLinLay);;

		LinearLayout.LayoutParams linLayParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		linLayParams.bottomMargin = Common.pixelsFromDPs(this, 20);
		
		LinearLayout.LayoutParams dimTxtParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f);

		for (Dimension dim : dimensions) {
			LinearLayout newLinLay = new LinearLayout(this);
			newLinLay.setOrientation(LinearLayout.VERTICAL);
			newLinLay.setLayoutParams(linLayParams);
			
			TextView newLabel = new TextView(this);
			newLabel.setText(dim.getLabel());
			
			SeekBar newBar = new SeekBar(this);
			newBar.setId((int)dim.getId());
			newBar.setMax(100);
			
			LinearLayout newLinLayClass = new LinearLayout(this);
			newLinLayClass.setOrientation(LinearLayout.HORIZONTAL);
			
			TextView newMin = new TextView(this);
			newMin.setText(dim.getMin());
			newMin.setGravity(Gravity.LEFT);
			newMin.setLayoutParams(dimTxtParams);
			newLinLayClass.addView(newMin);
			
			TextView newMed = new TextView(this);
			if(dim.getMed() != null && dim.getMed() != "")
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
	
	public void reviewPhotos(View view) {
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
			}
		);

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
				}
			}
		);

		alert.show();
		Common.log(5, TAG, "reviewComment: will exit");
	}

	static class httpRequestHandler extends Handler {
		WeakReference<ReviewActivity> outerClass;
		
		httpRequestHandler(ReviewActivity outerClass) {
			this.outerClass = new WeakReference<ReviewActivity>(outerClass);
		}
		
		@Override
		public void handleMessage(android.os.Message msg) {
			ReviewActivity outerClassLocalObj = outerClass.get();
			String errorMsg = null;
			DimensionsList newRevDims = null;

			switch (msg.what) {
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
        	case HTTPRequest.responseOutputs.SUCCESS:
        		newRevDims = (DimensionsList) msg.getData().getSerializable("response");
        		break;
        	}
			
        	if(outerClassLocalObj.dialog != null && outerClassLocalObj.dialog.isShowing())
        		outerClassLocalObj.dialog.dismiss();
        	
        	if (msg.what != HTTPRequest.responseOutputs.SUCCESS) {
        		outerClassLocalObj.shutdownWithError(
    					"httpRequestHandler: ERROR - " + errorMsg,
    					"Error obtaining information for Article review; cannot continue");
        	} else if(newRevDims == null) {
        		outerClassLocalObj.shutdownWithError(
    					"httpRequestHandler: ERROR - received dimensions where null",
    					"Error obtaining information for Article review; cannot continue");
        	} else {
        		outerClassLocalObj.addNewReview();
        	}
        }
    }
}
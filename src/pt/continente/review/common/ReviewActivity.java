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
import android.widget.Toast;

public class ReviewActivity extends Activity {
	private static final String TAG = "CntRev - ReviewActivity";

	private static ImageView imageView;

	private Article article = null; 
	private Review review = null; 
	private List<Dimension> dimensions = null;
	private DimensionsList newRevDims = null;

	private ProgressDialog dialog;
	private String responseStr = "ORIGINAL STATE";
	
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
		long revId = (long) getIntent().getLongExtra("revId", -1);
		article = (Article) getIntent().getSerializableExtra("Article");
		
		
		if (revId != -1) {
			Common.log(5, TAG, "onCreate: will create recover existing review");
			int result = getDataFromRevId(revId);
			if (result < 0) {
				Common.log(1, TAG, "onCreate: ERROR getting information from DB; cannot continue (revId = '" + revId + "'");
				Toast.makeText(this, "Error getting information from DB; cannot continue", Toast.LENGTH_LONG).show();
			} else
				showReview();
		} else if (article != null) {
			Common.log(5, TAG, "onCreate: will create new review");

			String url = Common.httpVariables.DIMENSIONS_PREFIX + article.getId();
			Common.log(5, TAG, "onCreate: will atempt to launch service to get content from url '" + url + "'");
			(new HTTPRequest(new httpRequestHandler(this), url, HTTPRequest.requestTypes.GET_DIMENSIONS)).start();
			dialog = ProgressDialog.show(this, "A ober informação", "a consultar...");
			
		} else {
			Common.log(1, TAG, "onCreate: ERROR activity started with unexpected inputs");
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_article, menu);
		return true;
	}
	
	private int getDataFromRevId(long revId) {
		Common.log(5, TAG, "getDataFromRevId: started");
    	SQLiteHelper dbHelper = new SQLiteHelper(this);
    	
    	ReviewsTable revTab;
    	try {
			revTab = new ReviewsTable(dbHelper);
			revTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "getDataFromRevId: could not open the table 1 - " + e.getMessage());
			return -1;
		}
    	review = revTab.getItem(revId);
    	revTab.close();
    	if (review == null) {
    		Common.log(1, TAG, "getDataFromRevId: could not get Object from DB 1");
    		return -2;
    	}
    	Common.log(5, TAG, "getDataFromRevId: built Review with Id '" + review.getId() + "'");
    	
    	ArticlesTable artTab;
    	try {
			artTab = new ArticlesTable(dbHelper);
			artTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "getDataFromRevId: could not open the table 2 - " + e.getMessage());
			return -1;
		}
    	article = artTab.getItem(review.getArticleId());
    	artTab.close();
    	if (article == null) {
    		Common.log(1, TAG, "getDataFromRevId: could not get Object from DB 2");
    		return -2;
    	}
    	Common.log(5, TAG, "getDataFromRevId: built Article with Id '" + article.getId() + "'");
    	
    	ReviewDimensionsTable revDimTab;
    	try {
    		revDimTab = new ReviewDimensionsTable(dbHelper);
    		revDimTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "getDataFromRevId: could not open the table 3 - " + e.getMessage());
			return -1;
		}
    	List<Long> revDims = revDimTab.getAllItemsOfReview(review.getId());
    	revDimTab.close();
    	if (revDims == null) {
    		Common.log(1, TAG, "getDataFromRevId: could not get Object from DB 3");
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
			Common.log(1, TAG, "getDataFromRevId: could not open the table 4 - " + e.getMessage());
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
	
	
	private void showReview() {
		Common.log(5, TAG, "onCreate: will set text info in activity");

		if (article != null)
			addNewReview();
		
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
			addDimensions();
	}


	private boolean addNewReview() {
		
    	SQLiteHelper dbHelper = new SQLiteHelper(this);
    	
    	if(newRevDims == null) {
    		Common.log(1, TAG, "addNewReview: could not retrieve dimensions from Host");
    		Common.longToast(this, responseStr);
    		return false;
    	}
		Common.log(5, TAG, "addNewReview: retrieved '" + newRevDims.size() + "' dimensions for Article with Id '" + article.getId() + "'");
    	dimensions = newRevDims.getObject();
		
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
    	long revTmpId = -1;
    	revTmpId = revTab.findItem(article.getId());
    	if (revTmpId > 0) {
			Common.log(5, TAG, "addNewReview: review already exists, will recover");
			review = revTab.getItem(revTmpId);
    	} else {
			Common.log(5, TAG, "addNewReview: review is new - will add");
	    	Review revTmp;
	    	revTmp = new Review(-1, Common.revStates.WORK_IN_PROGRESS, article.getId(), null);
	    	revTmpId = revTab.addItem(revTmp);
	        Common.log(3, TAG, "addNewReview: created new review with ID '" + revTmpId + "'");
	        
    	}
    	revTab.close();
		Common.log(5, TAG, "addNewReview: will exit");
    	if (revTmpId < 0)
    		return false;
    	else
    		return true;
	}

	private void addDimensions() {
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
        	switch (msg.what) {
        	case HTTPRequest.responseOutputs.FAILED_ERROR_ON_SUPPLIED_URL:
        		outerClassLocalObj.responseStr = "Supplied value was not valid";
        		break;
        	case HTTPRequest.responseOutputs.FAILED_QUERY_FROM_INTERNET:
        		outerClassLocalObj.responseStr = "No answer from internet (connection or server down)";
        		break;
        	case HTTPRequest.responseOutputs.FAILED_GETTING_VALID_RESPONSE_FROM_QUERY:
        		outerClassLocalObj.responseStr = "Query return was empty";
        		break;
        	case HTTPRequest.responseOutputs.FAILED_PROCESSING_RETURNED_OBJECT:
        		outerClassLocalObj.responseStr = "Query was invalid (not compatible with expected result)";
        		break;
        	case HTTPRequest.responseOutputs.SUCCESS:
        		outerClassLocalObj.responseStr = "Retorno COM resultado"; 
        		outerClassLocalObj.newRevDims = (DimensionsList) msg.getData().getSerializable("response");
        		break;
        	}
        	if(outerClassLocalObj.dialog != null && outerClassLocalObj.dialog.isShowing())
        		outerClassLocalObj.dialog.dismiss();
        	outerClassLocalObj.showReview();
        }
    }
}
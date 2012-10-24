package pt.continente.review.common;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import pt.continente.review.R;
import pt.continente.review.getpictures.PhotosManagementActivity;
import pt.continente.review.tables.ArticlesTable;
import pt.continente.review.tables.DimensionsTable;
import pt.continente.review.tables.ReviewDimensionsTable;
import pt.continente.review.tables.ReviewsTable;
import pt.continente.review.tables.SQLiteHelper;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class ReviewActivity extends Activity {
	private static final String TAG = "CntRev - ReviewActivity";
	private Article article = null; 
	private Review review = null; 
	private List<Dimension> dimensions = null;
	private long revId = -1; 
	Bitmap productBitmap = null;
	URL url = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Common.log(5, TAG, "onCreate: started");
		setContentView(R.layout.activity_review);
		
		TextView t = (TextView) findViewById(R.id.articleName);

		revId = (long) getIntent().getLongExtra("revId", -1);
		article = (Article) getIntent().getSerializableExtra("Article");
		if (revId != -1) {
			int result = getDataFromRevId();
			if (result < 0) {
				Toast.makeText(this, "Error getting information from DB; cannot continue", Toast.LENGTH_LONG).show();
			}
		} else if (article != null) {
			t.setText(article.getName());
			Common.log(5, TAG, "onCreate: will add review");
			if(!addNewReview()) {
				Common.log(1, TAG, "onCreate: ERROR adding data to tables (result was '" + revId + "'");
			}
			Common.log(5, TAG, "onCreate: will set bitmap");
			if (productBitmap == null) {
				try {
					ImageView i = (ImageView) findViewById(R.id.articleIcon);
					url = new URL(HTTPGateway.imagePrefix + article.getImageURL());
					InputStream is = (InputStream) url.getContent();
					productBitmap = BitmapFactory.decodeStream(is);
					i.setImageBitmap(productBitmap);

				} catch (Exception e) {
					Common.log(1, TAG, "onCreate: Erro no carregamento da imagem do artigo no link " + HTTPGateway.imagePrefix + article.getImageURL() + "\nErro e:" + e.getMessage());
				}
			}
		} else {
			Common.log(1, TAG, "onCreate: ERROR activity started with unexpected inputs");
		}
		
		Common.log(5, TAG, "onCreate: will draw dimensions");
		if (dimensions != null)
			addDimensions();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_article, menu);
		return true;
	}
	
	private int getDataFromRevId() {
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
	
	private boolean addNewReview() {
		Common.log(5, TAG, "getDataFromRevId: started");

    	SQLiteHelper dbHelper = new SQLiteHelper(this);
    	
    	List<Dimension> newRevDims = new HTTPGateway().getDimensions(article.getId());
    	
    	if(newRevDims == null) {
    		Common.log(1, TAG, "addNewReview: could not retrieve dimensions from Host");
    		return false;
    	}
		Common.log(5, TAG, "addNewReview: retrieved '" + newRevDims.size() + "' dimensions for Article with Id '" + article.getId() + "'");
    	dimensions = newRevDims;
		
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
    	long revTmpId;
    	revTmpId = revTab.findItem(article.getId());
    	if (revTmpId > 0) {
			Common.log(5, TAG, "addNewReview: review already exists, will recover");
			revId = revTmpId;
			review = revTab.getItem(revId);
    	} else {
			Common.log(5, TAG, "addNewReview: review is new - will add");
	    	Review revTmp;
	    	revTmp = new Review(-1, Common.revStates.WORK_IN_PROGRESS, article.getId(), null);
	    	revId = revTab.addItem(revTmp);
	        Common.log(3, TAG, "addNewReview: created new review with ID '" + revTmpId + "'");
	        
    	}
    	revTab.close();
		Common.log(5, TAG, "addNewReview: will exit");
    	if (revId < 0)
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
		
		for (Dimension dim : dimensions) {
			LinearLayout newLinLay = new LinearLayout(this);
			newLinLay.setOrientation(LinearLayout.VERTICAL);
			
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
			newLinLayClass.addView(newMin);
			
			//TODO definir weight dos textviews como 1
			
			TextView newMed = new TextView(this);
			if(dim.getMed() != null && dim.getMed() != "")
				newMed.setText(dim.getMed());
			else
				newMed.setText("");
			newMed.setGravity(Gravity.CENTER_HORIZONTAL);
			newLinLayClass.addView(newMed);
			
			TextView newMax = new TextView(this);
			newMax.setText(dim.getMax());
			newMax.setGravity(Gravity.RIGHT);
			newLinLayClass.addView(newMax);
			

			newLinLay.addView(newLabel);
			newLinLay.addView(newBar);
			newLinLay.addView(newLinLayClass);
			
			linLay.addView(newLinLay);
		}
	}
	
	public void reviewPhotos(View view) {
    	Intent intent = new Intent(this, PhotosManagementActivity.class);
    	intent.putExtra("revId", revId);
    	startActivity(intent);
	}

	public void reviewComment(View view) {
		Toast.makeText(this, "Review management not yet implemented", Toast.LENGTH_SHORT).show();
	}
}
package pt.continente.review.common;

import java.io.InputStream;
import java.net.URL;

import pt.continente.review.R;
import pt.continente.review.getpictures.PhotosManagementActivity;
import pt.continente.review.tables.ArticlesTable;
import pt.continente.review.tables.ReviewsTable;
import pt.continente.review.tables.SQLiteHelper;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ReviewActivity extends Activity {
	private static final String TAG = "CntRev - ArticleActivity";
	private Article article = null; 
	private long revId = -1; 
	Bitmap productBitmap = null;
	URL url = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_article);
		
		TextView t = (TextView) findViewById(R.id.articleName);

		article = (Article) getIntent().getSerializableExtra("Article");
		t.setText(article.getName());
		
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
		
		
		/*
		 * THIS SECTION ADDS THE ARTICLE TO TABLES AND GENERATES A NEW REVIEW
		 */
		revId = addNewReview();
		
		if(revId < 0) {
			Common.log(1, TAG, "onCreate: error adding data to tables (result was '" + revId + "'");
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_article, menu);
		return true;
	}
	
	private long addNewReview() {
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
			return -1;
		}
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
			return -1;
		}
    	
    	Review revTmp;
    	long revTmpId;
    	revTmp = new Review(-1, Common.revStates.WORK_IN_PROGRESS, article.getId(), null);
    	revTmpId = revTab.addItem(revTmp);
        Common.log(3, TAG, "addNewReview: created new review with ID '" + revTmpId + "'");
    	revTab.close();
    	
    	return revTmpId;
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
package pt.continente.review;

import java.net.URL;

import pt.continente.review.common.Article;
import pt.continente.review.common.Common;
import pt.continente.review.common.HTTPGetImage;
import pt.continente.review.common.Preferences;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bugsense.trace.BugSenseHandler;

public class ArticleActivity extends Activity {
	private static final String TAG = "CntRev - ArticleActivity";
	private static Article article = null;
	private static TextView articleNameTextView;
	private static Context context;
	private static Resources resources;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		BugSenseHandler.initAndStartSession(this, Common.bugSenseAppKey);
		Common.log(5, TAG, "onCreate: started");
		setContentView(R.layout.activity_article);
		context = this;
		resources = getResources();
		
		articleNameTextView = (TextView) findViewById(R.id.articleName);
		article = (Article) getIntent().getSerializableExtra("Article");
		
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

		if (article == null) {
			finish();
			Common.longToast(this, resources.getString(R.string.toast_articleErrorRetrieving) + "\n" + resources.getString(R.string.toast_generalCannotContinue));
			return;
		}
		
		TextView t = (TextView) findViewById(R.id.articleName);
		t.setText(article.getName());

	
		Bitmap articleBitmap = article.getImage();
		if (articleBitmap == null) {
			try {
				URL url = new URL(Common.httpVariables.IMAGE_PREFIX + article.getImageURL());
				(new HTTPGetImage(imageThreadHandler, url)).start();
			} catch (Exception e) {
				Common.log(1, TAG, "Erro no carregamento da imagem do artigo no link " + Common.httpVariables.IMAGE_PREFIX + article.getImageURL() + "\nErro e:" + e.getMessage());
				e.printStackTrace();
			}
		} else {
			articleNameTextView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(resources, articleBitmap), null, null, null);
		}

		Common.log(5, TAG, "onResume: finished");
	}

	public void startReview(View view)
	{
		Intent intent = new Intent(this, ReviewActivity.class);
		intent.putExtra("ArticleImage", article.getImage());
		article.setImage(null);
		intent.putExtra("Article", article);
		startActivity(intent);
		finish();
	}
	
	public static Handler imageThreadHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			Common.log(5, TAG, "imageThreadHandler: started");
			switch (msg.what) {
			case HTTPGetImage.responseOutputs.FAILED_GETTING_CONTENT:
				Common.log(1, TAG, "imageThreadHandler: ERROR getting response from the internet");
				Common.longToast(context, resources.getString(R.string.toast_imageErrorFetching));
				break;
			case HTTPGetImage.responseOutputs.FAILED_CONVERTING_RESPONSE:
				Common.log(1, TAG, "imageThreadHandler: ERROR converting internet response into image");
				Common.longToast(context, resources.getString(R.string.toast_imageErrorFetching));
				break;
			case HTTPGetImage.responseOutputs.SUCCESS:
				Bitmap articleBitmap = (Bitmap) msg.obj;
				if(articleBitmap != null) {
					articleNameTextView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(resources, articleBitmap), null, null, null);
					article.setImage(articleBitmap);
				} else {
					Common.log(1, TAG, "imageThreadHandler: ERROR retrieved image but was null");
					Common.longToast(context, resources.getString(R.string.toast_imageErrorFetching));
				}
				break;
			}
			Common.log(5, TAG, "imageThreadHandler: finished");
		};
	};
}

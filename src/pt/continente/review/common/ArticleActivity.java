package pt.continente.review.common;

import java.net.URL;

import pt.continente.review.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ArticleActivity extends Activity {
	private static final String TAG = "CntRev - ArticleActivity";
	private Article article = null; 
	private static ImageView imageView;
	private static Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Common.log(5, TAG, "onCreate: started");
		setContentView(R.layout.activity_article);
		context = this;
		
		TextView t = (TextView) findViewById(R.id.articleName);
		imageView = (ImageView) findViewById(R.id.articleIcon);
		
		article = (Article) getIntent().getSerializableExtra("Article");
		t.setText(article.getName());
		
		if (article == null) {
			finish();
			Common.longToast(this, "Error retrieving the Article; cannot continue");
			return;
		}
		
		Bitmap articleImage = article.getImage();
		if (articleImage == null) {
			try {
				URL url = new URL(Common.httpVariables.IMAGE_PREFIX + article.getImageURL());
				HTTPGetImage imageThread = new HTTPGetImage(imageThreadHandler, url);
				imageThread.start();

			} catch (Exception e) {
				Common.log(1, TAG, "Erro no carregamento da imagem do artigo no link " + Common.httpVariables.IMAGE_PREFIX + article.getImageURL() + "\nErro e:" + e.getMessage());
				e.printStackTrace();
			}
		} else {
			imageView.setImageBitmap(articleImage);
		}
		Common.log(7, TAG, "onCreate: finished");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_article, menu);
		return true;
	}
	
	public void startReview(View view)
	{
		Intent intent = new Intent(this, ReviewActivity.class);
		intent.putExtra("Article", article);
		startActivity(intent);
	}
	
	public static Handler imageThreadHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			Common.log(5, TAG, "imageThreadHandler: started");
			switch (msg.what) {
			case HTTPGetImage.responseOutputs.FAILED_GETTING_CONTENT:
				Common.longToast(context, "Error getting response from the internet");
				break;
			case HTTPGetImage.responseOutputs.FAILED_CONVERTING_RESPONSE:
				Common.longToast(context, "Error converting internet response into image");
				break;
			case HTTPGetImage.responseOutputs.SUCCESS:
				Common.longToast(context, "Successfuly retrieved image");
				Bitmap articleBitmap = (Bitmap) msg.getData().getParcelable("response");
				imageView.setImageBitmap(articleBitmap);
				break;
			}
			Common.log(5, TAG, "imageThreadHandler: finished");
		};
	};
}

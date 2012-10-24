package pt.continente.review.common;

import java.io.InputStream;
import java.net.URL;

import pt.continente.review.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ArticleActivity extends Activity {
	private static final String TAG = "CntRev - ArticleActivity";
	private Article article = null; 
	private Bitmap productBitmap = null;
	private URL url = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Common.log(5, TAG, "onCreate: started");
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
				Common.log(1, TAG, "Erro no carregamento da imagem do artigo no link " + HTTPGateway.imagePrefix + article.getImageURL() + "\nErro e:" + e.getMessage());
			}
		}

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
	
}

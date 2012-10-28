package pt.continente.review.getpictures;

import pt.continente.review.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
 
public class FullImageActivity extends Activity {
	
	private long imgId;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);
 
        // get intent data
        Intent i = getIntent();
 
        // Selected image id
        Bitmap image = i.getExtras().getParcelable("img");
        imgId = i.getExtras().getLong("imgId");
        ImageView imageView = (ImageView) findViewById(R.id.full_image_view);
        imageView.setImageBitmap(image);
    }
 
    public void apagar(View view) {
    	voltarAtividadeAnterior(RESULT_CANCELED);
    }

    public void voltar(View view) {
    	voltarAtividadeAnterior(RESULT_OK);
    }
    
    @Override
	public void onBackPressed() {
    	voltarAtividadeAnterior(RESULT_OK);
	}

	private void voltarAtividadeAnterior(int resultado) {
    	Intent returnIntent = new Intent();
    	returnIntent.putExtra("imgId", imgId);
    	setResult(resultado, returnIntent);        
    	finish();
    }
    
}
package pt.continente.review;

import java.util.Random;

import pt.continente.review.common.Article;
import pt.continente.review.common.ArticleActivity;
import pt.continente.review.common.Common;
import pt.continente.review.common.Dimension;
import pt.continente.review.common.HTTPRequest;
import pt.continente.review.common.IntentIntegrator;
import pt.continente.review.common.IntentResult;
import pt.continente.review.common.Review;
import pt.continente.review.tables.ArticlesTable;
import pt.continente.review.tables.DimensionsTable;
import pt.continente.review.tables.ReviewDimensionsTable;
import pt.continente.review.tables.ReviewImagesTable;
import pt.continente.review.tables.ReviewsTable;
import pt.continente.review.tables.SQLiteHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainMenuActivity extends Activity { 

	private static final String TAG = "CntRev - MainMenuActivity";
	private static ProgressDialog dialog;
	private static Article scannedArticle;
	private String responseStr = "ORIGINAL STATE";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main_menu, menu);
		return true;
	}

	public void reviewHistory(View view) {
		Common.log(5, TAG, "Review History Button Pressed");
    	Intent intent = new Intent(this, ReviewsListActivity.class);
    	startActivity(intent);
	}

	public void searchProduct(View view) {
		Common.log(5, TAG, "Product Search Button Pressed");
		Toast.makeText(getApplicationContext(),
				"Pesquisa de artigos ainda não foi implementada.",
				Toast.LENGTH_SHORT).show();
	}

	public void insertEAN(View view) {
		Common.log(5, TAG, "Manual EAN insert Button Pressed");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Procura produto");
		alert.setMessage("Introduzir EAN do produto");

		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input.getText().toString();
					Common.log(5, TAG, "searchProduct: capturou o input '" + value + "'");
					launchArticleFromEAN(value);
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
	}

	public void startScanner(View view) {
		IntentIntegrator.initiateScan(this);
	} 
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_settings:
	        	Common.log(5, TAG,
	    				"Settings menu click");
	    		Toast.makeText(getApplicationContext(),
	    				"Settings",
	    				Toast.LENGTH_SHORT).show();
	    		return true;  
	        case R.id.menu_debug:
	        	Common.log(5, TAG,
	    				"Development tools menu click");
	    		Toast.makeText(getApplicationContext(),
	    				"Isto ainda há de me dar jeito, nem que seja para definir o IP do servidor",
	    				Toast.LENGTH_SHORT).show();
	    		Intent intent = new Intent(this, DevelopmentTools.class);
				startActivity(intent);
	    		return true;
	        case R.id.menu_clearDB:
	        	limparBD();
	        	return true;
	        case R.id.menu_createTestData:
	    		Common.log(5, TAG, "onOptionsItemSelected: create test articles");
	        	criarArtigosTeste();
	    		Common.log(5, TAG, "onOptionsItemSelected: create test reviews");
	        	criarReviewsTeste();
	    		Common.log(5, TAG, "onOptionsItemSelected: create test dimensions");
	        	criarDimensionsTeste();
	        	return true;
	        case R.id.menu_getSampleDimensions:
	        	Common.shortToast(this, "Not implemented");
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Common.log(3, TAG, "O scanner retornou algo");
		switch (requestCode) {
		case IntentIntegrator.REQUEST_CODE: {
			if (resultCode != RESULT_CANCELED) {
				IntentResult scanResult = IntentIntegrator.parseActivityResult(
						requestCode, resultCode, data);
				if (scanResult != null) {
					String ean = scanResult.getContents();
					Common.log(5, TAG, "Resultado da actividade de leitura do EAN:" + ean);
					launchArticleFromEAN(ean);
				}
			}
			break;
		}
		}
	}

	public void launchArticleFromEAN(String ean) {
		Common.log(5, TAG, "launchArticleFromEAN: started");

		String url = "http://" + Common.httpVariables.SERVER_IP + "/ContinenteReview/article.php?ean=" + ean;
		Common.log(5, TAG, "launchArticleFromEAN: will atempt to launch service to get content from url '" + url + "'");
		
		scannedArticle = null;
		HTTPRequest myHttpThread = new HTTPRequest(httpThreadHandler, url, HTTPRequest.requestTypes.GET_ARTICLE);
		myHttpThread.start();
		dialog = ProgressDialog.show(this, "A ober informação", "a consultar...");
		
		Common.log(5, TAG, "launchArticleFromEAN: finished");
	}
	
	private void launchArticleActivity() {
		if (scannedArticle != null) {
			Common.log(3, TAG, "Vou arrancar um novo article activity");
			Intent intent = new Intent(this, ArticleActivity.class);
			Common.log(3, TAG, "Acabei de criar o Intent");
			intent.putExtra("Article", scannedArticle);
			startActivity(intent);
			Common.log(3, TAG, "Arranquei a Activity Article Activity");
		} else {
			Common.log(5, TAG, "launchArticleFromEAN: null article received");
			Common.longToast(this, responseStr);
		}
	}

    public void criarArtigosTeste() {
    	SQLiteHelper dbHelper = new SQLiteHelper(this);
    	ArticlesTable artTab;
    	try {
			artTab = new ArticlesTable(dbHelper);
			artTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "criarArtigosTeste: could not open the table - " + e.getMessage());
			return;
		}

    	Article tmpArt;
    	tmpArt = new Article(1, "Artigo 1", "Descrição do Artigo 1", "1234567890001", 1.234, "http://www.continente.pt/Images/media/Products/Lar/01/07/04/03/2144139_lar.jpg", null, 1, 1, 1, 1);
    	artTab.addItem(tmpArt);
        Common.log(3, TAG, "criarArtigosTeste: created '" + tmpArt.getName() + "'");
    	tmpArt = new Article(2, "Artigo 2", "Descrição do Artigo 2", "1234567890002", 2.234, "http://www.continente.pt/Images/media/Products/Lar/16/03/02/08/4164414_lar.jpg", null, 2, 2, 2, 2);
    	artTab.addItem(tmpArt);
        Common.log(3, TAG, "criarArtigosTeste: created '" + tmpArt.getName() + "'");
    	tmpArt = new Article(3, "Artigo 3", "Descrição do Artigo 3", "1234567890003", 3.234, "http://www.continente.pt/Images/media/Products/Lar/01/11/06/07/4138123_lar.jpg", null, 3, 3, 3, 3);
    	artTab.addItem(tmpArt);
        Common.log(3, TAG, "criarArtigosTeste: created '" + tmpArt.getName() + "'");
    	tmpArt = new Article(4, "Artigo 4", "Descrição do Artigo 4", "1234567890004", 4.234, "http://www.continente.pt/Images/media/Products/Lar/01/11/06/07/4138123_lar.jpg", null, 4, 4, 4, 4);
    	artTab.addItem(tmpArt);
        Common.log(3, TAG, "criarArtigosTeste: created '" + tmpArt.getName() + "'");
    	
        artTab.close();
    }

    public void criarReviewsTeste() {
    	SQLiteHelper dbHelper = new SQLiteHelper(this);
    	ReviewsTable revTab;
    	try {
			revTab = new ReviewsTable(dbHelper);
			revTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "criarArtigosTeste: could not open the table - " + e.getMessage());
			return;
		}
    	
    	Review revTmp;
    	long revTmpId;
    	
    	revTmp = new Review(-1, Common.revStates.PENDING_USER, 1, null);
    	revTmpId = revTab.addItem(revTmp);
    	criarReviewDimensionsTeste(revTmpId);
    	
    	revTmp = new Review(-1, Common.revStates.COMPLETED, 2, "Comment for Art 2");
    	revTmpId = revTab.addItem(revTmp);
    	criarReviewDimensionsTeste(revTmpId);

    	revTmp = new Review(-1, Common.revStates.COMPLETED, 3, null);
    	revTmpId = revTab.addItem(revTmp);
    	criarReviewDimensionsTeste(revTmpId);

    	revTmp = new Review(-1, Common.revStates.COMPLETED, 4, null);
    	revTmpId = revTab.addItem(revTmp);
    	criarReviewDimensionsTeste(revTmpId);
    	
    	revTab.close();
    }

    public void criarDimensionsTeste() {
    	SQLiteHelper dbHelper = new SQLiteHelper(this);
    	DimensionsTable dimTab;
    	try {
    		dimTab = new DimensionsTable(dbHelper);
    		dimTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "criarDimensionsTeste: could not open the table - " + e.getMessage());
			return;
		}
    	
    	Dimension tmpDim;
    	tmpDim = new Dimension(26, "Dim26", "Dimension 26", "Min26", null, "Max26");
    	dimTab.addItem(tmpDim);
    	tmpDim = new Dimension(27, "Dim27", "Dimension 27", "Min27", "Med27", "Max27");
    	dimTab.addItem(tmpDim);
    	tmpDim = new Dimension(28, "Dim28", "Dimension 28", "Min28", null, "Max28");
    	dimTab.addItem(tmpDim);
    	tmpDim = new Dimension(29, "Dim29", "Dimension 29", "Min29", "Med29", "Max29");
    	dimTab.addItem(tmpDim);
    	
    	dimTab.close();
    }

    public void criarReviewDimensionsTeste(long revId) {
		Common.log(5, TAG, "criarReviewDimensionsTeste: started");
    	SQLiteHelper dbHelper = new SQLiteHelper(this);
    	ReviewDimensionsTable revDimTab;
    	try {
    		revDimTab = new ReviewDimensionsTable(dbHelper);
    		revDimTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "criarReviewDimensionsTeste: could not open the table - " + e.getMessage());
			return;
		}
    	
    	Random rand = new Random();
    	if(rand.nextBoolean())
    		revDimTab.addItem(revId, 26);
    	if(rand.nextBoolean())
    		revDimTab.addItem(revId, 27);
    	if(rand.nextBoolean())
    		revDimTab.addItem(revId, 28);
    	if(rand.nextBoolean())
    		revDimTab.addItem(revId, 29);
    	
    	revDimTab.close();
    }
    
    
    public void limparBD() {
    	SQLiteHelper dbHelper = new SQLiteHelper(this);
    	ArticlesTable artTab;
    	ReviewsTable revTab;
    	ReviewImagesTable revImgTab;
    	DimensionsTable dimTab;
    	ReviewDimensionsTable revDimTab;
    	try {
			artTab = new ArticlesTable(dbHelper);
			artTab.open();
			revTab = new ReviewsTable(dbHelper);
			revTab.open();
			revImgTab = new ReviewImagesTable(dbHelper);
			revImgTab.open();
			dimTab = new DimensionsTable(dbHelper);
			dimTab.open();
			revDimTab = new ReviewDimensionsTable(dbHelper);
			revDimTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "criarArtigosTeste: could not open the table - " + e.getMessage());
			return;
		}
    	artTab.deleteAllItems();
    	revTab.deleteAllItems();
    	revImgTab.deleteAllItems();
    	dimTab.deleteAllItems();
    	revDimTab.deleteAllItems();
    	
    	artTab.close();
    	revTab.close();
    	revImgTab.close();
    	dimTab.close();
    	revDimTab.close();
    }

	public Handler httpThreadHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
        	switch (msg.what) {
        	case HTTPRequest.responseOutputs.FAILED_ERROR_ON_SUPPLIED_URL:
        		responseStr = "Supplied value was not valid";
        		break;
        	case HTTPRequest.responseOutputs.FAILED_QUERY_FROM_INTERNET:
        		responseStr = "No answer from internet (connection or server down)";
        		break;
        	case HTTPRequest.responseOutputs.FAILED_GETTING_VALID_RESPONSE_FROM_QUERY:
        		responseStr = "Query return was empty";
        		break;
        	case HTTPRequest.responseOutputs.FAILED_PROCESSING_RETURNED_OBJECT:
        		responseStr = "Query was invalid (not compatible with expected result)";
        		break;
        	case HTTPRequest.responseOutputs.SUCCESS:
        		responseStr = "Retorno COM resultado"; 
        		scannedArticle = (Article) msg.getData().getSerializable("response");
        		break;
        	}
        	if(dialog != null && dialog.isShowing())
        		dialog.dismiss();
        	launchArticleActivity();
        };
    };

}

package pt.continente.review;

import java.lang.ref.WeakReference;

import pt.continente.review.common.Article;
import pt.continente.review.common.Common;
import pt.continente.review.common.HTTPRequest;
import pt.continente.review.common.Preferences;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;

public class MainMenuActivity extends Activity { 
	private static final String TAG = "CntRev - MainMenuActivity";

	private ProgressDialog dialog;
	private Article scannedArticle;
	private String responseStr = "ORIGINAL STATE";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		BugSenseHandler.initAndStartSession(this, "6804ac88");
		setContentView(R.layout.layout_home_screen);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.general_menu, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		int currentVersion = android.os.Build.VERSION.SDK_INT;
		if(currentVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			TextView textView = (TextView) findViewById(R.id.titleOldHeader);
			textView.setVisibility(TextView.GONE);
		}
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
		input.setRawInputType(Configuration.KEYBOARD_12KEY);
		alert.setView(input);
		

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input.getText().toString();
					Common.log(5, TAG, "searchProduct: capturou o input '" + value + "'");
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY,0);
					launchArticleFromEAN(value); 
				}
			}
		);

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY,0);
					// Canceled.
				}
			}
		);

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
//		imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);

		alert.show();
		
	}

	public void giveFeedback(View view) {
		Common.sendAppReviewByEmail(this);
		return;
	}
	
	public void startScanner(View view) {
		IntentIntegrator.initiateScan(this);
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

		String url = Common.httpVariables.ARTICLE_PREFIX + ean;
		Common.log(5, TAG, "launchArticleFromEAN: will atempt to launch service to get content from url '" + url + "'");
		
		scannedArticle = null;
		(new HTTPRequest(this, new httpRequestHandler(this), url, HTTPRequest.requestTypes.GET_ARTICLE)).start();
		dialog = ProgressDialog.show(this, "A obter informação", "a consultar...");
		
		Common.log(5, TAG, "launchArticleFromEAN: finished");
	}
	
	private void launchArticleActivity() {
		if (scannedArticle != null) {
			Common.log(7, TAG, "Vou arrancar um novo article activity");
			Intent intent = new Intent(this, ArticleActivity.class);
			Common.log(7, TAG, "Acabei de criar o Intent");
			intent.putExtra("Article", scannedArticle);
			startActivity(intent);
			Common.log(7, TAG, "Arranquei a Activity Article Activity");
		} else {
			Common.log(5, TAG, "launchArticleFromEAN: null article received");
			Common.longToast(this, responseStr);
		}
	}
	
	static class httpRequestHandler extends Handler {
		WeakReference<MainMenuActivity> outerClass;
		
		httpRequestHandler(MainMenuActivity outerClass) {
			this.outerClass = new WeakReference<MainMenuActivity>(outerClass);
		}
		
		@Override
		public void handleMessage(android.os.Message msg) {
			MainMenuActivity outerClassLocalObj = outerClass.get();
			String errorMsg = null;
			switch (msg.what) {
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
        		errorMsg = "Could not find this article (" + serverErrorMsg + ")";
        		break;
        	case HTTPRequest.responseOutputs.SUCCESS:
        		errorMsg = "Retorno COM resultado"; 
        		outerClassLocalObj.scannedArticle = (Article) msg.obj;
        		break;
    		default:
        		errorMsg = "Undefined error when retrieving data from the internet";
        		break;
        	}
        	if(outerClassLocalObj.dialog != null && outerClassLocalObj.dialog.isShowing())
        		outerClassLocalObj.dialog.dismiss();
        	outerClassLocalObj.responseStr = errorMsg;
        	outerClassLocalObj.launchArticleActivity();
        }
    }

}

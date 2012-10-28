package pt.continente.review.common;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class HTTPRequest extends Thread {
	private static final String TAG = "CntRev - HTTPRequest";
	
	private String urlBeingSought;
	private HttpResponse response;
	private Handler parentHandler;

	public HTTPRequest(Handler parentHandler, String url) {
		response = null;
		urlBeingSought = url;
		this.parentHandler = parentHandler;
	}

	@Override
	public void run() {
		super.run();
		Common.log(5, TAG, "run: started");
		
		DefaultHttpClient client = new DefaultHttpClient();
		HttpContext localContext = new BasicHttpContext();
		HttpGet httpGet = new HttpGet(urlBeingSought);
		response = null;
		
		try {
			response = client.execute(httpGet, localContext);
		} catch (ClientProtocolException e) {
			Common.log(3, TAG, "run: Erro ao obter informação da internet (ClientProtocolException) - " + e.getMessage());
		} catch (IOException e) {
			Common.log(3, TAG, "run: Erro ao obter informação da internet (IOException) - " + e.getMessage());
		} catch (Exception e) {
			Common.log(3, TAG, "run: Erro ao obter informação da internet (UndefinedException) - " + e.getMessage());
			e.printStackTrace();
		}
		
		Message messageToParent = new Message();

		if (response == null) {
	        messageToParent.what = Common.httpVariables.SUCCESS_FALSE;
		} else {
			Document newDocument = null;
			Article newArticle = null;
			try {
				HttpEntity entity = response.getEntity();
				InputStream instream = entity.getContent();
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setIgnoringElementContentWhitespace(true);
				DocumentBuilder builder = dbf.newDocumentBuilder();
				newDocument = builder.parse(instream);
			} catch (Exception e) {
				e.printStackTrace();
			}
			newArticle = HTTPGateway.getProductFromDoc(newDocument);
			
			messageToParent.what = Common.httpVariables.SUCCESS_TRUE;
	        Bundle messageData = new Bundle();
	        messageData.putSerializable("response", newArticle);
	        messageToParent.setData(messageData);
		}
//		try {
//			Thread.sleep(5000);
//        } catch (InterruptedException e) {
//        	e.printStackTrace();
//        }
        parentHandler.sendMessage(messageToParent);
		Common.log(5, TAG, "run: finished");
	}
}

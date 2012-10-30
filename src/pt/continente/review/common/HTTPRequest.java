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
	private int requestType;
	
	public static class requestTypes {
		public static final int GET_ARTICLE = 1;
		public static final int GET_DIMENSIONS = 2;
	}
	
	public static class responseOutputs {
		public final static int SUCCESS = 10;
		public final static int FAILED_ERROR_ON_SUPPLIED_URL = 11;
		public final static int FAILED_QUERY_FROM_INTERNET = 12;
		public final static int FAILED_GETTING_VALID_RESPONSE_FROM_QUERY = 13;
		public final static int FAILED_PROCESSING_RETURNED_OBJECT = 14;
	}

	public HTTPRequest(Handler parentHandler, String url, int requestType) {
		response = null;
		urlBeingSought = url;
		this.parentHandler = parentHandler;
		this.requestType = requestType;
	}

	@Override
	public void run() {
		super.run();
		Common.log(5, TAG, "run: started");

		// simulates delay in fetch
		boolean simulateDealy = true;
		try {
			Thread.sleep(simulateDealy ? 2000 : 1);
        } catch (InterruptedException e) {
        	e.printStackTrace();
        }

		Message messageToParent = new Message();
		messageToParent.what = 0;
		
		DefaultHttpClient client = new DefaultHttpClient();
		HttpContext localContext = new BasicHttpContext();
		response = null;
		HttpGet httpGet = null;
		
		try {
			httpGet = new HttpGet(urlBeingSought);
		} catch (IllegalArgumentException e) {
			Common.log(1, TAG, "run: ERROR in supplied url - " + e.getMessage());
			messageToParent.what = responseOutputs.FAILED_ERROR_ON_SUPPLIED_URL;
		}
		
		if (messageToParent.what != 0) {
			parentHandler.sendMessage(messageToParent);
			return;
		}
		

		try {
			response = client.execute(httpGet, localContext);
		} catch (ClientProtocolException e) {
			Common.log(1, TAG, "run: ERROR obtaining response to internet query (ClientProtocolException) - " + e.getMessage());
			messageToParent.what = responseOutputs.FAILED_QUERY_FROM_INTERNET;
		} catch (IOException e) {
			Common.log(1, TAG, "run: ERROR obtaining response to internet query (IOException) - " + e.getMessage());
			messageToParent.what = responseOutputs.FAILED_QUERY_FROM_INTERNET;
		} catch (Exception e) {
			Common.log(1, TAG, "run: ERROR obtaining response to internet query (UndefinedException) - " + e.getMessage());
			e.printStackTrace();
			messageToParent.what = responseOutputs.FAILED_QUERY_FROM_INTERNET;
		}

		if (messageToParent.what != 0) {
			parentHandler.sendMessage(messageToParent);
			return;
		}
		
		
		if (response == null) {
			Common.log(3, TAG, "run: got empty response from query");
			messageToParent.what = responseOutputs.FAILED_GETTING_VALID_RESPONSE_FROM_QUERY;
			parentHandler.sendMessage(messageToParent);
			return;
		} else {
			Document newDocument = null;
			try {
				HttpEntity entity = response.getEntity();
				InputStream instream = entity.getContent();
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setIgnoringElementContentWhitespace(true);
				DocumentBuilder builder = dbf.newDocumentBuilder();
				newDocument = builder.parse(instream);
			} catch (Exception e) {
				Common.log(1, TAG, "run: ERROR processing the returned object - " + e.getMessage());
				messageToParent.what = responseOutputs.FAILED_PROCESSING_RETURNED_OBJECT;
				parentHandler.sendMessage(messageToParent);
				return;
			}
			
	        Bundle messageData = new Bundle();
			
			switch (requestType) {
			case requestTypes.GET_ARTICLE:
				Article newArticle = HTTPResponseProcessor.getProductFromDoc(newDocument);
		        messageData.putSerializable("response", newArticle);
				break;
			case requestTypes.GET_DIMENSIONS:
				//TODO alterar código para processar dimensions
				DimensionsList newDimList = HTTPResponseProcessor.getDimensionsFromDoc(newDocument);
		        messageData.putSerializable("response", newDimList);
				break;
			}

			messageToParent.what = responseOutputs.SUCCESS;
	        messageToParent.setData(messageData);
			parentHandler.sendMessage(messageToParent);
		}
		Common.log(5, TAG, "run: finished");
	}
}

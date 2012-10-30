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
		boolean simulateDelay = true;
		try {
			Thread.sleep(simulateDelay ? 2000 : 1);
        } catch (InterruptedException e) {
			Common.log(1, TAG, "run: ERROR in applying delay - " + e.getMessage());
			e.printStackTrace();
        }
		Common.log(5, TAG, "run: terminou o delay forçado");

		Message messageToParent = null;
		DefaultHttpClient client = null;
		HttpContext localContext = null;
		try {
			messageToParent = new Message();
			client = new DefaultHttpClient();
			localContext = new BasicHttpContext();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Common.log(5, TAG, "run: criou as variáveis chave");
		
		messageToParent.what = 0;
		response = null;
		HttpGet httpGet = null;
		
		Common.log(5, TAG, "run: vai criar objeto GET");
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
		
		Common.log(5, TAG, "run: vai obter dados da net");
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
		
		
		Common.log(5, TAG, "run: vai processar respostas");
		if (response == null) {
			Common.log(3, TAG, "run: got empty response from query");
			messageToParent.what = responseOutputs.FAILED_GETTING_VALID_RESPONSE_FROM_QUERY;
			parentHandler.sendMessage(messageToParent);
			return;
		} else {
			Common.log(5, TAG, "run: vai obter o documento a partir da resposta");
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
			
			Common.log(5, TAG, "run: criar a mensagem a partir do documento");
	        Bundle messageData = new Bundle();
			
			switch (requestType) {
			case requestTypes.GET_ARTICLE:
				Common.log(5, TAG, "run: vai processar artigo");
				Article newArticle = HTTPResponseProcessor.getProductFromDoc(newDocument);
		        messageData.putSerializable("response", newArticle);
				Common.log(5, TAG, "run: artigo processado");
				break;
			case requestTypes.GET_DIMENSIONS:
				Common.log(5, TAG, "run: vai processar dimensões");
				DimensionsList newDimList = HTTPResponseProcessor.getDimensionsFromDoc(newDocument);
		        messageData.putSerializable("response", newDimList);
				Common.log(5, TAG, "run: dimensões procesadas");
				break;
			}

			Common.log(5, TAG, "run: vai finalizar e enviar mensagem 1");
			messageToParent.what = responseOutputs.SUCCESS;
			Common.log(5, TAG, "run: vai finalizar e enviar mensagem 2");
	        messageToParent.setData(messageData);
			Common.log(5, TAG, "run: vai finalizar e enviar mensagem 3");
			parentHandler.sendMessage(messageToParent);
		}
		Common.log(5, TAG, "run: finished");
	}
}

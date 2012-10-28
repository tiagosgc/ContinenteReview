package pt.continente.review.common;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.os.Handler;

public class HTTPGateway {
	private static final String TAG = "CntRev - HTTPGATEWAY";
	
	
	public Document getXMLDocument(String url) {
		Common.log(5, TAG, "getXMLDocument: Inicio de get da string:" + url);
		
		DocumentBuilder builder = null;
		Document newDocument = null;
		HttpResponse response = null;
		
		try {
			HttpEntity entity = response.getEntity();
			InputStream instream = entity.getContent();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringElementContentWhitespace(true);
			builder = dbf.newDocumentBuilder();
			newDocument = builder.parse(instream);
		} catch (Exception e) {
			Common.log(3, TAG, "Erro no processamento do retorno - " + e.getMessage());
			return null;
		}
		return newDocument;
	}
	
	public Article getProduct(String ean) {
		Common.log(5, TAG, "Inicio de getProduct com ean=" + ean);

		Document document = getXMLDocument("http://" + Common.httpVariables.SERVER_IP + "/ContinenteReview/article.php?ean=" + ean);
		return getProductFromDoc(document);
	}

	public static Article getProductFromDoc(Document document) {

		if (document == null) {
			return null;
		}
		
		Element root;
		NodeList nodeList;

		document.getDocumentElement().normalize();
		root = document.getDocumentElement();
		nodeList = root.getChildNodes();
		
		Node proxNode;
		String id = "-1";
		String name = "";
		String description = "Description";
		String productEan = "";
		String price = "";
		String urlImg = "";
		String prodStructL1 = "";
		String prodStructL2 = "";
		String prodStructL3 = "";
		String prodStructL4 = "";

		for (int i = 0; i < nodeList.getLength(); i++) {
			proxNode = nodeList.item(i);
			if (proxNode.getNodeName().compareTo("id") == 0) {
				id = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("name") == 0) {
				name = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("ean") == 0) {
				productEan = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("price") == 0) {
				price = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("urlImg") == 0) {
				urlImg = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("prodStructL1") == 0) {
				prodStructL1 = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("prodStructL2") == 0) {
				prodStructL2 = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("prodStructL3") == 0) {
				prodStructL3 = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("prodStructL4") == 0) {
				prodStructL4 = proxNode.getTextContent();
			}

		}
		Common.log(3, TAG, "Name(String):" + name);
		Article gettedArticle = new Article(Long.parseLong(id), name,
				description, productEan, Double.parseDouble(price), urlImg,
				null, Integer.parseInt(prodStructL1),
				Integer.parseInt(prodStructL2), Integer.parseInt(prodStructL3),
				Integer.parseInt(prodStructL4));
		Common.log(3, TAG, "" + gettedArticle);
		return gettedArticle;
	}

	public List<Dimension> getDimensions(long articleId) {
		Common.log(5, TAG, "getDimensions: Inicio com article_id = "
				+ articleId);

		Document document = getXMLDocument("http://" + Common.httpVariables.SERVER_IP
				+ "/ContinenteReview/dimensions.php?article_id=" + articleId);

		if (document == null) {
			return null;
		}
		
		Element root;
		NodeList dimensions;
		NodeList dimensionNodes;
		List<Dimension> returnList = new ArrayList<Dimension>();
		document.getDocumentElement().normalize();
		root = document.getDocumentElement();
		dimensions = root.getChildNodes();

		Node proxNode;
		Node proxDimension;
		long id = 0 ;
		String name = "";
		String label = "";
		String min = "";
		String med = "";
		String max = "";

		// str = str.replaceAll("[0-9]", "X");
		for (int i = 0; i < dimensions.getLength(); i++) {
			proxDimension = dimensions.item(i);
			dimensionNodes = proxDimension.getChildNodes();
			for (int j = 0; j < dimensionNodes.getLength(); j++) {
				proxNode = dimensionNodes.item(j);
				if (proxNode.getNodeName().compareTo("id") == 0) {
					id = Long.parseLong(proxNode.getTextContent());
				}
				if (proxNode.getNodeName().compareTo("name") == 0) {
					name = proxNode.getTextContent();
				}
				if (proxNode.getNodeName().compareTo("labelDimension") == 0) {
					label = proxNode.getTextContent();
				}
				if (proxNode.getNodeName().compareTo("labelMin") == 0) {
					min = proxNode.getTextContent();
				}
				if (proxNode.getNodeName().compareTo("labelMed") == 0) {
					med = proxNode.getTextContent();
				}
				if (proxNode.getNodeName().compareTo("labelMax") == 0) {
					max = proxNode.getTextContent();
				}
			}
			Common.log(5, TAG, "" + id + ":" + name + "." + label + "*" + min + ";;" + med + ";;;" + max);
			returnList.add(new Dimension(id,name,label,min,med,max));
		}
		return returnList;

		// Log.d("Tiago", "resposta:" + result);

	}

//	private class httpGetTask extends AsyncTask<String, Integer, List<HttpResponse>> {
//		HttpContext localContext;
//		List<HttpResponse> results;
//		
//		@Override
//		protected void onPreExecute() {
//			try {
//				localContext = new BasicHttpContext();
//    			results = new ArrayList<HttpResponse>();
//			} catch (Exception e) {
//				Common.log(3, TAG, "httpGetTask: onPreExecute: error initiating variables");
//			}
//			super.onPreExecute();
//		}
//
//		protected List<HttpResponse> doInBackground(String... urls) {
//			for (String url : urls) {
//				DefaultHttpClient client = new DefaultHttpClient();
//				HttpGet httpGet = new HttpGet(url);
//				HttpResponse response = null;
//	    		try {
//	    			response = client.execute(httpGet, localContext);
//	    		} catch (ClientProtocolException e) {
//	    			Common.log(3, TAG, "httpGetTask: doInBackground: Erro ao obter informação da internet (ClientProtocolException) - " + e.getMessage());
//	    		} catch (IOException e) {
//	    			Common.log(3, TAG, "httpGetTask: doInBackground: Erro ao obter informação da internet (IOException) - " + e.getMessage());
//	    		} catch (Exception e) {
//	    			Common.log(3, TAG, "httpGetTask: doInBackground: Erro ao obter informação da internet (UndefinedException) - " + e.getMessage());
//	    			e.printStackTrace();
//	    		}
//	    		if(response != null)
//	    			results.add(response);
//	    		//publishProgress((int) ((i / (float) count) * 100));
//	    		if (isCancelled()) break; // Escape early if cancel() is called
//			}
//			return results;
//		}
//
//		protected void onProgressUpdate(Integer... progress) {
//			//setProgressPercent(progress[0]);
//		}
//
//		protected void onPostExecute(List<HttpResponse> result) {
//			responses = results;
//		}
//	}
}

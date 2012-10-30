package pt.continente.review.common;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HTTPResponseProcessor {
	private static final String TAG = "CntRev - HTTPResponseProcessor";
	

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

	
	
	
	public static DimensionsList getDimensionsFromDoc(Document document) {
		Common.log(5, TAG, "getDimensions: started");

		if (document == null) {
			return null;
		}
		
		Element root;
		NodeList dimensions;
		NodeList dimensionNodes;
		DimensionsList returnList = new DimensionsList();
		
		document.getDocumentElement().normalize();
		root = document.getDocumentElement();
		dimensions = root.getChildNodes();
		
		Common.log(5, TAG, "getDimensions: found '" + dimensions.getLength() + "' elements in response");


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
		Common.log(5, TAG, "getDimensions: built an array with '" + returnList.size() + "' elements");
		Common.log(5, TAG, "getDimensions: finished");
		return returnList;
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

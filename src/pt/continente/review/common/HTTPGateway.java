package pt.continente.review.common;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.*;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HTTPGateway {
	private static final String TAG = "CntRev - HTTPGATEWAY";
	static String serverIP = "172.16.0.185";
	static String imagePrefix = "http://www.continente.pt/Images/media/Products/";

	public Article getProduct(String ean) {
		Common.log(5, TAG, "Inicio de getProduct com ean=" + ean);

		DocumentBuilder builder;
		Document document;
		Element root;
		NodeList nodeList;

		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpGet httpGet = new HttpGet("http://" + HTTPGateway.serverIP
					+ "/ContinenteReview/produto.php?ean=" + ean);

			HttpResponse response = httpClient.execute(httpGet, localContext);
			HttpEntity entity = response.getEntity();
			InputStream instream = entity.getContent();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringElementContentWhitespace(true);
			builder = dbf.newDocumentBuilder();
			document = builder.parse(instream);
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
			Article gettedArticle = new Article(Long.parseLong(id), name, description, productEan, Double.parseDouble(price),
					urlImg, null, Integer.parseInt(prodStructL1), Integer.parseInt(prodStructL2), Integer.parseInt(prodStructL3),
					Integer.parseInt(prodStructL4));
			Common.log(3, TAG, "" + gettedArticle);
			return gettedArticle;

		} catch (Exception e) {
			Common.log(3, TAG, "Erro na ligação a \""
					+ HTTPGateway.serverIP
					+ "/ContinenteReview/produto.php?ean=" + ean + "\"" + e);
			e.printStackTrace();
			return null;
		}

		// Log.d("Tiago", "resposta:" + result);

	}

}

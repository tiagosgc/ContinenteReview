package pt.continente.review.common;

public class HTTPResponseException extends Exception {

	String url = "";
	String msg = "";
	public HTTPResponseException(String url) {
		// TODO Auto-generated constructor stub
		this.msg = url;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -2141875753471565057L;
	public void setUrl(String urlBeingSought) {
		// TODO Auto-generated method stub
		this.msg = urlBeingSought;
	}

}

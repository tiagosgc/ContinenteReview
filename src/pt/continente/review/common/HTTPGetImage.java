package pt.continente.review.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

public class HTTPGetImage extends Thread {
	private static final String TAG = "CntRev - HTTPGetImage";
	
	private URL urlBeingSought;
	private Handler parentHandler;

	public static class responseOutputs {
		public final static int SUCCESS = 10;
		public final static int FAILED_GETTING_CONTENT = 11;
		public final static int FAILED_CONVERTING_RESPONSE = 12;
	}

	public HTTPGetImage(Handler parentHandler, URL url) {
		this.parentHandler = parentHandler;
		this.urlBeingSought = url;
	}

	@Override
	public void run() {
		super.run();
		Common.log(5, TAG, "run: started");

		// simulates delay in fetch
		boolean simulateDelay = true;
		try {
			Thread.sleep(simulateDelay ? 1000 : 1);
        } catch (InterruptedException e) {
        	e.printStackTrace();
        }

		Message messageToParent = new Message();
		messageToParent.what = 0;
		
		InputStream is = null;
		try {
			is = (InputStream) urlBeingSought.getContent();
		} catch (IOException e) {
			Common.log(1, TAG, "run: ERROR while gettinf content - " + e.getMessage());
			messageToParent.what = responseOutputs.FAILED_GETTING_CONTENT;
		}
		if (messageToParent.what != 0) {
			parentHandler.sendMessage(messageToParent);
			return;
		}
		
		Bitmap productBitmap = BitmapFactory.decodeStream(is);
		
		if (productBitmap == null) {
			Common.log(1, TAG, "run: ERROR converting response to image");
			messageToParent.what = responseOutputs.FAILED_CONVERTING_RESPONSE;
			parentHandler.sendMessage(messageToParent);
			return;
		}
		
		messageToParent.what = responseOutputs.SUCCESS;
		messageToParent.obj = productBitmap;
//		OLD WAY - Bundle messageData = new Bundle();
//		OLD WAY - messageData.putParcelable("response", (Parcelable) productBitmap);
		parentHandler.sendMessage(messageToParent);
		
		Common.log(5, TAG, "run: finished");
	}
}

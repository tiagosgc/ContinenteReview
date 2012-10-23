package pt.continente.review.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class Common {
	//private static final String TAG = "CntRev - Common";
	private static final int LOG_LEVEL = 5;
	
	public static final class revStates {
		public static final int PENDING_USER = 1;
		public static final int PENDING_SYSTEM = 2;
		public static final int WORK_IN_PROGRESS = 5;
		public static final int COMPLETED = 10;
		public static final int SUBMITED = 15;
	}
	
	public Common() {
		// TODO Auto-generated constructor stub
	}

	public static void log(int debugLevel, String tag, String msg) {
		if (debugLevel <= LOG_LEVEL)
			Log.i(tag, msg);
	}

	public static byte[] imageToBlob(Bitmap image) {
		if(image == null)
			return null;
		byte[] blob = null;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		image.compress(Bitmap.CompressFormat.PNG, 100, stream);
/*		boolean success = image.compress(Bitmap.CompressFormat.PNG, 100, stream);
		Common.log(5, TAG, "imageToBlob: image compression " + (success ? "was" : "WAS NOT") + " a success");
		if (!success) {
			Bitmap cloneImg = Bitmap.createScaledBitmap(image, image.getWidth(), image.getHeight(), false); 
			stream = new ByteArrayOutputStream();
			cloneImg.compress(Bitmap.CompressFormat.PNG, 100, stream);
		}
*/		blob = stream.toByteArray();
		return blob;
	}

	public static Bitmap blobToImage(byte[] blob) {
		if(blob == null)
			return null;
		Bitmap image = null;
    	ByteArrayInputStream imageStream = new ByteArrayInputStream(blob);
    	image = BitmapFactory.decodeStream(imageStream);
		return image;
	}

}

package pt.continente.review;

import java.util.ArrayList;

import pt.continente.review.common.Common;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
 
public class ImageAdapter extends BaseAdapter {
	private static final String TAG = "CntRev - ImageAdapter";
    private Context mContext;
    private ArrayList<Bitmap> mImages;
    private ArrayList<Long> mImageIds;
    
    // Constructor
    public ImageAdapter(Context c){
    	Common.log(5, TAG, "ImageAdapter: entrou");
        mContext = c;
        mImages = new ArrayList<Bitmap>();
        mImageIds = new ArrayList<Long>();
    }

	public boolean addItem(Bitmap newImage, long imgId) {
		if(newImage != null && imgId != -1) {
			if(mImageIds.contains(imgId)) {
		    	Common.log(1, TAG, "addItem: did not create as it would duplicate item with Id '" + imgId + "'");
				return false;
			} else {
				mImages.add(newImage);
				mImageIds.add(imgId);
		    	Common.log(5, TAG, "addItem: successfuly added new element with Id '" + imgId + "'");
				return true;
			}
		}
    	Common.log(1, TAG, "addItem: did not create as at least one of the inputs has wrong value");
		return false;
	}
	
	public void deleteAllItems() {
		mImages.clear();
		mImageIds.clear();
	}

	public boolean deleteItem(long imgId) {
    	Common.log(5, TAG, "deleteItem: entrou");
		int positionToDelete = mImageIds.indexOf(imgId);
		if(positionToDelete != -1) {
			mImages.remove(positionToDelete);
			mImageIds.remove(positionToDelete);
	    	Common.log(5, TAG, "deleteItem: deleted record with Id '" + imgId + "' at position '" + positionToDelete + "'");
	    	return true;
	    } else {
	    	Common.log(1, TAG, "deleteItem: could not find any item with Id '" + imgId + "'");
	    	return false;
    	}
	}

    @Override
    public int getCount() {
    	return mImages.size();
    }
 
    @Override
    public Object getItem(int position) {
    	return mImages.get(position);
    }
 
    public long getImgId(int position) {
    	return mImageIds.get(position);
    }
 
    public ArrayList<Bitmap> getAllItems() {
    	return mImages;
    }
 
    @Override
    public long getItemId(int position) {
        return 0;
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView = new ImageView(mContext);
        imageView.setImageBitmap(mImages.get(position));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new GridView.LayoutParams(Common.pixelsFromDPs(mContext, 100), Common.pixelsFromDPs(mContext, 100)));
        return imageView;
    }
 
}
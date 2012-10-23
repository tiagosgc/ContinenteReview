package pt.continente.review;

import java.util.ArrayList;

import pt.continente.review.common.Common;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ReviewListAdapter extends BaseAdapter {
	private static final String TAG = "CntRev - ReviewListAdapter";
    private Context mContext;
    private ArrayList<Long> revIds;
    private ArrayList<String> revArtNames;

	public ReviewListAdapter(Context c) {
    	Common.log(5, TAG, "ReviewListAdapter: started");
        mContext = c;
        revIds = new ArrayList<Long>();
        revArtNames = new ArrayList<String>();
	}

	public boolean addItem(long revId, String revArtName) {
		if(revId != -1 && revArtName != null) {
			if(revIds.contains(revId)) {
		    	Common.log(1, TAG, "addItem: did not create as it would duplicate item with Id '" + revId + "'");
				return false;
			} else {
				revIds.add(revId);
				revArtNames.add(revArtName);
		    	Common.log(5, TAG, "addItem: successfuly added new element with Id '" + revId + "'");
				return true;
			}
		}
    	Common.log(1, TAG, "addItem: did not create as at least one of the inputs has wrong value");
		return false;
	}
	
	public void deleteAllItems() {
		revIds.clear();
		revArtNames.clear();
	}

	public boolean deleteItem(long revId) {
    	Common.log(5, TAG, "deleteItem: entrou");
		int positionToDelete = revIds.indexOf(revId);
		if(positionToDelete != -1) {
			revIds.remove(positionToDelete);
			revArtNames.remove(positionToDelete);
	    	Common.log(5, TAG, "deleteItem: deleted record with Id '" + revIds + "' at position '" + positionToDelete + "'");
	    	return true;
	    } else {
	    	Common.log(1, TAG, "deleteItem: could not find any item with Id '" + revIds + "'");
	    	return false;
    	}
	}

	@Override
	public int getCount() {
		return revIds.size();
	}

	@Override
	public Object getItem(int position) {
		return revArtNames.get(position);
	}

	@Override
	public long getItemId(int position) {
		return revIds.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView view = new TextView(mContext);
		view.setText(revArtNames.get(position));
		view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
		return view;
	}

}

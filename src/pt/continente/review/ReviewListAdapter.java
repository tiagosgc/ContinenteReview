package pt.continente.review;

import java.util.ArrayList;

import pt.continente.review.common.Common;
import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ReviewListAdapter extends BaseAdapter {
	private static final String TAG = "CntRev - ReviewListAdapter";
    private Context mContext;
    private ArrayList<Long> revIds;
    private ArrayList<String> revArtNames;
    private ArrayList<Boolean> areTitles;

	public ReviewListAdapter(Context c) {
    	Common.log(5, TAG, "ReviewListAdapter: started");
        mContext = c;
        revIds = new ArrayList<Long>();
        revArtNames = new ArrayList<String>();
        areTitles = new ArrayList<Boolean>();
	}

	public boolean addItem(long revId, String revArtName, Boolean isTitle) {
		if(revId != -1 && revArtName != null && !isTitle) {
			if(revIds.contains(revId)) {
		    	Common.log(1, TAG, "addItem: did not create as it would duplicate item with Id '" + revId + "'");
				return false;
			} else {
				revIds.add(revId);
				revArtNames.add(revArtName);
				areTitles.add(false);
		    	Common.log(5, TAG, "addItem: successfuly added new element with Id '" + revId + "'");
				return true;
			}
		} else if(isTitle == true && revArtName != null) {
			revIds.add(-1L);
			revArtNames.add(revArtName);
			areTitles.add(isTitle);
			return true;
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
		if(!areTitles.get(position)) {
			TextView listItem = new TextView(mContext);
			listItem.setText(revArtNames.get(position));
			listItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			listItem.setHeight(Common.pixelsFromDPs(mContext, 48));
			listItem.setGravity(Gravity.CENTER_VERTICAL);
			listItem.setPadding(Common.pixelsFromDPs(mContext, 5), 0, 0, 0);
			listItem.setClickable(false);
			return listItem;
		} else {
			LinearLayout linLay = new LinearLayout(mContext);
			linLay.setOrientation(LinearLayout.VERTICAL);
			
			//Adds the "empty list" box if previous category was empty
			if(position > 0 && areTitles.get(position - 1)) {
				TextView emptyBox = new TextView(mContext);
				emptyBox.setText(mContext.getResources().getString(R.string.label_reviewsListEmpty));
				emptyBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
				emptyBox.setHeight(Common.pixelsFromDPs(mContext, 48));
				emptyBox.setGravity(Gravity.CENTER_VERTICAL);
				emptyBox.setClickable(true);
				emptyBox.setPadding(Common.pixelsFromDPs(mContext, 5), 0, 0, 0);
				linLay.addView(emptyBox);
			}
			
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			params.setMargins(0, Common.pixelsFromDPs(mContext, 10), 0, 0);
			TextView listTitle = new TextView(mContext);
			listTitle.setLayoutParams(params);
			listTitle.setText(revArtNames.get(position));
			listTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			
			listTitle.setHeight(Common.pixelsFromDPs(mContext, 48));
			listTitle.setGravity(Gravity.CENTER_VERTICAL);
			listTitle.setBackgroundColor(mContext.getResources().getColor(R.color.Red));
			listTitle.setTextColor(mContext.getResources().getColor(R.color.white));
			listTitle.setClickable(true);
			listTitle.setPadding(Common.pixelsFromDPs(mContext, 5), 0, 0, 0);
			linLay.addView(listTitle);
			return linLay;
		}
	}

}

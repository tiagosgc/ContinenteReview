package pt.continente.review;

import java.util.List;

import com.bugsense.trace.BugSenseHandler;

import pt.continente.review.common.Article;
import pt.continente.review.common.Common;
import pt.continente.review.common.Preferences;
import pt.continente.review.common.Review;
import pt.continente.review.tables.ArticlesTable;
import pt.continente.review.tables.ReviewsTable;
import pt.continente.review.tables.SQLiteHelper;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class ReviewsListActivity extends Activity {
	private static final String TAG = "CntRev - ReviewsListActivity";
    private ReviewListAdapter adapter;
	private SQLiteHelper dbHelper;
	private ReviewsTable revsTable;
	private ArticlesTable artsTable;
	private Context context = this;
	private ListView lv;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BugSenseHandler.initAndStartSession(this, Common.bugSenseAppKey);
        setContentView(R.layout.activity_reviews_list);

        Common.log(5, TAG, "onCreate: started");

        dbHelper = new SQLiteHelper(this);
        adapter = new ReviewListAdapter(this);
        revsTable = null;
        artsTable = null;

        Common.log(5, TAG, "onCreate: will create the table Objects");
        try {
        	revsTable = new ReviewsTable(dbHelper);
        } catch (Exception e) {
        	Common.log(1, TAG, "onCreate: error creating the table Object 1");
        }
        
        try {
        	artsTable = new ArticlesTable(dbHelper);
        } catch (Exception e) {
        	Common.log(1, TAG, "onCreate: error creating the table Object 2");
        }
        
        Common.log(5, TAG, "onCreate: will exit");
        
        lv = (ListView) findViewById(R.id.ListView00);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            	launchReview(id, adapter.isReadOnly(position));
            }
        });
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.general_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onResume() {
        Common.log(5, TAG, "onResume: started");
        adapter.deleteAllItems();
        adapter.notifyDataSetChanged();
        try {
			revsTable.open();
		} catch (Exception e) {
			Common.log(1, TAG, "onResume: error opening table - " + e.getMessage());
		}
        try {
			artsTable.open();
		} catch (Exception e) {
			Common.log(1, TAG, "onResume: error opening table - " + e.getMessage());
		}
        Common.log(5, TAG, "onResume: will update the view");
        updateView();
        Common.log(5, TAG, "onResume: finished");
        super.onResume();
	}

	@Override
	protected void onPause() {
        Common.log(5, TAG, "onPause: started");
        revsTable.close();
        artsTable.close();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
        Common.log(5, TAG, "onDestroy: started");
		dbHelper.close();
		super.onDestroy();
	}

	private void updateView() {
    	Common.log(5, TAG, "updateView: started");
    	List<Review> revs;
    	
    	adapter.deleteAllItems();
    	
    	/*
    	 * Add Pending items
    	 */
//    	adapter.addItem(-1, context.getResources().getString(R.string.label_reviewsListPending), true, false);
//    	revs = revsTable.getAllItemsByState(Common.revStates.PENDING_USER);
//    	if(revs.size() > 0) addItemsToAdapter(revs, false);
//    	else Common.log(3, TAG, "updateView: no PENDING reviews found");
    	
    	/*
    	 * Add In Progress items
    	 */
    	adapter.addItem(-1, context.getResources().getString(R.string.label_reviewsListWIP), true, false);
    	revs = revsTable.getAllItemsByState(Common.revStates.WORK_IN_PROGRESS);
    	if(revs.size() > 0) addItemsToAdapter(revs, false);
    	else Common.log(3, TAG, "updateView: no WIP reviews found");
    	
    	/*
    	 * Add Completed items
    	 */
    	adapter.addItem(-1, context.getResources().getString(R.string.label_reviewsListCompleted), true, false);
    	revs = revsTable.getAllItemsByState(Common.revStates.COMPLETED);
    	if(revs.size() > 0) addItemsToAdapter(revs, true);
    	else Common.log(3, TAG, "updateView: no COMPLETED reviews found");
      
    	adapter.notifyDataSetChanged();
    	Common.log(5, TAG, "updateView: will exit");
    }
	
	private void addItemsToAdapter(List<Review> revs, boolean isReadOnly) {
    	Common.log(5, TAG, "addItemsToAdapter: started ('" + revs.size() + "' reviews will be processed)");
        for(Review item : revs) {
        	//Common.log(5, TAG, "updateAdapters: will attempt to get Article with Id '" + item.getArticleId() + "'");
        	Article artTmp = artsTable.getItem(item.getArticleId());
        	adapter.addItem(item.getId(), artTmp.getName(), false, isReadOnly);
        }
        Common.log(5, TAG, "addItemsToAdapter: finished");
	}
	
	private void launchReview(long id, boolean isReadOnly) {
    	Intent intent = new Intent(this, ReviewActivity.class);
    	intent.putExtra("revId", id);
    	intent.putExtra("isReviewReadOnly", isReadOnly);
    	startActivity(intent);
	}
}

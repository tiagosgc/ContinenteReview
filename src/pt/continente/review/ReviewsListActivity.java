package pt.continente.review;

import java.util.List;

import pt.continente.review.common.Article;
import pt.continente.review.common.Common;
import pt.continente.review.common.Review;
import pt.continente.review.common.ReviewActivity;
import pt.continente.review.tables.ArticlesTable;
import pt.continente.review.tables.ReviewsTable;
import pt.continente.review.tables.SQLiteHelper;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class ReviewsListActivity extends Activity {
	private static final String TAG = "CntRev - ReviewsListActivity";
    private ReviewListAdapter adapterPending;
    private ReviewListAdapter adapterWIP;
    private ReviewListAdapter adapterComplete;
	private SQLiteHelper dbHelper;
	private ReviewsTable revsTable;
	private ArticlesTable artsTable;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews_list);

        Common.log(5, TAG, "onCreate: started");

        dbHelper = new SQLiteHelper(this);
        adapterPending = new ReviewListAdapter(this);
        adapterWIP = new ReviewListAdapter(this);
        adapterComplete = new ReviewListAdapter(this);
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
        
        ListView lv;

        lv = (ListView) findViewById(R.id.ListView00);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                launchReview(id);
            }
        });

        lv = (ListView) findViewById(R.id.ListView01);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                launchReview(id);
            }
        });

        lv = (ListView) findViewById(R.id.ListView02);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                launchReview(id);
            }
        });
    }

    @Override
	protected void onResume() {
        Common.log(5, TAG, "onResume: started");
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
    	
    	revs = revsTable.getAllItemsByState(Common.revStates.PENDING_USER);
    	if(revs.size() > 0)
    		updateAdapters((ListView) findViewById(R.id.ListView00), revs, adapterPending);
    	else
    		showEmpty(R.id.empty1);

    	revs = revsTable.getAllItemsByState(Common.revStates.WORK_IN_PROGRESS);
    	if(revs.size() > 0)
    		updateAdapters((ListView) findViewById(R.id.ListView01), revs, adapterWIP);
    	else
    		showEmpty(R.id.empty2);
    	
    	revs = revsTable.getAllItemsByState(Common.revStates.COMPLETED);
    	if(revs.size() > 0)
    		updateAdapters((ListView) findViewById(R.id.ListView02), revs, adapterComplete);
    	else
    		showEmpty(R.id.empty3);
        
    	Common.log(5, TAG, "updateView: will exit");
    }
	
	private void updateAdapters(ListView view, List<Review> revs, ReviewListAdapter adapter) {
    	Common.log(5, TAG, "updateAdapters: started");
    	Common.log(5, TAG, "updateAdapters: got '" + revs.size() + "' items from table");
    	adapter.deleteAllItems();
    	Common.log(5, TAG, "updateAdapters: deleted all items");
        for(Review item : revs) {
        	Common.log(5, TAG, "updateAdapters: will attempt to get Article with Id '" + item.getArticleId() + "'");
        	Article artTmp = artsTable.getItem(item.getArticleId());
        	adapter.addItem(item.getId(), artTmp.getName());
        }
        Common.log(5, TAG, "updateAdapters: added items to adapter (" + adapter.getCount() + ")");
        view.setAdapter(adapter);
        Common.log(5, TAG, "updateAdapters: will exit");
	}
	
	private void showEmpty(int txtViewResource) {
		TextView txtView = (TextView) findViewById(txtViewResource);
		txtView.setVisibility(TextView.VISIBLE);
	}
	
	private void launchReview(long id) {
    	Intent intent = new Intent(this, ReviewActivity.class);
    	intent.putExtra("revId", id);
    	startActivity(intent);
	}
}

package pt.continente.review;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class ReviewWithTabs extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_with_tabs);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_review_with_tabs, menu);
        return true;
    }
}

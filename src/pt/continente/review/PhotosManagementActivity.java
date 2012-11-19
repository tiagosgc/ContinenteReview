package pt.continente.review;

import java.io.File;
import java.util.List;

import pt.continente.review.R;
import pt.continente.review.common.Common;
import pt.continente.review.common.ReviewImage;
import pt.continente.review.tables.ReviewImagesTable;
import pt.continente.review.tables.SQLiteHelper;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
 
public class PhotosManagementActivity extends Activity {
	private static final String TAG = "CntRev - PhotosManagementActivity";
	private static final int CAMERA_REQUEST = 1234;
	private static final int FULL_SCREEN_VIEW = 1235;
	
	private long relevantRevId;
	private ImageAdapter imageAdapter;
	private SQLiteHelper dbHelper;
	private ReviewImagesTable revImgsTable;
	private List<ReviewImage> revImgs;
	private GridView gridView;
	
	
	// TEM QUE RETORNAR UM INTENT
	//     i.putParcelableArrayListExtra("imagens", localAdapter.getAllItems());
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_photos_management);
		
		Common.log(5, TAG, "onCreate: started");
		gridView = (GridView) findViewById(R.id.grid_view);
		
		dbHelper = new SQLiteHelper(this);
		imageAdapter = new ImageAdapter(this);
		revImgsTable = null;
		revImgs = null;
		gridView.setAdapter(imageAdapter);
		
		Intent i = getIntent();
		relevantRevId = i.getExtras().getLong("revId");
		
		//TODO Fred:criar handling para Id <0 (inválido)        
		
		Common.log(5, TAG, "onCreate: will create the table Object");
		try {
			revImgsTable = new ReviewImagesTable(dbHelper);
		} catch (Exception e) {
			Common.log(1, TAG, "onCreate: error creating the table Object");
		}
		
		/* On Click event for Single Gridview Item */
		gridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v,int position, long id) {
				Intent i = new Intent(getApplicationContext(), FullImageActivity.class);
				i.putExtra("img", (Parcelable) imageAdapter.getItem(position));
				i.putExtra("imgId", imageAdapter.getImgId(position));
				startActivityForResult(i, FULL_SCREEN_VIEW);
			}
		});
		Common.log(5, TAG, "onCreate: will exit");
	}
	
	@Override
	protected void onResume() {
		Common.log(5, TAG, "onResume: started");
		try {
			revImgsTable.open();
		} catch (Exception e) {
			Common.log(1, TAG, "onResume: error opening table - " + e.getMessage());
		}
		Common.log(5, TAG, "onResume: will update the view");
		updateView();
//		if(revImgs.size() == 0) {
//			Common.log(5, TAG, "onResume: since there are no photos for this review, will atempt to open camera immediately");
//			adicionaNova(null);
//		}
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		Common.log(5, TAG, "onPause: started");
		revImgsTable.close();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		Common.log(5, TAG, "onDestroy: started");
		dbHelper.close();
		super.onDestroy();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home: // handles ActionBar home icon click
			finish();
			return true;  
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	public void adicionaNova(View v) {
		Log.i(TAG, "adicionaNova: entrou");
		Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE); 
		startActivityForResult(cameraIntent, CAMERA_REQUEST); 
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {  
			Bitmap photo = (Bitmap) data.getExtras().get("data");
			ReviewImage newRevImg = new ReviewImage(-1, relevantRevId, photo);
			try {
				revImgsTable.open();
			} catch (Exception e) {
				Common.log(1, TAG, "onActivityResult: error opening table 1 - " + e.getMessage());
			}
			if(revImgsTable.addItem(newRevImg)<=0)
				Common.log(3, TAG, "onActivityResult: could not add the new picture");
			//TODO Fred:testar se isto está a fazer o q é suposto (apagar a foto q acabou de tirar)
			deleteLastFromDCIM();
			updateView();
		}
		if (requestCode == FULL_SCREEN_VIEW && resultCode == RESULT_CANCELED) {
			try {
				revImgsTable.open();
			} catch (Exception e) {
				Common.log(1, TAG, "onActivityResult: error opening table 2 - " + e.getMessage());
			}
			long imgIdToDelete = data.getExtras().getLong("imgId");
			revImgsTable.deleteItem(imgIdToDelete);
			updateView();
		}
	}
	
	private void updateView() {
		Common.log(5, TAG, "updateView: started");
		revImgs = revImgsTable.getItems(relevantRevId);
		
		Common.log(5, TAG, "updateView: got '" + revImgs.size() + "' items from table");
		imageAdapter.deleteAllItems();
		for(ReviewImage item : revImgs) {
			imageAdapter.addItem(item.getImage(), item.getId());
		}
		
		Common.log(5, TAG, "updateView: added items to adapter");
		imageAdapter.notifyDataSetChanged();
		
		Common.log(5, TAG, "updateView: will exit");
	}

	
	private boolean deleteLastFromDCIM() {
		
		boolean success = false;
		try {
			File[] images = new File(Environment.getExternalStorageDirectory()
					+ File.separator + "DCIM/Camera").listFiles();
			File latestSavedImage = images[0];
			for (int i = 1; i < images.length; ++i) {
				if (images[i].lastModified() > latestSavedImage.lastModified()) {
					latestSavedImage = images[i];
				}
			}
			
			// OR JUST Use  success = latestSavedImage.delete();
			success = new File(Environment.getExternalStorageDirectory()
					+ File.separator + "DCIM/Camera/"
					+ latestSavedImage.getAbsoluteFile()).delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}
}
package pt.continente.review.common;

import android.graphics.Bitmap;

public class ReviewImage {
	public long imgId = -1;
	public long revId = -1;
	public Bitmap revImg = null;
	
	public ReviewImage(long imgId, long revId, Bitmap revImg) {
		setId(imgId);
		setRevId(revId);
		setImage(revImg);
	}

	public long getId() {
		return imgId;
	}

	private void setId(long imgId) {
		this.imgId = imgId;
	}

	public long getRevId() {
		return revId;
	}

	private void setRevId(long revId) {
		this.revId = revId;
	}

	public Bitmap getImage() {
		return revImg;
	}

	private void setImage(Bitmap revImg) {
		this.revImg = revImg;
	}

	public boolean isFullyDefinedExceptId() {
		if(revId != -1 && revImg != null)
			return true;
		return false;
	}
	
	public boolean isFullyDefined() {
		if(imgId != -1 && isFullyDefinedExceptId())
			return true;
		return false;
	}
}

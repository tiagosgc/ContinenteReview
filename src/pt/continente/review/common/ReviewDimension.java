package pt.continente.review.common;


public class ReviewDimension {
	//private static final String TAG = "CntRev - ReviewDimension";
	private long revId;
	private long dimId;
	private int revDimValue;
	
	public ReviewDimension(long revId, long dimId, int revDimValue) {
        this.revId = revId;
        this.dimId = dimId;
        this.revDimValue = revDimValue;
	}

	public long getRevId() {
		return revId;
	}

	public long getDimId() {
		return dimId;
	}

	public int getValue() {
		return revDimValue;
	}

	public void setValue(int revDimValue) {
		this.revDimValue = revDimValue;
	}
}

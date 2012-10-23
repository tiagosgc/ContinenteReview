package pt.continente.review.common;


public class Dimension {
	//private static final String TAG = "CntRev - Article";

	private long dimId;
	private String dimName;
	private String dimLabel;
	private String dimMin;
	private String dimMed;
	private String dimMax;

	
	public Dimension(long dimId, String dimName, String dimLabel, String dimMin, String dimMed, String dimMax) {
        this.dimId = dimId;
        this.dimName = dimName;
        this.dimLabel = dimLabel;
        this.dimMin = dimMin;
        this.dimMed = dimMed; 
        this.dimMax = dimMax;
	}

	public long getId() {
		return dimId;
	}

	public String getName() {
		return dimName;
	}

	public String getLabel() {
		return dimLabel;
	}

	public String getMin() {
		return dimMin;
	}

	public String getMed() {
		return dimMed;
	}

	public String getMax() {
		return dimMax;
	}

	public boolean isFullyDefined() {
		if(dimId != -1 && dimName != null && dimLabel != null && dimMin != null && dimMax != null)
			return true;
		return false;
	}

    public String toString() {
		return "I'm a dimension '" + dimLabel + "'";
    }
}

package pt.continente.review.common;

import java.io.Serializable;

import android.graphics.Bitmap;

public class Article implements Serializable {
	private static final String TAG = "CntRev - Article";
	private static final long serialVersionUID = 1L;

	private long artId;
	private String artName;
	private String artDescription;
	private String artEAN;
	private double artPrice;
	private String artImageURL;
	
	/*TODO Tiago: Este Bitmap tem que desaparecer, para termos um artigo veradeiramente serializable. 
	 *Em vez disto passamos a ter um "nome de ficheiro", não nulo quando o bitmap já foi guardado
	 *Podemos tambem apagar tudo no on destroy da actividade, não sei se é pertinenente ou naõ 
	 */
	private Bitmap artImage;
	private int artStructureL1;
	private int artStructureL2;
	private int artStructureL3;
	private int artStructureL4;
	

	public Article(long artId, String artName, String artDescription, String artEAN, double artPrice, String artImageURL, Bitmap artImage, int artStructureL1, int artStructureL2, int artStructureL3, int artStructureL4) {
        this.artId = artId;
        this.artName = artName;
        this.artDescription = artDescription;
        this.artEAN = artEAN;
        this.artPrice = artPrice; 
        this.artImageURL = artImageURL;
        this.artImage = artImage;
        this.artStructureL1 = artStructureL1;
        this.artStructureL2 = artStructureL2;
        this.artStructureL3 = artStructureL3;
        this.artStructureL4 = artStructureL4;
        Common.log(5, TAG, "Article: built article with ID '" + artId + "'");
	}

	public long getId() {
		return artId;
	}

	public String getName() {
		return artName;
	}

	public String getDescription() {
		return artDescription;
	}

	public String getEAN() {
		return artEAN;
	}

	public double getPrice() {
		return artPrice;
	}

	public String getImageURL() {
		return artImageURL;
	}

	public Bitmap getImage() {
		return artImage;
	}

	public void setImage(Bitmap artImage) {
		this.artImage = artImage;
	}

	public int getStructureL1() {
		return artStructureL1;
	}

	public int getStructureL2() {
		return artStructureL2;
	}

	public int getStructureL3() {
		return artStructureL3;
	}

	public int getStructureL4() {
		return artStructureL4;
	}

	public boolean isFullyDefined() {
		if(artId != -1 && artName != null && artDescription != null && artEAN != null && artPrice != -1)
			return true;
		return false;
	}

    public String toString() {
		return "I'm a product '" + artName + "'";
    }
}

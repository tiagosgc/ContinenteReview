package pt.continente.review.common;

import pt.continente.review.tables.ArticlesTable;
import pt.continente.review.tables.SQLiteHelper;

public class Review {
	private static final String TAG = "CntRev - Review";
	public long revId = -1;
	public int revState = -1;
	public long revArtId = -1;
	public String rev_comment = null;
	
	private Article revArt = null;
	
	
	public Review(long rev_id, int rev_state, long rev_art_id, String rev_comment) {
		setId(rev_id);
		setState(rev_state);
		setArticleId(rev_art_id);
		setComment(rev_comment);
	}

	public long getId() {
		return revId;
	}

	private void setId(long rev_id) {
		this.revId = rev_id;
	}

	public int getState() {
		return revState;
	}

	private void setState(int rev_state) {
		this.revState = rev_state;
	}

	public long getArticleId() {
		return revArtId;
	}

	private void setArticleId(long rev_art_id) {
		this.revArtId = rev_art_id;
	}

	public String getComment() {
		return rev_comment;
	}

	public void setComment(String rev_comment) {
		this.rev_comment = rev_comment;
	}
	
	public Article getArticle(SQLiteHelper dbHelper) {
		if(revArt == null && revArtId != -1) {
			ArticlesTable artTable;
			try {
				artTable = new ArticlesTable(dbHelper);
				artTable.open();
			} catch (Exception e) {
				Common.log(1, TAG, "getArticle: error opening table - " + e.getMessage());
				return null;
			}
			revArt = artTable.getItem(revArtId);
			artTable.close();
		}
		return revArt;
	}
	
	public boolean isFullyDefinedExceptId() {
		if(revState != -1 && revArtId != -1)
			return true;
		return false;
	}
	
	public boolean isFullyDefined() {
		if(revId != -1 && isFullyDefinedExceptId())
			return true;
		return false;
	}
}

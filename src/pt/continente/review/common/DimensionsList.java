package pt.continente.review.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DimensionsList implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private List<Dimension> dimsList;
	
	public DimensionsList() {
		dimsList = new ArrayList<Dimension>();
	}
	
	public void add(Dimension dim) {
		dimsList.add(dim);
	}
	
	public Dimension get(int location) {
		return dimsList.get(location);
	}

	public List<Dimension> getObject() {
		return dimsList;
	}

	public void clear() {
		dimsList.clear();
	}
	
	public int size() {
		return dimsList.size();
	}
	
}

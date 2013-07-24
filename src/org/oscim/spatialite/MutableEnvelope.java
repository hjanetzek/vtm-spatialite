package org.oscim.spatialite;

public class MutableEnvelope {

	public double maxX;
	public double minX;
	public double maxY;
	public double minY;

	public MutableEnvelope() {
	}

	public MutableEnvelope(double minX, double maxX, double minY, double maxY) {
		this.minX = minX;
		this.maxX = maxX;

		this.minY = minY;
		this.maxY = maxY;
	}

	public double getMaxX() {
		return maxX;
	}
	public double getMinX() {
		return minX;
	}
	public double getMaxY() {
		return maxY;
	}
	public double getMinY() {
		return minY;
	}

	public void add(Envelope bbox) {
		if (minX > bbox.minX)
			minX = bbox.minX;
		if (minY > bbox.minY)
			minY = bbox.minY;
		if (maxX < bbox.maxX)
			maxX = bbox.maxX;
		if (maxY < bbox.maxY)
			maxY = bbox.maxY;
    }

}

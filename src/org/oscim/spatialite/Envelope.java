package org.oscim.spatialite;

public class Envelope {

	public final double maxX;
	public final double minX;
	public final double maxY;
	public final double minY;

	public Envelope(double minX, double maxX, double minY, double maxY) {
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

	public Envelope(MutableEnvelope mutableEnvelope) {
		this.minX = mutableEnvelope.minX;
		this.minY = mutableEnvelope.minY;
		this.maxX = mutableEnvelope.maxX;
		this.maxY = mutableEnvelope.maxY;
	}

	@Override
	public String toString() {
		return minX + "," + minY + "x" + maxX + "," + maxY;
	}

}

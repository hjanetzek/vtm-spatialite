package org.oscim.spatialite;

public class DBLayer {


	public DBLayer(String table, String geomColumn, String geomType, String coordDimension, int srid,
                   boolean spatialIndex, String proj4txt) {
		this.table = table;
		this.geomColumn = geomColumn;
		this.type = geomType;
		this.srid = srid;
		this.spatialIndex = spatialIndex;
		this.proj4txt = proj4txt;
		this.coordDimension = coordDimension;
	}

	public String geomColumn;
	public String table;
	public int srid;
	public boolean spatialIndex;
	public String type;
	public String[] columns;
	public String proj4txt;
	public String coordDimension;

	public boolean hasName;

	public void setUserColumns(String[] columns){
		this.columns = columns;
		for (String c : columns)
			if ("name".equals(c))
				hasName = true;
	}
}

package org.oscim.spatialite;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jsqlite.Callback;
import jsqlite.Constants;
import jsqlite.Database;

import org.oscim.backend.Log;

/**
 * Basic communicator with a simple SpatiaLite database.
 *
 * @author jaak
 */
public class SpatialLiteDb {
	final static String TAG = SpatialLiteDb.class.getName();

	private static final int DEFAULT_SRID = 4326;
	//private static final int SDK_SRID = 3857;

	private final Database db;

	//private String dbPath;
	//private String sdk_proj4text;
	//private String spatialiteVersion;

	public SpatialLiteDb(String dbPath) {

		if (!new File(dbPath).exists()) {
			Log.e(TAG, "File not found: " + dbPath);
			db = null;
			return;

		}

		//this.dbPath = dbPath;
		db = new Database();
		try {
			db.open(dbPath, Constants.SQLITE_OPEN_READWRITE);
		} catch (Exception e) {
			Log.e(TAG, "SpatialLite: Failed to open database! " + e.getMessage());
			return;
		}

		//sdk_proj4text = qryProj4Def(SDK_SRID);
		//sdk_proj4text =
		//		"+proj=merc +lon_0=0 +k=1 +x_0=0 +y_0=0 +a=6378137 +b=6378137 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs";
		//this.spatialiteVersion = qrySpatialiteVersion();

	}

	public void close() {
		try {
			db.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Database getDB() {
		return this.db;
	}

	public Map<String, DBLayer> qrySpatialLayerMetadata() {
		final Map<String, DBLayer> dbLayers = new HashMap<String, DBLayer>();

		String typeColumn = "type";
		try {
			db.exec("SELECT "
					+ typeColumn
					+ " FROM \"geometry_columns\", spatial_ref_sys"
					+ " WHERE geometry_columns.srid=spatial_ref_sys.srid order by f_table_name",
					null);
		} catch (Exception e) {
			typeColumn = "geometry_type";
		}
		final boolean intGeometryType = typeColumn.equals("geometry_type");

		String qry = "SELECT \"f_table_name\", \"f_geometry_column\", "
				+ typeColumn
				+ ", \"coord_dimension\", geometry_columns.srid, \"spatial_index_enabled\", proj4text"
				+ " FROM \"geometry_columns\", spatial_ref_sys"
				+ " WHERE geometry_columns.srid=spatial_ref_sys.srid order by f_table_name";

		Log.d(TAG, qry);
		try {
			db.exec(qry, new Callback() {

				@Override
				public void columns(String[] coldata) {
				}

				@Override
				public void types(String[] types) {
				}

				@Override
				public boolean newrow(String[] rowdata) {
					int srid = DEFAULT_SRID;
					try {
						srid = Integer.parseInt(rowdata[4]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}

					// type column in Spatialite <4.x
					String geomType = rowdata[2];

					if (intGeometryType) {
						int geomTypeInt = Integer.parseInt(rowdata[2]);
						geomType = getGeometryType(geomTypeInt);
					}

					DBLayer dbLayer = new DBLayer(rowdata[0], rowdata[1],
							geomType, rowdata[3], srid, rowdata[5]
									.equals("1") ? true : false, rowdata[6]);

					dbLayers.put(dbLayer.table + "." + dbLayer.geomColumn, dbLayer);
					return false;
				}
			});

		} catch (Exception e) {
			Log.e(TAG, "SpatialLite: Failed to query metadata! "
					+ e.getMessage());
		}
		return dbLayers;
	}

	protected String getGeometryType(int geomTypeInt) {
		String geomType = null;

		// from https://www.gaia-gis.it/fossil/libspatialite/wiki?name=switching-to-4.0
		switch (geomTypeInt % 1000) {
			case 0:
				geomType = "GEOMETRY";
				break;
			case 1:
				geomType = "POINT";
				break;
			case 2:
				geomType = "LINESTRING";
				break;
			case 3:
				geomType = "POLYGON";
				break;
			case 4:
				geomType = "MULTIPOINT";
				break;
			case 5:
				geomType = "MULTILINESTRING";
				break;
			case 6:
				geomType = "MULTIPOLYGON";
				break;
			case 7:
				geomType = "GEOMETRYCOLLECTION";
				break;
		}

		switch (geomTypeInt / 1000) {
			case 1:
				geomType += " XYZ";
				break;
			case 2:
				geomType += " XYM";
				break;
			case 3:
				geomType += " XYZM";
				break;
		}

		return geomType;
	}

	public String qrySpatialiteVersion() {
		final List<String> spatialiteVersion = new ArrayList<String>();
		Callback cb = new Callback() {
			@Override
			public void columns(String[] coldata) {
			}

			@Override
			public void types(String[] types) {
			}

			@Override
			public boolean newrow(String[] rowdata) {
				Log.i(TAG, "spatialite_version:" + rowdata[0] + " proj4_version:" + rowdata[1]
						+ " geos_version:" + rowdata[2] + " sqlite version: " + rowdata[3]);
				spatialiteVersion.add(rowdata[0]);
				return false;
			}
		};

		String qry = "SELECT spatialite_version(), proj4_version(), geos_version(), sqlite_version()";
		// Log.d(TAG, qry);
		try {
			db.exec(qry, cb);

		} catch (Exception e) {
			Log.e(TAG, "SpatialLite: Failed to query versions. " + e.getMessage());
		}
		if (spatialiteVersion.size() > 0)
			return spatialiteVersion.get(0);

		return null;
	}

	//	private String qryProj4Def(int srid) {
	//
	//		String qry = "select proj4text from spatial_ref_sys where srid = "
	//				+ srid;
	//		ProjRowCallBack rcb = new ProjRowCallBack();
	//
	//		Log.d(TAG, qry);
	//		try {
	//			db.exec(qry, rcb);
	//
	//		} catch (Exception e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//		// if not found
	//		return rcb.foundName;
	//	}

	public class ProjRowCallBack implements Callback {

		public String foundName;

		@Override
		public void columns(String[] coldata) {
			Log.d(TAG, "SRID Columns: " + Arrays.toString(coldata));
		}

		@Override
		public void types(String[] types) {
			// not called really
			Log.d(TAG, "Types: " + Arrays.toString(types));
		}

		@Override
		public boolean newrow(String[] rowdata) {
			Log.d(TAG, "srid row: " + Arrays.toString(rowdata));
			foundName = rowdata[0];
			return false;
		}
	}

	public Envelope qryDataExtent(final DBLayer dbLayer) {

		String qry = "SELECT Min(MbrMinX(" + dbLayer.geomColumn + ")), Min(MbrMinY("
				+ dbLayer.geomColumn + ")), Max(MbrMaxX(" + dbLayer.geomColumn + ")), Max(MbrMaxY("
				+ dbLayer.geomColumn + ")) FROM " + dbLayer.table;
		Log.d(TAG, qry);
		final MutableEnvelope mutableEnvelope = new MutableEnvelope();

		try {
			db.exec(qry, new Callback() {

				@Override
				public void columns(String[] coldata) {
				}

				@Override
				public void types(String[] types) {
				}

				@Override
				public boolean newrow(String[] rowdata) {
					Envelope bbox = new Envelope(Double.parseDouble(rowdata[0]), Double
							.parseDouble(rowdata[2]),
							Double.parseDouble(rowdata[1]), Double.parseDouble(rowdata[3]));
					Log.d(TAG, "original bbox :" + bbox);

					Envelope transformedBbox;

					// convert to SDK proj
					//                    if (dbLayer.srid != SDK_SRID) {
					//                        Log.d(TAG, "SpatialLite: Data must be transformed from " + dbLayer.srid
					//                                + " to " + SDK_SRID);
					//
					//                        transformedBbox = GeoUtils.transformBboxJavaProj(bbox,
					//                                dbLayer.proj4txt.replace("longlat", "latlong"),
					//                                sdk_proj4text);
					//
					//                        Log.d(TAG, "bbox converted to Map SRID:" + transformedBbox);
					//                    } else {
					transformedBbox = bbox;
					//                    }

					mutableEnvelope.add(transformedBbox);
					return false;
				}
			});

		} catch (Exception e) {
			Log.e(TAG, "SpatialLite: Failed to query envelope! "
					+ e.getMessage());
		}

		return new Envelope(mutableEnvelope);

	}

	public String[] qryColumns(final DBLayer dbLayer) {

		String qry = "PRAGMA table_info(" + dbLayer.table + ")";
		Log.d(TAG, qry);
		final ArrayList<String> columns = new ArrayList<String>();

		try {
			db.exec(qry, new Callback() {

				@Override
				public void columns(String[] coldata) {
				}

				@Override
				public void types(String[] types) {
				}

				@Override
				public boolean newrow(String[] rowdata) {
					String col = rowdata[1];
					String type = rowdata[2];
					// add only known safe column types, skip geometries
					if (type.equals("INTEGER") || type.equals("TEXT") || type.startsWith("VARCHAR")) {
						columns.add(col);
					}
					return false;
				}
			});

		} catch (Exception e) {
			Log.e(TAG, "SpatialLite: Failed to query columns! "
					+ e.getMessage());
		}

		return columns.toArray(new String[0]);
	}

	public void defineEPSG3857() {
		try {
			db.exec("replace into spatial_ref_sys (srid,auth_name,auth_srid,ref_sys_name,proj4text) values(3857,'epsg',3857,'Google Sperhical Mercator','+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +no_defs')",
					null);
		} catch (Exception e) {
			Log.e(TAG, "SpatialLite: Failed to insert EPSG3857 definition"
					+ e.getMessage());
		}
	}

	//	public long insertSpatiaLiteGeom(DBLayer dbLayer, Geometry geom) {
	//		String wktGeom = WktWriter.writeWkt(geom, dbLayer.type);
	//		String userColumns = "";
	//		String userValues = "";
	//		if (geom.userData instanceof Map) {
	//			@SuppressWarnings("unchecked")
	//			Map<String, String> userData = (Map<String, String>) geom.userData;
	//			for (Map.Entry<String, String> entry : userData.entrySet()) {
	//				if (!entry.getKey().startsWith("_")) {
	//					userColumns += "," + entry.getKey();
	//					userValues += "," + escapeSql(entry.getValue());
	//				}
	//			}
	//		}
	//
	//		String qry = "INSERT INTO \"" + dbLayer.table + "\" (" + dbLayer.geomColumn + userColumns
	//				+ ") VALUES (Transform(GeometryFromText('" + wktGeom + "'," + SDK_SRID + "), "
	//				+ Integer.toString(dbLayer.srid) + ") " + userValues + ")";
	//		Log.d(TAG, qry);
	//		try {
	//			db.exec(qry, null);
	//		} catch (Exception e) {
	//			Log.e(TAG, "SpatialLite: Failed to insert data! " + e.getMessage());
	//			return 0;
	//		}
	//		return db.last_insert_rowid();
	//	}
	//
	//	public void updateSpatiaLiteGeom(DBLayer dbLayer, long id, Geometry geom) {
	//		String wktGeom = WktWriter.writeWkt(geom, dbLayer.type);
	//		String userFields = "";
	//		if (geom.userData instanceof Map) {
	//			@SuppressWarnings("unchecked")
	//			Map<String, String> userData = (Map<String, String>) geom.userData;
	//			for (Map.Entry<String, String> entry : userData.entrySet()) {
	//				if (!entry.getKey().startsWith("_")) {
	//					userFields += "," + entry.getKey() + "=" + escapeSql(entry.getValue());
	//				}
	//			}
	//		}
	//
	//		String qry = "UPDATE \"" + dbLayer.table + "\" SET " + dbLayer.geomColumn
	//				+ "=Transform(GeometryFromText('" + wktGeom + "'," + SDK_SRID + "), "
	//				+ Integer.toString(dbLayer.srid) + ") " + userFields + " WHERE rowid=" + id;
	//		Log.d(TAG, qry);
	//		try {
	//			db.exec(qry, null);
	//		} catch (Exception e) {
	//			Log.e(TAG, "SpatialLite: Failed to update data! " + e.getMessage());
	//		}
	//	}

	public void deleteSpatiaLiteGeom(DBLayer dbLayer, long id) {
		String qry = "DELETE FROM \"" + dbLayer.table + "\" WHERE rowid=" + id;
		Log.d(TAG, qry);
		try {
			db.exec(qry, null);
		} catch (Exception e) {
			Log.e(TAG, "SpatialLite: Failed to delete data! " + e.getMessage());
		}
	}

	static String escapeSql(String value) {
		if (value == null) {
			return "NULL";
		}
		String hexValue;
		try {
			hexValue = String.format("%x", new BigInteger(1, value.getBytes("UTF-8"))); // NOTE: implicitly assuming here database is using UTF-8 encoding
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unsupported character");
		}
		return "X'" + hexValue + "'";
	}
}

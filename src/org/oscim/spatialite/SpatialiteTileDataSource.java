/*
 * Copyright 2013 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.spatialite;

import java.util.Map;

import jsqlite.Callback;
import jsqlite.Database;

import org.oscim.backend.Log;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.tilesource.ITileDataSink;
import org.oscim.tilesource.ITileDataSource;
import org.oscim.tilesource.mapfile.Projection;
import org.oscim.utils.wkb.WKBReader;

public class SpatialiteTileDataSource implements ITileDataSource, WKBReader.Callback {
	/* private */final static String TAG = SpatialiteTileDataSource.class.getName();
	/* private */final MapElement mElem;
	/* private */final WKBReader mWKBReader;

	private final SpatialLiteDb db;
	private final Map<String, DBLayer> dbLayers;

	private ITileDataSink mSink;

	public SpatialiteTileDataSource(SpatialLiteDb db, Map<String, DBLayer> layers) {
		this.db = db;
		this.dbLayers = layers;

		mElem = new MapElement();

		// initialize WKBReader to use MapElement (extends GeometryBuffer) as output.
		mWKBReader = new WKBReader(mElem, true);
		mWKBReader.setCallback(this);
	}

	/**
	 * @see org.oscim.tilesource.ITileDataSource#executeQuery(MapTile,
	 *      ITileDataSink)
	 */
	@Override
	public QueryResult executeQuery(MapTile tile, ITileDataSink mapDataSink) {
		mSink = mapDataSink;

		for (String layerKey : dbLayers.keySet()) {
			qrySpatiaLiteGeom(dbLayers.get(layerKey), tile, 500);
		}
		return QueryResult.SUCCESS;
	}

	/**
	 * @see org.oscim.tilesource.ITileDataSource#destroy()
	 */
	@Override
	public void destroy() {
		db.close();
	}

	/**
	 * Callback from WKBReader.parse()
	 * @see org.oscim.utils.wkb.WKBReader.Callback#process(GeometryBuffer)
	 */
	@Override
	public void process(GeometryBuffer geom) {

		if (geom.type == GeometryType.NONE)
			return;

		mSink.process((MapElement) geom);
	}

	public void qrySpatiaLiteGeom(DBLayer dbLayer, final Tile tile, int limit) {

		Callback cb = new Callback() {

			@Override
			public void columns(String[] coldata) {
				//Log.d(TAG, "columns" + Arrays.toString(coldata));
			}

			@Override
			public void types(String[] types) {
			}

			@Override
			public boolean newrow(String[] rowdata) {
				mElem.tags.clear();

				for (int i = 2; i < rowdata.length; i++) {
					if (rowdata[i] == null)
						continue;
					//Log.d(TAG, "n: " + rowdata[i]);
					mElem.tags.add(new Tag(Tag.TAG_KEY_NAME, rowdata[i]));
				}

				// First column is always row id
				// userData.put("_id", rowdata[0]);

				// this.process() will be called for each parsed geometry
				try {
					mWKBReader.parse(rowdata[1]);
				} catch (Exception e) {
					e.printStackTrace();
				}

				return false;
			}
		};

		String userColumn = "";

		if (dbLayer.hasName) {
			userColumn = ", name";
		}

		String geomCol = dbLayer.geomColumn;
		Envelope bbox = tileToBBox(tile, 0);

		double sx = bbox.getMinX();
		double sy = bbox.getMinY();
		double sw = Tile.SIZE / (bbox.getMaxX() - sx);
		double sh = Tile.SIZE / (bbox.getMaxY() - sy);

		double minX = bbox.getMinX();
		double minY = bbox.getMinY();
		double maxX = bbox.getMaxX();
		double maxY = bbox.getMaxY();

		if (dbLayer.srid == 4326) {
			minX = Projection.tileXToLongitude(tile.tileX, tile.zoomLevel);
			maxX = Projection.tileXToLongitude(tile.tileX + 1, tile.zoomLevel);
			minY = Projection.tileYToLatitude(tile.tileY + 1, tile.zoomLevel);
			maxY = Projection.tileYToLatitude(tile.tileY, tile.zoomLevel);
		}

		String qry = "SELECT rowid, HEX(AsBinary("
				+ "Simplify("
				// clip to Tile.SIZE
				+ "Intersection(GeomFromText('POLYGON((-2 -2, 402 -2, 402 402, -2 402, -2 -2))'),"
				//+ "Intersection(GeomFromText('POLYGON((2 2, 398 2, 398 398, 2 398, 2 2))'),"
				// translate to tile coordinates
				+ "ScaleCoords(ShiftCoords("
				+ "Transform("
				+ geomCol + ", 3857)"
				+ "," + (-sx) + "," + (-sy) + "), " + sw + "," + sh + ")"
				+ ") "   // end Intersection
				+ ", 2)" // end Simplify (2 pixel)
				+ "))"  // end HEX
				+ userColumn
				+ " FROM \"" + dbLayer.table + "\"";

		if (!dbLayer.spatialIndex) {
			qry += " WHERE MBRIntersects(BuildMBR("
					+ minX + "," + minY + "," + maxX + "," + maxY
					+ ")," + dbLayer.geomColumn + ")";
		} else {
			qry += " WHERE ROWID IN (select pkid from idx_"
					+ dbLayer.table + "_" + dbLayer.geomColumn
					+ " WHERE pkid MATCH RtreeIntersects("
					+ minX + "," + minY + "," + maxX + "," + maxY
					+ "))";
		}

		qry += " LIMIT " + limit;

		//Log.d(TAG, qry);
		long time = 0;
		long wait = System.currentTimeMillis();

		try {
			Database d = db.getDB();
			synchronized (d) {
				time = System.currentTimeMillis();
				wait = time - wait;
				db.getDB().exec(qry, cb);
			}
		} catch (Exception e) {
			Log.e(TAG, "SpatialLite: Failed to query data! "
					+ e.getMessage() + ":"
					+ db.getDB().error_message());
		}

		time = System.currentTimeMillis() - time;
		if (time > 10) {
			Log.d(TAG, tile + " took: " + time + " wait: " + wait + " (" + dbLayer.table + ")");
		}
	}

	double scaleFactor = 20037508.342789244;

	private Envelope tileToBBox(Tile tile, int pixel) {
		long size = Tile.SIZE;

		double tileX = tile.tileX * size;
		double tileY = tile.tileY * size;

		double center = (size << tile.zoomLevel) >> 1;

		double minLat = ((center - (tileY + size + pixel)) / center) * scaleFactor;
		double maxLat = ((center - (tileY - pixel)) / center) * scaleFactor;

		double minLon = (((tileX - pixel) - center) / center) * scaleFactor;
		double maxLon = (((tileX + size + pixel) - center) / center) * scaleFactor;

		return new Envelope(minLon, maxLon, minLat, maxLat);
	}

	float pixelAtZoom(int zoomLevel) {
		return 20037508.342789244f / 256 / (1 << zoomLevel);
	}
}

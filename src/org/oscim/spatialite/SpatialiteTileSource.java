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

import java.util.Arrays;
import java.util.Map;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.tilesource.ITileDataSource;
import org.oscim.tilesource.MapInfo;
import org.oscim.tilesource.TileSource;

import android.util.Log;

public class SpatialiteTileSource extends TileSource {
	private static final String TAG = SpatialiteTileSource.class.getName();

	String dbFile;

	private Map<String, DBLayer> layers;

	public SpatialiteTileSource(String dbFile) {
		this.dbFile = dbFile;
	}

	@Override
	public ITileDataSource getDataSource() {
		SpatialLiteDb db = new SpatialLiteDb(dbFile);

		return new SpatialiteTileDataSource(db, layers);
	}

	@Override
	public OpenResult open() {
		SpatialLiteDb db = new SpatialLiteDb(dbFile);

		layers = db.qrySpatialLayerMetadata();

		for (String layerKey : layers.keySet()) {
			DBLayer layer = layers.get(layerKey);
			layer.setUserColumns(db.qryColumns(layer));

			Log.d(TAG, "load layer: " + layerKey
					+ ", " + Arrays.deepToString(layer.columns));
		}

		db.close();

		return OpenResult.SUCCESS;
	}

	@Override
	public void close() {
	}

	@Override
	public MapInfo getMapInfo() {
		return mMapInfo;
	}

	private static final MapInfo mMapInfo =
			new MapInfo(new BoundingBox(-180, -90, 180, 90),
					new Byte((byte) 4), new GeoPoint(0.0, 0.0),
					null, 0, 0, 0, "en", "comment", "author", null);
}

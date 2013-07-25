package org.oscim.android.test;

import org.oscim.core.MapPosition;
import org.oscim.layers.labeling.LabelLayer;
import org.oscim.layers.tile.vector.MapTileLayer;
import org.oscim.renderer.GLRenderer;
import org.oscim.spatialite.SpatialiteTileSource;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.theme.ThemeLoader;
import org.oscim.tilesource.TileSource;
import org.oscim.view.MapView;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;

public class MapActivity extends org.oscim.android.MapActivity {


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapView = (MapView) findViewById(R.id.mapView);

		//mMap = mMapView.getMap();
		//TileSource tileSource = new OSciMap2TileSource();
		//tileSource.setOption("url", "http://city.informatik.uni-bremen.de/osci/map-live");

		//TileSource tileSource = new OSciMap4TileSource();
		//tileSource.setOption("url", "http://city.informatik.uni-bremen.de/osci/testing");

		//MapTileLayer l = mMapView.setBaseMap(tileSource);
		//mMapView.setDebugSettings(new DebugSettings(false, false, true, false, false));

		//mMapView.getLayerManager().add(new BuildingOverlay(mMapView, l.getTileLayer()));
		//mMapView.getLayerManager().add(new LabelLayer(mMapView, l.getTileLayer()));

		//mMapView.setTheme(InternalRenderTheme.DEFAULT);
		//mMapView.setTheme(InternalRenderTheme.TRONRENDER);

		//mMap.setBackgroundMap(new BitmapTileLayer(mMap, MapQuestAerial.INSTANCE));

		GLRenderer.setBackgroundColor(Color.DKGRAY);

		MapTileLayer layer = new MapTileLayer(mMapView);

		String path = Environment.getExternalStorageDirectory().getPath();
		//TileSource tileSource = new SpatialiteTileSource(path + "/ne.sqlite");
		TileSource tileSource = new SpatialiteTileSource(path + "/bremen-map.sqlite");

		layer.setTileSource(tileSource);
		//layer.setRenderTheme(new DebugTheme());
		layer.setRenderTheme(ThemeLoader.load(InternalRenderTheme.DEFAULT));

		mMapView.getLayerManager().add(layer);
		mMapView.getLayerManager().add(new LabelLayer(mMapView, layer.getTileLayer()));

		MapPosition p = new MapPosition();
		p.setPosition(53.08, 8.81);
		p.setZoomLevel(15);
		mMapView.setMapPosition(p);

		mMapView.setClickable(true);
		mMapView.setFocusable(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_map, menu);
		return true;
	}
}

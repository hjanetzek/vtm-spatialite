Experimental TileSource for spatialite. 

Download http://city.informatik.uni-bremen.de/~jeff/ne.sqlite to /sdcard/ for testing.
A pre-compiled apk is available at http://city.informatik.uni-bremen.de/~jeff/vtm-spatialite-android.apk

The master branch works with hjanetzek/vtm-android:master. release_0_5 branch matches vtm-android:release_0_5

DIY:
The Database should only contain geometries in EPSG:3857 projection, 4326 should work though.

```
ogr2ogr -f SQLite -dsco SPATIALITE=yes -overwrite -nlt multipolygon -clipsrc -180 -85.5 180 85.5 -s_srs EPSG:4326 -t_srs EPSG:3857 ne.sqlite 50m_physical/ne_50m_land.shp

ogr2ogr -f SQLite -dsco SPATIALITE=yes -clipsrc -180 -85.5 180 85.5 -s_srs EPSG:4326 -t_srs EPSG:3857 ne.sqlite 110m_cultural/ne_110m_populated_places.shp
```

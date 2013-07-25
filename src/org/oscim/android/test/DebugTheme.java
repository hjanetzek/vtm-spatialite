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
package org.oscim.android.test;

import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.renderinstruction.Area;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.RenderInstruction;
import org.oscim.theme.renderinstruction.Text;
import org.oscim.view.MapView;

import android.graphics.Color;

public class DebugTheme implements IRenderTheme {

	private static final String TAG_KEY_ID = "id";

	private final RenderInstruction[] mText = new RenderInstruction[] {
			Text.createText(Tag.TAG_KEY_NAME, 22, 3, Color.BLACK, Color.WHITE, true),
	};

	private final RenderInstruction[] mLine = new RenderInstruction[] {
			new Line(1000, Color.RED, 1)
	};

	private final RenderInstruction[] mPoly = new RenderInstruction[] {
			//new Area(0, 0x88000088),
			//new Area(0, Color.LTGRAY)
			null,
			new Line(999, Color.BLUE, 2)
	};

	public DebugTheme() {
		scaleTextSize(1 + (MapView.dpi / 240 - 1) * 0.5f);
	}

	@Override
	public RenderInstruction[] matchElement(MapElement element, int zoomLevel) {
		if (element.isPoint())
			return mText;
		else if (element.isLine())
			return mLine;
		else if (element.isPoly()) {
			if (element.tags.containsKey(TAG_KEY_ID)) {
				Tag t = element.tags.get(TAG_KEY_ID);
				int id = Integer.parseInt(t.value);
				int hash = hashCode(id + 37) % 256;
				mPoly[0] = new Area(hash, rainBowColor(hash));
			} else {
				mPoly[0] = new Area(0, Color.LTGRAY);
			}
			return mPoly;

		}
		return null;
	}

	@Override
	public void destroy() {
	}

	@Override
	public int getLevels() {
		// only required when used with 'MapElement.layer'
		// to properly separate 'levels'.
		return 1000;
	}

	@Override
	public int getMapBackground() {
		return 0;
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		mText[0].scaleTextSize(scaleFactor);
	}

	// from http://stackoverflow.com/questions/6082915/
	// a-good-hash-function-to-use-in-interviews-for-integer-numbers-strings
	private static int hashCode(int a) {
		a ^= (a << 13);
		a ^= (a >>> 17);
		a ^= (a << 5);
		return a;
	}

	// from http://krazydad.com/tutorials/makecolors.php
	private static int rainBowColor(float pos) {
		float i = (255 * 255 / pos);
		int r = (int) Math.round(Math.sin(0.024 * i + 0) * 127 + 128);
		int g = (int) Math.round(Math.sin(0.024 * i + 2) * 127 + 128);
		int b = (int) Math.round(Math.sin(0.024 * i + 4) * 127 + 128);
		return 0xff000000 | (r << 16) | (g << 8) | b;
	}
}

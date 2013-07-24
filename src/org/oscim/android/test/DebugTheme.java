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

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Color;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.renderinstruction.Area;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.RenderInstruction;
import org.oscim.theme.renderinstruction.Text;

public class DebugTheme implements IRenderTheme {

	private final RenderInstruction[] mText = new RenderInstruction[] {
	        Text.createText(Tag.TAG_KEY_NAME, 22, 3, Color.BLACK, Color.WHITE, true),
	};

	private final RenderInstruction[] mLine = new RenderInstruction[] {
	        new Line(1, Color.RED, 1)
	};

	private final RenderInstruction[] mPoly = new RenderInstruction[] {
			//new Area(0, 0x88000088),
	        new Area(0, Color.LTGRAY),
	        new Line(2, Color.BLUE, 2)
	};

	public DebugTheme() {
		scaleTextSize(1 + (CanvasAdapter.dpi / 240 - 1) * 0.5f);
	}

	@Override
	public RenderInstruction[] matchElement(MapElement element, int zoomLevel) {
		if (element.isPoint())
			return mText;
		else if (element.isLine())
			return mLine;
		else if (element.isPoly())
			return mPoly;

		return null;
	}

	@Override
	public void destroy() {
	}

	@Override
	public int getLevels() {
		return 3;
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
	}

}
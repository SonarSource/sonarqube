/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.charts.deprecated;

import java.awt.Color;
import java.awt.Paint;
import org.jfree.chart.renderer.category.BarRenderer;

public class CustomBarRenderer extends BarRenderer {

  protected static final Paint[] COLORS = {
    Color.red, Color.blue, Color.green,
    Color.yellow, Color.orange, Color.cyan,
    Color.magenta, Color.blue};

  /**
   * The colors.
   */
  private Paint[] colors;

  /**
   * Creates a new renderer.
   *
   * @param colors the colors.
   */
  public CustomBarRenderer(final Paint[] colors) {
    this.colors = colors;
  }

  /**
   * Returns the paint for an item.  Overrides the default behaviour inherited from
   * AbstractSeriesRenderer.
   *
   * @param row    the series.
   * @param column the category.
   * @return The item color.
   */
  @Override
  public Paint getItemPaint(final int row, final int column) {
    return this.colors[column % this.colors.length];
  }

  public Paint[] getColors() {
    return colors;
  }

  public void setColors(Paint[] colors) {
    this.colors = colors;
  }

}

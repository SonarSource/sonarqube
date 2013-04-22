/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.charts;

import org.apache.commons.lang.StringUtils;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.sonar.api.charts.AbstractChart;
import org.sonar.api.charts.ChartParameters;

import java.awt.*;

public class XradarChart extends AbstractChart {

  /**
   * see an example of complete URL in XradarChartTest
   */

  public static final String PARAM_COLOR = "c";
  public static final String PARAM_MAX_VALUE = "m";
  public static final String PARAM_INTERIOR_GAP = "g";
  public static final String PARAM_LABELS = "l";
  public static final String PARAM_VALUES = "v";

  public String getKey() {
    return "xradar";
  }

  @Override
  protected Plot getPlot(ChartParameters params) {
    SpiderWebPlot plot = new SpiderWebPlot(createDataset(params));
    plot.setStartAngle(0D);
    plot.setOutlineVisible(false);
    plot.setAxisLinePaint(Color.decode("0xCCCCCC"));
    plot.setSeriesOutlineStroke(new BasicStroke(2f));

    if (params.getValue(PARAM_INTERIOR_GAP) != null) {
      plot.setInteriorGap(Double.parseDouble(params.getValue(PARAM_INTERIOR_GAP, "0.4", false)));
    }
    if (params.getValue(PARAM_MAX_VALUE) != null) {
      plot.setMaxValue(Double.parseDouble(params.getValue(PARAM_MAX_VALUE, "100", false)));
    }
    configureColors(plot, params);
    return plot;
  }

  private void configureColors(SpiderWebPlot plot, ChartParameters params) {
    String[] colors = params.getValues(PARAM_COLOR, "|");
    for (int i = 0; i < colors.length; i++) {
      plot.setSeriesPaint(i, Color.decode("0x" + colors[i]));
    }
  }

  private CategoryDataset createDataset(ChartParameters params) {
    String[] labels = params.getValues(PARAM_LABELS, ",");
    String[] values = params.getValues(PARAM_VALUES, "|");

    DefaultCategoryDataset set = new DefaultCategoryDataset();
    for (int indexValues = 0; indexValues < values.length; indexValues++) {
      String[] fields = StringUtils.split(values[indexValues], ",");
      for (int i = 0; i < fields.length; i++) {
        set.addValue(Double.parseDouble(fields[i]), "" + indexValues, labels[i]);
      }
    }
    return set;
  }
}
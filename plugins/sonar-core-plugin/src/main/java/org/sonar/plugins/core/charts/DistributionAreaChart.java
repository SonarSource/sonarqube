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
package org.sonar.plugins.core.charts;

import org.apache.commons.lang.StringUtils;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.sonar.api.charts.AbstractChart;
import org.sonar.api.charts.ChartParameters;

import java.text.NumberFormat;

public class DistributionAreaChart extends AbstractChart {
  private static final String PARAM_COLORS = "c";

  @Override
  public String getKey() {
    return "distarea";
  }

  @Override
  protected Plot getPlot(ChartParameters params) {
    DefaultCategoryDataset dataset = createDataset(params);

    CategoryAxis domainAxis = new CategoryAxis();
    domainAxis.setCategoryMargin(0.0);
    domainAxis.setLowerMargin(0.0);
    domainAxis.setUpperMargin(0.0);

    NumberAxis rangeAxis = new NumberAxis();
    rangeAxis.setNumberFormatOverride(NumberFormat.getIntegerInstance(params.getLocale()));
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

    AreaRenderer renderer = new AreaRenderer();
    CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);
    plot.setForegroundAlpha(0.5f);
    plot.setDomainGridlinesVisible(true);
    configureColors(dataset, plot, params.getValues(PARAM_COLORS, ","));
    return plot;
  }

  private DefaultCategoryDataset createDataset(ChartParameters params) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    String[] series = params.getValues("v", "|", true);
    int index = 0;
    while (index < series.length) {
      String[] pairs = StringUtils.split(series[index], ";");
      if (pairs.length == 0) {
        dataset.addValue((Number)0.0, index, "0");

      } else {
        for (String pair : pairs) {
          String[] keyValue = StringUtils.split(pair, "=");
          double val = Double.parseDouble(keyValue[1]);
          dataset.addValue((Number) val, index, keyValue[0]);
        }
      }
      index++;
    }
    return dataset;
  }
}

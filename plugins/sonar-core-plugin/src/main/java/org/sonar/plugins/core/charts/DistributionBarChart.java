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
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.sonar.api.charts.AbstractChart;
import org.sonar.api.charts.ChartParameters;

import java.awt.*;
import java.text.DecimalFormat;

public class DistributionBarChart extends AbstractChart {


  public static final String PARAM_VALUES = "v";
  public static final String PARAM_COLORS = "c";
  public static final String PARAM_Y_SUFFIX = "ysuf";
  public static final String PARAM_X_SUFFIX = "xsuf";
  public static final String PARAM_FONT_SIZE = "fs";

  @Override
  public String getKey() {
    return "distbar";
  }

  @Override
  public Plot getPlot(ChartParameters params) {
    CategoryPlot plot = generateJFreeChart(params);
    plot.setOutlinePaint(OUTLINE_COLOR);
    plot.setDomainGridlinePaint(GRID_COLOR);
    plot.setRangeGridlinePaint(GRID_COLOR);
    return plot;
  }

  private CategoryPlot generateJFreeChart(ChartParameters params) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    CategoryPlot plot = new CategoryPlot();

    Font font = getFont(params.getValue(PARAM_FONT_SIZE));
    configureDomainAxis(plot, font);
    configureRangeAxis(plot, params.getValue(PARAM_Y_SUFFIX, "", true), font);
    configureRenderer(plot);
    configureValues(dataset, params.getValues(PARAM_VALUES, "|", true), params.getValue(PARAM_X_SUFFIX, "", true));
    configureColors(dataset, plot, params.getValues(PARAM_COLORS, ","));

    plot.setDataset(dataset);
    return plot;
  }

  static void configureValues(DefaultCategoryDataset dataset, String[] series, String xSuffix) {
    int index = 0;
    while (index < series.length) {
      String[] pairs = StringUtils.split(series[index], ";");
      if (pairs.length == 0) {
        dataset.addValue((Number) 0.0, index, "0");

      } else {
        for (String pair : pairs) {
          String[] keyValue = StringUtils.split(pair, "=");
          double val = Double.parseDouble(keyValue[1]);
          dataset.addValue((Number) val, index, keyValue[0] + xSuffix);
        }
      }
      index++;
    }

  }

  private void configureRenderer(CategoryPlot plot) {
    BarRenderer renderer = new BarRenderer();
    renderer.setDrawBarOutline(true);
    renderer.setSeriesItemLabelsVisible(0, true);
    renderer.setItemMargin(0);
    plot.setRenderer(renderer);
  }

  private void configureDomainAxis(CategoryPlot plot, Font font) {
    CategoryAxis categoryAxis = new CategoryAxis();
    categoryAxis.setTickMarksVisible(true);
    categoryAxis.setTickLabelFont(font);
    categoryAxis.setTickLabelPaint(OUTLINE_COLOR);
    plot.setDomainAxis(categoryAxis);
    plot.setDomainGridlinesVisible(false);
  }

  private Font getFont(String fontSize) {
    int size = FONT_SIZE;
    if (!StringUtils.isBlank(fontSize)) {
      size = Integer.parseInt(fontSize);
    }
    return new Font("SansSerif", Font.PLAIN, size);
  }

  private void configureRangeAxis(CategoryPlot plot, String valueLabelSuffix, Font font) {
    NumberAxis numberAxis = new NumberAxis();
    numberAxis.setUpperMargin(0.3);
    numberAxis.setTickLabelFont(font);
    numberAxis.setTickLabelPaint(OUTLINE_COLOR);
    String suffix = "";
    if (valueLabelSuffix != null && !"".equals(valueLabelSuffix)) {
      suffix = new StringBuilder().append("'").append(valueLabelSuffix).append("'").toString();
    }
    numberAxis.setNumberFormatOverride(new DecimalFormat("0" + suffix));
    numberAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    plot.setRangeAxis(numberAxis);
  }
}

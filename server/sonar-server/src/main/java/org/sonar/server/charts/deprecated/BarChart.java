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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;

public class BarChart extends BaseChartWeb implements DeprecatedChart {

  private BarRenderer renderer = null;
  protected DefaultCategoryDataset dataset = null;
  protected CategoryAxis categoryAxis = null;
  protected NumberAxis numberAxis = null;

  public BarChart(Map<String, String> params) {
    super(params);
    jfreechart = new JFreeChart(null, TextTitle.DEFAULT_FONT, new CategoryPlot(), false);
  }

  @Override
  protected BufferedImage getChartImage() throws IOException {
    configure();
    return getBufferedImage(jfreechart);
  }

  protected void configure() {
    configureChart(jfreechart, false);
    configureCategoryDataset();
    configureCategoryAxis();
    configureRenderer();
    configureRangeAxis();
    configureCategoryPlot();
    applyParams();
  }

  protected void configureCategoryPlot() {
    CategoryPlot plot = jfreechart.getCategoryPlot();
    plot.setNoDataMessage(DEFAULT_MESSAGE_NODATA);
    // To remove inner space around chart
    plot.setInsets(RectangleInsets.ZERO_INSETS);
    plot.setDataset(dataset);
    plot.setDomainAxis(categoryAxis);
    plot.setRenderer(renderer);
    plot.setRangeAxis(numberAxis);
  }

  protected void configureCategoryDataset() {
    dataset = new DefaultCategoryDataset();
  }

  protected void configureCategoryAxis() {
    categoryAxis = new CategoryAxis();
    categoryAxis.setLabelFont(DEFAULT_FONT);
    categoryAxis.setLabelPaint(BASE_COLOR);
    categoryAxis.setTickLabelFont(DEFAULT_FONT);
    categoryAxis.setTickLabelPaint(BASE_COLOR);
    categoryAxis.setVisible(false);
  }

  protected void configureRenderer() {
    if (params.get(BaseChartWeb.CHART_PARAM_TYPE).equals(BaseChartWeb.STACKED_BAR_CHART)) {
      renderer = new StackedBarRenderer();
    } else {
      renderer = new BarRenderer();
    }
    renderer.setItemMargin(0.0);
    renderer.setDrawBarOutline(false);
  }

  protected void configureRangeAxis() {
    numberAxis = new NumberAxis();
    numberAxis.setLabelFont(DEFAULT_FONT);
    numberAxis.setLabelPaint(BASE_COLOR);
    numberAxis.setTickLabelFont(DEFAULT_FONT);
    numberAxis.setTickLabelPaint(BASE_COLOR);
    numberAxis.setTickMarksVisible(true);
    numberAxis.setVisible(false);
    numberAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
  }

  protected void applyCommomParamsBar() {
    // -- Plot
    CategoryPlot plot = jfreechart.getCategoryPlot();
    plot.setOrientation(BaseChartWeb.BAR_CHART_VERTICAL.equals(params.get(BaseChartWeb.CHART_PARAM_TYPE))
      || BaseChartWeb.BAR_CHART_VERTICAL_CUSTOM.equals(params.get(BaseChartWeb.CHART_PARAM_TYPE)) ?
        PlotOrientation.VERTICAL :
        PlotOrientation.HORIZONTAL);
    plot.setOutlineVisible("y".equals(params.get(BaseChartWeb.CHART_PARAM_OUTLINE_VISIBLE)));
    plot.setRangeGridlinesVisible("y".equals(params.get(BaseChartWeb.CHART_PARAM_OUTLINE_RANGEGRIDLINES_VISIBLE)));
    String insetsParam = params.get(CHART_PARAM_INSETS);
    if (isParamValueValid(insetsParam)) {
      double insets = convertParamToDouble(insetsParam);
      RectangleInsets rectangleInsets = new RectangleInsets(insets, insets, insets, insets);
      plot.setInsets(rectangleInsets);
    }

    // -- Category Axis
    boolean categoryAxisIsVisible = "y".equals(params.get(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_VISIBLE));
    double categoryAxisUpperMargin = convertParamToDouble(params.get(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_UPPER), DEFAULT_CATEGORIES_AXISMARGIN);
    double categoryAxisLowerMargin = convertParamToDouble(params.get(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_LOWER), DEFAULT_CATEGORIES_AXISMARGIN);
    categoryAxis.setVisible(categoryAxisIsVisible);
    categoryAxis.setTickLabelsVisible(categoryAxisIsVisible);
    categoryAxis.setLowerMargin(categoryAxisLowerMargin);
    categoryAxis.setUpperMargin(categoryAxisUpperMargin);

    // -- Range Axis
    boolean rangeAxisIsVisible = "y".equals(params.get(BaseChartWeb.CHART_PARAM_RANGEAXIS_VISIBLE));
    double rangeAxisUpperMargin = convertParamToDouble(params.get(BaseChartWeb.CHART_PARAM_SERIES_AXISMARGIN_UPPER), DEFAULT_SERIES_AXISMARGIN);
    double rangeAxisLowerMargin = convertParamToDouble(params.get(BaseChartWeb.CHART_PARAM_SERIES_AXISMARGIN_LOWER), DEFAULT_SERIES_AXISMARGIN);
    numberAxis.setTickLabelsVisible(rangeAxisIsVisible);
    numberAxis.setVisible(rangeAxisIsVisible);
    numberAxis.setLowerMargin(rangeAxisLowerMargin);
    numberAxis.setUpperMargin(rangeAxisUpperMargin);
    String rangeMax = params.get(BaseChartWeb.CHART_PARAM_RANGEMAX);
    if (isParamValueValid(rangeMax)) {
      double iRangeMax = Double.parseDouble(rangeMax);
      numberAxis.setRange(0.0, iRangeMax);
    }
    String tickUnit = params.get(BaseChartWeb.CHART_PARAM_SERIES_AXISMARGIN_TICKUNIT);
    if (isParamValueValid(tickUnit)) {
      numberAxis.setTickUnit(new NumberTickUnit(convertParamToDouble(tickUnit)));
    }
  }

  private void applyParams() {
    applyCommonParams();
    applyCommomParamsBar();

    configureColors(params.get(BaseChartWeb.CHART_PARAM_COLORS), renderer);
    addMeasures(params.get(BaseChartWeb.CHART_PARAM_VALUES));
  }

  private void addMeasures(String values) {
    if (values != null && values.length() > 0) {
      // Values
      StringTokenizer stValues = new StringTokenizer(values, ",");
      int nbValues = stValues.countTokens();

      // Categories
      String categoriesParam = params.get(BaseChartWeb.CHART_PARAM_CATEGORIES);
      String[] categoriesSplit;
      if (categoriesParam != null && categoriesParam.length() > 0) {
        categoriesSplit = categoriesParam.split(",");
      } else {
        categoriesSplit = new String[1];
        categoriesSplit[0] = BaseChartWeb.DEFAULT_NAME_CATEGORY;
      }

      // Series
      String seriesParam = params.get(BaseChartWeb.CHART_PARAM_SERIES);
      String[] seriesSplit;
      if (seriesParam != null && seriesParam.length() > 0) {
        seriesSplit = seriesParam.split(",");
      } else {
        seriesSplit = new String[nbValues];
        for (int i = 0; i < nbValues; i++) {
          seriesSplit[i] = BaseChartWeb.DEFAULT_NAME_SERIE + i;
        }
      }

      for (String currentCategory : categoriesSplit) {
        for (String currentSerie : seriesSplit) {
          double currentValue = 0.0;
          if (stValues.hasMoreTokens()) {
            try {
              currentValue = Double.parseDouble(stValues.nextToken());
            } catch (NumberFormatException e) {
              // ignore
            }
          }
          dataset.addValue(currentValue, currentSerie, currentCategory);
        }
      }
    }
  }

}

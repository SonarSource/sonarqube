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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;

public class CustomBarChart extends BarChart {

  private CustomBarRenderer renderer = null;

  public CustomBarChart(Map<String, String> params) {
    super(params);
  }

  @Override
  protected BufferedImage getChartImage() throws IOException {
    configure();
    return getBufferedImage(jfreechart);
  }

  @Override
  protected void configure() {
    configureChart(jfreechart, false);
    configureCategoryDataset();
    configureCategoryAxis();
    configureRenderer();
    configureRangeAxis();
    configureCategoryPlot();
    applyParams();
  }

  @Override
  protected void configureCategoryDataset() {
    dataset = new DefaultCategoryDataset();
  }

  @Override
  protected void configureRenderer() {
    renderer = new CustomBarRenderer(null);
    renderer.setItemMargin(0.0);
    renderer.setDrawBarOutline(false);
  }

  @Override
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

  protected void applyParams() {
    applyCommonParams();
    applyCommomParamsBar();

    configureColors(params.get(CHART_PARAM_COLORS));
    addMeasures(params.get(CHART_PARAM_VALUES));
  }

  private void configureColors(String colorsParam) {
    Paint[] colors = CustomBarRenderer.COLORS;
    if (colorsParam != null && colorsParam.length() > 0) {
      StringTokenizer stColors = new StringTokenizer(colorsParam, ",");
      colors = new Paint[stColors.countTokens()];
      int i = 0;
      while (stColors.hasMoreTokens()) {
        colors[i] = Color.decode("0x" + stColors.nextToken());
        i++;
      }
    }

    renderer.setColors(colors);
  }

  private void addMeasures(String values) {
    if (values != null && values.length() > 0) {
      // Values
      StringTokenizer stValues = new StringTokenizer(values, ",");
      int nbValues = stValues.countTokens();

      // Categories
      String categoriesParam = params.get(CHART_PARAM_CATEGORIES);
      String[] categoriesSplit = null;
      if (categoriesParam != null && categoriesParam.length() > 0) {
        categoriesSplit = categoriesParam.split(",");
      } else {
        categoriesSplit = new String[nbValues];
        for (int i = 0; i < nbValues; i++) {
          categoriesSplit[i] = new StringBuilder().append(DEFAULT_NAME_CATEGORY).append(i).toString();
        }
      }

      // Series
      String[] seriesSplit = {DEFAULT_NAME_SERIE};
      int nbSeries = 1;

      for (String currentCategory : categoriesSplit) {
        for (int iSeries = 0; iSeries < nbSeries; iSeries++) {
          String currentSerie = seriesSplit[iSeries];
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

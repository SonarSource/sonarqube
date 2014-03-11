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

import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomBarChartTest extends BaseChartWebTest {

  @Test
  public void testEmptyParameters() throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    CustomBarChart chart = new CustomBarChart(params);
    BufferedImage img = chart.getChartImage();
    saveChart(img, "custom-horizontal-bar-chart-empty.png");
    assertChartSizeGreaterThan(img, 50);
  }

  @Test
  public void testAllParameters() throws IOException {
    Map<String, String> params = getDefaultParams();
    params.put(BaseChartWeb.CHART_PARAM_TITLE, "JFreeChart by Servlet");
    params.put(BaseChartWeb.CHART_PARAM_DIMENSIONS, "750x250");
    params.put(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_VISIBLE, "y");
    params.put(BaseChartWeb.CHART_PARAM_RANGEAXIS_VISIBLE, "y");
    params.put(BaseChartWeb.CHART_PARAM_TYPE, BaseChartWeb.BAR_CHART_VERTICAL_CUSTOM);
    params.put(BaseChartWeb.CHART_PARAM_VALUES, "6,2,3,7,5,1,9");
    params.put(BaseChartWeb.CHART_PARAM_SERIES, "1,2");
    params.put(BaseChartWeb.CHART_PARAM_CATEGORIES, "0+,5+,10+,20+,30+,60+,90+");
    params.put(BaseChartWeb.CHART_PARAM_COLORS, "FF0000,FF0000,FF0000,CC9900,CC9900,CC9900,00FF00,00FF00,00FF00");
    params.put(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_UPPER, "0.05");
    params.put(BaseChartWeb.CHART_PARAM_SERIES_AXISMARGIN_UPPER, "0.05");
    params.put(BaseChartWeb.CHART_PARAM_INSETS, "20");
    CustomBarChart chart = new CustomBarChart(params);
    BufferedImage img = chart.getChartImage();
    saveChart(img, "custom-horizontal-bar-chart-all.png");
    assertChartSizeGreaterThan(img, 50);
  }

  @Test
  public void testComplexityChart() throws IOException {
    Map<String, String> params = getDefaultParams();
    params.put(BaseChartWeb.CHART_PARAM_DIMENSIONS, "150x100");
    params.put(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_VISIBLE, "y");
    params.put(BaseChartWeb.CHART_PARAM_RANGEAXIS_VISIBLE, "y");
    params.put(BaseChartWeb.CHART_PARAM_TYPE, BaseChartWeb.BAR_CHART_VERTICAL_CUSTOM);
    params.put(BaseChartWeb.CHART_PARAM_VALUES, "6,2,3");
    params.put(BaseChartWeb.CHART_PARAM_CATEGORIES, "0+,5+,10+,20+,30+,60+,90+");
    params.put(BaseChartWeb.CHART_PARAM_COLORS, "4192D9");
    params.put(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_UPPER, "0.05");
    params.put(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_LOWER, "0.05");
    params.put(BaseChartWeb.CHART_PARAM_SERIES_AXISMARGIN_UPPER, "0.2");
    params.put(BaseChartWeb.CHART_PARAM_INSETS, "1");
    params.put(BaseChartWeb.CHART_PARAM_OUTLINE_RANGEGRIDLINES_VISIBLE, "y");
    params.put(BaseChartWeb.CHART_PARAM_OUTLINE_VISIBLE, "y");
    CustomBarChart chart = new CustomBarChart(params);
    BufferedImage img = chart.getChartImage();
    saveChart(img, "custom-horizontal-bar-chart-complexity.png");
    assertChartSizeGreaterThan(img, 50);
  }

  private Map<String, String> getDefaultParams() {
    Map<String, String> params = new HashMap<String, String>();
    params.put(BaseChartWeb.CHART_PARAM_TYPE, BaseChartWeb.BAR_CHART_HORIZONTAL);
    params.put(BaseChartWeb.CHART_PARAM_VALUES, "100,50");
    return params;
  }

}

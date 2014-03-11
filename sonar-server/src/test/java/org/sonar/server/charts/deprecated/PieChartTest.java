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

public class PieChartTest extends BaseChartWebTest {

  @Test
  public void testPieChartDefaultDimensions() throws IOException {
    Map<String, String> params = getDefaultParams();
    PieChart chart = new PieChart(params);
    BufferedImage img = chart.getChartImage();
    saveChart(img, "pie-chart-default.png");
    assertChartSizeGreaterThan(img, 50);
  }

  @Test
  public void testPieChartSpecificDimensions() throws IOException {
    Map<String, String> params = getDefaultParams();
    params.put(BaseChartWeb.CHART_PARAM_DIMENSIONS, "200x200");
    PieChart chart = new PieChart(params);
    BufferedImage img = chart.getChartImage();
    saveChart(img, "pie-chart-specific-dimensions.png");
    assertChartSizeGreaterThan(img, 50);
  }

  @Test
  public void testPieChartOneValue() throws IOException {
    Map<String, String> params = getDefaultParams();
    params.put(BaseChartWeb.CHART_PARAM_VALUES, "100");
    PieChart chart = new PieChart(params);
    BufferedImage img = chart.getChartImage();
    saveChart(img, "pie-chart-one-value.png");
    assertChartSizeGreaterThan(img, 50);
  }

  @Test
  public void testPieChartOthersColors() throws IOException {
    Map<String, String> params = getDefaultParams();
    params.put(BaseChartWeb.CHART_PARAM_COLORS, "FFFF00,9900FF");
    PieChart chart = new PieChart(params);
    BufferedImage img = chart.getChartImage();
    saveChart(img, "pie-chart-others-colors.png");
    assertChartSizeGreaterThan(img, 50);
  }

  @Test
  public void testPieChartNullValues() throws IOException {
    Map<String, String> params = getDefaultParams();
    params.put(BaseChartWeb.CHART_PARAM_VALUES, null);
    PieChart chart = new PieChart(params);
    BufferedImage img = chart.getChartImage();
    saveChart(img, "pie-chart-null-values.png");
    assertChartSizeGreaterThan(img, 50);
  }

  @Test
  public void testPieChartWrongValues() throws IOException {
    Map<String, String> params = getDefaultParams();
    params.put(BaseChartWeb.CHART_PARAM_VALUES, "wrong,value");
    PieChart chart = new PieChart(params);
    BufferedImage img = chart.getChartImage();
    saveChart(img, "pie-chart-wrong-values.png");
    assertChartSizeGreaterThan(img, 50);
  }

  @Test
  public void testPieChartTitle() throws IOException {
    Map<String, String> params = getDefaultParams();
    params.put(BaseChartWeb.CHART_PARAM_TITLE, "JFreeChart by Servlet");
    params.put(BaseChartWeb.CHART_PARAM_DIMENSIONS, "200x200");
    PieChart chart = new PieChart(params);
    BufferedImage img = chart.getChartImage();
    saveChart(img, "pie-chart-title.png");
    assertChartSizeGreaterThan(img, 50);
  }

  private Map<String, String> getDefaultParams() {
    Map<String, String> params = new HashMap<String, String>();
    params.put(BaseChartWeb.CHART_PARAM_TYPE, BaseChartWeb.PIE_CHART);
    params.put(BaseChartWeb.CHART_PARAM_VALUES, "100,50");
    params.put(BaseChartWeb.CHART_PARAM_DIMENSIONS, "50x50");
    return params;
  }

}

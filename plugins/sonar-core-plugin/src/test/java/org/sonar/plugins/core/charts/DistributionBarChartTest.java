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

import org.junit.Test;
import org.sonar.api.charts.ChartParameters;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class DistributionBarChartTest extends AbstractChartTest {

  @Test
  public void simpleSample() throws IOException {
    DistributionBarChart chart = new DistributionBarChart();
    BufferedImage image = chart.generateImage(new ChartParameters("v=0%3D5%3B1%3D22%3B2%3D2"));
    assertChartSizeGreaterThan(image, 1000);
    saveChart(image, "DistributionBarChartTest/simpleSample.png");
  }

  @Test
  public void addXSuffix() throws IOException {
    DistributionBarChart chart = new DistributionBarChart();
    // should suffix x labels with +
    BufferedImage image = chart.generateImage(new ChartParameters("v=0%3D5%3B1%3D22%3B2%3D2&xsuf=%2B"));
    assertChartSizeGreaterThan(image, 1000);
    saveChart(image, "DistributionBarChartTest/addXSuffix.png");
  }

  @Test
  public void addYSuffix() throws IOException {
    DistributionBarChart chart = new DistributionBarChart();
    // should suffix y labels with %
    BufferedImage image = chart.generateImage(new ChartParameters("v=0%3D5%3B1%3D22%3B2%3D2&ysuf=%25"));
    assertChartSizeGreaterThan(image, 1000);
    saveChart(image, "DistributionBarChartTest/addYSuffix.png");
  }

  @Test
  public void manySeries() throws IOException {
    DistributionBarChart chart = new DistributionBarChart();
    BufferedImage image = chart.generateImage(new ChartParameters("v=0%3D5%3B1%3D22%3B2%3D2|0%3D7%3B1%3D15%3B2%3D4"));
    assertChartSizeGreaterThan(image, 1000);
    saveChart(image, "DistributionBarChartTest/manySeries.png");
  }

  @Test
  public void manySeriesIncludingAnEmptySerie() throws IOException {
    DistributionBarChart chart = new DistributionBarChart();
    // the third serie should not have the second default color, but the third one !
    BufferedImage image = chart.generateImage(new ChartParameters("v=0%3D5%3B1%3D22%3B2%3D2||0%3D7%3B1%3D15%3B2%3D4"));
    assertChartSizeGreaterThan(image, 1000);
    saveChart(image, "DistributionBarChartTest/manySeriesIncludingAnEmptySerie.png");
  }

  @Test
  public void overridenSize() throws IOException {
    DistributionBarChart chart = new DistributionBarChart();
    BufferedImage image = chart.generateImage(new ChartParameters("v=0%3D5%3B1%3D22%3B2%3D2|0%3D7%3B1%3D15%3B2%3D4&w=500&h=200"));
    assertChartSizeGreaterThan(image, 1000);
    saveChart(image, "DistributionBarChartTest/overridenSize.png");
  }

  @Test
  public void changeColor() throws IOException {
    DistributionBarChart chart = new DistributionBarChart();
    BufferedImage image = chart.generateImage(new ChartParameters("v=0%3D5%3B1%3D22%3B2%3D2&c=777777&bgc=777777"));
    assertChartSizeGreaterThan(image, 1000);
    saveChart(image, "DistributionBarChartTest/changeColor.png");
  }

  @Test
  public void smallSize() throws IOException {
    DistributionBarChart chart = new DistributionBarChart();
    BufferedImage image = chart.generateImage(new ChartParameters("v=0%3D5%3B1%3D22%3B2%3D2%3B4%3D22%3B5%3D22%3B6%3D22&c=777777&w=120&h=80&fs=8"));
    assertChartSizeGreaterThan(image, 500);
    saveChart(image, "DistributionBarChartTest/smallSize.png");
  }
}

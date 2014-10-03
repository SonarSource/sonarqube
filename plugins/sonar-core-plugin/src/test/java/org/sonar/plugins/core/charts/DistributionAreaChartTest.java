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

public class DistributionAreaChartTest extends AbstractChartTest {

  @Test
  public void oneSerie() throws IOException {
    DistributionAreaChart chart = new DistributionAreaChart();
    BufferedImage image = chart.generateImage(new ChartParameters("v=0%3D5%3B1%3D22%3B2%3D2"));
    assertChartSizeGreaterThan(image, 1000);
    saveChart(image, "DistributionAreaChartTest/oneSerie.png");
  }

  @Test
  public void manySeries() throws IOException {
    DistributionAreaChart chart = new DistributionAreaChart();
    BufferedImage image = chart.generateImage(new ChartParameters("v=0%3D5%3B1%3D22%3B2%3D2|0%3D7%3B1%3D15%3B2%3D4"));
    assertChartSizeGreaterThan(image, 1000);
    saveChart(image, "DistributionAreaChartTest/manySeries.png");
  }

  @Test
  public void manySeriesWithDifferentCategories() throws IOException {
    DistributionAreaChart chart = new DistributionAreaChart();
    BufferedImage image = chart.generateImage(new ChartParameters("v=0%3D5%3B1%3D22%3B2%3D2|2%3D7%3B4%3D15%3B9%3D4"));
    assertChartSizeGreaterThan(image, 1000);
    saveChart(image, "DistributionAreaChartTest/manySeriesWithDifferentCategories.png");
  }

  @Test
  public void manySeriesIncludingAnEmptySerie() throws IOException {
    // the third serie should not have the second default color, but the third one !
    DistributionAreaChart chart = new DistributionAreaChart();
    BufferedImage image = chart.generateImage(new ChartParameters("v=0%3D5%3B1%3D22%3B2%3D2||2%3D7%3B4%3D15%3B9%3D4"));
    assertChartSizeGreaterThan(image, 1000);
    saveChart(image, "DistributionAreaChartTest/manySeriesIncludingAnEmptySerie.png");
  }
}

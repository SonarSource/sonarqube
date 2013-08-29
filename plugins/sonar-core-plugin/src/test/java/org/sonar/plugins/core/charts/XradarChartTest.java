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

import org.junit.Test;
import org.sonar.api.charts.AbstractChartTest;
import org.sonar.api.charts.ChartParameters;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class XradarChartTest extends AbstractChartTest {

  @Test
  public void shouldGenerateXradar() throws IOException {
    String url = "w=200&h=200&l=Usa.,Eff.,Rel.,Main.,Por.&g=0.25&m=100&v=90,80,70,60,50|40,30,20,10,10&c=CAE3F2|F8A036";
    XradarChart radar = new XradarChart();
    BufferedImage img = radar.generateImage(new ChartParameters(url));
    saveChart(img, "shouldGenerateXradar.png");
    assertChartSizeGreaterThan(img, 50);
  }

  @Test
  public void negativeValuesAreNotDisplayed() throws IOException {
    String url = "w=200&h=200&l=Usa.,Eff.,Rel.,Main.,Por.&g=0.3&m=100&v=-90,-80,70,60,50&c=CAE3F2";
    XradarChart radar = new XradarChart();
    BufferedImage img = radar.generateImage(new ChartParameters(url));
    saveChart(img, "negativeValuesAreNotDisplayed.png");

    // you have to check visually that it does not work ! Min value is 0. This is a limitation of JFreeChart.
    assertChartSizeGreaterThan(img, 50);
  }
} 

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
package org.sonar.server.charts;

import org.junit.Test;
import org.sonar.api.charts.Chart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ChartFactoryTest {

  @Test
  public void shouldFindChartsFromPlugins() {
    ChartFactory factory = new ChartFactory(new Chart[]{new FakeChart()});
    Chart chart = factory.getChart("fake");
    assertEquals(chart.getClass(), FakeChart.class);

    assertNull(factory.getChart("unknown"));
  }
}

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

import com.google.common.collect.Maps;
import org.sonar.api.ServerSide;
import org.sonar.api.charts.Chart;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.CheckForNull;

import java.util.Map;

@ServerSide
public final class ChartFactory {
  private static final Logger LOG = Loggers.get(ChartFactory.class);
  private final Map<String, Chart> chartsByKey = Maps.newHashMap();

  public ChartFactory(Chart[] charts) {
    for (Chart chart : charts) {
      if (chartsByKey.containsKey(chart.getKey())) {
        LOG.warn("Duplicated chart key:" + chart.getKey() + ". Existing chart: " + chartsByKey.get(chart.getKey()).getClass().getCanonicalName());

      } else {
        chartsByKey.put(chart.getKey(), chart);
      }
    }
  }

  public ChartFactory() {
    // DO NOT SUPPRESS : used by picocontainer if no charts
  }

  @CheckForNull
  public Chart getChart(String key) {
    return chartsByKey.get(key);
  }
}

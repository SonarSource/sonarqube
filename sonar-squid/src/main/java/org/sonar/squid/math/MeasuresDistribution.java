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

package org.sonar.squid.math;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.sonar.squid.api.SourceCode;
import org.sonar.squid.measures.MetricDef;

public class MeasuresDistribution {

  private final Collection<SourceCode> units;

  public MeasuresDistribution(Collection<SourceCode> units) {
    this.units = units;
  }

  public Map<Integer, Integer> distributeAccordingTo(MetricDef metric, int... thresholds) {
    Map<Integer, Integer> result = new TreeMap<Integer, Integer>();
    for (int threshold : thresholds) {
      result.put(threshold, 0);
    }
    for (SourceCode unit : units) {
      for (int index = thresholds.length - 1; index >= 0; index--) {
        if (unit.getDouble(metric) >= thresholds[index]) {
          result.put(thresholds[index], result.get(thresholds[index]) + 1);
          break;
        }
      }
    }
    return result;
  }
}

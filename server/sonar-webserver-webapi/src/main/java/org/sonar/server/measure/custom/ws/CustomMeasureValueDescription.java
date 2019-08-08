/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.measure.custom.ws;

import com.google.common.base.Joiner;
import org.sonar.api.measures.Metric;

class CustomMeasureValueDescription {
  private CustomMeasureValueDescription() {
    // utility class
  }

  static String measureValueDescription() {
    StringBuilder description = new StringBuilder("Measure value. Value type depends on metric type:");
    description.append("<ul>");
    for (Metric.ValueType metricType : Metric.ValueType.values()) {
      description.append("<li>");
      description.append(String.format("%s - %s", metricType.name(), metricTypeWsDescription(metricType)));
      description.append("</li>");
    }
    description.append("</ul>");

    return description.toString();
  }

  private static String metricTypeWsDescription(Metric.ValueType metricType) {
    switch (metricType) {
      case BOOL:
        return "the possible values are true or false";
      case INT:
      case MILLISEC:
        return "type: integer";
      case FLOAT:
      case PERCENT:
      case RATING:
        return "type: double";
      case LEVEL:
        return "the possible values are " + formattedMetricLevelNames();
      case STRING:
      case DATA:
      case DISTRIB:
        return "type: string";
      case WORK_DUR:
        return "long representing the number of minutes";
      default:
        return "metric type not supported";
    }
  }

  private static String formattedMetricLevelNames() {
    return Joiner.on(", ").join(Metric.Level.names());
  }
}

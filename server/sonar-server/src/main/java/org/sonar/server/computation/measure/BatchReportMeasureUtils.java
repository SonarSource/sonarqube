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

package org.sonar.server.computation.measure;

import org.sonar.batch.protocol.output.BatchReport;

import javax.annotation.CheckForNull;

public class BatchReportMeasureUtils {
  private BatchReportMeasureUtils() {
    // static methods only
  }

  /**
   * return the numerical value as a double. It's the type used in db.
   * Returns null if no numerical value found
   */
  @CheckForNull
  public static Double valueAsDouble(BatchReport.Measure measure) {
    switch (measure.getValueType()) {
      case BOOLEAN:
        return measure.getBooleanValue() ? 1.0d : 0.0d;
      case INT:
        return Double.valueOf(measure.getIntValue());
      case LONG:
        return Double.valueOf(measure.getLongValue());
      case DOUBLE:
        return measure.getDoubleValue();
      default:
        return null;
    }
  }

  /**
   * check that measure has a value (numerical or string) and a metric key
   */
  public static void checkMeasure(BatchReport.Measure measure) {
    if (!measure.hasValueType() || !measure.hasMetricKey()) {
      throw newIllegalStateException(measure);
    }

    boolean hasValueOrData = false;
    switch (measure.getValueType()) {
      case DOUBLE:
        hasValueOrData = measure.hasDoubleValue();
        break;
      case INT:
        hasValueOrData = measure.hasIntValue();
        break;
      case LONG:
        hasValueOrData = measure.hasLongValue();
        break;
      case STRING:
        hasValueOrData = measure.hasStringValue();
        break;
      case BOOLEAN:
        hasValueOrData = measure.hasBooleanValue();
        break;
    }

    if (!hasValueOrData) {
      throw newIllegalStateException(measure);
    }
  }

  private static IllegalStateException newIllegalStateException(BatchReport.Measure measure) {
    return new IllegalStateException(String.format("Measure %s does not have value", measure));
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.measure.ws;

import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.Measure;

import static org.sonar.server.measure.ws.MeasureValueFormatter.formatMeasureValue;
import static org.sonar.server.measure.ws.MeasureValueFormatter.formatNumericalValue;

class MeasureDtoToWsMeasure {

  private MeasureDtoToWsMeasure() {
    // static methods
  }

  /**
   * @see #measureDtoToWsMeasure(MetricDto, MeasureDto)
   * add component uuid to the WS Measure object
   *
   */
  static Measure dbToWsMeasure(MeasureDto dbMeasure, MetricDto dbMetric, ComponentDto componentDto) {
    return map(dbMetric, dbMeasure).setComponent(componentDto.getKey()).build();
  }

  static Measure measureDtoToWsMeasure(MetricDto metricDto, MeasureDto measureDto) {
    return map(metricDto, measureDto).build();
  }

  private static Measure.Builder map(MetricDto dbMetric, MeasureDto dbMeasure) {
    try {
      Measure.Builder measure = Measure.newBuilder();
      measure.setMetric(dbMetric.getKey());
      // a measure value can be null, new_violations metric for example
      if (dbMeasure.getValue() != null
        || dbMeasure.getData() != null) {
        measure.setValue(formatMeasureValue(dbMeasure, dbMetric));
      }

      WsMeasures.PeriodValue.Builder periodBuilder = WsMeasures.PeriodValue.newBuilder();
      for (int i = 1; i <= 5; i++) {
        if (dbMeasure.getVariation(i) != null) {
          measure.getPeriodsBuilder().addPeriodsValue(periodBuilder
            .clear()
            .setIndex(i)
            .setValue(formatNumericalValue(dbMeasure.getVariation(i), dbMetric)));
        }
      }

      return measure;
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error while mapping a measure of metric key '%s' and parameters %s", dbMetric.getKey(), dbMeasure.toString()), e);
    }
  }
}

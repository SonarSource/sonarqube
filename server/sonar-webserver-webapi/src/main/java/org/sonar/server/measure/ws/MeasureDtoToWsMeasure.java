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
package org.sonar.server.measure.ws;

import javax.annotation.Nullable;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.Measure;

import static java.lang.Double.compare;
import static java.util.Optional.ofNullable;
import static org.sonar.server.measure.ws.MeasureValueFormatter.formatMeasureValue;
import static org.sonar.server.measure.ws.MeasureValueFormatter.formatNumericalValue;

class MeasureDtoToWsMeasure {

  private MeasureDtoToWsMeasure() {
    // static methods
  }

  static void updateMeasureBuilder(Measure.Builder measureBuilder, MetricDto metricDto, MeasureDto measureDto) {
    Double value = measureDto.getValue();
    Double variation = measureDto.getVariation();
    updateMeasureBuilder(measureBuilder, metricDto, value == null ? Double.NaN : value, measureDto.getData(), variation == null ? Double.NaN : variation);
  }

  static void updateMeasureBuilder(Measure.Builder measureBuilder, MetricDto metricDto, LiveMeasureDto measureDto) {
    Double value = measureDto.getValue();
    Double variation = measureDto.getVariation();
    updateMeasureBuilder(measureBuilder, metricDto, value == null ? Double.NaN : value, measureDto.getDataAsString(), variation == null ? Double.NaN : variation);
  }

  static void updateMeasureBuilder(Measure.Builder measureBuilder, MetricDto metric, double doubleValue, @Nullable String stringValue, double variation) {
    measureBuilder.setMetric(metric.getKey());
    Double bestValue = metric.getBestValue();
    // a measure value can be null, new_violations metric for example
    if (!Double.isNaN(doubleValue) || stringValue != null) {
      measureBuilder.setValue(formatMeasureValue(doubleValue, stringValue, metric));
      ofNullable(bestValue).ifPresent(v -> measureBuilder.setBestValue(compare(doubleValue, v) == 0));
    }

    Measures.PeriodValue.Builder periodBuilder = Measures.PeriodValue.newBuilder();
    if (Double.isNaN(variation)) {
      return;
    }
    Measures.PeriodValue.Builder builderForValue = periodBuilder
      .clear()
      .setIndex(1)
      .setValue(formatNumericalValue(variation, metric));
    ofNullable(bestValue).ifPresent(v -> builderForValue.setBestValue(compare(variation, v) == 0));
    measureBuilder.getPeriodsBuilder().addPeriodsValue(builderForValue);
  }
}

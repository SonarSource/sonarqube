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

import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.WsMeasures;

import static org.sonar.server.measure.ws.MeasureValueFormatter.formatNumericalValue;
import static org.sonar.server.measure.ws.MeasureValueFormatter.formatMeasureValue;

class MeasureDtoToWsMeasure {

  private MeasureDtoToWsMeasure() {
    // static methods
  }

  static WsMeasures.Measure measureDtoToWsMeasure(MetricDto metricDto, MeasureDto measureDto) {
    WsMeasures.Measure.Builder measure = WsMeasures.Measure.newBuilder();
    measure.setMetric(metricDto.getKey());
    // a measure value can be null, new_violations metric for example
    if (measureDto.getValue() != null
      || measureDto.getData()!=null) {
      measure.setValue(formatMeasureValue(measureDto, metricDto));
    }
    if (measureDto.getVariation(1) != null) {
      measure.setVariationValueP1(formatNumericalValue(measureDto.getVariation(1), metricDto));
    }
    if (measureDto.getVariation(2) != null) {
      measure.setVariationValueP2(formatNumericalValue(measureDto.getVariation(2), metricDto));
    }
    if (measureDto.getVariation(3) != null) {
      measure.setVariationValueP3(formatNumericalValue(measureDto.getVariation(3), metricDto));
    }
    if (measureDto.getVariation(4) != null) {
      measure.setVariationValueP4(formatNumericalValue(measureDto.getVariation(4), metricDto));
    }
    if (measureDto.getVariation(5) != null) {
      measure.setVariationValueP5(formatNumericalValue(measureDto.getVariation(5), metricDto));
    }

    return measure.build();
  }
}

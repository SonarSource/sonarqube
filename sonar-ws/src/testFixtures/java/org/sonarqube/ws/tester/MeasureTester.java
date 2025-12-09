/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonarqube.ws.tester;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.client.measures.ComponentRequest;

import static java.lang.Double.parseDouble;
import static java.util.Collections.singletonList;

public class MeasureTester {

  private final TesterSession session;

  MeasureTester(TesterSession session) {
    this.session = session;
  }


  @CheckForNull
  public Measure getMeasure(String componentKey, String metricKey) {
    return getMeasuresByMetricKey(componentKey, metricKey).get(metricKey);
  }

  @CheckForNull
  public Double getMeasureAsDouble(String componentKey, String metricKey) {
    Measure measure = getMeasure(componentKey, metricKey);
    return (measure == null) ? null : parseDouble(measure.getValue());
  }

  public Map<String, Measure> getMeasuresByMetricKey(String componentKey, String... metricKeys) {
    return getStreamMeasures(componentKey, metricKeys)
      .filter(Measure::hasValue)
      .collect(Collectors.toMap(Measure::getMetric, Function.identity()));
  }

  public Map<String, Double> getMeasuresAsDoubleByMetricKey(String componentKey, String... metricKeys) {
    return getStreamMeasures(componentKey, metricKeys)
      .filter(Measure::hasValue)
      .collect(Collectors.toMap(Measure::getMetric, measure -> parseDouble(measure.getValue())));
  }

  private Stream<Measure> getStreamMeasures(String componentKey, String... metricKeys) {
    return session.wsClient().measures().component(new ComponentRequest()
      .setComponent(componentKey)
      .setMetricKeys(Arrays.asList(metricKeys)))
      .getComponent().getMeasuresList()
      .stream();
  }

  @CheckForNull
  public Measure getMeasureWithVariation(String componentKey, String metricKey) {
    Measures.ComponentWsResponse response = session.wsClient().measures().component(new ComponentRequest()
      .setComponent(componentKey)
      .setMetricKeys(singletonList(metricKey))
      .setAdditionalFields(singletonList("period")));
    List<Measure> measures = response.getComponent().getMeasuresList();
    return measures.size() == 1 ? measures.get(0) : null;
  }

  @CheckForNull
  public Map<String, Measure> getMeasuresWithVariationsByMetricKey(String componentKey, String... metricKeys) {
    return session.wsClient().measures().component(new ComponentRequest()
      .setComponent(componentKey)
      .setMetricKeys(Arrays.asList(metricKeys))
      .setAdditionalFields(singletonList("period"))).getComponent().getMeasuresList()
      .stream()
      .collect(Collectors.toMap(Measure::getMetric, Function.identity()));
  }

  /**
   * Return leak period value
   */
  @CheckForNull
  public Double getLeakPeriodValue(String componentKey, String metricKey) {
    Measures.PeriodValue periodsValue = getMeasureWithVariation(componentKey, metricKey).getPeriod();
    return parseDouble(periodsValue.getValue());
  }

}

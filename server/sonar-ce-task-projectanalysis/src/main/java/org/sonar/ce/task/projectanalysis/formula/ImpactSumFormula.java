/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.formula;

import java.util.Optional;
import org.sonar.ce.task.projectanalysis.measure.Measure;

import static java.util.Objects.requireNonNull;

public class ImpactSumFormula implements Formula<ImpactSumFormula.ImpactCounter> {

  private final String metricKey;


  private ImpactSumFormula(String metricKey) {
    this.metricKey = requireNonNull(metricKey, "Metric key cannot be null");
  }

  @Override
  public ImpactSumFormula.ImpactCounter createNewCounter() {
    return new ImpactSumFormula.ImpactCounter();
  }

  public static ImpactSumFormula createImpactSumFormula(String metricKey) {
    return new ImpactSumFormula(metricKey);
  }

  @Override
  public Optional<Measure> createMeasure(ImpactCounter counter, CreateMeasureContext context) {
    return counter.getValue().map(v -> Measure.newMeasureBuilder().create(v));
  }

  @Override
  public String[] getOutputMetricKeys() {
    return new String[]{metricKey};
  }

  class ImpactCounter implements Counter<ImpactSumFormula.ImpactCounter> {

    private boolean initialized = false;
    private boolean hasEmptyValue = false;
    private final MeasureImpactBuilder measureImpactBuilder = new MeasureImpactBuilder();

    @Override
    public void aggregate(ImpactSumFormula.ImpactCounter counter) {
      Optional<String> value = counter.getValue();
      if (value.isPresent()) {
        initialized = true;
        measureImpactBuilder.add(value.get());
      } else {
        hasEmptyValue = true;
      }
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      Optional<Measure> measureOptional = context.getMeasure(metricKey);
      String data = measureOptional.map(Measure::getData).orElse(null);
      if (data != null) {
        initialized = true;
        measureImpactBuilder.add(data);
      } else {
        hasEmptyValue = true;
      }
    }

    public Optional<String> getValue() {
      if (initialized && !hasEmptyValue) {
        return Optional.ofNullable(measureImpactBuilder.build());
      }
      return Optional.empty();
    }
  }
}

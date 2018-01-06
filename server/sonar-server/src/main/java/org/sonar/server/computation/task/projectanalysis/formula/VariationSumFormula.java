/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.formula;

import com.google.common.base.Optional;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.formula.counter.DoubleValue;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

/**
 * A Formula which aggregates variations of a specific metric by simply making the sums of its variations. It supports
 * make the sum of only specific periods.
 */
public class VariationSumFormula implements Formula<VariationSumFormula.VariationSumCounter> {
  private final String metricKey;

  public VariationSumFormula(String metricKey) {
    this.metricKey = requireNonNull(metricKey, "Metric key cannot be null");
  }

  @Override
  public VariationSumCounter createNewCounter() {
    return new VariationSumCounter(metricKey);
  }

  @Override
  public Optional<Measure> createMeasure(VariationSumCounter counter, CreateMeasureContext context) {
    if (!CrawlerDepthLimit.LEAVES.isDeeperThan(context.getComponent().getType()) || !counter.doubleValue.isSet()) {
      return Optional.absent();
    }
    return Optional.of(newMeasureBuilder().setVariation(counter.doubleValue.getValue()).createNoValue());
  }

  @Override
  public String[] getOutputMetricKeys() {
    return new String[] {metricKey};
  }

  public static final class VariationSumCounter implements Counter<VariationSumCounter> {
    private final DoubleValue doubleValue = new DoubleValue();
    private final String metricKey;

    private VariationSumCounter(String metricKey) {
      this.metricKey = metricKey;
    }

    @Override
    public void aggregate(VariationSumCounter counter) {
      doubleValue.increment(counter.doubleValue);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      Optional<Measure> measure = context.getMeasure(metricKey);
      if (!measure.isPresent() || !measure.get().hasVariation()) {
        return;
      }
      double variation = measure.get().getVariation();
      doubleValue.increment(variation);
    }

  }
}

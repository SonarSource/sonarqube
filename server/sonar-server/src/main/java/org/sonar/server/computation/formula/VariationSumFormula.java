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
package org.sonar.server.computation.formula;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.formula.counter.DoubleVariationValue;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.period.Period;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

/**
 * A Formula which aggregates variations of a specific metric by simply making the sums of its variations. It supports
 * make the sum of only specific periods.
 */
public class VariationSumFormula implements Formula<VariationSumFormula.VariationSumCounter> {
  private final String metricKey;
  private final Predicate<Period> supportedPeriods;

  public VariationSumFormula(String metricKey, Predicate<Period> supportedPeriods) {
    this.supportedPeriods = supportedPeriods;
    this.metricKey = requireNonNull(metricKey, "Metric key cannot be null");
  }

  @Override
  public VariationSumCounter createNewCounter() {
    return new VariationSumCounter(metricKey, supportedPeriods);
  }

  @Override
  public Optional<Measure> createMeasure(VariationSumCounter counter, CreateMeasureContext context) {
    if (!CrawlerDepthLimit.LEAVES.isDeeperThan(context.getComponent().getType())) {
      return Optional.absent();
    }
    MeasureVariations.Builder variations = createAndPopulateBuilder(counter.array, context);
    if (variations.isEmpty()) {
      return Optional.absent();
    }
    return Optional.of(newMeasureBuilder().setVariations(variations.build()).createNoValue());
  }

  private MeasureVariations.Builder createAndPopulateBuilder(DoubleVariationValue.Array array, CreateMeasureContext context) {
    MeasureVariations.Builder builder = MeasureVariations.newMeasureVariationsBuilder();
    for (Period period : from(context.getPeriods()).filter(supportedPeriods)) {
      DoubleVariationValue elements = array.get(period);
      if (elements.isSet()) {
        builder.setVariation(period, elements.getValue());
      }
    }
    return builder;
  }

  @Override
  public String[] getOutputMetricKeys() {
    return new String[] {metricKey};
  }

  public static final class VariationSumCounter implements Counter<VariationSumCounter> {
    private final DoubleVariationValue.Array array = DoubleVariationValue.newArray();
    private final String metricKey;
    private final Predicate<Period> supportedPeriods;

    private VariationSumCounter(String metricKey, Predicate<Period> supportedPeriods) {
      this.metricKey = metricKey;
      this.supportedPeriods = supportedPeriods;
    }

    @Override
    public void aggregate(VariationSumCounter counter) {
      array.incrementAll(counter.array);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      Optional<Measure> measure = context.getMeasure(metricKey);
      if (!measure.isPresent() || !measure.get().hasVariations()) {
        return;
      }
      MeasureVariations variations = measure.get().getVariations();
      for (Period period : from(context.getPeriods()).filter(supportedPeriods)) {
        if (variations.hasVariation(period.getIndex())) {
          double variation = variations.getVariation(period.getIndex());
          if (variation > 0) {
            array.increment(period, variations.getVariation(period.getIndex()));
          }
        }
      }
    }
  }
}

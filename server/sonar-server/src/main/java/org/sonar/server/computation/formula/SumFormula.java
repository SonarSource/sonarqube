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
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.formula.counter.IntSumCounter;
import org.sonar.server.computation.formula.counter.LongSumCounter;
import org.sonar.server.computation.formula.counter.SumCounter;
import org.sonar.server.computation.measure.Measure;

import static java.util.Objects.requireNonNull;

public abstract class SumFormula<T extends SumCounter<U, T>, U extends Number> implements Formula<T> {
  protected final String metricKey;

  public SumFormula(String metricKey) {
    this.metricKey = requireNonNull(metricKey, "Metric key cannot be null");
  }

  public static IntSumFormula createIntSumFormula(String metricKey) {
    return new IntSumFormula(metricKey);
  }

  public static class IntSumFormula extends SumFormula<IntSumCounter, Integer> {
    private IntSumFormula(String metricKey) {
      super(metricKey);
    }

    @Override
    public IntSumCounter createNewCounter() {
      return new IntSumCounter(metricKey);
    }

    @Override
    public Optional<Measure> createMeasure(IntSumCounter counter, CreateMeasureContext context) {
      Optional<Integer> valueOptional = counter.getValue();
      if (shouldCreateMeasure(context, valueOptional)) {
        return Optional.of(Measure.newMeasureBuilder().create(valueOptional.get()));
      }
      return Optional.absent();
    }
  }

  public static LongSumFormula createLongSumFormula(String metricKey) {
    return new LongSumFormula(metricKey);
  }

  public static class LongSumFormula extends SumFormula<LongSumCounter, Long> {
    private LongSumFormula(String metricKey) {
      super(metricKey);
    }

    @Override
    public LongSumCounter createNewCounter() {
      return new LongSumCounter(metricKey);
    }

    @Override
    public Optional<Measure> createMeasure(LongSumCounter counter, CreateMeasureContext context) {
      Optional<Long> valueOptional = counter.getValue();
      if (shouldCreateMeasure(context, valueOptional)) {
        return Optional.of(Measure.newMeasureBuilder().create(valueOptional.get()));
      }
      return Optional.absent();
    }
  }

  private static <T extends Number> boolean shouldCreateMeasure(CreateMeasureContext context, Optional<T> value) {
    return value.isPresent() && CrawlerDepthLimit.LEAVES.isDeeperThan(context.getComponent().getType());
  }

  @Override
  public String[] getOutputMetricKeys() {
    return new String[] {metricKey};
  }
}

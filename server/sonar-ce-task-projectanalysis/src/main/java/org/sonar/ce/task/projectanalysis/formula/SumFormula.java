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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.formula.counter.IntSumCounter;
import org.sonar.ce.task.projectanalysis.formula.counter.LongSumCounter;
import org.sonar.ce.task.projectanalysis.formula.counter.SumCounter;
import org.sonar.ce.task.projectanalysis.measure.Measure;

import static java.util.Objects.requireNonNull;

public abstract class SumFormula<T extends SumCounter<U, T>, U extends Number> implements Formula<T> {
  protected final String metricKey;
  @CheckForNull
  protected final U defaultInputValue;

  public SumFormula(String metricKey, @Nullable U defaultInputValue) {
    this.metricKey = requireNonNull(metricKey, "Metric key cannot be null");
    this.defaultInputValue = defaultInputValue;
  }

  public static IntSumFormula createIntSumFormula(String metricKey) {
    return createIntSumFormula(metricKey, null);
  }

  public static IntSumFormula createIntSumFormula(String metricKey, @Nullable Integer defaultInputValue) {
    return new IntSumFormula(metricKey, defaultInputValue);
  }

  public static class IntSumFormula extends SumFormula<IntSumCounter, Integer> {
    private IntSumFormula(String metricKey, @Nullable Integer defaultInputValue) {
      super(metricKey, defaultInputValue);
    }

    @Override
    public IntSumCounter createNewCounter() {
      return new IntSumCounter(metricKey, defaultInputValue);
    }

    @Override
    public Optional<Measure> createMeasure(IntSumCounter counter, CreateMeasureContext context) {
      Optional<Integer> valueOptional = counter.getValue();
      Integer value = valueOptional.orElse(null);
      if (shouldCreateMeasure(context, value)) {
        return Optional.of(Measure.newMeasureBuilder().create(value));
      }
      return Optional.empty();
    }
  }

  public static LongSumFormula createLongSumFormula(String metricKey) {
    return createLongSumFormula(metricKey, null);
  }

  public static LongSumFormula createLongSumFormula(String metricKey, @Nullable Long defaultInputValue) {
    return new LongSumFormula(metricKey, defaultInputValue);
  }

  public static class LongSumFormula extends SumFormula<LongSumCounter, Long> {
    private LongSumFormula(String metricKey, @Nullable Long defaultInputValue) {
      super(metricKey, defaultInputValue);
    }

    @Override
    public LongSumCounter createNewCounter() {
      return new LongSumCounter(metricKey, defaultInputValue);
    }

    @Override
    public Optional<Measure> createMeasure(LongSumCounter counter, CreateMeasureContext context) {
      Optional<Long> valueOptional = counter.getValue();
      Long value = valueOptional.orElse(null);
      if (shouldCreateMeasure(context, value)) {

        return Optional.of(Measure.newMeasureBuilder().create(value));
      }
      return Optional.empty();
    }
  }

  protected static <T extends Number> boolean shouldCreateMeasure(CreateMeasureContext context, @Nullable T value) {
    return value != null && CrawlerDepthLimit.LEAVES.isDeeperThan(context.getComponent().getType());
  }

  @Override
  public String[] getOutputMetricKeys() {
    return new String[] {metricKey};
  }
}

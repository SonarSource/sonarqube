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
import org.sonar.server.computation.task.projectanalysis.measure.Measure;

import static java.util.Objects.requireNonNull;

public class AverageFormula implements Formula<AverageFormula.AverageCounter> {

  private final String outputMetricKey;

  private final String mainMetric;
  private final String byMetric;

  private AverageFormula(Builder builder) {
    this.outputMetricKey = builder.outputMetricKey;
    this.mainMetric = builder.mainMetric;
    this.byMetric = builder.byMetric;
  }

  @Override
  public AverageCounter createNewCounter() {
    return new AverageCounter();
  }

  @Override
  public Optional<Measure> createMeasure(AverageCounter counter, CreateMeasureContext context) {
    Optional<Double> mainValueOptional = counter.getMainValue();
    Optional<Double> byValueOptional = counter.getByValue();
    if (mainValueOptional.isPresent() && byValueOptional.isPresent()) {
      double mainValue = mainValueOptional.get();
      double byValue = byValueOptional.get();
      if (byValue > 0d) {
        return Optional.of(Measure.newMeasureBuilder().create(mainValue / byValue, context.getMetric().getDecimalScale()));
      }
    }
    return Optional.absent();
  }

  @Override
  public String[] getOutputMetricKeys() {
    return new String[] {outputMetricKey};
  }

  public static class Builder {
    private String outputMetricKey;
    private String mainMetric;
    private String byMetric;

    private Builder() {
      // prevents instantiation outside static method
    }

    public static Builder newBuilder() {
      return new Builder();
    }

    public Builder setOutputMetricKey(String m) {
      this.outputMetricKey = m;
      return this;
    }

    public Builder setMainMetricKey(String m) {
      this.mainMetric = m;
      return this;
    }

    public Builder setByMetricKey(String m) {
      this.byMetric = m;
      return this;
    }

    public AverageFormula build() {
      requireNonNull(outputMetricKey, "Output metric key cannot be null");
      requireNonNull(mainMetric, "Main metric Key cannot be null");
      requireNonNull(byMetric, "By metric Key cannot be null");
      return new AverageFormula(this);
    }
  }

  class AverageCounter implements Counter<AverageCounter> {

    private boolean initialized = false;

    private double mainValue = 0d;
    private double byValue = 0d;

    @Override
    public void aggregate(AverageCounter counter) {
      addValuesIfPresent(counter.getMainValue(), counter.getByValue());
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      Optional<Double> mainValueOptional = getDoubleValue(context.getMeasure(mainMetric));
      Optional<Double> byValueOptional = getDoubleValue(context.getMeasure(byMetric));
      addValuesIfPresent(mainValueOptional, byValueOptional);
    }

    private void addValuesIfPresent(Optional<Double> counterMainValue, Optional<Double> counterByValue) {
      if (counterMainValue.isPresent() && counterByValue.isPresent()) {
        initialized = true;
        mainValue += counterMainValue.get();
        byValue += counterByValue.get();
      }
    }

    public Optional<Double> getMainValue() {
      return getValue(mainValue);
    }

    public Optional<Double> getByValue() {
      return getValue(byValue);
    }

    private Optional<Double> getValue(double value) {
      if (initialized) {
        return Optional.of(value);
      }
      return Optional.absent();
    }

    private Optional<Double> getDoubleValue(Optional<Measure> measureOptional) {
      if (!measureOptional.isPresent()) {
        return Optional.absent();
      }
      Measure measure = measureOptional.get();
      switch (measure.getValueType()) {
        case DOUBLE:
          return Optional.of(measure.getDoubleValue());
        case LONG:
          return Optional.of((double) measure.getLongValue());
        case INT:
          return Optional.of((double) measure.getIntValue());
        case NO_VALUE:
          return Optional.absent();
        default:
          throw new IllegalArgumentException(String.format("Measure of type '%s' are not supported", measure.getValueType().name()));
      }
    }
  }
}

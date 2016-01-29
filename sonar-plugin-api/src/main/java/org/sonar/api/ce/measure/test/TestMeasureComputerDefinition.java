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
package org.sonar.api.ce.measure.test;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class TestMeasureComputerDefinition implements MeasureComputer.MeasureComputerDefinition {

  private final Set<String> inputMetricKeys;
  private final Set<String> outputMetrics;

  private TestMeasureComputerDefinition(MeasureComputerDefinitionBuilderImpl builder) {
    this.inputMetricKeys = ImmutableSet.copyOf(builder.inputMetricKeys);
    this.outputMetrics = ImmutableSet.copyOf(builder.outputMetrics);
  }

  @Override
  public Set<String> getInputMetrics() {
    return inputMetricKeys;
  }

  @Override
  public Set<String> getOutputMetrics() {
    return outputMetrics;
  }

  public static class MeasureComputerDefinitionBuilderImpl implements Builder {

    private String[] inputMetricKeys = new String[] {};
    private String[] outputMetrics;

    @Override
    public Builder setInputMetrics(String... inputMetrics) {
      this.inputMetricKeys = validateInputMetricKeys(inputMetrics);
      return this;
    }

    @Override
    public Builder setOutputMetrics(String... outputMetrics) {
      this.outputMetrics = validateOutputMetricKeys(outputMetrics);
      return this;
    }

    @Override
    public MeasureComputer.MeasureComputerDefinition build() {
      validateInputMetricKeys(this.inputMetricKeys);
      validateOutputMetricKeys(this.outputMetrics);
      return new TestMeasureComputerDefinition(this);
    }

    private static String[] validateInputMetricKeys(@Nullable String[] inputMetrics) {
      requireNonNull(inputMetrics, "Input metrics cannot be null");
      checkNotNull(inputMetrics);
      return inputMetrics;
    }

    private static String[] validateOutputMetricKeys(@Nullable String[] outputMetrics) {
      requireNonNull(outputMetrics, "Output metrics cannot be null");
      checkArgument(outputMetrics.length > 0, "At least one output metric must be defined");
      FluentIterable.from(CoreMetrics.getMetrics()).transform(MetricToKey.INSTANCE).filter(new MatchMetricKey(outputMetrics)).size();
      checkNotNull(outputMetrics);
      return outputMetrics;
    }

    private static void checkNotNull(String[] metrics) {
      for (String metric : metrics) {
        requireNonNull(metric, "Null metric is not allowed");
      }
    }
  }

  private enum MetricToKey implements Function<Metric, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull Metric input) {
      return input.getKey();
    }
  }

  private static class MatchMetricKey implements Predicate<String> {

    private final List<String> metrics;

    public MatchMetricKey(String[] metrics) {
      this.metrics = Arrays.asList(metrics);
    }

    @Override
    public boolean apply(@Nonnull String metric) {
      checkArgument(!metrics.contains(metric), "Core metrics are not allowed");
      return true;
    }
  }
}

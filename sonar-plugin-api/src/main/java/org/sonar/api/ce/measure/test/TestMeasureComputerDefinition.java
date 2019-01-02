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
package org.sonar.api.ce.measure.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

public class TestMeasureComputerDefinition implements MeasureComputer.MeasureComputerDefinition {

  private final Set<String> inputMetricKeys;
  private final Set<String> outputMetrics;

  private TestMeasureComputerDefinition(MeasureComputerDefinitionBuilderImpl builder) {
    this.inputMetricKeys = unmodifiableSet(new HashSet<>(asList(builder.inputMetricKeys)));
    this.outputMetrics = unmodifiableSet(new HashSet<>(asList(builder.outputMetrics)));
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

    private static String[] validateInputMetricKeys(String[] inputMetrics) {
      requireNonNull(inputMetrics, "Input metrics cannot be null");
      checkNotNull(inputMetrics);
      return inputMetrics;
    }

    private static String[] validateOutputMetricKeys(String[] outputMetrics) {
      requireNonNull(outputMetrics, "Output metrics cannot be null");
      checkArgument(outputMetrics.length > 0, "At least one output metric must be defined");

      List<String> outputMetricKeys = asList(outputMetrics);
      CoreMetrics.getMetrics().stream()
        .map(Metric::getKey)
        .forEach(metricKey -> checkArgument(!outputMetricKeys.contains(metricKey), "Core metrics are not allowed"));
      checkNotNull(outputMetrics);
      return outputMetrics;
    }

    private static void checkNotNull(String[] metrics) {
      for (String metric : metrics) {
        requireNonNull(metric, "Null metric is not allowed");
      }
    }
  }
}

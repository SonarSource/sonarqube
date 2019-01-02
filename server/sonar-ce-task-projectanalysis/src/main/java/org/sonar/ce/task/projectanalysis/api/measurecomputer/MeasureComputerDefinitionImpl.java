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
package org.sonar.ce.task.projectanalysis.api.measurecomputer;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.ce.measure.MeasureComputer;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class MeasureComputerDefinitionImpl implements MeasureComputer.MeasureComputerDefinition {

  private final Set<String> inputMetricKeys;
  private final Set<String> outputMetrics;

  private MeasureComputerDefinitionImpl(BuilderImpl builder) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MeasureComputerDefinitionImpl that = (MeasureComputerDefinitionImpl) o;

    if (!inputMetricKeys.equals(that.inputMetricKeys)) {
      return false;
    }
    return outputMetrics.equals(that.outputMetrics);
  }

  @Override
  public int hashCode() {
    int result = inputMetricKeys.hashCode();
    result = 31 * result + outputMetrics.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "MeasureComputerDefinitionImpl{" +
      "inputMetricKeys=" + inputMetricKeys +
      ", outputMetrics=" + outputMetrics +
      '}';
  }

  public static class BuilderImpl implements Builder {

    private String[] inputMetricKeys = new String[] {};
    @CheckForNull
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
      return new MeasureComputerDefinitionImpl(this);
    }

    private static String[] validateInputMetricKeys(@Nullable String[] inputMetrics) {
      requireNonNull(inputMetrics, "Input metrics cannot be null");
      checkNotNull(inputMetrics);
      return inputMetrics;
    }

    private static String[] validateOutputMetricKeys(@Nullable String[] outputMetrics) {
      requireNonNull(outputMetrics, "Output metrics cannot be null");
      checkArgument(outputMetrics.length > 0, "At least one output metric must be defined");
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

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

package org.sonar.server.computation.measure.api;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sonar.api.ce.measure.MeasureComputer;

import static com.google.common.base.Preconditions.checkArgument;

public class MeasureComputerImpl implements MeasureComputer {

  private final Set<String> inputMetricKeys;
  private final Set<String> outputMetrics;
  private final Implementation implementation;

  public MeasureComputerImpl(MeasureComputerBuilderImpl builder) {
    this.inputMetricKeys = ImmutableSet.copyOf(builder.inputMetricKeys);
    this.outputMetrics = ImmutableSet.copyOf(builder.outputMetrics);
    this.implementation = builder.measureComputerImplementation;
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
  public Implementation getImplementation() {
    return implementation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MeasureComputerImpl that = (MeasureComputerImpl) o;

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
    return "MeasureComputerImpl{" +
      "inputMetricKeys=" + inputMetricKeys +
      ", outputMetrics=" + outputMetrics +
      ", implementation=" + implementation +
      '}';
  }

  public static class MeasureComputerBuilderImpl implements MeasureComputerBuilder {

    private String[] inputMetricKeys;
    private String[] outputMetrics;
    private Implementation measureComputerImplementation;

    @Override
    public MeasureComputerBuilder setInputMetrics(String... inputMetrics) {
      checkArgument(inputMetrics != null && inputMetrics.length > 0, "At least one input metric must be defined");
      this.inputMetricKeys = inputMetrics;
      return this;
    }

    @Override
    public MeasureComputerBuilder setOutputMetrics(String... outputMetrics) {
      checkArgument(outputMetrics != null && outputMetrics.length > 0, "At least one output metric must be defined");
      this.outputMetrics = outputMetrics;
      return this;
    }

    @Override
    public MeasureComputerBuilder setImplementation(Implementation impl) {
      checkImplementation(impl);
      this.measureComputerImplementation = impl;
      return this;
    }

    @Override
    public MeasureComputer build() {
      checkArgument(this.inputMetricKeys != null, "At least one input metric must be defined");
      checkArgument(this.outputMetrics != null, "At least one output metric must be defined");
      checkImplementation(this.measureComputerImplementation);
      return new MeasureComputerImpl(this);
    }

    private static void checkImplementation(Implementation impl) {
      checkArgument(impl != null, "The implementation is missing");
    }
  }
}

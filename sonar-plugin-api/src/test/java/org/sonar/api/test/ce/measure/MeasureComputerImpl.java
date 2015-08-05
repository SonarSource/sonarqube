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

package org.sonar.api.test.ce.measure;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sonar.api.ce.measure.MeasureComputer;

import static com.google.common.base.Preconditions.checkArgument;

public class MeasureComputerImpl implements MeasureComputer {

  private final Set<String> inputMetricKeys;
  private final Set<String> outputMetrics;
  private final Implementation measureComputerImplementation;

  public MeasureComputerImpl(MeasureComputerBuilderImpl builder) {
    this.inputMetricKeys = ImmutableSet.copyOf(builder.inputMetricKeys);
    this.outputMetrics = ImmutableSet.copyOf(builder.outputMetrics);
    this.measureComputerImplementation = builder.measureComputerImplementation;
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
    return measureComputerImplementation;
  }

  @Override
  public String toString() {
    return "MeasureComputerImpl{" +
      "inputMetricKeys=" + inputMetricKeys +
      ", outputMetrics=" + outputMetrics +
      ", implementation=" + measureComputerImplementation.toString() +
      '}';
  }

  public static class MeasureComputerBuilderImpl implements MeasureComputerBuilder {

    private String[] inputMetricKeys;
    private String[] outputMetrics;
    private Implementation measureComputerImplementation;

    @Override
    public MeasureComputerBuilder setInputMetrics(String... inputMetrics) {
      this.inputMetricKeys = inputMetrics;
      checkInputMetricKeys();
      return this;
    }

    @Override
    public MeasureComputerBuilder setOutputMetrics(String... outputMetrics) {
      this.outputMetrics = outputMetrics;
      checkOutputMetricKeys();
      return this;
    }

    @Override
    public MeasureComputerBuilder setImplementation(Implementation impl) {
      this.measureComputerImplementation = impl;
      checkImplementation();
      return this;
    }

    @Override
    public MeasureComputer build() {
      checkInputMetricKeys();
      checkOutputMetricKeys();
      checkImplementation();
      return new MeasureComputerImpl(this);
    }

    private void checkInputMetricKeys(){
      checkArgument(this.inputMetricKeys != null && inputMetricKeys.length > 0, "At least one input metrics must be defined");
    }

    private void checkOutputMetricKeys(){
      checkArgument(this.outputMetrics != null && outputMetrics.length > 0, "At least one output metrics must be defined");
    }

    private void checkImplementation(){
      checkArgument(this.measureComputerImplementation != null, "The implementation is missing");
    }
  }
}

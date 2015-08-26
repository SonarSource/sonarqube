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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.ce.measure.MeasureComputer;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureComputerImplTest {

  private static final MeasureComputer.Implementation DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION = new MeasureComputer.Implementation() {
    @Override
    public void compute(MeasureComputer.Implementation.Context ctx) {
      // Nothing here for this test
    }

    @Override
    public String toString() {
      return "Test implementation";
    }
  };

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void build_measure_computer() throws Exception {
    String inputMetric = "ncloc";
    String outputMetric = "comment_density";
    MeasureComputer measureComputer = new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics(inputMetric)
      .setOutputMetrics(outputMetric)
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION)
      .build();

    assertThat(measureComputer.getInputMetrics()).containsOnly(inputMetric);
    assertThat(measureComputer.getOutputMetrics()).containsOnly(outputMetric);
    assertThat(measureComputer.getImplementation()).isEqualTo(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION);
  }

  @Test
  public void build_measure_computer_with_multiple_metrics() throws Exception {
    String[] inputMetrics = {"ncloc", "comment"};
    String[] outputMetrics = {"comment_density_1", "comment_density_2"};
    MeasureComputer measureComputer = new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics(inputMetrics)
      .setOutputMetrics(outputMetrics)
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION)
      .build();

    assertThat(measureComputer.getInputMetrics()).containsOnly(inputMetrics);
    assertThat(measureComputer.getOutputMetrics()).containsOnly(outputMetrics);
    assertThat(measureComputer.getImplementation()).isEqualTo(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION);
  }

  @Test
  public void input_metrics_can_be_empty() throws Exception {
    MeasureComputer measureComputer = new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics()
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION)
      .build();

    assertThat(measureComputer.getInputMetrics()).isEmpty();
  }

  @Test
  public void input_metrics_is_empty_when_not_set() throws Exception {
    MeasureComputer measureComputer = new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION)
      .build();

    assertThat(measureComputer.getInputMetrics()).isEmpty();
  }

  @Test
  public void fail_with_NPE_when_null_input_metrics() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Input metrics cannot be null");

    new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics(null)
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION);
  }

  @Test
  public void fail_with_NPE_when_one_input_metric_is_null() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Null metric is not allowed");

    new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics("ncloc", null)
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION);
  }

  @Test
  public void fail_with_IAE_when_no_output_metrics() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("At least one output metric must be defined");

    new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION)
      .build();
  }

  @Test
  public void fail_with_IAE_when_null_output_metrics() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("At least one output metric must be defined");

    new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics(null)
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION);
  }

  @Test
  public void fail_with_NPE_when_one_output_metric_is_null() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Null metric is not allowed");

    new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics("comment_density_1", null)
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION);
  }

  @Test
  public void fail_with_IAE_with_empty_output_metrics() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("At least one output metric must be defined");

    new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics()
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION);
  }

  @Test
  public void fail_with_IAE_when_no_implementation() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("The implementation is missing");

    new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .build();
  }

  @Test
  public void fail_with_IAE_when_null_implementation() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("The implementation is missing");

    new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .setImplementation(null);
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    MeasureComputer computer = new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION)
      .build();

    MeasureComputer sameComputer = new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics("comment", "ncloc")
      .setOutputMetrics("comment_density_2", "comment_density_1")
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION)
      .build();

    MeasureComputer anotherComputer = new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics("comment")
      .setOutputMetrics("debt")
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION)
      .build();

    assertThat(computer).isEqualTo(computer);
    assertThat(computer).isEqualTo(sameComputer);
    assertThat(computer).isNotEqualTo(anotherComputer);
    assertThat(computer).isNotEqualTo(null);

    assertThat(computer.hashCode()).isEqualTo(computer.hashCode());
    assertThat(computer.hashCode()).isEqualTo(sameComputer.hashCode());
    assertThat(computer.hashCode()).isNotEqualTo(anotherComputer.hashCode());
  }

  @Test
  public void test_to_string() throws Exception {
    assertThat(new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .setImplementation(DEFAULT_MEASURE_COMPUTER_IMPLEMENTATION)
      .build().toString())
      .isEqualTo("MeasureComputerImpl{inputMetricKeys=[ncloc, comment], outputMetrics=[comment_density_1, comment_density_2], implementation=Test implementation}");
  }
}

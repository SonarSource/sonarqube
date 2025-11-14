/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.junit.Test;
import org.sonar.api.ce.measure.MeasureComputer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MeasureComputerDefinitionImplTest {

  @Test
  public void build_measure_computer_definition() {
    String inputMetric = "ncloc";
    String outputMetric = "comment_density";
    MeasureComputer.MeasureComputerDefinition measureComputer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics(inputMetric)
      .setOutputMetrics(outputMetric)
      .build();

    assertThat(measureComputer.getInputMetrics()).containsOnly(inputMetric);
    assertThat(measureComputer.getOutputMetrics()).containsOnly(outputMetric);
  }

  @Test
  public void build_measure_computer_with_multiple_metrics() {
    String[] inputMetrics = {"ncloc", "comment"};
    String[] outputMetrics = {"comment_density_1", "comment_density_2"};
    MeasureComputer.MeasureComputerDefinition measureComputer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics(inputMetrics)
      .setOutputMetrics(outputMetrics)
      .build();

    assertThat(measureComputer.getInputMetrics()).containsOnly(inputMetrics);
    assertThat(measureComputer.getOutputMetrics()).containsOnly(outputMetrics);
  }

  @Test
  public void input_metrics_can_be_empty() {
    MeasureComputer.MeasureComputerDefinition measureComputer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics()
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .build();

    assertThat(measureComputer.getInputMetrics()).isEmpty();
  }

  @Test
  public void input_metrics_is_empty_when_not_set() {
    MeasureComputer.MeasureComputerDefinition measureComputer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .build();

    assertThat(measureComputer.getInputMetrics()).isEmpty();
  }

  @Test
  public void fail_with_NPE_when_null_input_metrics() {
    assertThatThrownBy(() -> {
      new MeasureComputerDefinitionImpl.BuilderImpl()
        .setInputMetrics((String[]) null)
        .setOutputMetrics("comment_density_1", "comment_density_2");
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Input metrics cannot be null");
  }

  @Test
  public void fail_with_NPE_when_one_input_metric_is_null() {
    assertThatThrownBy(() -> {
      new MeasureComputerDefinitionImpl.BuilderImpl()
        .setInputMetrics("ncloc", null)
        .setOutputMetrics("comment_density_1", "comment_density_2");
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Null metric is not allowed");
  }

  @Test
  public void fail_with_NPE_when_no_output_metrics() {
    assertThatThrownBy(() -> {
      new MeasureComputerDefinitionImpl.BuilderImpl()
        .setInputMetrics("ncloc", "comment")
        .build();
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Output metrics cannot be null");
  }

  @Test
  public void fail_with_NPE_when_null_output_metrics() {
    assertThatThrownBy(() -> {
      new MeasureComputerDefinitionImpl.BuilderImpl()
        .setInputMetrics("ncloc", "comment")
        .setOutputMetrics((String[]) null);
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Output metrics cannot be null");
  }

  @Test
  public void fail_with_NPE_when_one_output_metric_is_null() {
    assertThatThrownBy(() -> {
      new MeasureComputerDefinitionImpl.BuilderImpl()
        .setInputMetrics("ncloc", "comment")
        .setOutputMetrics("comment_density_1", null);
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Null metric is not allowed");
  }

  @Test
  public void fail_with_IAE_with_empty_output_metrics() {
    assertThatThrownBy(() -> {
      new MeasureComputerDefinitionImpl.BuilderImpl()
        .setInputMetrics("ncloc", "comment")
        .setOutputMetrics();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("At least one output metric must be defined");
  }

  @Test
  public void test_equals_and_hashcode() {
    MeasureComputer.MeasureComputerDefinition computer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .build();

    MeasureComputer.MeasureComputerDefinition sameComputer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("comment", "ncloc")
      .setOutputMetrics("comment_density_2", "comment_density_1")
      .build();

    MeasureComputer.MeasureComputerDefinition anotherComputer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("comment")
      .setOutputMetrics("debt")
      .build();

    assertThat(computer)
      .isEqualTo(computer)
      .isEqualTo(sameComputer)
      .isNotEqualTo(anotherComputer)
      .isNotNull()
      .hasSameHashCodeAs(computer)
      .hasSameHashCodeAs(sameComputer)
      .doesNotHaveSameHashCodeAs(anotherComputer.hashCode());
  }

  @Test
  public void test_to_string() {
    assertThat(new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .build())
      .hasToString("MeasureComputerDefinitionImpl{inputMetricKeys=[ncloc, comment], outputMetrics=[comment_density_1, comment_density_2]}");
  }

}

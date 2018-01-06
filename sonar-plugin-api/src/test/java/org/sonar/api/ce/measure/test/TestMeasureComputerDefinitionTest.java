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
package org.sonar.api.ce.measure.test;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.ce.measure.MeasureComputer.MeasureComputerDefinition;
import org.sonar.api.ce.measure.test.TestMeasureComputerDefinition.MeasureComputerDefinitionBuilderImpl;
import org.sonar.api.measures.CoreMetrics;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMeasureComputerDefinitionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void build_definition() throws Exception {
    MeasureComputerDefinition definition = new MeasureComputerDefinitionBuilderImpl()
      .setInputMetrics("INPUT_1", "INPUT_2")
      .setOutputMetrics("OUTPUT_1", "OUTPUT_2")
      .build();

    assertThat(definition.getInputMetrics()).containsOnly("INPUT_1", "INPUT_2");
    assertThat(definition.getOutputMetrics()).containsOnly("OUTPUT_1", "OUTPUT_2");
  }

  @Test
  public void build_definition_without_input_metric() throws Exception {
    MeasureComputerDefinition definition = new MeasureComputerDefinitionBuilderImpl()
      .setOutputMetrics("OUTPUT_1", "OUTPUT_2")
      .build();

    assertThat(definition.getInputMetrics()).isEmpty();
    assertThat(definition.getOutputMetrics()).containsOnly("OUTPUT_1", "OUTPUT_2");
  }

  @Test
  public void fail_with_NPE_when_building_definition_with_null_input_metrics() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Input metrics cannot be null");

    new MeasureComputerDefinitionBuilderImpl()
      .setInputMetrics(null)
      .setOutputMetrics("OUTPUT_1", "OUTPUT_2")
      .build();
  }

  @Test
  public void fail_with_NPE_when_building_definition_with_on_null_input_metric() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Null metric is not allowed");

    new MeasureComputerDefinitionBuilderImpl()
      .setInputMetrics("INPUT_1", null)
      .setOutputMetrics("OUTPUT_1", "OUTPUT_2")
      .build();
  }

  @Test
  public void fail_with_NPE_when_building_definition_with_null_output_metrics() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Output metrics cannot be null");

    new MeasureComputerDefinitionBuilderImpl()
      .setInputMetrics("INPUT_1", "INPUT_2")
      .setOutputMetrics(null)
      .build();
  }

  @Test
  public void fail_with_NPE_when_building_definition_without_output_metrics() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Output metrics cannot be null");

    new MeasureComputerDefinitionBuilderImpl()
      .setInputMetrics("INPUT_1", "INPUT_2")
      .build();
  }

  @Test
  public void fail_with_NPE_when_building_definition_with_on_null_ouput_metric() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Null metric is not allowed");

    new MeasureComputerDefinitionBuilderImpl()
      .setInputMetrics("INPUT_1", "INPUT_2")
      .setOutputMetrics("OUTPUT_1", null)
      .build();
  }

  @Test
  public void fail_with_IAE_when_building_definition_with_empty_output_metrics() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("At least one output metric must be defined");

    new MeasureComputerDefinitionBuilderImpl()
      .setInputMetrics("INPUT_1", "INPUT_2")
      .setOutputMetrics()
      .build();
  }

  @Test
  public void fail_with_IAE_when_building_definition_with_core_metrics_in_output_metrics() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Core metrics are not allowed");

    new MeasureComputerDefinitionBuilderImpl()
      .setInputMetrics("INPUT_1", "INPUT_2")
      .setOutputMetrics(CoreMetrics.NCLOC_KEY)
      .build();
  }
}

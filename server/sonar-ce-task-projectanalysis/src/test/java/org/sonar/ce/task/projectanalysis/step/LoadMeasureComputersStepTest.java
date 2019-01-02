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
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.ce.task.projectanalysis.api.measurecomputer.MeasureComputerWrapper;
import org.sonar.ce.task.projectanalysis.measure.MeasureComputersHolderImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Arrays.array;
import static org.sonar.api.measures.CoreMetrics.CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.MILLISEC;

public class LoadMeasureComputersStepTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final String NEW_METRIC_1 = "metric1";
  private static final String NEW_METRIC_2 = "metric2";
  private static final String NEW_METRIC_3 = "metric3";
  private static final String NEW_METRIC_4 = "metric4";

  private MeasureComputersHolderImpl holder = new MeasureComputersHolderImpl();

  @Test
  public void support_core_metrics_as_input_metrics() {
    MeasureComputer[] computers = new MeasureComputer[] {newMeasureComputer(array(NCLOC_KEY), array(NEW_METRIC_1))};
    ComputationStep underTest = new LoadMeasureComputersStep(holder, array(new TestMetrics()), computers);
    underTest.execute(new TestComputationStepContext());

    assertThat(holder.getMeasureComputers()).hasSize(1);
  }

  @Test
  public void support_plugin_metrics_as_input_metrics() {
    MeasureComputer[] computers = new MeasureComputer[] {newMeasureComputer(array(NEW_METRIC_1), array(NEW_METRIC_2))};
    ComputationStep underTest = new LoadMeasureComputersStep(holder, array(new TestMetrics()), computers);
    underTest.execute(new TestComputationStepContext());

    assertThat(holder.getMeasureComputers()).hasSize(1);
  }

  @Test
  public void sort_computers() {
    // Should be the last to be executed
    MeasureComputer measureComputer1 = newMeasureComputer(array(NEW_METRIC_3), array(NEW_METRIC_4));
    // Should be the first to be executed
    MeasureComputer measureComputer2 = newMeasureComputer(array(NEW_METRIC_1), array(NEW_METRIC_2));
    // Should be the second to be executed
    MeasureComputer measureComputer3 = newMeasureComputer(array(NEW_METRIC_2), array(NEW_METRIC_3));
    MeasureComputer[] computers = new MeasureComputer[] {measureComputer1, measureComputer2, measureComputer3};

    ComputationStep underTest = new LoadMeasureComputersStep(holder, array(new TestMetrics()), computers);
    underTest.execute(new TestComputationStepContext());

    List<MeasureComputerWrapper> result = newArrayList(holder.getMeasureComputers());
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getComputer()).isEqualTo(measureComputer2);
    assertThat(result.get(1).getComputer()).isEqualTo(measureComputer3);
    assertThat(result.get(2).getComputer()).isEqualTo(measureComputer1);
  }

  @Test
  public void sort_computers_when_one_computer_has_no_input_metric() {
    // Should be the last to be executed
    MeasureComputer measureComputer1 = newMeasureComputer(array(NEW_METRIC_3), array(NEW_METRIC_4));
    // Should be the first to be executed
    MeasureComputer measureComputer2 = newMeasureComputer(new String[] {}, array(NEW_METRIC_2));
    // Should be the second to be executed
    MeasureComputer measureComputer3 = newMeasureComputer(array(NEW_METRIC_2), array(NEW_METRIC_3));
    MeasureComputer[] computers = new MeasureComputer[] {measureComputer1, measureComputer2, measureComputer3};

    ComputationStep underTest = new LoadMeasureComputersStep(holder, array(new TestMetrics()), computers);
    underTest.execute(new TestComputationStepContext());

    List<MeasureComputerWrapper> result = newArrayList(holder.getMeasureComputers());
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getComputer()).isEqualTo(measureComputer2);
    assertThat(result.get(1).getComputer()).isEqualTo(measureComputer3);
    assertThat(result.get(2).getComputer()).isEqualTo(measureComputer1);
  }

  @Test
  public void fail_with_ISE_when_input_metric_is_unknown() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Metric 'unknown' cannot be used as an input metric as it's not a core metric and no plugin declare this metric");

    MeasureComputer[] computers = new MeasureComputer[] {newMeasureComputer(array("unknown"), array(NEW_METRIC_4))};
    ComputationStep underTest = new LoadMeasureComputersStep(holder, array(new TestMetrics()), computers);
    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void fail_with_ISE_when_output_metric_is_not_define_by_plugin() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Metric 'unknown' cannot be used as an output metric because no plugins declare this metric");

    MeasureComputer[] computers = new MeasureComputer[] {newMeasureComputer(array(NEW_METRIC_4), array("unknown"))};
    ComputationStep underTest = new LoadMeasureComputersStep(holder, array(new TestMetrics()), computers);
    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void fail_with_ISE_when_output_metric_is_a_core_metric() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Metric 'ncloc' cannot be used as an output metric because it's a core metric");

    MeasureComputer[] computers = new MeasureComputer[] {newMeasureComputer(array(NEW_METRIC_4), array(NCLOC_KEY))};
    ComputationStep underTest = new LoadMeasureComputersStep(holder, array(new TestMetrics()), computers);
    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void not_fail_if_input_metrics_are_same_as_output_metrics() {
    MeasureComputer[] computers = new MeasureComputer[] {newMeasureComputer(array(NEW_METRIC_1), array(NEW_METRIC_1))};
    ComputationStep underTest = new LoadMeasureComputersStep(holder, array(new TestMetrics()), computers);
    underTest.execute(new TestComputationStepContext());

    assertThat(holder.getMeasureComputers()).hasSize(1);
  }

  @Test
  public void return_empty_list_when_no_measure_computers() {
    ComputationStep underTest = new LoadMeasureComputersStep(holder, array(new TestMetrics()));
    underTest.execute(new TestComputationStepContext());

    assertThat(holder.getMeasureComputers()).isEmpty();
  }

  @Test
  public void return_empty_list_when_no_metrics_neither_measure_computers() {
    ComputationStep underTest = new LoadMeasureComputersStep(holder);
    underTest.execute(new TestComputationStepContext());

    assertThat(holder.getMeasureComputers()).isEmpty();
  }

  @Test
  public void fail_with_ISE_when_no_metrics_are_defined_by_plugin_but_measure_computer_use_a_new_metric() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Metric 'metric1' cannot be used as an output metric because no plugins declare this metric");

    MeasureComputer[] computers = new MeasureComputer[] {newMeasureComputer(array(NCLOC_KEY), array(NEW_METRIC_1))};
    ComputationStep underTest = new LoadMeasureComputersStep(holder, computers);
    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void fail_with_ISE_when_two_measure_computers_generate_the_same_output_metric() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Output metric 'metric1' is already defined by another measure computer 'TestMeasureComputer'");

    MeasureComputer[] computers = new MeasureComputer[] {newMeasureComputer(array(NCLOC_KEY), array(NEW_METRIC_1)), newMeasureComputer(array(CLASSES_KEY), array(NEW_METRIC_1))};
    ComputationStep underTest = new LoadMeasureComputersStep(holder, array(new TestMetrics()), computers);
    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void fail_with_IAE_when_creating_measure_computer_definition_without_using_the_builder_and_with_invalid_output_metrics() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("At least one output metric must be defined");

    MeasureComputer measureComputer = new MeasureComputer() {
      @Override
      public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
        // Create a instance of MeasureComputerDefinition without using the builder
        return new MeasureComputer.MeasureComputerDefinition() {
          @Override
          public Set<String> getInputMetrics() {
            return ImmutableSet.of(NCLOC_KEY);
          }

          @Override
          public Set<String> getOutputMetrics() {
            // Empty output metric is not allowed !
            return Collections.emptySet();
          }
        };
      }

      @Override
      public void compute(MeasureComputerContext context) {
        // Nothing needs to be done as we're only testing metada
      }
    };

    MeasureComputer[] computers = new MeasureComputer[] {measureComputer};
    ComputationStep underTest = new LoadMeasureComputersStep(holder, array(new TestMetrics()), computers);
    underTest.execute(new TestComputationStepContext());
  }

  private static MeasureComputer newMeasureComputer(final String[] inputMetrics, final String[] outputMetrics) {
    return new MeasureComputer() {
      @Override
      public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
        return defContext.newDefinitionBuilder()
          .setInputMetrics(inputMetrics)
          .setOutputMetrics(outputMetrics)
          .build();
      }

      @Override
      public void compute(MeasureComputerContext context) {
        // Nothing needs to be done as we're only testing metada
      }

      @Override
      public String toString() {
        return "TestMeasureComputer";
      }
    };
  }

  private static class TestMetrics implements Metrics {
    @Override
    public List<Metric> getMetrics() {
      return Lists.newArrayList(
        new Metric.Builder(NEW_METRIC_1, "metric1", DATA).create(),
        new Metric.Builder(NEW_METRIC_2, "metric2", MILLISEC).create(),
        new Metric.Builder(NEW_METRIC_3, "metric3", INT).create(),
        new Metric.Builder(NEW_METRIC_4, "metric4", FLOAT).create());
    }
  }

}

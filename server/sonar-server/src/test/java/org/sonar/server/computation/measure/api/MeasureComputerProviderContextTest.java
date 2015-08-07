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
import java.util.Collections;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.ce.measure.MeasureComputer;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureComputerProviderContextTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  MeasureComputerProviderContext underTest = new MeasureComputerProviderContext();

  @Test
  public void return_empty_list() throws Exception {
    assertThat(underTest.getMeasureComputers()).isEmpty();
  }

  @Test
  public void add_measure_computer() throws Exception {
    underTest.add(newMeasureComputer("debt_density"));

    assertThat(underTest.getMeasureComputers()).hasSize(1);
  }

  @Test
  public void add_measure_computers_sharing_same_input_metrics() throws Exception {
    underTest.add(newMeasureComputer(new String[]{"ncloc"}, new String[]{"debt_density"}));
    underTest.add(newMeasureComputer(new String[]{"ncloc"}, new String[]{"comment"}));

    assertThat(underTest.getMeasureComputers()).hasSize(2);
  }

  @Test
  public void fail_with_unsupported_operation_exception_when_output_metric_have_already_been_registered() throws Exception {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("The output metric 'debt_density' is already declared by another computer. This computer has these input metrics '[ncloc, debt]' and these output metrics '[debt_by_line, debt_density]");

    underTest.add(newMeasureComputer("debt_by_line","debt_density"));
    underTest.add(newMeasureComputer("total_debt", "debt_density"));
  }

  @Test
  public void fail_with_unsupported_operation_exception_when_multiple_output_metrics_have_already_been_registered() throws Exception {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("The output metric 'debt_density' is already declared by another computer. This computer has these input metrics '[ncloc, debt]' and these output metrics '[debt_density]'. " +
      "The output metric 'debt_by_line' is already declared by another computer. This computer has these input metrics '[ncloc, debt]' and these output metrics '[debt_by_line]");

    underTest.add(newMeasureComputer("debt_by_line"));
    underTest.add(newMeasureComputer("debt_density"));
    underTest.add(newMeasureComputer("debt_by_line", "debt_density"));
  }

  @Test
  public void create_measure_computer_without_using_the_builder() throws Exception {
    // Create a instance of MeasureComputer without using the builder
    MeasureComputer measureComputer = new MeasureComputer() {
      @Override
      public Set<String> getInputMetrics() {
        return ImmutableSet.of("ncloc", "debt");
      }

      @Override
      public Set<String> getOutputMetrics() {
        return ImmutableSet.of("debt_density");
      }

      @Override
      public Implementation getImplementation() {
        return new MeasureComputer.Implementation() {
          @Override
          public void compute(Context ctx) {
          }
        };
      }
    };

    underTest.add(measureComputer);
    assertThat(underTest.getMeasureComputers()).hasSize(1);
  }

  @Test
  public void fail_with_IAE_when_creating_measure_computer_without_using_the_builder_but_with_invalid_output_metrics() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("At least one output metric must be defined");

    MeasureComputer measureComputer = new MeasureComputer() {
      @Override
      public Set<String> getInputMetrics() {
        return ImmutableSet.of("ncloc", "debt");
      }

      @Override
      public Set<String> getOutputMetrics() {
        return Collections.emptySet();
      }

      @Override
      public Implementation getImplementation() {
        return new MeasureComputer.Implementation() {
          @Override
          public void compute(Context ctx) {}
        };
      }
    };

    underTest.add(measureComputer);
  }

  private MeasureComputer newMeasureComputer(String... outputMetrics) {
    return newMeasureComputer(new String[]{"ncloc", "debt"}, outputMetrics);
  }

  private MeasureComputer newMeasureComputer(String[] inputMetrics, String[] outputMetrics) {
    return new MeasureComputerImpl.MeasureComputerBuilderImpl()
      .setInputMetrics(inputMetrics)
      .setOutputMetrics(outputMetrics)
      .setImplementation(new MeasureComputer.Implementation() {
        @Override
        public void compute(Context ctx) {
          // Nothing to do here for this test
        }
      })
      .build();
  }

}

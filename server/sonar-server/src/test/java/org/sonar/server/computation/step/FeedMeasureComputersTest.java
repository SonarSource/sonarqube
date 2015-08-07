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

package org.sonar.server.computation.step;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.MeasureComputerProvider;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.server.computation.measure.MeasureComputersHolderImpl;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Arrays.array;
import static org.mockito.Mockito.mock;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.MILLISEC;

public class FeedMeasureComputersTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final String NEW_METRIC_1 = "metric1";
  private static final String NEW_METRIC_2 = "metric2";
  private static final String NEW_METRIC_3 = "metric3";
  private static final String NEW_METRIC_4 = "metric4";

  MeasureComputersHolderImpl holder = new MeasureComputersHolderImpl();

  @Test
  public void support_core_metrics_as_input_metrics() throws Exception {
    MeasureComputer.Implementation implementation = mock(MeasureComputer.Implementation.class);
    MeasureComputerProvider[] providers = new MeasureComputerProvider[] {
      new NewMeasureComputerProvider(
        array(CoreMetrics.NCLOC_KEY),
        array(NEW_METRIC_1),
        implementation),
    };
    ComputationStep underTest = new FeedMeasureComputers(holder, array(new TestMetrics()), providers);
    underTest.execute();

    assertThat(holder.getMeasureComputers()).hasSize(1);
  }

  @Test
  public void support_plugin_metrics_as_input_metrics() throws Exception {
    MeasureComputer.Implementation implementation = mock(MeasureComputer.Implementation.class);
    MeasureComputerProvider[] providers = new MeasureComputerProvider[] {
      new NewMeasureComputerProvider(
        array(NEW_METRIC_1),
        array(NEW_METRIC_2),
        implementation),
    };
    ComputationStep underTest = new FeedMeasureComputers(holder, array(new TestMetrics()), providers);
    underTest.execute();

    assertThat(holder.getMeasureComputers()).hasSize(1);
  }

  @Test
  public void sort_computers() throws Exception {
    MeasureComputer.Implementation implementation1 = mock(MeasureComputer.Implementation.class);
    MeasureComputer.Implementation implementation2 = mock(MeasureComputer.Implementation.class);
    MeasureComputer.Implementation implementation3 = mock(MeasureComputer.Implementation.class);

    MeasureComputerProvider[] providers = new MeasureComputerProvider[] {
      // Should be the last to be executed
      new NewMeasureComputerProvider(
        array(NEW_METRIC_3),
        array(NEW_METRIC_4),
        implementation3),
      // Should be the first to be executed
      new NewMeasureComputerProvider(
        array(NEW_METRIC_1),
        array(NEW_METRIC_2),
        implementation1),
      // Should be the second to be executed
      new NewMeasureComputerProvider(
        array(NEW_METRIC_2),
        array(NEW_METRIC_3),
        implementation2)
    };

    ComputationStep underTest = new FeedMeasureComputers(holder, array(new TestMetrics()), providers);
    underTest.execute();

    List<MeasureComputer> computers = newArrayList(holder.getMeasureComputers());
    assertThat(computers).hasSize(3);
    assertThat(computers.get(0).getImplementation()).isEqualTo(implementation1);
    assertThat(computers.get(1).getImplementation()).isEqualTo(implementation2);
    assertThat(computers.get(2).getImplementation()).isEqualTo(implementation3);
  }

  @Test
  public void fail_with_ISE_when_input_metric_is_unknown() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Metric 'unknown' cannot be used as an input metric as it's no a core metric and no plugin declare this metric");

    MeasureComputerProvider[] providers = new MeasureComputerProvider[] {
      new NewMeasureComputerProvider(
        array("unknown"),
        array(NEW_METRIC_4),
        mock(MeasureComputer.Implementation.class)),
    };

    ComputationStep underTest = new FeedMeasureComputers(holder, array(new TestMetrics()), providers);
    underTest.execute();
  }

  @Test
  public void not_fail_if_input_metrics_are_same_as_output_metrics() throws Exception {
    MeasureComputer.Implementation implementation = mock(MeasureComputer.Implementation.class);
    MeasureComputerProvider[] providers = new MeasureComputerProvider[] {
      new NewMeasureComputerProvider(
        array(NEW_METRIC_1),
        array(NEW_METRIC_1),
        implementation),
    };
    ComputationStep underTest = new FeedMeasureComputers(holder, array(new TestMetrics()), providers);
    underTest.execute();

    assertThat(holder.getMeasureComputers()).hasSize(1);
  }

  @Test
  public void return_empty_list_when_no_measure_computers() throws Exception {
    ComputationStep underTest = new FeedMeasureComputers(holder, array(new TestMetrics()));
    underTest.execute();

    assertThat(holder.getMeasureComputers()).isEmpty();
  }

  @Test
  public void support_no_plugin_metrics() throws Exception {
    MeasureComputer.Implementation implementation = mock(MeasureComputer.Implementation.class);
    MeasureComputerProvider[] providers = new MeasureComputerProvider[] {
      new NewMeasureComputerProvider(
        array(CoreMetrics.NCLOC_KEY),
        array(CoreMetrics.COMMENT_LINES_KEY),
        implementation),
    };
    ComputationStep underTest = new FeedMeasureComputers(holder, providers);
    underTest.execute();

    assertThat(holder.getMeasureComputers()).hasSize(1);
  }

  @Test
  public void return_empty_list_when_no_metrics_neither_measure_computers() throws Exception {
    ComputationStep underTest = new FeedMeasureComputers(holder);
    underTest.execute();

    assertThat(holder.getMeasureComputers()).isEmpty();
  }

  private static class NewMeasureComputerProvider implements MeasureComputerProvider {

    private final String[] inputMetrics;
    private final String[] outputMetrics;
    private final MeasureComputer.Implementation measureComputerImplementation;

    public NewMeasureComputerProvider(String[] inputMetrics, String[] outputMetrics, MeasureComputer.Implementation measureComputerImplementation) {
      this.inputMetrics = inputMetrics;
      this.outputMetrics = outputMetrics;
      this.measureComputerImplementation = measureComputerImplementation;
    }

    @Override
    public void register(Context ctx) {
      ctx.add(ctx.newMeasureComputerBuilder()
        .setInputMetrics(inputMetrics)
        .setOutputMetrics(outputMetrics)
        .setImplementation(measureComputerImplementation)
        .build());
    }
  }

  private static class TestMetrics implements Metrics {
    @Override
    public List<Metric> getMetrics() {
      return Lists.<Metric>newArrayList(
        new Metric.Builder(NEW_METRIC_1, "metric1", DATA).create(),
        new Metric.Builder(NEW_METRIC_2, "metric2", MILLISEC).create(),
        new Metric.Builder(NEW_METRIC_3, "metric3", INT).create(),
        new Metric.Builder(NEW_METRIC_4, "metric4", FLOAT).create()
        );
    }
  }

}

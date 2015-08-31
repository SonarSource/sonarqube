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

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.MeasureComputerProvider;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.utils.dag.DirectAcyclicGraph;
import org.sonar.server.computation.measure.MutableMeasureComputersHolder;
import org.sonar.server.computation.measure.api.MeasureComputerProviderContext;

public class FeedMeasureComputers implements ComputationStep {

  private static final Set<String> CORE_METRIC_KEYS = FluentIterable.from(CoreMetrics.getMetrics()).transform(MetricToKey.INSTANCE).toSet();

  private final MutableMeasureComputersHolder measureComputersHolder;
  private final Metrics[] metricsRepositories;
  private final MeasureComputerProvider[] measureComputerProviders;

  public FeedMeasureComputers(MutableMeasureComputersHolder measureComputersHolder, Metrics[] metricsRepositories, MeasureComputerProvider[] measureComputerProviders) {
    this.measureComputersHolder = measureComputersHolder;
    this.measureComputerProviders = measureComputerProviders;
    this.metricsRepositories = metricsRepositories;
  }

  /**
   * Constructor override used by Pico to instantiate the class when no plugin is defining metrics
   */
  public FeedMeasureComputers(MutableMeasureComputersHolder measureComputersHolder, MeasureComputerProvider[] measureComputerProviders) {
    this(measureComputersHolder, new Metrics[] {}, measureComputerProviders);
  }

  /**
   * Constructor override used by Pico to instantiate the class when no plugin is defining measure computers
   */
  public FeedMeasureComputers(MutableMeasureComputersHolder measureComputersHolder, Metrics[] metricsRepositories) {
    this(measureComputersHolder, metricsRepositories, new MeasureComputerProvider[] {});
  }

  /**
   * Constructor override used by Pico to instantiate the class when no plugin is defining metrics neither measure computers
   */
  public FeedMeasureComputers(MutableMeasureComputersHolder measureComputersHolder) {
    this(measureComputersHolder, new Metrics[] {}, new MeasureComputerProvider[] {});
  }

  @Override
  public void execute() {
    MeasureComputerProviderContext context = new MeasureComputerProviderContext();
    for (MeasureComputerProvider provider : measureComputerProviders) {
      provider.register(context);
    }
    validateInputMetrics(context.getMeasureComputers());
    measureComputersHolder.setMeasureComputers(sortComputers(context.getMeasureComputers()));
  }

  private static Iterable<MeasureComputer> sortComputers(List<MeasureComputer> computers) {
    Map<String, MeasureComputer> computersByOutputMetric = new HashMap<>();
    Map<String, MeasureComputer> computersByInputMetric = new HashMap<>();
    feedComputersByMetric(computers, computersByOutputMetric, computersByInputMetric);
    ToComputerByKey toComputerByOutputMetricKey = new ToComputerByKey(computersByOutputMetric);
    ToComputerByKey toComputerByInputMetricKey = new ToComputerByKey(computersByInputMetric);

    DirectAcyclicGraph dag = new DirectAcyclicGraph();
    for (MeasureComputer computer : computers) {
      dag.add(computer);
      for (MeasureComputer dependency : getDependencies(computer, toComputerByOutputMetricKey)) {
        dag.add(computer, dependency);
      }
      for (MeasureComputer generates : getDependents(computer, toComputerByInputMetricKey)) {
        dag.add(generates, computer);
      }
    }
    return dag.sort();
  }

  private static void feedComputersByMetric(List<MeasureComputer> computers, Map<String, MeasureComputer> computersByOutputMetric,
    Map<String, MeasureComputer> computersByInputMetric) {
    for (MeasureComputer computer : computers) {
      for (String outputMetric : computer.getOutputMetrics()) {
        computersByOutputMetric.put(outputMetric, computer);
      }
      for (String inputMetric : computer.getInputMetrics()) {
        computersByInputMetric.put(inputMetric, computer);
      }
    }
  }

  private void validateInputMetrics(List<MeasureComputer> computers) {
    Set<String> pluginMetricKeys = FluentIterable.from(Arrays.asList(metricsRepositories))
      .transformAndConcat(MetricsToMetricList.INSTANCE)
      .transform(MetricToKey.INSTANCE)
      .toSet();
    // TODO would be nice to generate an error containing all bad input metrics
    for (MeasureComputer computer : computers) {
      for (String metric : computer.getInputMetrics()) {
        if (!pluginMetricKeys.contains(metric) && !CORE_METRIC_KEYS.contains(metric)) {
          throw new IllegalStateException(String.format("Metric '%s' cannot be used as an input metric as it's no a core metric and no plugin declare this metric", metric));
        }
      }
    }
  }

  private static Iterable<MeasureComputer> getDependencies(MeasureComputer measureComputer, ToComputerByKey toComputerByOutputMetricKey) {
    // Remove null computer because a computer can depend on a metric that is only generated by a sensor or on a core metrics
    return FluentIterable.from(measureComputer.getInputMetrics()).transform(toComputerByOutputMetricKey).filter(Predicates.notNull());
  }

  private static Iterable<MeasureComputer> getDependents(MeasureComputer measureComputer, ToComputerByKey toComputerByInputMetricKey) {
    return FluentIterable.from(measureComputer.getInputMetrics()).transform(toComputerByInputMetricKey);
  }

  private static class ToComputerByKey implements Function<String, MeasureComputer> {
    private final Map<String, MeasureComputer> computersByMetric;

    private ToComputerByKey(Map<String, MeasureComputer> computersByMetric) {
      this.computersByMetric = computersByMetric;
    }

    @Override
    public MeasureComputer apply(@Nonnull String metricKey) {
      return computersByMetric.get(metricKey);
    }
  }

  private enum MetricToKey implements Function<Metric, String> {
    INSTANCE;

    @Nullable
    @Override
    public String apply(@Nonnull Metric input) {
      return input.key();
    }
  }

  private enum MetricsToMetricList implements Function<Metrics, List<Metric>> {
    INSTANCE;

    @Override
    public List<Metric> apply(@Nonnull Metrics input) {
      return input.getMetrics();
    }
  }

  @Override
  public String getDescription() {
    return "Feed measure computers";
  }
}

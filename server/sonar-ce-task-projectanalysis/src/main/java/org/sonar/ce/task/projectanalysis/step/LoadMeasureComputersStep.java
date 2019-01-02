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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.utils.dag.DirectAcyclicGraph;
import org.sonar.ce.task.projectanalysis.api.measurecomputer.MeasureComputerDefinitionImpl;
import org.sonar.ce.task.projectanalysis.api.measurecomputer.MeasureComputerWrapper;
import org.sonar.ce.task.projectanalysis.measure.MutableMeasureComputersHolder;
import org.sonar.ce.task.step.ComputationStep;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sonar.api.ce.measure.MeasureComputer.MeasureComputerDefinition;

public class LoadMeasureComputersStep implements ComputationStep {

  private static final Set<String> CORE_METRIC_KEYS = from(CoreMetrics.getMetrics()).transform(MetricToKey.INSTANCE).toSet();
  private Set<String> pluginMetricKeys;

  private final MutableMeasureComputersHolder measureComputersHolder;
  private final MeasureComputer[] measureComputers;

  public LoadMeasureComputersStep(MutableMeasureComputersHolder measureComputersHolder, Metrics[] metricsRepositories, MeasureComputer[] measureComputers) {
    this.measureComputersHolder = measureComputersHolder;
    this.measureComputers = measureComputers;
    this.pluginMetricKeys = from(Arrays.asList(metricsRepositories))
      .transformAndConcat(MetricsToMetricList.INSTANCE)
      .transform(MetricToKey.INSTANCE)
      .toSet();
  }

  /**
   * Constructor override used by Pico to instantiate the class when no plugin is defining metrics
   */
  public LoadMeasureComputersStep(MutableMeasureComputersHolder measureComputersHolder, MeasureComputer[] measureComputers) {
    this(measureComputersHolder, new Metrics[] {}, measureComputers);
  }

  /**
   * Constructor override used by Pico to instantiate the class when no plugin is defining measure computers
   */
  public LoadMeasureComputersStep(MutableMeasureComputersHolder measureComputersHolder, Metrics[] metricsRepositories) {
    this(measureComputersHolder, metricsRepositories, new MeasureComputer[] {});
  }

  /**
   * Constructor override used by Pico to instantiate the class when no plugin is defining metrics neither measure computers
   */
  public LoadMeasureComputersStep(MutableMeasureComputersHolder measureComputersHolder) {
    this(measureComputersHolder, new Metrics[] {}, new MeasureComputer[] {});
  }

  @Override
  public void execute(ComputationStep.Context context) {
    List<MeasureComputerWrapper> wrappers = from(Arrays.asList(measureComputers)).transform(ToMeasureWrapper.INSTANCE).toList();
    validateMetrics(wrappers);
    measureComputersHolder.setMeasureComputers(sortComputers(wrappers));
  }

  private static Iterable<MeasureComputerWrapper> sortComputers(List<MeasureComputerWrapper> wrappers) {
    Map<String, MeasureComputerWrapper> computersByOutputMetric = new HashMap<>();
    Map<String, MeasureComputerWrapper> computersByInputMetric = new HashMap<>();
    feedComputersByMetric(wrappers, computersByOutputMetric, computersByInputMetric);
    ToComputerByKey toComputerByOutputMetricKey = new ToComputerByKey(computersByOutputMetric);
    ToComputerByKey toComputerByInputMetricKey = new ToComputerByKey(computersByInputMetric);

    DirectAcyclicGraph dag = new DirectAcyclicGraph();
    for (MeasureComputerWrapper computer : wrappers) {
      dag.add(computer);
      for (MeasureComputerWrapper dependency : getDependencies(computer, toComputerByOutputMetricKey)) {
        dag.add(computer, dependency);
      }
      for (MeasureComputerWrapper generates : getDependents(computer, toComputerByInputMetricKey)) {
        dag.add(generates, computer);
      }
    }
    return dag.sort();
  }

  private static void feedComputersByMetric(List<MeasureComputerWrapper> wrappers, Map<String, MeasureComputerWrapper> computersByOutputMetric,
    Map<String, MeasureComputerWrapper> computersByInputMetric) {
    for (MeasureComputerWrapper computer : wrappers) {
      for (String outputMetric : computer.getDefinition().getOutputMetrics()) {
        computersByOutputMetric.put(outputMetric, computer);
      }
      for (String inputMetric : computer.getDefinition().getInputMetrics()) {
        computersByInputMetric.put(inputMetric, computer);
      }
    }
  }

  private void validateMetrics(List<MeasureComputerWrapper> wrappers) {
    from(wrappers).transformAndConcat(ToInputMetrics.INSTANCE).filter(new ValidateInputMetric()).size();
    from(wrappers).transformAndConcat(ToOutputMetrics.INSTANCE).filter(new ValidateOutputMetric()).size();
    from(wrappers).filter(new ValidateUniqueOutputMetric()).size();
  }

  private static Iterable<MeasureComputerWrapper> getDependencies(MeasureComputerWrapper measureComputer, ToComputerByKey toComputerByOutputMetricKey) {
    // Remove null computer because a computer can depend on a metric that is only generated by a sensor or on a core metrics
    return from(measureComputer.getDefinition().getInputMetrics()).transform(toComputerByOutputMetricKey).filter(Predicates.notNull());
  }

  private static Iterable<MeasureComputerWrapper> getDependents(MeasureComputerWrapper measureComputer, ToComputerByKey toComputerByInputMetricKey) {
    return from(measureComputer.getDefinition().getInputMetrics()).transform(toComputerByInputMetricKey);
  }

  private static class ToComputerByKey implements Function<String, MeasureComputerWrapper> {
    private final Map<String, MeasureComputerWrapper> computersByMetric;

    private ToComputerByKey(Map<String, MeasureComputerWrapper> computersByMetric) {
      this.computersByMetric = computersByMetric;
    }

    @Override
    public MeasureComputerWrapper apply(@Nonnull String metricKey) {
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

  private enum ToMeasureWrapper implements Function<MeasureComputer, MeasureComputerWrapper> {
    INSTANCE;

    @Override
    public MeasureComputerWrapper apply(@Nonnull MeasureComputer measureComputer) {
      MeasureComputerDefinition def = measureComputer.define(MeasureComputerDefinitionImpl.BuilderImpl::new);
      return new MeasureComputerWrapper(measureComputer, validateDef(def));
    }

    private static MeasureComputerDefinition validateDef(MeasureComputerDefinition def) {
      if (def instanceof MeasureComputerDefinitionImpl) {
        return def;
      }
      // If the computer has not been created by the builder, we recreate it to make sure it's valid
      Set<String> inputMetrics = def.getInputMetrics();
      Set<String> outputMetrics = def.getOutputMetrics();
      return new MeasureComputerDefinitionImpl.BuilderImpl()
        .setInputMetrics(from(inputMetrics).toArray(String.class))
        .setOutputMetrics(from(outputMetrics).toArray(String.class))
        .build();
    }
  }

  private enum ToInputMetrics implements Function<MeasureComputerWrapper, Collection<String>> {
    INSTANCE;

    @Override
    public Collection<String> apply(@Nonnull MeasureComputerWrapper input) {
      return input.getDefinition().getInputMetrics();
    }
  }

  private class ValidateInputMetric implements Predicate<String> {
    @Override
    public boolean apply(@Nonnull String metric) {
      checkState(pluginMetricKeys.contains(metric) || CORE_METRIC_KEYS.contains(metric),
        "Metric '%s' cannot be used as an input metric as it's not a core metric and no plugin declare this metric", metric);
      return true;
    }
  }

  private enum ToOutputMetrics implements Function<MeasureComputerWrapper, Collection<String>> {
    INSTANCE;

    @Override
    public Collection<String> apply(@Nonnull MeasureComputerWrapper input) {
      return input.getDefinition().getOutputMetrics();
    }
  }

  private class ValidateOutputMetric implements Predicate<String> {
    @Override
    public boolean apply(@Nonnull String metric) {
      checkState(!CORE_METRIC_KEYS.contains(metric), "Metric '%s' cannot be used as an output metric because it's a core metric", metric);
      checkState(pluginMetricKeys.contains(metric), "Metric '%s' cannot be used as an output metric because no plugins declare this metric", metric);
      return true;
    }
  }

  private static class ValidateUniqueOutputMetric implements Predicate<MeasureComputerWrapper> {
    private Set<String> allOutputMetrics = new HashSet<>();

    @Override
    public boolean apply(@Nonnull MeasureComputerWrapper wrapper) {
      for (String outputMetric : wrapper.getDefinition().getOutputMetrics()) {
        checkState(!allOutputMetrics.contains(outputMetric),
          "Output metric '%s' is already defined by another measure computer '%s'", outputMetric, wrapper.getComputer());
        allOutputMetrics.add(outputMetric);
      }
      return true;
    }
  }

  @Override
  public String getDescription() {
    return "Load measure computers";
  }
}

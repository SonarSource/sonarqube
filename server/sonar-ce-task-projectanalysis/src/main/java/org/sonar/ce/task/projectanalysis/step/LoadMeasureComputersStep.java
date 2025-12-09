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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.utils.dag.DirectAcyclicGraph;
import org.sonar.ce.task.projectanalysis.api.measurecomputer.MeasureComputerDefinitionImpl;
import org.sonar.ce.task.projectanalysis.api.measurecomputer.MeasureComputerWrapper;
import org.sonar.ce.task.projectanalysis.measure.MutableMeasureComputersHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.springframework.beans.factory.annotation.Autowired;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sonar.api.ce.measure.MeasureComputer.MeasureComputerDefinition;

public class LoadMeasureComputersStep implements ComputationStep {

  private static final Set<String> CORE_METRIC_KEYS = CoreMetrics.getMetrics().stream().map(Metric::getKey).collect(Collectors.toSet());
  private final Set<String> pluginMetricKeys;
  private final MutableMeasureComputersHolder measureComputersHolder;
  private final MeasureComputer[] measureComputers;

  @Autowired(required = false)
  public LoadMeasureComputersStep(MutableMeasureComputersHolder measureComputersHolder, Metrics[] metricsRepositories, MeasureComputer[] measureComputers) {
    this.measureComputersHolder = measureComputersHolder;
    this.measureComputers = measureComputers;
    this.pluginMetricKeys = Arrays.stream(metricsRepositories)
      .flatMap(m -> m.getMetrics().stream())
      .map(Metric::getKey)
      .collect(Collectors.toSet());
  }

  /**
   * Constructor override used by the ioc container to instantiate the class when no plugin is defining metrics
   */
  @Autowired(required = false)
  public LoadMeasureComputersStep(MutableMeasureComputersHolder measureComputersHolder, MeasureComputer[] measureComputers) {
    this(measureComputersHolder, new Metrics[] {}, measureComputers);
  }

  /**
   * Constructor override used by the ioc container to instantiate the class when no plugin is defining measure computers
   */
  @Autowired(required = false)
  public LoadMeasureComputersStep(MutableMeasureComputersHolder measureComputersHolder, Metrics[] metricsRepositories) {
    this(measureComputersHolder, metricsRepositories, new MeasureComputer[] {});
  }

  /**
   * Constructor override used by the ioc container to instantiate the class when no plugin is defining metrics neither measure computers
   */
  @Autowired(required = false)
  public LoadMeasureComputersStep(MutableMeasureComputersHolder measureComputersHolder) {
    this(measureComputersHolder, new Metrics[] {}, new MeasureComputer[] {});
  }

  @Override
  public void execute(Context context) {
    List<MeasureComputerWrapper> wrappers = Arrays.stream(measureComputers).map(ToMeasureWrapper.INSTANCE).toList();
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
    wrappers.stream().flatMap(s -> ToInputMetrics.INSTANCE.apply(s).stream()).forEach(this::validateInputMetric);
    wrappers.stream().flatMap(s -> ToOutputMetrics.INSTANCE.apply(s).stream()).forEach(this::validateOutputMetric);
    ValidateUniqueOutputMetric validateUniqueOutputMetric = new ValidateUniqueOutputMetric();
    wrappers.forEach(validateUniqueOutputMetric::validate);
  }

  private static Collection<MeasureComputerWrapper> getDependencies(MeasureComputerWrapper measureComputer, ToComputerByKey toComputerByOutputMetricKey) {
    // Remove null computer because a computer can depend on a metric that is only generated by a sensor or on a core metrics
    return measureComputer.getDefinition().getInputMetrics().stream()
      .map(toComputerByOutputMetricKey)
      .filter(Objects::nonNull)
      .toList();
  }

  private static Collection<MeasureComputerWrapper> getDependents(MeasureComputerWrapper measureComputer, ToComputerByKey toComputerByInputMetricKey) {
    return measureComputer.getDefinition().getInputMetrics().stream()
      .map(toComputerByInputMetricKey)
      .toList();
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

  private void validateInputMetric(String metric) {
    checkState(pluginMetricKeys.contains(metric) || CORE_METRIC_KEYS.contains(metric),
      "Metric '%s' cannot be used as an input metric as it's not a core metric and no plugin declare this metric", metric);
  }

  private enum ToOutputMetrics implements Function<MeasureComputerWrapper, Collection<String>> {
    INSTANCE;

    @Override
    public Collection<String> apply(@Nonnull MeasureComputerWrapper input) {
      return input.getDefinition().getOutputMetrics();
    }
  }

  private void validateOutputMetric(String metric) {
    checkState(!CORE_METRIC_KEYS.contains(metric), "Metric '%s' cannot be used as an output metric because it's a core metric", metric);
    checkState(pluginMetricKeys.contains(metric), "Metric '%s' cannot be used as an output metric because no plugins declare this metric", metric);
  }

  private static class ValidateUniqueOutputMetric {
    private final Set<String> allOutputMetrics = new HashSet<>();

    public boolean validate(@Nonnull MeasureComputerWrapper wrapper) {
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

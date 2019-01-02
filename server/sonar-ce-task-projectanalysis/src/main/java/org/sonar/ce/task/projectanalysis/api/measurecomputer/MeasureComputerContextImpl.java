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
package org.sonar.ce.task.projectanalysis.api.measurecomputer;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Issue;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer.MeasureComputerContext;
import org.sonar.api.ce.measure.MeasureComputer.MeasureComputerDefinition;
import org.sonar.api.ce.measure.Settings;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepository;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.core.issue.DefaultIssue;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class MeasureComputerContextImpl implements MeasureComputerContext {

  private final ConfigurationRepository config;
  private final MeasureRepository measureRepository;
  private final MetricRepository metricRepository;

  private final org.sonar.ce.task.projectanalysis.component.Component internalComponent;
  private final Component component;
  private final List<DefaultIssue> componentIssues;

  private MeasureComputerDefinition definition;
  private Set<String> allowedMetrics;

  public MeasureComputerContextImpl(org.sonar.ce.task.projectanalysis.component.Component component, ConfigurationRepository config,
    MeasureRepository measureRepository, MetricRepository metricRepository, ComponentIssuesRepository componentIssuesRepository) {
    this.config = config;
    this.internalComponent = component;
    this.measureRepository = measureRepository;
    this.metricRepository = metricRepository;
    this.component = newComponent(component);
    this.componentIssues = componentIssuesRepository.getIssues(component);
  }

  /**
   * Definition needs to be reset each time a new computer is processed.
   * Defining it by a setter allows to reduce the number of this class to be created (one per component instead of one per component and per computer).
   */
  public MeasureComputerContextImpl setDefinition(MeasureComputerDefinition definition) {
    this.definition = definition;
    this.allowedMetrics = allowedMetric(definition);
    return this;
  }

  private static Set<String> allowedMetric(MeasureComputerDefinition definition) {
    Set<String> allowedMetrics = new HashSet<>();
    allowedMetrics.addAll(definition.getInputMetrics());
    allowedMetrics.addAll(definition.getOutputMetrics());
    return allowedMetrics;
  }

  @Override
  public Component getComponent() {
    return component;
  }

  @Override
  public Settings getSettings() {
    return new Settings() {
      @Override
      @CheckForNull
      public String getString(String key) {
        return config.getConfiguration().get(key).orElse(null);
      }

      @Override
      public String[] getStringArray(String key) {
        return config.getConfiguration().getStringArray(key);
      }
    };
  }

  @Override
  @CheckForNull
  public Measure getMeasure(String metric) {
    validateInputMetric(metric);
    Optional<org.sonar.ce.task.projectanalysis.measure.Measure> measure = measureRepository.getRawMeasure(internalComponent, metricRepository.getByKey(metric));
    if (measure.isPresent()) {
      return new MeasureImpl(measure.get());
    }
    return null;
  }

  @Override
  public Iterable<Measure> getChildrenMeasures(String metric) {
    validateInputMetric(metric);
    return FluentIterable.from(internalComponent.getChildren())
      .transform(new ComponentToMeasure(metricRepository.getByKey(metric)))
      .transform(ToMeasureAPI.INSTANCE)
      .filter(Predicates.notNull());
  }

  @Override
  public void addMeasure(String metricKey, int value) {
    Metric metric = metricRepository.getByKey(metricKey);
    validateAddMeasure(metric);
    measureRepository.add(internalComponent, metric, newMeasureBuilder().create(value));
  }

  @Override
  public void addMeasure(String metricKey, double value) {
    Metric metric = metricRepository.getByKey(metricKey);
    validateAddMeasure(metric);
    measureRepository.add(internalComponent, metric, newMeasureBuilder().create(value, metric.getDecimalScale()));
  }

  @Override
  public void addMeasure(String metricKey, long value) {
    Metric metric = metricRepository.getByKey(metricKey);
    validateAddMeasure(metric);
    measureRepository.add(internalComponent, metric, newMeasureBuilder().create(value));
  }

  @Override
  public void addMeasure(String metricKey, String value) {
    Metric metric = metricRepository.getByKey(metricKey);
    validateAddMeasure(metric);
    measureRepository.add(internalComponent, metric, newMeasureBuilder().create(value));
  }

  @Override
  public void addMeasure(String metricKey, boolean value) {
    Metric metric = metricRepository.getByKey(metricKey);
    validateAddMeasure(metric);
    measureRepository.add(internalComponent, metric, newMeasureBuilder().create(value));
  }

  private void validateInputMetric(String metric) {
    checkArgument(allowedMetrics.contains(metric), "Only metrics in %s can be used to load measures", definition.getInputMetrics());
  }

  private void validateAddMeasure(Metric metric) {
    checkArgument(definition.getOutputMetrics().contains(metric.getKey()), "Only metrics in %s can be used to add measures. Metric '%s' is not allowed.",
      definition.getOutputMetrics(), metric.getKey());
    if (measureRepository.getRawMeasure(internalComponent, metric).isPresent()) {
      throw new UnsupportedOperationException(String.format("A measure on metric '%s' already exists on component '%s'", metric.getKey(), internalComponent.getDbKey()));
    }
  }

  @Override
  public List<? extends Issue> getIssues() {
    return componentIssues;
  }

  private static Component newComponent(org.sonar.ce.task.projectanalysis.component.Component component) {
    return new ComponentImpl(
      component.getDbKey(),
      Component.Type.valueOf(component.getType().name()),
      component.getType() == org.sonar.ce.task.projectanalysis.component.Component.Type.FILE
        ? new ComponentImpl.FileAttributesImpl(component.getFileAttributes().getLanguageKey(), component.getFileAttributes().isUnitTest())
        : null);
  }

  private class ComponentToMeasure
    implements Function<org.sonar.ce.task.projectanalysis.component.Component, Optional<org.sonar.ce.task.projectanalysis.measure.Measure>> {

    private final Metric metric;

    public ComponentToMeasure(Metric metric) {
      this.metric = metric;
    }

    @Override
    public Optional<org.sonar.ce.task.projectanalysis.measure.Measure> apply(@Nonnull org.sonar.ce.task.projectanalysis.component.Component input) {
      return measureRepository.getRawMeasure(input, metric);
    }
  }

  private enum ToMeasureAPI implements Function<Optional<org.sonar.ce.task.projectanalysis.measure.Measure>, Measure> {
    INSTANCE;

    @Nullable
    @Override
    public Measure apply(@Nonnull Optional<org.sonar.ce.task.projectanalysis.measure.Measure> input) {
      return input.isPresent() ? new MeasureImpl(input.get()) : null;
    }
  }

}

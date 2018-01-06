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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Issue;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.Settings;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.api.ce.measure.MeasureComputer.MeasureComputerContext;
import static org.sonar.api.ce.measure.MeasureComputer.MeasureComputerDefinition;

public class TestMeasureComputerContext implements MeasureComputerContext {

  private final Component component;
  private final MeasureComputerDefinition definition;
  private final Settings settings;

  private Map<String, Measure> componentMeasureByMetricKey = new HashMap<>();
  private Multimap<String, Measure> childrenComponentMeasureByMetricKey = ArrayListMultimap.create();
  private List<Issue> issues = new ArrayList<>();

  public TestMeasureComputerContext(Component component, Settings settings, MeasureComputerDefinition definition) {
    this.settings = settings;
    this.component = component;
    this.definition = definition;
  }

  @Override
  public Component getComponent() {
    return component;
  }

  @Override
  public Settings getSettings() {
    return settings;
  }

  @Override
  @CheckForNull
  public Measure getMeasure(String metric) {
    validateInputMetric(metric);
    return componentMeasureByMetricKey.get(metric);
  }

  @Override
  public Iterable<Measure> getChildrenMeasures(String metric) {
    validateInputMetric(metric);
    return childrenComponentMeasureByMetricKey.get(metric);
  }

  @Override
  public void addMeasure(String metricKey, int value) {
    validateAddMeasure(metricKey);
    componentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
  }

  public void addInputMeasure(String metricKey, int value) {
    componentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
  }

  public void addChildrenMeasures(String metricKey, Integer... values) {
    for (Integer value : values) {
      childrenComponentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
    }
  }

  @Override
  public void addMeasure(String metricKey, double value) {
    validateAddMeasure(metricKey);
    componentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
  }

  public void addInputMeasure(String metricKey, double value) {
    componentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
  }

  public void addChildrenMeasures(String metricKey, Double... values) {
    for (Double value : values) {
      childrenComponentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
    }
  }

  @Override
  public void addMeasure(String metricKey, long value) {
    validateAddMeasure(metricKey);
    componentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
  }

  public void addInputMeasure(String metricKey, long value) {
    componentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
  }

  public void addChildrenMeasures(String metricKey, Long... values) {
    for (Long value : values) {
      childrenComponentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
    }
  }

  @Override
  public void addMeasure(String metricKey, String value) {
    validateAddMeasure(metricKey);
    componentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
  }

  @Override
  public void addMeasure(String metricKey, boolean value) {
    validateAddMeasure(metricKey);
    componentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
  }


  public void addInputMeasure(String metricKey, boolean value) {
    componentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
  }

  public void addInputMeasure(String metricKey, String value) {
    componentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
  }

  public void addChildrenMeasures(String metricKey, String... values) {
    for (String value : values) {
      childrenComponentMeasureByMetricKey.put(metricKey, TestMeasure.createMeasure(value));
    }
  }

  @Override
  public List<Issue> getIssues() {
    return issues;
  }

  public void setIssues(List<Issue> issues) {
    this.issues = issues;
  }

  private void validateInputMetric(String metric) {
    Set<String> allowedMetrics = new HashSet<>();
    allowedMetrics.addAll(definition.getInputMetrics());
    allowedMetrics.addAll(definition.getOutputMetrics());
    checkArgument(allowedMetrics.contains(metric), "Only metrics in %s can be used to load measures", definition.getInputMetrics());
  }

  private void validateAddMeasure(String metricKey) {
    checkArgument(definition.getOutputMetrics().contains(metricKey), "Only metrics in %s can be used to add measures. Metric '%s' is not allowed.",
      definition.getOutputMetrics(), metricKey);
    if (componentMeasureByMetricKey.get(metricKey) != null) {
      throw new UnsupportedOperationException(String.format("A measure on metric '%s' already exists", metricKey));
    }
  }
}

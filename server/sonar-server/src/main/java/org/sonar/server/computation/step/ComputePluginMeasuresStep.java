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

import com.google.common.base.Optional;
import java.util.List;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.MetricDefinition;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.ProjectSettingsRepository;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.MetricRepository;

import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

/**
 * TODO
 */
public class ComputePluginMeasuresStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  // TODO We should not inject MetricsDefinition[] but a list of Metrics (took from the parent pico)
  private final MetricDefinition[] metricDefinitions;
  private final ProjectSettingsRepository settings;

  public ComputePluginMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository, MetricDefinition[] metricDefinitions,
    ProjectSettingsRepository settings) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.metricDefinitions = metricDefinitions;
    this.settings = settings;
  }

  @Override
  public void execute() {
    new NewMetricDefinitionsVisitor().visit(treeRootHolder.getRoot());
  }

  private class NewMetricDefinitionsVisitor extends DepthTraversalTypeAwareVisitor {
    public NewMetricDefinitionsVisitor() {
      super(FILE, PRE_ORDER);
    }

    @Override
    public void visitAny(org.sonar.server.computation.component.Component component) {
      MetricDefinition.NewMetricContext newMetricContext = new MetricDefinition.NewMetricContext();
      for (MetricDefinition metricDefinition : metricDefinitions) {
        metricDefinition.define(newMetricContext);
      }

      // TODO use DirectAcyclicGraph to execute measure computation in the right order
      for (MetricDefinition.Metric metric : newMetricContext.getMetrics()) {
        MeasureComputerContextImpl measureComputerContext = new MeasureComputerContextImpl(component, metric);
        metric.getComputer().getMeasureComputer().compute(measureComputerContext);
      }
    }
  }

  @Override
  public String getDescription() {
    return "Compute measures from plugin";
  }

  private class MeasureComputerContextImpl implements MetricDefinition.MeasureComputerContext {

    private final org.sonar.server.computation.component.Component component;
    private final MetricDefinition.Metric metricDefinition;

    public MeasureComputerContextImpl(org.sonar.server.computation.component.Component component, MetricDefinition.Metric metricDefinition) {
      this.component = component;
      this.metricDefinition = metricDefinition;
    }

    @Override
    public Settings getSettings() {
      return settings.getProjectSettings(component.getKey());
    }

    @Override
    public MetricDefinition.Component getComponent() {
      return new ComponentImpl(component);
    }

    @Override
    public MetricDefinition.Measure getMeasure(String metricKey) {
      // TODO Validate that this metric owns to the metric input metrics
      Optional<org.sonar.server.computation.measure.Measure> measure = measureRepository.getRawMeasure(component, metricRepository.getByKey(metricKey));
      if (measure.isPresent()) {
        return new MeasureImpl(measure.get());
      }
      return null;
    }

    @Override
    public List<MetricDefinition.Measure> getChildrenMeasures(String metricKey) {
      // TODO Validate that this metric owns to the metric input metrics
      return null;
    }

    @Override
    public void saveMeasure(int value) {
      measureRepository.add(component, metricRepository.getByKey(metricDefinition.getKey()), newMeasureBuilder().create(value));
    }

    @Override
    public void saveMeasure(double value) {
      measureRepository.add(component, metricRepository.getByKey(metricDefinition.getKey()), newMeasureBuilder().create(value));
    }

    @Override
    public void saveMeasure(long value) {
      measureRepository.add(component, metricRepository.getByKey(metricDefinition.getKey()), newMeasureBuilder().create(value));
    }

    @Override
    public void saveMeasure(String value) {
      measureRepository.add(component, metricRepository.getByKey(metricDefinition.getKey()), newMeasureBuilder().create(value));
    }
  }

  private static class ComponentImpl implements MetricDefinition.Component {

    private final Type type;
    private final boolean isUnitTest;

    public ComponentImpl(org.sonar.server.computation.component.Component component) {
      this.type = Type.valueOf(component.getType().name());
      this.isUnitTest = component.getFileAttributes().isUnitTest();
    }

    @Override
    public Type getType() {
      return type;
    }

    @Override
    public boolean isUnitTest() {
      return isUnitTest;
    }
  }

  private static class MeasureImpl implements MetricDefinition.Measure {

    private final org.sonar.server.computation.measure.Measure measure;

    private MeasureImpl(org.sonar.server.computation.measure.Measure measure) {
      this.measure = measure;
    }

    @Override
    public int getIntValue() {
      return measure.getIntValue();
    }

    @Override
    public long getLongValue() {
      return measure.getLongValue();
    }

    @Override
    public double getDoubleValue() {
      return measure.getDoubleValue();
    }

    @Override
    public String getStringValue() {
      return measure.getStringValue();
    }
  }
}

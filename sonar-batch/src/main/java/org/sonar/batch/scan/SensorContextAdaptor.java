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
package org.sonar.batch.scan;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.HighlightingBuilder;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueBuilder;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueBuilder;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.MeasureBuilder;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasureBuilder;
import org.sonar.api.batch.sensor.symbol.SymbolTableBuilder;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.measures.Formula;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.SumChildDistributionFormula;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.highlighting.DefaultHighlightingBuilder;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.batch.symbol.DefaultSymbolTableBuilder;

import java.io.Serializable;

/**
 * Implements {@link SensorContext} but forward everything to {@link org.sonar.api.batch.SensorContext} for backward compatibility.
 *
 */
public class SensorContextAdaptor implements SensorContext {

  private org.sonar.api.batch.SensorContext sensorContext;
  private MetricFinder metricFinder;
  private Project project;
  private ResourcePerspectives perspectives;
  private Settings settings;
  private FileSystem fs;
  private ActiveRules activeRules;
  private ComponentDataCache componentDataCache;

  public SensorContextAdaptor(org.sonar.api.batch.SensorContext sensorContext, MetricFinder metricFinder, Project project, ResourcePerspectives perspectives,
    Settings settings, FileSystem fs, ActiveRules activeRules, ComponentDataCache componentDataCache) {
    this.sensorContext = sensorContext;
    this.metricFinder = metricFinder;
    this.project = project;
    this.perspectives = perspectives;
    this.settings = settings;
    this.fs = fs;
    this.activeRules = activeRules;
    this.componentDataCache = componentDataCache;
  }

  @Override
  public Settings settings() {
    return settings;
  }

  @Override
  public FileSystem fileSystem() {
    return fs;
  }

  @Override
  public ActiveRules activeRules() {
    return activeRules;
  }

  @Override
  public <G extends Serializable> MeasureBuilder<G> measureBuilder() {
    return new DefaultMeasureBuilder<G>();
  }

  @Override
  public Measure getMeasure(String metricKey) {
    Metric<?> m = findMetricOrFail(metricKey);
    return getMeasure(m);
  }

  @Override
  public <G extends Serializable> Measure<G> getMeasure(Metric<G> metric) {
    org.sonar.api.measures.Metric<G> m = (org.sonar.api.measures.Metric<G>) findMetricOrFail(metric.key());
    org.sonar.api.measures.Measure<G> measure = sensorContext.getMeasure(m);
    if (measure == null) {
      return null;
    }
    return this.<G>measureBuilder()
      .onProject()
      .forMetric(metric)
      .withValue(measure.value())
      .build();
  }

  @Override
  public Measure getMeasure(InputFile file, String metricKey) {
    Metric<?> m = findMetricOrFail(metricKey);
    return getMeasure(file, m);
  }

  private Metric<?> findMetricOrFail(String metricKey) {
    Metric<?> m = metricFinder.findByKey(metricKey);
    if (m == null) {
      throw new IllegalStateException("Unknow metric with key: " + metricKey);
    }
    return m;
  }

  @Override
  public <G extends Serializable> Measure<G> getMeasure(InputFile file, Metric<G> metric) {
    File fileRes = File.create(file.relativePath());
    org.sonar.api.measures.Metric<G> m = (org.sonar.api.measures.Metric<G>) findMetricOrFail(metric.key());
    org.sonar.api.measures.Measure<G> measure = sensorContext.getMeasure(fileRes, m);
    if (measure == null) {
      return null;
    }
    return this.<G>measureBuilder()
      .onFile(file)
      .forMetric(metric)
      .withValue(measure.value())
      .build();
  }

  @Override
  public void addMeasure(Measure<?> measure) {
    org.sonar.api.measures.Metric<?> m = metricFinder.findByKey(measure.metric().key());
    if (m == null) {
      throw new IllegalStateException("Unknow metric with key: " + measure.metric().key());
    }

    org.sonar.api.measures.Measure measureToSave = new org.sonar.api.measures.Measure(m);
    setValueAccordingToMetricType(measure, m, measureToSave);
    if (measure.inputFile() != null) {
      Formula formula = measure.metric() instanceof org.sonar.api.measures.Metric ?
        ((org.sonar.api.measures.Metric) measure.metric()).getFormula() : null;
      if (formula instanceof SumChildDistributionFormula
        && !Scopes.isHigherThanOrEquals(Scopes.FILE, ((SumChildDistributionFormula) formula).getMinimumScopeToPersist())) {
        measureToSave.setPersistenceMode(PersistenceMode.MEMORY);
      }
      sensorContext.saveMeasure(measure.inputFile(), measureToSave);
    } else {
      sensorContext.saveMeasure(measureToSave);
    }
  }

  private void setValueAccordingToMetricType(Measure<?> measure, org.sonar.api.measures.Metric<?> m, org.sonar.api.measures.Measure measureToSave) {
    switch (m.getType()) {
      case BOOL:
        measureToSave.setValue(Boolean.TRUE.equals(measure.value()) ? 1.0 : 0.0);
        break;
      case INT:
      case MILLISEC:
        measureToSave.setValue(Double.valueOf((Integer) measure.value()));
        break;
      case FLOAT:
      case PERCENT:
      case RATING:
        measureToSave.setValue((Double) measure.value());
        break;
      case STRING:
      case LEVEL:
      case DATA:
      case DISTRIB:
        measureToSave.setData((String) measure.value());
        break;
      case WORK_DUR:
        measureToSave.setValue(Double.valueOf((Long) measure.value()));
        break;
      default:
        if (m.isNumericType()) {
          measureToSave.setValue((Double) measure.value());
        } else if (m.isDataType()) {
          measureToSave.setData((String) measure.value());
        } else {
          throw new UnsupportedOperationException("Unsupported type :" + m.getType());
        }
    }
  }

  @Override
  public IssueBuilder issueBuilder() {
    return new DefaultIssueBuilder();
  }

  @Override
  public boolean addIssue(Issue issue) {
    Resource r;
    InputPath inputPath = issue.inputPath();
    if (inputPath != null) {
      if (inputPath instanceof InputDir) {
        r = Directory.create(inputPath.relativePath());
      } else {
        r = File.create(inputPath.relativePath());
      }
    } else {
      r = project;
    }
    Issuable issuable = perspectives.as(Issuable.class, r);
    if (issuable == null) {
      return false;
    }
    return issuable.addIssue(toDefaultIssue(project.getKey(), r.getKey(), issue));
  }

  public static DefaultIssue toDefaultIssue(String projectKey, String componentKey, Issue issue) {
    return new org.sonar.core.issue.DefaultIssueBuilder()
      .componentKey(componentKey)
      .projectKey(projectKey)
      .ruleKey(RuleKey.of(issue.ruleKey().repository(), issue.ruleKey().rule()))
      .effortToFix(issue.effortToFix())
      .line(issue.line())
      .message(issue.message())
      .severity(issue.severity())
      .build();
  }

  @Override
  public HighlightingBuilder highlightingBuilder(InputFile inputFile) {
    return new DefaultHighlightingBuilder(((DefaultInputFile) inputFile).key(), componentDataCache);
  }

  @Override
  public SymbolTableBuilder symbolTableBuilder(InputFile inputFile) {
    return new DefaultSymbolTableBuilder(((DefaultInputFile) inputFile).key(), componentDataCache);
  }

}

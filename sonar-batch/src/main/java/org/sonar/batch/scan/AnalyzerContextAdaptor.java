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

import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.api.analyzer.AnalyzerContext;
import org.sonar.batch.api.analyzer.issue.AnalyzerIssue;
import org.sonar.batch.api.analyzer.measure.AnalyzerMeasure;
import org.sonar.batch.api.measures.Metric;

import java.io.Serializable;
import java.util.Collection;

public class AnalyzerContextAdaptor implements AnalyzerContext {

  private SensorContext sensorContext;
  private MetricFinder metricFinder;
  private Project project;
  private ResourcePerspectives perspectives;

  public AnalyzerContextAdaptor(SensorContext sensorContext, MetricFinder metricFinder, Project project, ResourcePerspectives perspectives) {
    this.sensorContext = sensorContext;
    this.metricFinder = metricFinder;
    this.project = project;
    this.perspectives = perspectives;
  }

  @Override
  public AnalyzerMeasure<?> getMeasure(String metricKey) {
    Metric<?> m = metricFinder.findByKey(metricKey);
    if (m == null) {
      throw new IllegalStateException("Unknow metric with key: " + metricKey);
    }
    return getMeasure(m);
  }

  @Override
  public <G extends Serializable> AnalyzerMeasure<G> getMeasure(Metric<G> metric) {
    org.sonar.api.measures.Metric<G> m = metricFinder.findByKey(metric.key());
    Measure<G> measure = sensorContext.getMeasure(m);
    if (measure == null) {
      return null;
    }
    return AnalyzerMeasure.<G>builder()
      .onProject()
      .forMetric(metric)
      .withValue(measure.value())
      .build();
  }

  @Override
  public AnalyzerMeasure<?> getMeasure(InputFile file, String metricKey) {
    Metric<?> m = metricFinder.findByKey(metricKey);
    if (m == null) {
      throw new IllegalStateException("Unknow metric with key: " + metricKey);
    }
    return getMeasure(file, m);
  }

  @Override
  public <G extends Serializable> AnalyzerMeasure<G> getMeasure(InputFile file, Metric<G> metric) {
    File fileRes = File.fromIOFile(file.file(), project);
    org.sonar.api.measures.Metric<G> m = metricFinder.findByKey(metric.key());
    Measure<G> measure = sensorContext.getMeasure(fileRes, m);
    if (measure == null) {
      return null;
    }
    return AnalyzerMeasure.<G>builder()
      .onFile(file)
      .forMetric(metric)
      .withValue(measure.value())
      .build();
  }

  @Override
  public void addMeasure(AnalyzerMeasure<?> measure) {
    org.sonar.api.measures.Metric<?> m = metricFinder.findByKey(measure.metricKey());

    Measure measureToSave = new Measure(m);
    switch (m.getType()) {
      case BOOL:
        measureToSave.setValue(Boolean.TRUE.equals(measure.value()) ? 1.0 : 0.0);
        break;
      case INT:
      case MILLISEC:
        measureToSave.setValue(Double.valueOf(((Integer) measure.value())));
        break;
      case FLOAT:
      case PERCENT:
      case RATING:
        measureToSave.setValue(((Double) measure.value()));
        break;
      case STRING:
      case LEVEL:
      case DATA:
      case DISTRIB:
        measureToSave.setData(((String) measure.value()));
        break;
      case WORK_DUR:
        measureToSave.setValue(Double.valueOf(((Long) measure.value())));
        break;
      default:
        if (m.isNumericType()) {
          measureToSave.setValue(((Double) measure.value()));
        } else if (m.isDataType()) {
          measureToSave.setData(((String) measure.value()));
        } else {
          throw new UnsupportedOperationException("Unsupported type :" + m.getType());
        }
    }
    if (measure.inputFile() != null) {
      File fileRes = File.fromIOFile(measure.inputFile().file(), project);
      sensorContext.saveMeasure(fileRes, measureToSave);
    } else {
      sensorContext.saveMeasure(measureToSave);
    }
  }

  @Override
  public void addIssue(AnalyzerIssue issue) {
    Resource r;
    if (issue.inputFile() != null) {
      r = File.fromIOFile(issue.inputFile().file(), project);
    } else {
      r = project;
    }
    Issuable issuable = perspectives.as(Issuable.class, r);
    issuable.addIssue(issuable.newIssueBuilder()
      .ruleKey(RuleKey.of(issue.ruleKey().repository(), issue.ruleKey().rule()))
      .effortToFix(issue.effortToFix())
      .line(issue.line())
      .message(issue.message())
      .build());
  }

  @Override
  public void addIssues(Collection<AnalyzerIssue> issues) {
    for (AnalyzerIssue analyzerIssue : issues) {
      addIssue(analyzerIssue);
    }
  }

}

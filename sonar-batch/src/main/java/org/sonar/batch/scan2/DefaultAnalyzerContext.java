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
package org.sonar.batch.scan2;

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueBuilder;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueBuilder;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.MeasureBuilder;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasureBuilder;

import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.config.Settings;
import org.sonar.batch.issue.IssueFilters;
import org.sonar.batch.scan.SensorContextAdaptor;
import org.sonar.core.component.ComponentKeys;

import java.io.Serializable;

public class DefaultAnalyzerContext implements SensorContext {

  private final AnalyzerMeasureCache measureCache;
  private final AnalyzerIssueCache issueCache;
  private final ProjectDefinition def;
  private final Settings settings;
  private final FileSystem fs;
  private final ActiveRules activeRules;
  private final IssueFilters issueFilters;

  public DefaultAnalyzerContext(ProjectDefinition def, AnalyzerMeasureCache measureCache, AnalyzerIssueCache issueCache,
    Settings settings, FileSystem fs, ActiveRules activeRules, IssueFilters issueFilters) {
    this.def = def;
    this.measureCache = measureCache;
    this.issueCache = issueCache;
    this.settings = settings;
    this.fs = fs;
    this.activeRules = activeRules;
    this.issueFilters = issueFilters;
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
    return measureCache.byMetric(def.getKey(), def.getKey(), metricKey);
  }

  @Override
  public <G extends Serializable> Measure<G> getMeasure(Metric<G> metric) {
    return (Measure<G>) measureCache.byMetric(def.getKey(), def.getKey(), metric.key());
  }

  @Override
  public Measure getMeasure(InputFile file, String metricKey) {
    return measureCache.byMetric(def.getKey(), ComponentKeys.createEffectiveKey(def.getKey(), file), metricKey);
  }

  @Override
  public <G extends Serializable> Measure<G> getMeasure(InputFile file, Metric<G> metric) {
    return (Measure<G>) measureCache.byMetric(def.getKey(), ComponentKeys.createEffectiveKey(def.getKey(), file), metric.key());
  }

  @Override
  public void addMeasure(Measure<?> measure) {
    if (measure.inputFile() != null) {
      measureCache.put(def.getKey(), ComponentKeys.createEffectiveKey(def.getKey(), measure.inputFile()), (DefaultMeasure) measure);
    } else {
      measureCache.put(def.getKey(), def.getKey(), (DefaultMeasure) measure);
    }
  }

  @Override
  public IssueBuilder issueBuilder() {
    return new DefaultIssueBuilder();
  }

  @Override
  public boolean addIssue(Issue issue) {
    String resourceKey;
    if (issue.inputFile() != null) {
      resourceKey = ComponentKeys.createEffectiveKey(def.getKey(), issue.inputFile());
    } else {
      resourceKey = def.getKey();
    }
    // TODO Lot of things to do. See ModuleIssues::initAndAddIssue
    if (issueFilters.accept(SensorContextAdaptor.toDefaultIssue(def.getKey(), resourceKey, issue), null)) {
      issueCache.put(def.getKey(), resourceKey, (DefaultIssue) issue);
      return true;
    }

    return false;
  }
}

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

import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.api.analyzer.AnalyzerContext;
import org.sonar.batch.api.analyzer.issue.AnalyzerIssue;
import org.sonar.batch.api.analyzer.measure.AnalyzerMeasure;
import org.sonar.batch.api.measures.Metric;
import org.sonar.batch.issue.ModuleIssues;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.issue.DefaultIssueBuilder;

import java.io.Serializable;
import java.util.Collection;

public class DefaultAnalyzerContext implements AnalyzerContext {

  private final MeasureCache measureCache;
  private ProjectDefinition def;
  private ModuleIssues moduleIssues;

  public DefaultAnalyzerContext(ProjectDefinition def, MeasureCache measureCache, ModuleIssues moduleIssues) {
    this.def = def;
    this.measureCache = measureCache;
    this.moduleIssues = moduleIssues;
  }

  @Override
  public AnalyzerMeasure getMeasure(String metricKey) {
    return measureCache.byMetric(def.getKey(), metricKey);
  }

  @Override
  public <G extends Serializable> AnalyzerMeasure<G> getMeasure(Metric<G> metric) {
    return (AnalyzerMeasure<G>) measureCache.byMetric(def.getKey(), metric.key());
  }

  @Override
  public AnalyzerMeasure getMeasure(InputFile file, String metricKey) {
    return measureCache.byMetric(ComponentKeys.createEffectiveKey(def.getKey(), file), metricKey);
  }

  @Override
  public <G extends Serializable> AnalyzerMeasure<G> getMeasure(InputFile file, Metric<G> metric) {
    return (AnalyzerMeasure<G>) measureCache.byMetric(ComponentKeys.createEffectiveKey(def.getKey(), file), metric.key());
  }

  @Override
  public void addMeasure(org.sonar.batch.api.analyzer.measure.AnalyzerMeasure<?> measure) {
    if (measure.inputFile() != null) {
      measureCache.put(ComponentKeys.createEffectiveKey(def.getKey(), measure.inputFile()), measure);
    } else {
      measureCache.put(def.getKey(), measure);
    }
  }

  @Override
  public void addIssue(AnalyzerIssue issue) {
    DefaultIssueBuilder builder = new DefaultIssueBuilder()
      .projectKey(def.getKey());
    if (issue.inputFile() != null) {
      builder.componentKey(ComponentKeys.createEffectiveKey(def.getKey(), issue.inputFile()));
    } else {
      builder.componentKey(def.getKey());
    }

    moduleIssues.initAndAddIssue((DefaultIssue) builder
      .ruleKey(RuleKey.of(issue.ruleKey().repository(), issue.ruleKey().rule()))
      .message(issue.message())
      .line(issue.line())
      .effortToFix(issue.effortToFix())
      .build());
  }

  @Override
  public void addIssues(Collection<AnalyzerIssue> issues) {
    for (AnalyzerIssue analyzerIssue : issues) {
      addIssue(analyzerIssue);
    }

  }

}

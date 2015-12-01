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
package org.sonar.api.batch.sensor.internal;

import com.google.common.annotations.Beta;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.highlighting.internal.SyntaxHighlightingRule;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Metric;

/**
 * Utility class to help testing {@link Sensor}.
 * 
 * Usage: call {@link #create(File)} to create an "in memory" implementation of {@link SensorContext} then
 * pass it to your {@link Sensor}. You can then query elements provided by your sensor using methods {@link #allIssues()}, ...
 * 
 * @since 5.1
 */
@Beta
public class SensorContextTester implements SensorContext {

  private Settings settings;
  private DefaultFileSystem fs;
  private ActiveRules activeRules;
  private MockAnalysisMode analysisMode;
  private InMemorySensorStorage sensorStorage;

  private SensorContextTester(File moduleBaseDir) {
    this.settings = new Settings();
    this.fs = new DefaultFileSystem(moduleBaseDir);
    this.activeRules = new ActiveRulesBuilder().build();
    this.analysisMode = new MockAnalysisMode();
    this.sensorStorage = new InMemorySensorStorage();
  }

  public static SensorContextTester create(File moduleBaseDir) {
    return new SensorContextTester(moduleBaseDir);
  }

  @Override
  public Settings settings() {
    return settings;
  }

  public void setSettings(Settings settings) {
    this.settings = settings;
  }

  @Override
  public DefaultFileSystem fileSystem() {
    return fs;
  }

  public void setFileSystem(DefaultFileSystem fs) {
    this.fs = fs;
  }

  @Override
  public ActiveRules activeRules() {
    return activeRules;
  }

  public void setActiveRules(ActiveRules activeRules) {
    this.activeRules = activeRules;
  }

  @Override
  public MockAnalysisMode analysisMode() {
    return analysisMode;
  }

  @Override
  public <G extends Serializable> NewMeasure<G> newMeasure() {
    return new DefaultMeasure<>(sensorStorage);
  }

  public Collection<Measure> measures(String componentKey) {
    return sensorStorage.measuresByComponentAndMetric.row(componentKey).values();
  }

  public <G extends Serializable> Measure<G> measure(String componetKey, Metric<G> metric) {
    return measure(componetKey, metric.key());
  }

  public <G extends Serializable> Measure<G> measure(String componentKey, String metricKey) {
    return sensorStorage.measuresByComponentAndMetric.row(componentKey).get(metricKey);
  }

  @Override
  public NewIssue newIssue() {
    return new DefaultIssue(sensorStorage);
  }

  public Collection<Issue> allIssues() {
    return sensorStorage.allIssues;
  }

  @CheckForNull
  public Integer lineHits(String fileKey, CoverageType type, int line) {
    Map<CoverageType, DefaultCoverage> defaultCoverageByType = sensorStorage.coverageByComponent.get(fileKey);
    if (defaultCoverageByType == null) {
      return null;
    }
    if (defaultCoverageByType.containsKey(type)) {
      return defaultCoverageByType.get(type).hitsByLine().get(line);
    }
    return null;
  }

  @CheckForNull
  public Integer conditions(String fileKey, CoverageType type, int line) {
    Map<CoverageType, DefaultCoverage> defaultCoverageByType = sensorStorage.coverageByComponent.get(fileKey);
    if (defaultCoverageByType == null) {
      return null;
    }
    if (defaultCoverageByType.containsKey(type)) {
      return defaultCoverageByType.get(type).conditionsByLine().get(line);
    }
    return null;
  }

  @CheckForNull
  public Integer coveredConditions(String fileKey, CoverageType type, int line) {
    Map<CoverageType, DefaultCoverage> defaultCoverageByType = sensorStorage.coverageByComponent.get(fileKey);
    if (defaultCoverageByType == null) {
      return null;
    }
    if (defaultCoverageByType.containsKey(type)) {
      return defaultCoverageByType.get(type).coveredConditionsByLine().get(line);
    }
    return null;
  }

  @Override
  public NewHighlighting newHighlighting() {
    return new DefaultHighlighting(sensorStorage);
  }

  @Override
  public NewCoverage newCoverage() {
    return new DefaultCoverage(sensorStorage);
  }

  public List<TypeOfText> highlightingTypeAt(String componentKey, int line, int lineOffset) {
    DefaultHighlighting syntaxHighlightingData = sensorStorage.highlightingByComponent.get(componentKey);
    if (syntaxHighlightingData == null) {
      return Collections.emptyList();
    }
    List<TypeOfText> result = new ArrayList<>();
    DefaultTextPointer location = new DefaultTextPointer(line, lineOffset);
    for (SyntaxHighlightingRule sortedRule : syntaxHighlightingData.getSyntaxHighlightingRuleSet()) {
      if (sortedRule.range().start().compareTo(location) <= 0 && sortedRule.range().end().compareTo(location) > 0) {
        result.add(sortedRule.getTextType());
      }
    }
    return result;
  }

  public static class MockAnalysisMode implements AnalysisMode {
    private boolean isPreview = false;
    private boolean isIssues = false;

    @Override
    public boolean isPreview() {
      return isPreview;
    }

    public void setPreview(boolean value) {
      this.isPreview = value;
    }

    @Override
    public boolean isIssues() {
      return this.isIssues;
    }

    public void setIssues(boolean issues) {
      this.isIssues = issues;
    }

    @Override
    public boolean isPublish() {
      return !isPreview && !isIssues;
    }
  }

  private static class InMemorySensorStorage implements SensorStorage {

    private Table<String, String, Measure> measuresByComponentAndMetric = HashBasedTable.create();

    private Collection<Issue> allIssues = new ArrayList<>();

    private Map<String, DefaultHighlighting> highlightingByComponent = new HashMap<>();
    private Map<String, Map<CoverageType, DefaultCoverage>> coverageByComponent = new HashMap<>();

    @Override
    public void store(Measure measure) {
      measuresByComponentAndMetric.row(measure.inputComponent().key()).put(measure.metric().key(), measure);
    }

    @Override
    public void store(Issue issue) {
      allIssues.add(issue);
    }

    @Override
    public void store(DefaultHighlighting highlighting) {
      highlightingByComponent.put(highlighting.inputFile().key(), highlighting);
    }

    @Override
    public void store(DefaultCoverage defaultCoverage) {
      String key = defaultCoverage.inputFile().key();
      if (!coverageByComponent.containsKey(key)) {
        coverageByComponent.put(key, new EnumMap<CoverageType, DefaultCoverage>(CoverageType.class));
      }
      coverageByComponent.get(key).put(defaultCoverage.type(), defaultCoverage);
    }

  }

}

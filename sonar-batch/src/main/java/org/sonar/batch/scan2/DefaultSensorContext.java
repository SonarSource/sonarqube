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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.DefaultActiveRule;
import org.sonar.api.batch.sensor.dependency.Dependency;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.test.TestCase;
import org.sonar.api.batch.sensor.test.internal.DefaultTestCase;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.dependency.DependencyCache;
import org.sonar.batch.duplication.BlockCache;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.batch.issue.IssueFilters;
import org.sonar.batch.scan.SensorContextAdapter;
import org.sonar.batch.test.CoveragePerTestCache;
import org.sonar.batch.test.TestCaseCache;
import org.sonar.core.component.ComponentKeys;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class DefaultSensorContext extends BaseSensorContext {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultSensorContext.class);

  public static final List<Metric> INTERNAL_METRICS = Arrays.<Metric>asList(
    // Computed by DsmDecorator
    CoreMetrics.DEPENDENCY_MATRIX,
    CoreMetrics.DIRECTORY_CYCLES,
    CoreMetrics.DIRECTORY_EDGES_WEIGHT,
    CoreMetrics.DIRECTORY_FEEDBACK_EDGES,
    CoreMetrics.DIRECTORY_TANGLE_INDEX,
    CoreMetrics.DIRECTORY_TANGLES,
    CoreMetrics.FILE_CYCLES,
    CoreMetrics.FILE_EDGES_WEIGHT,
    CoreMetrics.FILE_FEEDBACK_EDGES,
    CoreMetrics.FILE_TANGLE_INDEX,
    CoreMetrics.FILE_TANGLES,
    // Computed by ScmActivitySensor
    CoreMetrics.SCM_AUTHORS_BY_LINE,
    CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE,
    CoreMetrics.SCM_REVISIONS_BY_LINE,
    // Computed by core duplication plugin
    CoreMetrics.DUPLICATIONS_DATA,
    CoreMetrics.DUPLICATION_LINES_DATA,
    CoreMetrics.DUPLICATED_FILES,
    CoreMetrics.DUPLICATED_LINES,
    CoreMetrics.DUPLICATED_BLOCKS
    );
  private final MeasureCache measureCache;
  private final IssueCache issueCache;
  private final ProjectDefinition def;
  private final ActiveRules activeRules;
  private final IssueFilters issueFilters;
  private final TestCaseCache testCaseCache;
  private final CoveragePerTestCache coveragePerTestCache;
  private final DependencyCache dependencyCache;

  public DefaultSensorContext(ProjectDefinition def, MeasureCache measureCache, IssueCache issueCache,
    Settings settings, FileSystem fs, ActiveRules activeRules, IssueFilters issueFilters, ComponentDataCache componentDataCache,
    BlockCache blockCache, DuplicationCache duplicationCache, TestCaseCache testCaseCache, CoveragePerTestCache coveragePerTestCache, DependencyCache dependencyCache) {
    super(settings, fs, activeRules, componentDataCache, blockCache, duplicationCache);
    this.def = def;
    this.measureCache = measureCache;
    this.issueCache = issueCache;
    this.activeRules = activeRules;
    this.issueFilters = issueFilters;
    this.testCaseCache = testCaseCache;
    this.coveragePerTestCache = coveragePerTestCache;
    this.dependencyCache = dependencyCache;
  }

  @Override
  public void store(Measure newMeasure) {
    DefaultMeasure<Serializable> measure = (DefaultMeasure<Serializable>) newMeasure;
    if (!measure.isFromCore() && INTERNAL_METRICS.contains(measure.metric())) {
      throw new IllegalArgumentException("Metric " + measure.metric().key() + " is an internal metric computed by SonarQube. Please remove or update offending plugin.");
    }
    InputFile inputFile = measure.inputFile();
    if (inputFile != null) {
      measureCache.put(def.getKey(), ComponentKeys.createEffectiveKey(def.getKey(), inputFile), measure);
    } else {
      measureCache.put(def.getKey(), def.getKey(), measure);
    }
  }

  @Override
  public void store(Issue issue) {
    String resourceKey;
    InputPath inputPath = issue.inputPath();
    if (inputPath != null) {
      resourceKey = ComponentKeys.createEffectiveKey(def.getKey(), inputPath);
    } else {
      resourceKey = def.getKey();
    }
    RuleKey ruleKey = issue.ruleKey();
    DefaultActiveRule activeRule = (DefaultActiveRule) activeRules.find(ruleKey);
    if (activeRule == null) {
      // rule does not exist or is not enabled -> ignore the issue
      LOG.debug("Rule {} does not exists or is not enabled. Issue {} is ignored.", issue.ruleKey(), issue);
      return;
    }
    if (Strings.isNullOrEmpty(activeRule.name()) && Strings.isNullOrEmpty(issue.message())) {
      throw MessageException.of(String.format("The rule '%s' has no name and the related issue has no message.", ruleKey));
    }

    updateIssue((DefaultIssue) issue, activeRule);

    if (!issueFilters.accept(SensorContextAdapter.toDefaultIssue(def.getKey(), resourceKey, issue), null)) {
      LOG.debug("Issue {} was excluded by some filters.", issue);
      return;
    }
    issueCache.put(def.getKey(), resourceKey, (DefaultIssue) issue);
  }

  private void updateIssue(DefaultIssue issue, DefaultActiveRule activeRule) {
    if (Strings.isNullOrEmpty(issue.message())) {
      issue.message(activeRule.name());
    }
  }

  @Override
  public void store(TestCase testCase) {
    if (testCaseCache.contains(((DefaultTestCase) testCase).testFile(), testCase.name())) {
      throw new IllegalArgumentException("There is already a test case with the same name: " + testCase.name());
    }
    testCaseCache.put(((DefaultTestCase) testCase).testFile(), testCase);
  }

  @Override
  public void saveCoveragePerTest(TestCase testCase, InputFile coveredFile, List<Integer> coveredLines) {
    Preconditions.checkNotNull(testCase);
    Preconditions.checkArgument(coveredFile.type() == Type.MAIN, "Should be a main file: " + coveredFile);
    coveragePerTestCache.put(testCase, coveredFile, coveredLines);
  }

  @Override
  public void store(Dependency dep) {
    if (dependencyCache.get(def.getKey(), dep.from(), dep.to()) != null) {
      throw new IllegalStateException("Dependency between " + dep.from() + " and " + dep.to() + " was already saved.");
    }
    dependencyCache.put(def.getKey(), dep);
  }

}

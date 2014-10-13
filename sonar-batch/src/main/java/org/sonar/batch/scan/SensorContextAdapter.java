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

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.Issue.Severity;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.test.TestCaseCoverage;
import org.sonar.api.batch.sensor.test.TestCaseExecution;
import org.sonar.api.batch.sensor.test.internal.DefaultTestCaseExecution;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.design.Dependency;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.measures.Formula;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.SumChildDistributionFormula;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.Testable;
import org.sonar.batch.duplication.BlockCache;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.batch.scan2.BaseSensorContext;
import org.sonar.core.component.ComponentKeys;

/**
 * Implements {@link SensorContext} but forward everything to {@link org.sonar.api.batch.SensorContext} for backward compatibility.
 * Will be dropped once old {@link Sensor} API is dropped.
 *
 */
public class SensorContextAdapter extends BaseSensorContext {

  private static final String USES = "USES";
  private final org.sonar.api.batch.SensorContext sensorContext;
  private final MetricFinder metricFinder;
  private final Project project;
  private final ResourcePerspectives perspectives;
  private final SonarIndex sonarIndex;

  public SensorContextAdapter(org.sonar.api.batch.SensorContext sensorContext, MetricFinder metricFinder, Project project, ResourcePerspectives perspectives,
    Settings settings, FileSystem fs, ActiveRules activeRules, ComponentDataCache componentDataCache, BlockCache blockCache,
    DuplicationCache duplicationCache, SonarIndex sonarIndex) {
    super(settings, fs, activeRules, componentDataCache, blockCache, duplicationCache);
    this.sensorContext = sensorContext;
    this.metricFinder = metricFinder;
    this.project = project;
    this.perspectives = perspectives;
    this.sonarIndex = sonarIndex;
  }

  private org.sonar.api.measures.Metric findMetricOrFail(String metricKey) {
    org.sonar.api.measures.Metric m = metricFinder.findByKey(metricKey);
    if (m == null) {
      throw new IllegalStateException("Unknow metric with key: " + metricKey);
    }
    return m;
  }

  @Override
  public void store(Measure newMeasure) {
    DefaultMeasure measure = (DefaultMeasure) newMeasure;
    org.sonar.api.measures.Metric m = findMetricOrFail(measure.metric().key());
    org.sonar.api.measures.Measure measureToSave = new org.sonar.api.measures.Measure(m);
    setValueAccordingToMetricType(newMeasure, m, measureToSave);
    measureToSave.setFromCore(measure.isFromCore());
    if (newMeasure.inputFile() != null) {
      Formula formula = newMeasure.metric() instanceof org.sonar.api.measures.Metric ?
        ((org.sonar.api.measures.Metric) newMeasure.metric()).getFormula() : null;
      if (formula instanceof SumChildDistributionFormula
        && !Scopes.isHigherThanOrEquals(Scopes.FILE, ((SumChildDistributionFormula) formula).getMinimumScopeToPersist())) {
        measureToSave.setPersistenceMode(PersistenceMode.MEMORY);
      }
      sensorContext.saveMeasure(newMeasure.inputFile(), measureToSave);
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
        throw new UnsupportedOperationException("Unsupported type :" + m.getType());
    }
  }

  @Override
  public void store(Issue issue) {
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
      return;
    }
    issuable.addIssue(toDefaultIssue(project.getKey(), ComponentKeys.createEffectiveKey(project, r), issue));
  }

  public static DefaultIssue toDefaultIssue(String projectKey, String componentKey, Issue issue) {
    Severity overridenSeverity = issue.overridenSeverity();
    return new org.sonar.core.issue.DefaultIssueBuilder()
      .componentKey(componentKey)
      .projectKey(projectKey)
      .ruleKey(RuleKey.of(issue.ruleKey().repository(), issue.ruleKey().rule()))
      .effortToFix(issue.effortToFix())
      .line(issue.line())
      .message(issue.message())
      .severity(overridenSeverity != null ? overridenSeverity.name() : null)
      .build();
  }

  @Override
  public void store(TestCaseExecution testCase) {
    File testRes = getTestResource(((DefaultTestCaseExecution) testCase).testFile());
    MutableTestPlan testPlan = perspectives.as(MutableTestPlan.class, testRes);
    if (testPlan != null) {
      testPlan
        .addTestCase(testCase.name())
        .setDurationInMs(testCase.durationInMs())
        .setType(testCase.type().name())
        .setStatus(org.sonar.api.test.TestCase.Status.valueOf(testCase.status().name()))
        .setMessage(testCase.message())
        .setStackTrace(testCase.stackTrace());
    }
  }

  @Override
  public void store(TestCaseCoverage testCaseCoverage) {
    File testRes = getTestResource(testCaseCoverage.testFile());
    File mainRes = getMainResource(testCaseCoverage.coveredFile());
    Testable testAbleFile = perspectives.as(MutableTestable.class, mainRes);
    if (testAbleFile != null) {
      MutableTestPlan testPlan = perspectives.as(MutableTestPlan.class, testRes);
      if (testPlan != null) {
        for (MutableTestCase mutableTestCase : testPlan.testCasesByName(testCaseCoverage.testName())) {
          mutableTestCase.setCoverageBlock(testAbleFile, testCaseCoverage.coveredLines());
        }
      } else {
        throw new IllegalStateException("Unable to get MutableTestPlan perspective from " + testRes);
      }
    } else {
      throw new IllegalStateException("Unable to get MutableTestable perspective from " + mainRes);
    }
  }

  private File getTestResource(InputFile testFile) {
    File testRes = File.create(testFile.relativePath());
    testRes.setQualifier(Qualifiers.UNIT_TEST_FILE);
    // Reload
    testRes = sensorContext.getResource(testRes);
    if (testRes == null) {
      throw new IllegalArgumentException("Provided input file is not indexed or not a test file: " + testFile);
    }
    return testRes;
  }

  private File getMainResource(InputFile mainFile) {
    File mainRes = File.create(mainFile.relativePath());
    // Reload
    mainRes = sensorContext.getResource(mainRes);
    if (mainRes == null) {
      throw new IllegalArgumentException("Provided input file is not indexed or not a main file: " + mainRes);
    }
    return mainRes;
  }

  private File getFile(InputFile file) {
    if (file.type() == InputFile.Type.MAIN) {
      return getMainResource(file);
    } else {
      return getTestResource(file);
    }
  }

  @Override
  public void store(org.sonar.api.batch.sensor.dependency.Dependency dep) {
    File fromResource = getFile(dep.from());
    File toResource = getFile(dep.to());
    if (sonarIndex.getEdge(fromResource, toResource) != null) {
      throw new IllegalStateException("Dependency between " + dep.from() + " and " + dep.to() + " was already saved.");
    }
    Directory fromParent = fromResource.getParent();
    Directory toParent = toResource.getParent();
    Dependency parentDep = null;
    if (!fromParent.equals(toParent)) {
      parentDep = sonarIndex.getEdge(fromParent, toParent);
      if (parentDep != null) {
        parentDep.setWeight(parentDep.getWeight() + 1);
      } else {
        parentDep = new Dependency(fromParent, toParent).setUsage(USES).setWeight(1);
        parentDep = sensorContext.saveDependency(parentDep);
      }
    }
    sensorContext.saveDependency(new Dependency(fromResource, toResource)
      .setUsage(USES)
      .setWeight(dep.weight())
      .setParent(parentDep));
  }

}

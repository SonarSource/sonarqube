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
package org.sonar.batch.sensor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.dependency.internal.DefaultDependency;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.config.Settings;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.issue.ModuleIssues;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.batch.sensor.coverage.CoverageExclusions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultSensorStorageTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ActiveRules activeRules;
  private DefaultFileSystem fs;
  private DefaultSensorStorage sensorStorage;
  private Settings settings;
  private ModuleIssues moduleIssues;
  private Project project;
  private DefaultIndex sonarIndex;

  private ResourceCache resourceCache;

  @Before
  public void prepare() throws Exception {
    activeRules = new ActiveRulesBuilder().build();
    fs = new DefaultFileSystem(temp.newFolder().toPath());
    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.findByKey(CoreMetrics.NCLOC_KEY)).thenReturn(CoreMetrics.NCLOC);
    when(metricFinder.findByKey(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY)).thenReturn(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION);
    settings = new Settings();
    moduleIssues = mock(ModuleIssues.class);
    project = new Project("myProject");
    sonarIndex = mock(DefaultIndex.class);
    CoverageExclusions coverageExclusions = mock(CoverageExclusions.class);
    when(coverageExclusions.accept(any(Resource.class), any(Measure.class))).thenReturn(true);
    resourceCache = new ResourceCache();
    sensorStorage = new DefaultSensorStorage(metricFinder, project,
      moduleIssues, settings, fs, activeRules, mock(DuplicationCache.class), sonarIndex, coverageExclusions, resourceCache, mock(ReportPublisher.class));
  }

  @Test
  public void shouldFailIfUnknowMetric() {
    InputFile file = new DefaultInputFile("foo", "src/Foo.php");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unknow metric with key: lines");

    sensorStorage.store(new DefaultMeasure()
      .onFile(file)
      .forMetric(CoreMetrics.LINES)
      .withValue(10));
  }

  @Test
  public void shouldSaveFileMeasureToSensorContext() {
    InputFile file = new DefaultInputFile("foo", "src/Foo.php");

    ArgumentCaptor<org.sonar.api.measures.Measure> argumentCaptor = ArgumentCaptor.forClass(org.sonar.api.measures.Measure.class);
    Resource sonarFile = File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php");
    resourceCache.add(sonarFile, null).setInputPath(file);
    when(sonarIndex.addMeasure(eq(sonarFile), argumentCaptor.capture())).thenReturn(null);
    sensorStorage.store(new DefaultMeasure()
      .onFile(file)
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    org.sonar.api.measures.Measure m = argumentCaptor.getValue();
    assertThat(m.getValue()).isEqualTo(10.0);
    assertThat(m.getMetric()).isEqualTo(CoreMetrics.NCLOC);
  }

  @Test
  public void shouldSetAppropriatePersistenceMode() {
    // Metric FUNCTION_COMPLEXITY_DISTRIBUTION is only persisted on directories.

    InputFile file = new DefaultInputFile("foo", "src/Foo.php");

    ArgumentCaptor<org.sonar.api.measures.Measure> argumentCaptor = ArgumentCaptor.forClass(org.sonar.api.measures.Measure.class);
    Resource sonarFile = File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php");
    resourceCache.add(sonarFile, null).setInputPath(file);

    when(sonarIndex.addMeasure(eq(sonarFile), argumentCaptor.capture())).thenReturn(null);

    sensorStorage.store(new DefaultMeasure()
      .onFile(file)
      .forMetric(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION)
      .withValue("foo"));

    org.sonar.api.measures.Measure m = argumentCaptor.getValue();
    assertThat(m.getData()).isEqualTo("foo");
    assertThat(m.getMetric()).isEqualTo(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION);
    assertThat(m.getPersistenceMode()).isEqualTo(PersistenceMode.MEMORY);

  }

  @Test
  public void shouldSaveProjectMeasureToSensorContext() {

    ArgumentCaptor<org.sonar.api.measures.Measure> argumentCaptor = ArgumentCaptor.forClass(org.sonar.api.measures.Measure.class);
    when(sonarIndex.addMeasure(eq(project), argumentCaptor.capture())).thenReturn(null);

    sensorStorage.store(new DefaultMeasure()
      .onProject()
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    org.sonar.api.measures.Measure m = argumentCaptor.getValue();
    assertThat(m.getValue()).isEqualTo(10.0);
    assertThat(m.getMetric()).isEqualTo(CoreMetrics.NCLOC);
  }

  @Test
  public void shouldAddIssueOnFile() {
    InputFile file = new DefaultInputFile("foo", "src/Foo.php").setLines(4);

    ArgumentCaptor<org.sonar.api.issue.internal.DefaultIssue> argumentCaptor = ArgumentCaptor.forClass(org.sonar.api.issue.internal.DefaultIssue.class);

    sensorStorage.store(new DefaultIssue()
      .onFile(file)
      .forRule(RuleKey.of("foo", "bar"))
      .message("Foo")
      .atLine(3)
      .effortToFix(10.0));

    verify(moduleIssues).initAndAddIssue(argumentCaptor.capture());

    org.sonar.api.issue.internal.DefaultIssue issue = argumentCaptor.getValue();
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("foo", "bar"));
    assertThat(issue.message()).isEqualTo("Foo");
    assertThat(issue.line()).isEqualTo(3);
    assertThat(issue.severity()).isNull();
    assertThat(issue.effortToFix()).isEqualTo(10.0);
  }

  @Test
  public void shouldAddIssueOnDirectory() {
    InputDir dir = new DefaultInputDir("foo", "src");

    ArgumentCaptor<org.sonar.api.issue.internal.DefaultIssue> argumentCaptor = ArgumentCaptor.forClass(org.sonar.api.issue.internal.DefaultIssue.class);

    sensorStorage.store(new DefaultIssue()
      .onDir(dir)
      .forRule(RuleKey.of("foo", "bar"))
      .message("Foo")
      .effortToFix(10.0));

    verify(moduleIssues).initAndAddIssue(argumentCaptor.capture());

    org.sonar.api.issue.internal.DefaultIssue issue = argumentCaptor.getValue();
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("foo", "bar"));
    assertThat(issue.message()).isEqualTo("Foo");
    assertThat(issue.line()).isNull();
    assertThat(issue.severity()).isNull();
    assertThat(issue.effortToFix()).isEqualTo(10.0);
  }

  @Test
  public void shouldAddIssueOnProject() {
    ArgumentCaptor<org.sonar.api.issue.internal.DefaultIssue> argumentCaptor = ArgumentCaptor.forClass(org.sonar.api.issue.internal.DefaultIssue.class);

    sensorStorage.store(new DefaultIssue()
      .onProject()
      .forRule(RuleKey.of("foo", "bar"))
      .message("Foo")
      .overrideSeverity(Severity.BLOCKER)
      .effortToFix(10.0));

    verify(moduleIssues).initAndAddIssue(argumentCaptor.capture());

    org.sonar.api.issue.internal.DefaultIssue issue = argumentCaptor.getValue();
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("foo", "bar"));
    assertThat(issue.message()).isEqualTo("Foo");
    assertThat(issue.line()).isNull();
    assertThat(issue.severity()).isEqualTo("BLOCKER");
    assertThat(issue.effortToFix()).isEqualTo(10.0);
  }

  @Test
  public void shouldStoreDependencyInSameFolder() {

    Resource foo = File.create("src/Foo.java").setEffectiveKey("foo:src/Foo.java");
    Resource bar = File.create("src/Bar.java").setEffectiveKey("foo:src/Bar.java");
    resourceCache.add(foo, null);
    resourceCache.add(bar, null);

    sensorStorage.store(new DefaultDependency()
      .from(new DefaultInputFile("foo", "src/Foo.java").setType(Type.MAIN))
      .to(new DefaultInputFile("foo", "src/Bar.java").setType(Type.MAIN))
      .weight(3));

    ArgumentCaptor<Dependency> argumentCaptor = ArgumentCaptor.forClass(Dependency.class);

    verify(sonarIndex).addDependency(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getFrom()).isEqualTo(foo);
    assertThat(argumentCaptor.getValue().getTo()).isEqualTo(bar);
    assertThat(argumentCaptor.getValue().getWeight()).isEqualTo(3);
    assertThat(argumentCaptor.getValue().getUsage()).isEqualTo("USES");
  }

  @Test
  public void throw_if_attempt_to_save_same_dep_twice() {

    Resource foo = File.create("src/Foo.java").setEffectiveKey("foo:src/Foo.java");
    Resource bar = File.create("src/Bar.java").setEffectiveKey("foo:src/Bar.java");
    resourceCache.add(foo, null);
    resourceCache.add(bar, null);

    when(sonarIndex.getEdge(foo, bar)).thenReturn(new Dependency(foo, bar));

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Dependency between foo:src/Foo.java and foo:src/Bar.java was already saved.");

    sensorStorage.store(new DefaultDependency()
      .from(new DefaultInputFile("foo", "src/Foo.java").setType(Type.MAIN))
      .to(new DefaultInputFile("foo", "src/Bar.java").setType(Type.MAIN))
      .weight(3));
  }

  @Test
  public void shouldStoreDependencyInDifferentFolder() {

    Resource foo = File.create("src1/Foo.java").setEffectiveKey("foo:src1/Foo.java");
    Resource bar = File.create("src2/Bar.java").setEffectiveKey("foo:src2/Bar.java");
    resourceCache.add(foo, null);
    resourceCache.add(bar, null);

    sensorStorage.store(new DefaultDependency()
      .from(new DefaultInputFile("foo", "src1/Foo.java").setType(Type.MAIN))
      .to(new DefaultInputFile("foo", "src2/Bar.java").setType(Type.MAIN))
      .weight(3));

    ArgumentCaptor<Dependency> argumentCaptor = ArgumentCaptor.forClass(Dependency.class);

    verify(sonarIndex, times(2)).addDependency(argumentCaptor.capture());
    assertThat(argumentCaptor.getAllValues()).hasSize(2);
    Dependency value1 = argumentCaptor.getAllValues().get(0);
    assertThat(value1.getFrom()).isEqualTo(Directory.create("src1"));
    assertThat(value1.getTo()).isEqualTo(Directory.create("src2"));
    assertThat(value1.getWeight()).isEqualTo(1);
    assertThat(value1.getUsage()).isEqualTo("USES");

    Dependency value2 = argumentCaptor.getAllValues().get(1);
    assertThat(value2.getFrom()).isEqualTo(foo);
    assertThat(value2.getTo()).isEqualTo(bar);
    assertThat(value2.getWeight()).isEqualTo(3);
    assertThat(value2.getUsage()).isEqualTo("USES");
  }

  @Test
  public void shouldIncrementParentWeight() {

    Resource src1 = Directory.create("src1").setEffectiveKey("foo:src1");
    Resource src2 = Directory.create("src2").setEffectiveKey("foo:src2");
    Resource foo = File.create("src1/Foo.java").setEffectiveKey("foo:src1/Foo.java");
    Resource bar = File.create("src2/Bar.java").setEffectiveKey("foo:src2/Bar.java");
    resourceCache.add(src1, null);
    resourceCache.add(src2, null);
    resourceCache.add(foo, src1);
    resourceCache.add(bar, src2);
    Dependency parentDep = new Dependency(src1, src2).setWeight(4);
    when(sonarIndex.getEdge(src1, src2)).thenReturn(parentDep);

    sensorStorage.store(new DefaultDependency()
      .from(new DefaultInputFile("foo", "src1/Foo.java").setType(Type.MAIN))
      .to(new DefaultInputFile("foo", "src2/Bar.java").setType(Type.MAIN))
      .weight(3));

    ArgumentCaptor<Dependency> argumentCaptor = ArgumentCaptor.forClass(Dependency.class);

    verify(sonarIndex).addDependency(argumentCaptor.capture());

    assertThat(parentDep.getWeight()).isEqualTo(5);

    Dependency value = argumentCaptor.getValue();
    assertThat(value.getFrom()).isEqualTo(foo);
    assertThat(value.getTo()).isEqualTo(bar);
    assertThat(value.getWeight()).isEqualTo(3);
    assertThat(value.getUsage()).isEqualTo("USES");
  }
}

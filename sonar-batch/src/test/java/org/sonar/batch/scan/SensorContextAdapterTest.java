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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.dependency.internal.DefaultDependency;
import org.sonar.api.batch.sensor.issue.Issue.Severity;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.design.Dependency;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.duplication.BlockCache;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.ComponentDataCache;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SensorContextAdapterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ActiveRules activeRules;
  private DefaultFileSystem fs;
  private SensorContextAdapter adaptor;
  private SensorContext sensorContext;
  private Settings settings;
  private ResourcePerspectives resourcePerspectives;
  private Project project;
  private SonarIndex sonarIndex;

  @Before
  public void prepare() {
    activeRules = new ActiveRulesBuilder().build();
    fs = new DefaultFileSystem();
    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.findByKey(CoreMetrics.NCLOC_KEY)).thenReturn(CoreMetrics.NCLOC);
    when(metricFinder.findByKey(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY)).thenReturn(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION);
    sensorContext = mock(SensorContext.class);
    settings = new Settings();
    resourcePerspectives = mock(ResourcePerspectives.class);
    ComponentDataCache componentDataCache = mock(ComponentDataCache.class);
    BlockCache blockCache = mock(BlockCache.class);
    project = new Project("myProject");
    sonarIndex = mock(SonarIndex.class);
    adaptor = new SensorContextAdapter(sensorContext, metricFinder, project,
      resourcePerspectives, settings, fs, activeRules, componentDataCache, blockCache, mock(DuplicationCache.class), sonarIndex);
  }

  @Test
  public void shouldProvideComponents() {
    assertThat(adaptor.activeRules()).isEqualTo(activeRules);
    assertThat(adaptor.fileSystem()).isEqualTo(fs);
    assertThat(adaptor.settings()).isEqualTo(settings);

    assertThat(adaptor.newIssue()).isNotNull();
    assertThat(adaptor.newMeasure()).isNotNull();
  }

  @Test
  public void shouldFailIfUnknowMetric() {
    InputFile file = new DefaultInputFile("foo", "src/Foo.php");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unknow metric with key: lines");

    adaptor.store(new DefaultMeasure()
      .onFile(file)
      .forMetric(CoreMetrics.LINES)
      .withValue(10));
  }

  @Test
  public void shouldSaveFileMeasureToSensorContext() {
    InputFile file = new DefaultInputFile("foo", "src/Foo.php");

    ArgumentCaptor<org.sonar.api.measures.Measure> argumentCaptor = ArgumentCaptor.forClass(org.sonar.api.measures.Measure.class);
    when(sensorContext.saveMeasure(eq(file), argumentCaptor.capture())).thenReturn(null);

    adaptor.store(new DefaultMeasure()
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
    when(sensorContext.saveMeasure(eq(file), argumentCaptor.capture())).thenReturn(null);

    adaptor.store(new DefaultMeasure()
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
    when(sensorContext.saveMeasure(argumentCaptor.capture())).thenReturn(null);

    adaptor.store(new DefaultMeasure()
      .onProject()
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    org.sonar.api.measures.Measure m = argumentCaptor.getValue();
    assertThat(m.getValue()).isEqualTo(10.0);
    assertThat(m.getMetric()).isEqualTo(CoreMetrics.NCLOC);
  }

  @Test
  public void shouldAddIssueOnFile() {
    InputFile file = new DefaultInputFile("foo", "src/Foo.php");

    ArgumentCaptor<Issue> argumentCaptor = ArgumentCaptor.forClass(Issue.class);

    Issuable issuable = mock(Issuable.class);
    when(resourcePerspectives.as(Issuable.class, File.create("src/Foo.php"))).thenReturn(issuable);

    when(issuable.addIssue(argumentCaptor.capture())).thenReturn(true);

    adaptor.store(new DefaultIssue()
      .onFile(file)
      .ruleKey(RuleKey.of("foo", "bar"))
      .message("Foo")
      .atLine(3)
      .effortToFix(10.0));

    Issue issue = argumentCaptor.getValue();
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("foo", "bar"));
    assertThat(issue.message()).isEqualTo("Foo");
    assertThat(issue.line()).isEqualTo(3);
    assertThat(issue.severity()).isNull();
    assertThat(issue.effortToFix()).isEqualTo(10.0);
  }

  @Test
  public void shouldAddIssueOnDirectory() {
    InputDir dir = new DefaultInputDir("foo", "src");

    ArgumentCaptor<Issue> argumentCaptor = ArgumentCaptor.forClass(Issue.class);

    Issuable issuable = mock(Issuable.class);
    when(resourcePerspectives.as(Issuable.class, Directory.create("src"))).thenReturn(issuable);

    when(issuable.addIssue(argumentCaptor.capture())).thenReturn(true);

    adaptor.store(new DefaultIssue()
      .onDir(dir)
      .ruleKey(RuleKey.of("foo", "bar"))
      .message("Foo")
      .effortToFix(10.0));

    Issue issue = argumentCaptor.getValue();
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("foo", "bar"));
    assertThat(issue.message()).isEqualTo("Foo");
    assertThat(issue.line()).isNull();
    assertThat(issue.severity()).isNull();
    assertThat(issue.effortToFix()).isEqualTo(10.0);
  }

  @Test
  public void shouldAddIssueOnProject() {
    ArgumentCaptor<Issue> argumentCaptor = ArgumentCaptor.forClass(Issue.class);

    Issuable issuable = mock(Issuable.class);
    when(resourcePerspectives.as(Issuable.class, (Resource) project)).thenReturn(issuable);

    when(issuable.addIssue(argumentCaptor.capture())).thenReturn(true);

    adaptor.store(new DefaultIssue()
      .onProject()
      .ruleKey(RuleKey.of("foo", "bar"))
      .message("Foo")
      .overrideSeverity(Severity.BLOCKER)
      .effortToFix(10.0));

    Issue issue = argumentCaptor.getValue();
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("foo", "bar"));
    assertThat(issue.message()).isEqualTo("Foo");
    assertThat(issue.line()).isNull();
    assertThat(issue.severity()).isEqualTo("BLOCKER");
    assertThat(issue.effortToFix()).isEqualTo(10.0);
  }

  @Test
  public void shouldStoreDependencyInSameFolder() {

    File foo = File.create("src/Foo.java");
    File bar = File.create("src/Bar.java");
    when(sensorContext.getResource(foo)).thenReturn(foo);
    when(sensorContext.getResource(bar)).thenReturn(bar);

    adaptor.store(new DefaultDependency()
      .from(new DefaultInputFile("foo", "src/Foo.java").setType(Type.MAIN))
      .to(new DefaultInputFile("foo", "src/Bar.java").setType(Type.MAIN))
      .weight(3));

    ArgumentCaptor<Dependency> argumentCaptor = ArgumentCaptor.forClass(Dependency.class);

    verify(sensorContext).saveDependency(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getFrom()).isEqualTo(foo);
    assertThat(argumentCaptor.getValue().getTo()).isEqualTo(bar);
    assertThat(argumentCaptor.getValue().getWeight()).isEqualTo(3);
    assertThat(argumentCaptor.getValue().getUsage()).isEqualTo("USES");
  }

  @Test
  public void throw_if_attempt_to_save_same_dep_twice() {

    File foo = File.create("src/Foo.java");
    File bar = File.create("src/Bar.java");
    when(sensorContext.getResource(foo)).thenReturn(foo);
    when(sensorContext.getResource(bar)).thenReturn(bar);
    when(sonarIndex.getEdge(foo, bar)).thenReturn(new Dependency(foo, bar));

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Dependency between [moduleKey=foo, relative=src/Foo.java, abs=null] and [moduleKey=foo, relative=src/Bar.java, abs=null] was already saved.");

    adaptor.store(new DefaultDependency()
      .from(new DefaultInputFile("foo", "src/Foo.java").setType(Type.MAIN))
      .to(new DefaultInputFile("foo", "src/Bar.java").setType(Type.MAIN))
      .weight(3));
  }

  @Test
  public void shouldStoreDependencyInDifferentFolder() {

    File foo = File.create("src1/Foo.java");
    File bar = File.create("src2/Bar.java");
    when(sensorContext.getResource(foo)).thenReturn(foo);
    when(sensorContext.getResource(bar)).thenReturn(bar);

    adaptor.store(new DefaultDependency()
      .from(new DefaultInputFile("foo", "src1/Foo.java").setType(Type.MAIN))
      .to(new DefaultInputFile("foo", "src2/Bar.java").setType(Type.MAIN))
      .weight(3));

    ArgumentCaptor<Dependency> argumentCaptor = ArgumentCaptor.forClass(Dependency.class);

    verify(sensorContext, times(2)).saveDependency(argumentCaptor.capture());
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

    File foo = File.create("src1/Foo.java");
    File bar = File.create("src2/Bar.java");
    Directory src1 = Directory.create("src1");
    Directory src2 = Directory.create("src2");
    when(sensorContext.getResource(foo)).thenReturn(foo);
    when(sensorContext.getResource(bar)).thenReturn(bar);
    Dependency parentDep = new Dependency(src1, src2).setWeight(4);
    when(sonarIndex.getEdge(src1, src2)).thenReturn(parentDep);

    adaptor.store(new DefaultDependency()
      .from(new DefaultInputFile("foo", "src1/Foo.java").setType(Type.MAIN))
      .to(new DefaultInputFile("foo", "src2/Bar.java").setType(Type.MAIN))
      .weight(3));

    ArgumentCaptor<Dependency> argumentCaptor = ArgumentCaptor.forClass(Dependency.class);

    verify(sensorContext).saveDependency(argumentCaptor.capture());

    assertThat(parentDep.getWeight()).isEqualTo(5);

    Dependency value = argumentCaptor.getValue();
    assertThat(value.getFrom()).isEqualTo(foo);
    assertThat(value.getTo()).isEqualTo(bar);
    assertThat(value.getWeight()).isEqualTo(3);
    assertThat(value.getUsage()).isEqualTo("USES");
  }
}

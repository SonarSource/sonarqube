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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueBuilder;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasureBuilder;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SensorContextAdapterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ActiveRules activeRules;
  private DefaultFileSystem fs;
  private SensorContextAdaptor adaptor;
  private SensorContext sensorContext;
  private Settings settings;
  private ResourcePerspectives resourcePerspectives;

  @Before
  public void prepare() {
    activeRules = new ActiveRulesBuilder().build();
    fs = new DefaultFileSystem();
    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.findByKey(CoreMetrics.NCLOC_KEY)).thenReturn(CoreMetrics.NCLOC);
    sensorContext = mock(SensorContext.class);
    settings = new Settings();
    resourcePerspectives = mock(ResourcePerspectives.class);
    adaptor = new SensorContextAdaptor(sensorContext, metricFinder, new Project("myProject"),
      resourcePerspectives, settings, fs, activeRules);
  }

  @Test
  public void shouldProvideComponents() {
    assertThat(adaptor.activeRules()).isEqualTo(activeRules);
    assertThat(adaptor.fileSystem()).isEqualTo(fs);
    assertThat(adaptor.settings()).isEqualTo(settings);

    assertThat(adaptor.issueBuilder()).isNotNull();
    assertThat(adaptor.measureBuilder()).isNotNull();
  }

  @Test
  public void shouldRedirectProjectMeasuresToSensorContext() {
    Measure<Integer> measure = adaptor.getMeasure(CoreMetrics.NCLOC_KEY);
    assertThat(measure).isNull();

    when(sensorContext.getMeasure(CoreMetrics.NCLOC)).thenReturn(new org.sonar.api.measures.Measure<Integer>(CoreMetrics.NCLOC, 10.0));

    measure = adaptor.getMeasure(CoreMetrics.NCLOC);
    assertThat(measure.metric()).isEqualTo(CoreMetrics.NCLOC);
    assertThat(measure.inputFile()).isNull();
    assertThat(measure.value()).isEqualTo(10);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unknow metric with key: lines");
    adaptor.getMeasure(CoreMetrics.LINES);
  }

  @Test
  public void shouldRedirectFileMeasuresToSensorContext() {
    InputFile file = new DefaultInputFile("src/Foo.php");

    Measure<Integer> measure = adaptor.getMeasure(file, CoreMetrics.NCLOC_KEY);
    assertThat(measure).isNull();

    when(sensorContext.getMeasure(File.create("src/Foo.php"), CoreMetrics.NCLOC)).thenReturn(new org.sonar.api.measures.Measure<Integer>(CoreMetrics.NCLOC, 10.0));
    measure = adaptor.getMeasure(file, CoreMetrics.NCLOC);

    assertThat(measure.metric()).isEqualTo(CoreMetrics.NCLOC);
    assertThat(measure.inputFile()).isEqualTo(file);
    assertThat(measure.value()).isEqualTo(10);
  }

  @Test
  public void shouldAddMeasureToSensorContext() {
    InputFile file = new DefaultInputFile("src/Foo.php");

    ArgumentCaptor<org.sonar.api.measures.Measure> argumentCaptor = ArgumentCaptor.forClass(org.sonar.api.measures.Measure.class);
    when(sensorContext.saveMeasure(eq(file), argumentCaptor.capture())).thenReturn(null);

    adaptor.addMeasure(new DefaultMeasureBuilder()
      .onFile(file)
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10)
      .build());

    org.sonar.api.measures.Measure m = argumentCaptor.getValue();
    assertThat(m.getValue()).isEqualTo(10.0);
    assertThat(m.getMetric()).isEqualTo(CoreMetrics.NCLOC);
  }

  @Test
  public void shouldAddIssue() {
    InputFile file = new DefaultInputFile("src/Foo.php");

    ArgumentCaptor<Issue> argumentCaptor = ArgumentCaptor.forClass(Issue.class);

    Issuable issuable = mock(Issuable.class);
    when(resourcePerspectives.as(Issuable.class, File.create("src/Foo.php"))).thenReturn(issuable);

    when(issuable.addIssue(argumentCaptor.capture())).thenReturn(true);

    adaptor.addIssue(new DefaultIssueBuilder()
      .onFile(file)
      .ruleKey(RuleKey.of("foo", "bar"))
      .message("Foo")
      .atLine(3)
      .effortToFix(10.0)
      .build());

    Issue issue = argumentCaptor.getValue();
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("foo", "bar"));
    assertThat(issue.message()).isEqualTo("Foo");
    assertThat(issue.line()).isEqualTo(3);
    assertThat(issue.effortToFix()).isEqualTo(10.0);
  }
}

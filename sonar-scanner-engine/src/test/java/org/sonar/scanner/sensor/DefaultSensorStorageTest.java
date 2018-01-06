/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.sensor;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.metric.ScannerMetrics;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;
import org.sonar.scanner.issue.ModuleIssues;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.repository.ContextPropertiesCache;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.measure.MeasureCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DefaultSensorStorageTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DefaultSensorStorage underTest;
  private MapSettings settings;
  private ModuleIssues moduleIssues;
  private MeasureCache measureCache;
  private ScannerReportWriter reportWriter;
  private ContextPropertiesCache contextPropertiesCache = new ContextPropertiesCache();
  private BranchConfiguration branchConfiguration;

  @Before
  public void prepare() throws Exception {
    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.<Integer>findByKey(CoreMetrics.NCLOC_KEY)).thenReturn(CoreMetrics.NCLOC);
    when(metricFinder.<String>findByKey(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY)).thenReturn(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION);

    settings = new MapSettings();
    moduleIssues = mock(ModuleIssues.class);
    measureCache = mock(MeasureCache.class);

    ReportPublisher reportPublisher = mock(ReportPublisher.class);
    reportWriter = new ScannerReportWriter(temp.newFolder());
    when(reportPublisher.getWriter()).thenReturn(reportWriter);

    branchConfiguration = mock(BranchConfiguration.class);

    underTest = new DefaultSensorStorage(metricFinder,
      moduleIssues, settings.asConfig(), reportPublisher, measureCache,
      mock(SonarCpdBlockIndex.class), contextPropertiesCache, new ScannerMetrics(), branchConfiguration);
  }

  @Test
  public void shouldFailIfUnknownMetric() {
    InputFile file = new TestInputFileBuilder("foo", "src/Foo.php").build();

    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("Unknown metric: lines");

    underTest.store(new DefaultMeasure()
      .on(file)
      .forMetric(CoreMetrics.LINES)
      .withValue(10));
  }

  @Test
  public void should_save_issue() {
    InputFile file = new TestInputFileBuilder("foo", "src/Foo.php").build();

    DefaultIssue issue = new DefaultIssue().at(new DefaultIssueLocation().on(file));
    underTest.store(issue);

    ArgumentCaptor<Issue> argumentCaptor = ArgumentCaptor.forClass(Issue.class);
    verify(moduleIssues).initAndAddIssue(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).isEqualTo(issue);
  }

  @Test
  public void should_skip_issue_on_short_branch_when_file_status_is_SAME() {
    InputFile file = new TestInputFileBuilder("foo", "src/Foo.php").setStatus(InputFile.Status.SAME).build();
    when(branchConfiguration.isShortLivingBranch()).thenReturn(true);

    DefaultIssue issue = new DefaultIssue().at(new DefaultIssueLocation().on(file));
    underTest.store(issue);

    verifyZeroInteractions(moduleIssues);
  }

  @Test
  public void should_save_highlighting() {
    DefaultInputFile file = new TestInputFileBuilder("foo", "src/Foo.php")
      .setContents("// comment").build();

    DefaultHighlighting highlighting = new DefaultHighlighting(underTest).onFile(file).highlight(0, 1, TypeOfText.KEYWORD);
    underTest.store(highlighting);

    assertThat(reportWriter.hasComponentData(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, file.batchId())).isTrue();
  }

  @Test
  public void should_skip_highlighting_on_short_branch_when_file_status_is_SAME() {
    DefaultInputFile file = new TestInputFileBuilder("foo", "src/Foo.php")
      .setContents("// comment")
      .setStatus(InputFile.Status.SAME).build();
    when(branchConfiguration.isShortLivingBranch()).thenReturn(true);

    DefaultHighlighting highlighting = new DefaultHighlighting(underTest).onFile(file).highlight(0, 1, TypeOfText.KEYWORD);
    underTest.store(highlighting);

    assertThat(reportWriter.hasComponentData(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, file.batchId())).isFalse();
  }

  @Test
  public void should_save_file_measure() {
    InputFile file = new TestInputFileBuilder("foo", "src/Foo.php").build();

    ArgumentCaptor<DefaultMeasure> argumentCaptor = ArgumentCaptor.forClass(DefaultMeasure.class);
    when(measureCache.put(eq(file.key()), eq(CoreMetrics.NCLOC_KEY), argumentCaptor.capture())).thenReturn(null);
    underTest.store(new DefaultMeasure()
      .on(file)
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    DefaultMeasure m = argumentCaptor.getValue();
    assertThat(m.value()).isEqualTo(10);
    assertThat(m.metric()).isEqualTo(CoreMetrics.NCLOC);
  }

  @Test
  public void should_skip_file_measure_on_short_branch_when_file_status_is_SAME() {
    InputFile file = new TestInputFileBuilder("foo", "src/Foo.php").setStatus(InputFile.Status.SAME).build();
    when(branchConfiguration.isShortLivingBranch()).thenReturn(true);

    underTest.store(new DefaultMeasure()
      .on(file)
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    verifyZeroInteractions(measureCache);
  }

  @Test
  public void should_save_project_measure() throws IOException {
    String projectKey = "myProject";
    DefaultInputModule module = new DefaultInputModule(ProjectDefinition.create().setKey(projectKey).setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));

    ArgumentCaptor<DefaultMeasure> argumentCaptor = ArgumentCaptor.forClass(DefaultMeasure.class);
    when(measureCache.put(eq(module.key()), eq(CoreMetrics.NCLOC_KEY), argumentCaptor.capture())).thenReturn(null);

    underTest.store(new DefaultMeasure()
      .on(module)
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    DefaultMeasure m = argumentCaptor.getValue();
    assertThat(m.value()).isEqualTo(10);
    assertThat(m.metric()).isEqualTo(CoreMetrics.NCLOC);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void duplicateHighlighting() throws Exception {
    InputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.java")
      .setModuleBaseDir(temp.newFolder().toPath()).build();
    DefaultHighlighting h = new DefaultHighlighting(null)
      .onFile(inputFile);
    underTest.store(h);
    underTest.store(h);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void duplicateSymbolTable() throws Exception {
    InputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.java")
      .setModuleBaseDir(temp.newFolder().toPath()).build();
    DefaultSymbolTable st = new DefaultSymbolTable(null)
      .onFile(inputFile);
    underTest.store(st);
    underTest.store(st);
  }

  @Test
  public void shouldStoreContextProperty() {
    underTest.storeProperty("foo", "bar");
    assertThat(contextPropertiesCache.getAll()).containsOnly(entry("foo", "bar"));
  }

  @Test
  public void shouldValidateStrictlyPositiveLine() throws Exception {
    InputFile file = new TestInputFileBuilder("module", "testfile").setModuleBaseDir(temp.newFolder().toPath()).build();
    Map<Integer, Integer> map = ImmutableMap.of(0, 3);
    String data = KeyValueFormat.format(map);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("must be > 0");
    underTest.validateCoverageMeasure(data, file);
  }

  @Test
  public void shouldValidateMaxLine() throws Exception {
    InputFile file = new TestInputFileBuilder("module", "testfile").setModuleBaseDir(temp.newFolder().toPath()).build();
    Map<Integer, Integer> map = ImmutableMap.of(11, 3);
    String data = KeyValueFormat.format(map);

    thrown.expect(IllegalStateException.class);
    underTest.validateCoverageMeasure(data, file);
  }

  @Test
  public void mergeCoverageLineMetrics_should_be_sorted() {
    assertThat(DefaultSensorStorage.mergeCoverageLineMetric(CoreMetrics.COVERAGE_LINE_HITS_DATA, "1=1", "1=1")).isEqualTo("1=2");
    assertThat(DefaultSensorStorage.mergeCoverageLineMetric(CoreMetrics.COVERAGE_LINE_HITS_DATA, "1=1", "2=1")).isEqualTo("1=1;2=1");
    assertThat(DefaultSensorStorage.mergeCoverageLineMetric(CoreMetrics.COVERAGE_LINE_HITS_DATA, "2=1", "1=1")).isEqualTo("1=1;2=1");

    assertThat(DefaultSensorStorage.mergeCoverageLineMetric(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "1=1", "1=1")).isEqualTo("1=1");
    assertThat(DefaultSensorStorage.mergeCoverageLineMetric(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "1=1", "2=1")).isEqualTo("1=1;2=1");
    assertThat(DefaultSensorStorage.mergeCoverageLineMetric(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "2=1", "1=1")).isEqualTo("1=1;2=1");
  }

}

/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.code.internal.DefaultSignificantCode;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.internal.DefaultExternalIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.rule.internal.DefaultAdHocRule;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.core.metric.ScannerMetrics;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;
import org.sonar.scanner.issue.IssuePublisher;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.repository.ContextPropertiesCache;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DefaultSensorStorageTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultSensorStorage underTest;
  private MapSettings settings;
  private IssuePublisher moduleIssues;
  private ScannerReportWriter reportWriter;
  private ContextPropertiesCache contextPropertiesCache = new ContextPropertiesCache();
  private BranchConfiguration branchConfiguration;
  private DefaultInputProject project;
  private ScannerReportReader reportReader;
  private ReportPublisher reportPublisher;

  @Before
  public void prepare() throws Exception {
    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.<Integer>findByKey(CoreMetrics.NCLOC_KEY)).thenReturn(CoreMetrics.NCLOC);
    when(metricFinder.<String>findByKey(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY)).thenReturn(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION);
    when(metricFinder.<Integer>findByKey(CoreMetrics.LINES_TO_COVER_KEY)).thenReturn(CoreMetrics.LINES_TO_COVER);

    settings = new MapSettings();
    moduleIssues = mock(IssuePublisher.class);

    reportPublisher = mock(ReportPublisher.class);
    final File reportDir = temp.newFolder();
    FileStructure fileStructure = new FileStructure(reportDir);
    reportWriter = new ScannerReportWriter(fileStructure);
    reportReader = new ScannerReportReader(fileStructure);
    when(reportPublisher.getWriter()).thenReturn(reportWriter);
    when(reportPublisher.getReader()).thenReturn(reportReader);

    branchConfiguration = mock(BranchConfiguration.class);

    underTest = new DefaultSensorStorage(metricFinder,
      moduleIssues, settings.asConfig(), reportPublisher, mock(SonarCpdBlockIndex.class), contextPropertiesCache, new ScannerMetrics(), branchConfiguration);

    project = new DefaultInputProject(ProjectDefinition.create()
      .setKey("foo")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder()));
  }

  @Test
  public void should_merge_coverage() {
    DefaultInputFile file = new TestInputFileBuilder("foo", "src/Foo.php").setLines(5).build();

    DefaultCoverage coverage = new DefaultCoverage(underTest);
    coverage.onFile(file).lineHits(3, 1);

    DefaultCoverage coverage2 = new DefaultCoverage(underTest);
    coverage2.onFile(file).lineHits(1, 1);

    underTest.store(coverage);
    underTest.store(coverage2);

    List<ScannerReport.LineCoverage> lineCoverage = new ArrayList<>();
    reportReader.readComponentCoverage(file.scannerId()).forEachRemaining(lineCoverage::add);
    assertThat(lineCoverage).containsExactly(
      // should be sorted by line
      ScannerReport.LineCoverage.newBuilder().setLine(1).setHits(true).build(),
      ScannerReport.LineCoverage.newBuilder().setLine(3).setHits(true).build());

  }

  @Test
  public void shouldFailIfUnknownMetric() {
    InputFile file = new TestInputFileBuilder("foo", "src/Foo.php").build();

    assertThatThrownBy(() -> underTest.store(new DefaultMeasure()
      .on(file)
      .forMetric(CoreMetrics.LINES)
      .withValue(10)))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("Unknown metric: lines");
  }

  @Test
  public void shouldIgnoreMeasuresOnFolders() {
    underTest.store(new DefaultMeasure()
      .on(new DefaultInputDir("foo", "bar"))
      .forMetric(CoreMetrics.LINES)
      .withValue(10));

    verifyNoMoreInteractions(reportPublisher);
  }

  @Test
  public void shouldIgnoreMeasuresOnModules() throws IOException {
    ProjectDefinition module = ProjectDefinition.create().setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder());
    ProjectDefinition root = ProjectDefinition.create().addSubProject(module);

    underTest.store(new DefaultMeasure()
      .on(new DefaultInputModule(module))
      .forMetric(CoreMetrics.LINES)
      .withValue(10));

    verifyNoMoreInteractions(reportPublisher);
  }

  @Test
  public void should_save_issue() {
    InputFile file = new TestInputFileBuilder("foo", "src/Foo.php").build();

    DefaultIssue issue = new DefaultIssue(project).at(new DefaultIssueLocation().on(file));
    underTest.store(issue);

    ArgumentCaptor<Issue> argumentCaptor = ArgumentCaptor.forClass(Issue.class);
    verify(moduleIssues).initAndAddIssue(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).isEqualTo(issue);
  }

  @Test
  public void should_save_external_issue() {
    InputFile file = new TestInputFileBuilder("foo", "src/Foo.php").build();

    DefaultExternalIssue externalIssue = new DefaultExternalIssue(project).at(new DefaultIssueLocation().on(file));
    underTest.store(externalIssue);

    ArgumentCaptor<ExternalIssue> argumentCaptor = ArgumentCaptor.forClass(ExternalIssue.class);
    verify(moduleIssues).initAndAddExternalIssue(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).isEqualTo(externalIssue);
  }

  @Test
  public void should_skip_issue_on_pr_when_file_status_is_SAME() {
    InputFile file = new TestInputFileBuilder("foo", "src/Foo.php").setStatus(InputFile.Status.SAME).build();
    when(branchConfiguration.isPullRequest()).thenReturn(true);

    DefaultIssue issue = new DefaultIssue(project).at(new DefaultIssueLocation().on(file));
    underTest.store(issue);

    verifyNoInteractions(moduleIssues);
  }

  @Test
  public void has_issues_delegates_to_report_publisher() {
    DefaultInputFile file1 = new TestInputFileBuilder("foo", "src/Foo1.php").setStatus(InputFile.Status.SAME).build();
    DefaultInputFile file2 = new TestInputFileBuilder("foo", "src/Foo2.php").setStatus(InputFile.Status.SAME).build();

    reportWriter.writeComponentIssues(file1.scannerId(), List.of(ScannerReport.Issue.newBuilder().build()));
    assertThat(underTest.hasIssues(file1)).isTrue();
    assertThat(underTest.hasIssues(file2)).isFalse();
  }

  @Test
  public void should_save_highlighting() {
    DefaultInputFile file = new TestInputFileBuilder("foo", "src/Foo.php")
      .setContents("// comment").build();

    DefaultHighlighting highlighting = new DefaultHighlighting(underTest).onFile(file).highlight(1, 0, 1, 1, TypeOfText.KEYWORD);
    underTest.store(highlighting);

    assertThat(reportWriter.hasComponentData(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, file.scannerId())).isTrue();
  }

  @Test
  public void should_skip_highlighting_on_pr_when_file_status_is_SAME() {
    DefaultInputFile file = new TestInputFileBuilder("foo", "src/Foo.php")
      .setContents("// comment")
      .setStatus(InputFile.Status.SAME).build();
    when(branchConfiguration.isPullRequest()).thenReturn(true);

    DefaultHighlighting highlighting = new DefaultHighlighting(underTest).onFile(file).highlight(1, 0, 1, 1, TypeOfText.KEYWORD);
    underTest.store(highlighting);

    assertThat(reportWriter.hasComponentData(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, file.scannerId())).isFalse();
  }

  @Test
  public void should_save_file_measure() {
    DefaultInputFile file = new TestInputFileBuilder("foo", "src/Foo.php")
      .build();

    underTest.store(new DefaultMeasure()
      .on(file)
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    ScannerReport.Measure m = reportReader.readComponentMeasures(file.scannerId()).next();
    assertThat(m.getIntValue().getValue()).isEqualTo(10);
    assertThat(m.getMetricKey()).isEqualTo(CoreMetrics.NCLOC_KEY);
  }

  @Test
  public void should_not_skip_file_measures_on_pull_request_when_file_status_is_SAME() {
    DefaultInputFile file = new TestInputFileBuilder("foo", "src/Foo.php").setStatus(InputFile.Status.SAME).build();
    when(branchConfiguration.isPullRequest()).thenReturn(true);

    underTest.store(new DefaultMeasure()
      .on(file)
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    ScannerReport.Measure m = reportReader.readComponentMeasures(file.scannerId()).next();
    assertThat(m.getIntValue().getValue()).isEqualTo(10);
    assertThat(m.getMetricKey()).isEqualTo(CoreMetrics.NCLOC_KEY);
  }

  @Test
  public void should_skip_significant_code_on_pull_request_when_file_status_is_SAME() {
    DefaultInputFile file = new TestInputFileBuilder("foo", "src/Foo.php")
      .setStatus(InputFile.Status.SAME)
      .setContents("foo")
      .build();
    when(branchConfiguration.isPullRequest()).thenReturn(true);

    underTest.store(new DefaultSignificantCode()
      .onFile(file)
      .addRange(file.selectLine(1)));

    assertThat(reportWriter.hasComponentData(FileStructure.Domain.SGNIFICANT_CODE, file.scannerId())).isFalse();
  }

  @Test
  public void should_save_significant_code() {
    DefaultInputFile file = new TestInputFileBuilder("foo", "src/Foo.php")
      .setContents("foo")
      .build();
    underTest.store(new DefaultSignificantCode()
      .onFile(file)
      .addRange(file.selectLine(1)));

    assertThat(reportWriter.hasComponentData(FileStructure.Domain.SGNIFICANT_CODE, file.scannerId())).isTrue();
  }

  @Test
  public void should_save_project_measure() throws IOException {
    String projectKey = "myProject";
    DefaultInputModule module = new DefaultInputModule(ProjectDefinition.create().setKey(projectKey).setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));

    underTest.store(new DefaultMeasure()
      .on(module)
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    ScannerReport.Measure m = reportReader.readComponentMeasures(module.scannerId()).next();
    assertThat(m.getIntValue().getValue()).isEqualTo(10);
    assertThat(m.getMetricKey()).isEqualTo(CoreMetrics.NCLOC_KEY);
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
  public void duplicateSignificantCode() throws Exception {
    InputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.java")
      .setModuleBaseDir(temp.newFolder().toPath()).build();
    DefaultSignificantCode h = new DefaultSignificantCode(null)
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
  public void store_whenAdhocRuleIsSpecified_shouldWriteAdhocRuleToReport() {

    underTest.store(new DefaultAdHocRule().ruleId("ruleId").engineId("engineId")
      .name("name")
      .addDefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH)
      .addDefaultImpact(SoftwareQuality.RELIABILITY, Severity.MEDIUM)
      .cleanCodeAttribute(CleanCodeAttribute.CLEAR)
      .severity(org.sonar.api.batch.rule.Severity.MAJOR)
      .type(RuleType.CODE_SMELL)
      .description("description"));

    try (CloseableIterator<ScannerReport.AdHocRule> adhocRuleIt = reportReader.readAdHocRules()) {
      ScannerReport.AdHocRule adhocRule = adhocRuleIt.next();
      assertThat(adhocRule)
        .extracting(ScannerReport.AdHocRule::getRuleId, ScannerReport.AdHocRule::getName, ScannerReport.AdHocRule::getSeverity,
          ScannerReport.AdHocRule::getType, ScannerReport.AdHocRule::getDescription)
        .containsExactlyInAnyOrder("ruleId", "name", Constants.Severity.MAJOR, ScannerReport.IssueType.CODE_SMELL, "description");
      assertThat(adhocRule.getDefaultImpactsList()).hasSize(2).extracting(ScannerReport.Impact::getSoftwareQuality, ScannerReport.Impact::getSeverity)
        .containsExactlyInAnyOrder(
          Tuple.tuple(SoftwareQuality.MAINTAINABILITY.name(), Severity.HIGH.name()),
          Tuple.tuple(SoftwareQuality.RELIABILITY.name(), Severity.MEDIUM.name()));
      assertThat(adhocRule.getCleanCodeAttribute())
        .isEqualTo(CleanCodeAttribute.CLEAR.name());
    }
  }

  @Test
  public void store_whenAdhocRuleIsSpecifiedWithOptionalFieldEmpty_shouldWriteAdhocRuleWithDefaultImpactsToReport() {
    underTest.store(new DefaultAdHocRule().ruleId("ruleId").engineId("engineId")
      .name("name")
      .description("description"));
    try (CloseableIterator<ScannerReport.AdHocRule> adhocRuleIt = reportReader.readAdHocRules()) {
      ScannerReport.AdHocRule adhocRule = adhocRuleIt.next();
      assertThat(adhocRule).extracting(ScannerReport.AdHocRule::getSeverity, ScannerReport.AdHocRule::getType)
        .containsExactlyInAnyOrder(Constants.Severity.UNSET_SEVERITY, ScannerReport.IssueType.UNSET);
      assertThat(adhocRule.getDefaultImpactsList()).extracting(ScannerReport.Impact::getSoftwareQuality, ScannerReport.Impact::getSeverity)
        .containsExactlyInAnyOrder(Tuple.tuple(SoftwareQuality.MAINTAINABILITY.name(), Severity.MEDIUM.name()));
      assertThat(adhocRule.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.CONVENTIONAL.name());
    }
  }
}

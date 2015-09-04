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
package org.sonar.server.computation.source;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.ViewsComponent;
import org.sonar.server.computation.component.VisitorsCrawler;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.DAYS_SINCE_LAST_COMMIT_KEY;
import static org.sonar.api.measures.CoreMetrics.LAST_COMMIT_DATE_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.component.Component.Type.VIEW;
import static org.sonar.server.computation.component.ViewsComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

public class LastCommitVisitorTest {

  public static final int FILE_REF = 1;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.LAST_COMMIT_DATE)
    .add(CoreMetrics.DAYS_SINCE_LAST_COMMIT);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  System2 system2 = mock(System2.class);

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(1_800_000_000_000L);
  }

  @Test
  public void aggregate_date_of_last_commit_to_directories_and_project() {
    // simulate the output of visitFile()
    LastCommitVisitor visitor = new LastCommitVisitor(reportReader, metricRepository, measureRepository, system2) {
      @Override
      public void visitFile(Component file, Path<LastCommit> path) {
        path.parent().addDate(file.getReportAttributes().getRef() * 1_000_000_000L);
      }
    };

    // project with 1 module, 2 directories and 3 files
    ReportComponent project = ReportComponent.builder(PROJECT, 1)
      .addChildren(
        ReportComponent.builder(MODULE, 11)
          .addChildren(
            ReportComponent.builder(DIRECTORY, 111)
              .addChildren(
                createFileComponent(1111),
                createFileComponent(1112))
              .build(),
            ReportComponent.builder(DIRECTORY, 112)
              .addChildren(
                createFileComponent(1121))
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(project);

    VisitorsCrawler underTest = new VisitorsCrawler(Lists.<ComponentVisitor>newArrayList(visitor));
    underTest.visit(project);

    // directories
    assertDate(111, 1_112_000_000_000L);
    assertDaysSinceLastCommit(111, 7962);
    assertDate(112, 1_121_000_000_000L);
    assertDaysSinceLastCommit(112, 7858);

    // module = most recent commit date of directories
    assertDate(11, 1_121_000_000_000L);
    assertDaysSinceLastCommit(11, 7858);

    // project
    assertDate(1, 1_121_000_000_000L);
    assertDaysSinceLastCommit(1, 7858);
  }

  @Test
  public void aggregate_date_of_last_commit_to_views() {
    // view with 3 nested sub-views and 3 projects
    ViewsComponent view = ViewsComponent.builder(VIEW, 1)
      .addChildren(
        builder(SUBVIEW, 11)
          .addChildren(
            builder(SUBVIEW, 111)
              .addChildren(
                builder(PROJECT_VIEW, 1111).build(),
                builder(PROJECT_VIEW, 1112).build())
              .build(),
            builder(SUBVIEW, 112)
              .addChildren(
                builder(PROJECT_VIEW, 1121).build())
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(view);

    // the second project has the most recent commit date
    measureRepository.addRawMeasure(1111, CoreMetrics.LAST_COMMIT_DATE_KEY, newMeasureBuilder().create(1_500_000_000_000L));
    measureRepository.addRawMeasure(1112, CoreMetrics.LAST_COMMIT_DATE_KEY, newMeasureBuilder().create(1_700_000_000_000L));
    measureRepository.addRawMeasure(1121, CoreMetrics.LAST_COMMIT_DATE_KEY, newMeasureBuilder().create(1_600_000_000_000L));

    VisitorsCrawler underTest = new VisitorsCrawler(Lists.<ComponentVisitor>newArrayList(new LastCommitVisitor(reportReader, metricRepository, measureRepository, system2)));
    underTest.visit(view);

    // second level of sub-views
    assertDate(111, 1_700_000_000_000L);
    assertDaysSinceLastCommit(111, 1157);
    assertDate(112, 1_600_000_000_000L);
    assertDaysSinceLastCommit(112, 2314);

    // first level of sub-views
    assertDate(11, 1_700_000_000_000L);
    assertDaysSinceLastCommit(11, 1157);

    // view
    assertDate(1, 1_700_000_000_000L);
    assertDaysSinceLastCommit(1, 1157);
  }

  @Test
  public void compute_date_of_file_from_blame_info_of_report() throws Exception {
    VisitorsCrawler underTest = new VisitorsCrawler(Lists.<ComponentVisitor>newArrayList(new LastCommitVisitor(reportReader, metricRepository, measureRepository, system2)));

    BatchReport.Changesets changesets = BatchReport.Changesets.newBuilder()
      .setComponentRef(FILE_REF)
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("john")
        .setDate(1_500_000_000_000L)
        .setRevision("rev-1")
        .build())
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("tom")
        // this is the most recent change
        .setDate(1_600_000_000_000L)
        .setRevision("rev-2")
        .build())
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("john")
        .setDate(1_500_000_000_000L)
        .setRevision("rev-1")
        .build())
      .addChangesetIndexByLine(0)
      .build();
    reportReader.putChangesets(changesets);
    ReportComponent file = createFileComponent(FILE_REF);
    treeRootHolder.setRoot(file);

    underTest.visit(file);

    assertDate(FILE_REF, 1_600_000_000_000L);
    assertDaysSinceLastCommit(FILE_REF, 2314);
  }

  private void assertDate(int componentRef, long expectedDate) {
    Optional<Measure> measure = measureRepository.getAddedRawMeasure(componentRef, LAST_COMMIT_DATE_KEY);
    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getLongValue()).isEqualTo(expectedDate);
  }

  private void assertDaysSinceLastCommit(int componentRef, int numberOfDays) {
    Optional<Measure> measure = measureRepository.getAddedRawMeasure(componentRef, DAYS_SINCE_LAST_COMMIT_KEY);
    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getIntValue()).isEqualTo(numberOfDays);
  }

  /**
   * When the file was not changed since previous analysis, than the report may not contain
   * the SCM blame information. In this case the date of last commit is loaded
   * from the base measure of previous analysis, directly from database
   */
  @Test
  public void reuse_date_of_previous_analysis_if_blame_info_is_not_in_report() throws Exception {
    VisitorsCrawler underTest = new VisitorsCrawler(Lists.<ComponentVisitor>newArrayList(new LastCommitVisitor(reportReader, metricRepository, measureRepository, system2)));
    ReportComponent file = createFileComponent(FILE_REF);
    treeRootHolder.setRoot(file);
    measureRepository.addBaseMeasure(FILE_REF, LAST_COMMIT_DATE_KEY, newMeasureBuilder().create(1_500_000_000L));

    underTest.visit(file);

    assertDate(FILE_REF, 1_500_000_000L);
  }

  @Test
  public void date_is_not_computed_on_file_if_blame_is_not_in_report_nor_in_previous_analysis() throws Exception {
    VisitorsCrawler underTest = new VisitorsCrawler(Lists.<ComponentVisitor>newArrayList(new LastCommitVisitor(reportReader, metricRepository, measureRepository, system2)));
    ReportComponent file = createFileComponent(FILE_REF);
    treeRootHolder.setRoot(file);

    underTest.visit(file);

    Optional<Measure> measure = measureRepository.getAddedRawMeasure(FILE_REF, LAST_COMMIT_DATE_KEY);
    assertThat(measure.isPresent()).isFalse();
  }

  private ReportComponent createFileComponent(int fileRef) {
    return ReportComponent.builder(FILE, fileRef).setFileAttributes(new FileAttributes(false, "js")).build();
  }
}

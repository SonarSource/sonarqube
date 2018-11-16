/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.source;

import com.google.common.collect.Lists;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.LAST_COMMIT_DATE_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.ce.task.projectanalysis.component.ViewsComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class LastCommitVisitorTest {

  private static final int PROJECT_REF = 1;
  private static final int DIR_REF = 2;
  private static final int FILE_1_REF = 1_111;
  private static final int FILE_2_REF = 1_112;
  private static final int FILE_3_REF = 1_121;
  private static final int DIR_1_REF = 3;
  private static final int DIR_2_REF = 4;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.LAST_COMMIT_DATE);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Rule
  public ScmInfoRepositoryRule scmInfoRepository = new ScmInfoRepositoryRule();

  @Test
  public void aggregate_date_of_last_commit_to_directories_and_project() {
    final long FILE_1_DATE = 1_100_000_000_000L;
    // FILE_2 is the most recent file in DIR_1
    final long FILE_2_DATE = 1_200_000_000_000L;
    // FILE_3 is the most recent file in the project
    final long FILE_3_DATE = 1_300_000_000_000L;

    // simulate the output of visitFile()
    LastCommitVisitor visitor = new LastCommitVisitor(metricRepository, measureRepository, scmInfoRepository) {
      @Override
      public void visitFile(Component file, Path<LastCommit> path) {
        long fileDate;
        switch (file.getReportAttributes().getRef()) {
          case FILE_1_REF:
            fileDate = FILE_1_DATE;
            break;
          case FILE_2_REF:
            fileDate = FILE_2_DATE;
            break;
          case FILE_3_REF:
            fileDate = FILE_3_DATE;
            break;
          default:
            throw new IllegalArgumentException();
        }
        path.parent().addDate(fileDate);
      }
    };

    // project with 1 module, 2 directories and 3 files
    ReportComponent project = ReportComponent.builder(PROJECT, PROJECT_REF)
      .addChildren(
        ReportComponent.builder(DIRECTORY, DIR_REF)
          .addChildren(
            ReportComponent.builder(DIRECTORY, DIR_1_REF)
              .addChildren(
                createFileComponent(FILE_1_REF),
                createFileComponent(FILE_2_REF))
              .build(),
            ReportComponent.builder(DIRECTORY, DIR_2_REF)
              .addChildren(
                createFileComponent(FILE_3_REF))
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(project);

    VisitorsCrawler underTest = new VisitorsCrawler(Lists.newArrayList(visitor));
    underTest.visit(project);

    assertDate(DIR_1_REF, FILE_2_DATE);
    assertDate(DIR_2_REF, FILE_3_DATE);

    assertDate(DIR_REF, FILE_3_DATE);

    // project
    assertDate(PROJECT_REF, FILE_3_DATE);
  }

  @Test
  public void aggregate_date_of_last_commit_to_views() {
    final int VIEW_REF = 1;
    final int SUBVIEW_1_REF = 2;
    final int SUBVIEW_2_REF = 3;
    final int SUBVIEW_3_REF = 4;
    final int PROJECT_1_REF = 5;
    final int PROJECT_2_REF = 6;
    final int PROJECT_3_REF = 7;
    final long PROJECT_1_DATE = 1_500_000_000_000L;
    // the second project has the most recent commit date
    final long PROJECT_2_DATE = 1_700_000_000_000L;
    final long PROJECT_3_DATE = 1_600_000_000_000L;
    // view with 3 nested sub-views and 3 projects
    ViewsComponent view = ViewsComponent.builder(VIEW, VIEW_REF)
      .addChildren(
        builder(SUBVIEW, SUBVIEW_1_REF)
          .addChildren(
            builder(SUBVIEW, SUBVIEW_2_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECT_1_REF).build(),
                builder(PROJECT_VIEW, PROJECT_2_REF).build())
              .build(),
            builder(SUBVIEW, SUBVIEW_3_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECT_3_REF).build())
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(view);

    measureRepository.addRawMeasure(PROJECT_1_REF, LAST_COMMIT_DATE_KEY, newMeasureBuilder().create(PROJECT_1_DATE));
    measureRepository.addRawMeasure(PROJECT_2_REF, LAST_COMMIT_DATE_KEY, newMeasureBuilder().create(PROJECT_2_DATE));
    measureRepository.addRawMeasure(PROJECT_3_REF, LAST_COMMIT_DATE_KEY, newMeasureBuilder().create(PROJECT_3_DATE));

    VisitorsCrawler underTest = new VisitorsCrawler(Lists.newArrayList(new LastCommitVisitor(metricRepository, measureRepository, scmInfoRepository)));
    underTest.visit(view);

    // second level of sub-views
    assertDate(SUBVIEW_2_REF, PROJECT_2_DATE);
    assertDate(SUBVIEW_3_REF, PROJECT_3_DATE);

    // first level of sub-views
    assertDate(SUBVIEW_1_REF, PROJECT_2_DATE);

    // view
    assertDate(VIEW_REF, PROJECT_2_DATE);
  }

  @Test
  public void compute_date_of_file_from_scm_repo() {
    VisitorsCrawler underTest = new VisitorsCrawler(Lists.newArrayList(new LastCommitVisitor(metricRepository, measureRepository, scmInfoRepository)));

    scmInfoRepository.setScmInfo(FILE_1_REF,
      Changeset.newChangesetBuilder()
        .setAuthor("john")
        .setDate(1_500_000_000_000L)
        .setRevision("rev-1")
        .build(),
      Changeset.newChangesetBuilder()
        .setAuthor("tom")
        // this is the most recent change
        .setDate(1_600_000_000_000L)
        .setRevision("rev-2")
        .build(),
      Changeset.newChangesetBuilder()
        .setAuthor("john")
        .setDate(1_500_000_000_000L)
        .setRevision("rev-1")
        .build());

    ReportComponent file = createFileComponent(FILE_1_REF);
    treeRootHolder.setRoot(file);

    underTest.visit(file);

    assertDate(FILE_1_REF, 1_600_000_000_000L);
  }

  @Test
  public void date_is_not_computed_on_file_if_blame_is_not_in_scm_repo() {
    VisitorsCrawler underTest = new VisitorsCrawler(Lists.newArrayList(new LastCommitVisitor(metricRepository, measureRepository, scmInfoRepository)));
    ReportComponent file = createFileComponent(FILE_1_REF);
    treeRootHolder.setRoot(file);

    underTest.visit(file);

    Optional<Measure> measure = measureRepository.getAddedRawMeasure(FILE_1_REF, LAST_COMMIT_DATE_KEY);
    assertThat(measure.isPresent()).isFalse();
  }

  private void assertDate(int componentRef, long expectedDate) {
    Optional<Measure> measure = measureRepository.getAddedRawMeasure(componentRef, LAST_COMMIT_DATE_KEY);
    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getLongValue()).isEqualTo(expectedDate);
  }

  private ReportComponent createFileComponent(int fileRef) {
    return ReportComponent.builder(FILE, fileRef).setFileAttributes(new FileAttributes(false, "js", 1)).build();
  }
}

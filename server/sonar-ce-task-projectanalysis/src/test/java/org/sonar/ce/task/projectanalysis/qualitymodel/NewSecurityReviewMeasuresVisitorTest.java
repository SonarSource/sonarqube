/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.qualitymodel;

import java.util.Arrays;
import java.util.Date;
import javax.annotation.Nullable;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepositoryRule;
import org.sonar.ce.task.projectanalysis.issue.FillComponentIssuesVisitorRule;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.BranchType;
import org.sonar.server.measure.Rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REVIEW_RATING;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REVIEW_RATING_KEY;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureAssert.assertThat;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.B;
import static org.sonar.server.measure.Rating.C;
import static org.sonar.server.measure.Rating.D;
import static org.sonar.server.measure.Rating.E;

public class NewSecurityReviewMeasuresVisitorTest {

  private static final Offset<Double> VARIATION_COMPARISON_OFFSET = Offset.offset(0.01);

  private static final long LEAK_PERIOD_SNAPSHOT_IN_MILLISEC = 12323l;
  private static final Date DEFAULT_CREATION_DATE = new Date(1000l);
  private static final Date BEFORE_LEAK_PERIOD_DATE = new Date(LEAK_PERIOD_SNAPSHOT_IN_MILLISEC - 5000L);
  private static final Date AFTER_LEAK_PERIOD_DATE = new Date(LEAK_PERIOD_SNAPSHOT_IN_MILLISEC + 5000L);

  private static final String LANGUAGE_KEY_1 = "lKey1";

  private static final int PROJECT_REF = 1;
  private static final int ROOT_DIR_REF = 12;
  private static final int DIRECTORY_REF = 123;
  private static final int FILE_1_REF = 1231;
  private static final int FILE_2_REF = 1232;

  private static final Component ROOT_PROJECT = builder(Component.Type.PROJECT, PROJECT_REF).setKey("project")
    .addChildren(
      builder(DIRECTORY, ROOT_DIR_REF).setKey("dir")
        .addChildren(
          builder(DIRECTORY, DIRECTORY_REF).setKey("directory")
            .addChildren(
              builder(FILE, FILE_1_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_KEY_1, 1)).setKey("file1").build(),
              builder(FILE, FILE_2_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_KEY_1, 1)).setKey("file2").build())
            .build())
        .build())
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NEW_SECURITY_REVIEW_RATING)
    .add(NEW_SECURITY_HOTSPOTS_REVIEWED)
    .add(NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS)
    .add(NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public PeriodHolderRule periodsHolder = new PeriodHolderRule().setPeriod(new Period("mode", null, LEAK_PERIOD_SNAPSHOT_IN_MILLISEC));
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public ComponentIssuesRepositoryRule componentIssuesRepositoryRule = new ComponentIssuesRepositoryRule(treeRootHolder);
  @Rule
  public FillComponentIssuesVisitorRule fillComponentIssuesVisitorRule = new FillComponentIssuesVisitorRule(componentIssuesRepositoryRule, treeRootHolder);

  private VisitorsCrawler underTest = new VisitorsCrawler(Arrays.asList(fillComponentIssuesVisitorRule,
    new NewSecurityReviewMeasuresVisitor(componentIssuesRepositoryRule, measureRepository, periodsHolder, analysisMetadataHolder, metricRepository)));

  @Test
  public void compute_measures_when_100_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newIssue().setCreationDate(AFTER_LEAK_PERIOD_DATE));
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE));
    fillComponentIssuesVisitorRule.setIssues(ROOT_DIR_REF,
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE));

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, A, 100.0);
    verifyRatingAndReviewedMeasures(FILE_2_REF, A, 100.0);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, A, 100.0);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, A, 100.0);
    verifyRatingAndReviewedMeasures(PROJECT_REF, A, 100.0);
  }

  @Test
  public void compute_measures_when_more_than_80_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newIssue().setCreationDate(AFTER_LEAK_PERIOD_DATE));
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, A, 100.0);
    verifyRatingAndReviewedMeasures(FILE_2_REF, A, 80.0);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, A, 87.5);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, A, 87.5);
    verifyRatingAndReviewedMeasures(PROJECT_REF, A, 87.5);
  }

  @Test
  public void compute_measures_when_more_than_70_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newIssue().setCreationDate(AFTER_LEAK_PERIOD_DATE));
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, A, 100.0);
    verifyRatingAndReviewedMeasures(FILE_2_REF, B, 71.42);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, B, 75.0);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, B, 75.0);
    verifyRatingAndReviewedMeasures(PROJECT_REF, B, 75.0);
  }

  @Test
  public void compute_measures_when_more_than_50_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, C, 50.0);
    verifyRatingAndReviewedMeasures(FILE_2_REF, C, 60.0);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, C, 57.14);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, C, 57.14);
    verifyRatingAndReviewedMeasures(PROJECT_REF, C, 57.14);
  }

  @Test
  public void compute_measures_when_more_30_than_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, D, 33.33);
    verifyRatingAndReviewedMeasures(FILE_2_REF, D, 40.0);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, D, 37.5);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, D, 37.5);
    verifyRatingAndReviewedMeasures(PROJECT_REF, D, 37.5);
  }

  @Test
  public void compute_measures_when_less_than_30_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, D, 33.33);
    verifyRatingAndReviewedMeasures(FILE_2_REF, E, 0.0);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, E, 16.66);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, E, 16.66);
    verifyRatingAndReviewedMeasures(PROJECT_REF, E, 16.66);
  }

  @Test
  public void compute_A_rating_and_no_percent_when_no_new_hotspot_on_new_code() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(PROJECT_REF, A, null);
  }

  @Test
  public void compute_measures_on_pr() {
    periodsHolder.setPeriod(null);
    Branch b = mock(Branch.class);
    when(b.getType()).thenReturn(BranchType.PULL_REQUEST);
    analysisMetadataHolder.setBranch(b);
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Dates is not taken into account on PR
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(BEFORE_LEAK_PERIOD_DATE));

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, C, 50.0);
    verifyRatingAndReviewedMeasures(FILE_2_REF, C, 57.14);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, C, 55.55);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, C, 55.55);
    verifyRatingAndReviewedMeasures(PROJECT_REF, C, 55.55);
  }

  @Test
  public void compute_status_related_measures() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_TO_REVIEW, null).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyHotspotStatusMeasures(FILE_1_REF, null, null);
    verifyHotspotStatusMeasures(FILE_2_REF, null, null);
    verifyHotspotStatusMeasures(DIRECTORY_REF, null, null);
    verifyHotspotStatusMeasures(ROOT_DIR_REF, null, null);
    verifyHotspotStatusMeasures(PROJECT_REF, 4, 3);
  }

  @Test
  public void compute_0_status_related_measures_when_no_hotspot() {
    treeRootHolder.setRoot(ROOT_PROJECT);

    underTest.visit(ROOT_PROJECT);

    verifyHotspotStatusMeasures(PROJECT_REF, 0, 0);
  }

  @Test
  public void no_measure_if_there_is_no_period() {
    periodsHolder.setPeriod(null);
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED));

    underTest.visit(ROOT_PROJECT);

    assertThat(measureRepository.getAddedRawMeasures(PROJECT_REF).values()).isEmpty();
  }

  private void verifyRatingAndReviewedMeasures(int componentRef, Rating expectedReviewRating, @Nullable Double expectedHotspotsReviewed) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_REVIEW_RATING_KEY)).hasVariation(expectedReviewRating.getIndex());
    if (expectedHotspotsReviewed != null){
      assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_HOTSPOTS_REVIEWED_KEY)).hasVariation(expectedHotspotsReviewed,
        VARIATION_COMPARISON_OFFSET);
    } else {
      assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_HOTSPOTS_REVIEWED_KEY)).isAbsent();
    }
  }

  private void verifyHotspotStatusMeasures(int componentRef, @Nullable Integer hotspotsReviewed, @Nullable Integer hotspotsToReview) {
    if (hotspotsReviewed == null) {
      Assertions.assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY)).isEmpty();
    } else {
      assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY)).hasVariation(hotspotsReviewed);
    }
    if (hotspotsReviewed == null) {
      Assertions.assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY)).isEmpty();
    } else {
      assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY)).hasVariation(hotspotsToReview);
    }
  }

  private static DefaultIssue newHotspot(String status, @Nullable String resolution) {
    return new DefaultIssue()
      .setKey(Uuids.create())
      .setSeverity(MINOR)
      .setStatus(status)
      .setResolution(resolution)
      .setType(RuleType.SECURITY_HOTSPOT)
      .setCreationDate(DEFAULT_CREATION_DATE);
  }

  private static DefaultIssue newIssue() {
    return new DefaultIssue()
      .setKey(Uuids.create())
      .setSeverity(MAJOR)
      .setType(RuleType.BUG)
      .setCreationDate(DEFAULT_CREATION_DATE);
  }

}

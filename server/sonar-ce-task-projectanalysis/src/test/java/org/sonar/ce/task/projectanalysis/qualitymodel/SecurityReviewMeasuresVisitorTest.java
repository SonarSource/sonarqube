/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.core.rule.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepositoryRule;
import org.sonar.ce.task.projectanalysis.issue.FillComponentIssuesVisitorRule;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.Uuids;
import org.sonar.server.measure.Rating;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_STATUS;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_TO_REVIEW_STATUS;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING_KEY;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.core.issue.DefaultIssue.STATUS_REVIEWED;
import static org.sonar.core.issue.DefaultIssue.STATUS_TO_REVIEW;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.B;
import static org.sonar.server.measure.Rating.C;
import static org.sonar.server.measure.Rating.D;
import static org.sonar.server.measure.Rating.E;

class SecurityReviewMeasuresVisitorTest {

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
              builder(FILE, FILE_1_REF).setKey("file1").build(),
              builder(FILE, FILE_2_REF).setKey("file2").build())
            .build())
        .build())
    .build();

  @RegisterExtension
  private final TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @RegisterExtension
  private final MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(SECURITY_REVIEW_RATING)
    .add(SECURITY_HOTSPOTS_REVIEWED)
    .add(SECURITY_HOTSPOTS_REVIEWED_STATUS)
    .add(SECURITY_HOTSPOTS_TO_REVIEW_STATUS);
  private final ComponentIssuesRepositoryRule componentIssuesRepositoryRule = new ComponentIssuesRepositoryRule(treeRootHolder);
  @RegisterExtension
  private final FillComponentIssuesVisitorRule fillComponentIssuesVisitorRule =
    new FillComponentIssuesVisitorRule(componentIssuesRepositoryRule, treeRootHolder);
  @RegisterExtension
  private final MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private final VisitorsCrawler underTest = new VisitorsCrawler(asList(fillComponentIssuesVisitorRule,
    new SecurityReviewMeasuresVisitor(componentIssuesRepositoryRule, measureRepository, metricRepository)));

  @Test
  void compute_rating_and_reviewed_measures_when_100_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_SAFE),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, A, 100.0);
    verifyRatingAndReviewedMeasures(FILE_2_REF, A, 100.0);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, A, 100.0);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, A, 100.0);
    verifyRatingAndReviewedMeasures(PROJECT_REF, A, 100.0);
  }

  @Test
  void compute_rating_and_reviewed__measures_when_more_than_80_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, A, 100.0);
    verifyRatingAndReviewedMeasures(FILE_2_REF, A, 80.0);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, A, 87.5);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, A, 87.5);
    verifyRatingAndReviewedMeasures(PROJECT_REF, A, 87.5);
  }

  @Test
  void compute_rating_and_reviewed__measures_when_more_than_70_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, A, 100.0);
    verifyRatingAndReviewedMeasures(FILE_2_REF, B, 71.4);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, B, 75.0);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, B, 75.0);
    verifyRatingAndReviewedMeasures(PROJECT_REF, B, 75.0);
  }

  @Test
  void compute_rating_and_reviewed__measures_when_more_than_50_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, C, 50.0);
    verifyRatingAndReviewedMeasures(FILE_2_REF, C, 60.0);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, C, 57.1);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, C, 57.1);
    verifyRatingAndReviewedMeasures(PROJECT_REF, C, 57.1);
  }

  @Test
  void compute_rating_and_reviewed__measures_when_more_30_than_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, D, 33.3);
    verifyRatingAndReviewedMeasures(FILE_2_REF, D, 40.0);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, D, 37.5);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, D, 37.5);
    verifyRatingAndReviewedMeasures(PROJECT_REF, D, 37.5);
  }

  @Test
  void compute_rating_and_reviewed__measures_when_less_than_30_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_TO_REVIEW, null),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(FILE_1_REF, D, 33.3);
    verifyRatingAndReviewedMeasures(FILE_2_REF, E, 0.0);
    verifyRatingAndReviewedMeasures(DIRECTORY_REF, E, 16.7);
    verifyRatingAndReviewedMeasures(ROOT_DIR_REF, E, 16.7);
    verifyRatingAndReviewedMeasures(PROJECT_REF, E, 16.7);
  }

  @Test
  void compute_A_rating_and_no_reviewed_when_no_hotspot() {
    treeRootHolder.setRoot(ROOT_PROJECT);

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(PROJECT_REF, A, null);
  }

  @Test
  void compute_status_related_measures() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyHotspotStatusMeasures(FILE_1_REF, 1, 1);
    verifyHotspotStatusMeasures(FILE_2_REF, 3, 2);
    verifyHotspotStatusMeasures(DIRECTORY_REF, 4, 3);
    verifyHotspotStatusMeasures(ROOT_DIR_REF, 4, 3);
    verifyHotspotStatusMeasures(PROJECT_REF, 4, 3);
  }

  @Test
  void compute_0_status_related_measures_when_no_hotspot() {
    treeRootHolder.setRoot(ROOT_PROJECT);

    underTest.visit(ROOT_PROJECT);

    verifyHotspotStatusMeasures(PROJECT_REF, 0, 0);
  }

  private void verifyRatingAndReviewedMeasures(int componentRef, Rating expectedReviewRating, @Nullable Double expectedHotspotsReviewed) {
    verifySecurityReviewRating(componentRef, expectedReviewRating);
    if (expectedHotspotsReviewed != null) {
      verifySecurityHotspotsReviewed(componentRef, expectedHotspotsReviewed);
    } else {
      assertThat(measureRepository.getAddedRawMeasure(componentRef, SECURITY_HOTSPOTS_REVIEWED_KEY)).isEmpty();
    }
  }

  private void verifySecurityReviewRating(int componentRef, Rating rating) {
    Measure measure = measureRepository.getAddedRawMeasure(componentRef, SECURITY_REVIEW_RATING_KEY).get();
    assertThat(measure.getIntValue()).isEqualTo(rating.getIndex());
    assertThat(measure.getData()).isEqualTo(rating.name());
  }

  private void verifySecurityHotspotsReviewed(int componentRef, double percent) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, SECURITY_HOTSPOTS_REVIEWED_KEY).get().getDoubleValue()).isEqualTo(percent);
  }

  private void verifyHotspotStatusMeasures(int componentRef, @Nullable Integer hotspotsReviewed, @Nullable Integer hotspotsToReview) {
    if (hotspotsReviewed == null){
      assertThat(measureRepository.getAddedRawMeasure(componentRef, SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY)).isEmpty();
    } else {
      assertThat(measureRepository.getAddedRawMeasure(componentRef, SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY).get().getIntValue()).isEqualTo(hotspotsReviewed);
    }
    if (hotspotsReviewed == null){
      assertThat(measureRepository.getAddedRawMeasure(componentRef, SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY)).isEmpty();
    } else {
      assertThat(measureRepository.getAddedRawMeasure(componentRef, SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY).get().getIntValue()).isEqualTo(hotspotsToReview);
    }
  }

  private static DefaultIssue newHotspot(String status, @Nullable String resolution) {
    return new DefaultIssue()
      .setKey(Uuids.create())
      .setSeverity(MINOR)
      .setStatus(status)
      .setResolution(resolution)
      .setType(RuleType.SECURITY_HOTSPOT);
  }

  private static DefaultIssue newIssue() {
    return new DefaultIssue()
      .setKey(Uuids.create())
      .setSeverity(MAJOR)
      .setType(RuleType.BUG);
  }

}

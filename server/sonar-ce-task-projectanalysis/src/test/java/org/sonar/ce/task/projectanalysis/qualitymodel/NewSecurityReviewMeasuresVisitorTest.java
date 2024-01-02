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
package org.sonar.ce.task.projectanalysis.qualitymodel;

import java.util.Arrays;
import javax.annotation.Nullable;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepositoryRule;
import org.sonar.ce.task.projectanalysis.issue.FillComponentIssuesVisitorRule;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.issue.NewIssueClassifier;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.server.measure.Rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
  private static final Offset<Double> VALUE_COMPARISON_OFFSET = Offset.offset(0.01);
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
  public ComponentIssuesRepositoryRule componentIssuesRepositoryRule = new ComponentIssuesRepositoryRule(treeRootHolder);
  @Rule
  public FillComponentIssuesVisitorRule fillComponentIssuesVisitorRule = new FillComponentIssuesVisitorRule(componentIssuesRepositoryRule, treeRootHolder);
  private final NewIssueClassifier newIssueClassifier = mock(NewIssueClassifier.class);
  private final VisitorsCrawler underTest = new VisitorsCrawler(Arrays.asList(fillComponentIssuesVisitorRule,
    new NewSecurityReviewMeasuresVisitor(componentIssuesRepositoryRule, measureRepository, metricRepository, newIssueClassifier)));

  @Before
  public void setup() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
  }

  @Test
  public void compute_measures_when_100_percent_hotspots_reviewed() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      // Should not be taken into account
      oldHotspot(STATUS_TO_REVIEW, null),
      oldHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED));
    fillComponentIssuesVisitorRule.setIssues(ROOT_DIR_REF,
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED));

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
      // Should not be taken into account
      oldHotspot(STATUS_TO_REVIEW, null),
      oldHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
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
      // Should not be taken into account
      oldHotspot(STATUS_TO_REVIEW, null),
      oldHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
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
      // Should not be taken into account
      oldHotspot(STATUS_TO_REVIEW, null),
      oldHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
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
      // Should not be taken into account
      oldHotspot(STATUS_TO_REVIEW, null),
      oldHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
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
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      // Should not be taken into account
      newIssue());
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_TO_REVIEW, null),
      // Should not be taken into account
      oldHotspot(STATUS_TO_REVIEW, null),
      oldHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
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
      oldHotspot(STATUS_TO_REVIEW, null),
      oldHotspot(STATUS_REVIEWED, RESOLUTION_FIXED),
      newIssue());

    underTest.visit(ROOT_PROJECT);

    verifyRatingAndReviewedMeasures(PROJECT_REF, A, null);
  }

  @Test
  public void compute_status_related_measures() {
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
    when(newIssueClassifier.isEnabled()).thenReturn(false);
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newHotspot(STATUS_TO_REVIEW, null),
      newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED));

    underTest.visit(ROOT_PROJECT);

    assertThat(measureRepository.getAddedRawMeasures(PROJECT_REF).values()).isEmpty();
  }

  private void verifyRatingAndReviewedMeasures(int componentRef, Rating expectedReviewRating, @Nullable Double expectedHotspotsReviewed) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_REVIEW_RATING_KEY)).hasValue(expectedReviewRating.getIndex());
    if (expectedHotspotsReviewed != null) {
      assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_HOTSPOTS_REVIEWED_KEY)).hasValue(expectedHotspotsReviewed,
        VALUE_COMPARISON_OFFSET);
    } else {
      assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_HOTSPOTS_REVIEWED_KEY)).isAbsent();
    }
  }

  private void verifyHotspotStatusMeasures(int componentRef, @Nullable Integer hotspotsReviewed, @Nullable Integer hotspotsToReview) {
    if (hotspotsReviewed == null) {
      Assertions.assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY)).isEmpty();
    } else {
      assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY)).hasValue(hotspotsReviewed);
    }
    if (hotspotsReviewed == null) {
      Assertions.assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY)).isEmpty();
    } else {
      assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY)).hasValue(hotspotsToReview);
    }
  }

  private DefaultIssue newHotspot(String status, @Nullable String resolution) {
    return createHotspot(status, resolution, true);
  }

  private DefaultIssue oldHotspot(String status, @Nullable String resolution) {
    return createHotspot(status, resolution, false);
  }

  private DefaultIssue createHotspot(String status, @Nullable String resolution, boolean isNew) {
    DefaultIssue issue = new DefaultIssue()
      .setKey(UuidFactoryFast.getInstance().create())
      .setSeverity(MINOR)
      .setStatus(status)
      .setResolution(resolution)
      .setType(RuleType.SECURITY_HOTSPOT);
    when(newIssueClassifier.isNew(any(), eq(issue))).thenReturn(isNew);
    return issue;
  }

  private DefaultIssue newIssue() {
    DefaultIssue issue = new DefaultIssue()
      .setKey(Uuids.create())
      .setSeverity(MAJOR)
      .setType(RuleType.BUG);
    when(newIssueClassifier.isNew(any(), eq(issue))).thenReturn(false);
    return issue;

  }

}

/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.qualitymodel;

import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.ViewsComponent;
import org.sonar.server.computation.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.server.computation.task.projectanalysis.issue.ComponentIssuesRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ViewsComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.toEntries;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.A;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.B;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.C;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.D;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.E;

public class ReliabilityAndSecurityRatingMeasuresVisitorForViewsTest {

  private static final int ROOT_REF = 1;
  private static final int SUBVIEW_REF = 11;
  private static final int SUB_SUBVIEW_1_REF = 111;
  private static final int SUB_SUBVIEW_2_REF = 112;
  private static final int PROJECT_VIEW_1_REF = 1111;
  private static final int PROJECT_VIEW_2_REF = 1112;
  private static final int PROJECT_VIEW_3_REF = 1121;
  private static final int PROJECT_VIEW_4_REF = 12;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(builder(VIEW, ROOT_REF)
      .addChildren(
        builder(SUBVIEW, SUBVIEW_REF)
          .addChildren(
            builder(SUBVIEW, SUB_SUBVIEW_1_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build(),
                builder(PROJECT_VIEW, PROJECT_VIEW_2_REF).build())
              .build(),
            builder(SUBVIEW, SUB_SUBVIEW_2_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECT_VIEW_3_REF).build())
              .build())
          .build(),
        builder(PROJECT_VIEW, PROJECT_VIEW_4_REF).build())
      .build());

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(RELIABILITY_RATING)
    .add(SECURITY_RATING);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Rule
  public ComponentIssuesRepositoryRule componentIssuesRepositoryRule = new ComponentIssuesRepositoryRule(treeRootHolder);

  VisitorsCrawler underTest = new VisitorsCrawler(
    Arrays.asList(new ReliabilityAndSecurityRatingMeasuresVisitor(metricRepository, measureRepository, componentIssuesRepositoryRule)));

  @Test
  public void measures_created_for_view_are_all_zero_when_no_child() {
    ViewsComponent root = builder(VIEW, 1).build();
    treeRootHolder.setRoot(root);

    underTest.visit(root);

    assertThat(toEntries(measureRepository.getRawMeasures(root)))
      .containsOnly(
        entryOf(RELIABILITY_RATING_KEY, createMaintainabilityRatingMeasure(A)),
        entryOf(SECURITY_RATING_KEY, createMaintainabilityRatingMeasure(A)));
  }

  @Test
  public void compute_reliability_rating() throws Exception {
    treeRootHolder.setRoot(treeRootHolder.getRoot());

    addRawMeasure(RELIABILITY_RATING_KEY, PROJECT_VIEW_1_REF, B);
    addRawMeasure(RELIABILITY_RATING_KEY, PROJECT_VIEW_2_REF, C);
    addRawMeasure(RELIABILITY_RATING_KEY, PROJECT_VIEW_3_REF, D);
    addRawMeasure(RELIABILITY_RATING_KEY, PROJECT_VIEW_4_REF, E);

    underTest.visit(treeRootHolder.getRoot());

    verifyRawMeasure(SUB_SUBVIEW_1_REF, RELIABILITY_RATING_KEY, C);
    verifyRawMeasure(SUB_SUBVIEW_2_REF, RELIABILITY_RATING_KEY, D);
    verifyRawMeasure(SUBVIEW_REF, RELIABILITY_RATING_KEY, D);
    verifyRawMeasure(ROOT_REF, RELIABILITY_RATING_KEY, E);
  }

  @Test
  public void compute_security_rating() throws Exception {
    treeRootHolder.setRoot(treeRootHolder.getRoot());

    addRawMeasure(SECURITY_RATING_KEY, PROJECT_VIEW_2_REF, B);
    addRawMeasure(SECURITY_RATING_KEY, PROJECT_VIEW_1_REF, C);
    addRawMeasure(SECURITY_RATING_KEY, PROJECT_VIEW_3_REF, D);
    addRawMeasure(SECURITY_RATING_KEY, PROJECT_VIEW_4_REF, E);

    underTest.visit(treeRootHolder.getRoot());

    verifyRawMeasure(SUB_SUBVIEW_1_REF, SECURITY_RATING_KEY, C);
    verifyRawMeasure(SUB_SUBVIEW_2_REF, SECURITY_RATING_KEY, D);
    verifyRawMeasure(SUBVIEW_REF, SECURITY_RATING_KEY, D);
    verifyRawMeasure(ROOT_REF, SECURITY_RATING_KEY, E);
  }

  private void addRawMeasure(String metricKey, int componentRef, Rating value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value.getIndex(), value.name()));
  }

  private void verifyRawMeasure(int componentRef, String metricKey, Rating rating) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(entryOf(metricKey, newMeasureBuilder().create(rating.getIndex(), rating.name())));
  }

  private static Measure createMaintainabilityRatingMeasure(Rating rating) {
    return newMeasureBuilder().create(rating.getIndex(), rating.name());
  }

}

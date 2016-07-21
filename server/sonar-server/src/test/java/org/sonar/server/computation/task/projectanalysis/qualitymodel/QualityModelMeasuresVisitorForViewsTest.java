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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor;
import org.sonar.server.computation.task.projectanalysis.component.ViewsComponent;
import org.sonar.server.computation.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.server.computation.task.projectanalysis.issue.ComponentIssuesRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST;
import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A;
import static org.sonar.api.measures.CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_DEBT_RATIO;
import static org.sonar.api.measures.CoreMetrics.SQALE_DEBT_RATIO_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
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

public class QualityModelMeasuresVisitorForViewsTest {

  private static final double[] RATING_GRID = new double[] {34, 50, 362, 900, 36258};

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
    .add(NCLOC)
    .add(DEVELOPMENT_COST)
    .add(TECHNICAL_DEBT)
    .add(SQALE_DEBT_RATIO)
    .add(SQALE_RATING)
    .add(EFFORT_TO_REACH_MAINTAINABILITY_RATING_A)
    .add(RELIABILITY_RATING)
    .add(SECURITY_RATING);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Rule
  public ComponentIssuesRepositoryRule componentIssuesRepositoryRule = new ComponentIssuesRepositoryRule(treeRootHolder);

  private RatingSettings ratingSettings = mock(RatingSettings.class);

  private VisitorsCrawler underTest;

  @Before
  public void setUp() {
    when(ratingSettings.getRatingGrid()).thenReturn(new RatingGrid(RATING_GRID));
    underTest = new VisitorsCrawler(
      Arrays.<ComponentVisitor>asList(new QualityModelMeasuresVisitor(metricRepository, measureRepository, ratingSettings, componentIssuesRepositoryRule)));
  }

  @Test
  public void measures_created_for_view_are_all_zero_when_no_child() {
    ViewsComponent root = builder(VIEW, 1).build();
    treeRootHolder.setRoot(root);

    underTest.visit(root);

    assertThat(toEntries(measureRepository.getRawMeasures(root)))
      .containsOnly(
        entryOf(DEVELOPMENT_COST_KEY, newMeasureBuilder().create("0")),
        entryOf(SQALE_DEBT_RATIO_KEY, newMeasureBuilder().create(0d, 1)),
        entryOf(SQALE_RATING_KEY, createMaintainabilityRatingMeasure(A)),
        entryOf(EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY, newMeasureBuilder().create(0L)),
        entryOf(RELIABILITY_RATING_KEY, createMaintainabilityRatingMeasure(A)),
        entryOf(SECURITY_RATING_KEY, createMaintainabilityRatingMeasure(A)));
  }

  @Test
  public void verify_aggregation_of_developmentCost_and_value_of_measures_computed_from_that() {
    long debtRoot = 9999l;
    long debtSubView = 96325l;
    long debtSubSubView1 = 96325l;
    long debtSubSubView2 = 99633l;

    addRawMeasure(TECHNICAL_DEBT_KEY, ROOT_REF, debtRoot);
    addRawMeasure(TECHNICAL_DEBT_KEY, SUBVIEW_REF, debtSubView);
    addRawMeasure(TECHNICAL_DEBT_KEY, SUB_SUBVIEW_1_REF, debtSubSubView1);
    addRawMeasure(TECHNICAL_DEBT_KEY, PROJECT_VIEW_1_REF, 66000l);
    addRawMeasure(TECHNICAL_DEBT_KEY, PROJECT_VIEW_2_REF, 4200l);
    addRawMeasure(TECHNICAL_DEBT_KEY, SUB_SUBVIEW_2_REF, debtSubSubView2);
    addRawMeasure(TECHNICAL_DEBT_KEY, PROJECT_VIEW_3_REF, 25200l);
    addRawMeasure(TECHNICAL_DEBT_KEY, PROJECT_VIEW_4_REF, 33000l);

    addRawMeasure(DEVELOPMENT_COST_KEY, PROJECT_VIEW_1_REF, "40");
    addRawMeasure(DEVELOPMENT_COST_KEY, PROJECT_VIEW_2_REF, "70");
    addRawMeasure(DEVELOPMENT_COST_KEY, PROJECT_VIEW_3_REF, "50");
    addRawMeasure(DEVELOPMENT_COST_KEY, PROJECT_VIEW_4_REF, "100");

    underTest.visit(treeRootHolder.getRoot());

    assertNoNewRawMeasureOnProjectViews();
    assertNewRawMeasures(SUB_SUBVIEW_1_REF, debtSubSubView1, 110, D);
    assertNewRawMeasures(SUB_SUBVIEW_2_REF, debtSubSubView2, 50, E);
    assertNewRawMeasures(SUBVIEW_REF, debtSubView, 160, D);
    assertNewRawMeasures(ROOT_REF, debtRoot, 260, B);
  }

  @Test
  public void compute_reliability_and_security_rating() throws Exception {
    treeRootHolder.setRoot(treeRootHolder.getRoot());

    addRawMeasure(RELIABILITY_RATING_KEY, PROJECT_VIEW_1_REF, B);
    addRawMeasure(RELIABILITY_RATING_KEY, PROJECT_VIEW_2_REF, C);
    addRawMeasure(RELIABILITY_RATING_KEY, PROJECT_VIEW_3_REF, D);
    addRawMeasure(RELIABILITY_RATING_KEY, PROJECT_VIEW_4_REF, E);

    addRawMeasure(SECURITY_RATING_KEY, PROJECT_VIEW_2_REF, B);
    addRawMeasure(SECURITY_RATING_KEY, PROJECT_VIEW_1_REF, C);
    addRawMeasure(SECURITY_RATING_KEY, PROJECT_VIEW_3_REF, D);
    addRawMeasure(SECURITY_RATING_KEY, PROJECT_VIEW_4_REF, E);

    underTest.visit(treeRootHolder.getRoot());

    verifyRawMeasure(SUB_SUBVIEW_1_REF, RELIABILITY_RATING_KEY, C);
    verifyRawMeasure(SUB_SUBVIEW_2_REF, RELIABILITY_RATING_KEY, D);
    verifyRawMeasure(SUBVIEW_REF, RELIABILITY_RATING_KEY, D);
    verifyRawMeasure(ROOT_REF, RELIABILITY_RATING_KEY, E);

    verifyRawMeasure(SUB_SUBVIEW_1_REF, SECURITY_RATING_KEY, C);
    verifyRawMeasure(SUB_SUBVIEW_2_REF, SECURITY_RATING_KEY, D);
    verifyRawMeasure(SUBVIEW_REF, SECURITY_RATING_KEY, D);
    verifyRawMeasure(ROOT_REF, SECURITY_RATING_KEY, E);
  }

  @Test
  public void compute_effort_to_maintainability_rating_A_measure() throws Exception {
    treeRootHolder.setRoot(treeRootHolder.getRoot());

    long projectView1DevCosts = 40L;
    addRawMeasure(DEVELOPMENT_COST_KEY, PROJECT_VIEW_1_REF, Long.toString(projectView1DevCosts));
    long projectView2DevCosts = 70L;
    addRawMeasure(DEVELOPMENT_COST_KEY, PROJECT_VIEW_2_REF, Long.toString(projectView2DevCosts));
    long projectView3DevCosts = 50L;
    addRawMeasure(DEVELOPMENT_COST_KEY, PROJECT_VIEW_3_REF, Long.toString(projectView3DevCosts));
    long projectView4DevCosts = 100L;
    addRawMeasure(DEVELOPMENT_COST_KEY, PROJECT_VIEW_4_REF, Long.toString(projectView4DevCosts));

    long subSubView1Effort = 10000L;
    addRawMeasure(TECHNICAL_DEBT_KEY, SUB_SUBVIEW_1_REF, subSubView1Effort);

    long subSubView2Effort = 20000L;
    addRawMeasure(TECHNICAL_DEBT_KEY, SUB_SUBVIEW_2_REF, subSubView2Effort);

    long subViewEffort = 30000L;
    addRawMeasure(TECHNICAL_DEBT_KEY, SUBVIEW_REF, subViewEffort);

    long viewEffort = 35000L;
    addRawMeasure(TECHNICAL_DEBT_KEY, ROOT_REF, viewEffort);

    underTest.visit(treeRootHolder.getRoot());

    verifyRawMeasure(SUB_SUBVIEW_1_REF, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY,
      (long) (subSubView1Effort - RATING_GRID[0] * (projectView1DevCosts + projectView2DevCosts)));
    verifyRawMeasure(SUB_SUBVIEW_2_REF, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY,
      (long) (subSubView2Effort - RATING_GRID[0] * projectView3DevCosts));
    verifyRawMeasure(SUBVIEW_REF, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY,
      (long) (subViewEffort - RATING_GRID[0] * (projectView1DevCosts + projectView2DevCosts + projectView3DevCosts)));
    verifyRawMeasure(ROOT_REF, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY,
      (long) (viewEffort - RATING_GRID[0] * (projectView1DevCosts + projectView2DevCosts + projectView3DevCosts +
        projectView4DevCosts)));
  }

  private void assertNewRawMeasures(int componentRef, long debt, long devCost, Rating rating) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(
      entryOf(DEVELOPMENT_COST_KEY, newMeasureBuilder().create(String.valueOf(devCost))),
      entryOf(SQALE_DEBT_RATIO_KEY, newMeasureBuilder().create(debt / (double) devCost * 100.0, 1)),
      entryOf(SQALE_RATING_KEY, createMaintainabilityRatingMeasure(rating)));
  }

  private void assertNoNewRawMeasureOnProjectViews() {
    assertNoNewRawMeasure(PROJECT_VIEW_1_REF);
    assertNoNewRawMeasure(PROJECT_VIEW_2_REF);
    assertNoNewRawMeasure(PROJECT_VIEW_3_REF);
    assertNoNewRawMeasure(PROJECT_VIEW_4_REF);
  }

  private void addRawMeasure(String metricKey, int componentRef, String value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

  private void addRawMeasure(String metricKey, int componentRef, long value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

  private void addRawMeasure(String metricKey, int componentRef, Rating value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value.getIndex(), value.name()));
  }

  private void assertNoNewRawMeasure(int componentRef) {
    assertThat(measureRepository.getAddedRawMeasures(componentRef).isEmpty()).isTrue();
  }

  private void verifyRawMeasure(int componentRef, String metricKey, Rating rating) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(entryOf(metricKey, newMeasureBuilder().create(rating.getIndex(), rating.name())));
  }

  private void verifyRawMeasure(int componentRef, String metricKey, long value) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(entryOf(metricKey, newMeasureBuilder().create(value)));
  }

  private static Measure createMaintainabilityRatingMeasure(Rating rating) {
    return newMeasureBuilder().create(rating.getIndex(), rating.name());
  }

}

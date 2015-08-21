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

package org.sonar.server.computation.sqale;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.ViewsComponent;
import org.sonar.server.computation.component.VisitorsCrawler;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_DEBT_RATIO_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.server.computation.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.component.Component.Type.VIEW;
import static org.sonar.server.computation.component.ViewsComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;
import static org.sonar.server.computation.sqale.SqaleRatingGrid.SqaleRating.A;
import static org.sonar.server.computation.sqale.SqaleRatingGrid.SqaleRating.B;
import static org.sonar.server.computation.sqale.SqaleRatingGrid.SqaleRating.D;
import static org.sonar.server.computation.sqale.SqaleRatingGrid.SqaleRating.E;

public class ViewsSqaleMeasuresVisitorTest {

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
    .add(CoreMetrics.DEVELOPMENT_COST)
    .add(CoreMetrics.TECHNICAL_DEBT)
    .add(CoreMetrics.SQALE_DEBT_RATIO)
    .add(CoreMetrics.SQALE_RATING);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private SqaleRatingSettings sqaleRatingSettings = mock(SqaleRatingSettings.class);

  private VisitorsCrawler underTest = new VisitorsCrawler(Arrays.<ComponentVisitor>asList(new SqaleMeasuresVisitor(metricRepository, measureRepository, sqaleRatingSettings)));

  @Before
  public void setUp() {
    when(sqaleRatingSettings.getRatingGrid()).thenReturn(RATING_GRID);
  }

  @Test
  public void measures_created_for_project_are_all_zero_when_they_have_no_FILE_child() {
    ViewsComponent root = builder(VIEW, 1).build();
    treeRootHolder.setRoot(root);

    underTest.visit(root);

    assertThat(toEntries(measureRepository.getRawMeasures(root)))
      .containsOnly(
          entryOf(DEVELOPMENT_COST_KEY, newMeasureBuilder().create("0")),
          entryOf(SQALE_DEBT_RATIO_KEY, newMeasureBuilder().create(0d)),
          entryOf(SQALE_RATING_KEY, createSqaleRatingMeasure(A)));
  }

  private Measure createSqaleRatingMeasure(SqaleRatingGrid.SqaleRating sqaleRating) {
    return newMeasureBuilder().create(sqaleRating.getIndex(), sqaleRating.name());
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

  private void assertNewRawMeasures(int componentRef, long debt, long devCost, SqaleRatingGrid.SqaleRating sqaleRating) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).containsOnly(
        entryOf(DEVELOPMENT_COST_KEY, newMeasureBuilder().create(String.valueOf(devCost))),
        entryOf(SQALE_DEBT_RATIO_KEY, newMeasureBuilder().create(debt / (double) devCost * 100.0)),
        entryOf(SQALE_RATING_KEY, createSqaleRatingMeasure(sqaleRating)));
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

  private void verifyFileMeasures(int componentRef, long measureValue, long debt, long languageCost, SqaleRatingGrid.SqaleRating expectedRating) {
    long developmentCost = measureValue * languageCost;
    verifyComponentMeasures(componentRef, developmentCost, debt / developmentCost, expectedRating);
  }

  private void verifyComponentMeasures(int componentRef, long expectedDevCost, double expectedDebtRatio, SqaleRatingGrid.SqaleRating expectedRating) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).containsOnly(
      entryOf(DEVELOPMENT_COST_KEY, newMeasureBuilder().create(Long.toString(expectedDevCost))),
      entryOf(SQALE_DEBT_RATIO_KEY, newMeasureBuilder().create(expectedDebtRatio * 100.0)),
      entryOf(SQALE_RATING_KEY, createSqaleRatingMeasure(expectedRating)));
  }

  private void assertNoNewRawMeasure(int componentRef) {
    assertThat(measureRepository.getAddedRawMeasures(componentRef).isEmpty()).isTrue();
  }

}

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

package org.sonar.server.computation.task.projectanalysis.step;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolderRule;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_DUPLICATED;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_DUPLICATED_KEY;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ViewsComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations.newMeasureVariationsBuilder;

public class ViewsNewDuplicationMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int SUBVIEW_REF = 12;
  private static final int SUB_SUBVIEW_REF = 123;
  private static final int PROJECT_VIEW_1_REF = 1231;
  private static final int PROJECT_VIEW_2_REF = 1232;
  private static final int PROJECT_VIEW_3_REF = 13;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(builder(VIEW, ROOT_REF)
      .addChildren(
        builder(SUBVIEW, SUBVIEW_REF)
          .addChildren(
            builder(SUBVIEW, SUB_SUBVIEW_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build(),
                builder(PROJECT_VIEW, PROJECT_VIEW_2_REF).build())
              .build())
          .build(),
        builder(PROJECT_VIEW, PROJECT_VIEW_3_REF).build())
      .build());
  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NEW_LINES_DUPLICATED);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  ComputationStep underTest = new NewDuplicationMeasuresStep(treeRootHolder, periodsHolder, metricRepository, measureRepository);

  @Before
  public void setUp() {
    periodsHolder.setPeriods(
      new Period(2, "mode_p_1", null, parseDate("2009-12-25").getTime(), "u1"),
      new Period(5, "mode_p_5", null, parseDate("2011-02-18").getTime(), "u2"));
  }

  @Test
  public void aggregate_duplicated_lines() {
    addRawMeasure(PROJECT_VIEW_1_REF, NEW_LINES_DUPLICATED_KEY, 10);
    addRawMeasure(PROJECT_VIEW_2_REF, NEW_LINES_DUPLICATED_KEY, 40);
    addRawMeasure(PROJECT_VIEW_3_REF, NEW_LINES_DUPLICATED_KEY, 50);

    underTest.execute();

    assertNoNewRawMeasuresOnProjectViews();
    assertRawMeasureValue(SUB_SUBVIEW_REF, NEW_LINES_DUPLICATED_KEY, 50);
    assertRawMeasureValue(SUBVIEW_REF, NEW_LINES_DUPLICATED_KEY, 50);
    assertRawMeasureValue(ROOT_REF, NEW_LINES_DUPLICATED_KEY, 100);
  }

  @Test
  public void aggregate_zero_duplicated_line() {
    addRawMeasure(PROJECT_VIEW_1_REF, NEW_LINES_DUPLICATED_KEY, 0);
    addRawMeasure(PROJECT_VIEW_2_REF, NEW_LINES_DUPLICATED_KEY, 0);
    // no raw measure for PROJECT_VIEW_3_REF

    underTest.execute();

    assertNoNewRawMeasuresOnProjectViews();
    assertRawMeasureValue(SUB_SUBVIEW_REF, NEW_LINES_DUPLICATED_KEY, 0);
    assertRawMeasureValue(SUBVIEW_REF, NEW_LINES_DUPLICATED_KEY, 0);
    assertRawMeasureValue(ROOT_REF, NEW_LINES_DUPLICATED_KEY, 0);
  }

  @Test
  public void aggregate_zero_duplicated_line_when_no_data() {
    underTest.execute();

    assertNoNewRawMeasuresOnProjectViews();
    assertRawMeasureValue(SUB_SUBVIEW_REF, NEW_LINES_DUPLICATED_KEY, 0);
    assertRawMeasureValue(SUBVIEW_REF, NEW_LINES_DUPLICATED_KEY, 0);
    assertRawMeasureValue(ROOT_REF, NEW_LINES_DUPLICATED_KEY, 0);
  }

  private void addRawMeasure(int componentRef, String metricKey, double value) {
    measureRepository.addRawMeasure(componentRef, metricKey, createMeasure(value, value));
  }

  private static Measure createMeasure(@Nullable Double variationPeriod2, @Nullable Double variationPeriod5) {
    MeasureVariations.Builder variationBuilder = newMeasureVariationsBuilder();
    if (variationPeriod2 != null) {
      variationBuilder.setVariation(new Period(2, "", null, 1L, "U2"), variationPeriod2);
    }
    if (variationPeriod5 != null) {
      variationBuilder.setVariation(new Period(5, "", null, 1L, "U2"), variationPeriod5);
    }
    return newMeasureBuilder()
      .setVariations(variationBuilder.build())
      .createNoValue();
  }

  private void assertNoNewRawMeasuresOnProjectViews() {
    assertThat(measureRepository.getAddedRawMeasures(PROJECT_VIEW_1_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(PROJECT_VIEW_2_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(PROJECT_VIEW_3_REF)).isEmpty();
  }

  private void assertRawMeasureValue(int componentRef, String metricKey, int value) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey).get().getVariations().getVariation2()).isEqualTo(value);
  }

}

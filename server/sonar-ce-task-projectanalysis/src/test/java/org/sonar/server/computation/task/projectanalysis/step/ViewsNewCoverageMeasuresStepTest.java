/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.ViewsComponent;
import org.sonar.server.computation.task.projectanalysis.formula.coverage.LinesAndConditionsWithUncoveredMetricKeys;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ViewsComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class ViewsNewCoverageMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int SUBVIEW_REF = 11;
  private static final int SUB_SUBVIEW_1_REF = 111;
  private static final int PROJECT_VIEW_1_REF = 1111;
  private static final int SUB_SUBVIEW_2_REF = 112;
  private static final int PROJECT_VIEW_2_REF = 1121;
  private static final int PROJECT_VIEW_3_REF = 1122;
  private static final int PROJECT_VIEW_4_REF = 12;

  private static final ViewsComponent VIEWS_TREE = builder(VIEW, ROOT_REF)
    .addChildren(
      builder(SUBVIEW, SUBVIEW_REF)
        .addChildren(
          builder(SUBVIEW, SUB_SUBVIEW_1_REF)
            .addChildren(
              builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build())
            .build(),
          builder(SUBVIEW, SUB_SUBVIEW_2_REF)
            .addChildren(
              builder(PROJECT_VIEW, PROJECT_VIEW_2_REF).build(),
              builder(PROJECT_VIEW, PROJECT_VIEW_3_REF).build())
            .build(),
          builder(PROJECT_VIEW, PROJECT_VIEW_4_REF).build())
        .build())
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public PeriodHolderRule periodsHolder = new PeriodHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.COVERAGE_LINE_HITS_DATA)
    .add(CoreMetrics.CONDITIONS_BY_LINE)
    .add(CoreMetrics.COVERED_CONDITIONS_BY_LINE)
    .add(CoreMetrics.NEW_LINES_TO_COVER)
    .add(CoreMetrics.NEW_UNCOVERED_LINES)
    .add(CoreMetrics.NEW_CONDITIONS_TO_COVER)
    .add(CoreMetrics.NEW_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.NEW_COVERAGE)
    .add(CoreMetrics.NEW_BRANCH_COVERAGE)
    .add(CoreMetrics.NEW_LINE_COVERAGE);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private NewCoverageMeasuresStep underTest = new NewCoverageMeasuresStep(treeRootHolder, periodsHolder,
    measureRepository, metricRepository);

  @Before
  public void setUp() {
    periodsHolder.setPeriod(new Period("mode_p_1", null, parseDate("2009-12-25").getTime(), "U1"));
  }

  @Test
  public void verify_aggregation_of_measures_for_new_conditions() {
    String newLinesToCover = CoreMetrics.NEW_LINES_TO_COVER_KEY;
    String newUncoveredLines = CoreMetrics.NEW_UNCOVERED_LINES_KEY;
    String newConditionsToCover = CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY;
    String newUncoveredConditions = CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY;

    MetricKeys metricKeys = new MetricKeys(newLinesToCover, newUncoveredLines, newConditionsToCover, newUncoveredConditions);

    treeRootHolder.setRoot(VIEWS_TREE);
    // PROJECT_VIEW_1_REF has no measure
    measureRepository.addRawMeasure(PROJECT_VIEW_2_REF, newLinesToCover, createMeasure(1d));
    measureRepository.addRawMeasure(PROJECT_VIEW_2_REF, newUncoveredLines, createMeasure(10d));
    measureRepository.addRawMeasure(PROJECT_VIEW_2_REF, newConditionsToCover, createMeasure(4d));
    measureRepository.addRawMeasure(PROJECT_VIEW_2_REF, newUncoveredConditions, createMeasure(40d));
    measureRepository.addRawMeasure(PROJECT_VIEW_3_REF, newLinesToCover, createMeasure(11d));
    measureRepository.addRawMeasure(PROJECT_VIEW_3_REF, newUncoveredLines, createMeasure(20d));
    measureRepository.addRawMeasure(PROJECT_VIEW_3_REF, newConditionsToCover, createMeasure(14d));
    measureRepository.addRawMeasure(PROJECT_VIEW_3_REF, newUncoveredConditions, createMeasure(50d));
    measureRepository.addRawMeasure(PROJECT_VIEW_4_REF, newLinesToCover, createMeasure(21d));
    measureRepository.addRawMeasure(PROJECT_VIEW_4_REF, newUncoveredLines, createMeasure(30d));
    measureRepository.addRawMeasure(PROJECT_VIEW_4_REF, newConditionsToCover, createMeasure(24d));
    measureRepository.addRawMeasure(PROJECT_VIEW_4_REF, newUncoveredConditions, createMeasure(60d));

    underTest.execute();

    assertNoAddedRawMeasureOnProjectViews();
    assertNoAddedRawMeasures(SUB_SUBVIEW_1_REF);
    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_SUBVIEW_2_REF))).contains(
      entryOf(metricKeys.newLinesToCover, createMeasure(12d)),
      entryOf(metricKeys.newUncoveredLines, createMeasure(30d)),
      entryOf(metricKeys.newConditionsToCover, createMeasure(18d)),
      entryOf(metricKeys.newUncoveredConditions, createMeasure(90d)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(
      entryOf(metricKeys.newLinesToCover, createMeasure(33d)),
      entryOf(metricKeys.newUncoveredLines, createMeasure(60d)),
      entryOf(metricKeys.newConditionsToCover, createMeasure(42d)),
      entryOf(metricKeys.newUncoveredConditions, createMeasure(150d)));
  }

  @Test
  public void verify_aggregates_variations_for_new_code_line_and_branch_Coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.NEW_LINES_TO_COVER_KEY, CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.NEW_UNCOVERED_LINES_KEY, CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY);
    String codeCoverageKey = CoreMetrics.NEW_COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.NEW_LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.NEW_BRANCH_COVERAGE_KEY;

    verify_aggregates_variations(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  private void verify_aggregates_variations(LinesAndConditionsWithUncoveredMetricKeys metricKeys, String codeCoverageKey, String lineCoverageKey, String branchCoverageKey) {
    treeRootHolder.setRoot(VIEWS_TREE);
    measureRepository
      .addRawMeasure(PROJECT_VIEW_1_REF, metricKeys.getLines(), createMeasure(3000d))
      .addRawMeasure(PROJECT_VIEW_1_REF, metricKeys.getConditions(), createMeasure(300d))
      .addRawMeasure(PROJECT_VIEW_1_REF, metricKeys.getUncoveredLines(), createMeasure(30d))
      .addRawMeasure(PROJECT_VIEW_1_REF, metricKeys.getUncoveredConditions(), createMeasure(9d))
      // PROJECT_VIEW_2_REF
      .addRawMeasure(PROJECT_VIEW_2_REF, metricKeys.getLines(), createMeasure(2000d))
      .addRawMeasure(PROJECT_VIEW_2_REF, metricKeys.getConditions(), createMeasure(400d))
      .addRawMeasure(PROJECT_VIEW_2_REF, metricKeys.getUncoveredLines(), createMeasure(200d))
      .addRawMeasure(PROJECT_VIEW_2_REF, metricKeys.getUncoveredConditions(), createMeasure(16d))
      // PROJECT_VIEW_3_REF has no measure
      // PROJECT_VIEW_4_REF
      .addRawMeasure(PROJECT_VIEW_4_REF, metricKeys.getLines(), createMeasure(1000d))
      .addRawMeasure(PROJECT_VIEW_4_REF, metricKeys.getConditions(), createMeasure(300d))
      .addRawMeasure(PROJECT_VIEW_4_REF, metricKeys.getUncoveredLines(), createMeasure(100d))
      .addRawMeasure(PROJECT_VIEW_4_REF, metricKeys.getUncoveredConditions(), createMeasure(6d));

    underTest.execute();

    assertNoAddedRawMeasureOnProjectViews();

    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_SUBVIEW_1_REF))).contains(
      entryOf(codeCoverageKey, createMeasure(98.8d)),
      entryOf(lineCoverageKey, createMeasure(99d)),
      entryOf(branchCoverageKey, createMeasure(97d)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_SUBVIEW_2_REF))).contains(
      entryOf(codeCoverageKey, createMeasure(91d)),
      entryOf(lineCoverageKey, createMeasure(90d)),
      entryOf(branchCoverageKey, createMeasure(96d)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUBVIEW_REF))).contains(
      entryOf(codeCoverageKey, createMeasure(94.8d)),
      entryOf(lineCoverageKey, createMeasure(94.5d)),
      entryOf(branchCoverageKey, createMeasure(96.9d)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(
      entryOf(codeCoverageKey, createMeasure(94.8d)),
      entryOf(lineCoverageKey, createMeasure(94.5d)),
      entryOf(branchCoverageKey, createMeasure(96.9d)));
  }

  private static final class MetricKeys {
    private final String newLinesToCover;
    private final String newUncoveredLines;
    private final String newConditionsToCover;
    private final String newUncoveredConditions;

    public MetricKeys(String newLinesToCover, String newUncoveredLines, String newConditionsToCover, String newUncoveredConditions) {
      this.newLinesToCover = newLinesToCover;
      this.newUncoveredLines = newUncoveredLines;
      this.newConditionsToCover = newConditionsToCover;
      this.newUncoveredConditions = newUncoveredConditions;
    }
  }

  private static Measure createMeasure(Double expectedVariation) {
    return newMeasureBuilder()
      .setVariation(expectedVariation)
      .createNoValue();
  }

  private void assertNoAddedRawMeasureOnProjectViews() {
    assertNoAddedRawMeasures(PROJECT_VIEW_1_REF);
    assertNoAddedRawMeasures(PROJECT_VIEW_2_REF);
    assertNoAddedRawMeasures(PROJECT_VIEW_3_REF);
  }

  private void assertNoAddedRawMeasures(int componentRef) {
    assertThat(measureRepository.getAddedRawMeasures(componentRef)).isEmpty();
  }

}

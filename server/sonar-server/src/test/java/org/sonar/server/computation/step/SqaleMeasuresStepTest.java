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
package org.sonar.server.computation.step;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepoEntry;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.sqale.SqaleRatingGrid;
import org.sonar.server.computation.sqale.SqaleRatingSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_DEBT_RATIO_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;
import static org.sonar.server.computation.sqale.SqaleRatingGrid.SqaleRating.A;
import static org.sonar.server.computation.sqale.SqaleRatingGrid.SqaleRating.C;

public class SqaleMeasuresStepTest {

  private static final String METRIC_KEY_1 = "mKey1";
  private static final String METRIC_KEY_2 = "mKey2";
  private static final Metric METRIC_1 = new MetricImpl(1, METRIC_KEY_1, "metric1", Metric.MetricType.FLOAT);
  private static final Metric METRIC_2 = new MetricImpl(2, METRIC_KEY_2, "metric2", Metric.MetricType.WORK_DUR);
  private static final String LANGUAGE_KEY_1 = "lKey1";
  private static final String LANGUAGE_KEY_2 = "lKey2";
  private static final double[] RATING_GRID = new double[] {34, 50, 362, 900, 36258};
  private static final long DEV_COST_LANGUAGE_1 = 33;
  private static final long DEV_COST_LANGUAGE_2 = 42;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(METRIC_1).add(METRIC_2);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  private SqaleRatingSettings sqaleRatingSettings = mock(SqaleRatingSettings.class);

  private SqaleMeasuresStep underTest = new SqaleMeasuresStep(treeRootHolder, metricRepository, measureRepository, sqaleRatingSettings);

  @Before
  public void setUp() throws Exception {
    // assumes SQALE rating configuration is consistent
    when(sqaleRatingSettings.getRatingGrid()).thenReturn(RATING_GRID);
    when(sqaleRatingSettings.getSizeMetricKey(LANGUAGE_KEY_1)).thenReturn(METRIC_KEY_1);
    when(sqaleRatingSettings.getSizeMetricKey(LANGUAGE_KEY_2)).thenReturn(METRIC_KEY_2);
    when(sqaleRatingSettings.getDevCost(LANGUAGE_KEY_1)).thenReturn(DEV_COST_LANGUAGE_1);
    when(sqaleRatingSettings.getDevCost(LANGUAGE_KEY_2)).thenReturn(DEV_COST_LANGUAGE_2);

    // this measures are always retrieved by the step
    metricRepository.add(CoreMetrics.DEVELOPMENT_COST);
    metricRepository.add(CoreMetrics.TECHNICAL_DEBT);
    metricRepository.add(CoreMetrics.SQALE_DEBT_RATIO);
    metricRepository.add(CoreMetrics.SQALE_RATING);
  }

  @Test
  public void measures_created_for_project_are_all_zero_when_they_have_no_FILE_child() {
    DumbComponent root = DumbComponent.builder(PROJECT, 1).build();
    treeRootHolder.setRoot(root);

    underTest.execute();

    assertThat(toEntries(measureRepository.getRawMeasures(root))).containsOnly(
      MeasureRepoEntry.entryOf(DEVELOPMENT_COST_KEY, newMeasureBuilder().create("0")),
      MeasureRepoEntry.entryOf(SQALE_DEBT_RATIO_KEY, newMeasureBuilder().create(0d)),
      MeasureRepoEntry.entryOf(SQALE_RATING_KEY, createSqaleRatingMeasure(A))
      );
  }

  private Measure createSqaleRatingMeasure(SqaleRatingGrid.SqaleRating sqaleRating) {
    return newMeasureBuilder().create(sqaleRating.getIndex(), sqaleRating.name());
  }

  @Test
  public void verify_computation_of_measures_for_file_depending_upon_language_1() {
    verify_computation_of_measure_for_file(33000l, DEV_COST_LANGUAGE_1, METRIC_KEY_1, LANGUAGE_KEY_1, C);
  }

  @Test
  public void verify_computation_of_measures_for_file_depending_upon_language_2() {
    verify_computation_of_measure_for_file(4200l, DEV_COST_LANGUAGE_2, METRIC_KEY_2, LANGUAGE_KEY_2, A);
  }

  /**
   * Verify the computation of measures values depending upon which language is associated to the file by
   * processing a tree of a single Component of type FILE.
   */
  private void verify_computation_of_measure_for_file(long debt, long languageCost, String metricKey, String languageKey,
    SqaleRatingGrid.SqaleRating expectedRating) {
    long measureValue = 10;

    DumbComponent fileComponent = createFileComponent(languageKey, 1);
    treeRootHolder.setRoot(fileComponent);
    measureRepository.addRawMeasure(fileComponent.getRef(), metricKey, newMeasureBuilder().create(measureValue));
    measureRepository.addRawMeasure(fileComponent.getRef(), TECHNICAL_DEBT_KEY, newMeasureBuilder().create(debt));

    underTest.execute();

    verifyFileMeasures(fileComponent.getRef(), measureValue, debt, languageCost, expectedRating);
  }

  @Test
  public void verify_aggregation_of_developmentCost_and_value_of_measures_computed_from_that() {
    DumbComponent root = DumbComponent.builder(PROJECT, 1)
      .addChildren(
        DumbComponent.builder(MODULE, 11)
          .addChildren(
            DumbComponent.builder(DIRECTORY, 111)
              .addChildren(
                createFileComponent(LANGUAGE_KEY_1, 1111),
                createFileComponent(LANGUAGE_KEY_2, 1112)
              ).build(),
            DumbComponent.builder(DIRECTORY, 112)
              .addChildren(
                createFileComponent(LANGUAGE_KEY_2, 1121)
              ).build()
          ).build(),
        DumbComponent.builder(MODULE, 12)
          .addChildren(
            DumbComponent.builder(DIRECTORY, 121)
              .addChildren(
                createFileComponent(LANGUAGE_KEY_1, 1211)
              ).build(),
            DumbComponent.builder(DIRECTORY, 122).build()
          ).build(),
        DumbComponent.builder(MODULE, 13).build()
      ).build();

    treeRootHolder.setRoot(root);

    long measureValue1111 = 10;
    long debt1111 = 66000l;
    measureRepository.addRawMeasure(1111, METRIC_KEY_1, newMeasureBuilder().create(measureValue1111));
    measureRepository.addRawMeasure(1111, TECHNICAL_DEBT_KEY, newMeasureBuilder().create(debt1111));

    long measureValue1112 = 10;
    long debt1112 = 4200l;
    measureRepository.addRawMeasure(1112, METRIC_KEY_2, newMeasureBuilder().create(measureValue1112));
    measureRepository.addRawMeasure(1112, TECHNICAL_DEBT_KEY, newMeasureBuilder().create(debt1112));

    long debt111 = 96325l;
    measureRepository.addRawMeasure(111, TECHNICAL_DEBT_KEY, newMeasureBuilder().create(debt111));

    long measureValue1121 = 30;
    long debt1121 = 25200l;
    measureRepository.addRawMeasure(1121, METRIC_KEY_2, newMeasureBuilder().create(measureValue1121));
    measureRepository.addRawMeasure(1121, TECHNICAL_DEBT_KEY, newMeasureBuilder().create(debt1121));

    long debt112 = 99633l;
    measureRepository.addRawMeasure(112, TECHNICAL_DEBT_KEY, newMeasureBuilder().create(debt112));

    long measureValue1211 = 20;
    long debt1211 = 33000l;
    measureRepository.addRawMeasure(1211, METRIC_KEY_1, newMeasureBuilder().create(measureValue1211));
    measureRepository.addRawMeasure(1211, TECHNICAL_DEBT_KEY, newMeasureBuilder().create(debt1211));

    long debt121 = 7524l;
    measureRepository.addRawMeasure(121, TECHNICAL_DEBT_KEY, newMeasureBuilder().create(debt121));

    long debt1 = 9999l;
    measureRepository.addRawMeasure(1, TECHNICAL_DEBT_KEY, newMeasureBuilder().create(debt1));

    underTest.execute();

    // verify measures on files
    verifyFileMeasures(1111, measureValue1111, debt1111, DEV_COST_LANGUAGE_1, C);
    verifyFileMeasures(1112, measureValue1112, debt1112, DEV_COST_LANGUAGE_2, A);
    verifyFileMeasures(1121, measureValue1121, debt1121, DEV_COST_LANGUAGE_2, A);
    verifyFileMeasures(1211, measureValue1211, debt1211, DEV_COST_LANGUAGE_1, C);
    // directory has no children => no file => 0 everywhere and A rating
    verifyComponentMeasures(122, 0, 0, A);
    // directory has children => dev cost is aggregated
    long devCost111 = measureValue1111 * DEV_COST_LANGUAGE_1 + measureValue1112 * DEV_COST_LANGUAGE_2;
    verifyComponentMeasures(111, devCost111, debt111 / (double) devCost111, C);
    long devCost112 = measureValue1121 * DEV_COST_LANGUAGE_2;
    verifyComponentMeasures(112, devCost112, debt112 / (double) devCost112, C);
    long devCost121 = measureValue1211 * DEV_COST_LANGUAGE_1;
    verifyComponentMeasures(121, devCost121, debt121 / (double) devCost121, A);
    // just for fun, we didn't define any debt on module => they must all have rating A
    long devCost11 = devCost111 + devCost112;
    verifyComponentMeasures(11, devCost11, 0, A);
    long devCost12 = devCost121;
    verifyComponentMeasures(12, devCost12, 0, A);
    long devCost13 = 0;
    verifyComponentMeasures(13, devCost13, 0, A);
    // project has aggregated dev cost of all files
    long devCost1 = devCost11 + devCost12 + devCost13;
    verifyComponentMeasures(1, devCost1, debt1 / (double) devCost1, A);
  }

  private DumbComponent createFileComponent(String languageKey1, int fileRef) {
    return DumbComponent.builder(FILE, fileRef).setFileAttributes(new FileAttributes(false, languageKey1)).build();
  }

  private void verifyNoMeasure(int componentRef) {
    assertThat(measureRepository.getRawMeasures(componentRef).isEmpty()).isTrue();
  }

  private void verifyFileMeasures(int componentRef, long measureValue, long debt, long languageCost, SqaleRatingGrid.SqaleRating expectedRating) {
    long developmentCost = measureValue * languageCost;
    verifyComponentMeasures(componentRef, developmentCost, debt / developmentCost, expectedRating);
  }

  private void verifyComponentMeasures(int componentRef, long expectedDevCost, double expectedDebtRatio, SqaleRatingGrid.SqaleRating expectedRating) {
    assertThat(toEntries(measureRepository.getNewRawMeasures(componentRef))).containsOnly(
      MeasureRepoEntry.entryOf(DEVELOPMENT_COST_KEY, newMeasureBuilder().create(Long.toString(expectedDevCost))),
      MeasureRepoEntry.entryOf(SQALE_DEBT_RATIO_KEY, newMeasureBuilder().create(expectedDebtRatio * 100.0)),
      MeasureRepoEntry.entryOf(SQALE_RATING_KEY, createSqaleRatingMeasure(expectedRating))
      );
  }

}

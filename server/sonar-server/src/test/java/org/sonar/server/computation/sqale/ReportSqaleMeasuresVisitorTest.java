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
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.VisitorsCrawler;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepositoryRule;

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
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;
import static org.sonar.server.computation.sqale.SqaleRatingGrid.SqaleRating.A;
import static org.sonar.server.computation.sqale.SqaleRatingGrid.SqaleRating.C;

public class ReportSqaleMeasuresVisitorTest {

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
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(METRIC_1)
    .add(METRIC_2)
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
    // assumes SQALE rating configuration is consistent
    when(sqaleRatingSettings.getRatingGrid()).thenReturn(RATING_GRID);
    when(sqaleRatingSettings.getSizeMetricKey(LANGUAGE_KEY_1)).thenReturn(METRIC_KEY_1);
    when(sqaleRatingSettings.getSizeMetricKey(LANGUAGE_KEY_2)).thenReturn(METRIC_KEY_2);
    when(sqaleRatingSettings.getDevCost(LANGUAGE_KEY_1)).thenReturn(DEV_COST_LANGUAGE_1);
    when(sqaleRatingSettings.getDevCost(LANGUAGE_KEY_2)).thenReturn(DEV_COST_LANGUAGE_2);
  }

  @Test
  public void measures_created_for_project_are_all_zero_when_they_have_no_FILE_child() {
    ReportComponent root = ReportComponent.builder(PROJECT, 1).build();
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

    int componentRef = 1;
    ReportComponent fileComponent = createFileComponent(languageKey, componentRef);
    treeRootHolder.setRoot(fileComponent);
    addRawMeasure(metricKey, componentRef, measureValue);
    addRawMeasure(TECHNICAL_DEBT_KEY, componentRef, debt);

    underTest.visit(fileComponent);

    verifyFileMeasures(componentRef, measureValue, debt, languageCost, expectedRating);
  }

  @Test
  public void verify_aggregation_of_developmentCost_and_value_of_measures_computed_from_that() {
    ReportComponent root = ReportComponent.builder(PROJECT, 1)
      .addChildren(
        ReportComponent.builder(MODULE, 11)
          .addChildren(
            ReportComponent.builder(DIRECTORY, 111)
              .addChildren(
                createFileComponent(LANGUAGE_KEY_1, 1111),
                createFileComponent(LANGUAGE_KEY_2, 1112))
              .build(),
            ReportComponent.builder(DIRECTORY, 112)
              .addChildren(
                createFileComponent(LANGUAGE_KEY_2, 1121))
              .build())
          .build(),
        ReportComponent.builder(MODULE, 12)
          .addChildren(
            ReportComponent.builder(DIRECTORY, 121)
              .addChildren(
                createFileComponent(LANGUAGE_KEY_1, 1211))
              .build(),
            ReportComponent.builder(DIRECTORY, 122).build())
          .build(),
        ReportComponent.builder(MODULE, 13).build())
      .build();

    treeRootHolder.setRoot(root);

    long measureValue1111 = 10;
    long debt1111 = 66000l;
    addRawMeasure(METRIC_KEY_1, 1111, measureValue1111);
    addRawMeasure(TECHNICAL_DEBT_KEY, 1111, debt1111);

    long measureValue1112 = 10;
    long debt1112 = 4200l;
    addRawMeasure(METRIC_KEY_2, 1112, measureValue1112);
    addRawMeasure(TECHNICAL_DEBT_KEY, 1112, debt1112);

    long debt111 = 96325l;
    addRawMeasure(TECHNICAL_DEBT_KEY, 111, debt111);

    long measureValue1121 = 30;
    long debt1121 = 25200l;
    addRawMeasure(METRIC_KEY_2, 1121, measureValue1121);
    addRawMeasure(TECHNICAL_DEBT_KEY, 1121, debt1121);

    long debt112 = 99633l;
    addRawMeasure(TECHNICAL_DEBT_KEY, 112, debt112);

    long measureValue1211 = 20;
    long debt1211 = 33000l;
    addRawMeasure(METRIC_KEY_1, 1211, measureValue1211);
    addRawMeasure(TECHNICAL_DEBT_KEY, 1211, debt1211);

    long debt121 = 7524l;
    addRawMeasure(TECHNICAL_DEBT_KEY, 121, debt121);

    long debt1 = 9999l;
    addRawMeasure(TECHNICAL_DEBT_KEY, 1, debt1);

    underTest.visit(root);

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

  private ReportComponent createFileComponent(String languageKey1, int fileRef) {
    return ReportComponent.builder(FILE, fileRef).setFileAttributes(new FileAttributes(false, languageKey1)).build();
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

}

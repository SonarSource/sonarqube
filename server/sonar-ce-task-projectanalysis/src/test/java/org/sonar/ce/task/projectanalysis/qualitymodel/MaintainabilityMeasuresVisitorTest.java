/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepositoryRule;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.measure.DebtRatingGrid;
import org.sonar.server.measure.Rating;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST;
import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A;
import static org.sonar.api.measures.CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_DEBT_RATIO;
import static org.sonar.api.measures.CoreMetrics.SQALE_DEBT_RATIO_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.toEntries;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.C;
import static org.sonar.server.measure.Rating.E;

public class MaintainabilityMeasuresVisitorTest {

  static final String LANGUAGE_KEY_1 = "lKey1";
  static final String LANGUAGE_KEY_2 = "lKey2";

  static final double[] RATING_GRID = new double[] {0.1, 0.2, 0.5, 1};

  static final long DEV_COST_LANGUAGE_1 = 30;
  static final long DEV_COST_LANGUAGE_2 = 42;

  static final int PROJECT_REF = 1;
  static final int DIRECTORY_REF = 123;
  static final int FILE_1_REF = 1231;
  static final int FILE_2_REF = 1232;

  static final Component ROOT_PROJECT = builder(Component.Type.PROJECT, PROJECT_REF).setKey("project")
    .addChildren(
      builder(DIRECTORY, DIRECTORY_REF).setKey("directory")
        .addChildren(
          builder(FILE, FILE_1_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_KEY_1, 1)).setKey("file1").build(),
          builder(FILE, FILE_2_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_KEY_1, 1)).setKey("file2").build())
        .build())
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NCLOC)
    .add(DEVELOPMENT_COST)
    .add(TECHNICAL_DEBT)
    .add(SQALE_DEBT_RATIO)
    .add(SQALE_RATING)
    .add(EFFORT_TO_REACH_MAINTAINABILITY_RATING_A);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Rule
  public ComponentIssuesRepositoryRule componentIssuesRepositoryRule = new ComponentIssuesRepositoryRule(treeRootHolder);

  private RatingSettings ratingSettings = mock(RatingSettings.class);

  private VisitorsCrawler underTest;

  @Before
  public void setUp() {
    // assumes rating configuration is consistent
    when(ratingSettings.getDebtRatingGrid()).thenReturn(new DebtRatingGrid(RATING_GRID));
    when(ratingSettings.getDevCost(LANGUAGE_KEY_1)).thenReturn(DEV_COST_LANGUAGE_1);
    when(ratingSettings.getDevCost(LANGUAGE_KEY_2)).thenReturn(DEV_COST_LANGUAGE_2);

    underTest = new VisitorsCrawler(singletonList(new MaintainabilityMeasuresVisitor(metricRepository, measureRepository, ratingSettings)));
  }

  @Test
  public void measures_created_for_project_are_all_zero_when_they_have_no_FILE_child() {
    ReportComponent root = builder(PROJECT, 1).build();
    treeRootHolder.setRoot(root);

    underTest.visit(root);

    assertThat(measureRepository.getRawMeasures(root).entrySet().stream().map(e -> entryOf(e.getKey(), e.getValue())))
      .containsOnly(
        entryOf(DEVELOPMENT_COST_KEY, newMeasureBuilder().create("0")),
        entryOf(SQALE_DEBT_RATIO_KEY, newMeasureBuilder().create(0d, 1)),
        entryOf(SQALE_RATING_KEY, createMaintainabilityRatingMeasure(A)),
        entryOf(EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY, newMeasureBuilder().create(0L)));
  }

  @Test
  public void compute_development_cost() {
    ReportComponent root = builder(PROJECT, 1).addChildren(
      builder(DIRECTORY, 111).addChildren(
        createFileComponent(LANGUAGE_KEY_1, 1111),
        createFileComponent(LANGUAGE_KEY_2, 1112),
        // Unit test should not be ignored
        builder(FILE, 1113).setFileAttributes(new FileAttributes(true, LANGUAGE_KEY_1, 1)).build())
        .build(),
      builder(DIRECTORY, 112).addChildren(
        createFileComponent(LANGUAGE_KEY_2, 1121))
        .build(),
      builder(DIRECTORY, 121).addChildren(
        createFileComponent(LANGUAGE_KEY_1, 1211))
        .build(),
      builder(DIRECTORY, 122).build())
      .build();

    treeRootHolder.setRoot(root);

    int ncloc1112 = 12;
    addRawMeasure(NCLOC_KEY, 1112, ncloc1112);

    int ncloc1113 = 15;
    addRawMeasure(NCLOC_KEY, 1113, ncloc1113);

    int nclocValue1121 = 30;
    addRawMeasure(NCLOC_KEY, 1121, nclocValue1121);

    int ncloc1211 = 20;
    addRawMeasure(NCLOC_KEY, 1211, ncloc1211);

    underTest.visit(root);

    // verify measures on files
    verifyAddedRawMeasure(1112, DEVELOPMENT_COST_KEY, Long.toString(ncloc1112 * DEV_COST_LANGUAGE_2));
    verifyAddedRawMeasure(1113, DEVELOPMENT_COST_KEY, Long.toString(ncloc1113 * DEV_COST_LANGUAGE_1));
    verifyAddedRawMeasure(1121, DEVELOPMENT_COST_KEY, Long.toString(nclocValue1121 * DEV_COST_LANGUAGE_2));
    verifyAddedRawMeasure(1211, DEVELOPMENT_COST_KEY, Long.toString(ncloc1211 * DEV_COST_LANGUAGE_1));

    // directory has no children => no file => 0 everywhere and A rating
    verifyAddedRawMeasure(122, DEVELOPMENT_COST_KEY, "0");

    // directory has children => dev cost is aggregated
    verifyAddedRawMeasure(111, DEVELOPMENT_COST_KEY, Long.toString(
      ncloc1112 * DEV_COST_LANGUAGE_2 +
        ncloc1113 * DEV_COST_LANGUAGE_1));
    verifyAddedRawMeasure(112, DEVELOPMENT_COST_KEY, Long.toString(nclocValue1121 * DEV_COST_LANGUAGE_2));
    verifyAddedRawMeasure(121, DEVELOPMENT_COST_KEY, Long.toString(ncloc1211 * DEV_COST_LANGUAGE_1));

    verifyAddedRawMeasure(1, DEVELOPMENT_COST_KEY, Long.toString(
      ncloc1112 * DEV_COST_LANGUAGE_2 +
        ncloc1113 * DEV_COST_LANGUAGE_1 +
        nclocValue1121 * DEV_COST_LANGUAGE_2 +
        ncloc1211 * DEV_COST_LANGUAGE_1));
  }

  @Test
  public void compute_maintainability_debt_ratio_measure() {
    treeRootHolder.setRoot(ROOT_PROJECT);

    int file1Ncloc = 10;
    addRawMeasure(NCLOC_KEY, FILE_1_REF, file1Ncloc);
    long file1MaintainabilityCost = 100L;
    addRawMeasure(TECHNICAL_DEBT_KEY, FILE_1_REF, file1MaintainabilityCost);

    int file2Ncloc = 5;
    addRawMeasure(NCLOC_KEY, FILE_2_REF, file2Ncloc);
    long file2MaintainabilityCost = 1L;
    addRawMeasure(TECHNICAL_DEBT_KEY, FILE_2_REF, file2MaintainabilityCost);

    long directoryMaintainabilityCost = 100L;
    addRawMeasure(TECHNICAL_DEBT_KEY, DIRECTORY_REF, directoryMaintainabilityCost);

    long projectMaintainabilityCost = 1000L;
    addRawMeasure(TECHNICAL_DEBT_KEY, PROJECT_REF, projectMaintainabilityCost);

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasure(FILE_1_REF, SQALE_DEBT_RATIO_KEY, file1MaintainabilityCost * 1d / (file1Ncloc * DEV_COST_LANGUAGE_1) * 100);
    verifyAddedRawMeasure(FILE_2_REF, SQALE_DEBT_RATIO_KEY, file2MaintainabilityCost * 1d / (file2Ncloc * DEV_COST_LANGUAGE_1) * 100);
    verifyAddedRawMeasure(DIRECTORY_REF, SQALE_DEBT_RATIO_KEY, directoryMaintainabilityCost * 1d / ((file1Ncloc + file2Ncloc) * DEV_COST_LANGUAGE_1) * 100);
    verifyAddedRawMeasure(PROJECT_REF, SQALE_DEBT_RATIO_KEY, projectMaintainabilityCost * 1d / ((file1Ncloc + file2Ncloc) * DEV_COST_LANGUAGE_1) * 100);
  }

  @Test
  public void compute_maintainability_rating_measure() {
    treeRootHolder.setRoot(ROOT_PROJECT);

    addRawMeasure(NCLOC_KEY, FILE_1_REF, 10);
    addRawMeasure(TECHNICAL_DEBT_KEY, FILE_1_REF, 100L);

    addRawMeasure(NCLOC_KEY, FILE_2_REF, 5);
    addRawMeasure(TECHNICAL_DEBT_KEY, FILE_2_REF, 1L);

    addRawMeasure(TECHNICAL_DEBT_KEY, DIRECTORY_REF, 100L);
    addRawMeasure(TECHNICAL_DEBT_KEY, PROJECT_REF, 1000L);

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasure(FILE_1_REF, SQALE_RATING_KEY, C);
    verifyAddedRawMeasure(FILE_2_REF, SQALE_RATING_KEY, A);
    verifyAddedRawMeasure(DIRECTORY_REF, SQALE_RATING_KEY, C);
    verifyAddedRawMeasure(PROJECT_REF, SQALE_RATING_KEY, E);
  }

  @Test
  public void compute_effort_to_maintainability_rating_A_measure() {
    treeRootHolder.setRoot(ROOT_PROJECT);

    int file1Ncloc = 10;
    long file1Effort = 100L;
    addRawMeasure(NCLOC_KEY, FILE_1_REF, file1Ncloc);
    addRawMeasure(TECHNICAL_DEBT_KEY, FILE_1_REF, file1Effort);

    int file2Ncloc = 5;
    long file2Effort = 20L;
    addRawMeasure(NCLOC_KEY, FILE_2_REF, file2Ncloc);
    addRawMeasure(TECHNICAL_DEBT_KEY, FILE_2_REF, file2Effort);

    long dirEffort = 120L;
    addRawMeasure(TECHNICAL_DEBT_KEY, DIRECTORY_REF, dirEffort);

    long projectEffort = 150L;
    addRawMeasure(TECHNICAL_DEBT_KEY, PROJECT_REF, projectEffort);

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasure(FILE_1_REF, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY,
      (long) (file1Effort - RATING_GRID[0] * file1Ncloc * DEV_COST_LANGUAGE_1));
    verifyAddedRawMeasure(FILE_2_REF, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY,
      (long) (file2Effort - RATING_GRID[0] * file2Ncloc * DEV_COST_LANGUAGE_1));
    verifyAddedRawMeasure(DIRECTORY_REF, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY,
      (long) (dirEffort - RATING_GRID[0] * (file1Ncloc + file2Ncloc) * DEV_COST_LANGUAGE_1));
    verifyAddedRawMeasure(PROJECT_REF, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY,
      (long) (projectEffort - RATING_GRID[0] * (file1Ncloc + file2Ncloc) * DEV_COST_LANGUAGE_1));
  }

  @Test
  public void compute_0_effort_to_maintainability_rating_A_when_effort_is_lower_than_dev_cost() {
    treeRootHolder.setRoot(ROOT_PROJECT);

    addRawMeasure(NCLOC_KEY, FILE_1_REF, 10);
    addRawMeasure(TECHNICAL_DEBT_KEY, FILE_1_REF, 2L);

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasure(FILE_1_REF, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY, 0L);
  }

  @Test
  public void effort_to_maintainability_rating_A_is_same_as_effort_when_no_dev_cost() {
    treeRootHolder.setRoot(ROOT_PROJECT);

    addRawMeasure(TECHNICAL_DEBT_KEY, FILE_1_REF, 100L);

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasure(FILE_1_REF, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY, 100);
  }

  private void addRawMeasure(String metricKey, int componentRef, long value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

  private void addRawMeasure(String metricKey, int componentRef, int value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

  private void verifyAddedRawMeasure(int componentRef, String metricKey, long value) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(entryOf(metricKey, newMeasureBuilder().create(value)));
  }

  private void verifyAddedRawMeasure(int componentRef, String metricKey, double value) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(entryOf(metricKey, newMeasureBuilder().create(value, 1)));
  }

  private void verifyAddedRawMeasure(int componentRef, String metricKey, Rating rating) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(entryOf(metricKey, newMeasureBuilder().create(rating.getIndex(), rating.name())));
  }

  private void verifyAddedRawMeasure(int componentRef, String metricKey, String value) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(entryOf(metricKey, newMeasureBuilder().create(value)));
  }

  private static ReportComponent createFileComponent(String languageKey1, int fileRef) {
    return builder(FILE, fileRef).setFileAttributes(new FileAttributes(false, languageKey1, 1)).build();
  }

  private static Measure createMaintainabilityRatingMeasure(Rating rating) {
    return newMeasureBuilder().create(rating.getIndex(), rating.name());
  }

}

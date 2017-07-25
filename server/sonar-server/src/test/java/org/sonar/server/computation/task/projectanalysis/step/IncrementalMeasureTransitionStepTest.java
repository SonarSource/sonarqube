/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.metric.ScannerMetrics;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component.Status;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.step.ComputationStep;

public class IncrementalMeasureTransitionStepTest extends BaseStepTest {
  private static final int ROOT_REF = 1;
  private static final int FILE1_REF = 2;
  private static final int FILE2_REF = 3;
  private static final String PROJECT_KEY = "project";
  private static final String FILE1_KEY = "file1";
  private static final String FILE2_KEY = "file2";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(
      builder(PROJECT, ROOT_REF).setKey(PROJECT_KEY)
        .addChildren(
          builder(FILE, FILE1_REF).setKey(FILE1_KEY).setStatus(Status.SAME).build(),
          builder(FILE, FILE2_REF).setKey(FILE2_KEY).setStatus(Status.CHANGED).build())
        .build());
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.COMMENT_LINES_DENSITY)
    .add(CoreMetrics.COMMENT_LINES);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  public ScannerMetrics scannerMetrics = new ScannerMetrics();

  IncrementalMeasureTransitionStep underTest = new IncrementalMeasureTransitionStep(treeRootHolder, measureRepository,
    analysisMetadataHolder, metricRepository, scannerMetrics);

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void copy_scanner_measures_only() {
    analysisMetadataHolder.setIncrementalAnalysis(true);
    measureRepository.addBaseMeasure(FILE1_REF, CoreMetrics.COMMENT_LINES_DENSITY_KEY, newMeasureBuilder().create(20));
    measureRepository.addBaseMeasure(FILE1_REF, CoreMetrics.COMMENT_LINES_KEY, newMeasureBuilder().create(2));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE1_REF, CoreMetrics.COMMENT_LINES_DENSITY_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasures(FILE1_REF)).hasSize(1);
    assertThat(measureRepository.getAddedRawMeasure(FILE1_REF, CoreMetrics.COMMENT_LINES_KEY).get().getIntValue()).isEqualTo(2);
  }

  @Test
  public void copy_same_files_measures_only() {
    analysisMetadataHolder.setIncrementalAnalysis(true);
    measureRepository.addBaseMeasure(FILE1_REF, CoreMetrics.COMMENT_LINES_KEY, newMeasureBuilder().create(2));
    measureRepository.addBaseMeasure(FILE2_REF, CoreMetrics.COMMENT_LINES_KEY, newMeasureBuilder().create(2));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE2_REF, CoreMetrics.COMMENT_LINES_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasures(FILE1_REF)).hasSize(1);
    assertThat(measureRepository.getAddedRawMeasure(FILE1_REF, CoreMetrics.COMMENT_LINES_KEY).get().getIntValue()).isEqualTo(2);
  }

  @Test
  public void skip_if_not_incremental() {
    analysisMetadataHolder.setIncrementalAnalysis(false);
    measureRepository.addBaseMeasure(FILE1_REF, CoreMetrics.COMMENT_LINES_KEY, newMeasureBuilder().create(2));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasures(FILE1_REF)).isEmpty();
  }

}

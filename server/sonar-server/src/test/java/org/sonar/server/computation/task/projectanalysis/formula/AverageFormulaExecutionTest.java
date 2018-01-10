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
package org.sonar.server.computation.task.projectanalysis.formula;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.MODULE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class AverageFormulaExecutionTest {

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.FUNCTION_COMPLEXITY)
    .add(CoreMetrics.COMPLEXITY_IN_FUNCTIONS)
    .add(CoreMetrics.FUNCTIONS);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public PeriodHolderRule periodsHolder = new PeriodHolderRule();

  FormulaExecutorComponentVisitor underTest;

  @Before
  public void setUp() throws Exception {
    underTest = FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
      .buildFor(Lists.newArrayList(
        AverageFormula.Builder.newBuilder()
          .setOutputMetricKey(FUNCTION_COMPLEXITY_KEY)
          .setMainMetricKey(COMPLEXITY_IN_FUNCTIONS_KEY)
          .setByMetricKey(FUNCTIONS_KEY)
          .build()));
  }

  @Test
  public void add_measures() {
    ReportComponent project = builder(PROJECT, 1)
      .addChildren(
        builder(MODULE, 11)
          .addChildren(
            builder(DIRECTORY, 111)
              .addChildren(
                builder(Component.Type.FILE, 1111).build(),
                builder(Component.Type.FILE, 1112).build()
              ).build()
          ).build(),
        builder(MODULE, 12)
          .addChildren(
            builder(DIRECTORY, 121)
              .addChildren(
                builder(Component.Type.FILE, 1211).build()
              ).build()
          ).build()
      ).build();

    treeRootHolder.setRoot(project);

    measureRepository.addRawMeasure(1111, COMPLEXITY_IN_FUNCTIONS_KEY, newMeasureBuilder().create(5));
    measureRepository.addRawMeasure(1111, FUNCTIONS_KEY, newMeasureBuilder().create(2));

    measureRepository.addRawMeasure(1112, COMPLEXITY_IN_FUNCTIONS_KEY, newMeasureBuilder().create(1));
    measureRepository.addRawMeasure(1112, FUNCTIONS_KEY, newMeasureBuilder().create(1));

    measureRepository.addRawMeasure(1211, COMPLEXITY_IN_FUNCTIONS_KEY, newMeasureBuilder().create(9));
    measureRepository.addRawMeasure(1211, FUNCTIONS_KEY, newMeasureBuilder().create(2));

    new PathAwareCrawler<>(underTest).visit(project);

    assertThat(toEntries(measureRepository.getAddedRawMeasures(1))).containsOnly(entryOf(FUNCTION_COMPLEXITY_KEY, newMeasureBuilder().create(3d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(11))).containsOnly(entryOf(FUNCTION_COMPLEXITY_KEY, newMeasureBuilder().create(2d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(111))).containsOnly(entryOf(FUNCTION_COMPLEXITY_KEY, newMeasureBuilder().create(2d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(1111))).containsOnly(entryOf(FUNCTION_COMPLEXITY_KEY, newMeasureBuilder().create(2.5d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(1112))).containsOnly(entryOf(FUNCTION_COMPLEXITY_KEY, newMeasureBuilder().create(1d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(12))).containsOnly(entryOf(FUNCTION_COMPLEXITY_KEY, newMeasureBuilder().create(4.5d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(121))).containsOnly(entryOf(FUNCTION_COMPLEXITY_KEY, newMeasureBuilder().create(4.5d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(1211))).containsOnly(entryOf(FUNCTION_COMPLEXITY_KEY, newMeasureBuilder().create(4.5d, 1)));
  }

  @Test
  public void not_add_measures_when_no_data_on_file() {
    ReportComponent project = builder(PROJECT, 1)
      .addChildren(
        builder(MODULE, 11)
          .addChildren(
            builder(DIRECTORY, 111)
              .addChildren(
                builder(Component.Type.FILE, 1111).build()
              ).build()
          ).build()
      ).build();

    treeRootHolder.setRoot(project);

    new PathAwareCrawler<>(underTest).visit(project);

    assertThat(measureRepository.getAddedRawMeasures(1)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(11)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(111)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(1111)).isEmpty();
  }

}

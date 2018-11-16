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
package org.sonar.ce.task.projectanalysis.formula;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.formula.SumFormula.createIntSumFormula;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class SumFormulaExecutionTest {

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(CoreMetrics.LINES);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  FormulaExecutorComponentVisitor underTest;

  @Before
  public void setUp() throws Exception {
    underTest = FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
      .buildFor(Lists.newArrayList(createIntSumFormula(LINES_KEY)));
  }

  @Test
  public void add_measures() {
    ReportComponent project = builder(PROJECT, 1)
      .addChildren(
        builder(DIRECTORY, 111)
          .addChildren(
            builder(Component.Type.FILE, 1111).build(),
            builder(Component.Type.FILE, 1112).build())
          .build(),
        builder(DIRECTORY, 121)
          .addChildren(
            builder(Component.Type.FILE, 1211).build())
          .build())
      .build();

    treeRootHolder.setRoot(project);

    measureRepository.addRawMeasure(1111, LINES_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(1112, LINES_KEY, newMeasureBuilder().create(8));
    measureRepository.addRawMeasure(1211, LINES_KEY, newMeasureBuilder().create(2));

    new PathAwareCrawler<>(underTest).visit(project);

    assertThat(toEntries(measureRepository.getAddedRawMeasures(1))).containsOnly(entryOf(LINES_KEY, newMeasureBuilder().create(20)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(111))).containsOnly(entryOf(LINES_KEY, newMeasureBuilder().create(18)));
    assertThat(measureRepository.getAddedRawMeasures(1111)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(1112)).isEmpty();
    assertThat(toEntries(measureRepository.getAddedRawMeasures(121))).containsOnly(entryOf(LINES_KEY, newMeasureBuilder().create(2)));
    assertThat(measureRepository.getAddedRawMeasures(1211)).isEmpty();
  }

  @Test
  public void not_add_measures_when_no_data_on_file() {
    ReportComponent project = builder(PROJECT, 1)
      .addChildren(
        builder(DIRECTORY, 111)
          .addChildren(
            builder(Component.Type.FILE, 1111).build())
          .build())
      .build();

    treeRootHolder.setRoot(project);

    new PathAwareCrawler<>(underTest).visit(project);

    assertThat(measureRepository.getAddedRawMeasures(1)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(111)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(1111)).isEmpty();
  }

}

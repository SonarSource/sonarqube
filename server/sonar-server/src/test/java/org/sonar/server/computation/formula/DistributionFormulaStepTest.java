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

package org.sonar.server.computation.formula;

import com.google.common.collect.Lists;
import org.assertj.guava.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.step.ComputeFormulaMeasuresStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.DumbComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;

public class DistributionFormulaStepTest {

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  ComputeFormulaMeasuresStep sut;

  @Before
  public void setUp() throws Exception {
    FormulaRepository formulaRepository = mock(FormulaRepository.class);
    when(formulaRepository.getFormulas()).thenReturn(Lists.<Formula>newArrayList(new DistributionFormula(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY)));
    sut = new ComputeFormulaMeasuresStep(treeRootHolder, measureRepository, metricRepository, formulaRepository);
  }

  @Test
  public void add_measures() throws Exception {
    DumbComponent project = builder(PROJECT, 1)
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

    measureRepository.addRawMeasure(1111, FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0.5=3;3.5=5;6.5=9"));
    measureRepository.addRawMeasure(1112, FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0.5=0;3.5=2;6.5=1"));
    measureRepository.addRawMeasure(1211, FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0.5=1;3.5=3;6.5=2"));

    sut.execute();

    assertThat(toEntries(measureRepository.getNewRawMeasures(1))).containsOnly(entryOf(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0.5=4;3.5=10;6.5=12")));
    assertThat(toEntries(measureRepository.getNewRawMeasures(11))).containsOnly(entryOf(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0.5=3;3.5=7;6.5=10")));
    assertThat(toEntries(measureRepository.getNewRawMeasures(111))).containsOnly(entryOf(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0.5=3;3.5=7;6.5=10")));
    Assertions.assertThat(measureRepository.getNewRawMeasures(1111)).isEmpty();
    Assertions.assertThat(measureRepository.getNewRawMeasures(1112)).isEmpty();
    assertThat(toEntries(measureRepository.getNewRawMeasures(12))).containsOnly(entryOf(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0.5=1;3.5=3;6.5=2")));
    assertThat(toEntries(measureRepository.getNewRawMeasures(121))).containsOnly(entryOf(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0.5=1;3.5=3;6.5=2")));
    Assertions.assertThat(measureRepository.getNewRawMeasures(1211)).isEmpty();
  }

}

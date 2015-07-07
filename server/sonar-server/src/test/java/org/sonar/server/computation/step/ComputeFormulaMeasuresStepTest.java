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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.formula.Counter;
import org.sonar.server.computation.formula.CounterContext;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaRepository;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.DumbComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;

public class ComputeFormulaMeasuresStepTest {

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.LINES)
    .add(CoreMetrics.NCLOC);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  ComputeFormulaMeasuresStep sut;

  @Before
  public void setUp() {
    FormulaRepository formulaRepository = mock(FormulaRepository.class);
    when(formulaRepository.getFormulas()).thenReturn(Lists.<Formula>newArrayList(new FakeFormula()));
    sut = new ComputeFormulaMeasuresStep(treeRootHolder, measureRepository, metricRepository, formulaRepository);
  }

  @Test
  public void add_measures() {
    DumbComponent project = DumbComponent.builder(PROJECT, 1)
      .addChildren(
        DumbComponent.builder(MODULE, 11)
          .addChildren(
            DumbComponent.builder(DIRECTORY, 111)
              .addChildren(
                builder(Component.Type.FILE, 1111).build(),
                builder(Component.Type.FILE, 1112).build()
              ).build()
          ).build(),
        DumbComponent.builder(MODULE, 12)
          .addChildren(
            DumbComponent.builder(DIRECTORY, 121)
              .addChildren(
                builder(Component.Type.FILE, 1211).build()
              ).build()
          ).build()
      ).build();

    treeRootHolder.setRoot(project);

    measureRepository.addRawMeasure(1111, CoreMetrics.LINES_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(1112, CoreMetrics.LINES_KEY, newMeasureBuilder().create(8));
    measureRepository.addRawMeasure(1211, CoreMetrics.LINES_KEY, newMeasureBuilder().create(2));

    sut.execute();

    assertThat(toEntries(measureRepository.getNewRawMeasures(1))).containsOnly(entryOf(CoreMetrics.NCLOC_KEY, newMeasureBuilder().create(20)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(11))).containsOnly(entryOf(CoreMetrics.NCLOC_KEY, newMeasureBuilder().create(18)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(111))).containsOnly(entryOf(CoreMetrics.NCLOC_KEY, newMeasureBuilder().create(18)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(1111))).containsOnly(entryOf(CoreMetrics.NCLOC_KEY, newMeasureBuilder().create(10)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(1112))).containsOnly(entryOf(CoreMetrics.NCLOC_KEY, newMeasureBuilder().create(8)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(12))).containsOnly(entryOf(CoreMetrics.NCLOC_KEY, newMeasureBuilder().create(2)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(121))).containsOnly(entryOf(CoreMetrics.NCLOC_KEY, newMeasureBuilder().create(2)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(1211))).containsOnly(entryOf(CoreMetrics.NCLOC_KEY, newMeasureBuilder().create(2)));
  }

  @Test
  public void add_no_measure() {
    DumbComponent project = DumbComponent.builder(PROJECT, 1)
      .addChildren(
        DumbComponent.builder(MODULE, 11)
          .addChildren(
            DumbComponent.builder(DIRECTORY, 111)
              .addChildren(
                builder(Component.Type.FILE, 1111).build()
              ).build()
          ).build()
      ).build();

    treeRootHolder.setRoot(project);

    sut.execute();

    assertThat(measureRepository.getNewRawMeasures(1)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(11)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(111)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(1111)).isEmpty();
  }

  @Test
  public void add_no_measure_when_no_file() {
    DumbComponent project = DumbComponent.builder(PROJECT, 1)
      .addChildren(
        DumbComponent.builder(MODULE, 11)
          .addChildren(
            DumbComponent.builder(DIRECTORY, 111).build()
          ).build()
      ).build();

    treeRootHolder.setRoot(project);

    sut.execute();

    assertThat(measureRepository.getNewRawMeasures(1)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(11)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(111)).isEmpty();
  }

  @Test
  public void add_no_measure_on_module_without_file() {
    DumbComponent project = DumbComponent.builder(PROJECT, 1)
      .addChildren(
        DumbComponent.builder(MODULE, 11)
          .addChildren(
            DumbComponent.builder(DIRECTORY, 111).build()
          ).build(),
        DumbComponent.builder(MODULE, 12)
          .addChildren(
            DumbComponent.builder(DIRECTORY, 121)
              .addChildren(
                builder(Component.Type.FILE, 1211).build()
              ).build()
          ).build()
      ).build();
    treeRootHolder.setRoot(project);
    measureRepository.addRawMeasure(1211, CoreMetrics.LINES_KEY, newMeasureBuilder().create(10));

    sut.execute();

    assertThat(toEntries(measureRepository.getNewRawMeasures(1))).containsOnly(entryOf(CoreMetrics.NCLOC_KEY, newMeasureBuilder().create(10)));
    assertThat(measureRepository.getNewRawMeasures(11)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(111)).isEmpty();
    assertThat(toEntries(measureRepository.getNewRawMeasures(12))).containsOnly(entryOf(CoreMetrics.NCLOC_KEY, newMeasureBuilder().create(10)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(121))).containsOnly(entryOf(CoreMetrics.NCLOC_KEY, newMeasureBuilder().create(10)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(1211))).containsOnly(entryOf(CoreMetrics.NCLOC_KEY, newMeasureBuilder().create(10)));
  }

  private static class FakeFormula implements Formula<FakeCounter> {

    @Override
    public FakeCounter createNewCounter() {
      return new FakeCounter();
    }

    @Override
    public Optional<Measure> createMeasure(FakeCounter counter, Component.Type componentType) {
      if (counter.value <= 0) {
        return Optional.absent();
      }
      return Optional.of(Measure.newMeasureBuilder().create(counter.value));
    }

    @Override
    public String getOutputMetricKey() {
      return CoreMetrics.NCLOC_KEY;
    }
  }

  private static class FakeCounter implements Counter<FakeCounter> {

    private int value = 0;

    @Override
    public void aggregate(FakeCounter counter) {
      this.value += counter.value;
    }

    @Override
    public void aggregate(CounterContext counterContext) {
      Optional<Measure> measureOptional = counterContext.getMeasure(CoreMetrics.LINES_KEY);
      if (measureOptional.isPresent()) {
        value += measureOptional.get().getIntValue();
      }
    }
  }
}

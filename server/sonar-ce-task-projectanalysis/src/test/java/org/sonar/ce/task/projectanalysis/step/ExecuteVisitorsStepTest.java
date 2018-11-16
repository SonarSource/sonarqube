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
package org.sonar.ce.task.projectanalysis.step;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.ChangeLogLevel;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ComponentVisitor;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.TestComputationStepContext;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class ExecuteVisitorsStepTest {

  private static final String TEST_METRIC_KEY = "test";

  private static final int ROOT_REF = 1;
  private static final int DIRECTORY_REF = 123;
  private static final int FILE_1_REF = 1231;
  private static final int FILE_2_REF = 1232;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(1, NCLOC)
    .add(new MetricImpl(2, TEST_METRIC_KEY, "name", Metric.MetricType.INT));
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF).setKey("project")
        .addChildren(
          builder(DIRECTORY, DIRECTORY_REF).setKey("directory")
            .addChildren(
              builder(FILE, FILE_1_REF).setKey("file1").build(),
              builder(FILE, FILE_2_REF).setKey("file2").build())
            .build())
        .build());
  }

  @Test
  public void execute_with_type_aware_visitor() {
    ExecuteVisitorsStep underStep = new ExecuteVisitorsStep(treeRootHolder, singletonList(new TestTypeAwareVisitor()));

    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(1));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(2));
    measureRepository.addRawMeasure(DIRECTORY_REF, NCLOC_KEY, newMeasureBuilder().create(3));
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(3));

    underStep.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(2);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(3);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(4);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(4);
  }

  @Test
  public void execute_with_path_aware_visitor() {
    ExecuteVisitorsStep underTest = new ExecuteVisitorsStep(treeRootHolder, singletonList(new TestPathAwareVisitor()));

    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(1));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(1));

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(2);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(2);
  }

  @Test
  public void execute_logs_at_info_level_all_execution_duration_of_all_visitors() {
    try (ChangeLogLevel executor = new ChangeLogLevel(ExecuteVisitorsStep.class, LoggerLevel.DEBUG);
      ChangeLogLevel step1 = new ChangeLogLevel(VisitorA.class, LoggerLevel.DEBUG);
      ChangeLogLevel step2 = new ChangeLogLevel(VisitorB.class, LoggerLevel.DEBUG);
      ChangeLogLevel step3 = new ChangeLogLevel(VisitorB.class, LoggerLevel.DEBUG)) {
      ExecuteVisitorsStep underTest = new ExecuteVisitorsStep(
        treeRootHolder,
        asList(new VisitorA(), new VisitorB(), new VisitorC()));

      underTest.execute(new TestComputationStepContext());

      List<String> logs = logTester.logs(LoggerLevel.DEBUG);
      assertThat(logs).hasSize(4);
      assertThat(logs.get(0)).isEqualTo("  Execution time for each component visitor:");
      assertThat(logs.get(1)).startsWith("  - VisitorA | time=");
      assertThat(logs.get(2)).startsWith("  - VisitorB | time=");
      assertThat(logs.get(3)).startsWith("  - VisitorC | time=");

    }
  }

  private static class VisitorA extends TypeAwareVisitorAdapter {
    VisitorA() {
      super(CrawlerDepthLimit.PROJECT, Order.PRE_ORDER);
    }
  }

  private static class VisitorB extends TypeAwareVisitorAdapter {
    VisitorB() {
      super(CrawlerDepthLimit.PROJECT, Order.PRE_ORDER);
    }
  }

  private static class VisitorC extends TypeAwareVisitorAdapter {
    VisitorC() {
      super(CrawlerDepthLimit.PROJECT, Order.PRE_ORDER);
    }
  }

  private class TestTypeAwareVisitor extends TypeAwareVisitorAdapter {

    TestTypeAwareVisitor() {
      super(CrawlerDepthLimit.FILE, ComponentVisitor.Order.POST_ORDER);
    }

    @Override
    public void visitAny(Component any) {
      int ncloc = measureRepository.getRawMeasure(any, metricRepository.getByKey(NCLOC_KEY)).get().getIntValue();
      measureRepository.add(any, metricRepository.getByKey(TEST_METRIC_KEY), newMeasureBuilder().create(ncloc + 1));
    }
  }

  private class TestPathAwareVisitor extends PathAwareVisitorAdapter<Counter> {

    TestPathAwareVisitor() {
      super(CrawlerDepthLimit.FILE, ComponentVisitor.Order.POST_ORDER, new SimpleStackElementFactory<Counter>() {
        @Override
        public Counter createForAny(Component component) {
          return new Counter();
        }
      });
    }

    @Override
    public void visitProject(Component project, Path<Counter> path) {
      computeAndSaveMeasures(project, path);
    }

    @Override
    public void visitDirectory(Component directory, Path<Counter> path) {
      computeAndSaveMeasures(directory, path);
    }

    @Override
    public void visitFile(Component file, Path<Counter> path) {
      int ncloc = measureRepository.getRawMeasure(file, metricRepository.getByKey(NCLOC_KEY)).get().getIntValue();
      path.current().add(ncloc);
      computeAndSaveMeasures(file, path);
    }

    private void computeAndSaveMeasures(Component component, Path<Counter> path) {
      measureRepository.add(component, metricRepository.getByKey(TEST_METRIC_KEY), newMeasureBuilder().create(path.current().getValue()));
      increaseParentValue(path);
    }

    private void increaseParentValue(Path<Counter> path) {
      if (!path.isRoot()) {
        path.parent().add(path.current().getValue());
      }
    }
  }

  public class Counter {
    private int value = 0;

    public void add(int value) {
      this.value += value;
    }

    public int getValue() {
      return value;
    }
  }
}

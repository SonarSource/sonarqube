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
package org.sonar.server.computation.task.projectanalysis.measure;

import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.test.TestMeasureComputerDefinitionContext;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor;
import org.sonar.server.computation.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.server.computation.task.projectanalysis.issue.ComponentIssuesRepository;
import org.sonar.server.computation.task.projectanalysis.api.measurecomputer.MeasureComputerDefinitionImpl;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ViewsComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class ViewsMeasureComputersVisitorTest {

  private static final String NEW_METRIC_KEY = "new_metric_key";
  private static final String NEW_METRIC_NAME = "new metric name";

  private static final org.sonar.api.measures.Metric<Integer> NEW_METRIC = new org.sonar.api.measures.Metric.Builder(NEW_METRIC_KEY, NEW_METRIC_NAME,
    org.sonar.api.measures.Metric.ValueType.INT)
    .create();

  private static final int ROOT_REF = 1;
  private static final int VIEW_REF = 12;
  private static final int SUB_SUBVIEW_REF = 123;
  private static final int PROJECT_VIEW_1_REF = 1231;
  private static final int PROJECT_VIEW_2_REF = 1232;

  private static final Component TREE_WITH_SUB_VIEWS = builder(VIEW, ROOT_REF)
    .addChildren(
      builder(SUBVIEW, VIEW_REF)
        .addChildren(
          builder(SUBVIEW, SUB_SUBVIEW_REF)
            .addChildren(
              builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build(),
              builder(PROJECT_VIEW, PROJECT_VIEW_2_REF).build())
            .build())
        .build())
    .build();

  private static final Component TREE_WITH_DIRECT_PROJECT_VIEW = builder(VIEW, ROOT_REF)
    .addChildren(
      builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build(),
      builder(PROJECT_VIEW, PROJECT_VIEW_2_REF).build())
    .build();

  private static final MeasureComputer MEASURE_COMPUTER = new MeasureComputer() {
    @Override
    public MeasureComputer.MeasureComputerDefinition define(MeasureComputer.MeasureComputerDefinitionContext defContext) {
      return new MeasureComputerDefinitionImpl.BuilderImpl()
        .setInputMetrics(NCLOC_KEY, COMMENT_LINES_KEY)
        .setOutputMetrics(NEW_METRIC_KEY)
        .build();
    }

    @Override
    public void compute(MeasureComputer.MeasureComputerContext context) {
      org.sonar.api.ce.measure.Measure ncloc = context.getMeasure(NCLOC_KEY);
      org.sonar.api.ce.measure.Measure comment = context.getMeasure(COMMENT_LINES_KEY);
      if (ncloc != null && comment != null) {
        context.addMeasure(NEW_METRIC_KEY, ncloc.getIntValue() + comment.getIntValue());
      }
    }
  };

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NCLOC)
    .add(COMMENT_LINES)
    .add(NEW_METRIC);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(TREE_WITH_SUB_VIEWS, metricRepository);

  @Rule
  public MeasureComputersHolderRule measureComputersHolder = new MeasureComputersHolderRule(new TestMeasureComputerDefinitionContext());

  ComponentIssuesRepository componentIssuesRepository = mock(ComponentIssuesRepository.class);

  @Test
  public void compute_plugin_measure() {
    treeRootHolder.setRoot(TREE_WITH_SUB_VIEWS);

    addRawMeasure(PROJECT_VIEW_1_REF, NCLOC_KEY, 10);
    addRawMeasure(PROJECT_VIEW_1_REF, COMMENT_LINES_KEY, 2);
    addRawMeasure(PROJECT_VIEW_2_REF, NCLOC_KEY, 40);
    addRawMeasure(PROJECT_VIEW_2_REF, COMMENT_LINES_KEY, 5);
    addRawMeasure(SUB_SUBVIEW_REF, NCLOC_KEY, 50);
    addRawMeasure(SUB_SUBVIEW_REF, COMMENT_LINES_KEY, 7);
    addRawMeasure(VIEW_REF, NCLOC_KEY, 50);
    addRawMeasure(VIEW_REF, COMMENT_LINES_KEY, 7);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 50);
    addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, 7);

    measureComputersHolder.addMeasureComputer(MEASURE_COMPUTER);

    VisitorsCrawler visitorsCrawler = new VisitorsCrawler(Arrays.asList(new MeasureComputersVisitor(metricRepository, measureRepository, null,
      measureComputersHolder, componentIssuesRepository)));
    visitorsCrawler.visit(treeRootHolder.getRoot());

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasure(57, SUB_SUBVIEW_REF, NEW_METRIC_KEY);
    assertAddedRawMeasure(57, VIEW_REF, NEW_METRIC_KEY);
    assertAddedRawMeasure(57, ROOT_REF, NEW_METRIC_KEY);
  }

  @Test
  public void compute_plugin_measure_on_views_tree_having_only_one_view_with_a_project_view() {
    treeRootHolder.setRoot(TREE_WITH_DIRECT_PROJECT_VIEW);

    addRawMeasure(PROJECT_VIEW_1_REF, NCLOC_KEY, 10);
    addRawMeasure(PROJECT_VIEW_1_REF, COMMENT_LINES_KEY, 2);
    addRawMeasure(PROJECT_VIEW_2_REF, NCLOC_KEY, 40);
    addRawMeasure(PROJECT_VIEW_2_REF, COMMENT_LINES_KEY, 5);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 50);
    addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, 7);

    measureComputersHolder.addMeasureComputer(MEASURE_COMPUTER);

    VisitorsCrawler visitorsCrawler = new VisitorsCrawler(Arrays.asList(new MeasureComputersVisitor(metricRepository, measureRepository, null,
      measureComputersHolder, componentIssuesRepository)));
    visitorsCrawler.visit(treeRootHolder.getRoot());

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasure(57, ROOT_REF, NEW_METRIC_KEY);
  }

  @Test
  public void nothing_to_compute_when_no_project_view() {
    treeRootHolder.setRoot(builder(VIEW, ROOT_REF)
      .addChildren(
        builder(SUBVIEW, VIEW_REF)
          .addChildren(
            builder(SUBVIEW, SUB_SUBVIEW_REF)
              .build())
          .build())
      .build());

    measureComputersHolder.addMeasureComputer(MEASURE_COMPUTER);

    VisitorsCrawler visitorsCrawler = new VisitorsCrawler(Arrays.asList(new MeasureComputersVisitor(metricRepository, measureRepository, null,
      measureComputersHolder, componentIssuesRepository)));
    visitorsCrawler.visit(treeRootHolder.getRoot());

    assertNoAddedRawMeasureOnProjectViews();
    assertNoAddedRawMeasure(SUB_SUBVIEW_REF);
    assertNoAddedRawMeasure(VIEW_REF);
    assertNoAddedRawMeasure(ROOT_REF);
  }

  @Test
  public void nothing_to_compute_when_no_measure_computers() {
    treeRootHolder.setRoot(TREE_WITH_SUB_VIEWS);

    addRawMeasure(PROJECT_VIEW_1_REF, NCLOC_KEY, 10);
    addRawMeasure(PROJECT_VIEW_1_REF, COMMENT_LINES_KEY, 2);
    addRawMeasure(PROJECT_VIEW_2_REF, NCLOC_KEY, 40);
    addRawMeasure(PROJECT_VIEW_2_REF, COMMENT_LINES_KEY, 5);
    addRawMeasure(SUB_SUBVIEW_REF, NCLOC_KEY, 50);
    addRawMeasure(SUB_SUBVIEW_REF, COMMENT_LINES_KEY, 7);
    addRawMeasure(VIEW_REF, NCLOC_KEY, 50);
    addRawMeasure(VIEW_REF, COMMENT_LINES_KEY, 7);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 50);
    addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, 7);

    VisitorsCrawler visitorsCrawler = new VisitorsCrawler(Arrays.asList(new MeasureComputersVisitor(metricRepository, measureRepository, null,
      measureComputersHolder, componentIssuesRepository)));
    visitorsCrawler.visit(treeRootHolder.getRoot());

    assertNoAddedRawMeasureOnProjectViews();
    assertNoAddedRawMeasure(SUB_SUBVIEW_REF);
    assertNoAddedRawMeasure(VIEW_REF);
    assertNoAddedRawMeasure(ROOT_REF);
  }

  private void assertNoAddedRawMeasureOnProjectViews() {
    assertNoAddedRawMeasure(PROJECT_VIEW_1_REF);
    assertNoAddedRawMeasure(PROJECT_VIEW_2_REF);
  }

  private void addRawMeasure(int componentRef, String metricKey, int value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

  private void assertNoAddedRawMeasure(int componentRef) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).isEmpty();
  }

  private void assertAddedRawMeasure(int value, int componentRef, String metricKey) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).containsOnly(entryOf(metricKey, newMeasureBuilder().create(value)));
  }

}

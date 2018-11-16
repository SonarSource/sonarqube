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
package org.sonar.ce.task.projectanalysis.measure;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.ce.task.projectanalysis.api.measurecomputer.MeasureComputerDefinitionImpl;
import org.sonar.ce.task.projectanalysis.api.measurecomputer.MeasureComputerWrapper;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class ReportMeasureComputersVisitorTest {

  private static final String NEW_METRIC_KEY = "new_metric_key";
  private static final String NEW_METRIC_NAME = "new metric name";

  private static final org.sonar.api.measures.Metric<Integer> NEW_METRIC = new org.sonar.api.measures.Metric.Builder(NEW_METRIC_KEY, NEW_METRIC_NAME,
    org.sonar.api.measures.Metric.ValueType.INT)
    .create();

  private static final int ROOT_REF = 1;
  private static final int DIRECTORY_REF = 123;
  private static final int FILE_1_REF = 1231;
  private static final int FILE_2_REF = 1232;

  private static final Component ROOT = builder(PROJECT, ROOT_REF).setKey("project")
    .addChildren(
      builder(DIRECTORY, DIRECTORY_REF).setKey("directory")
        .addChildren(
          builder(FILE, FILE_1_REF).setKey("file1").build(),
          builder(FILE, FILE_2_REF).setKey("file2").build())
        .build())
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(ROOT);

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NCLOC)
    .add(COMMENT_LINES)
    .add(NEW_METRIC);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(ROOT, metricRepository);

  ComponentIssuesRepository componentIssuesRepository = mock(ComponentIssuesRepository.class);

  MeasureComputersHolderImpl measureComputersHolder = new MeasureComputersHolderImpl();

  @Test
  public void compute_plugin_measure() {
    addRawMeasure(FILE_1_REF, NCLOC_KEY, 10);
    addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, 2);
    addRawMeasure(FILE_2_REF, NCLOC_KEY, 40);
    addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, 5);
    addRawMeasure(DIRECTORY_REF, NCLOC_KEY, 50);
    addRawMeasure(DIRECTORY_REF, COMMENT_LINES_KEY, 7);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 50);
    addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, 7);

    final MeasureComputer.MeasureComputerDefinition definition = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics(NCLOC_KEY, COMMENT_LINES_KEY)
      .setOutputMetrics(NEW_METRIC_KEY)
      .build();
    measureComputersHolder.setMeasureComputers(newArrayList(
      new MeasureComputerWrapper(
        new MeasureComputer() {
          @Override
          public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
            return definition;
          }

          @Override
          public void compute(MeasureComputerContext context) {
            org.sonar.api.ce.measure.Measure ncloc = context.getMeasure(NCLOC_KEY);
            org.sonar.api.ce.measure.Measure comment = context.getMeasure(COMMENT_LINES_KEY);
            if (ncloc != null && comment != null) {
              context.addMeasure(NEW_METRIC_KEY, ncloc.getIntValue() + comment.getIntValue());
            }
          }
        },
        definition)));

    VisitorsCrawler visitorsCrawler = new VisitorsCrawler(Arrays.asList(new MeasureComputersVisitor(metricRepository, measureRepository, null,
      measureComputersHolder, componentIssuesRepository)));
    visitorsCrawler.visit(ROOT);

    assertAddedRawMeasure(12, FILE_1_REF, NEW_METRIC_KEY);
    assertAddedRawMeasure(45, FILE_2_REF, NEW_METRIC_KEY);
    assertAddedRawMeasure(57, DIRECTORY_REF, NEW_METRIC_KEY);
    assertAddedRawMeasure(57, ROOT_REF, NEW_METRIC_KEY);
  }

  @Test
  public void nothing_to_compute() {
    addRawMeasure(FILE_1_REF, NCLOC_KEY, 10);
    addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, 2);
    addRawMeasure(FILE_2_REF, NCLOC_KEY, 40);
    addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, 5);
    addRawMeasure(DIRECTORY_REF, NCLOC_KEY, 50);
    addRawMeasure(DIRECTORY_REF, COMMENT_LINES_KEY, 7);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 50);
    addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, 7);

    measureComputersHolder.setMeasureComputers(Collections.emptyList());
    VisitorsCrawler visitorsCrawler = new VisitorsCrawler(Arrays.asList(new MeasureComputersVisitor(metricRepository, measureRepository, null,
      measureComputersHolder, componentIssuesRepository)));
    visitorsCrawler.visit(ROOT);

    assertNoAddedRawMeasure(FILE_1_REF);
    assertNoAddedRawMeasure(FILE_2_REF);
    assertNoAddedRawMeasure(DIRECTORY_REF);
    assertNoAddedRawMeasure(ROOT_REF);
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

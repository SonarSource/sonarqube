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

package org.sonar.server.computation.measure;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.VisitorsCrawler;
import org.sonar.server.computation.issue.ComponentIssuesRepository;
import org.sonar.server.computation.measure.api.MeasureComputerImpl;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.ReportComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;

public class MeasureComputersVisitorTest {

  private static final String NEW_METRIC_KEY = "new_metric_key";
  private static final String NEW_METRIC_NAME = "new metric name";

  private static final org.sonar.api.measures.Metric<Integer> NEW_METRIC = new org.sonar.api.measures.Metric.Builder(NEW_METRIC_KEY, NEW_METRIC_NAME,
    org.sonar.api.measures.Metric.ValueType.INT)
    .create();

  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 12;
  private static final int DIRECTORY_REF = 123;
  private static final int FILE_1_REF = 1231;
  private static final int FILE_2_REF = 1232;

  private static final Component ROOT = builder(PROJECT, ROOT_REF).setKey("project")
    .addChildren(
      builder(MODULE, MODULE_REF).setKey("module")
        .addChildren(
          builder(DIRECTORY, DIRECTORY_REF).setKey("directory")
            .addChildren(
              builder(FILE, FILE_1_REF).setKey("file1").build(),
              builder(FILE, FILE_2_REF).setKey("file2").build()
            ).build()
        ).build()
    ).build();

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
  public void compute_plugin_measure() throws Exception {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(2));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(40));
    measureRepository.addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(5));
    measureRepository.addRawMeasure(DIRECTORY_REF, NCLOC_KEY, newMeasureBuilder().create(50));
    measureRepository.addRawMeasure(DIRECTORY_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(7));
    measureRepository.addRawMeasure(MODULE_REF, NCLOC_KEY, newMeasureBuilder().create(50));
    measureRepository.addRawMeasure(MODULE_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(7));
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(50));
    measureRepository.addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(7));

    measureComputersHolder.setMeasureComputers(newArrayList(
      new MeasureComputerImpl.MeasureComputerBuilderImpl()
        .setInputMetrics(NCLOC_KEY, COMMENT_LINES_KEY)
        .setOutputMetrics(NEW_METRIC_KEY)
        .setImplementation(
          new MeasureComputer.Implementation() {
            @Override
            public void compute(Context ctx) {
              org.sonar.api.ce.measure.Measure ncloc = ctx.getMeasure(NCLOC_KEY);
              org.sonar.api.ce.measure.Measure comment = ctx.getMeasure(COMMENT_LINES_KEY);
              if (ncloc != null && comment != null) {
                ctx.addMeasure(NEW_METRIC_KEY, ncloc.getIntValue() + comment.getIntValue());
              }
            }
          }
        )
        .build()
      ));

    VisitorsCrawler visitorsCrawler = new VisitorsCrawler(Arrays.<ComponentVisitor>asList(new MeasureComputersVisitor(metricRepository, measureRepository, null, measureComputersHolder, componentIssuesRepository)));
    visitorsCrawler.visit(ROOT);

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_1_REF))).containsOnly(entryOf(NEW_METRIC_KEY, newMeasureBuilder().create(12)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_2_REF))).containsOnly(entryOf(NEW_METRIC_KEY, newMeasureBuilder().create(45)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).containsOnly(entryOf(NEW_METRIC_KEY, newMeasureBuilder().create(57)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).containsOnly(entryOf(NEW_METRIC_KEY, newMeasureBuilder().create(57)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).containsOnly(entryOf(NEW_METRIC_KEY, newMeasureBuilder().create(57)));
  }

  @Test
  public void nothing_to_compute() throws Exception {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(2));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(40));
    measureRepository.addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(5));
    measureRepository.addRawMeasure(DIRECTORY_REF, NCLOC_KEY, newMeasureBuilder().create(50));
    measureRepository.addRawMeasure(DIRECTORY_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(7));
    measureRepository.addRawMeasure(MODULE_REF, NCLOC_KEY, newMeasureBuilder().create(50));
    measureRepository.addRawMeasure(MODULE_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(7));
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(50));
    measureRepository.addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(7));

    measureComputersHolder.setMeasureComputers(Collections.<MeasureComputer>emptyList());
    VisitorsCrawler visitorsCrawler = new VisitorsCrawler(Arrays.<ComponentVisitor>asList(new MeasureComputersVisitor(metricRepository, measureRepository, null, measureComputersHolder, componentIssuesRepository)));
    visitorsCrawler.visit(ROOT);

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_1_REF))).isEmpty();
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_2_REF))).isEmpty();
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).isEmpty();
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).isEmpty();
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).isEmpty();
  }

}

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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.MetricDefinition;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.DumbComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;

public class ComputePluginMeasuresStepTest {

  private static final String NEW_METRIC_KEY = "new_metric_key";
  private static final String NEW_METRIC_NAME = "new metric name";

  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 12;
  private static final int DIRECTORY_REF = 123;
  private static final int FILE_1_REF = 1231;
  private static final int FILE_2_REF = 1232;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(1, NCLOC)
    .add(2, COMMENT_LINES)
    .add(new MetricImpl(3, NEW_METRIC_KEY, NEW_METRIC_NAME, Metric.MetricType.INT));

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, MODULE_REF)
            .addChildren(
              builder(DIRECTORY, DIRECTORY_REF)
                .addChildren(
                  builder(FILE, FILE_1_REF).build(),
                  builder(FILE, FILE_2_REF).build()
                ).build()
            ).build()
        ).build());
  }

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

    MetricDefinition[] metricDefinitions = new MetricDefinition[]{new NewMetricDefinition()};
    ComputationStep underTest = new ComputePluginMeasuresStep(treeRootHolder, metricRepository, measureRepository, metricDefinitions, null);
    underTest.execute();

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_1_REF))).containsOnly(entryOf(NEW_METRIC_KEY, newMeasureBuilder().create(12)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_2_REF))).containsOnly(entryOf(NEW_METRIC_KEY, newMeasureBuilder().create(45)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).containsOnly(entryOf(NEW_METRIC_KEY, newMeasureBuilder().create(57)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).containsOnly(entryOf(NEW_METRIC_KEY, newMeasureBuilder().create(57)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).containsOnly(entryOf(NEW_METRIC_KEY, newMeasureBuilder().create(57)));
  }

  public class NewMetricDefinition implements MetricDefinition {
    @Override
    public void define(NewMetricContext newMetricContext) {
      newMetricContext.createNewMetric()
        .setKey(NEW_METRIC_KEY)
        .setName(NEW_METRIC_NAME)
        .createMeasureComputer()
        .setInputMetricKeys(NCLOC_KEY, COMMENT_LINES_KEY)
        .setMeasureComputer(new MetricDefinition.MeasureComputer() {
          @Override
          public void compute(MetricDefinition.MeasureComputerContext context) {
            Measure ncloc = context.getMeasure(NCLOC_KEY);
            Measure comment = context.getMeasure(COMMENT_LINES_KEY);
            if (ncloc != null && comment != null) {
              context.saveMeasure(ncloc.getIntValue() + comment.getIntValue());
            }
          }
        })
        .done()
        .done();
    }
  }
}

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
import org.junit.rules.TemporaryFolder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.design.FileDependenciesCache;
import org.sonar.server.computation.design.FileDependency;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasuresCache;
import org.sonar.server.design.db.DsmDataEncoder;
import org.sonar.server.design.db.DsmDb;
import org.sonar.server.measure.InternalMetrics;
import org.sonar.server.util.CloseableIterator;

import javax.annotation.CheckForNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ComputeFileDependenciesStepTest extends BaseStepTest {

  static final String PROJECT_UUID = "PROJECT";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  BatchReportWriter writer;

  ComputationContext context;

  ComputeFileDependenciesStep sut;

  FileDependenciesCache fileDependenciesCache;
  MeasuresCache measuresCache;

  @Before
  public void setup() throws Exception {
    File reportDir = temp.newFolder();
    writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    context = new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID));

    fileDependenciesCache = new FileDependenciesCache(temp.newFolder("fileDependencies"), System2.INSTANCE);
    measuresCache = new MeasuresCache(temp.newFolder("measures"), System2.INSTANCE);
    sut = new ComputeFileDependenciesStep(fileDependenciesCache, measuresCache);
  }

  @Override
  protected ComputationStep step() {
    return new ComputeFileDependenciesStep(fileDependenciesCache, measuresCache);
  }

  @Test
  public void feed_file_dependencies_cache() throws Exception {
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid(PROJECT_UUID)
      .addChildRef(2)
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_A")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_A")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_B")
      .addChildRef(5)
      .addChildRef(6)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(5)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_B")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(6)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_C")
      .build());

    writer.writeFileDependencies(3, Collections.singletonList(
      BatchReport.FileDependency.newBuilder()
        .setToFileRef(5)
        .setWeight(1)
        .build()
      ));
    writer.writeFileDependencies(3, Collections.singletonList(
      BatchReport.FileDependency.newBuilder()
        .setToFileRef(6)
        .setWeight(1)
        .build()
      ));

    sut.execute(context);

    // Dependencies on file FILE_A
    assertThat(getFileDependencies(3)).hasSize(2);
    List<FileDependency> fileDependencies = getFileDependencies(3);
    FileDependency fileDependency = fileDependencies.get(0);
    assertThat(fileDependency.getFrom()).isEqualTo(3);
    assertThat(fileDependency.getTo()).isEqualTo(5);
    assertThat(fileDependency.getWeight()).isEqualTo(1);

    fileDependency = fileDependencies.get(1);
    assertThat(fileDependency.getFrom()).isEqualTo(3);
    assertThat(fileDependency.getTo()).isEqualTo(6);
    assertThat(fileDependency.getWeight()).isEqualTo(1);

    // Dependency on directory DIRECTORY_A
    assertThat(getFileDependencies(2)).hasSize(1);
    FileDependency directorDependency = getFileDependencies(2).get(0);
    assertThat(directorDependency.getFrom()).isEqualTo(2);
    assertThat(directorDependency.getTo()).isEqualTo(4);
    assertThat(directorDependency.getWeight()).isEqualTo(2);

    // No dependency on project
    assertThat(getFileDependencies(1)).isEmpty();
  }

  @Test
  public void not_feed_file_dependencies_cache_when_dependencies_are_on_same_folder() throws Exception {
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid(PROJECT_UUID)
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_A")
      .addChildRef(3)
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_A")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_B")
      .build());

    writer.writeFileDependencies(3, Collections.singletonList(
      BatchReport.FileDependency.newBuilder()
        .setToFileRef(4)
        .setWeight(1)
        .build()
      ));

    sut.execute(context);

    // Dependencies on file FILE_A
    assertThat(getFileDependencies(3)).hasSize(1);
    List<FileDependency> fileDependencies = getFileDependencies(3);
    FileDependency fileDependency = fileDependencies.get(0);
    assertThat(fileDependency.getFrom()).isEqualTo(3);
    assertThat(fileDependency.getTo()).isEqualTo(4);
    assertThat(fileDependency.getWeight()).isEqualTo(1);

    // No dependency on directory DIRECTORY_A because children dependencies are in the same folder
    assertThat(getFileDependencies(2)).isEmpty();
  }

  @Test
  public void compute_dsm() throws Exception {
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid(PROJECT_UUID)
      .addChildRef(2)
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_A")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_A")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_B")
      .addChildRef(5)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(5)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_B")
      .build());

    writer.writeFileDependencies(3, Collections.singletonList(
      BatchReport.FileDependency.newBuilder()
        .setToFileRef(5)
        .setWeight(1)
        .build()
    ));

    sut.execute(context);

    // On project
    Measure projectMeasure = getMeasure(1, InternalMetrics.DEPENDENCY_MATRIX_KEY);
    assertThat(projectMeasure.getByteValue()).isNotNull();

    DsmDb.Data projectDsmData = DsmDataEncoder.decodeDsmData(projectMeasure.getByteValue());
    assertThat(projectDsmData.getUuid(0)).isEqualTo("DIRECTORY_A");
    assertThat(projectDsmData.getCellCount()).isEqualTo(1);
    assertThat(projectDsmData.getCell(0).getWeight()).isEqualTo(1);
    assertThat(projectDsmData.getCell(0).getOffset()).isEqualTo(2);
    assertThat(projectDsmData.getUuid(1)).isEqualTo("DIRECTORY_B");

    // On directory
    Measure directoryMeasure = getMeasure(2, InternalMetrics.DEPENDENCY_MATRIX_KEY);
    assertThat(directoryMeasure.getMetricKey()).isEqualTo(InternalMetrics.DEPENDENCY_MATRIX_KEY);
    assertThat(directoryMeasure.getByteValue()).isNotNull();

    DsmDb.Data directoryDsmData = DsmDataEncoder.decodeDsmData(directoryMeasure.getByteValue());
    assertThat(directoryDsmData.getUuid(0)).isEqualTo("FILE_A");
    assertThat(directoryDsmData.getCellCount()).isEqualTo(1);
    assertThat(directoryDsmData.getCell(0).getWeight()).isEqualTo(1);
    assertThat(directoryDsmData.getCell(0).getOffset()).isEqualTo(2);
    assertThat(directoryDsmData.getUuid(1)).isEqualTo("FILE_B");

    // Nothing on file
    assertThat(getMeasure(3, InternalMetrics.DEPENDENCY_MATRIX_KEY)).isNull();
    assertThat(getMeasure(5, InternalMetrics.DEPENDENCY_MATRIX_KEY)).isNull();
  }

  @Test
  public void compute_only_dsm_on_sub_project() throws Exception {
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid(PROJECT_UUID)
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setUuid("MODULE")
      .addChildRef(3)
      .addChildRef(5)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_A")
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_A")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(5)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_B")
      .addChildRef(6)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(6)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_B")
      .build());

    writer.writeFileDependencies(4, Collections.singletonList(
      BatchReport.FileDependency.newBuilder()
        .setToFileRef(6)
        .setWeight(1)
        .build()
    ));

    sut.execute(context);

    // Nothing on project
    assertThat(getMeasure(1, InternalMetrics.DEPENDENCY_MATRIX_KEY)).isNull();

    // On sub project
    Measure projectMeasure = getMeasure(2, InternalMetrics.DEPENDENCY_MATRIX_KEY);
    assertThat(projectMeasure.getMetricKey()).isEqualTo(InternalMetrics.DEPENDENCY_MATRIX_KEY);
    assertThat(projectMeasure.getByteValue()).isNotNull();
  }

  @Test
  public void not_compute_dsm_when_too_many_components() throws Exception {
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid(PROJECT_UUID)
      .addChildRef(2)
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_A")
      .setPath("DirectoryAPath")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_A")
      .build());

    BatchReport.Component.Builder directoryBuilder = BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_B");

    // Add dependencies on 205 components, as it's more than 200 no measure will be computed
    for (int i = 5; i < 210; i++) {
      writer.writeComponent(BatchReport.Component.newBuilder()
        .setRef(i)
        .setType(Constants.ComponentType.FILE)
        .setUuid("FILE_" + i)
        .build());
      directoryBuilder.addChildRef(i);

      writer.writeFileDependencies(3, Collections.singletonList(
        BatchReport.FileDependency.newBuilder()
          .setToFileRef(i)
          .setWeight(1)
          .build()
      ));
    }
    writer.writeComponent(directoryBuilder.build());

    sut.execute(context);

    // No measure
    assertThat(getMeasure(1, InternalMetrics.DEPENDENCY_MATRIX_KEY)).isNull();
    assertThat(getMeasure(2, InternalMetrics.DEPENDENCY_MATRIX_KEY)).isNull();
  }

  @Test
  public void compute_measures() throws Exception {
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid(PROJECT_UUID)
      .addChildRef(2)
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_A")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_A")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_B")
      .addChildRef(5)
      .addChildRef(6)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(5)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_B")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(6)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_C")
      .build());

    writer.writeFileDependencies(3, Collections.singletonList(
      BatchReport.FileDependency.newBuilder()
        .setToFileRef(5)
        .setWeight(2)
        .build()
    ));
    writer.writeFileDependencies(6, Collections.singletonList(
      BatchReport.FileDependency.newBuilder()
        .setToFileRef(3)
        .setWeight(3)
        .build()
    ));

    sut.execute(context);

    // On project
    assertThat(getMeasure(1, CoreMetrics.DIRECTORY_CYCLES_KEY).getValue()).isEqualTo(1d);
    assertThat(getMeasure(1, CoreMetrics.DIRECTORY_FEEDBACK_EDGES_KEY).getValue()).isEqualTo(1d);
    assertThat(getMeasure(1, CoreMetrics.DIRECTORY_TANGLES_KEY).getValue()).isEqualTo(1d);
    assertThat(getMeasure(1, CoreMetrics.DIRECTORY_EDGES_WEIGHT_KEY).getValue()).isEqualTo(2d);

    assertThat(getMeasure(1, CoreMetrics.FILE_CYCLES_KEY)).isNull();
    assertThat(getMeasure(1, CoreMetrics.FILE_FEEDBACK_EDGES_KEY)).isNull();
    assertThat(getMeasure(1, CoreMetrics.FILE_TANGLES_KEY)).isNull();
    assertThat(getMeasure(1, CoreMetrics.FILE_EDGES_WEIGHT_KEY)).isNull();

    // On directory
    assertThat(getMeasure(2, CoreMetrics.DIRECTORY_CYCLES_KEY)).isNull();
    assertThat(getMeasure(2, CoreMetrics.DIRECTORY_FEEDBACK_EDGES_KEY)).isNull();
    assertThat(getMeasure(2, CoreMetrics.DIRECTORY_TANGLES_KEY)).isNull();
    assertThat(getMeasure(2, CoreMetrics.DIRECTORY_EDGES_WEIGHT_KEY)).isNull();

    assertThat(getMeasure(2, CoreMetrics.FILE_CYCLES_KEY).getValue()).isEqualTo(0d);
    assertThat(getMeasure(2, CoreMetrics.FILE_FEEDBACK_EDGES_KEY).getValue()).isEqualTo(0d);
    assertThat(getMeasure(2, CoreMetrics.FILE_TANGLES_KEY).getValue()).isEqualTo(0d);
    assertThat(getMeasure(2, CoreMetrics.FILE_EDGES_WEIGHT_KEY).getValue()).isEqualTo(2d);

    // Nothing on file
    assertThat(getMeasure(3, CoreMetrics.DIRECTORY_CYCLES_KEY)).isNull();
    assertThat(getMeasure(3, CoreMetrics.DIRECTORY_FEEDBACK_EDGES_KEY)).isNull();
    assertThat(getMeasure(3, CoreMetrics.DIRECTORY_TANGLES_KEY)).isNull();
    assertThat(getMeasure(3, CoreMetrics.DIRECTORY_EDGES_WEIGHT_KEY)).isNull();

    assertThat(getMeasure(3, CoreMetrics.FILE_CYCLES_KEY)).isNull();
    assertThat(getMeasure(3, CoreMetrics.FILE_FEEDBACK_EDGES_KEY)).isNull();
    assertThat(getMeasure(3, CoreMetrics.FILE_TANGLES_KEY)).isNull();
    assertThat(getMeasure(3, CoreMetrics.FILE_EDGES_WEIGHT_KEY)).isNull();
  }

  @Test
  public void not_compute_measures_on_root_project_of_multi_modules_project() throws Exception {
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid(PROJECT_UUID)
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setUuid("MODULE")
      .addChildRef(3)
      .addChildRef(5)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_A")
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_A")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(5)
      .setType(Constants.ComponentType.DIRECTORY)
      .setUuid("DIRECTORY_B")
      .addChildRef(6)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(6)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_B")
      .build());

    writer.writeFileDependencies(4, Collections.singletonList(
      BatchReport.FileDependency.newBuilder()
        .setToFileRef(6)
        .setWeight(1)
        .build()
    ));

    sut.execute(context);

    // Nothing on project
    assertThat(getMeasure(1, CoreMetrics.DIRECTORY_CYCLES_KEY)).isNull();
    assertThat(getMeasure(1, CoreMetrics.DIRECTORY_FEEDBACK_EDGES_KEY)).isNull();
    assertThat(getMeasure(1, CoreMetrics.DIRECTORY_TANGLES_KEY)).isNull();
    assertThat(getMeasure(1, CoreMetrics.DIRECTORY_EDGES_WEIGHT_KEY)).isNull();

    assertThat(getMeasure(1, CoreMetrics.FILE_CYCLES_KEY)).isNull();
    assertThat(getMeasure(1, CoreMetrics.FILE_FEEDBACK_EDGES_KEY)).isNull();
    assertThat(getMeasure(1, CoreMetrics.FILE_TANGLES_KEY)).isNull();
    assertThat(getMeasure(1, CoreMetrics.FILE_EDGES_WEIGHT_KEY)).isNull();
  }

  @CheckForNull
  private Measure getMeasure(int componentRef, String metricKey) {
    try (CloseableIterator<Measure> traverse = measuresCache.traverse(componentRef)) {
      while (traverse.hasNext()) {
        Measure measure = traverse.next();
        if (measure.getMetricKey().equals(metricKey)) {
          return measure;
        }
      }
    }
    return null;
  }

  private List<FileDependency> getFileDependencies(int componentRef) {
    List<FileDependency> fileDependencies = new ArrayList<>();
    try (CloseableIterator<FileDependency> traverse = fileDependenciesCache.traverse(componentRef)) {
      while (traverse.hasNext()) {
        fileDependencies.add(traverse.next());
      }
    }
    return fileDependencies;
  }

}

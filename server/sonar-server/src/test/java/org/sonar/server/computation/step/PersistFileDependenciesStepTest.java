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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.design.FileDependencyDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.server.design.db.FileDependencyDao;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistFileDependenciesStepTest extends BaseStepTest {

  static final String PROJECT_UUID = "PROJECT";
  static final long PROJECT_SNAPSHOT_ID = 10L;
  static final long now = 123456789L;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static DbTester dbTester = new DbTester();

  DbClient dbClient;

  System2 system2 = mock(System2.class);

  DbSession session;

  BatchReportWriter writer;

  ComputationContext context;

  PersistFileDependenciesStep sut;

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new FileDependencyDao());

    system2 = mock(System2.class);
    when(system2.now()).thenReturn(now);

    File reportDir = temp.newFolder();
    writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setSnapshotId(PROJECT_SNAPSHOT_ID)
      .build());
    context = new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID));

    sut = new PersistFileDependenciesStep(dbClient, system2);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Override
  protected ComputationStep step() throws IOException {
    return new PersistFileDependenciesStep(dbClient, system2);
  }

  @Test
  public void supported_project_qualifiers() throws Exception {
    assertThat(step().supportedProjectQualifiers()).containsOnly(Qualifiers.PROJECT);
  }

  @Test
  public void persist_file_dependencies() throws Exception {
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

    List<FileDependencyDto> dtos = dbClient.fileDependencyDao().selectAll(session);
    assertThat(dtos).hasSize(1);

    FileDependencyDto dto = dtos.get(0);
    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getFromComponentUuid()).isEqualTo("FILE_A");
    assertThat(dto.getFromParentUuid()).isEqualTo("DIRECTORY_A");
    assertThat(dto.getToComponentUuid()).isEqualTo("FILE_B");
    assertThat(dto.getToParentUuid()).isEqualTo("DIRECTORY_B");
    assertThat(dto.getRootProjectSnapshotId()).isEqualTo(PROJECT_SNAPSHOT_ID);
    assertThat(dto.getWeight()).isEqualTo(1);
    assertThat(dto.getCreatedAt()).isEqualTo(now);
  }

  @Test
  public void nothing_to_persist() throws Exception {
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
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_A")
      .build());

    sut.execute(context);

    assertThat(dbClient.fileDependencyDao().selectAll(session)).isEmpty();
  }
}

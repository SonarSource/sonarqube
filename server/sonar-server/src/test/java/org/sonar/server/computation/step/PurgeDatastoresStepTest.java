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

import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.config.Settings;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.computation.dbcleaner.ProjectCleaner;
import org.sonar.db.DbSession;
import org.sonar.db.purge.IdUuidPair;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.component.ProjectSettingsRepository;
import org.sonar.server.db.DbClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PurgeDatastoresStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  DbIdsRepository dbIdsRepository = new DbIdsRepository();

  ProjectCleaner projectCleaner = mock(ProjectCleaner.class);
  ProjectSettingsRepository projectSettingsRepository = mock(ProjectSettingsRepository.class);

  PurgeDatastoresStep sut = new PurgeDatastoresStep(mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS), projectCleaner, dbIdsRepository, treeRootHolder, projectSettingsRepository);

  @Before
  public void setUp() throws Exception {
    when(projectSettingsRepository.getProjectSettings(PROJECT_KEY)).thenReturn(new Settings());
  }

  @Test
  public void call_purge_method_of_the_purge_task() throws IOException {
    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("UUID-1234").setKey(PROJECT_KEY).build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setComponentId(project, 123L);

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    sut.execute();

    ArgumentCaptor<IdUuidPair> argumentCaptor = ArgumentCaptor.forClass(IdUuidPair.class);
    verify(projectCleaner).purge(any(DbSession.class), argumentCaptor.capture(), any(Settings.class));
    assertThat(argumentCaptor.getValue().getId()).isEqualTo(123L);
    assertThat(argumentCaptor.getValue().getUuid()).isEqualTo("UUID-1234");
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }
}

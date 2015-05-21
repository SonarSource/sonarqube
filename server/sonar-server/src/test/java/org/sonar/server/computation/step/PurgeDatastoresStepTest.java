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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.config.Settings;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.dbcleaner.ProjectCleaner;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.purge.IdUuidPair;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.db.DbClient;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PurgeDatastoresStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  ProjectCleaner projectCleaner = mock(ProjectCleaner.class);
  DbComponentsRefCache dbComponentsRefCache = new DbComponentsRefCache();
  PurgeDatastoresStep sut = new PurgeDatastoresStep(mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS), projectCleaner, dbComponentsRefCache);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void call_purge_method_of_the_purge_task() throws IOException {
    dbComponentsRefCache.addComponent(1, new DbComponentsRefCache.DbComponent(123L, PROJECT_KEY, "UUID-1234"));

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    ComponentDto project = mock(ComponentDto.class);
    ComputationContext context = new ComputationContext(new BatchReportReader(reportDir), PROJECT_KEY, new Settings());

    sut.execute(context);

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

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
import org.mockito.Mockito;
import org.sonar.api.config.Settings;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.dbcleaner.ProjectCleaner;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.purge.IdUuidPair;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PurgeDatastoresStepTest extends BaseStepTest {

  ProjectCleaner projectCleaner = mock(ProjectCleaner.class);;
  PurgeDatastoresStep sut = new PurgeDatastoresStep(mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS), projectCleaner);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void call_purge_method_of_the_purge_task() throws IOException {
    ComponentDto project = mock(ComponentDto.class);
    when(project.getId()).thenReturn(123L);
    when(project.uuid()).thenReturn("UUID-1234");
    ComputationContext context = new ComputationContext(mock(BatchReportReader.class), project);

    sut.execute(context);

    verify(projectCleaner).purge(any(DbSession.class), any(IdUuidPair.class), any(Settings.class));
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }
}

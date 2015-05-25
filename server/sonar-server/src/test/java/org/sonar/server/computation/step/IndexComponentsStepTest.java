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
import org.sonar.api.config.Settings;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.resource.ResourceIndexerDao;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.batch.FileBatchReportReader;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.component.DbComponentsRefCache.DbComponent;
import org.sonar.server.computation.component.ComponentTreeBuilders;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.db.DbClient;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexComponentsStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  ResourceIndexerDao resourceIndexerDao = mock(ResourceIndexerDao.class);
  DbComponentsRefCache dbComponentsRefCache = new DbComponentsRefCache();
  IndexComponentsStep sut = new IndexComponentsStep(resourceIndexerDao, dbComponentsRefCache);

  @Test
  public void call_indexProject_of_dao() throws IOException {
    dbComponentsRefCache.addComponent(1, new DbComponent(123L, PROJECT_KEY, "PROJECT_UUID"));

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    ComponentDto project = mock(ComponentDto.class);
    when(project.getId()).thenReturn(123L);
    ComputationContext context = new ComputationContext(new FileBatchReportReader(new BatchReportReader(reportDir)), PROJECT_KEY, new Settings(), mock(DbClient.class), ComponentTreeBuilders.from(DumbComponent.DUMB_PROJECT), mock(LanguageRepository.class));

    sut.execute(context);

    verify(resourceIndexerDao).indexProject(123L);
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }
}

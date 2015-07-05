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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.component.ResourceIndexDao;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DumbComponent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class IndexComponentsStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  ResourceIndexDao resourceIndexDao = mock(ResourceIndexDao.class);
  DbIdsRepository dbIdsRepository = new DbIdsRepository();
  IndexComponentsStep sut = new IndexComponentsStep(resourceIndexDao, dbIdsRepository, treeRootHolder);

  @Test
  public void call_indexProject_of_dao() throws IOException {
    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("PROJECT_UUID").setKey(PROJECT_KEY).build();
    dbIdsRepository.setComponentId(project, 123L);
    treeRootHolder.setRoot(project);

    sut.execute();

    verify(resourceIndexDao).indexProject(123L);
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }
}

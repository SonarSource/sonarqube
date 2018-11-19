/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.BranchPersister;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.MutableDbIdsRepository;
import org.sonar.server.computation.task.projectanalysis.component.MutableDisabledComponentsHolder;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class PersistComponentsStepTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_fail_if_project_is_not_stored_in_database_yet() {
    TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);
    Component component = mock(Component.class);
    DbClient dbClient = mock(DbClient.class);
    ComponentDao componentDao = mock(ComponentDao.class);
    String projectKey = randomAlphabetic(20);

    doReturn(component).when(treeRootHolder).getRoot();
    doReturn(projectKey).when(component).getKey();
    doReturn(componentDao).when(dbClient).componentDao();
    doReturn(emptyList()).when(componentDao).selectAllComponentsFromProjectKey(any(DbSession.class), eq(projectKey));

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The project '" + projectKey + "' is not stored in the database, during a project analysis");

    new PersistComponentsStep(
      dbClient,
      treeRootHolder,
      mock(MutableDbIdsRepository.class),
      System2.INSTANCE,
      mock(MutableDisabledComponentsHolder.class),
      mock(AnalysisMetadataHolder.class),
      mock(BranchPersister.class)).execute();
  }
}

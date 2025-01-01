/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.component.BranchPersister;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.MutableDisabledComponentsHolder;
import org.sonar.ce.task.projectanalysis.dependency.ProjectDependenciesHolder;
import org.sonar.ce.task.projectanalysis.component.ProjectPersister;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class PersistComponentsStepTest {

  @Test
  public void should_fail_if_project_is_not_stored_in_database_yet() {
    TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);
    Component component = mock(Component.class);
    DbClient dbClient = mock(DbClient.class);
    ComponentDao componentDao = mock(ComponentDao.class);
    String projectKey = secure().nextAlphabetic(20);

    doReturn(component).when(treeRootHolder).getRoot();
    doReturn(projectKey).when(component).getKey();
    doReturn(componentDao).when(dbClient).componentDao();
    doReturn(emptyList()).when(componentDao).selectByBranchUuid(eq(projectKey), any(DbSession.class));

    var underTest = new PersistComponentsStep(
      dbClient,
      treeRootHolder,
      System2.INSTANCE,
      mock(MutableDisabledComponentsHolder.class),
      mock(BranchPersister.class),
      mock(ProjectPersister.class),
      mock(ProjectDependenciesHolder.class));

    var context = new TestComputationStepContext();
    assertThatThrownBy(() -> underTest.execute(context))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("The project '" + projectKey + "' is not stored in the database, during a project analysis");
  }
}

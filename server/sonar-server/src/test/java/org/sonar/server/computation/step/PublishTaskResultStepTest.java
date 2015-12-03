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
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.taskprocessor.MutableTaskResultHolder;
import org.sonar.server.computation.taskprocessor.MutableTaskResultHolderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PublishTaskResultStepTest {
  private static final Component ROOT_COMPONENT = mock(Component.class);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
      .setRoot(ROOT_COMPONENT);

  private DbIdsRepository dbIdsRepository = mock(DbIdsRepository.class);
  private MutableTaskResultHolder taskResultHolder = new MutableTaskResultHolderImpl();

  private PublishTaskResultStep underTest = new PublishTaskResultStep(taskResultHolder, treeRootHolder, dbIdsRepository);

  @Test
  public void verify_getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Publish task results");
  }

  @Test
  public void execute_populate_TaskResultHolder_with_a_TaskResult_with_snapshot_id_of_the_root_taken_from_DbIdsRepository() {
    long snapshotId = 1233L;

    when(dbIdsRepository.getSnapshotId(ROOT_COMPONENT)).thenReturn(snapshotId);

    underTest.execute();

    assertThat(taskResultHolder.getResult().getSnapshotId()).isEqualTo(snapshotId);
  }
}

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
package org.sonar.server.ce.projectdump;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeQueueImpl;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.platform.NodeInformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

public class ExportSubmitterImplTest {

  private static final String SOME_SUBMITTER_UUID = "some submitter uuid";

  private final System2 system2 = System2.INSTANCE;
  @Rule
  public DbTester db = DbTester.create(system2);

  private final DbClient dbClient = db.getDbClient();
  private final CeQueue ceQueue = new CeQueueImpl(system2, db.getDbClient(), UuidFactoryFast.getInstance(), mock(NodeInformation.class));

  private final ExportSubmitterImpl underTest = new ExportSubmitterImpl(ceQueue, dbClient);

  @Test
  public void submitProjectExport_fails_with_NPE_if_project_key_is_null() {
    assertThatThrownBy(() -> underTest.submitProjectExport(null, SOME_SUBMITTER_UUID))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Project key can not be null");
  }

  @Test
  public void submitProjectExport_fails_with_IAE_if_project_with_specified_key_does_not_exist() {
    assertThatThrownBy(() -> underTest.submitProjectExport("blabalble", SOME_SUBMITTER_UUID))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Project with key [blabalble] does not exist");
  }

  @Test
  public void submitProjectExport_submits_task_with_project_uuid_and_submitterLogin_if_present() {
    ComponentDto projectDto = db.components().insertPrivateProject();

    underTest.submitProjectExport(projectDto.getKey(), SOME_SUBMITTER_UUID);

    assertThat(dbClient.ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getComponentUuid, CeQueueDto::getTaskType, CeQueueDto::getSubmitterUuid)
      .containsExactlyInAnyOrder(tuple(projectDto.uuid(), "PROJECT_EXPORT", SOME_SUBMITTER_UUID));
  }

  @Test
  public void submitProjectExport_submits_task_with_project_uuid_and_no_submitterLogin_if_null() {
    ComponentDto projectDto = db.components().insertPrivateProject();

    underTest.submitProjectExport(projectDto.getKey(), null);

    assertThat(dbClient.ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getComponentUuid, CeQueueDto::getTaskType, CeQueueDto::getSubmitterUuid)
      .containsExactlyInAnyOrder(tuple(projectDto.uuid(), "PROJECT_EXPORT", null));
  }

}

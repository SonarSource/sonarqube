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
package org.sonar.server.ce.queue;

import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.System2;
import org.sonar.ce.queue.CeQueue;
import org.sonar.db.DbInputStream;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskInputDao;
import org.sonar.db.ce.CeTaskTypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CeQueueCleanerIT {

  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final ServerUpgradeStatus serverUpgradeStatus = mock(ServerUpgradeStatus.class);
  private final CeQueue queue = mock(CeQueue.class);

  @Test
  public void start_does_not_reset_in_progress_tasks_to_pending() {
    insertInQueue("TASK_1", CeQueueDto.Status.PENDING);
    insertInQueue("TASK_2", CeQueueDto.Status.IN_PROGRESS);

    runCleaner();

    assertThat(dbTester.getDbClient().ceQueueDao().countByStatus(dbTester.getSession(), CeQueueDto.Status.PENDING)).isOne();
    assertThat(dbTester.getDbClient().ceQueueDao().countByStatus(dbTester.getSession(), CeQueueDto.Status.IN_PROGRESS)).isOne();
  }

  @Test
  public void start_clears_queue_if_version_upgrade() {
    when(serverUpgradeStatus.isUpgraded()).thenReturn(true);

    runCleaner();

    verify(queue).clear();
  }

  @Test
  public void start_deletes_orphan_report_files() {
    // analysis reports are persisted but the associated
    // task is not in the queue
    insertInQueue("TASK_1", CeQueueDto.Status.PENDING);
    insertTaskData("TASK_1");
    insertTaskData("TASK_2");

    runCleaner();

    CeTaskInputDao dataDao = dbTester.getDbClient().ceTaskInputDao();
    Optional<DbInputStream> task1Data = dataDao.selectData(dbTester.getSession(), "TASK_1");
    assertThat(task1Data).isPresent();
    task1Data.get().close();

    assertThat(dataDao.selectData(dbTester.getSession(), "TASK_2")).isNotPresent();
  }

  private CeQueueDto insertInQueue(String taskUuid, CeQueueDto.Status status) {
    CeQueueDto dto = new CeQueueDto();
    dto.setTaskType(CeTaskTypes.REPORT);
    dto.setComponentUuid("PROJECT_1");
    dto.setUuid(taskUuid);
    dto.setStatus(status);
    dbTester.getDbClient().ceQueueDao().insert(dbTester.getSession(), dto);
    dbTester.getSession().commit();
    return dto;
  }

  private void insertTaskData(String taskUuid) {
    dbTester.getDbClient().ceTaskInputDao().insert(dbTester.getSession(), taskUuid, IOUtils.toInputStream("{binary}"));
    dbTester.getSession().commit();
  }

  private void runCleaner() {
    CeQueueCleaner cleaner = new CeQueueCleaner(dbTester.getDbClient(), serverUpgradeStatus, queue);
    cleaner.start();
  }
}

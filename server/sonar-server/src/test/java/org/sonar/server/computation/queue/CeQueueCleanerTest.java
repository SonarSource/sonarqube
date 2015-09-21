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
package org.sonar.server.computation.queue;

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.ReportFiles;
import org.sonar.server.computation.queue.CeQueueCleaner;
import org.sonar.server.computation.queue.CeQueueImpl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CeQueueCleanerTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  ServerUpgradeStatus serverUpgradeStatus = mock(ServerUpgradeStatus.class);
  ReportFiles reportFiles = mock(ReportFiles.class, Mockito.RETURNS_DEEP_STUBS);
  CeQueueImpl queue = mock(CeQueueImpl.class);
  CeQueueCleaner underTest = new CeQueueCleaner(dbTester.getDbClient(), serverUpgradeStatus, reportFiles, queue);

  @Test
  public void reset_in_progress_tasks_to_pending() throws IOException {
    insertInQueue("TASK_1", CeQueueDto.Status.PENDING);
    insertInQueue("TASK_2", CeQueueDto.Status.IN_PROGRESS);

    underTest.clean(dbTester.getSession());

    assertThat(dbTester.getDbClient().ceQueueDao().countByStatus(dbTester.getSession(), CeQueueDto.Status.PENDING)).isEqualTo(2);
    assertThat(dbTester.getDbClient().ceQueueDao().countByStatus(dbTester.getSession(), CeQueueDto.Status.IN_PROGRESS)).isEqualTo(0);
  }

  @Test
  public void clear_queue_if_version_upgrade() {
    when(serverUpgradeStatus.isUpgraded()).thenReturn(true);

    underTest.clean(dbTester.getSession());

    verify(queue).clear();
  }

  @Test
  public void cancel_task_if_report_file_is_missing() throws IOException {
    CeQueueDto task = insertInQueue("TASK_1", CeQueueDto.Status.PENDING, false);

    underTest.clean(dbTester.getSession());

    verify(queue).cancel(any(DbSession.class), eq(task));
  }

  @Test
  public void delete_orphan_report_files() throws Exception {
    // two files on disk but on task in queue
    insertInQueue("TASK_1", CeQueueDto.Status.PENDING, true);
    when(reportFiles.listUuids()).thenReturn(asList("TASK_1", "TASK_2"));

    underTest.clean(dbTester.getSession());

    verify(reportFiles).deleteIfExists("TASK_2");
  }

  private void insertInQueue(String taskUuid, CeQueueDto.Status status) throws IOException {
    insertInQueue(taskUuid, status, true);
  }

  private CeQueueDto insertInQueue(String taskUuid, CeQueueDto.Status status, boolean createFile) throws IOException {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid("PROJECT_1");
    queueDto.setUuid(taskUuid);
    queueDto.setStatus(status);
    dbTester.getDbClient().ceQueueDao().insert(dbTester.getSession(), queueDto);
    dbTester.getSession().commit();

    File file = tempFolder.newFile();
    when(reportFiles.fileForUuid(taskUuid)).thenReturn(file);
    if (!createFile) {
      file.delete();
    }
    return queueDto;
  }
}

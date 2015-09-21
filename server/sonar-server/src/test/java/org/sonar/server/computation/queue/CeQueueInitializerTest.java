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
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.ReportFiles;
import org.sonar.server.computation.monitoring.CEQueueStatus;
import org.sonar.server.computation.monitoring.CEQueueStatusImpl;
import org.sonar.server.computation.queue.CeProcessingScheduler;
import org.sonar.server.computation.queue.CeQueueCleaner;
import org.sonar.server.computation.queue.CeQueueInitializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CeQueueInitializerTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  ReportFiles reportFiles = mock(ReportFiles.class, Mockito.RETURNS_DEEP_STUBS);
  CEQueueStatus queueStatus = new CEQueueStatusImpl();
  CeQueueCleaner cleaner = mock(CeQueueCleaner.class);
  CeProcessingScheduler scheduler = mock(CeProcessingScheduler.class);
  CeQueueInitializer underTest = new CeQueueInitializer(dbTester.getDbClient(), queueStatus, cleaner, scheduler);

  @Test
  public void init_jmx_counters() throws IOException {
    insertInQueue("TASK_1", CeQueueDto.Status.PENDING);
    insertInQueue("TASK_2", CeQueueDto.Status.PENDING);
    // this in-progress task is going to be moved to PENDING
    insertInQueue("TASK_3", CeQueueDto.Status.IN_PROGRESS);

    underTest.start();

    assertThat(queueStatus.getPendingCount()).isEqualTo(3);
  }

  @Test
  public void init_jmx_counters_when_queue_is_empty() {
    underTest.start();

    assertThat(queueStatus.getPendingCount()).isEqualTo(0);
  }

  @Test
  public void clean_queue_then_start_scheduler_of_workers() throws IOException {
    InOrder inOrder = Mockito.inOrder(cleaner, scheduler);

    underTest.start();

    inOrder.verify(cleaner).clean(any(DbSession.class));
    inOrder.verify(scheduler).startScheduling();
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

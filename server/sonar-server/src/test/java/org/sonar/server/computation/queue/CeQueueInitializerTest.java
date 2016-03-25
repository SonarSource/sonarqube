/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.queue;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.computation.taskprocessor.CeProcessingScheduler;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyZeroInteractions;

public class CeQueueInitializerTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  Server server = mock(Server.class);
  CeQueueCleaner cleaner = mock(CeQueueCleaner.class);
  CeProcessingScheduler scheduler = mock(CeProcessingScheduler.class);
  CeQueueInitializer underTest = new CeQueueInitializer(dbTester.getDbClient(), cleaner, scheduler);

  @Test
  public void clean_queue_then_start_scheduler_of_workers() throws IOException {
    InOrder inOrder = Mockito.inOrder(cleaner, scheduler);

    underTest.onServerStart(server);

    inOrder.verify(cleaner).clean(any(DbSession.class));
    inOrder.verify(scheduler).startScheduling();
  }

  @Test
  public void onServerStart_has_no_effect_if_called_twice_to_support_medium_test_doing_startup_tasks_multiple_times() {

    underTest.onServerStart(server);

    reset(cleaner, scheduler);

    underTest.onServerStart(server);

    verifyZeroInteractions(cleaner, scheduler);

  }
}

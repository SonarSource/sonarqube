/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.queue;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.purge.PurgeProfiler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PurgeCeActivitiesTest {

  private DbClient dbClient = mock(DbClient.class);
  private PurgeDao purgeDao = mock(PurgeDao.class);
  private DbSession dbSession = mock(DbSession.class);
  private PurgeProfiler profiler = mock(PurgeProfiler.class);
  private PurgeCeActivities underTest = new PurgeCeActivities(dbClient, profiler);

  @Test
  public void starts_calls_purgeDao_and_commit() {
    when(dbClient.purgeDao()).thenReturn(purgeDao);
    when(dbClient.openSession(false)).thenReturn(dbSession);

    underTest.start();

    InOrder inOrder = Mockito.inOrder(purgeDao, dbSession);
    inOrder.verify(purgeDao).purgeCeActivities(dbSession, profiler);
    inOrder.verify(purgeDao).purgeCeScannerContexts(dbSession, profiler);
    inOrder.verify(dbSession).commit();
    inOrder.verify(dbSession).close();
    inOrder.verifyNoMoreInteractions();
  }
}

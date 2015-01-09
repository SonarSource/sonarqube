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

package org.sonar.server.computation;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.db.AnalysisReportDao;
import org.sonar.server.db.DbClient;

import static org.mockito.Mockito.*;

public class AnalysisReportQueueCleanerTest {

  AnalysisReportQueueCleaner sut;
  ServerUpgradeStatus serverUpgradeStatus;
  DbClient dbClient;
  AnalysisReportDao analysisReportDao;
  DbSession session;

  @Before
  public void before() {
    analysisReportDao = mock(AnalysisReportDao.class);
    serverUpgradeStatus = mock(ServerUpgradeStatus.class);
    dbClient = mock(DbClient.class);
    session = mock(DbSession.class);

    when(dbClient.analysisReportDao()).thenReturn(analysisReportDao);
    when(dbClient.openSession(false)).thenReturn(session);

    sut = new AnalysisReportQueueCleaner(serverUpgradeStatus, dbClient);
  }

  @Test
  public void start_must_call_dao_clean_update_to_pending_by_default() {
    sut.start();
    verify(analysisReportDao).resetAllToPendingStatus(any(DbSession.class));
    sut.stop();
  }

  @Test
  public void start_must_call_dao_truncate_when_upgrading() {
    when(serverUpgradeStatus.isUpgraded()).thenReturn(Boolean.TRUE);
    sut.start();
    verify(analysisReportDao).truncate(any(DbSession.class));
    sut.stop();
  }
}

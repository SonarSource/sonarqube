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
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.db.AnalysisReportDao;
import org.sonar.server.db.DbClient;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ComputationServiceTest {

  ComputationService sut;
  DbClient dbClient;
  AnalysisReportDao analysisReportDao;
  DbSession session;

  @Before
  public void before() {
    analysisReportDao = mock(AnalysisReportDao.class);
    dbClient = mock(DbClient.class);
    session = mock(DbSession.class);

    when(dbClient.analysisReportDao()).thenReturn(analysisReportDao);
    when(dbClient.openSession(false)).thenReturn(session);

    sut = new ComputationService(dbClient);
  }

  @Test
  public void create_must_call_dao_insert() throws Exception {
    sut.create("ANY-KEY");
    verify(analysisReportDao).insert(any(DbSession.class), any(AnalysisReportDto.class));
  }
}
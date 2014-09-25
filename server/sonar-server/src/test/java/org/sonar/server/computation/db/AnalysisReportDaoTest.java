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

package org.sonar.server.computation.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnalysisReportDaoTest extends AbstractDaoTestCase {
  private AnalysisReportDao dao;
  private DbSession session;
  private System2 system2;

  @Before
  public void before() {
    this.session = getMyBatis().openSession(false);
    this.system2 = mock(System2.class);
    this.dao = new AnalysisReportDao(system2);
  }

  @Test
  public void insert() {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-09-26").getTime());

    AnalysisReportDto report = new AnalysisReportDto()
      .setProjectKey("123456789-987654321")
      .setData("data-project")
      .setStatus("pending");
    report.setCreatedAt(DateUtils.parseDate("2014-09-24"))
      .setUpdatedAt(DateUtils.parseDate("2014-09-25"));

    dao.insert(session, report);
    session.commit();

    checkTables("insert", new String[]{"id"}, "analysis_reports");
  }
}

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
package org.sonar.server.issue.db;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssueDaoTest extends AbstractDaoTestCase {

  private IssueDao dao;
  private DbSession session;
  private System2 system2;

  @Before
  public void before() throws Exception {
    this.session = getMyBatis().openSession(false);
    this.system2 = mock(System2.class);
    this.dao = new IssueDao(system2);
  }

  @After
  public void after() {
    this.session.close();
  }

  @Test
  public void find_after_dates() throws Exception {
    setupData("shared", "should_select_all");

    Date t0 = new Date(0);
    assertThat(dao.findAfterDate(session, t0)).hasSize(3);

    Date t2014 = DateUtils.parseDate("2014-01-01");
    assertThat(dao.findAfterDate(session, t2014)).hasSize(1);
  }

  @Test
  public void find_after_dates_with_project() throws Exception {
    setupData("shared", "find_after_dates_with_project");

    assertThat(dao.findAfterDate(session, DateUtils.parseDate("2014-01-01"), ImmutableMap.of("project", "struts"))).hasSize(1);
  }
}

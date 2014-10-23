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

public class IssueAuthorizationDaoTest extends AbstractDaoTestCase {

  private IssueAuthorizationDao dao;
  private DbSession session;

  @Before
  public void before() throws Exception {
    this.session = getMyBatis().openSession(false);
    this.dao = new IssueAuthorizationDao(System2.INSTANCE);
  }

  @After
  public void after() {
    this.session.close();
  }

  @Test(expected = IllegalStateException.class)
  public void get_nullable_by_key_is_not_implemented() {
    assertThat(dao.getNullableByKey(session, "ABCD"));
  }

  @Test
  public void synchronize_after_since_beginning() throws Exception {
    setupData("synchronize_after_since_beginning");

    assertThat(session.getActionCount()).isEqualTo(0);

    dao.synchronizeAfter(session, new Date(0));
    // SynchronizeAfter adds an implicit action (refresh) after execution of synchronization
    assertThat(session.getActionCount()).isEqualTo(2);
  }

  @Test
  public void synchronize_after_since_given_date() {
    setupData("synchronize_after_since_given_date");

    assertThat(session.getActionCount()).isEqualTo(0);

    dao.synchronizeAfter(session, DateUtils.parseDate("2014-09-01"));
    // SynchronizeAfter adds an implicit action (refresh) after execution of synchronization
    assertThat(session.getActionCount()).isEqualTo(2);
  }

  @Test
  public void synchronize_after_with_project() {
    setupData("synchronize_after_with_project");

    assertThat(session.getActionCount()).isEqualTo(0);

    dao.synchronizeAfter(session, DateUtils.parseDate("2014-01-01"), ImmutableMap.of(IssueAuthorizationDao.PROJECT_UUID, "ABCD"));
    // SynchronizeAfter adds an implicit action (refresh) after execution of synchronization
    assertThat(session.getActionCount()).isEqualTo(2);
  }

}

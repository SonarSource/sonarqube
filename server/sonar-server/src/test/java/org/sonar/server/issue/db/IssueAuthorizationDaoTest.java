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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.db.IssueAuthorizationDto;
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
  public void get_nullable_by_key_is_not_implemented(){
    assertThat(dao.getNullableByKey(session, "sonar"));
  }

  @Test
  public void find_after_date(){
    setupData("find_after_date");

    Iterable<IssueAuthorizationDto> results = dao.findAfterDate(session, new Date(0));
    assertThat(results).hasSize(1);

    IssueAuthorizationDto dto = results.iterator().next();
    assertThat(dto.getProject()).isEqualTo("org.struts:struts");
    assertThat(dto.getKey()).isEqualTo("org.struts:struts");
    assertThat(dto.getPermission()).isEqualTo("user");
    assertThat(dto.getGroups()).containsExactly("Anyone", "devs");
    assertThat(dto.getUsers()).containsExactly("user1");
    assertThat(dto.getUpdatedAt()).isEqualTo(DateUtils.parseDate("2014-01-01"));
  }

  @Test
  public void find_after_date_return_dtos_after_given_date(){
    setupData("find_after_date_return_dtos_after_given_date");

    assertThat(dao.findAfterDate(session, new Date(0))).hasSize(2);

    assertThat(dao.findAfterDate(session, DateUtils.parseDate("2014-09-01"))).hasSize(1);
  }
}

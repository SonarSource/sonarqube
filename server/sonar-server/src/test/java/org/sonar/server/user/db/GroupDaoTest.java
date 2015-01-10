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

package org.sonar.server.user.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.GroupDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroupDaoTest extends AbstractDaoTestCase {

  GroupDao dao;
  DbSession session;
  System2 system2;

  @Before
  public void setUp() {
    this.session = getMyBatis().openSession(false);
    this.system2 = mock(System2.class);
    this.dao = new GroupDao(system2);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void select_by_key() {
    setupData("select_by_key");

    GroupDto group = new GroupDao().getByKey(session, "sonar-users");
    assertThat(group).isNotNull();
    assertThat(group.getId()).isEqualTo(1L);
    assertThat(group.getName()).isEqualTo("sonar-users");
    assertThat(group.getDescription()).isEqualTo("Sonar Users");
    assertThat(group.getCreatedAt()).isEqualTo(DateUtils.parseDate("2014-09-07"));
    assertThat(group.getUpdatedAt()).isEqualTo(DateUtils.parseDate("2014-09-08"));
  }

  @Test
  public void find_by_user_login() throws Exception {
    setupData("find_by_user_login");

    assertThat(dao.findByUserLogin(session, "john")).hasSize(2);
    assertThat(dao.findByUserLogin(session, "max")).isEmpty();
  }

  @Test
  public void insert() throws Exception {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-09-08").getTime());

    setupData("empty");

    GroupDto dto = new GroupDto()
      .setId(1L)
      .setName("sonar-users")
      .setDescription("Sonar Users");

    dao.insert(session, dto);
    session.commit();

    checkTables("insert", "groups");
  }
}

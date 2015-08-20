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

package org.sonar.db.user;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class GroupDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  GroupDao dao;
  DbSession session;
  System2 system2;

  @Before
  public void setUp() {
    dbTester.truncateTables();
    this.session = dbTester.myBatis().openSession(false);
    this.system2 = mock(System2.class);
    this.dao = new GroupDao(system2);
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void select_by_key() {
    dbTester.prepareDbUnit(getClass(), "select_by_key.xml");

    GroupDto group = new GroupDao(system2).selectOrFailByName(session, "sonar-users");
    assertThat(group).isNotNull();
    assertThat(group.getId()).isEqualTo(1L);
    assertThat(group.getName()).isEqualTo("sonar-users");
    assertThat(group.getDescription()).isEqualTo("Sonar Users");
    assertThat(group.getCreatedAt()).isEqualTo(DateUtils.parseDate("2014-09-07"));
    assertThat(group.getUpdatedAt()).isEqualTo(DateUtils.parseDate("2014-09-08"));
  }

  @Test
  public void select_by_id() {
    dbTester.prepareDbUnit(getClass(), "select_by_key.xml");

    GroupDto group = new GroupDao(system2).selectOrFailById(session, 1L);
    assertThat(group).isNotNull();
    assertThat(group.getId()).isEqualTo(1L);
    assertThat(group.getName()).isEqualTo("sonar-users");
    assertThat(group.getDescription()).isEqualTo("Sonar Users");
    assertThat(group.getCreatedAt()).isEqualTo(DateUtils.parseDate("2014-09-07"));
    assertThat(group.getUpdatedAt()).isEqualTo(DateUtils.parseDate("2014-09-08"));
  }

  @Test
  public void find_by_user_login() {
    dbTester.prepareDbUnit(getClass(), "find_by_user_login.xml");

    assertThat(dao.selectByUserLogin(session, "john")).hasSize(2);
    assertThat(dao.selectByUserLogin(session, "max")).isEmpty();
  }

  @Test
  public void insert() {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-09-08").getTime());

    dbTester.prepareDbUnit(getClass(), "empty.xml");

    GroupDto dto = new GroupDto()
      .setId(1L)
      .setName("sonar-users")
      .setDescription("Sonar Users");

    dao.insert(session, dto);
    session.commit();

    dbTester.assertDbUnit(getClass(), "insert-result.xml", "groups");
  }

  @Test
  public void update() {
    when(system2.now()).thenReturn(DateUtils.parseDate("2013-07-25").getTime());

    dbTester.prepareDbUnit(getClass(), "update.xml");

    GroupDto dto = new GroupDto()
      .setId(1L)
      .setName("new-name")
      .setDescription("New Description");

    dao.update(session, dto);
    session.commit();

    dbTester.assertDbUnit(getClass(), "update-result.xml", "groups");
  }

  @Test
  public void select_by_query() {
    dbTester.prepareDbUnit(getClass(), "select_by_query.xml");

    /*
     * Ordering and paging are not fully tested, case insensitive sort is broken on MySQL
     */

    // Null query
    assertThat(new GroupDao(system2).selectByQuery(session, null, 0, 10))
      .hasSize(5)
      .extracting("name").containsOnly("customers-group1", "customers-group2", "customers-group3", "SONAR-ADMINS", "sonar-users");

    // Empty query
    assertThat(new GroupDao(system2).selectByQuery(session, "", 0, 10))
      .hasSize(5)
      .extracting("name").containsOnly("customers-group1", "customers-group2", "customers-group3", "SONAR-ADMINS", "sonar-users");

    // Filter on name
    assertThat(new GroupDao(system2).selectByQuery(session, "sonar", 0, 10))
      .hasSize(2)
      .extracting("name").containsOnly("SONAR-ADMINS", "sonar-users");

    // Pagination
    assertThat(new GroupDao(system2).selectByQuery(session, null, 0, 3))
      .hasSize(3);
    assertThat(new GroupDao(system2).selectByQuery(session, null, 3, 3))
      .hasSize(2);
    assertThat(new GroupDao(system2).selectByQuery(session, null, 6, 3)).isEmpty();
    assertThat(new GroupDao(system2).selectByQuery(session, null, 0, 5))
      .hasSize(5);
    assertThat(new GroupDao(system2).selectByQuery(session, null, 5, 5)).isEmpty();
  }

  @Test
  public void count_by_query() {
    dbTester.prepareDbUnit(getClass(), "select_by_query.xml");

    // Null query
    assertThat(new GroupDao(system2).countByQuery(session, null)).isEqualTo(5);

    // Empty query
    assertThat(new GroupDao(system2).countByQuery(session, "")).isEqualTo(5);

    // Filter on name
    assertThat(new GroupDao(system2).countByQuery(session, "sonar")).isEqualTo(2);
  }

  @Test
  public void delete_by_id() {
    dbTester.prepareDbUnit(getClass(), "select_by_key.xml");

    GroupDao groupDao = new GroupDao(system2);
    groupDao.deleteById(session, 1L);
    session.commit();

    assertThat(groupDao.countByQuery(session, null)).isZero();
  }

}

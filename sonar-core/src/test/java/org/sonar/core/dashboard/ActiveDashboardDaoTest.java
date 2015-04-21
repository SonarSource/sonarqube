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
package org.sonar.core.dashboard;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.persistence.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class ActiveDashboardDaoTest {

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  private ActiveDashboardDao dao;

  @Before
  public void createDao() throws Exception {
    dbTester.truncateTables();
    dao = new ActiveDashboardDao(dbTester.myBatis());
  }

  @Test
  public void shouldInsert() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shouldInsert.xml");

    ActiveDashboardDto dashboard = new ActiveDashboardDto();
    dashboard.setDashboardId(2L);
    dashboard.setUserId(3L);
    dashboard.setOrderIndex(4);
    dao.insert(dashboard);

    dbTester.assertDbUnit(getClass(), "shouldInsert-result.xml", "active_dashboards");
  }

  @Test
  public void shouldInsertWithNoUser() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shouldInsert.xml");

    ActiveDashboardDto dashboard = new ActiveDashboardDto();
    dashboard.setDashboardId(2L);
    dashboard.setOrderIndex(4);
    dao.insert(dashboard);

    dbTester.assertDbUnit(getClass(), "shouldInsertWithNoUser-result.xml", "active_dashboards");
  }

  @Test
  public void shouldGetMaxOrderIndexForNullUser() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shouldGetMaxOrderIndexForNullUser.xml");

    int index = dao.selectMaxOrderIndexForNullUser();

    assertThat(index).isEqualTo(15);
  }

  @Test
  public void shouldGetZeroMaxOrderIndex() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    int index = dao.selectMaxOrderIndexForNullUser();

    assertThat(index).isZero();
  }

  @Test
  public void should_get_dashboards_for_anonymous() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shouldSelectDashboardsForAnonymous.xml");

    assertThat(dao.selectGlobalDashboardsForUserLogin(null)).hasSize(2).extracting("id").containsExactly(2L, 1L);
  }

  @Test
  public void should_get_dashboards_for_user() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shouldSelectDashboardsForUser.xml");

    assertThat(dao.selectGlobalDashboardsForUserLogin("obiwan")).hasSize(2).extracting("id").containsExactly(2L, 1L);
  }

  @Test
  public void should_get_project_dashboards_for_anonymous() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shouldSelectProjectDashboardsForAnonymous.xml");

    assertThat(dao.selectProjectDashboardsForUserLogin(null)).hasSize(2).extracting("id").containsExactly(2L, 1L);
  }

  @Test
  public void should_get_project_dashboards_for_user() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shouldSelectProjectDashboardsForUser.xml");

    assertThat(dao.selectProjectDashboardsForUserLogin("obiwan")).hasSize(2).extracting("id").containsExactly(2L, 1L);
  }
}

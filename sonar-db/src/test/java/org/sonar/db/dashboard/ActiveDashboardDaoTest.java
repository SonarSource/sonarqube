/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.dashboard;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;


public class ActiveDashboardDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbSession session = dbTester.getSession();

  ActiveDashboardDao underTest = dbTester.getDbClient().activeDashboardDao();

  @Test
  public void shouldInsert() {
    dbTester.prepareDbUnit(getClass(), "shouldInsert.xml");

    ActiveDashboardDto dashboard = new ActiveDashboardDto();
    dashboard.setDashboardId(2L);
    dashboard.setUserId(3L);
    dashboard.setOrderIndex(4);
    underTest.insert(dashboard);

    dbTester.assertDbUnit(getClass(), "shouldInsert-result.xml", "active_dashboards");
  }

  @Test
  public void shouldInsertWithNoUser() {
    dbTester.prepareDbUnit(getClass(), "shouldInsert.xml");

    ActiveDashboardDto dashboard = new ActiveDashboardDto();
    dashboard.setDashboardId(2L);
    dashboard.setOrderIndex(4);
    underTest.insert(dashboard);

    dbTester.assertDbUnit(getClass(), "shouldInsertWithNoUser-result.xml", "active_dashboards");
  }

  @Test
  public void shouldGetMaxOrderIndexForNullUser() {
    dbTester.prepareDbUnit(getClass(), "shouldGetMaxOrderIndexForNullUser.xml");

    int index = underTest.selectMaxOrderIndexForNullUser();

    assertThat(index).isEqualTo(15);
  }

  @Test
  public void shouldGetZeroMaxOrderIndex() {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    int index = underTest.selectMaxOrderIndexForNullUser();

    assertThat(index).isZero();
  }

  @Test
  public void should_get_dashboards_for_anonymous() {
    dbTester.prepareDbUnit(getClass(), "shouldSelectDashboardsForAnonymous.xml");

    assertThat(underTest.selectGlobalDashboardsForUserLogin(null)).hasSize(2).extracting("id").containsExactly(2L, 1L);
  }

  @Test
  public void should_get_dashboards_for_user() {
    dbTester.prepareDbUnit(getClass(), "shouldSelectDashboardsForUser.xml");

    assertThat(underTest.selectGlobalDashboardsForUserLogin("obiwan")).hasSize(2).extracting("id").containsExactly(2L, 1L);
  }

  @Test
  public void should_get_project_dashboards_for_anonymous() {
    dbTester.prepareDbUnit(getClass(), "shouldSelectProjectDashboardsForAnonymous.xml");

    assertThat(underTest.selectProjectDashboardsForUserLogin(null)).hasSize(2).extracting("id").containsExactly(2L, 1L);
  }

  @Test
  public void should_get_project_dashboards_for_user() {
    dbTester.prepareDbUnit(getClass(), "shouldSelectProjectDashboardsForUser.xml");

    assertThat(underTest.selectProjectDashboardsForUserLogin("obiwan")).hasSize(2).extracting("id").containsExactly(2L, 1L);
  }

  @Test
  public void select_by_id() throws Exception {
    ActiveDashboardDto dto = new ActiveDashboardDto()
      .setDashboardId(10L)
      .setOrderIndex(2)
      .setUserId(5L);
    underTest.insert(session, dto);
    session.commit();

    ActiveDashboardDto dtoReloaded = underTest.selectById(session, dto.getId());
    assertThat(dtoReloaded).isNotNull();
    assertThat(dtoReloaded.getDashboardId()).isEqualTo(10L);
    assertThat(dtoReloaded.getUserId()).isEqualTo(5L);
    assertThat(dtoReloaded.getOrderIndex()).isEqualTo(2);

    assertThat(underTest.selectById(session, 123L)).isNull();
  }
}

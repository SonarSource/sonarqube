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
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ActiveDashboardDaoTest extends AbstractDaoTestCase {

  private ActiveDashboardDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new ActiveDashboardDao(getMyBatis());
  }

  @Test
  public void shouldInsert() throws Exception {
    setupData("shouldInsert");

    ActiveDashboardDto dashboard = new ActiveDashboardDto();
    dashboard.setDashboardId(2L);
    dashboard.setUserId(3L);
    dashboard.setOrderIndex(4);
    dao.insert(dashboard);

    checkTables("shouldInsert", "active_dashboards");
  }

  @Test
  public void shouldInsertWithNoUser() throws Exception {
    setupData("shouldInsert");

    ActiveDashboardDto dashboard = new ActiveDashboardDto();
    dashboard.setDashboardId(2L);
    dashboard.setOrderIndex(4);
    dao.insert(dashboard);

    checkTables("shouldInsertWithNoUser", "active_dashboards");
  }

  @Test
  public void shouldGetMaxOrderIndexForNullUser() throws Exception {
    setupData("shouldGetMaxOrderIndexForNullUser");

    int index = dao.selectMaxOrderIndexForNullUser();

    assertThat(index, is(15));
  }

  @Test
  public void shouldGetZeroMaxOrderIndex() throws Exception {
    setupData("empty");

    int index = dao.selectMaxOrderIndexForNullUser();

    assertThat(index, is(0));
  }

}

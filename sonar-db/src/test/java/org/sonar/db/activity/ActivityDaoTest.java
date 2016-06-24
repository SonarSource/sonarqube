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
package org.sonar.db.activity;

import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActivityDaoTest {

  System2 system = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system);
  DbSession dbSession = dbTester.getSession();

  ActivityDao underTest = dbTester.getDbClient().activityDao();

  @Test
  public void insert() {
    when(system.now()).thenReturn(1_500_000_000_000L);
    ActivityDto dto = new ActivityDto()
      .setKey("UUID_1")
      .setAction("THE_ACTION")
      .setType("THE_TYPE")
      .setAuthor("THE_AUTHOR")
      .setData("THE_DATA")
      .setProfileKey("PROFILE_KEY");
    underTest.insert(dbSession, dto);
    dbSession.commit();

    Map<String, Object> map = dbTester.selectFirst("select created_at as \"createdAt\", log_action as \"action\", " +
      "data_field as \"data\", profile_key as \"profileKey\" " +
      "from activities where log_key='UUID_1'");
    assertThat(map.get("action")).isEqualTo("THE_ACTION");
    // not possible to check exact date yet. dbTester#selectFirst() uses ResultSet#getObject(), which returns
    // non-JDBC interface in Oracle driver.
    assertThat(map.get("createdAt")).isNotNull();
    assertThat(map.get("data")).isEqualTo("THE_DATA");
    assertThat(map.get("profileKey")).isEqualTo("PROFILE_KEY");
  }
}

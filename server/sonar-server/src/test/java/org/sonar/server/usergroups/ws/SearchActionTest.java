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

package org.sonar.server.usergroups.ws;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupMembershipDao;
import org.sonar.db.user.UserGroupDao;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.GroupTesting.newGroupDto;

@Category(DbTests.class)
public class SearchActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private WsTester ws;

  private GroupDao groupDao;
  private GroupMembershipDao groupMembershipDao;
  private UserGroupDao userGroupDao;
  private DbSession dbSession;

  @Before
  public void setUp() {
    DbClient dbClient = db.getDbClient();
    groupDao = dbClient.groupDao();
    groupMembershipDao = dbClient.groupMembershipDao();
    userGroupDao = dbClient.userGroupDao();

    ws = new WsTester(new UserGroupsWs(new SearchAction(dbClient)));

    dbSession = dbClient.openSession(false);
  }

  @Test
  public void search_empty() throws Exception {
    newRequest().execute().assertJson(getClass(), "empty.json");
  }

  @Test
  public void search_without_parameters() throws Exception {
    insertGroups("users", "admins", "customer1", "customer2", "customer3");
    dbSession.commit();

    newRequest().execute().assertJson(getClass(), "five_groups.json");
  }

  @Test
  public void search_with_members() throws Exception {
    insertGroups("users", "admins", "customer1", "customer2", "customer3");
    insertMembers("users", 5);
    insertMembers("admins", 1);
    insertMembers("customer2", 4);
    dbSession.commit();

    newRequest().execute().assertJson(getClass(), "with_members.json");
  }

  @Test
  public void search_with_query() throws Exception {
    insertGroups("users", "admins", "customer%_%/1", "customer%_%/2", "customer%_%/3");
    dbSession.commit();

    newRequest().setParam(Param.TEXT_QUERY, "tomer%_%/").execute().assertJson(getClass(), "customers.json");
  }

  @Test
  public void search_with_paging() throws Exception {
    insertGroups("users", "admins", "customer1", "customer2", "customer3");
    dbSession.commit();

    newRequest()
      .setParam(Param.PAGE_SIZE, "3").execute().assertJson(getClass(), "page_1.json");
    newRequest()
      .setParam(Param.PAGE_SIZE, "3").setParam(Param.PAGE, "2").execute().assertJson(getClass(), "page_2.json");
    newRequest()
      .setParam(Param.PAGE_SIZE, "3").setParam(Param.PAGE, "3").execute().assertJson(getClass(), "page_3.json");
  }

  @Test
  public void search_with_fields() throws Exception {
    insertGroups("sonar-users");
    dbSession.commit();

    assertThat(newRequest().execute().outputAsString())
      .contains("id")
      .contains("name")
      .contains("description")
      .contains("membersCount");

    assertThat(newRequest().setParam(Param.FIELDS, "").execute().outputAsString())
      .contains("id")
      .contains("name")
      .contains("description")
      .contains("membersCount");

    assertThat(newRequest().setParam(Param.FIELDS, "name").execute().outputAsString())
      .contains("id")
      .contains("name")
      .doesNotContain("description")
      .doesNotContain("membersCount");

    assertThat(newRequest().setParam(Param.FIELDS, "description").execute().outputAsString())
      .contains("id")
      .doesNotContain("name")
      .contains("description")
      .doesNotContain("membersCount");

    assertThat(newRequest().setParam(Param.FIELDS, "membersCount").execute().outputAsString())
      .contains("id")
      .doesNotContain("name")
      .doesNotContain("description")
      .contains("membersCount");
  }

  private WsTester.TestRequest newRequest() {
    return ws.newGetRequest("api/user_groups", "search");
  }

  private void insertGroups(String... groupNames) {
    for (String groupName : groupNames) {
      groupDao.insert(dbSession, newGroupDto()
        .setName(groupName)
        .setDescription(StringUtils.capitalize(groupName)));
    }
  }

  private void insertMembers(String groupName, int count) {
    long groupId = groupDao.selectOrFailByName(dbSession, groupName).getId();
    for (int i = 0; i < count; i++) {
      userGroupDao.insert(dbSession, new UserGroupDto().setGroupId(groupId).setUserId((long) i + 1));
    }
  }
}

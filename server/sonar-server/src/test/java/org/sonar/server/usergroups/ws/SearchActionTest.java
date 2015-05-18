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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.GroupMembershipDao;
import org.sonar.core.user.UserGroupDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.db.GroupDao;
import org.sonar.server.user.db.UserGroupDao;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SearchActionTest {

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  WebService.Controller controller;

  WsTester tester;

  private GroupDao groupDao;

  private GroupMembershipDao groupMembershipDao;

  private UserGroupDao userGroupDao;

  private DbSession session;

  @Before
  public void setUp() {
    dbTester.truncateTables();

    groupDao = new GroupDao(mock(System2.class));
    groupMembershipDao = new GroupMembershipDao(dbTester.myBatis());
    userGroupDao = new UserGroupDao();

    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), groupDao, groupMembershipDao);

    tester = new WsTester(new UserGroupsWs(new SearchAction(dbClient)));
    controller = tester.controller("api/usergroups");

    session = dbClient.openSession(false);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void search_empty() throws Exception {
    tester.newGetRequest("api/usergroups", "search").execute().assertJson(getClass(), "empty.json");
  }

  @Test
  public void search_without_parameters() throws Exception {
    insertGroups("users", "admins", "customer1", "customer2", "customer3");
    session.commit();

    tester.newGetRequest("api/usergroups", "search").execute().assertJson(getClass(), "five_groups.json");
  }

  @Test
  public void search_with_members() throws Exception {
    insertGroups("users", "admins", "customer1", "customer2", "customer3");
    insertMembers("users", 5);
    insertMembers("admins", 1);
    insertMembers("customer2", 4);
    session.commit();

    tester.newGetRequest("api/usergroups", "search").execute().assertJson(getClass(), "with_members.json");
  }

  @Test
  public void search_with_query() throws Exception {
    insertGroups("users", "admins", "customer1", "customer2", "customer3");
    session.commit();

    tester.newGetRequest("api/usergroups", "search").setParam(WebService.Param.TEXT_QUERY, "custom").execute().assertJson(getClass(), "customers.json");
  }

  @Test
  public void search_with_paging() throws Exception {
    insertGroups("users", "admins", "customer1", "customer2", "customer3");
    session.commit();

    tester.newGetRequest("api/usergroups", "search")
      .setParam(WebService.Param.PAGE_SIZE, "3").execute().assertJson(getClass(), "page_1.json");
    tester.newGetRequest("api/usergroups", "search")
      .setParam(WebService.Param.PAGE_SIZE, "3").setParam(WebService.Param.PAGE, "2").execute().assertJson(getClass(), "page_2.json");
    tester.newGetRequest("api/usergroups", "search")
      .setParam(WebService.Param.PAGE_SIZE, "3").setParam(WebService.Param.PAGE, "3").execute().assertJson(getClass(), "page_3.json");
  }

  @Test
  public void search_with_fields() throws Exception {
    insertGroups("sonar-users");
    session.commit();

    assertThat(tester.newGetRequest("api/usergroups", "search").execute().outputAsString())
      .contains("key")
      .contains("name")
      .contains("description")
      .contains("membersCount");

    assertThat(tester.newGetRequest("api/usergroups", "search").setParam("f", "").execute().outputAsString())
      .contains("key")
      .contains("name")
      .contains("description")
      .contains("membersCount");

    assertThat(tester.newGetRequest("api/usergroups", "search").setParam("f", "name").execute().outputAsString())
      .contains("key")
      .contains("name")
      .doesNotContain("description")
      .doesNotContain("membersCount");

    assertThat(tester.newGetRequest("api/usergroups", "search").setParam("f", "description").execute().outputAsString())
      .contains("key")
      .doesNotContain("name")
      .contains("description")
      .doesNotContain("membersCount");

    assertThat(tester.newGetRequest("api/usergroups", "search").setParam("f", "membersCount").execute().outputAsString())
      .contains("key")
      .doesNotContain("name")
      .doesNotContain("description")
      .contains("membersCount");
  }

  private void insertGroups(String... groupNames) {
    for (String groupName : groupNames) {
      groupDao.insert(session, new GroupDto()
        .setName(groupName)
        .setDescription(StringUtils.capitalize(groupName)));
    }
  }

  private void insertMembers(String groupName, int count) {
    long groupId = groupDao.selectByKey(session, groupName).getId();
    for (int i = 0; i < count; i++) {
      userGroupDao.insert(session, new UserGroupDto().setGroupId(groupId).setUserId((long) i + 1));
    }
  }
}

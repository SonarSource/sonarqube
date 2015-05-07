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

package org.sonar.server.user.ws;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.es.EsTester;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.ws.WsTester;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchActionTest {

  @ClassRule
  public static final EsTester esTester = new EsTester().addDefinitions(new UserIndexDefinition(new Settings()));

  WebService.Controller controller;

  WsTester tester;

  UserIndex index;

  @Before
  public void setUp() {
    esTester.truncateIndices();

    index = new UserIndex(esTester.client());
    tester = new WsTester(new UsersWs(new SearchAction(index)));
    controller = tester.controller("api/users");

  }

  @Test
  public void search_empty() throws Exception {
    tester.newGetRequest("api/users", "search2").execute().assertJson(getClass(), "empty.json");
  }

  @Test
  public void search_without_parameters() throws Exception {
    injectUsers(5);

    tester.newGetRequest("api/users", "search2").execute().assertJson(getClass(), "five_users.json");
  }

  @Test
  public void search_with_query() throws Exception {
    injectUsers(5);

    tester.newGetRequest("api/users", "search2").setParam("q", "user-1").execute().assertJson(getClass(), "user_one.json");
  }

  @Test
  public void search_with_paging() throws Exception {
    injectUsers(10);

    tester.newGetRequest("api/users", "search2").setParam("ps", "5").execute().assertJson(getClass(), "page_one.json");
    tester.newGetRequest("api/users", "search2").setParam("ps", "5").setParam("p", "2").execute().assertJson(getClass(), "page_two.json");
  }

  @Test
  public void search_with_fields() throws Exception {
    injectUsers(1);

    assertThat(tester.newGetRequest("api/users", "search2").execute().outputAsString())
      .contains("login")
      .contains("name")
      .contains("email")
      .contains("scmAccounts");

    assertThat(tester.newGetRequest("api/users", "search2").setParam("f", "").execute().outputAsString())
      .contains("login")
      .contains("name")
      .contains("email")
      .contains("scmAccounts");

    assertThat(tester.newGetRequest("api/users", "search2").setParam("f", "login").execute().outputAsString())
      .contains("login")
      .doesNotContain("name")
      .doesNotContain("email")
      .doesNotContain("scmAccounts");

    assertThat(tester.newGetRequest("api/users", "search2").setParam("f", "scmAccounts").execute().outputAsString())
      .doesNotContain("login")
      .doesNotContain("name")
      .doesNotContain("email")
      .contains("scmAccounts");
  }

  private void injectUsers(int numberOfUsers) throws Exception {
    long createdAt = System.currentTimeMillis();
    UserDoc[] users = new UserDoc[numberOfUsers];
    for (int index = 0; index < numberOfUsers; index++) {
      users[index] = new UserDoc()
        .setActive(true)
        .setCreatedAt(createdAt)
        .setEmail(String.format("user-%d@mail.com", index))
        .setLogin(String.format("user-%d", index))
        .setName(String.format("User %d", index))
        .setScmAccounts(Arrays.asList(String.format("user-%d", index)))
        .setUpdatedAt(createdAt);
    }
    esTester.putDocuments(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, users);
  }
}

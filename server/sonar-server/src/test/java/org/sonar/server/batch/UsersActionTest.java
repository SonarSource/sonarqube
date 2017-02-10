/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.batch;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.scanner.protocol.input.ScannerInput.User;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class UsersActionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public EsTester es = new EsTester(new UserIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  UserIndex userIndex;

  WsTester tester;

  UsersAction usersAction;

  @Before
  public void before() {
    userIndex = new UserIndex(es.client());
    usersAction = new UsersAction(userIndex, userSessionRule);
    tester = new WsTester(new BatchWs(usersAction));
  }

  @Test
  public void return_minimal_fields() throws Exception {
    es.putDocuments(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER,
      new UserDoc().setLogin("ada.lovelace").setName("Ada Lovelace").setActive(false),
      new UserDoc().setLogin("grace.hopper").setName("Grace Hopper").setActive(true));
    userSessionRule.logIn("sonarqtech");

    WsTester.TestRequest request = tester.newGetRequest("batch", "users").setParam("logins", "ada.lovelace,grace.hopper");

    ByteArrayInputStream input = new ByteArrayInputStream(request.execute().output());
    User user1 = User.parseDelimitedFrom(input);
    User user2 = User.parseDelimitedFrom(input);
    assertThat(User.parseDelimitedFrom(input)).isNull();

    List<User> users = Arrays.asList(user1, user2);
    assertThat(users).extracting("login").containsOnly("ada.lovelace", "grace.hopper");
    assertThat(users).extracting("name").containsOnly("Ada Lovelace", "Grace Hopper");
  }

  @Test
  public void fail_without_being_logged() throws Exception {
    thrown.expect(UnauthorizedException.class);
    tester.newGetRequest("batch", "users").setParam("logins", "ada.lovelace,grace.hopper").execute();
  }

}

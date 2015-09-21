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

package org.sonar.server.scanner;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.batch.protocol.input.BatchInput.User;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.es.EsTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.ws.WsTester;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class UsersActionTest {

  @ClassRule
  public static EsTester es = new EsTester().addDefinitions(new UserIndexDefinition(new Settings()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  UserIndex userIndex;

  WsTester tester;

  UsersAction usersAction;

  @Before
  public void before() {
    es.truncateIndices();

    userIndex = new UserIndex(es.client());
    usersAction = new UsersAction(userIndex, userSessionRule);

    tester = new WsTester(new ScannerWs(new ScannerIndex(mock(Server.class)), usersAction));
  }

  @Test
  public void return_minimal_fields() throws Exception {
    es.putDocuments(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER,
      new UserDoc().setLogin("ada.lovelace").setName("Ada Lovelace").setActive(false),
      new UserDoc().setLogin("grace.hopper").setName("Grace Hopper").setActive(true));

    userSessionRule.login("sonarqtech").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION);

    WsTester.TestRequest request = tester.newGetRequest("scanner", "users").setParam("logins", "ada.lovelace,grace.hopper");

    ByteArrayInputStream input = new ByteArrayInputStream(request.execute().output());

    User user = User.parseDelimitedFrom(input);
    assertThat(user.getLogin()).isEqualTo("ada.lovelace");
    assertThat(user.getName()).isEqualTo("Ada Lovelace");

    user = User.parseDelimitedFrom(input);
    assertThat(user.getLogin()).isEqualTo("grace.hopper");
    assertThat(user.getName()).isEqualTo("Grace Hopper");

    assertThat(User.parseDelimitedFrom(input)).isNull();
  }
}

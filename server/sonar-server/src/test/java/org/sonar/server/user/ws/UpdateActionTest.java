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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.ws.WsTester;

import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpdateActionTest {

  WebService.Controller controller;

  WsTester tester;

  @Mock
  UserIndex index;

  @Mock
  UserUpdater updater;

  @Captor
  ArgumentCaptor<UpdateUser> userCaptor;

  @Before
  public void setUp() throws Exception {
    tester = new WsTester(new UsersWs(new UpdateAction(index, updater)));
    controller = tester.controller("api/users");
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  @Test
  public void update_user() throws Exception {
    Map<String, Object> userDocMap = newHashMap();
    userDocMap.put("login", "john");
    userDocMap.put("name", "John");
    userDocMap.put("email", "john@email.com");
    userDocMap.put("scmAccounts", newArrayList("jn"));
    userDocMap.put("active", true);
    userDocMap.put("createdAt", 15000L);
    userDocMap.put("updatedAt", 15000L);
    when(index.getByLogin("john")).thenReturn(new UserDoc(userDocMap));

    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scm_accounts", "jn")
      .setParam("password", "1234")
      .setParam("password_confirmation", "1234")
      .execute()
      .assertJson(getClass(), "update_user.json");

    verify(updater).update(userCaptor.capture());
    assertThat(userCaptor.getValue().login()).isEqualTo("john");
    assertThat(userCaptor.getValue().name()).isEqualTo("John");
    assertThat(userCaptor.getValue().email()).isEqualTo("john@email.com");
    assertThat(userCaptor.getValue().scmAccounts()).containsOnly("jn");
    assertThat(userCaptor.getValue().password()).isEqualTo("1234");
    assertThat(userCaptor.getValue().passwordConfirmation()).isEqualTo("1234");
  }

  @Test
  public void update_only_name() throws Exception {
    Map<String, Object> userDocMap = newHashMap();
    userDocMap.put("login", "john");
    userDocMap.put("name", "John");
    userDocMap.put("email", "john@email.com");
    userDocMap.put("scmAccounts", newArrayList("jn"));
    userDocMap.put("active", true);
    userDocMap.put("createdAt", 15000L);
    userDocMap.put("updatedAt", 15000L);
    when(index.getByLogin("john")).thenReturn(new UserDoc(userDocMap));

    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .setParam("name", "John")
      .execute();

    verify(updater).update(userCaptor.capture());
    assertThat(userCaptor.getValue().isNameChanged()).isTrue();
    assertThat(userCaptor.getValue().isEmailChanged()).isFalse();
    assertThat(userCaptor.getValue().isScmAccountsChanged()).isFalse();
    assertThat(userCaptor.getValue().isPasswordChanged()).isFalse();
    assertThat(userCaptor.getValue().isPasswordChanged()).isFalse();
  }

  @Test
  public void update_only_email() throws Exception {
    Map<String, Object> userDocMap = newHashMap();
    userDocMap.put("login", "john");
    userDocMap.put("name", "John");
    userDocMap.put("email", "john@email.com");
    userDocMap.put("scmAccounts", newArrayList("jn"));
    userDocMap.put("active", true);
    userDocMap.put("createdAt", 15000L);
    userDocMap.put("updatedAt", 15000L);
    when(index.getByLogin("john")).thenReturn(new UserDoc(userDocMap));

    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .setParam("email", "john@email.com")
      .execute();

    verify(updater).update(userCaptor.capture());
    assertThat(userCaptor.getValue().isNameChanged()).isFalse();
    assertThat(userCaptor.getValue().isEmailChanged()).isTrue();
    assertThat(userCaptor.getValue().isScmAccountsChanged()).isFalse();
    assertThat(userCaptor.getValue().isPasswordChanged()).isFalse();
    assertThat(userCaptor.getValue().isPasswordChanged()).isFalse();
  }

  @Test
  public void update_only_scm_accounts() throws Exception {
    Map<String, Object> userDocMap = newHashMap();
    userDocMap.put("login", "john");
    userDocMap.put("name", "John");
    userDocMap.put("email", "john@email.com");
    userDocMap.put("scmAccounts", newArrayList("jn"));
    userDocMap.put("active", true);
    userDocMap.put("createdAt", 15000L);
    userDocMap.put("updatedAt", 15000L);
    when(index.getByLogin("john")).thenReturn(new UserDoc(userDocMap));

    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .setParam("scm_accounts", "jn")
      .execute();

    verify(updater).update(userCaptor.capture());
    assertThat(userCaptor.getValue().isNameChanged()).isFalse();
    assertThat(userCaptor.getValue().isEmailChanged()).isFalse();
    assertThat(userCaptor.getValue().isScmAccountsChanged()).isTrue();
    assertThat(userCaptor.getValue().isPasswordChanged()).isFalse();
    assertThat(userCaptor.getValue().isPasswordChanged()).isFalse();
  }

  @Test
  public void update_only_password() throws Exception {
    Map<String, Object> userDocMap = newHashMap();
    userDocMap.put("login", "john");
    userDocMap.put("name", "John");
    userDocMap.put("email", "john@email.com");
    userDocMap.put("scmAccounts", newArrayList("jn"));
    userDocMap.put("active", true);
    userDocMap.put("createdAt", 15000L);
    userDocMap.put("updatedAt", 15000L);
    when(index.getByLogin("john")).thenReturn(new UserDoc(userDocMap));

    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .setParam("password", "1234")
      .setParam("password_confirmation", "1234")
      .execute();

    verify(updater).update(userCaptor.capture());
    assertThat(userCaptor.getValue().isNameChanged()).isFalse();
    assertThat(userCaptor.getValue().isEmailChanged()).isFalse();
    assertThat(userCaptor.getValue().isScmAccountsChanged()).isFalse();
    assertThat(userCaptor.getValue().isPasswordChanged()).isTrue();
    assertThat(userCaptor.getValue().isPasswordChanged()).isTrue();
  }
}

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
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UserService;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.ws.WsTester;

import java.util.Locale;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateActionTest {

  WebService.Controller controller;

  WsTester tester;

  @Mock
  UserService service;

  @Mock
  I18n i18n;

  @Captor
  ArgumentCaptor<NewUser> newUserCaptor;

  @Before
  public void setUp() throws Exception {
    tester = new WsTester(new UsersWs(new CreateAction(service, i18n), new UpdateAction(service)));
    controller = tester.controller("api/users");
  }

  @Test
  public void create_user() throws Exception {
    Map<String, Object> userDocMap = newHashMap();
    userDocMap.put("login", "john");
    userDocMap.put("name", "John");
    userDocMap.put("email", "john@email.com");
    userDocMap.put("scmAccounts", newArrayList("jn"));
    userDocMap.put("active", true);
    userDocMap.put("createdAt", 15000L);
    userDocMap.put("updatedAt", 15000L);
    when(service.getByLogin("john")).thenReturn(new UserDoc(userDocMap));

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scm_accounts", "jn")
      .setParam("password", "1234")
      .setParam("password_confirmation", "1234").execute()
      .assertJson(getClass(), "create_user.json");

    verify(service).create(newUserCaptor.capture());
    assertThat(newUserCaptor.getValue().login()).isEqualTo("john");
    assertThat(newUserCaptor.getValue().name()).isEqualTo("John");
    assertThat(newUserCaptor.getValue().email()).isEqualTo("john@email.com");
    assertThat(newUserCaptor.getValue().scmAccounts()).containsOnly("jn");
    assertThat(newUserCaptor.getValue().password()).isEqualTo("1234");
    assertThat(newUserCaptor.getValue().passwordConfirmation()).isEqualTo("1234");
  }

  @Test
  public void reactivate_user() throws Exception {
    Map<String, Object> userDocMap = newHashMap();
    userDocMap.put("login", "john");
    userDocMap.put("name", "John");
    userDocMap.put("email", "john@email.com");
    userDocMap.put("scmAccounts", newArrayList("jn"));
    userDocMap.put("active", true);
    userDocMap.put("createdAt", 15000L);
    userDocMap.put("updatedAt", 15000L);
    when(service.getByLogin("john")).thenReturn(new UserDoc(userDocMap));
    when(service.create(any(NewUser.class))).thenReturn(true);

    MockUserSession.set().setLogin("julien").setLocale(Locale.FRENCH);
    when(i18n.message(Locale.FRENCH, "user.reactivated", "user.reactivated", "john")).thenReturn("The user 'john' has been reactivated.");

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scm_accounts", "jn")
      .setParam("password", "1234")
      .setParam("password_confirmation", "1234").execute()
      .assertJson(getClass(), "reactivate_user.json");
  }

}

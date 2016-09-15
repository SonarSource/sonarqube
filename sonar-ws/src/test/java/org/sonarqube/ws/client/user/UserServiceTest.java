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

package org.sonarqube.ws.client.user;

import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_EMAIL;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PASSWORD;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNT;

public class UserServiceTest {

  @Rule
  public ServiceTester<UserService> serviceTester = new ServiceTester<>(new UserService(mock(WsConnector.class)));

  private UserService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void create() {
    underTest.create(CreateRequest.builder()
      .setLogin("john")
      .setPassword("123456")
      .setName("John")
      .setEmail("john@doo.com")
      .setScmAccounts(asList("jo", "hn"))
      .build());

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasParam(PARAM_LOGIN, "john")
      .hasParam(PARAM_PASSWORD, "123456")
      .hasParam(PARAM_NAME, "John")
      .hasParam(PARAM_EMAIL, "john@doo.com")
      .hasParam(PARAM_SCM_ACCOUNT, asList("jo", "hn"))
      .andNoOtherParam();
  }

  @Test
  public void update() {
    underTest.update(UpdateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setEmail("john@doo.com")
      .setScmAccounts(asList("jo", "hn"))
      .build());

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasParam(PARAM_LOGIN, "john")
      .hasParam(PARAM_NAME, "John")
      .hasParam(PARAM_EMAIL, "john@doo.com")
      .hasParam(PARAM_SCM_ACCOUNT, asList("jo", "hn"))
      .andNoOtherParam();
  }

}

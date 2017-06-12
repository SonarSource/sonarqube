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
package org.sonarqube.ws.client.user;

import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.WsUsers.CreateWsResponse;
import org.sonarqube.ws.WsUsers.GroupsWsResponse;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.server.ws.WebService.Param.FIELDS;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_EMAIL;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOCAL;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PASSWORD;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNT;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SELECTED;

public class UsersServiceTest {

  @Rule
  public ServiceTester<UsersService> serviceTester = new ServiceTester<>(new UsersService(mock(WsConnector.class)));

  private UsersService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void search() {
    underTest.search(SearchRequest.builder()
      .setQuery("john")
      .setPage(10)
      .setPageSize(50)
      .setPossibleFields(asList("email", "name"))
      .build());

    assertThat(serviceTester.getGetParser()).isSameAs(WsUsers.SearchWsResponse.parser());
    serviceTester.assertThat(serviceTester.getGetRequest())
      .hasParam(TEXT_QUERY, "john")
      .hasParam(PAGE, 10)
      .hasParam(PAGE_SIZE, 50)
      .hasParam(FIELDS, "email,name")
      .andNoOtherParam();
  }

  @Test
  public void create() {
    underTest.create(CreateRequest.builder()
      .setLogin("john")
      .setPassword("123456")
      .setName("John")
      .setEmail("john@doo.com")
      .setScmAccounts(asList("jo", "hn"))
      .setLocal(true)
      .build());

    assertThat(serviceTester.getPostParser()).isSameAs(CreateWsResponse.parser());
    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasParam(PARAM_LOGIN, "john")
      .hasParam(PARAM_PASSWORD, "123456")
      .hasParam(PARAM_NAME, "John")
      .hasParam(PARAM_EMAIL, "john@doo.com")
      .hasParam(PARAM_SCM_ACCOUNT, asList("jo", "hn"))
      .hasParam(PARAM_LOCAL, "true")
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

  @Test
  public void groups() {
    underTest.groups(GroupsRequest.builder()
      .setLogin("john")
      .setOrganization("orga-uuid")
      .setSelected("all")
      .setQuery("sonar-users")
      .setPage(10)
      .setPageSize(50)
      .build());

    assertThat(serviceTester.getGetParser()).isSameAs(GroupsWsResponse.parser());
    serviceTester.assertThat(serviceTester.getGetRequest())
      .hasParam(PARAM_LOGIN, "john")
      .hasParam(PARAM_ORGANIZATION, "orga-uuid")
      .hasParam(PARAM_SELECTED, "all")
      .hasParam(TEXT_QUERY, "sonar-users")
      .hasParam(PAGE, 10)
      .hasParam(PAGE_SIZE, 50)
      .andNoOtherParam();
  }

  @Test
  public void current() {
    underTest.current();

    assertThat(serviceTester.getGetParser()).isSameAs(WsUsers.CurrentWsResponse.parser());
  }
}

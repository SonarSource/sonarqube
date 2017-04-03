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

package org.sonarqube.ws.client.usergroup;

import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.WsUserGroups.CreateWsResponse;
import static org.sonarqube.ws.WsUserGroups.SearchWsResponse;
import static org.sonarqube.ws.WsUserGroups.UpdateWsResponse;

public class UserGroupsServiceTest {

  @Rule
  public ServiceTester<UserGroupsService> serviceTester = new ServiceTester<>(new UserGroupsService(mock(WsConnector.class)));

  private UserGroupsService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void create() {
    underTest.create(CreateWsRequest.builder()
      .setName("sonar-users")
      .setDescription("All users")
      .setOrganization("org")
      .build());

    assertThat(serviceTester.getPostParser()).isSameAs(CreateWsResponse.parser());
    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasParam("name", "sonar-users")
      .hasParam("description", "All users")
      .hasParam("organization", "org")
      .andNoOtherParam();
  }

  @Test
  public void update() {
    underTest.update(UpdateWsRequest.builder()
      .setId(10L)
      .setName("sonar-users")
      .setDescription("All users")
      .build());

    assertThat(serviceTester.getPostParser()).isSameAs(UpdateWsResponse.parser());
    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasParam("id", "10")
      .hasParam("name", "sonar-users")
      .hasParam("description", "All users")
      .andNoOtherParam();
  }

  @Test
  public void delete() {
    underTest.delete(DeleteWsRequest.builder()
      .setId(10L)
      .setName("sonar-users")
      .setOrganization("orga")
      .build());

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasParam("id", "10")
      .hasParam("name", "sonar-users")
      .hasParam("organization", "orga")
      .andNoOtherParam();
  }

  @Test
  public void addUser() throws Exception {
    underTest.addUser(AddUserWsRequest.builder()
      .setId(10L)
      .setName("sonar-users")
      .setLogin("john")
      .setOrganization("org")
      .build());

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasParam("id", "10")
      .hasParam("name", "sonar-users")
      .hasParam("login", "john")
      .hasParam("organization", "org")
      .andNoOtherParam();
  }

  @Test
  public void removeUser() throws Exception {
    underTest.removeUser(RemoveUserWsRequest.builder()
      .setId(10L)
      .setName("sonar-users")
      .setLogin("john")
      .setOrganization("org")
      .build());

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasParam("id", "10")
      .hasParam("name", "sonar-users")
      .hasParam("login", "john")
      .hasParam("organization", "org")
      .andNoOtherParam();
  }

  @Test
  public void search() {
    underTest.search(SearchWsRequest.builder()
      .setQuery("sonar-users")
      .setPage(10)
      .setPageSize(50)
      .setOrganization("orga")
      .setFields(asList("name", "description"))
      .build());

    assertThat(serviceTester.getGetParser()).isSameAs(SearchWsResponse.parser());
    serviceTester.assertThat(serviceTester.getGetRequest())
      .hasParam("q", "sonar-users")
      .hasParam("p", 10)
      .hasParam("ps", 50)
      .hasParam("organization", "orga")
      .hasParam("f", "name,description")
      .andNoOtherParam();
  }

}

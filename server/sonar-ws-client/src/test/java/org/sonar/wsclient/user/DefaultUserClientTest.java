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
package org.sonar.wsclient.user;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.user.internal.DefaultUserClient;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class DefaultUserClientTest {

  HttpRequestFactory requestFactory;
  DefaultUserClient client;

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Before
  public void setUp() {
    requestFactory = new HttpRequestFactory(httpServer.url());
    client = new DefaultUserClient(requestFactory);
  }

  @Test
  public void should_find_issues() {
    httpServer.stubResponseBody("{\"users\": [{\"login\": \"simon\", \"name\": \"Simon\", \"active\": true}]}");

    UserQuery query = UserQuery.create().logins("simon", "loic");
    List<User> users = client.find(query);

    assertThatGetRequestUrlContains("/api/users/search?", "logins=simon,loic");
    assertThat(users).hasSize(1);
    User simon = users.get(0);
    assertThat(simon.login()).isEqualTo("simon");
    assertThat(simon.name()).isEqualTo("Simon");
    assertThat(simon.email()).isNull();
    assertThat(simon.active()).isTrue();
  }

  @Test
  public void should_create_user() throws Exception {
    httpServer.stubResponseBody("{\"user\":{\"login\":\"daveloper\",\"name\":\"daveloper\",\"email\":null}}");

    UserParameters params = UserParameters.create().login("daveloper").password("pass1").passwordConfirmation("pass1");
    User createdUser = client.create(params);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/users/create");
    assertThat(httpServer.requestParams()).contains(
      entry("login", "daveloper"),
      entry("password", "pass1"),
      entry("password_confirmation", "pass1")
      );
    assertThat(createdUser).isNotNull();
    assertThat(createdUser.login()).isEqualTo("daveloper");
    assertThat(createdUser.name()).isEqualTo("daveloper");
    assertThat(createdUser.email()).isNull();
  }

  @Test
  public void should_update_user() throws Exception {
    httpServer.stubResponseBody("{\"user\":{\"login\":\"daveloper\",\"name\":\"daveloper\",\"email\":\"new_email\"}}");

    UserParameters params = UserParameters.create().login("daveloper").email("new_email");
    User updatedUser = client.update(params);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/users/update");
    assertThat(httpServer.requestParams()).contains(
      entry("login", "daveloper"),
      entry("email", "new_email")
      );
    assertThat(updatedUser).isNotNull();
    assertThat(updatedUser.login()).isEqualTo("daveloper");
    assertThat(updatedUser.name()).isEqualTo("daveloper");
    assertThat(updatedUser.email()).isEqualTo("new_email");
  }

  @Test
  public void should_deactivate_user() throws Exception {
    httpServer.stubStatusCode(200);

    client.deactivate("daveloper");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/users/deactivate");
    assertThat(httpServer.requestParams()).containsEntry("login", "daveloper");
  }

  private void assertThatGetRequestUrlContains(String baseUrl, String... parameters) {
    assertThat(httpServer.requestedPath()).startsWith(baseUrl);
    List<String> requestParameters = Arrays.asList(httpServer.requestedPath().substring(baseUrl.length()).split("&"));
    assertThat(requestParameters).containsOnly(parameters);
  }
}

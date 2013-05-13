/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.internal.HttpRequestFactory;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultUserClientTest {
  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void should_find_issues() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"users\": [{\"login\": \"simon\", \"name\": \"Simon\", \"active\": true}]}");

    UserClient client = new DefaultUserClient(requestFactory);
    UserQuery query = UserQuery.create().logins("simon", "loic");
    List<User> users = client.find(query);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/users/search?logins=simon,loic");
    assertThat(users).hasSize(1);
    User simon = users.get(0);
    assertThat(simon.login()).isEqualTo("simon");
    assertThat(simon.name()).isEqualTo("Simon");
    assertThat(simon.active()).isTrue();
  }
}

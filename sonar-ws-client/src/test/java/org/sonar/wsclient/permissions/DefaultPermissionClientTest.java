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

package org.sonar.wsclient.permissions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.permissions.internal.DefaultPermissionClient;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

public class DefaultPermissionClientTest {

  HttpRequestFactory requestFactory;
  DefaultPermissionClient client;

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Before
  public void setUp() {
    requestFactory = new HttpRequestFactory(httpServer.url());
    client = new DefaultPermissionClient(requestFactory);
  }

  @Test
  public void should_add_global_user_permission() {
    httpServer.stubStatusCode(200);

    PermissionParameters params = PermissionParameters.create().user("daveloper").permission("admin");
    client.addPermission(params);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/permissions/add");
    assertThat(httpServer.requestParams()).includes(
      entry("user", "daveloper"),
      entry("permission", "admin")
    );
  }

  @Test
  public void should_add_component_user_permission() {
    httpServer.stubStatusCode(200);

    PermissionParameters params = PermissionParameters.create().user("daveloper").component("org.sample.Sample").permission("admin");
    client.addPermission(params);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/permissions/add");
    assertThat(httpServer.requestParams()).includes(
      entry("user", "daveloper"),
      entry("component", "org.sample.Sample"),
      entry("permission", "admin")
    );
  }

  @Test
  public void should_add_global_group_permission() {
    httpServer.stubStatusCode(200);

    PermissionParameters params = PermissionParameters.create().group("my_group").permission("admin");
    client.addPermission(params);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/permissions/add");
    assertThat(httpServer.requestParams()).includes(
      entry("group", "my_group"),
      entry("permission", "admin")
    );
  }

  @Test
  public void should_add_component_group_permission() {
    httpServer.stubStatusCode(200);

    PermissionParameters params = PermissionParameters.create().group("my_group").component("org.sample.Sample").permission("admin");
    client.addPermission(params);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/permissions/add");
    assertThat(httpServer.requestParams()).includes(
      entry("group", "my_group"),
      entry("component", "org.sample.Sample"),
      entry("permission", "admin")
    );
  }

  @Test
  public void should_remove_global_user_permission() {
    httpServer.stubStatusCode(200);

    PermissionParameters params = PermissionParameters.create().user("daveloper").permission("admin");
    client.removePermission(params);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/permissions/remove");
    assertThat(httpServer.requestParams()).includes(
      entry("user", "daveloper"),
      entry("permission", "admin")
    );
  }

  @Test
  public void should_remove_component_user_permission() {
    httpServer.stubStatusCode(200);

    PermissionParameters params = PermissionParameters.create().user("daveloper").component("org.sample.Sample").permission("admin");
    client.removePermission(params);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/permissions/remove");
    assertThat(httpServer.requestParams()).includes(
      entry("user", "daveloper"),
      entry("component", "org.sample.Sample"),
      entry("permission", "admin")
    );
  }

  @Test
  public void should_remove_global_group_permission() {
    httpServer.stubStatusCode(200);

    PermissionParameters params = PermissionParameters.create().group("my_group").permission("admin");
    client.removePermission(params);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/permissions/remove");
    assertThat(httpServer.requestParams()).includes(
      entry("group", "my_group"),
      entry("permission", "admin")
    );
  }

  @Test
  public void should_remove_component_group_permission() {
    httpServer.stubStatusCode(200);

    PermissionParameters params = PermissionParameters.create().group("my_group").component("org.sample.Sample").permission("admin");
    client.removePermission(params);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/permissions/remove");
    assertThat(httpServer.requestParams()).includes(
      entry("group", "my_group"),
      entry("component", "org.sample.Sample"),
      entry("permission", "admin")
    );
  }
}

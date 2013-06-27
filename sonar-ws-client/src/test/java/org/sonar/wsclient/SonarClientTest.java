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
package org.sonar.wsclient;

import org.junit.Test;
import org.sonar.wsclient.issue.internal.DefaultActionPlanClient;
import org.sonar.wsclient.issue.internal.DefaultIssueClient;
import org.sonar.wsclient.permissions.DefaultPermissionClient;
import org.sonar.wsclient.user.DefaultUserClient;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class SonarClientTest {
  @Test
  public void should_build_clients() {
    SonarClient client = SonarClient.create("http://localhost:9000");
    assertThat(client.issueClient()).isNotNull().isInstanceOf(DefaultIssueClient.class);
    assertThat(client.actionPlanClient()).isNotNull().isInstanceOf(DefaultActionPlanClient.class);
    assertThat(client.userClient()).isNotNull().isInstanceOf(DefaultUserClient.class);
    assertThat(client.permissionClient()).isNotNull().isInstanceOf(DefaultPermissionClient.class);
  }

  @Test
  public void url_should_not_be_null() {
    try {
      SonarClient.builder().build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Server URL must be set");
    }
  }

  @Test
  public void url_should_not_be_empty() {
    try {
      SonarClient.create("");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Server URL must be set");
    }
  }

  @Test
  public void test_default_configuration() throws Exception {
    SonarClient client = SonarClient.create("http://localhost:9000");
    assertThat(client.requestFactory.getBaseUrl()).isEqualTo("http://localhost:9000");
    assertThat(client.requestFactory.getLogin()).isNull();
    assertThat(client.requestFactory.getPassword()).isNull();
    assertThat(client.requestFactory.getConnectTimeoutInMilliseconds()).isEqualTo(SonarClient.DEFAULT_CONNECT_TIMEOUT_MILLISECONDS);
    assertThat(client.requestFactory.getReadTimeoutInMilliseconds()).isEqualTo(SonarClient.DEFAULT_READ_TIMEOUT_MILLISECONDS);
    assertThat(client.requestFactory.getProxyHost()).isNull();
    assertThat(client.requestFactory.getProxyPort()).isEqualTo(0);
    assertThat(client.requestFactory.getProxyLogin()).isNull();
    assertThat(client.requestFactory.getProxyPassword()).isNull();

  }

  @Test
  public void test_custom_configuration() throws Exception {
    SonarClient client = SonarClient.builder().url("http://localhost:9000")
      .login("eric")
      .password("pass")
      .connectTimeoutMilliseconds(12345)
      .readTimeoutMilliseconds(6789)
      .proxy("localhost", 2052)
      .proxyLogin("proxyLogin")
      .proxyPassword("proxyPass")
      .build();
    assertThat(client.requestFactory.getBaseUrl()).isEqualTo("http://localhost:9000");
    assertThat(client.requestFactory.getLogin()).isEqualTo("eric");
    assertThat(client.requestFactory.getPassword()).isEqualTo("pass");
    assertThat(client.requestFactory.getConnectTimeoutInMilliseconds()).isEqualTo(12345);
    assertThat(client.requestFactory.getReadTimeoutInMilliseconds()).isEqualTo(6789);
    assertThat(client.requestFactory.getProxyHost()).isEqualTo("localhost");
    assertThat(client.requestFactory.getProxyPort()).isEqualTo(2052);
    assertThat(client.requestFactory.getProxyLogin()).isEqualTo("proxyLogin");
    assertThat(client.requestFactory.getProxyPassword()).isEqualTo("proxyPass");
  }
}

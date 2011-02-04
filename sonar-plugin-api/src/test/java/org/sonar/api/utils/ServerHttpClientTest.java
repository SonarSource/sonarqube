/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class ServerHttpClientTest {

  private String serverUrl = "http://test";
  
  private ServerHttpClient serverHttpClient;

  @Before
  public void before() {
    serverHttpClient = new ServerHttpClient(serverUrl);
  }

  @Test
  public void shouldReturnAValidResult() throws IOException {
    final String validContent = "valid";
    ServerHttpClient serverHttpClient = new ServerHttpClient(serverUrl) {
      @Override
      protected String getRemoteContent(String url) {
        return (validContent);
      }
    };

    assertThat(serverHttpClient.executeAction("an action"), is(validContent));
  }

  @Test
  public void shouldRemoveLastUrlSlash() {
    ServerHttpClient serverHttpClient = new ServerHttpClient(serverUrl + "/");
    assertThat(serverHttpClient.getUrl(), is(serverUrl));
  }

  @Test(expected = ServerHttpClient.ServerApiEmptyContentException.class)
  public void shouldThrowAnExceptionIfResultIsEmpty() throws IOException {
    final String invalidContent = " ";
    ServerHttpClient serverHttpClient = new ServerHttpClient(serverUrl) {
      @Override
      protected String getRemoteContent(String url) {
        return (invalidContent);
      }
    };
    serverHttpClient.executeAction("an action");
  }

  @Test
  public void shouldReturnMavenRepositoryUrl() {
    String sonarRepo = serverHttpClient.getMavenRepositoryUrl();
    assertThat(sonarRepo, is(serverUrl + ServerHttpClient.MAVEN_PATH));
  }

  @Test(expected = ServerHttpClient.ServerConnectionException.class)
  public void shouldFailIfCanNotConnectToServer() {
    ServerHttpClient serverHttpClient = new ServerHttpClient("fake") {
      @Override
      protected String getRemoteContent(String url) {
        throw new ServerConnectionException("");
      }
    };
    serverHttpClient.checkUp();
  }

}

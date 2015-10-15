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
package org.sonarqube.ws.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpRequestFactoryTest {
  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_get() {
    httpServer.stubStatusCode(200).stubResponseBody("{'issues': []}");

    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url());
    String json = factory.execute(new WsRequest("/api/issues"));

    assertThat(json).isEqualTo("{'issues': []}");
    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues");
  }

  @Test
  public void should_throw_illegal_state_exc_if_connect_exception() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to request http://localhost:1/api/issues");

    HttpRequestFactory factory = new HttpRequestFactory("http://localhost:1");
    factory.execute(new WsRequest("/api/issues"));
  }

  @Test
  public void test_authentication() {
    httpServer.stubStatusCode(200).stubResponseBody("{}");

    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url()).setLogin("karadoc").setPassword("legrascestlavie");
    String json = factory.execute(new WsRequest("/api/issues"));

    assertThat(json).isEqualTo("{}");
    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues");
    assertThat(httpServer.requestHeaders().get("Authorization")).isEqualTo("Basic a2FyYWRvYzpsZWdyYXNjZXN0bGF2aWU=");
  }

  @Test
  public void test_proxy() throws Exception {
    expectedException.expect(IllegalStateException.class);

    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url())
      .setProxyHost("localhost").setProxyPort(1)
      .setProxyLogin("john").setProxyPassword("smith");
    factory.execute(new WsRequest("/api/issues"));
  }

  @Test
  public void beginning_slash_is_optional() throws Exception {
    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url());
    factory.execute(new WsRequest("api/foo"));
    assertThat(httpServer.requestedPath()).isEqualTo("/api/foo");

    factory.execute(new WsRequest("/api/bar"));
    assertThat(httpServer.requestedPath()).isEqualTo("/api/bar");
  }
}

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
package org.sonar.wsclient.internal;

import com.github.kevinsawicki.http.HttpRequest;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class HttpRequestFactoryTest {
  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void test_get() {
    httpServer.doReturnStatus(200).doReturnBody("list of issues");

    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url());
    HttpRequest request = factory.get("/api/issues", Collections.<String, Object>emptyMap());

    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.body()).isEqualTo("list of issues");
    assertThat(request.code()).isEqualTo(200);
    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues");
  }

  @Test
  public void test_post() {
    httpServer.doReturnStatus(200);

    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url());
    HttpRequest request = factory.post("/api/issues/change", Collections.<String, Object>emptyMap());

    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.code()).isEqualTo(200);
    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/change");
  }

  @Test
  public void test_authentication() {
    httpServer.doReturnStatus(200).doReturnBody("list of issues");

    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url()).setLogin("karadoc").setPassword("legrascestlavie");
    HttpRequest request = factory.get("/api/issues", Collections.<String, Object>emptyMap());

    assertThat(request.body()).isEqualTo("list of issues");
    assertThat(request.code()).isEqualTo(200);
    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues");
    assertThat(httpServer.requestHeaders().get("Authorization")).isEqualTo("Basic a2FyYWRvYzpsZWdyYXNjZXN0bGF2aWU=");
  }

  @Test
  public void test_proxy() throws Exception {
    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url()).setProxyHost("localhost").setProxyPort(5020);
    HttpRequest request = factory.get("/api/issues", Collections.<String, Object>emptyMap());
    // it's not possible to check that the proxy is correctly configured
  }

  @Test
  public void test_proxy_credentials() throws Exception {
    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url())
      .setProxyHost("localhost").setProxyPort(5020)
      .setProxyLogin("john").setProxyPassword("smith");
    HttpRequest request = factory.get("/api/issues", Collections.<String, Object>emptyMap());
    // it's not possible to check that the proxy is correctly configured
  }
}

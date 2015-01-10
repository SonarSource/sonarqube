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
package org.sonar.wsclient.internal;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.internal.DefaultIssueClient;

import java.net.ConnectException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class HttpRequestFactoryTest {
  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void test_get() {
    httpServer.stubStatusCode(200).stubResponseBody("{'issues': []}");

    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url());
    String json = factory.get("/api/issues", Collections.<String, Object>emptyMap());

    assertThat(json).isEqualTo("{'issues': []}");
    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues");
  }

  @Test
  public void should_throw_illegal_state_exc_if_connect_exception() {
    HttpRequestFactory factory = new HttpRequestFactory("http://localhost:1");
    try {
      factory.get("/api/issues", Collections.<String, Object>emptyMap());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e).hasMessage("Fail to request http://localhost:1/api/issues");
      assertThat(e.getCause().getMessage()).matches(".*(Connection refused|Connexion refusée).*");
    }
  }

  @Test
  public void test_post() {
    httpServer.stubStatusCode(200).stubResponseBody("{}");

    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url());
    String json = factory.post("/api/issues/change", Collections.<String, Object>emptyMap());

    assertThat(json).isEqualTo("{}");
    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/change");
  }

  @Test
  public void post_successful_if_created() {
    httpServer.stubStatusCode(201).stubResponseBody("{}");

    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url());
    String json = factory.post("/api/issues/change", Collections.<String, Object>emptyMap());

    assertThat(json).isEqualTo("{}");
    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/change");
  }

  @Test
  public void post_successful_if_no_content() {
    httpServer.stubStatusCode(204).stubResponseBody("");

    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url());
    String json = factory.post("/api/issues/change", Collections.<String, Object>emptyMap());

    assertThat(json).isEqualTo("");
    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/change");
  }

  @Test
  public void test_authentication() {
    httpServer.stubStatusCode(200).stubResponseBody("{}");

    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url()).setLogin("karadoc").setPassword("legrascestlavie");
    String json = factory.get("/api/issues", Collections.<String, Object>emptyMap());

    assertThat(json).isEqualTo("{}");
    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues");
    assertThat(httpServer.requestHeaders().get("Authorization")).isEqualTo("Basic a2FyYWRvYzpsZWdyYXNjZXN0bGF2aWU=");
  }

  @Test
  public void test_proxy() throws Exception {
    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url())
      .setProxyHost("localhost").setProxyPort(1)
      .setProxyLogin("john").setProxyPassword("smith");
    try {
      factory.get("/api/issues", Collections.<String, Object>emptyMap());
      fail();
    } catch (IllegalStateException e) {
      // it's not possible to check that the proxy is correctly configured
      assertThat(e.getCause()).isInstanceOf(ConnectException.class);
    }
  }

  @Test
  public void beginning_slash_is_optional() throws Exception {
    HttpRequestFactory factory = new HttpRequestFactory(httpServer.url());
    factory.get("api/foo", Collections.<String, Object>emptyMap());
    assertThat(httpServer.requestedPath()).isEqualTo("/api/foo");

    factory.get("/api/bar", Collections.<String, Object>emptyMap());
    assertThat(httpServer.requestedPath()).isEqualTo("/api/bar");
  }

  @Test
  public void should_encode_characters() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"issues\": [{\"key\": \"ABCDE\"}]}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    client.find(IssueQuery.create().issues("ABC DE"));
    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/search?issues=ABC%20DE");

    client.find(IssueQuery.create().issues("ABC+BDE"));
    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/search?issues=ABC%2BBDE");

    client.find(IssueQuery.create().createdAfter(toDate("2013-01-01")));
    // TODO complete assertion with timestamp when test is isolated from default timezone
    assertThat(httpServer.requestedPath()).startsWith("/api/issues/search?createdAfter=2013-01-01T");
  }

  protected static Date toDate(String sDate) {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      return sdf.parse(sDate);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }
}

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
package org.sonar.wsclient.rule.internal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.internal.HttpRequestFactory;

import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

public class DefaultRuleTagClientTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  private DefaultRuleTagClient client;

  @Before
  public void initClient() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    this.client = new DefaultRuleTagClient(requestFactory);
  }

  @Test
  public void should_list_tags() {
    httpServer.stubResponseBody("[\"tag1\",\"tag2\",\"tag3\"]");

    Collection<String> result = client.list();

    assertThat(httpServer.requestedPath()).isEqualTo("/api/rule_tags/list");
    assertThat(result).containsOnly("tag1", "tag2", "tag3");
  }

  @Test
  public void should_create_rule_tag() {
    httpServer.stubStatusCode(200);

    final String newTag = "polop";
    client.create(newTag);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/rule_tags/create");
    assertThat(httpServer.requestParams()).includes(entry("tag", newTag));
  }

}

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

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

public class DefaultRuleClientTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  private DefaultRuleClient client;

  @Before
  public void initClient() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    this.client = new DefaultRuleClient(requestFactory);
  }

  @Test
  public void should_add_tags() {
    httpServer.stubStatusCode(200);

    final String ruleKey = "repo:rule1";
    client.addTags(ruleKey, "tag1", "tag2", "tag3");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/rules/add_tags");
    assertThat(httpServer.requestParams()).includes(
      entry("key", ruleKey),
      entry("tags", "tag1,tag2,tag3"));
  }

  @Test
  public void should_remove_tags() {
    httpServer.stubStatusCode(200);

    final String ruleKey = "repo:rule1";
    client.removeTags(ruleKey, "tag1", "tag2", "tag3");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/rules/remove_tags");
    assertThat(httpServer.requestParams()).includes(
      entry("key", ruleKey),
      entry("tags", "tag1,tag2,tag3"));
  }

}

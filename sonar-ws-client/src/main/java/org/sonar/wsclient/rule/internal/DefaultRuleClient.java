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

import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.rule.RuleClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Do not instantiate this class, but use {@link org.sonar.wsclient.SonarClient#ruleClient()}.
 */
public class DefaultRuleClient implements RuleClient {

  private static final String ROOT_URL = "/api/rules";
  private static final String ADD_TAGS_URL = ROOT_URL + "/add_tags";
  private static final String REMOVE_TAGS_URL = ROOT_URL + "/remove_tags";

  private final HttpRequestFactory requestFactory;

  public DefaultRuleClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public void addTags(String key, String... tags) {
    requestFactory.post(ADD_TAGS_URL, buildQueryParams(key, tags));
  }

  @Override
  public void removeTags(String key, String... tags) {
    requestFactory.post(REMOVE_TAGS_URL, buildQueryParams(key, tags));
  }

  private Map<String, Object> buildQueryParams(String key, String... tags) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("key", key);
    StringBuilder tagsBuilder = new StringBuilder();
    for (int i=0; i < tags.length - 1; i++) {
      tagsBuilder.append(tags[i]).append(",");
    }
    tagsBuilder.append(tags[tags.length - 1]);
    params.put("tags", tagsBuilder.toString());
    return params;
  }
}

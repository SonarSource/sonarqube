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
package org.sonar.wsclient.issue;

import org.sonar.wsclient.unmarshallers.JsonUtils;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * @since 3.6
 */
public class Issue {

  private final Map json;

  Issue(Map json) {
    this.json = json;
  }

  /**
   * Unique key
   */
  public String key() {
    return JsonUtils.getString(json, "key");
  }

  public String componentKey() {
    return JsonUtils.getString(json, "component");
  }

  public String ruleKey() {
    return JsonUtils.getString(json, "rule");
  }

  public String severity() {
    return JsonUtils.getString(json, "severity");
  }

  public String title() {
    return JsonUtils.getString(json, "title");
  }

  public String description() {
    return JsonUtils.getString(json, "desc");
  }

  public Integer line() {
    return JsonUtils.getInteger(json, "line");
  }

  public Double cost() {
    return JsonUtils.getDouble(json, "cost");
  }

  public String status() {
    return JsonUtils.getString(json, "status");
  }

  public String resolution() {
    return JsonUtils.getString(json, "resolution");
  }

  public String userLogin() {
    return JsonUtils.getString(json, "userLogin");
  }

  public String assignee() {
    return JsonUtils.getString(json, "assignee");
  }

  public Date createdAt() {
    return JsonUtils.getDateTime(json, "createdAt");
  }

  public Date updatedAt() {
    return JsonUtils.getDateTime(json, "updatedAt");
  }

  public Date closedAt() {
    return JsonUtils.getDateTime(json, "closedAt");
  }

  public String attribute(String key) {
    return attributes().get(key);
  }

  public Map<String, String> attributes() {
    Map<String, String> attr = (Map) json.get("attr");
    if (attr == null) {
      return Collections.emptyMap();
    }
    return attr;
  }
}

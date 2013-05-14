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

import javax.annotation.CheckForNull;

import java.util.Date;
import java.util.Map;

/**
 * @since 3.6
 */
public class ActionPlan {

  private final Map json;

  ActionPlan(Map json) {
    this.json = json;
  }

  /**
   * Unique key
   */
  public String key() {
    return JsonUtils.getString(json, "key");
  }

  public String project() {
    return JsonUtils.getString(json, "project");
  }

  public String name() {
    return JsonUtils.getString(json, "name");
  }

  @CheckForNull
  public String description() {
    return JsonUtils.getString(json, "desc");
  }

  public String status() {
    return JsonUtils.getString(json, "status");
  }

  /**
   * Login of the user who created the action plan.
   */
  public String userLogin() {
    return JsonUtils.getString(json, "userLogin");
  }

  @CheckForNull
  public Date deadLine() {
    return JsonUtils.getDateTime(json, "deadLine");
  }

  public Date createdAt() {
    return JsonUtils.getDateTime(json, "createdAt");
  }

  public Date updatedAt() {
    return JsonUtils.getDateTime(json, "updatedAt");
  }

  public Integer totalIssues() {
    return JsonUtils.getInteger(json, "totalIssues");
  }

  public Integer openIssues() {
    return JsonUtils.getInteger(json, "openIssues");
  }


}

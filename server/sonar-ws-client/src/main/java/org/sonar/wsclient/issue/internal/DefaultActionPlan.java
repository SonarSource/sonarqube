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
package org.sonar.wsclient.issue.internal;

import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.unmarshallers.JsonUtils;

import javax.annotation.CheckForNull;
import java.util.Date;
import java.util.Map;

/**
 * @since 3.6
 */
public class DefaultActionPlan implements ActionPlan {

  private final Map json;

  DefaultActionPlan(Map json) {
    this.json = json;
  }

  /**
   * Unique key
   */
  @Override
  public String key() {
    return JsonUtils.getString(json, "key");
  }

  @Override
  public String project() {
    return JsonUtils.getString(json, "project");
  }

  @Override
  public String name() {
    return JsonUtils.getString(json, "name");
  }

  @Override
  @CheckForNull
  public String description() {
    return JsonUtils.getString(json, "desc");
  }

  @Override
  public String status() {
    return JsonUtils.getString(json, "status");
  }

  /**
   * Login of the user who created the action plan.
   */
  @Override
  public String userLogin() {
    return JsonUtils.getString(json, "userLogin");
  }

  @Override
  @CheckForNull
  public Date deadLine() {
    return JsonUtils.getDateTime(json, "deadLine");
  }

  @Override
  public Date createdAt() {
    return JsonUtils.getDateTime(json, "createdAt");
  }

  @Override
  public Date updatedAt() {
    return JsonUtils.getDateTime(json, "updatedAt");
  }

  @Override
  @CheckForNull
  public Integer totalIssues() {
    return JsonUtils.getInteger(json, "totalIssues");
  }

  @Override
  @CheckForNull
  public Integer unresolvedIssues() {
    return JsonUtils.getInteger(json, "unresolvedIssues");
  }

}

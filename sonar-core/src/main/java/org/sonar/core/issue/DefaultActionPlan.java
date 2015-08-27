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

package org.sonar.core.issue;

import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.ActionPlan;
import org.sonar.core.util.Uuids;

public class DefaultActionPlan implements ActionPlan {

  private String key;
  private String name;
  private String projectKey;
  private String description;
  private String userLogin;
  private String status;
  private Date deadLine;
  private Date createdAt;
  private Date updatedAt;

  public DefaultActionPlan() {

  }

  public static DefaultActionPlan create(String name) {
    DefaultActionPlan actionPlan = new DefaultActionPlan();
    actionPlan.setKey(Uuids.create());
    Date now = new Date();
    actionPlan.setName(name);
    actionPlan.setStatus(ActionPlan.STATUS_OPEN);
    actionPlan.setCreatedAt(now).setUpdatedAt(now);
    return actionPlan;
  }

  @Override
  public String key() {
    return key;
  }

  public DefaultActionPlan setKey(String key) {
    this.key = key;
    return this;
  }

  @Override
  public String name() {
    return name;
  }

  public DefaultActionPlan setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public String projectKey() {
    return projectKey;
  }

  public DefaultActionPlan setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  @Override
  @CheckForNull
  public String description() {
    return description;
  }

  public DefaultActionPlan setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  @Override
  public String userLogin() {
    return userLogin;
  }

  public DefaultActionPlan setUserLogin(String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  @Override
  public String status() {
    return status;
  }

  public DefaultActionPlan setStatus(String status) {
    this.status = status;
    return this;
  }

  @Override
  @CheckForNull
  public Date deadLine() {
    return deadLine != null ? new Date(deadLine.getTime()) : null;
  }

  public DefaultActionPlan setDeadLine(@Nullable Date deadLine) {
    this.deadLine = deadLine != null ? new Date(deadLine.getTime()) : null;
    return this;
  }

  @Override
  public Date createdAt() {
    return createdAt;
  }

  public DefaultActionPlan setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @Override
  public Date updatedAt() {
    return updatedAt;
  }

  public DefaultActionPlan setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}

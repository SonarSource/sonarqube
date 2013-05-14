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

package org.sonar.core.issue;

import org.sonar.api.issue.ActionPlan;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.UUID;

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

  private DefaultActionPlan(){

  }

  public static DefaultActionPlan create(String name) {
    DefaultActionPlan actionPlan = new DefaultActionPlan();
    actionPlan.setKey(UUID.randomUUID().toString());
    Date now = new Date();
    actionPlan.setName(name);
    actionPlan.setStatus(ActionPlan.STATUS_OPEN);
    actionPlan.setCreatedAt(now).setUpdatedAt(now);
    return actionPlan;
  }

  public String key() {
    return key;
  }

  public DefaultActionPlan setKey(String key) {
    this.key = key;
    return this;
  }

  public String name() {
    return name;
  }

  public DefaultActionPlan setName(String name) {
    this.name = name;
    return this;
  }

  public String projectKey() {
    return projectKey;
  }

  public DefaultActionPlan setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  @CheckForNull
  public String description() {
    return description;
  }

  public DefaultActionPlan setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public String userLogin() {
    return userLogin;
  }

  public DefaultActionPlan setUserLogin(String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public String status() {
    return status;
  }

  public DefaultActionPlan setStatus(String status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public Date deadLine() {
    return deadLine != null ? new Date(deadLine.getTime()) : null;
  }

  public DefaultActionPlan setDeadLine(@Nullable Date deadLine) {
    this.deadLine = deadLine != null ? new Date(deadLine.getTime()) : null;
    return this;
  }

  public Date createdAt() {
    return createdAt;
  }

  public DefaultActionPlan setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public DefaultActionPlan setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}

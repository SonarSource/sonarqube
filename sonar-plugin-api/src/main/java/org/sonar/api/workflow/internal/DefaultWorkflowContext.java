/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.workflow.internal;

import com.google.common.annotations.Beta;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.config.Settings;
import org.sonar.api.workflow.WorkflowContext;

/**
 * @since 3.1
 */
@Beta
public final class DefaultWorkflowContext implements WorkflowContext {

  private Long userId;
  private String userLogin;
  private String userName;
  private String userEmail;
  private boolean isAdmin = false;
  private Long projectId;
  private Settings settings;

  public Long getUserId() {
    return userId;
  }

  public DefaultWorkflowContext setUserId(Long l) {
    this.userId = l;
    return this;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public DefaultWorkflowContext setUserLogin(String s) {
    this.userLogin = s;
    return this;
  }

  public String getUserName() {
    return userName;
  }

  public DefaultWorkflowContext setUserName(String s) {
    this.userName = s;
    return this;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public DefaultWorkflowContext setUserEmail(String userEmail) {
    this.userEmail = userEmail;
    return this;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public DefaultWorkflowContext setIsAdmin(boolean b) {
    isAdmin = b;
    return this;
  }

  public Long getProjectId() {
    return projectId;
  }

  public DefaultWorkflowContext setProjectId(Long l) {
    this.projectId = l;
    return this;
  }

  public Settings getProjectSettings() {
    return settings;
  }

  public DefaultWorkflowContext setSettings(Settings s) {
    this.settings = s;
    return this;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).toString();
  }
}

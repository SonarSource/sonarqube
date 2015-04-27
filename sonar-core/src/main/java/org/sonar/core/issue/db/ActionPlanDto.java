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

package org.sonar.core.issue.db;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.issue.ActionPlan;
import org.sonar.core.issue.DefaultActionPlan;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;

/**
 * @since 3.6
 */
public class ActionPlanDto {

  private Long id;
  private String kee;
  private String name;
  private String description;
  private String userLogin;
  private Long projectId;
  private String status;
  private Date deadLine;
  private Date createdAt;
  private Date updatedAt;

  // return by joins
  private String projectKey;

  public Long getId() {
    return id;
  }

  public ActionPlanDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getKey() {
    return kee;
  }

  public ActionPlanDto setKey(String kee) {
    this.kee = kee;
    return this;
  }

  public String getName() {
    return name;
  }

  public ActionPlanDto setName(String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public ActionPlanDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public ActionPlanDto setUserLogin(String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public Long getProjectId() {
    return projectId;
  }

  public ActionPlanDto setProjectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public ActionPlanDto setStatus(String status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public Date getDeadLine() {
    return deadLine;
  }

  public ActionPlanDto setDeadLine(@Nullable Date deadLine) {
    this.deadLine = deadLine;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public ActionPlanDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public ActionPlanDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  /**
   * Only for unit tests
   */
  public ActionPlanDto setProjectKey_unit_test_only(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ActionPlanDto actionPlanDto = (ActionPlanDto) o;
    return !((id != null) ? !id.equals(actionPlanDto.id) : (actionPlanDto.id != null));
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public DefaultActionPlan toActionPlan() {
    return new DefaultActionPlan()
      .setName(name)
      .setKey(kee)
      .setProjectKey(projectKey)
      .setDescription(description)
      .setStatus(status)
      .setDeadLine(deadLine)
      .setUserLogin(userLogin)
      .setCreatedAt(createdAt)
      .setUpdatedAt(updatedAt);
  }

  public static ActionPlanDto toActionDto(ActionPlan actionPlan, Long projectId) {
    return new ActionPlanDto().setKey(actionPlan.key())
             .setName(actionPlan.name())
             .setProjectId(projectId)
             .setDescription(actionPlan.description())
             .setStatus(actionPlan.status())
             .setDeadLine(actionPlan.deadLine())
             .setUserLogin(actionPlan.userLogin())
             .setCreatedAt(actionPlan.createdAt())
             .setUpdatedAt(actionPlan.updatedAt());
  }
}

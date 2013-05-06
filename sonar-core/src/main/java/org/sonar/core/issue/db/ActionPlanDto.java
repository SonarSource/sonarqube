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

package org.sonar.core.issue.db;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.core.issue.DefaultActionPlan;

import java.util.Date;

/**
 * @since 3.6
 */
public class ActionPlanDto {

  private Integer id;
  private String kee;
  private String name;
  private String description;
  private String userLogin;
  private Integer projectId;
  private String status;
  private Date deadLine;
  private Date createdAt;
  private Date updatedAt;

  public Integer getId() {
    return id;
  }

  public ActionPlanDto setId(Integer id) {
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

  public String getDescription() {
    return description;
  }

  public ActionPlanDto setDescription(String description) {
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

  public Integer getProjectId() {
    return projectId;
  }

  public ActionPlanDto setProjectId(Integer projectId) {
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

  public Date getDeadLine() {
    return deadLine;
  }

  public ActionPlanDto setDeadLine(Date deadLine) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ActionPlanDto actionPlanDto = (ActionPlanDto) o;
    return !(id != null ? !id.equals(actionPlanDto.id) : actionPlanDto.id != null);
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
    return DefaultActionPlan.create(name)
      .setKey(kee)
      .setDescription(description)
      .setStatus(status)
      .setDeadLine(deadLine)
      .setUserLogin(userLogin)
      .setCreationDate(createdAt)
      .setUpdateDate(updatedAt);
  }
}

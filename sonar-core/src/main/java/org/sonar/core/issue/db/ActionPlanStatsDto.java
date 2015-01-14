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
import org.sonar.core.issue.ActionPlanStats;

import java.util.Date;

/**
 * @since 3.6
 */
public class ActionPlanStatsDto {

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
  private int totalIssues;
  private int unresolvedIssues;
  // return by joins
  private String projectKey;

  public Integer getId() {
    return id;
  }

  public ActionPlanStatsDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getKee() {
    return kee;
  }

  public ActionPlanStatsDto setKee(String kee) {
    this.kee = kee;
    return this;
  }

  public String getName() {
    return name;
  }

  public ActionPlanStatsDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public ActionPlanStatsDto setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public ActionPlanStatsDto setUserLogin(String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public ActionPlanStatsDto setProjectId(Integer projectId) {
    this.projectId = projectId;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public ActionPlanStatsDto setStatus(String status) {
    this.status = status;
    return this;
  }

  public Date getDeadLine() {
    return deadLine;
  }

  public ActionPlanStatsDto setDeadLine(Date deadLine) {
    this.deadLine = deadLine;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public ActionPlanStatsDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public ActionPlanStatsDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public int getTotalIssues() {
    return totalIssues;
  }

  public ActionPlanStatsDto setTotalIssues(int totalIssues) {
    this.totalIssues = totalIssues;
    return this;
  }

  public int getUnresolvedIssues() {
    return unresolvedIssues;
  }

  public ActionPlanStatsDto setUnresolvedIssues(int unresolvedIssues) {
    this.unresolvedIssues = unresolvedIssues;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  /**
   * Only for unit tests
   */
  public ActionPlanStatsDto setProjectKey_unit_test_only(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public ActionPlanStats toActionPlanStat() {
    return ((ActionPlanStats) ActionPlanStats.create(name)
             .setKey(kee)
             .setProjectKey(projectKey)
             .setDescription(description)
             .setStatus(status)
             .setDeadLine(deadLine)
             .setUserLogin(userLogin)
             .setCreatedAt(createdAt)
             .setUpdatedAt(updatedAt))
             .setTotalIssues(totalIssues)
             .setUnresolvedIssues(unresolvedIssues);
  }

}

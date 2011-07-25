/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.jpa.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

@Entity
@Table(name = "reviews")
public final class Review {

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Long id;

  @Column(name = "user_id")
  private Integer userId;

  @Column(name = "assignee_id")
  private Integer assigneeId;

  @Column(name = "title")
  private String title;

  @Column(name = "status")
  private String status;

  @Column(name = "resolution")
  private String resolution;

  @Column(name = "rule_failure_permanent_id")
  private Integer permanentId;

  @Column(name = "project_id")
  private Integer projectId;

  @Column(name = "resource_id")
  private Integer resourceId;

  @Column(name = "resource_line")
  private Integer resourceLine;

  @Column(name = "created_at")
  private Date createdAt;

  @Column(name = "updated_at")
  private Date updatedAt;

  @Column(name = "severity")
  private String severity;

  /**
   * @return id of review
   */
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return id of user, who created this review
   */
  public Integer getUserId() {
    return userId;
  }

  public Review setUserId(Integer userId) {
    this.userId = userId;
    return this;
  }

  /**
   * @return id of assigned user or null, if not assigned
   */
  public Integer getAssigneeId() {
    return assigneeId;
  }

  public Review setAssigneeId(Integer assigneeId) {
    this.assigneeId = assigneeId;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public Review setTitle(String title) {
    this.title = title;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getResolution() {
    return resolution;
  }

  public void setResolution(String resolution) {
    this.resolution = resolution;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}

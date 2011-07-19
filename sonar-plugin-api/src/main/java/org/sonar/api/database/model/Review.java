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
package org.sonar.api.database.model;

import javax.persistence.*;

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

}

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
package org.sonar.wsclient.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @since 2.8
 */
public class Review extends Model {

  private Long id;
  private Date createdAt = null;
  private Date updatedAt = null;
  private String authorLogin = null;
  private String assigneeLogin = null;
  private String title = null;
  private String type = null;
  private String status = null;
  private String severity = null;
  private String resourceKee = null;
  private Integer line = null;
  private String resolution = null;
  private Long violationId;
  private List<Review.Comment> comments = new ArrayList<Review.Comment>();

  /**
   * @return id
   */
  public Long getId() {
    return id;
  }

  public Review setId(Long id) {
    this.id = id;
    return this;
  }

  /**
   * @return date of creation
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  public Review setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * @return date of last modification
   */
  public Date getUpdatedAt() {
    return updatedAt;
  }

  public Review setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * @return user that initiated review
   */
  public String getAuthorLogin() {
    return authorLogin;
  }

  public Review setAuthorLogin(String s) {
    this.authorLogin = s;
    return this;
  }

  /**
   * @return assignee
   */
  public String getAssigneeLogin() {
    return assigneeLogin;
  }

  public Review setAssigneeLogin(String s) {
    this.assigneeLogin = s;
    return this;
  }

  /**
   * @return title
   */
  public String getTitle() {
    return title;
  }

  public Review setTitle(String s) {
    this.title = s;
    return this;
  }

  /**
   * @deprecated since 2.9.
   */
  @Deprecated
  public String getType() {
    return type;
  }

  /**
   * @deprecated since 2.9.
   */
  @Deprecated
  public Review setType(String s) {
    this.type = s;
    return this;
  }

  /**
   * @return status
   */
  public String getStatus() {
    return status;
  }

  public Review setStatus(String status) {
    this.status = status;
    return this;
  }

  /**
   * @return severity
   */
  public String getSeverity() {
    return severity;
  }

  public Review setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  /**
   * @return resourceKee
   */
  public String getResourceKee() {
    return resourceKee;
  }

  public Review setResourceKee(String resourceKee) {
    this.resourceKee = resourceKee;
    return this;
  }

  /**
   * @return line
   */
  public Integer getLine() {
    return line;
  }

  public Review setLine(Integer line) {
    this.line = line;
    return this;
  }

  /**
   * @since 2.9
   */
  public String getResolution() {
    return resolution;
  }

  /**
   * @since 2.9
   */
  public Review setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  /**
   * @since 2.9
   * @return violation id
   */
  public Long getViolationId() {
    return violationId;
  }

  public Review setViolationId(Long violationId) {
    this.violationId = violationId;
    return this;
  }

  /**
   * @return comments
   */
  public List<Review.Comment> getComments() {
    return comments;
  }

  public Review addComments(Long id, Date updatedAt, String authorLogin, String text) {
    this.comments.add(new Review.Comment(id, updatedAt, authorLogin, text));
    return this;
  }

  /**
   * @since 2.8
   */
  public static final class Comment extends Model {

    private Long id = null;
    private String authorLogin = null;
    private Date updatedAt = null;
    private String text = null;

    private Comment(Long id, Date updatedAt, String authorLogin, String text) {
      this.id = id;
      this.updatedAt = updatedAt;
      this.authorLogin = authorLogin;
      this.text = text;
    }

    /**
     * @since 2.9
     * @return id
     */
    public Long getId() {
      return id;
    }

    /**
     * @return user that created this comment
     */
    public String getAuthorLogin() {
      return authorLogin;
    }

    /**
     * @return date of last modification
     */
    public Date getUpdatedAt() {
      return updatedAt;
    }

    /**
     * @return text
     */
    public String getText() {
      return text;
    }
  }

}

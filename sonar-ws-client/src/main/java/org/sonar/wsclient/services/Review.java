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
  private List<Review.Comment> comments = new ArrayList<Review.Comment>();

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return the createdAt
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * @param createdAt
   *          the createdAt to set
   */
  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * @return the updatedAt
   */
  public Date getUpdatedAt() {
    return updatedAt;
  }

  /**
   * @param updatedAt
   *          the updatedAt to set
   */
  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * @return the authorLogin
   */
  public String getAuthorLogin() {
    return authorLogin;
  }

  /**
   * @param authorLogin
   *          the authorLogin to set
   */
  public void setAuthorLogin(String authorLogin) {
    this.authorLogin = authorLogin;
  }

  /**
   * @return the assigneeLogin
   */
  public String getAssigneeLogin() {
    return assigneeLogin;
  }

  /**
   * @param assigneeLogin
   *          the assigneeLogin to set
   */
  public void setAssigneeLogin(String assigneeLogin) {
    this.assigneeLogin = assigneeLogin;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title
   *          the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type
   *          the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return the status
   */
  public String getStatus() {
    return status;
  }

  /**
   * @param status
   *          the status to set
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * @return the severity
   */
  public String getSeverity() {
    return severity;
  }

  /**
   * @param severity
   *          the severity to set
   */
  public void setSeverity(String severity) {
    this.severity = severity;
  }

  /**
   * @return the resourceKee
   */
  public String getResourceKee() {
    return resourceKee;
  }

  /**
   * @param resourceKee
   *          the resourceKee to set
   */
  public void setResourceKee(String resourceKee) {
    this.resourceKee = resourceKee;
  }

  /**
   * @return the line
   */
  public Integer getLine() {
    return line;
  }

  /**
   * @param line
   *          the line to set
   */
  public void setLine(Integer line) {
    this.line = line;
  }

  /**
   * @return the comments
   */
  public List<Review.Comment> getComments() {
    return comments;
  }

  /**
   * @param comments
   *          the comments to set
   */
  public void addComments(Date updatedAt, String authorLogin, String text) {
    this.comments.add(new Review.Comment(updatedAt, authorLogin, text));
  }

  /**
   * @since 2.8
   */
  public class Comment extends Model {

    private String authorLogin = null;
    private Date updatedAt = null;
    private String text = null;

    private Comment(Date updatedAt, String authorLogin, String text) {
      this.updatedAt = updatedAt;
      this.authorLogin = authorLogin;
      this.text = text;
    }

    /**
     * @return the authorLogin
     */
    public String getAuthorLogin() {
      return authorLogin;
    }

    /**
     * @return the updatedAt
     */
    public Date getUpdatedAt() {
      return updatedAt;
    }

    /**
     * @return the text
     */
    public String getText() {
      return text;
    }
  }

}

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

/**
 * @since 2.9
 */
public class ReviewCreateQuery extends CreateQuery<Review> {

  private Long violationId;
  private String comment;
  private String assignee;
  private String status;
  private String resolution;

  public ReviewCreateQuery() {
  }

  public Long getViolationId() {
    return violationId;
  }

  public ReviewCreateQuery setViolationId(Long violationId) {
    this.violationId = violationId;
    return this;
  }

  public String getComment() {
    return comment;
  }

  public ReviewCreateQuery setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public String getAssignee() {
    return assignee;
  }

  public ReviewCreateQuery setAssignee(String userLogin) {
    this.assignee = userLogin;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public ReviewCreateQuery setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getResolution() {
    return resolution;
  }

  public ReviewCreateQuery setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder();
    url.append(ReviewQuery.BASE_URL).append('?');
    appendUrlParameter(url, "violation_id", getViolationId());
    appendUrlParameter(url, "assignee", getAssignee());
    appendUrlParameter(url, "status", getStatus());
    appendUrlParameter(url, "resolution", getResolution());
    return url.toString();
  }

  /**
   * Property {@link #comment} is transmitted through request body as content may exceed URL size allowed by the server.
   */
  @Override
  public String getBody() {
    return comment;
  }

  @Override
  public Class<Review> getModelClass() {
    return Review.class;
  }
}

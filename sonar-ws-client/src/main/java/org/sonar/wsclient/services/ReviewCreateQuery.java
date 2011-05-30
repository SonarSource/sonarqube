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

/**
 * @since 2.9
 */
public class ReviewCreateQuery extends CreateQuery<Review> {

  private Long violationId;
  private String text;
  private String assignee;
  private Boolean falsePositive;

  public ReviewCreateQuery() {
  }

  /**
   * Builds a request that will create a simple review on a violation, without any assignee.
   * 
   * @param violationId
   *          The id of the violation that is reviewed
   * @param text
   *          The comment of the review
   */
  public static ReviewCreateQuery createSimpleReviewQuery(Long violationId, String text) {
    ReviewCreateQuery query = new ReviewCreateQuery();
    query.setText(text);
    query.setViolationId(violationId);
    return query;
  }

  /**
   * Builds a request that will create a simple review on a violation and that will be assigned to the given user.
   * 
   * @param violationId
   *          The id of the violation that is reviewed
   * @param text
   *          The comment of the review
   * @param userLogin
   *          The login of the user whom this review will be assigned to
   */
  public static ReviewCreateQuery createAssignedReviewQuery(Long violationId, String text, String userLogin) {
    ReviewCreateQuery query = createSimpleReviewQuery(violationId, text);
    query.setAssignee(userLogin);
    return query;
  }

  /**
   * Builds a request that will create a false-positive review on a violation.
   * 
   * @param violationId
   *          The id of the violation that is reviewed
   * @param text
   *          The comment of the review
   */
  public static ReviewCreateQuery createFalsePositiveReviewQuery(Long violationId, String text) {
    ReviewCreateQuery query = createSimpleReviewQuery(violationId, text);
    query.setFalsePositive(Boolean.TRUE);
    return query;
  }

  public Long getViolationId() {
    return violationId;
  }

  public ReviewCreateQuery setViolationId(Long violationId) {
    this.violationId = violationId;
    return this;
  }

  public String getText() {
    return text;
  }

  public ReviewCreateQuery setText(String text) {
    this.text = text;
    return this;
  }

  public String getAssignee() {
    return assignee;
  }

  public ReviewCreateQuery setAssignee(String userLogin) {
    this.assignee = userLogin;
    return this;
  }

  public Boolean getFalsePositive() {
    return falsePositive;
  }

  public ReviewCreateQuery setFalsePositive(Boolean falsePositive) {
    this.falsePositive = falsePositive;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder();
    url.append(ReviewQuery.BASE_URL);
    url.append("/");
    url.append('?');
    appendUrlParameter(url, "violation_id", getViolationId());
    appendUrlParameter(url, "text", getText());
    appendUrlParameter(url, "assignee", getAssignee());
    appendUrlParameter(url, "false_positive", getFalsePositive());
    return url.toString();
  }

  /**
   * Property 'text' is transmitted through request body as content may exceed URL size allowed by the server.
   */
  @Override
  public String getBody() {
    return text;
  }

  @Override
  public Class<Review> getModelClass() {
    return Review.class;
  }
}

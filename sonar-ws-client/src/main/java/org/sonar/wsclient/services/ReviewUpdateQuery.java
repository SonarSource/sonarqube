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
public class ReviewUpdateQuery extends CreateQuery<Review> {

  private Long reviewId;
  private String text;
  private String newText;
  private String assignee;
  private Boolean falsePositive;

  public ReviewUpdateQuery() {
  }

  /**
   * Builds a request that will add a comment on a an existing review.
   * 
   * @param reviewId
   *          The id of the review
   * @param text
   *          The new comment
   */
  public static ReviewUpdateQuery addCommentQuery(Long reviewId, String text) {
    ReviewUpdateQuery query = new ReviewUpdateQuery();
    query.setReviewId(reviewId);
    query.setNewText(text);
    return query;
  }

  /**
   * Builds a request that will update the false-positive status of an existing review.
   * 
   * @param reviewId
   *          The id of the review
   * @param text
   *          The comment for this modification
   * @param falsePositive
   *          the new false positive status of the review
   */
  public static ReviewUpdateQuery updateFalsePositiveQuery(Long reviewId, String text, Boolean falsePositive) {
    ReviewUpdateQuery query = addCommentQuery(reviewId, text);
    query.setFalsePositive(falsePositive);
    return query;
  }

  /**
   * Builds a request that will reassign an existing review to the given user. <br/>
   * <br/>
   * To unassign the review, simply pass "none" for the user login.
   * 
   * @param reviewId
   *          The id of the review that is reviewed
   * @param userLogin
   *          The login of the user whom this review will be assigned to, or "none" to unassign
   */
  public static ReviewUpdateQuery reassignQuery(Long reviewId, String userLogin) {
    ReviewUpdateQuery query = new ReviewUpdateQuery();
    query.setReviewId(reviewId);
    query.setAssignee(userLogin);
    return query;
  }

  /**
   * Builds a request that will edit the last comment of a an existing review (if the last comment belongs to the current user).
   * 
   * @param reviewId
   *          The id of the review
   * @param text
   *          The new text for the last comment
   */
  public static ReviewUpdateQuery editLastCommentQuery(Long reviewId, String text) {
    ReviewUpdateQuery query = new ReviewUpdateQuery();
    query.setReviewId(reviewId);
    query.setText(text);
    return query;
  }

  public Long getReviewId() {
    return reviewId;
  }

  public ReviewUpdateQuery setReviewId(Long reviewId) {
    this.reviewId = reviewId;
    return this;
  }

  public String getText() {
    return text;
  }

  public ReviewUpdateQuery setText(String text) {
    this.text = text;
    return this;
  }

  public String getNewText() {
    return newText;
  }

  public ReviewUpdateQuery setNewText(String text) {
    this.newText = text;
    return this;
  }

  public String getAssignee() {
    return assignee;
  }

  public ReviewUpdateQuery setAssignee(String userLogin) {
    this.assignee = userLogin;
    return this;
  }

  public Boolean getFalsePositive() {
    return falsePositive;
  }

  public ReviewUpdateQuery setFalsePositive(Boolean falsePositive) {
    this.falsePositive = falsePositive;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder();
    url.append(ReviewQuery.BASE_URL);
    url.append("/");
    url.append('?');
    appendUrlParameter(url, "id", getReviewId());
    appendUrlParameter(url, "text", getText());
    appendUrlParameter(url, "new_text", getNewText());
    appendUrlParameter(url, "assignee", getAssignee());
    appendUrlParameter(url, "false_positive", getFalsePositive());
    return url.toString();
  }

  /**
   * Properties 'text' or 'new_text' are transmitted through request body as content may exceed URL size allowed by the server.
   */
  @Override
  public String getBody() {
    return text == null ? newText : text;
  }

  @Override
  public Class<Review> getModelClass() {
    return Review.class;
  }
}

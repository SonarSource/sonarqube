/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.reviews.jira;

import com.atlassian.jira.rpc.soap.client.RemoteIssue;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.database.model.User;
import org.sonar.api.reviews.ReviewAction;
import org.sonar.api.reviews.ReviewContext;
import org.sonar.api.security.UserFinder;
import org.sonar.core.review.ReviewCommentDao;
import org.sonar.core.review.ReviewCommentDto;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;
import org.sonar.plugins.reviews.jira.soap.JiraSOAPClient;

import java.rmi.RemoteException;
import java.util.Date;

/**
 * @since 3.1
 */
public class JiraLinkReviewAction extends ReviewAction {

  public static final String COMMENT_TEXT_PARAM = "comment.text";

  private JiraSOAPClient jiraSOAPClient;
  private ReviewDao reviewDao;
  private ReviewCommentDao reviewCommentDao;
  private UserFinder userFinder;

  public JiraLinkReviewAction(JiraSOAPClient jiraSOAPClient, ReviewDao reviewDao, ReviewCommentDao reviewCommentDao, UserFinder userFinder) {
    this.jiraSOAPClient = jiraSOAPClient;
    this.reviewDao = reviewDao;
    this.reviewCommentDao = reviewCommentDao;
    this.userFinder = userFinder;
  }

  @Override
  public String getId() {
    return "create-link-to-jira";
  }

  @Override
  public void execute(ReviewContext reviewContext) {
    ReviewDto review = getReviewId(reviewContext);
    User user = getUser(reviewContext);

    RemoteIssue issue = null;
    try {
      issue = jiraSOAPClient.createIssue(review);
    } catch (RemoteException e) {
      throw new IllegalStateException("Impossible to create an issue on JIRA: " + e.getMessage(), e);
    }

    addCommentToReview(review, issue, user, reviewContext.getParamValue(COMMENT_TEXT_PARAM));

    updateReviewWithIssueInfo(review, issue);

  }

  private ReviewDto getReviewId(ReviewContext reviewContext) {
    String reviewId = reviewContext.getReviewProperty("id");
    Preconditions.checkState(StringUtils.isNotBlank(reviewId), "The review id is missing.");
    Preconditions.checkState(StringUtils.isNumeric(reviewId), "The given review with id is not a valid number: " + reviewId);
    ReviewDto review = reviewDao.findById(Long.parseLong(reviewId));
    Preconditions.checkNotNull(review, "The review with id '" + reviewId + "' does not exist.");
    return review;
  }

  private User getUser(ReviewContext reviewContext) {
    String userLogin = reviewContext.getUserProperty("login");
    Preconditions.checkState(StringUtils.isNotBlank(userLogin), "The user login is missing.");
    User user = userFinder.findByLogin(userLogin);
    Preconditions.checkNotNull(user, "The user with login '" + userLogin + "' does not exist.");
    return user;
  }

  private void addCommentToReview(ReviewDto review, RemoteIssue issue, User user, String text) {
    ReviewCommentDto comment = new ReviewCommentDto();
    comment.setReviewId(review.getId());
    comment.setUserId((long) user.getId());
    Date now = new Date();
    comment.setCreatedAt(now);
    comment.setUpdatedAt(now);

    StringBuilder message = new StringBuilder();
    if (!StringUtils.isBlank(text)) {
      message.append(text);
      message.append("\n\n");
    }
    message.append("Review linked to JIRA issue: http://localhost:8080/browse/");
    message.append(issue.getKey());
    comment.setText(message.toString());

    reviewCommentDao.insert(comment);
  }

  protected void updateReviewWithIssueInfo(ReviewDto review, RemoteIssue issue) {
    review.addKeyValueToData(JiraLinkReviewConstants.REVIEW_DATA_PROPERTY_KEY, issue.getKey());
    reviewDao.update(Lists.newArrayList(review));
  }

}

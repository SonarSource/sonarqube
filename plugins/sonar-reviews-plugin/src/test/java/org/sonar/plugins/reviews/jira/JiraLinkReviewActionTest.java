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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.database.model.User;
import org.sonar.api.reviews.ReviewContext;
import org.sonar.api.security.UserFinder;
import org.sonar.api.test.ReviewContextTestUtils;
import org.sonar.core.review.ReviewCommentDao;
import org.sonar.core.review.ReviewCommentDto;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;
import org.sonar.plugins.reviews.jira.soap.JiraSOAPClient;

import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JiraLinkReviewActionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private JiraLinkReviewAction action;
  private JiraSOAPClient soapClient;
  private ReviewDao reviewDao;
  private ReviewCommentDao reviewCommentDao;
  private UserFinder userFinder;

  @Before
  public void init() throws Exception {
    soapClient = mock(JiraSOAPClient.class);
    RemoteIssue remoteIssue = new RemoteIssue();
    remoteIssue.setKey("FOO-15");
    when(soapClient.createIssue(any(ReviewDto.class))).thenReturn(remoteIssue);

    reviewDao = mock(ReviewDao.class);
    when(reviewDao.findById(45L)).thenReturn(new ReviewDto().setId(45L));

    userFinder = mock(UserFinder.class);
    User user = new User();
    user.setId(12);
    when(userFinder.findByLogin("paul")).thenReturn(user);

    reviewCommentDao = mock(ReviewCommentDao.class);

    action = new JiraLinkReviewAction(soapClient, reviewDao, reviewCommentDao, userFinder);
  }

  @Test
  public void shouldExecute() throws Exception {
    ReviewContext reviewContext = ReviewContextTestUtils.createReviewContext("review={id=45}; user={login=paul}; params={comment.text=Hello world}");

    action.execute(reviewContext);

    verify(reviewDao).findById(45L);
    verify(userFinder).findByLogin("paul");
    verify(soapClient).createIssue(new ReviewDto().setId(45L));

    ArgumentCaptor<ReviewCommentDto> commentCaptor = ArgumentCaptor.forClass(ReviewCommentDto.class);
    verify(reviewCommentDao).insert(commentCaptor.capture());
    ReviewCommentDto comment = commentCaptor.getValue();
    assertThat(comment.getReviewId(), is(45L));
    assertThat(comment.getUserId(), is(12L));
    assertThat(comment.getText(), is("Hello world\n\nReview linked to JIRA issue: http://localhost:8080/browse/FOO-15"));

    ArgumentCaptor<Collection> reviewCaptor = ArgumentCaptor.forClass(Collection.class);
    verify(reviewDao).update(reviewCaptor.capture());
    ReviewDto reviewDto = (ReviewDto) reviewCaptor.getValue().iterator().next();
    assertThat(reviewDto.getData(), is("jira-issue-key=FOO-15"));

  }

  @Test
  public void shouldNotAddLinesBeforeLinkIfNoTextProvided() throws Exception {
    ReviewContext reviewContext = ReviewContextTestUtils.createReviewContext("review={id=45}; user={login=paul}");

    action.execute(reviewContext);

    ArgumentCaptor<ReviewCommentDto> commentCaptor = ArgumentCaptor.forClass(ReviewCommentDto.class);
    verify(reviewCommentDao).insert(commentCaptor.capture());
    assertThat(commentCaptor.getValue().getText(), is("Review linked to JIRA issue: http://localhost:8080/browse/FOO-15"));
  }

  @Test
  public void shouldFailIfReviewIdNotProvided() throws Exception {
    ReviewContext reviewContext = ReviewContextTestUtils.createReviewContext("");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The review id is missing.");
    action.execute(reviewContext);
  }

  @Test
  public void shouldFailIfReviewIdNotANumber() throws Exception {
    ReviewContext reviewContext = ReviewContextTestUtils.createReviewContext("review={id=toto}");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The given review with id is not a valid number: toto");
    action.execute(reviewContext);
  }

  @Test
  public void shouldFailIfReviewDoesNotExist() throws Exception {
    ReviewContext reviewContext = ReviewContextTestUtils.createReviewContext("review={id=100}");

    thrown.expect(NullPointerException.class);
    thrown.expectMessage("The review with id '100' does not exist.");
    action.execute(reviewContext);
  }

  @Test
  public void shouldFailIfUserDoesNotExist() throws Exception {
    ReviewContext reviewContext = ReviewContextTestUtils.createReviewContext("review={id=45}; user={login=invisible_man}");

    thrown.expect(NullPointerException.class);
    thrown.expectMessage("The user with login 'invisible_man' does not exist.");
    action.execute(reviewContext);
  }

}

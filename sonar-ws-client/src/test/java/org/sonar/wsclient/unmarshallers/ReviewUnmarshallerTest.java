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
package org.sonar.wsclient.unmarshallers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.sonar.wsclient.services.Review;
import org.sonar.wsclient.services.Review.Comment;

import java.util.List;

public class ReviewUnmarshallerTest extends UnmarshallerTestCase {

  @Test
  public void testEmptyJSON() {
    Review review = new ReviewUnmarshaller().toModel("[]");
    assertThat(review, nullValue());
  }

  @Test
  public void testToModels() {
    List<Review> reviews = new ReviewUnmarshaller().toModels(loadFile("/reviews/reviews-2.9.json"));
    assertThat(reviews.size(), is(2));

    Review review = reviews.get(0);
    assertThat(review.getId(), is(3L));
    assertNotNull(review.getCreatedAt());
    assertNotNull(review.getUpdatedAt());
    assertThat(review.getAuthorLogin(), is("admin"));
    assertThat(review.getAssigneeLogin(), is("admin"));
    assertThat(review.getTitle(), is("'static' modifier out of order with the JLS suggestions."));
    assertThat(review.getStatus(), is("RESOLVED"));
    assertThat(review.getResolution(), is("FALSE-POSITIVE"));
    assertThat(review.getSeverity(), is("MINOR"));
    assertThat(review.getResourceKee(), is("org.codehaus.sonar:sonar-channel:org.sonar.channel.CodeReaderConfiguration"));
    assertThat(review.getLine(), is(33));
    assertThat(review.getViolationId(), is(1L));
    List<Comment> comments = review.getComments();
    assertThat(comments.size(), is(4));
    Comment comment = comments.get(0);
    assertThat(comment.getId(), is(1L));
    assertNotNull(comment.getUpdatedAt());
    assertThat(comment.getAuthorLogin(), is("admin"));
    assertThat(comment.getText(), is("This is a review.<br/>And this is on multiple lines...<br/><br/><code>Wouhou!!!!!</code>"));

    review = reviews.get(1);
    assertThat(review.getAssigneeLogin(), nullValue());
    assertThat(review.getStatus(), is("OPEN"));
    assertThat(review.getResolution(), nullValue());
  }

  /*
   * Test Unmarshaller with JSON data received from a Sonar 2.8
   */
  @Test
  public void testToModelsForSonar2_8() {
    List<Review> reviews = new ReviewUnmarshaller().toModels(loadFile("/reviews/reviews-2.8.json"));
    assertThat(reviews.size(), is(2));

    Review review = reviews.get(0);
    assertThat(review.getAssigneeLogin(), nullValue());

    review = reviews.get(1);
    assertThat(review.getId(), is(3L));
    assertNotNull(review.getCreatedAt());
    assertNotNull(review.getUpdatedAt());
    assertThat(review.getAuthorLogin(), is("admin"));
    assertThat(review.getAssigneeLogin(), is("admin"));
    assertThat(review.getTitle(), is("'static' modifier out of order with the JLS suggestions."));
    assertThat(review.getType(), is("VIOLATION"));
    assertThat(review.getStatus(), is("OPEN"));
    assertThat(review.getSeverity(), is("MINOR"));
    assertThat(review.getResourceKee(), is("org.codehaus.sonar:sonar-channel:org.sonar.channel.CodeReaderConfiguration"));
    assertThat(review.getLine(), is(33));
    List<Comment> comments = review.getComments();
    assertThat(comments.size(), is(4));
    Comment comment = comments.get(0);
    assertNotNull(comment.getUpdatedAt());
    assertThat(comment.getAuthorLogin(), is("admin"));
    assertThat(comment.getText(), is("This is a review.<br/>And this is on multiple lines...<br/><br/><code>Wouhou!!!!!</code>"));
  }

}

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
package org.sonar.api.reviews;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.test.ReviewContextTestUtils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ReviewContextTest {

  private ReviewContext reviewContext;

  @Before
  public void init() {
    reviewContext = ReviewContextTestUtils.createReviewContext("review={id=45, status=RESOLVED}; project={id=12}; user={login=admin}; params={comment=This is a comment}");
  }

  @Test
  public void shouldGetReviewProps() {
    assertThat(reviewContext.getReviewProperty("id"), is("45"));
    assertThat(reviewContext.getReviewProperty("status"), is("RESOLVED"));
    assertThat(reviewContext.getReviewProperty("assignee"), is(nullValue()));
  }

  @Test
  public void shouldGetProjectProps() {
    assertThat(reviewContext.getProjectProperty("id"), is("12"));
  }

  @Test
  public void shouldGetUserProps() {
    assertThat(reviewContext.getUserProperty("login"), is("admin"));
  }

  @Test
  public void shouldGetParams() {
    assertThat(reviewContext.getParamValue("comment"), is("This is a comment"));
  }

  @Test
  public void testEmptyReviewContext() throws Exception {
    reviewContext = ReviewContextTestUtils.createReviewContext("");
    assertThat(reviewContext.getReviewProperty("id"), is(nullValue()));
    assertThat(reviewContext.getProjectProperty("id"), is(nullValue()));
    assertThat(reviewContext.getUserProperty("login"), is(nullValue()));
    assertThat(reviewContext.getParamValue("comment"), is(nullValue()));
  }

}

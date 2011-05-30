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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ReviewUpdateQueryTest extends QueryTestCase {

  @Test
  public void testAddComment() {
    ReviewUpdateQuery query = ReviewUpdateQuery.addCommentQuery(13L, "Hello World!");
    assertThat(query.getUrl(), is("/api/reviews/?id=13&new_text=Hello+World%21&"));
    assertThat(query.getModelClass().getName(), is(Review.class.getName()));
  }

  @Test
  public void testEditLastComment() {
    ReviewUpdateQuery query = ReviewUpdateQuery.editLastCommentQuery(13L, "Hello World!");
    assertThat(query.getUrl(), is("/api/reviews/?id=13&text=Hello+World%21&"));
  }

  @Test
  public void testReassign() {
    ReviewUpdateQuery query = ReviewUpdateQuery.reassignQuery(13L, "fabrice");
    assertThat(query.getUrl(), is("/api/reviews/?id=13&assignee=fabrice&"));
  }

  @Test
  public void testUpdateFalsePositive() {
    ReviewUpdateQuery query = ReviewUpdateQuery.updateFalsePositiveQuery(13L, "Hello World!", Boolean.TRUE);
    assertThat(query.getUrl(), is("/api/reviews/?id=13&new_text=Hello+World%21&false_positive=true&"));
  }

}
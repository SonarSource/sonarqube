/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.services;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ReviewUpdateQueryTest extends QueryTestCase {

  @Test
  public void testAddComment() {
    ReviewUpdateQuery query = ReviewUpdateQuery.addComment(13, "Hello World!");
    assertThat(query.getUrl(), is("/api/reviews/add_comment/13?"));
    assertThat(query.getBody(), is("Hello World!"));
    assertThat(query.getModelClass().getName(), is(Review.class.getName()));
  }

  @Test
  public void testReassign() {
    ReviewUpdateQuery query = ReviewUpdateQuery.reassign(13, "fabrice");
    assertThat(query.getUrl(), is("/api/reviews/reassign/13?assignee=fabrice&"));
    assertThat(query.getBody(), nullValue());
  }

  @Test
  public void testResolveAsFalsePositive() {
    ReviewUpdateQuery query = ReviewUpdateQuery.resolve(13, "FALSE-POSITIVE").setComment("Hello World!");
    assertThat(query.getUrl(), is("/api/reviews/resolve/13?resolution=FALSE-POSITIVE&"));
    assertThat(query.getBody(), is("Hello World!"));
  }

  @Test
  public void testResolveAsFixed() {
    ReviewUpdateQuery query = ReviewUpdateQuery.resolve(13, "FIXED");
    assertThat(query.getUrl(), is("/api/reviews/resolve/13?resolution=FIXED&"));
    assertThat(query.getBody(), nullValue());
  }

  @Test
  public void testReopen() {
    ReviewUpdateQuery query = ReviewUpdateQuery.reopen(13);
    assertThat(query.getUrl(), is("/api/reviews/reopen/13?"));
    assertThat(query.getBody(), nullValue());
  }

}

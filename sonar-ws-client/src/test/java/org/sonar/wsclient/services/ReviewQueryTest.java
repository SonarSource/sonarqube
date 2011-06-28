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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class ReviewQueryTest extends QueryTestCase {

  @Test
  public void queryForResource() {
    Resource resource = mock(Resource.class);
    when(resource.getId()).thenReturn(69);
    ReviewQuery query = ReviewQuery.createForResource(resource);
    assertThat(query.getUrl(), is("/api/reviews?resources=69&"));
    assertThat(query.getModelClass().getName(), is(Review.class.getName()));
  }

  @Test
  public void queryById() {
    assertThat(new ReviewQuery().setId(13L).getUrl(), is("/api/reviews?id=13&"));
    assertThat(new ReviewQuery().setIds(10L, 11L).getUrl(), is("/api/reviews?ids=10,11&"));
  }

  @Test
  public void queryByResolution() {
    ReviewQuery query = new ReviewQuery().setStatuses("RESOLVED").setResolutions("FALSE-POSITIVE");
    assertThat(query.getUrl(), is("/api/reviews?statuses=RESOLVED&resolutions=FALSE-POSITIVE&"));
  }

  @Test
  public void resourceTreeViolations() {
    ReviewQuery query = new ReviewQuery()
        .setStatuses("OPEN")
        .setSeverities("MINOR", "INFO")
        .setProjectKeysOrIds("com.sonar.foo:bar")
        .setResourceKeysOrIds("2", "3")
        .setAuthorLoginsOrIds("20")
        .setAssigneeLoginsOrIds("admin")
        .setOutput("html");
    assertThat(
        query.getUrl(),
        is("/api/reviews?statuses=OPEN&severities=MINOR,INFO&projects=com.sonar.foo%3Abar&resources=2,3&authors=20&assignees=admin&output=html&"));
  }

  @Test
  public void resourceTreeViolationsForSonar2_8() {
    ReviewQuery query = new ReviewQuery();
    query.setIds(10L, 11L).setReviewType("FALSE_POSITIVE").setStatuses("OPEN").setSeverities("MINOR", "INFO")
        .setProjectKeysOrIds("com.sonar.foo:bar").setResourceKeysOrIds("2", "3").setAuthorLoginsOrIds("20").setAssigneeLoginsOrIds("admin")
        .setOutput("html");
    assertThat(
        query.getUrl(),
        is("/api/reviews?ids=10,11&statuses=OPEN&severities=MINOR,INFO&projects=com.sonar.foo%3Abar&resources=2,3&authors=20&assignees=admin&output=html&review_type=FALSE_POSITIVE&"));
  }
}

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
package org.sonar.core.workflow;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.workflow.Comment;
import org.sonar.api.workflow.internal.DefaultReview;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Arrays;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class ReviewDatabaseStoreTest extends AbstractDaoTestCase {

  @Test
  public void store() {
    setupData("store");
    ReviewDatabaseStore store = new ReviewDatabaseStore(getMyBatis());
    DefaultReview review = new DefaultReview().setReviewId(1234L);
    review.setStatus("CLOSED");
    review.setResolution("RESOLVED");
    review.setProperty("who", "me");
    review.setProperty("why", "because");
    Comment comment = review.createComment();
    comment.setMarkdownText("this is a comment");
    comment.setUserId(555L);

    Date now = DateUtils.parseDate("2012-05-18");
    store.store(review, now);

    checkTables("store", "reviews");
    checkTables("store", new String[]{"id"}, "review_comments");
  }

  @Test
  public void completeProjectSettings() {
    setupData("completeProjectSettings");
    ReviewDatabaseStore store = new ReviewDatabaseStore(getMyBatis());

    Settings settings = new Settings();
    store.completeProjectSettings(100L, settings, Arrays.asList("not.available.on.project", "jira.project.key"));

    assertThat(settings.getString("not.available.on.project")).isNull();

    // project property
    assertThat(settings.getString("jira.project.key")).isEqualTo("FOO");
  }
}

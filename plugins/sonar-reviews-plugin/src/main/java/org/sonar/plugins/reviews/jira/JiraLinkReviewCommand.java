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

import com.google.common.collect.Lists;
import org.sonar.api.reviews.LinkReviewCommand;
import org.sonar.api.reviews.ReviewAction;
import org.sonar.api.reviews.ReviewCommand;
import org.sonar.api.reviews.ReviewContext;
import org.sonar.api.utils.KeyValueFormat;

import java.util.Collection;

/**
 * @since 3.1
 */
public class JiraLinkReviewCommand extends ReviewCommand implements LinkReviewCommand {

  private Collection<ReviewAction> actions = Lists.newArrayList();

  public JiraLinkReviewCommand(JiraLinkReviewAction jiraLinkReviewAction) {
    this.actions.add(jiraLinkReviewAction);
  }

  @Override
  public String getId() {
    return "link-to-jira";
  }

  @Override
  public String getName() {
    return "Link to JIRA";
  }

  @Override
  public Collection<ReviewAction> getActions() {
    return actions;
  }

  @Override
  public boolean isAvailableFor(ReviewContext reviewContext) {
    if (reviewContext.getReviewProperty("id") != null) {
      // we are in the context of a review => check if the review already had the issue info
      String reviewData = reviewContext.getReviewProperty("data");
      if (KeyValueFormat.parse(reviewData).containsKey(JiraLinkReviewConstants.REVIEW_DATA_PROPERTY_KEY)) {
        return false;
      }
    } else if (reviewContext.getProjectProperty("id") != null) {
      // we are in the context of a project => check if the params have been specified and if so, cache them
      System.out.println("==========> Check for project availability!");
      // TODO complete here the algorithm
    }
    return true;
  }
}

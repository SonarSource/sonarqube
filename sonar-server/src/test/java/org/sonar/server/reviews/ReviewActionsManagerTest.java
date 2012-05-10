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
package org.sonar.server.reviews;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.reviews.LinkReviewAction;
import org.sonar.api.reviews.ReviewAction;

import java.util.Collection;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ReviewActionsManagerTest {

  private ReviewAction fakeLinkReviewAction = new FakeLinkReviewAction();
  private ReviewAction simpleReviewAction = new SimpleReviewAction();
  private ReviewActionsManager manager;

  @Before
  public void init() throws Exception {
    manager = new ReviewActionsManager(new ReviewAction[] {fakeLinkReviewAction, simpleReviewAction});
  }

  @Test
  public void shouldReturnActionsById() throws Exception {
    assertThat(manager.getAction("fake-link-review"), is(fakeLinkReviewAction));
    assertThat(manager.getAction("simple-review-action"), is(simpleReviewAction));
  }

  @Test
  public void shouldReturnActionsByInterfaceName() throws Exception {
    Collection<ReviewAction> reviewActions = manager.getActions("org.sonar.api.reviews.LinkReviewAction");
    assertThat(reviewActions.size(), is(1));
    assertThat(reviewActions, hasItem(fakeLinkReviewAction));
  }

  class FakeLinkReviewAction implements LinkReviewAction {
    public String getId() {
      return "fake-link-review";
    }

    public String getName() {
      return "Fake action";
    }

    public void execute(Map<String, String> reviewContext) {
    }
  }
  class SimpleReviewAction implements ReviewAction {
    public String getId() {
      return "simple-review-action";
    }

    public String getName() {
      return "Simple action";
    }

    public void execute(Map<String, String> reviewContext) {
    }
  }
}

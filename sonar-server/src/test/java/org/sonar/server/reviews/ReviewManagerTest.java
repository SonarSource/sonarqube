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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.reviews.LinkReviewCommand;
import org.sonar.api.reviews.ReviewAction;
import org.sonar.api.reviews.ReviewCommand;
import org.sonar.api.reviews.ReviewContext;
import org.sonar.api.test.ReviewContextTestUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ReviewManagerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ReviewManager manager;
  private ReviewAction action1;
  private ReviewAction action2;
  private ReviewAction action3;
  private FakeLinkReviewCommand fakeLinkReviewCommand;
  private SimpleReviewCommand simpleReviewCommand;

  @Before
  public void init() throws Exception {
    action1 = mock(ReviewAction.class);
    action2 = mock(ReviewAction.class);
    action3 = mock(ReviewAction.class);
    fakeLinkReviewCommand = new FakeLinkReviewCommand();
    simpleReviewCommand = new SimpleReviewCommand();

    manager = new ReviewManager(new ReviewCommand[] {fakeLinkReviewCommand, simpleReviewCommand});
  }

  @Test
  public void testGetCommandById() throws Exception {
    assertThat(manager.getCommand("simple-review-action"), is((ReviewCommand) simpleReviewCommand));
  }

  @Test
  public void shouldGetAvailableCommandForReviewContext() throws Exception {
    Map<String, String> reviewMap = Maps.newHashMap();
    reviewMap.put("fake", "bar");
    Map<String, Map<String, String>> contextMap = Maps.newHashMap();
    contextMap.put("review", reviewMap);

    ReviewContext context = ReviewContext.createFromMap(contextMap);
    Collection<ReviewCommand> availableCommands = manager.getAvailableCommandsFor(context);
    assertThat(availableCommands.size(), is(1));
    assertThat(availableCommands, hasItem((ReviewCommand) fakeLinkReviewCommand));
  }

  @Test
  public void shouldFilterCommands() throws Exception {
    ReviewContext context = ReviewContextTestUtils.createReviewContext("review={fake=bar}");
    Collection<ReviewCommand> filteredCommands = manager.filterCommands(Lists.newArrayList(fakeLinkReviewCommand, simpleReviewCommand), context,
        "org.sonar.api.reviews.LinkReviewCommand");
    assertThat(filteredCommands.size(), is(1));
    assertThat(filteredCommands, hasItem((ReviewCommand) fakeLinkReviewCommand));
  }

  @Test
  public void shouldFilterCommandsButNoMatch() throws Exception {
    ReviewContext context = ReviewContextTestUtils.createReviewContext("review={simple=foo}");
    Collection<ReviewCommand> filteredCommands = manager.filterCommands(Lists.newArrayList(fakeLinkReviewCommand, simpleReviewCommand), context,
        "org.sonar.api.reviews.LinkReviewCommand");
    assertThat(filteredCommands.size(), is(0));
  }

  @Test
  public void shouldFailFilterCommandsIfUnknownInterface() throws Exception {
    ReviewContext context = ReviewContextTestUtils.createReviewContext("review={fake=bar}");

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("The following interface for review commands does not exist: org.sonar.api.reviews.UnkonwnReviewCommand");

    manager.filterCommands(Lists.newArrayList(fakeLinkReviewCommand, simpleReviewCommand), context, "org.sonar.api.reviews.UnkonwnReviewCommand");
  }

  @Test
  public void shouldExecuteCommandActions() throws Exception {
    ReviewContext reviewContext = ReviewContext.createFromMap(new HashMap<String, Map<String, String>>());
    manager.executeCommandActions("fake-link-review", reviewContext);
    verify(action1).execute(reviewContext);
    verify(action2).execute(reviewContext);
    verify(action3, never()).execute(reviewContext);
  }

  @Test
  public void shouldFailExecuteCommandActionsIfCommandDoesNotExist() throws Exception {
    ReviewContext reviewContext = ReviewContext.createFromMap(new HashMap<String, Map<String, String>>());

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The command with the following ID does not exist: unexisting-command");

    manager.executeCommandActions("unexisting-command", reviewContext);
  }

  class FakeLinkReviewCommand extends ReviewCommand implements LinkReviewCommand {
    @Override
    public String getId() {
      return "fake-link-review";
    }

    @Override
    public String getName() {
      return "Fake action";
    }

    @Override
    public boolean isAvailableFor(ReviewContext reviewContext) {
      return reviewContext.getReviewProperty("fake") != null;
    }

    @Override
    public Collection<ReviewAction> getActions() {
      return Lists.newArrayList(action1, action2);
    }
  }

  class SimpleReviewCommand extends ReviewCommand {
    @Override
    public String getId() {
      return "simple-review-action";
    }

    @Override
    public String getName() {
      return "Simple action";
    }

    @Override
    public boolean isAvailableFor(ReviewContext reviewContext) {
      return reviewContext.getReviewProperty("simple") != null;
    }

    @Override
    public Collection<ReviewAction> getActions() {
      return Lists.newArrayList(action3);
    }
  }
}

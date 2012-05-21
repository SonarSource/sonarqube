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
package org.sonar.core.review.workflow;

import com.google.common.collect.ListMultimap;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.core.review.workflow.condition.Condition;
import org.sonar.core.review.workflow.condition.HasProjectPropertyCondition;
import org.sonar.core.review.workflow.review.DefaultReview;
import org.sonar.core.review.workflow.review.DefaultWorkflowContext;
import org.sonar.core.review.workflow.review.Review;
import org.sonar.core.review.workflow.review.WorkflowContext;
import org.sonar.core.review.workflow.screen.CommentScreen;
import org.sonar.core.review.workflow.screen.Screen;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class WorkflowEngineTest {
  @Test
  public void listAvailableScreensForReview_empty() {
    WorkflowEngine engine = new WorkflowEngine(new Workflow(), mock(ReviewStore.class), new Settings());
    List<Screen> screens = engine.listAvailableScreens(new DefaultReview(), new DefaultWorkflowContext(), true);
    assertThat(screens).isEmpty();
  }

  @Test
  public void listAvailableScreensForReview() {
    Workflow workflow = new Workflow();
    workflow.addCommand("command-without-screen");
    workflow.addCommand("resolve");
    CommentScreen screen = new CommentScreen();
    workflow.setScreen("resolve", screen);

    WorkflowEngine engine = new WorkflowEngine(workflow, mock(ReviewStore.class), new Settings());
    List<Screen> screens = engine.listAvailableScreens(new DefaultReview(), new DefaultWorkflowContext(), true);
    assertThat(screens).containsExactly(screen);
  }

  @Test
  public void listAvailableScreensForReview_verify_conditions() {
    Workflow workflow = new Workflow();
    workflow.addCommand("resolve");
    Condition condition = mock(Condition.class);
    when(condition.doVerify(any(Review.class), any(WorkflowContext.class))).thenReturn(false);
    workflow.addCondition("resolve", condition);
    workflow.setScreen("resolve", new CommentScreen());

    WorkflowEngine engine = new WorkflowEngine(workflow, mock(ReviewStore.class), new Settings());
    DefaultReview review = new DefaultReview();
    DefaultWorkflowContext context = new DefaultWorkflowContext();
    assertThat(engine.listAvailableScreens(review, context, true)).isEmpty();

    verify(condition).doVerify(review, context);
  }

  @Test
  public void listAvailableScreensForReviews_empty() {
    WorkflowEngine engine = new WorkflowEngine(new Workflow(), mock(ReviewStore.class), new Settings());
    ListMultimap<Long, Screen> screens = engine.listAvailableScreens(
        new Review[]{new DefaultReview().setViolationId(1000L), new DefaultReview().setViolationId(2000L)},
        new DefaultWorkflowContext(), true);
    assertThat(screens.size()).isEqualTo(0);
  }

  @Test
  public void listAvailableScreensForReviews() {
    Workflow workflow = new Workflow();
    workflow.addCommand("command-without-screen");
    workflow.addCommand("resolve");
    CommentScreen screen = new CommentScreen();
    workflow.setScreen("resolve", screen);
    WorkflowEngine engine = new WorkflowEngine(workflow, mock(ReviewStore.class), new Settings());
    ListMultimap<Long, Screen> screens = engine.listAvailableScreens(
        new Review[]{new DefaultReview().setViolationId(1000L), new DefaultReview().setViolationId(2000L)},
        new DefaultWorkflowContext(), true);
    assertThat(screens.size()).isEqualTo(2);
    assertThat(screens.get(1000L)).containsExactly(screen);
    assertThat(screens.get(2000L)).containsExactly(screen);
  }

  @Test
  public void listAvailableScreensForReviews_load_project_properties() {
    Workflow workflow = new Workflow();
    workflow.addCommand("resolve");
    workflow.addCondition("resolve", new HasProjectPropertyCondition("foo"));

    ReviewStore store = mock(ReviewStore.class);
    WorkflowEngine engine = new WorkflowEngine(workflow, store, new Settings());

    engine.listAvailableScreens(
        new Review[]{new DefaultReview().setViolationId(1000L), new DefaultReview().setViolationId(2000L)},
        new DefaultWorkflowContext().setProjectId(300L),
        true);

    verify(store).completeProjectSettings(eq(300L), any(Settings.class), (List<String>) argThat(hasItem("foo")));
  }
}

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
import com.google.common.collect.Maps;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.core.review.workflow.condition.Condition;
import org.sonar.core.review.workflow.condition.HasProjectPropertyCondition;
import org.sonar.core.review.workflow.function.Function;
import org.sonar.core.review.workflow.review.*;
import org.sonar.core.review.workflow.screen.CommentScreen;
import org.sonar.core.review.workflow.screen.Screen;

import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class WorkflowEngineTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

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

  @Test
  public void execute_conditions_pass() {
    Workflow workflow = new Workflow();
    workflow.addCommand("resolve");
    workflow.addCondition("resolve", new HasProjectPropertyCondition("foo"));
    Function function = mock(Function.class);
    workflow.addFunction("resolve", function);

    ReviewStore store = mock(ReviewStore.class);
    Settings settings = new Settings();
    settings.setProperty("foo", "bar");
    WorkflowEngine engine = new WorkflowEngine(workflow, store, settings);

    MutableReview review = new DefaultReview().setViolationId(1000L);
    Map<String, String> parameters = Maps.newHashMap();
    DefaultWorkflowContext context = new DefaultWorkflowContext().setProjectId(300L);

    engine.execute("resolve", review, context, parameters);

    verify(store).completeProjectSettings(eq(300L), any(Settings.class), (List<String>) argThat(hasItem("foo")));
    verify(function).doExecute(eq(review), any(ImmutableReview.class), eq(context), eq(parameters));
  }

  @Test
  public void execute_fail_if_conditions_dont_pass() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Conditions are not respected");

    Workflow workflow = new Workflow();
    workflow.addCommand("resolve");
    workflow.addCondition("resolve", new HasProjectPropertyCondition("foo"));
    Function function = mock(Function.class);
    workflow.addFunction("resolve", function);

    ReviewStore store = mock(ReviewStore.class);
    Settings settings = new Settings();// missing property 'foo'
    WorkflowEngine engine = new WorkflowEngine(workflow, store, settings);

    MutableReview review = new DefaultReview().setViolationId(1000L);
    Map<String, String> parameters = Maps.newHashMap();
    DefaultWorkflowContext context = new DefaultWorkflowContext().setProjectId(300L);

    engine.execute("resolve", review, context, parameters);
  }
}

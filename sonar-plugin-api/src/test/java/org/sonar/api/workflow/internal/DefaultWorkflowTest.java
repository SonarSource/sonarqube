/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.workflow.internal;

import org.fest.assertions.MapAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.workflow.Workflow;
import org.sonar.api.workflow.condition.Condition;
import org.sonar.api.workflow.condition.HasProjectPropertyCondition;
import org.sonar.api.workflow.condition.StatusCondition;
import org.sonar.api.workflow.function.CommentFunction;
import org.sonar.api.workflow.function.Function;
import org.sonar.api.workflow.screen.CommentScreen;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultWorkflowTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void addCommand() {
    DefaultWorkflow workflow = new DefaultWorkflow();
    assertThat(workflow.getCommands()).isEmpty();

    assertThat(workflow.addCommand("resolve")).isSameAs(workflow);
    assertThat(workflow.getCommands()).containsOnly("resolve");
    assertThat(workflow.hasCommand("resolve")).isTrue();
  }

  @Test
  public void addCommand_does_not_accept_blank() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Empty command key");

    Workflow workflow = new DefaultWorkflow();
    workflow.addCommand("");
  }

  @Test
  public void addSeveralTimesTheSameCommand() {
    Workflow workflow = new DefaultWorkflow();
    workflow.addCommand("resolve");
    workflow.addCommand("resolve");
    assertThat(workflow.getCommands()).containsOnly("resolve");
    assertThat(workflow.getCommands()).hasSize(1);
  }

  @Test
  public void addCondition_fail_if_unknown_command() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Unknown command: resolve");

    Workflow workflow = new DefaultWorkflow();
    workflow.addCondition("resolve", new StatusCondition("OPEN"));
  }

  @Test
  public void addCondition() {
    Workflow workflow = new DefaultWorkflow();
    Condition condition = new StatusCondition("OPEN");
    workflow.addCommand("resolve");

    workflow.addCondition("resolve", condition);

    assertThat(workflow.getConditions("resolve")).containsExactly(condition);
  }

  @Test
  public void getConditions_empty() {
    Workflow workflow = new DefaultWorkflow();
    assertThat(workflow.getConditions("resolve")).isEmpty();
  }

  @Test
  public void keepCacheOfProjectPropertiesRequiredByConditions() {
    DefaultWorkflow workflow = new DefaultWorkflow();
    Condition condition1 = new HasProjectPropertyCondition("jira.url");
    Condition condition2 = new HasProjectPropertyCondition("jira.login");
    workflow.addCommand("create-jira-issue");
    workflow.addCondition("create-jira-issue", condition1);
    workflow.addCondition("create-jira-issue", condition2);

    assertThat(workflow.getProjectPropertyKeys()).containsExactly("jira.url", "jira.login");
  }

  @Test
  public void cacheOfProjectPropertiesIsNotNull() {
    DefaultWorkflow workflow = new DefaultWorkflow();

    assertThat(workflow.getProjectPropertyKeys()).isEmpty();
  }

  @Test
  public void keepFastLinksToReviewAndContextConditions() {
    DefaultWorkflow workflow = new DefaultWorkflow();
    workflow.addCommand("create-jira-issue");
    Condition contextCondition = new HasProjectPropertyCondition("jira.url");
    workflow.addCondition("create-jira-issue", contextCondition);
    Condition reviewCondition = new StatusCondition("OPEN");
    workflow.addCondition("create-jira-issue", reviewCondition);

    assertThat(workflow.getContextConditions("create-jira-issue")).containsExactly(contextCondition);
    assertThat(workflow.getReviewConditions("create-jira-issue")).containsExactly(reviewCondition);
  }

  @Test
  public void addFunction() {
    Workflow workflow = new DefaultWorkflow();
    workflow.addCommand("resolve");

    Function function = new CommentFunction();
    workflow.addFunction("resolve", function);

    assertThat(workflow.getFunctions("resolve")).containsExactly(function);
  }

  @Test
  public void getFunctions_empty() {
    Workflow workflow = new DefaultWorkflow();
    assertThat(workflow.getFunctions("resolve")).isEmpty();
  }

  @Test
  public void addFunction_fail_if_unknown_command() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Unknown command: resolve");

    Workflow workflow = new DefaultWorkflow();
    workflow.addFunction("resolve", new CommentFunction());
  }

  @Test
  public void setScreen_fail_if_unknown_command() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Unknown command: resolve");

    Workflow workflow = new DefaultWorkflow();
    workflow.setScreen("resolve", new CommentScreen());
  }

  @Test
  public void setScreen() {
    DefaultWorkflow workflow = new DefaultWorkflow();
    workflow.addCommand("resolve");
    CommentScreen screen = new CommentScreen();
    workflow.setScreen("resolve", screen);

    assertThat(workflow.getScreen("resolve")).isSameAs(screen);
    assertThat(workflow.getScreensByCommand()).includes(MapAssert.entry("resolve", screen));
    assertThat(workflow.getScreensByCommand()).hasSize(1);
  }
}

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

package org.sonar.server.issue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.ActionPlan;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class InternalRubyIssueServiceTest {

  private InternalRubyIssueService internalRubyIssueService;

  private IssueService issueService = mock(IssueService.class);
  private IssueCommentService commentService = mock(IssueCommentService.class);
  private ActionPlanService actionPlanService = mock(ActionPlanService.class);

  @Before
  public void before(){
    internalRubyIssueService = new InternalRubyIssueService(issueService, commentService, actionPlanService);
  }

  @Test
  public void should_create_action_plan(){
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");
    parameters.put("projectKey", "org.sonar.Sample");
    parameters.put("deadLine", "13/05/2113");

    ArgumentCaptor<ActionPlan> actionPlanCaptor = ArgumentCaptor.forClass(ActionPlan.class);
    Result result = internalRubyIssueService.createActionPlan(parameters);
    assertThat(result.ok()).isTrue();

    verify(actionPlanService).create(actionPlanCaptor.capture(), eq("org.sonar.Sample"));
    ActionPlan actionPlan = actionPlanCaptor.getValue();

    assertThat(actionPlan).isNotNull();
    assertThat(actionPlan.key()).isNotNull();
    assertThat(actionPlan.name()).isEqualTo("Long term");
    assertThat(actionPlan.description()).isEqualTo("Long term issues");
    assertThat(actionPlan.deadLine()).isNotNull();
  }

  @Test
  public void should_get_error_on_action_plan_result_when_no_project(){
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");

    Result result = internalRubyIssueService.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(new Result.Message("errors.cant_be_empty", "project"));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_no_name(){
    Map<String, String> parameters = newHashMap();
    parameters.put("name", null);
    parameters.put("description", "Long term issues");
    parameters.put("projectKey", "org.sonar.Sample");

    Result result = internalRubyIssueService.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(new Result.Message("errors.cant_be_empty", "name"));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_name_is_too_long(){
    Map<String, String> parameters = newHashMap();
    parameters.put("name", createLongString(201));
    parameters.put("description", "Long term issues");
    parameters.put("projectKey", "org.sonar.Sample");

    Result result = internalRubyIssueService.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(new Result.Message("errors.is_too_long", "name", 200));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_description_is_too_long(){
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", createLongString(1001));
    parameters.put("projectKey", "org.sonar.Sample");

    Result result = internalRubyIssueService.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(new Result.Message("errors.is_too_long", "description", 1000));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_dead_line_use_wrong_format(){
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");
    parameters.put("projectKey", "org.sonar.Sample");
    parameters.put("deadLine", "2013-05-18");

    Result result = internalRubyIssueService.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(new Result.Message("errors.is_not_valid", "date"));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_dead_line_is_in_the_past(){
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");
    parameters.put("projectKey", "org.sonar.Sample");
    parameters.put("deadLine", "01/01/2000");

    Result result = internalRubyIssueService.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(new Result.Message("issues_action_plans.date_cant_be_in_past"));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_name_is_already_used_for_project(){
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");
    parameters.put("projectKey", "org.sonar.Sample");

    when(actionPlanService.isNameAlreadyUsedForProject(anyString(), anyString())).thenReturn(true);

    Result result = internalRubyIssueService.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(new Result.Message("issues_action_plans.same_name_in_same_project"));
  }

  public String createLongString(int size){
    String result = "";
    for (int i = 0; i<size; i++) {
      result += "c";
    }
    return result;
  }
}

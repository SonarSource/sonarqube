/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.issue.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.user.UserFinder;
import org.sonar.api.utils.Durations;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.debt.DebtModelService;
import org.sonar.server.issue.IssueChangelogService;
import org.sonar.server.issue.IssueCommentService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.rule.RuleService;
import org.sonar.server.source.SourceService;
import org.sonar.server.source.ws.ScmWriter;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssuesWsTest {

  IssueShowAction showAction;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    IssueChangelogService issueChangelogService = mock(IssueChangelogService.class);
    IssueActionsWriter actionsWriter = mock(IssueActionsWriter.class);
    DebtModelService debtModelService = mock(DebtModelService.class);
    I18n i18n = mock(I18n.class);
    Durations durations = mock(Durations.class);

    showAction = new IssueShowAction(mock(DbClient.class), mock(IssueService.class), issueChangelogService, mock(IssueCommentService.class), actionsWriter, mock(ActionPlanService.class), mock(UserFinder.class),
      debtModelService, mock(RuleService.class), i18n, durations);
    SearchAction searchAction = new SearchAction(mock(DbClient.class), mock(IssueChangeDao.class), mock(IssueService.class), mock(IssueActionsWriter.class), mock(RuleService.class),
      mock(ActionPlanService.class), mock(UserFinder.class), mock(I18n.class), mock(Durations.class), mock(SourceService.class), mock(ScmWriter.class));
    tester = new WsTester(new IssuesWs(showAction, searchAction));
  }

  @Test
  public void define_controller() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("3.6");
    assertThat(controller.actions()).hasSize(14);
  }

  @Test
  public void define_show_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("show");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.since()).isEqualTo("4.2");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.handler()).isSameAs(showAction);
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(1);

    WebService.Param key = action.param("key");
    assertThat(key).isNotNull();
    assertThat(key.description()).isNotNull();
    assertThat(key.isRequired()).isFalse();
  }

  @Test
  public void define_changelog_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("changelog");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(2);
  }

  @Test
  public void define_assign_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("assign");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(3);
  }

  @Test
  public void define_add_comment_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("add_comment");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(3);
  }

  @Test
  public void define_delete_comment_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("delete_comment");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(1);
  }

  @Test
  public void define_edit_comment_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("edit_comment");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(3);
  }

  @Test
  public void define_change_severity_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("set_severity");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(3);
  }

  @Test
  public void define_plan_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("plan");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(3);
  }

  @Test
  public void define_do_transition_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("do_transition");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(3);
  }

  @Test
  public void define_transitions_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("transitions");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(1);
  }

  @Test
  public void define_create_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("create");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(6);
  }

  @Test
  public void define_do_action_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("do_action");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(3);
  }

  @Test
  public void define_bulk_change_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action action = controller.action("bulk_change");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isNotNull();
    assertThat(action.since()).isEqualTo("3.7");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(9);
  }

}

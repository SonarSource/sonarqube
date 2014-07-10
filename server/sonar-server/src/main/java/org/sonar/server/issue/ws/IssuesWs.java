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

import com.google.common.io.Resources;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class IssuesWs implements WebService {

  private final IssueShowAction showAction;
  private final IssueSearchAction searchAction;

  public IssuesWs(IssueShowAction showAction, IssueSearchAction searchAction) {
    this.showAction = showAction;
    this.searchAction = searchAction;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/issues");
    controller.setDescription("Coding rule issues");
    controller.setSince("3.6");

    showAction.define(controller);
    searchAction.define(controller);

    defineChangelogAction(controller);
    defineAssignAction(controller);
    defineAddCommentAction(controller);
    defineDeleteCommentAction(controller);
    defineEditCommentAction(controller);
    defineSetSeverityAction(controller);
    definePlanAction(controller);
    defineDoTransitionAction(controller);
    defineTransitionsAction(controller);
    defineCreateAction(controller);
    defineDoActionAction(controller);
    defineBulkChangeAction(controller);

    controller.done();
  }

  private void defineChangelogAction(NewController controller) {
    WebService.NewAction action = controller.createAction("changelog")
      .setDescription("Display changelog of an issue")
      .setSince("4.1")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "example-changelog.json"));

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    RailsHandler.addFormatParam(action);
  }

  private void defineAssignAction(NewController controller) {
    WebService.NewAction action = controller.createAction("assign")
      .setDescription("Assign/Unassign an issue. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("assignee")
      .setDescription("Login of the assignee")
      .setExampleValue("admin");
    RailsHandler.addFormatParam(action);
  }

  private void defineAddCommentAction(NewController controller) {
    WebService.NewAction action = controller.createAction("add_comment")
      .setDescription("Add a comment. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("text")
      .setDescription("Comment")
      .setExampleValue("blabla...");
    RailsHandler.addFormatParam(action);
  }

  private void defineDeleteCommentAction(NewController controller) {
    WebService.NewAction action = controller.createAction("delete_comment")
      .setDescription("Delete a comment. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("key")
      .setDescription("Key of the comment")
      .setRequired(true)
      .setExampleValue("392160d3-a4f2-4c52-a565-e4542cfa2096");
  }

  private void defineEditCommentAction(NewController controller) {
    WebService.NewAction action = controller.createAction("edit_comment")
      .setDescription("Edit a comment. Requires authentication and User role on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("key")
      .setDescription("Key of the comment")
      .setRequired(true)
      .setExampleValue("392160d3-a4f2-4c52-a565-e4542cfa2096");
    action.createParam("text")
      .setDescription("New comment")
      .setExampleValue("blabla2...");
    RailsHandler.addFormatParam(action);
  }

  private void defineSetSeverityAction(NewController controller) {
    WebService.NewAction action = controller.createAction("set_severity")
      .setDescription("Change severity. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("severity")
      .setDescription("New severity")
      .setExampleValue(Severity.BLOCKER)
      .setPossibleValues(Severity.ALL);
    RailsHandler.addFormatParam(action);
  }

  private void definePlanAction(NewController controller) {
    WebService.NewAction action = controller.createAction("plan")
      .setDescription("Plan/Unplan an issue. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("plan")
      .setDescription("Key of the action plan")
      .setExampleValue("3f19de90-1521-4482-a737-a311758ff513");
    RailsHandler.addFormatParam(action);
  }

  private void defineDoTransitionAction(NewController controller) {
    WebService.NewAction action = controller.createAction("do_transition")
      .setDescription("Do workflow transition on an issue. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("transition")
      .setDescription("Transition")
      .setExampleValue("reopen")
      .setPossibleValues(DefaultTransitions.ALL);
    RailsHandler.addFormatParam(action);
  }

  private void defineTransitionsAction(NewController controller) {
    WebService.NewAction action = controller.createAction("transitions")
      .setDescription("Get Possible Workflow Transitions for an Issue. Requires Browse permission on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "example-transitions.json"));

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
  }

  private void defineCreateAction(NewController controller) {
    WebService.NewAction action = controller.createAction("create")
      .setDescription("Create a manual issue. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("component")
      .setDescription("Key of the component on which to log the issue")
      .setRequired(true)
      .setExampleValue("org.apache.struts:struts:org.apache.struts.Action");
    action.createParam("rule")
      .setDescription("Manual rule key")
      .setRequired(true)
      .setExampleValue("manual:performance");
    action.createParam("severity")
      .setDescription("Severity of the issue")
      .setExampleValue(Severity.BLOCKER + "," + Severity.CRITICAL)
      .setPossibleValues(Severity.ALL);
    action.createParam("line")
      .setDescription("Line on which to log the issue. " +
        "If no line is specified, the issue is attached to the component and not to a specific line")
      .setExampleValue("15");
    action.createParam("message")
      .setDescription("Description of the issue")
      .setExampleValue("blabla...");
    RailsHandler.addFormatParam(action);
  }

  private void defineDoActionAction(NewController controller) {
    WebService.NewAction action = controller.createAction("do_action")
      .setDescription("Do workflow transition on an issue. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("actionKey")
      .setDescription("Action to perform")
      .setExampleValue("link-to-jira");
    RailsHandler.addFormatParam(action);
  }

  private void defineBulkChangeAction(NewController controller) {
    WebService.NewAction action = controller.createAction("bulk_change")
      .setDescription("Bulk change on issues. Requires authentication and User role on project(s)")
      .setSince("3.7")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issues")
      .setDescription("Comma-separated list of issue keys")
      .setRequired(true)
      .setExampleValue("01fc972e-2a3c-433e-bcae-0bd7f88f5123,01fc972e-2a3c-433e-bcae-0bd7f88f9999");
    action.createParam("actions")
      .setDescription("Comma-separated list of actions to perform. Possible values: assign | set_severity | plan | do_transition")
      .setRequired(true)
      .setExampleValue("assign,plan");
    action.createParam("assign.assignee")
      .setDescription("To assign the list of issues to a specific user (login), or unassign all the issues")
      .setExampleValue("sbrandhof");
    action.createParam("set_severity.severity")
      .setDescription("To change the severity of the list of issues")
      .setExampleValue(Severity.BLOCKER)
      .setPossibleValues(Severity.ALL);
    action.createParam("plan.plan")
      .setDescription("To plan the list of issues to a specific action plan (key), or unlink all the issues from an action plan")
      .setExampleValue("3f19de90-1521-4482-a737-a311758ff513");
    action.createParam("do_transition.transition")
      .setDescription("Transition")
      .setExampleValue("reopen")
      .setPossibleValues(DefaultTransitions.ALL);
    action.createParam("comment")
      .setDescription("To add a comment to a list of issues")
      .setExampleValue("blabla...");
    action.createParam("sendNotifications")
      .setDescription("Available since version 4.0")
      .setDefaultValue("false")
      .setBooleanPossibleValues();
    RailsHandler.addFormatParam(action);
  }

}

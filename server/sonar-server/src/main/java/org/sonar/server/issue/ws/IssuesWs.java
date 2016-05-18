/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
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
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class IssuesWs implements WebService {

  public static final String API_ENDPOINT = "api/issues";

  public static final String CHANGELOG_ACTION = "changelog";
  public static final String ADD_COMMENT_ACTION = "add_comment";
  public static final String DELETE_COMMENT_ACTION = "delete_comment";
  public static final String EDIT_COMMENT_ACTION = "edit_comment";
  public static final String TRANSITIONS_ACTION = "transitions";
  public static final String BULK_CHANGE_ACTION = "bulk_change";

  private final IssuesWsAction[] actions;

  public IssuesWs(IssuesWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(API_ENDPOINT);
    controller.setDescription("Read and update issues.");
    controller.setSince("3.6");
    for (IssuesWsAction action : actions) {
      action.define(controller);
    }
    defineRailsActions(controller);
    controller.done();
  }

  private static void defineRailsActions(NewController controller) {
    defineChangelogAction(controller);
    defineAddCommentAction(controller);
    defineDeleteCommentAction(controller);
    defineEditCommentAction(controller);
    defineTransitionsAction(controller);
    defineBulkChangeAction(controller);
  }

  private static void defineChangelogAction(NewController controller) {
    WebService.NewAction action = controller.createAction(CHANGELOG_ACTION)
      .setDescription("Display changelog of an issue")
      .setSince("4.1")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(IssuesWs.class, "example-changelog.json"));

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    RailsHandler.addFormatParam(action);
  }

  private static void defineAddCommentAction(NewController controller) {
    WebService.NewAction action = controller.createAction(ADD_COMMENT_ACTION)
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

  private static void defineDeleteCommentAction(NewController controller) {
    WebService.NewAction action = controller.createAction(DELETE_COMMENT_ACTION)
      .setDescription("Delete a comment. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("key")
      .setDescription("Key of the comment")
      .setRequired(true)
      .setExampleValue("392160d3-a4f2-4c52-a565-e4542cfa2096");
  }

  private static void defineEditCommentAction(NewController controller) {
    WebService.NewAction action = controller.createAction(EDIT_COMMENT_ACTION)
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

  private static void defineTransitionsAction(NewController controller) {
    WebService.NewAction action = controller.createAction(TRANSITIONS_ACTION)
      .setDescription("Get Possible Workflow Transitions for an Issue. Requires Browse permission on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(IssuesWs.class, "example-transitions.json"));

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
  }

  private static void defineBulkChangeAction(NewController controller) {
    WebService.NewAction action = controller.createAction(BULK_CHANGE_ACTION)
      .setDescription("Bulk change on issues. Requires authentication and User role on project(s)")
      .setSince("3.7")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issues")
      .setDescription("Comma-separated list of issue keys")
      .setRequired(true)
      .setExampleValue("01fc972e-2a3c-433e-bcae-0bd7f88f5123,01fc972e-2a3c-433e-bcae-0bd7f88f9999");
    action.createParam("actions")
      .setDescription("Comma-separated list of actions to perform. Possible values: assign | set_severity | do_transition | plan.<br>" +
        "In 5.5 action plans are dropped. plan action has no effect.")
      .setRequired(true)
      .setExampleValue("assign,set_severity");
    action.createParam("assign.assignee")
      .setDescription("To assign the list of issues to a specific user (login), or un-assign all the issues")
      .setExampleValue("john.smith");
    action.createParam("set_severity.severity")
      .setDescription("To change the severity of the list of issues")
      .setExampleValue(Severity.BLOCKER)
      .setPossibleValues(Severity.ALL);
    action.createParam("set_type.type")
      .setDescription("To change the type of the list of issues")
      .setExampleValue(RuleType.BUG)
      .setPossibleValues(RuleType.names())
      .setSince("5.5");
    action.createParam("plan.plan")
      .setDescription("In 5.5, action plans are dropped. Has no effect. To plan the list of issues to a specific action plan (key), or unlink all the issues from an action plan")
      .setDeprecatedSince("5.5")
      .setExampleValue("3f19de90-1521-4482-a737-a311758ff513");
    action.createParam("do_transition.transition")
      .setDescription("Transition")
      .setExampleValue("reopen")
      .setPossibleValues(DefaultTransitions.ALL);
    action.createParam("comment")
      .setDescription("To add a comment to a list of issues")
      .setExampleValue("Here is my comment");
    action.createParam("sendNotifications")
      .setDescription("Available since version 4.0")
      .setDefaultValue("false")
      .setPossibleValues("true", "false");
    RailsHandler.addFormatParam(action);
  }

}

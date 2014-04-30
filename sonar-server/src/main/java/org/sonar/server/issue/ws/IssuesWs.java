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
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class IssuesWs implements WebService {

  private final IssueShowAction showHandler;

  public IssuesWs(IssueShowAction showHandler) {
    this.showHandler = showHandler;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/issues");
    controller.setDescription("Coding rule issues");
    controller.setSince("3.6");

    showHandler.define(controller);
    defineSearchAction(controller);
    defineAssignAction(controller);
    defineAddCommentAction(controller);
    defineDeleteCommentAction(controller);
    defineEditCommentAction(controller);
    defineChangeSeverityAction(controller);
    definePlanAction(controller);
    defineDoTransitionAction(controller);
    defineTransitionsAction(controller);
    defineCreateAction(controller);
    defineBulkChangeAction(controller);

    controller.done();
  }

  private void defineSearchAction(NewController controller) {
    WebService.NewAction action = controller.createAction("search")
      .setDescription("Get a list of issues. If the number of issues is greater than 10,000, only the first 10,000 ones are returned by the web service. " +
        "Requires Browse permission on project(s).")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "example-search.json"));

    action.createParam("issues")
      .setDescription("Comma-separated list of issue keys.")
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("severities")
      .setDescription("Comma-separated list of severities.")
      .setExampleValue(Severity.BLOCKER + "," + Severity.CRITICAL)
      .setPossibleValues(Severity.ALL);
    action.createParam("statuses")
      .setDescription("Comma-separated list of statuses.")
      .setExampleValue(Issue.STATUS_OPEN + "," + Issue.STATUS_REOPENED)
      .setPossibleValues(Issue.STATUSES);
    action.createParam("resolutions")
      .setDescription("Comma-separated list of resolutions.")
      .setExampleValue(Issue.RESOLUTION_FIXED + "," + Issue.RESOLUTION_REMOVED)
      .setPossibleValues(Issue.RESOLUTIONS);
    action.createParam("resolved")
      .setDescription("To match resolved or unresolved issues.")
      .setExampleValue("true")
      .setPossibleValues("true", "false");
    action.createParam("components")
      .setDescription("To retrieve issues associated to a specific list of components (comma-separated list of component keys). " +
        "Note that if you set the value to a project key, only issues associated to this project are retrieved. " +
        "Issues associated to its sub-components (such as files, packages, etc.) are not retrieved. See also componentRoots.")
      .setExampleValue("org.apache.struts:struts:org.apache.struts.Action");
    action.createParam("componentRoots")
      .setDescription("To retrieve issues associated to a specific list of components and their sub-components (comma-separated list of component keys). " +
        "Views are not supported.")
      .setExampleValue("org.apache.struts:struts");
    action.createParam("rules")
      .setDescription("Comma-separated list of coding rule keys. Format is <repository>:<rule>.")
      .setExampleValue("squid:AvoidCycles");
    action.createParam("actionPlans")
      .setDescription("Comma-separated list of action plan keys (not names).")
      .setExampleValue("3f19de90-1521-4482-a737-a311758ff513");
    action.createParam("planned")
      .setDescription("To retrieve issues associated to an action plan or not.")
      .setExampleValue("true")
      .setPossibleValues("true", "false");
    action.createParam("reporters")
      .setDescription("Comma-separated list of reporter logins.")
      .setExampleValue("admin");
    action.createParam("assignees")
      .setDescription("Comma-separated list of assignee logins.")
      .setExampleValue("admin,usera");
    action.createParam("assigned")
      .setDescription("To retrieve assigned or unassigned issues.")
      .setExampleValue("true")
      .setPossibleValues("true", "false");
    action.createParam("createdAfter")
      .setDescription("To retrieve issues created after the given date (inclusive). Format: date or datetime ISO formats.")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam("createdBefore")
      .setDescription("To retrieve issues created before the given date (exclusive). Format: date or datetime ISO formats.")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam("pageSize")
      .setDescription("Maximum number of results per page.\n" +
        "Default value: 100 (except when the 'components' parameter is set, value is set to \"-1\" in this case)\n" +
        "If set to \"-1\", the max possible value is used.")
      .setExampleValue("50");
    action.createParam("pageIndex")
      .setDescription("Index of the selected page")
      .setDefaultValue("1")
      .setExampleValue("2");
  }

  private void defineAssignAction(NewController controller) {
    WebService.NewAction action = controller.createAction("assign")
      .setDescription("Assign/Unassign an issue. Requires authentication and Browse permission on project.")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue.")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("assignee")
      .setDescription("Login of the assignee.")
      .setExampleValue("admin");
  }

  private void defineAddCommentAction(NewController controller) {
    WebService.NewAction action = controller.createAction("add_comment")
      .setDescription("Add a comment. Requires authentication and Browse permission on project.")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue.")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("text")
      .setDescription("Comment.")
      .setExampleValue("blabla...");
  }

  private void defineDeleteCommentAction(NewController controller) {
    WebService.NewAction action = controller.createAction("delete_comment")
      .setDescription("Delete a comment. Requires authentication and Browse permission on project.")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("key")
      .setDescription("Key of the comment.")
      .setRequired(true)
      .setExampleValue("392160d3-a4f2-4c52-a565-e4542cfa2096");
  }

  private void defineEditCommentAction(NewController controller) {
    WebService.NewAction action = controller.createAction("edit_comment")
      .setDescription("Edit a comment. Requires authentication and User role on project.")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("key")
      .setDescription("Key of the comment.")
      .setRequired(true)
      .setExampleValue("392160d3-a4f2-4c52-a565-e4542cfa2096");
    action.createParam("text")
      .setDescription("New comment.")
      .setExampleValue("blabla2...");
  }

  private void defineChangeSeverityAction(NewController controller) {
    WebService.NewAction action = controller.createAction("set_severity")
      .setDescription("Change severity. Requires authentication and Browse permission on project.")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue.")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("severity")
      .setDescription("New severity.")
      .setExampleValue(Severity.BLOCKER)
      .setPossibleValues(Severity.ALL);
  }

  private void definePlanAction(NewController controller) {
    WebService.NewAction action = controller.createAction("plan")
      .setDescription("Plan/Unplan an issue. Requires authentication and Browse permission on project.")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue.")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("plan")
      .setDescription("Key of the action plan.")
      .setExampleValue("3f19de90-1521-4482-a737-a311758ff513");
  }

  private void defineDoTransitionAction(NewController controller) {
    WebService.NewAction action = controller.createAction("do_transition")
      .setDescription("Do workflow transition on an issue. Requires authentication and Browse permission on project.")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue.")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("transition")
      .setDescription("Transition.")
      .setExampleValue("reopen")
      .setPossibleValues(DefaultTransitions.ALL);
  }

  private void defineTransitionsAction(NewController controller) {
    WebService.NewAction action = controller.createAction("transitions")
      .setDescription("Get Possible Workflow Transitions for an Issue. Requires Browse permission on project.")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "example-transitions.json"));

    action.createParam("issue")
      .setDescription("Key of the issue.")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
  }

  private void defineCreateAction(NewController controller) {
    WebService.NewAction action = controller.createAction("create")
      .setDescription("Create a manual issue. Requires authentication and Browse permission on project.")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("component")
      .setDescription("Key of the component on which to log the issue.")
      .setRequired(true)
      .setExampleValue("org.apache.struts:struts:org.apache.struts.Action");
    action.createParam("rule")
      .setDescription("Manual rule key.")
      .setRequired(true)
      .setExampleValue("manual:performance");
    action.createParam("severity")
      .setDescription("Severity of the issue.")
      .setExampleValue(Severity.BLOCKER + "," + Severity.CRITICAL)
      .setPossibleValues(Severity.ALL);
    action.createParam("line")
      .setDescription("Line on which to log the issue.\n" +
        "If no line is specified, the issue is attached to the component and not to a specific line.")
      .setExampleValue("15");
    action.createParam("message")
      .setDescription("Description of the issue.")
      .setExampleValue("blabla...");
  }

  private void defineBulkChangeAction(NewController controller) {
    WebService.NewAction action = controller.createAction("bulk_change")
      .setDescription("Bulk change on issues. Requires authentication and User role on project(s).")
      .setSince("3.7")
      .setHandler(RailsHandler.INSTANCE)
      .setPost(true);

    action.createParam("issues")
      .setDescription("Comma-separated list of issue keys.")
      .setRequired(true)
      .setExampleValue("01fc972e-2a3c-433e-bcae-0bd7f88f5123,01fc972e-2a3c-433e-bcae-0bd7f88f9999");
    action.createParam("actions")
      // TODO It's not possible to load actions as actions defined by plugins are not availables
      .setDescription("Comma-separated list of actions to perform. Possible values: assign | set_severity | plan | do_transition.")
      .setRequired(true)
      .setExampleValue("assign,plan");
    action.createParam("assign.assignee")
      .setDescription("To assign the list of issues to a specific user (login), or unassign all the issues.")
      .setExampleValue("sbrandhof");
    action.createParam("set_severity.severity")
      .setDescription("To change the severity of the list of issues.")
      .setExampleValue(Severity.BLOCKER)
      .setPossibleValues(Severity.ALL);
    action.createParam("plan.plan")
      .setDescription("To plan the list of issues to a specific action plan (key), or unlink all the issues from an action plan.")
      .setExampleValue("3f19de90-1521-4482-a737-a311758ff513");
    action.createParam("do_transition.transition")
      .setDescription("Transition.")
      .setExampleValue("reopen")
      .setPossibleValues(DefaultTransitions.ALL);
    action.createParam("comment")
      .setDescription("To add a comment to a list of issues.")
      .setExampleValue("blabla...");
    action.createParam("sendNotifications")
      .setDescription("Available since version 4.0.")
      .setDefaultValue("false")
      .setExampleValue("true")
      .setPossibleValues("true", "false");
  }

}

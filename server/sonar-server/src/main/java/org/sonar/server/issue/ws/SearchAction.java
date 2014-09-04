package org.sonar.server.issue.ws;

import com.google.common.io.Resources;
import org.sonar.api.i18n.I18n;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Durations;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.issue.index.IssueIndex;

public class SearchAction implements RequestHandler {

  public static final String SEARCH_ACTION = "es-search";

  private static final String ACTIONS_EXTRA_FIELD = "actions";
  private static final String TRANSITIONS_EXTRA_FIELD = "transitions";
  private static final String ASSIGNEE_NAME_EXTRA_FIELD = "assigneeName";
  private static final String REPORTER_NAME_EXTRA_FIELD = "reporterName";
  private static final String ACTION_PLAN_NAME_EXTRA_FIELD = "actionPlanName";

  private static final String EXTRA_FIELDS_PARAM = "extra_fields";

  private final IssueIndex issueIndex;
  private final IssueActionsWriter actionsWriter;
  private final I18n i18n;
  private final Durations durations;

  public SearchAction(IssueIndex index, IssueActionsWriter actionsWriter, I18n i18n, Durations durations) {
    this.issueIndex = index;
    this.actionsWriter = actionsWriter;
    this.i18n = i18n;
    this.durations = durations;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(SEARCH_ACTION)
      .setDescription("Get a list of issues. If the number of issues is greater than 10,000, only the first 10,000 ones are returned by the web service. " +
        "Requires Browse permission on project(s)")
      .setSince("3.6")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-search.json"));

    action.createParam(IssueFilterParameters.ISSUES)
      .setDescription("Comma-separated list of issue keys")
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam(IssueFilterParameters.SEVERITIES)
      .setDescription("Comma-separated list of severities")
      .setExampleValue(Severity.BLOCKER + "," + Severity.CRITICAL)
      .setPossibleValues(Severity.ALL);
    action.createParam(IssueFilterParameters.STATUSES)
      .setDescription("Comma-separated list of statuses")
      .setExampleValue(Issue.STATUS_OPEN + "," + Issue.STATUS_REOPENED)
      .setPossibleValues(Issue.STATUSES);
    action.createParam(IssueFilterParameters.RESOLUTIONS)
      .setDescription("Comma-separated list of resolutions")
      .setExampleValue(Issue.RESOLUTION_FIXED + "," + Issue.RESOLUTION_REMOVED)
      .setPossibleValues(Issue.RESOLUTIONS);
    action.createParam(IssueFilterParameters.RESOLVED)
      .setDescription("To match resolved or unresolved issues")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.COMPONENTS)
      .setDescription("To retrieve issues associated to a specific list of components (comma-separated list of component keys). " +
        "Note that if you set the value to a project key, only issues associated to this project are retrieved. " +
        "Issues associated to its sub-components (such as files, packages, etc.) are not retrieved. See also componentRoots")
      .setExampleValue("org.apache.struts:struts:org.apache.struts.Action");
    action.createParam(IssueFilterParameters.COMPONENT_ROOTS)
      .setDescription("To retrieve issues associated to a specific list of components and their sub-components (comma-separated list of component keys). " +
        "Views are not supported")
      .setExampleValue("org.apache.struts:struts");
    action.createParam(IssueFilterParameters.RULES)
      .setDescription("Comma-separated list of coding rule keys. Format is <repository>:<rule>")
      .setExampleValue("squid:AvoidCycles");
    action.createParam(IssueFilterParameters.HIDE_RULES)
      .setDescription("To not return rules")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.ACTION_PLANS)
      .setDescription("Comma-separated list of action plan keys (not names)")
      .setExampleValue("3f19de90-1521-4482-a737-a311758ff513");
    action.createParam(IssueFilterParameters.PLANNED)
      .setDescription("To retrieve issues associated to an action plan or not")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.REPORTERS)
      .setDescription("Comma-separated list of reporter logins")
      .setExampleValue("admin");
    action.createParam(IssueFilterParameters.ASSIGNEES)
      .setDescription("Comma-separated list of assignee logins")
      .setExampleValue("admin,usera");
    action.createParam(IssueFilterParameters.ASSIGNED)
      .setDescription("To retrieve assigned or unassigned issues")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.LANGUAGES)
      .setDescription("Comma-separated list of languages. Available since 4.4")
      .setExampleValue("java,js");
    action.createParam(EXTRA_FIELDS_PARAM)
      .setDescription("Add some extra fields on each issue. Available since 4.4")
      .setPossibleValues(ACTIONS_EXTRA_FIELD, TRANSITIONS_EXTRA_FIELD, ASSIGNEE_NAME_EXTRA_FIELD, REPORTER_NAME_EXTRA_FIELD, ACTION_PLAN_NAME_EXTRA_FIELD);
    action.createParam(IssueFilterParameters.CREATED_AT)
      .setDescription("To retrieve issues created at a given date. Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam(IssueFilterParameters.CREATED_AFTER)
      .setDescription("To retrieve issues created after the given date (inclusive). Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam(IssueFilterParameters.CREATED_BEFORE)
      .setDescription("To retrieve issues created before the given date (exclusive). Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam(IssueFilterParameters.PAGE_SIZE)
      .setDescription("Maximum number of results per page. " +
        "Default value: 100 (except when the 'components' parameter is set, value is set to \"-1\" in this case). " +
        "If set to \"-1\", the max possible value is used")
      .setExampleValue("50");
    action.createParam(IssueFilterParameters.PAGE_INDEX)
      .setDescription("Index of the selected page")
      .setDefaultValue("1")
      .setExampleValue("2");
    action.createParam(IssueFilterParameters.SORT)
      .setDescription("Sort field")
      .setPossibleValues(IssueQuery.SORTS);
    action.createParam(IssueFilterParameters.ASC)
      .setDescription("Ascending sort")
      .setBooleanPossibleValues();
    action.createParam("format")
      .setDescription("Only json format is available. This parameter is kept only for backward compatibility and shouldn't be used anymore");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {

  }
}

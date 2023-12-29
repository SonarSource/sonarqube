/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.CleanCodeAttributeCategory;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.status.IssueStatus;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.issue.SearchRequest;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueQuery;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.issue.index.IssueScope;
import org.sonar.server.security.SecurityStandards.SQCategory;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Issues.SearchWsResponse;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.api.issue.Issue.RESOLUTIONS;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.STATUS_IN_REVIEW;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.issue.index.IssueIndex.FACET_ASSIGNED_TO_ME;
import static org.sonar.server.issue.index.IssueIndex.FACET_PROJECTS;
import static org.sonar.server.issue.index.IssueQueryFactory.ISSUE_STATUSES;
import static org.sonar.server.issue.index.IssueQueryFactory.UNKNOWN;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_INSECURE_INTERACTION;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_POROUS_DEFENSES;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_RISKY_RESOURCE;
import static org.sonar.server.security.SecurityStandards.UNKNOWN_STANDARD;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASC;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNED;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_AUTHOR;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CODE_VARIANTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AFTER;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_BEFORE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_IN_LAST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CWE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FILES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IMPACT_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IMPACT_SOFTWARE_QUALITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IN_NEW_CODE_PERIOD;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ON_COMPONENT_ONLY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_ASVS_40;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_ASVS_LEVEL;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_TOP_10;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_TOP_10_2021;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PCI_DSS_32;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PCI_DSS_40;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PULL_REQUEST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLUTIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLVED;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RULES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SANS_TOP_25;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SCOPES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SONARSOURCE_SECURITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TIMEZONE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TYPES;

public class SearchAction implements IssuesWsAction {
  private static final String LOGIN_MYSELF = "__me__";
  private static final Set<String> ISSUE_SCOPES = Arrays.stream(IssueScope.values()).map(Enum::name).collect(Collectors.toSet());
  private static final EnumSet<RuleType> ALL_RULE_TYPES_EXCEPT_SECURITY_HOTSPOTS = EnumSet.complementOf(EnumSet.of(RuleType.SECURITY_HOTSPOT));

  static final List<String> SUPPORTED_FACETS = List.of(
    FACET_PROJECTS,
    PARAM_FILES,
    FACET_ASSIGNED_TO_ME,
    PARAM_SEVERITIES,
    PARAM_STATUSES,
    PARAM_RESOLUTIONS,
    PARAM_RULES,
    PARAM_ASSIGNEES,
    PARAM_AUTHOR,
    PARAM_DIRECTORIES,
    PARAM_SCOPES,
    PARAM_LANGUAGES,
    PARAM_TAGS,
    PARAM_TYPES,
    PARAM_PCI_DSS_32,
    PARAM_PCI_DSS_40,
    PARAM_OWASP_ASVS_40,
    PARAM_OWASP_TOP_10,
    PARAM_OWASP_TOP_10_2021,
    PARAM_SANS_TOP_25,
    PARAM_CWE,
    PARAM_CREATED_AT,
    PARAM_SONARSOURCE_SECURITY,
    PARAM_CODE_VARIANTS,
    PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES,
    PARAM_IMPACT_SOFTWARE_QUALITIES,
    PARAM_IMPACT_SEVERITIES,
    PARAM_ISSUE_STATUSES);

  private static final String INTERNAL_PARAMETER_DISCLAIMER = "This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. ";
  private static final String NEW_FACET_ADDED_MESSAGE = "Facet '%s' has been added";
  private static final String NEW_PARAM_ADDED_MESSAGE = "Param '%s' has been added";
  private static final Set<String> FACETS_REQUIRING_PROJECT = newHashSet(PARAM_FILES, PARAM_DIRECTORIES);

  private final UserSession userSession;
  private final IssueIndex issueIndex;
  private final IssueQueryFactory issueQueryFactory;
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker;
  private final SearchResponseLoader searchResponseLoader;
  private final SearchResponseFormat searchResponseFormat;
  private final System2 system2;
  private final DbClient dbClient;

  public SearchAction(UserSession userSession, IssueIndex issueIndex, IssueQueryFactory issueQueryFactory, IssueIndexSyncProgressChecker issueIndexSyncProgressChecker,
    SearchResponseLoader searchResponseLoader, SearchResponseFormat searchResponseFormat, System2 system2, DbClient dbClient) {
    this.userSession = userSession;
    this.issueIndex = issueIndex;
    this.issueQueryFactory = issueQueryFactory;
    this.issueIndexSyncProgressChecker = issueIndexSyncProgressChecker;
    this.searchResponseLoader = searchResponseLoader;
    this.searchResponseFormat = searchResponseFormat;
    this.system2 = system2;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(ACTION_SEARCH)
      .setHandler(this)
      .setDescription("Search for issues.<br>Requires the 'Browse' permission on the specified project(s). <br>"
        + "For applications, it also requires 'Browse' permission on its child projects."
        + "<br/>When issue indexation is in progress returns 503 service unavailable HTTP code.")
      .setSince("3.6")
      .setChangelog(
        new Change("10.4", "Value '%s' for 'transition' response field is deprecated, use '%s' instead".formatted(DefaultTransitions.WONT_FIX, DefaultTransitions.ACCEPT)),
        new Change("10.4", "Possible value '%s' for 'transition' response field has been added".formatted(DefaultTransitions.ACCEPT)),
        new Change("10.4", format(NEW_PARAM_ADDED_MESSAGE, PARAM_ISSUE_STATUSES)),
        new Change("10.4", format("Parameters '%s' and '%s' are deprecated in favor of '%s'.", PARAM_RESOLUTIONS, PARAM_STATUSES, PARAM_ISSUE_STATUSES)),
        new Change("10.4", format("Parameters '%s' and '%s' are deprecated, use '%s' and '%s' instead.", PARAM_SEVERITIES, PARAM_TYPES,
          PARAM_IMPACT_SEVERITIES, PARAM_IMPACT_SOFTWARE_QUALITIES)),
        new Change("10.4", format(NEW_FACET_ADDED_MESSAGE, PARAM_ISSUE_STATUSES)),
        new Change("10.4", format("Facets '%s' and '%s' are deprecated in favor of '%s'", PARAM_RESOLUTIONS, PARAM_STATUSES, PARAM_ISSUE_STATUSES)),
        new Change("10.4", "Response fields 'severity' and 'type' are deprecated, use 'impacts' instead."),
        new Change("10.4", "Response field 'issueStatus' added"),
        new Change("10.4", "Response fields 'status' and 'resolutions' are deprecated, in favor of 'issueStatus'"),
        new Change("10.4", format("Possible value '%s' for 'issueStatus' field is deprecated.", IssueStatus.CONFIRMED)),
        new Change("10.2", "Add 'impacts', 'cleanCodeAttribute', 'cleanCodeAttributeCategory' fields to the response"),
        new Change("10.2", format(NEW_PARAM_ADDED_MESSAGE, PARAM_IMPACT_SOFTWARE_QUALITIES)),
        new Change("10.2", format(NEW_PARAM_ADDED_MESSAGE, PARAM_IMPACT_SEVERITIES)),
        new Change("10.2", format(NEW_PARAM_ADDED_MESSAGE, PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES)),
        new Change("10.2", format(NEW_FACET_ADDED_MESSAGE, PARAM_IMPACT_SOFTWARE_QUALITIES)),
        new Change("10.2", format(NEW_FACET_ADDED_MESSAGE, PARAM_IMPACT_SEVERITIES)),
        new Change("10.2", format(NEW_FACET_ADDED_MESSAGE, PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES)),
        new Change("10.2", format("Parameter '%s' renamed to '%s'", PARAM_COMPONENT_KEYS, PARAM_COMPONENTS)),
        new Change("10.1", "Add the 'codeVariants' parameter, facet and response field"),
        new Change("10.0", "Parameter 'sansTop25' is deprecated"),
        new Change("10.0", "The value 'sansTop25' for the parameter 'facets' has been deprecated"),
        new Change("10.0", format("Deprecated value 'ASSIGNEE' in parameter '%s' is dropped", Param.SORT)),
        new Change("10.0", format("Parameter 'sinceLeakPeriod' is removed, please use '%s' instead", PARAM_IN_NEW_CODE_PERIOD)),
        new Change("9.8", "Add message formatting to issue and locations response"),
        new Change("9.8", "response fields 'total', 's', 'ps' have been deprecated, please use 'paging' object instead"),
        new Change("9.7", "Issues flows in the response may contain a description and a type"),
        new Change("9.6", "Response field 'fromHotspot' dropped."),
        new Change("9.6", "Added facets 'pciDss-3.2' and 'pciDss-4.0"),
        new Change("9.6", "Added parameters 'pciDss-3.2' and 'pciDss-4.0"),
        new Change("9.6", "Response field 'ruleDescriptionContextKey' added"),
        new Change("9.6", "New possible value for 'additionalFields' parameter: 'ruleDescriptionContextKey'"),
        new Change("9.6", "Facet 'moduleUuids' is dropped."),
        new Change("9.4", format("Parameter 'sinceLeakPeriod' is deprecated, please use '%s' instead", PARAM_IN_NEW_CODE_PERIOD)),
        new Change("9.2", "Response field 'quickFixAvailable' added"),
        new Change("9.1", "Deprecated parameters 'authors', 'facetMode' and 'moduleUuids' were dropped"),
        new Change("8.6", "Parameter 'timeZone' added"),
        new Change("8.5", "Facet 'fileUuids' is dropped in favour of the new facet 'files'" +
          "Note that they are not strictly identical, the latter returns the file paths."),
        new Change("8.5", "Internal parameter 'fileUuids' has been dropped"),
        new Change("8.4", "parameters 'componentUuids', 'projectKeys' has been dropped."),
        new Change("8.2", "'REVIEWED', 'TO_REVIEW' status param values are no longer supported"),
        new Change("8.2", "Security hotspots are no longer returned as type 'SECURITY_HOTSPOT' is not supported anymore, use dedicated api/hotspots"),
        new Change("8.2", "response field 'fromHotspot' has been deprecated and is no more populated"),
        new Change("8.2", "Status 'IN_REVIEW' for Security Hotspots has been deprecated"),
        new Change("7.8", format("added new Security Hotspots statuses : %s, %s and %s", STATUS_TO_REVIEW, STATUS_IN_REVIEW, STATUS_REVIEWED)),
        new Change("7.8", "Security hotspots are returned by default"),
        new Change("7.7", format("Value 'authors' in parameter '%s' is deprecated, please use '%s' instead", FACETS, PARAM_AUTHOR)),
        new Change("7.6", format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT_KEYS)),
        new Change("7.4", "The facet 'projectUuids' is dropped in favour of the new facet 'projects'. " +
          "Note that they are not strictly identical, the latter returns the project keys."),
        new Change("7.4", "Parameter 'facetMode' does not accept anymore deprecated value 'debt'"),
        new Change("7.3", "response field 'fromHotspot' added to issues that are security hotspots"),
        new Change("7.3", "added facets 'sansTop25', 'owaspTop10' and 'cwe'"),
        new Change("7.2", "response field 'externalRuleEngine' added to issues that have been imported from an external rule engine"),
        new Change("7.2", format("value 'ASSIGNEE' in parameter '%s' is deprecated, it won't have any effect", Param.SORT)),
        new Change("6.5", "parameters 'projects', 'projectUuids', 'moduleUuids', 'directories', 'fileUuids' are marked as internal"),
        new Change("6.3", "response field 'email' is renamed 'avatar'"),
        new Change("5.5", "response fields 'reporter' and 'actionPlan' are removed (drop of action plan and manual issue features)"),
        new Change("5.5", "parameters 'reporters', 'actionPlans' and 'planned' are dropped and therefore ignored (drop of action plan and manual issue features)"),
        new Change("5.5", "response field 'debt' is renamed 'effort'"))
      .setResponseExample(getClass().getResource("search-example.json"));

    action.addPagingParams(100, MAX_PAGE_SIZE);
    action.createParam(FACETS)
      .setDescription("Comma-separated list of the facets to be computed. No facet is computed by default.")
      .setPossibleValues(SUPPORTED_FACETS);
    action.addSortParams(IssueQuery.SORTS, null, true);
    action.createParam(PARAM_ADDITIONAL_FIELDS)
      .setSince("5.2")
      .setDescription("Comma-separated list of the optional fields to be returned in response. Action plans are dropped in 5.5, it is not returned in the response.")
      .setPossibleValues(SearchAdditionalField.possibleValues());
    addComponentRelatedParams(action);
    action.createParam(PARAM_ISSUES)
      .setDescription("Comma-separated list of issue keys")
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam(PARAM_SEVERITIES)
      .setDescription("Comma-separated list of severities")
      .setExampleValue(Severity.BLOCKER + "," + Severity.CRITICAL)
      .setPossibleValues(Severity.ALL)
      .setDeprecatedSince("10.4");
    action.createParam(PARAM_IMPACT_SOFTWARE_QUALITIES)
      .setSince("10.2")
      .setDescription("Comma-separated list of Software Qualities")
      .setExampleValue(SoftwareQuality.MAINTAINABILITY + "," + SoftwareQuality.RELIABILITY)
      .setPossibleValues(SoftwareQuality.values());
    action.createParam(PARAM_IMPACT_SEVERITIES)
      .setSince("10.2")
      .setDescription("Comma-separated list of Software Quality Severities")
      .setExampleValue(org.sonar.api.issue.impact.Severity.HIGH + "," + org.sonar.api.issue.impact.Severity.MEDIUM)
      .setPossibleValues(org.sonar.api.issue.impact.Severity.values());
    action.createParam(PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES)
      .setSince("10.2")
      .setDescription("Comma-separated list of Clean Code Attribute Categories")
      .setExampleValue(CleanCodeAttributeCategory.ADAPTABLE + "," + CleanCodeAttributeCategory.INTENTIONAL)
      .setPossibleValues(CleanCodeAttributeCategory.values());
    action.createParam(PARAM_STATUSES)
      .setDescription("Comma-separated list of statuses")
      .setExampleValue(STATUS_OPEN + "," + STATUS_REOPENED)
      .setPossibleValues(ISSUE_STATUSES)
      .setDeprecatedSince("10.4");
    action.createParam(PARAM_RESOLUTIONS)
      .setDescription("Comma-separated list of resolutions")
      .setExampleValue(RESOLUTION_FIXED + "," + RESOLUTION_REMOVED)
      .setPossibleValues(RESOLUTIONS)
      .setDeprecatedSince("10.4");
    action.createParam(PARAM_RESOLVED)
      .setDescription("To match resolved or unresolved issues")
      .setBooleanPossibleValues();
    action.createParam(PARAM_RULES)
      .setDescription("Comma-separated list of coding rule keys. Format is &lt;repository&gt;:&lt;rule&gt;")
      .setExampleValue("java:S1144");
    action.createParam(PARAM_TAGS)
      .setDescription("Comma-separated list of tags.")
      .setExampleValue("security,convention");
    action.createParam(PARAM_TYPES)
      .setDescription("Comma-separated list of types.")
      .setSince("5.5")
      .setDeprecatedSince("10.4")
      .setPossibleValues(ALL_RULE_TYPES_EXCEPT_SECURITY_HOTSPOTS)
      .setExampleValue(format("%s,%s", RuleType.CODE_SMELL, RuleType.BUG));
    action.createParam(PARAM_OWASP_ASVS_LEVEL)
      .setDescription("Level of OWASP ASVS categories.")
      .setSince("9.7")
      .setPossibleValues(1, 2, 3);
    action.createParam(PARAM_PCI_DSS_32)
      .setDescription("Comma-separated list of PCI DSS v3.2 categories.")
      .setSince("9.6")
      .setExampleValue("4,6.5.8,10.1");
    action.createParam(PARAM_PCI_DSS_40)
      .setDescription("Comma-separated list of PCI DSS v4.0 categories.")
      .setSince("9.6")
      .setExampleValue("4,6.5.8,10.1");
    action.createParam(PARAM_OWASP_ASVS_40)
      .setDescription("Comma-separated list of OWASP ASVS v4.0 categories.")
      .setSince("9.7")
      .setExampleValue("6,10.1.1");
    action.createParam(PARAM_OWASP_TOP_10)
      .setDescription("Comma-separated list of OWASP Top 10 2017 lowercase categories.")
      .setSince("7.3")
      .setPossibleValues("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10");
    action.createParam(PARAM_OWASP_TOP_10_2021)
      .setDescription("Comma-separated list of OWASP Top 10 2021 lowercase categories.")
      .setSince("9.4")
      .setPossibleValues("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10");
    action.createParam(PARAM_SANS_TOP_25)
      .setDescription("Comma-separated list of SANS Top 25 categories.")
      .setDeprecatedSince("10.0")
      .setSince("7.3")
      .setPossibleValues(SANS_TOP_25_INSECURE_INTERACTION, SANS_TOP_25_RISKY_RESOURCE, SANS_TOP_25_POROUS_DEFENSES);
    action.createParam(PARAM_CWE)
      .setDescription("Comma-separated list of CWE identifiers. Use '" + UNKNOWN_STANDARD + "' to select issues not associated to any CWE.")
      .setExampleValue("12,125," + UNKNOWN_STANDARD);
    action.createParam(PARAM_SONARSOURCE_SECURITY)
      .setDescription("Comma-separated list of SonarSource security categories. Use '" + SQCategory.OTHERS.getKey() + "' to select issues not associated" +
        " with any category")
      .setSince("7.8")
      .setPossibleValues(Arrays.stream(SQCategory.values()).map(SQCategory::getKey).toList());
    action.createParam(PARAM_AUTHOR)
      .setDescription("SCM accounts. To set several values, the parameter must be called once for each value.")
      .setExampleValue("author=torvalds@linux-foundation.org&author=linux@fondation.org");
    action.createParam(PARAM_ASSIGNEES)
      .setDescription("Comma-separated list of assignee logins. The value '__me__' can be used as a placeholder for user who performs the request")
      .setExampleValue("admin,usera,__me__");
    action.createParam(PARAM_ASSIGNED)
      .setDescription("To retrieve assigned or unassigned issues")
      .setBooleanPossibleValues();
    action.createParam(PARAM_SCOPES)
      .setDescription("Comma-separated list of scopes. Available since 8.5")
      .setPossibleValues(IssueScope.MAIN.name(), IssueScope.TEST.name())
      .setExampleValue(format("%s,%s", IssueScope.MAIN.name(), IssueScope.TEST.name()));
    action.createParam(PARAM_LANGUAGES)
      .setDescription("Comma-separated list of languages. Available since 4.4")
      .setExampleValue("java,js");
    action.createParam(PARAM_CREATED_AT)
      .setDescription("Datetime to retrieve issues created during a specific analysis")
      .setExampleValue("2017-10-19T13:00:00+0200");
    action.createParam(PARAM_CREATED_AFTER)
      .setDescription("To retrieve issues created after the given date (inclusive). <br>" +
        "Either a date (use '" + PARAM_TIMEZONE + "' attribute or it will default to server timezone) or datetime can be provided. <br>" +
        "If this parameter is set, createdInLast must not be set")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");
    action.createParam(PARAM_CREATED_BEFORE)
      .setDescription("To retrieve issues created before the given date (exclusive). <br>" +
        "Either a date (use '" + PARAM_TIMEZONE + "' attribute or it will default to server timezone) or datetime can be provided.")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");
    action.createParam(PARAM_CREATED_IN_LAST)
      .setDescription("To retrieve issues created during a time span before the current time (exclusive). " +
        "Accepted units are 'y' for year, 'm' for month, 'w' for week and 'd' for day. " +
        "If this parameter is set, createdAfter must not be set")
      .setExampleValue("1m2w (1 month 2 weeks)");
    action.createParam(PARAM_IN_NEW_CODE_PERIOD)
      .setDescription("To retrieve issues created in the new code period.<br>" +
        "If this parameter is set to a truthy value, createdAfter must not be set and one component uuid or key must be provided.")
      .setBooleanPossibleValues()
      .setSince("9.4");
    action.createParam(PARAM_TIMEZONE)
      .setDescription(
        "To resolve dates passed to '" + PARAM_CREATED_AFTER + "' or '" + PARAM_CREATED_BEFORE + "' (does not apply to datetime) and to compute creation date histogram")
      .setRequired(false)
      .setExampleValue("'Europe/Paris', 'Z' or '+02:00'")
      .setSince("8.6");
    action.createParam(PARAM_CODE_VARIANTS)
      .setDescription("Comma-separated list of code variants.")
      .setExampleValue("windows,linux")
      .setSince("10.1");
    action.createParam(PARAM_ISSUE_STATUSES)
      .setDescription("")
      .setPossibleValues(IssueStatus.values())
      .setExampleValue("%s,%S".formatted(IssueStatus.ACCEPTED, IssueStatus.FIXED))
      .setSince("10.4");
  }

  private static void addComponentRelatedParams(WebService.NewAction action) {
    action.createParam(PARAM_ON_COMPONENT_ONLY)
      .setDescription("Return only issues at a component's level, not on its descendants (modules, directories, files, etc). " +
        "This parameter is only considered when componentKeys is set.")
      .setBooleanPossibleValues()
      .setDefaultValue("false");

    action.createParam(PARAM_COMPONENTS)
      .setDeprecatedKey(PARAM_COMPONENT_KEYS, "10.2")
      .setDescription("Comma-separated list of component keys. Retrieve issues associated to a specific list of components (and all its descendants). " +
        "A component can be a portfolio, project, module, directory or file.")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_PROJECTS)
      .setDescription("To retrieve issues associated to a specific list of projects (comma-separated list of project keys). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "If this parameter is set, projectUuids must not be set.")
      .setInternal(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_DIRECTORIES)
      .setDescription("To retrieve issues associated to a specific list of directories (comma-separated list of directory paths). " +
        "This parameter is only meaningful when a module is selected. " +
        INTERNAL_PARAMETER_DISCLAIMER)
      .setInternal(true)
      .setSince("5.1")
      .setExampleValue("src/main/java/org/sonar/server/");

    action.createParam(PARAM_FILES)
      .setDescription("To retrieve issues associated to a specific list of files (comma-separated list of file paths). " +
        INTERNAL_PARAMETER_DISCLAIMER)
      .setInternal(true)
      .setExampleValue("src/main/java/org/sonar/server/Test.java");

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key. Not available in the community edition.")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setSince("6.6");

    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id. Not available in the community edition.")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001)
      .setSince("7.1");
  }

  @Override
  public final void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchRequest searchRequest = toSearchWsRequest(dbSession, request);
      checkIfNeedIssueSync(dbSession, searchRequest);
      SearchWsResponse searchWsResponse = doHandle(searchRequest);
      writeProtobuf(searchWsResponse, request, response);
    }
  }

  private SearchWsResponse doHandle(SearchRequest request) {
    // prepare the Elasticsearch request
    SearchOptions options = createSearchOptionsFromRequest(request);
    EnumSet<SearchAdditionalField> additionalFields = SearchAdditionalField.getFromRequest(request);
    IssueQuery query = issueQueryFactory.create(request);

    Set<String> facetsRequiringProjectParameter = options.getFacets().stream()
      .filter(FACETS_REQUIRING_PROJECT::contains)
      .collect(Collectors.toSet());
    checkArgument(facetsRequiringProjectParameter.isEmpty() ||
      (!query.projectUuids().isEmpty()), "Facet(s) '%s' require to also filter by project",
      String.join(",", facetsRequiringProjectParameter));

    // execute request
    SearchResponse result = issueIndex.search(query, options);
    result.getHits();
    List<String> issueKeys = Arrays.stream(result.getHits().getHits())
      .map(SearchHit::getId)
      .toList();

    // load the additional information to be returned in response
    SearchResponseLoader.Collector collector = new SearchResponseLoader.Collector(issueKeys);
    collectLoggedInUser(collector);
    collectRequestParams(collector, request);
    Facets facets = new Facets(result, Optional.ofNullable(query.timeZone()).orElse(system2.getDefaultTimeZone().toZoneId()));
    if (!options.getFacets().isEmpty()) {
      // add missing values to facets. For example if assignee "john" and facet on "assignees" are requested, then
      // "john" should always be listed in the facet. If it is not present, then it is added with value zero.
      // This is a constraint from webapp UX.
      completeFacets(facets, request, query);
      collectFacets(collector, facets);
    }
    SearchResponseData preloadedData = new SearchResponseData();
    preloadedData.addRules(List.copyOf(query.rules()));
    SearchResponseData data = searchResponseLoader.load(preloadedData, collector, additionalFields, facets);

    // FIXME allow long in Paging
    Paging paging = forPageIndex(options.getPage()).withPageSize(options.getLimit()).andTotal((int) getTotalHits(result).value);
    return searchResponseFormat.formatSearch(additionalFields, data, paging, facets);
  }

  private static TotalHits getTotalHits(SearchResponse response) {
    return ofNullable(response.getHits().getTotalHits()).orElseThrow(() -> new IllegalStateException("Could not get total hits of search results"));
  }

  private static SearchOptions createSearchOptionsFromRequest(SearchRequest request) {
    SearchOptions options = new SearchOptions();
    options.setPage(request.getPage(), request.getPageSize());

    List<String> facets = request.getFacets();

    if (facets == null || facets.isEmpty()) {
      return options;
    }

    options.addFacets(facets);
    return options;
  }

  private void completeFacets(Facets facets, SearchRequest request, IssueQuery query) {
    addMandatoryValuesToFacet(facets, PARAM_SEVERITIES, Severity.ALL);
    addMandatoryValuesToFacet(facets, PARAM_STATUSES, ISSUE_STATUSES);
    addMandatoryValuesToFacet(facets, PARAM_IMPACT_SOFTWARE_QUALITIES, enumToStringCollection(SoftwareQuality.values()));
    addMandatoryValuesToFacet(facets, PARAM_IMPACT_SEVERITIES, enumToStringCollection(org.sonar.api.issue.impact.Severity.values()));
    addMandatoryValuesToFacet(facets, PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES, enumToStringCollection(CleanCodeAttributeCategory.values()));

    addMandatoryValuesToFacet(facets, PARAM_RESOLUTIONS, concat(singletonList(""), RESOLUTIONS));
    addMandatoryValuesToFacet(facets, FACET_PROJECTS, query.projectUuids());
    addMandatoryValuesToFacet(facets, PARAM_FILES, query.files());

    List<String> assignees = Lists.newArrayList("");
    List<String> assigneesFromRequest = request.getAssigneeUuids();
    if (assigneesFromRequest != null) {
      assignees.addAll(assigneesFromRequest);
      assignees.remove(LOGIN_MYSELF);
    }
    addMandatoryValuesToFacet(facets, PARAM_ASSIGNEES, assignees);
    addMandatoryValuesToFacet(facets, FACET_ASSIGNED_TO_ME, singletonList(userSession.getUuid()));
    addMandatoryValuesToFacet(facets, PARAM_RULES, query.ruleUuids());
    addMandatoryValuesToFacet(facets, PARAM_SCOPES, ISSUE_SCOPES);
    addMandatoryValuesToFacet(facets, PARAM_LANGUAGES, request.getLanguages());
    addMandatoryValuesToFacet(facets, PARAM_TAGS, request.getTags());

    setTypesFacet(facets);

    addMandatoryValuesToFacet(facets, PARAM_PCI_DSS_32, request.getPciDss32());
    addMandatoryValuesToFacet(facets, PARAM_PCI_DSS_40, request.getPciDss40());
    addMandatoryValuesToFacet(facets, PARAM_OWASP_ASVS_40, request.getOwaspAsvs40());
    addMandatoryValuesToFacet(facets, PARAM_OWASP_TOP_10, request.getOwaspTop10());
    addMandatoryValuesToFacet(facets, PARAM_OWASP_TOP_10_2021, request.getOwaspTop10For2021());
    addMandatoryValuesToFacet(facets, PARAM_SANS_TOP_25, request.getSansTop25());
    addMandatoryValuesToFacet(facets, PARAM_CWE, request.getCwe());
    addMandatoryValuesToFacet(facets, PARAM_SONARSOURCE_SECURITY, request.getSonarsourceSecurity());
    addMandatoryValuesToFacet(facets, PARAM_CODE_VARIANTS, request.getCodeVariants());
  }

  private static Collection<String> enumToStringCollection(Enum<?>... enumValues) {
    return Arrays.stream(enumValues).map(Enum::name).toList();
  }

  private static void setTypesFacet(Facets facets) {
    Map<String, Long> typeFacet = facets.get(PARAM_TYPES);
    if (typeFacet != null) {
      typeFacet.remove(RuleType.SECURITY_HOTSPOT.name());
    }
    addMandatoryValuesToFacet(facets, PARAM_TYPES, ALL_RULE_TYPES_EXCEPT_SECURITY_HOTSPOTS.stream().map(Enum::name).toList());
  }

  private static void addMandatoryValuesToFacet(Facets facets, String facetName, @Nullable Iterable<String> mandatoryValues) {
    Map<String, Long> buckets = facets.get(facetName);
    if (buckets != null && mandatoryValues != null) {
      for (String mandatoryValue : mandatoryValues) {
        buckets.putIfAbsent(mandatoryValue, 0L);
      }
    }
  }

  private void collectLoggedInUser(SearchResponseLoader.Collector collector) {
    if (userSession.isLoggedIn()) {
      collector.addUserUuids(singletonList(userSession.getUuid()));
    }
  }

  private static void collectFacets(SearchResponseLoader.Collector collector, Facets facets) {
    collector.addProjectUuids(facets.getBucketKeys(FACET_PROJECTS));
    collector.addRuleIds(facets.getBucketKeys(PARAM_RULES));
    collector.addUserUuids(facets.getBucketKeys(PARAM_ASSIGNEES));
  }

  private static void collectRequestParams(SearchResponseLoader.Collector collector, SearchRequest request) {
    collector.addUserUuids(request.getAssigneeUuids());
  }

  private SearchRequest toSearchWsRequest(DbSession dbSession, Request request) {
    return new SearchRequest()
      .setAdditionalFields(request.paramAsStrings(PARAM_ADDITIONAL_FIELDS))
      .setAsc(request.mandatoryParamAsBoolean(PARAM_ASC))
      .setAssigned(request.paramAsBoolean(PARAM_ASSIGNED))
      .setAssigneesUuid(getLogins(dbSession, request.paramAsStrings(PARAM_ASSIGNEES)))
      .setAuthors(request.multiParam(PARAM_AUTHOR))
      .setComponentKeys(request.paramAsStrings(PARAM_COMPONENTS))
      .setCreatedAfter(request.param(PARAM_CREATED_AFTER))
      .setCreatedAt(request.param(PARAM_CREATED_AT))
      .setCreatedBefore(request.param(PARAM_CREATED_BEFORE))
      .setCreatedInLast(request.param(PARAM_CREATED_IN_LAST))
      .setDirectories(request.paramAsStrings(PARAM_DIRECTORIES))
      .setFacets(request.paramAsStrings(FACETS))
      .setFiles(request.paramAsStrings(PARAM_FILES))
      .setInNewCodePeriod(request.paramAsBoolean(PARAM_IN_NEW_CODE_PERIOD))
      .setIssues(request.paramAsStrings(PARAM_ISSUES))
      .setScopes(request.paramAsStrings(PARAM_SCOPES))
      .setLanguages(request.paramAsStrings(PARAM_LANGUAGES))
      .setOnComponentOnly(request.paramAsBoolean(PARAM_ON_COMPONENT_ONLY))
      .setBranch(request.param(PARAM_BRANCH))
      .setPullRequest(request.param(PARAM_PULL_REQUEST))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setProjectKeys(request.paramAsStrings(PARAM_PROJECTS))
      .setResolutions(request.paramAsStrings(PARAM_RESOLUTIONS))
      .setResolved(request.paramAsBoolean(PARAM_RESOLVED))
      .setRules(request.paramAsStrings(PARAM_RULES))
      .setSort(request.param(Param.SORT))
      .setSeverities(request.paramAsStrings(PARAM_SEVERITIES))
      .setImpactSeverities(request.paramAsStrings(PARAM_IMPACT_SEVERITIES))
      .setImpactSoftwareQualities(request.paramAsStrings(PARAM_IMPACT_SOFTWARE_QUALITIES))
      .setCleanCodeAttributesCategories(request.paramAsStrings(PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES))
      .setStatuses(request.paramAsStrings(PARAM_STATUSES))
      .setIssueStatuses(request.paramAsStrings(PARAM_ISSUE_STATUSES))
      .setTags(request.paramAsStrings(PARAM_TAGS))
      .setTypes(allRuleTypesExceptHotspotsIfEmpty(request.paramAsStrings(PARAM_TYPES)))
      .setPciDss32(request.paramAsStrings(PARAM_PCI_DSS_32))
      .setPciDss40(request.paramAsStrings(PARAM_PCI_DSS_40))
      .setOwaspAsvsLevel(request.paramAsInt(PARAM_OWASP_ASVS_LEVEL))
      .setOwaspAsvs40(request.paramAsStrings(PARAM_OWASP_ASVS_40))
      .setOwaspTop10(request.paramAsStrings(PARAM_OWASP_TOP_10))
      .setOwaspTop10For2021(request.paramAsStrings(PARAM_OWASP_TOP_10_2021))
      .setSansTop25(request.paramAsStrings(PARAM_SANS_TOP_25))
      .setCwe(request.paramAsStrings(PARAM_CWE))
      .setSonarsourceSecurity(request.paramAsStrings(PARAM_SONARSOURCE_SECURITY))
      .setTimeZone(request.param(PARAM_TIMEZONE))
      .setCodeVariants(request.paramAsStrings(PARAM_CODE_VARIANTS));
  }

  private void checkIfNeedIssueSync(DbSession dbSession, SearchRequest searchRequest) {
    List<String> components = searchRequest.getComponentKeys();
    if (components != null && !components.isEmpty()) {
      issueIndexSyncProgressChecker.checkIfAnyComponentsNeedIssueSync(dbSession, components);
    } else {
      // component keys not provided - asking for global
      issueIndexSyncProgressChecker.checkIfIssueSyncInProgress(dbSession);
    }
  }

  private static List<String> allRuleTypesExceptHotspotsIfEmpty(@Nullable List<String> types) {
    if (types == null || types.isEmpty()) {
      return ALL_RULE_TYPES_EXCEPT_SECURITY_HOTSPOTS.stream().map(Enum::name).toList();
    }
    return types;
  }

  private List<String> getLogins(DbSession dbSession, @Nullable List<String> assigneeLogins) {
    List<String> userLogins = new ArrayList<>();

    for (String login : ofNullable(assigneeLogins).orElse(emptyList())) {
      if (LOGIN_MYSELF.equals(login)) {
        if (userSession.getLogin() == null) {
          userLogins.add(UNKNOWN);
        } else {
          userLogins.add(userSession.getLogin());
        }
      } else {
        userLogins.add(login);
      }
    }

    List<UserDto> userDtos = dbClient.userDao().selectByLogins(dbSession, userLogins);
    List<String> assigneeUuid = userDtos.stream().map(UserDto::getUuid).toList();

    if ((assigneeLogins != null) && firstNonNull(assigneeUuid, emptyList()).isEmpty()) {
      assigneeUuid = List.of("non-existent-uuid");
    }
    return assigneeUuid;
  }
}

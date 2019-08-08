/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.issue.SearchRequest;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueQuery;
import org.sonar.server.issue.index.IssueQueryFactory;
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
import static java.util.stream.Collectors.toList;
import static org.sonar.api.issue.Issue.RESOLUTIONS;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.STATUSES;
import static org.sonar.api.issue.Issue.STATUS_IN_REVIEW;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.process.ProcessProperties.Property.SONARCLOUD_ENABLED;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonar.server.issue.index.IssueIndex.FACET_ASSIGNED_TO_ME;
import static org.sonar.server.issue.index.IssueIndex.FACET_PROJECTS;
import static org.sonar.server.issue.index.IssueQuery.SORT_BY_ASSIGNEE;
import static org.sonar.server.issue.index.IssueQueryFactory.UNKNOWN;
import static org.sonar.server.security.SecurityStandardHelper.SANS_TOP_25_INSECURE_INTERACTION;
import static org.sonar.server.security.SecurityStandardHelper.SANS_TOP_25_POROUS_DEFENSES;
import static org.sonar.server.security.SecurityStandardHelper.SANS_TOP_25_RISKY_RESOURCE;
import static org.sonar.server.security.SecurityStandardHelper.SONARSOURCE_CWE_MAPPING;
import static org.sonar.server.security.SecurityStandardHelper.SONARSOURCE_OTHER_CWES_CATEGORY;
import static org.sonar.server.security.SecurityStandardHelper.UNKNOWN_STANDARD;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.DEPRECATED_PARAM_AUTHORS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_COUNT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_EFFORT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASC;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNED;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_AUTHOR;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AFTER;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_BEFORE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_IN_LAST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CWE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FILE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_MODULE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ON_COMPONENT_ONLY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_TOP_10;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PULL_REQUEST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLUTIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLVED;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RULES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SANS_TOP_25;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SINCE_LEAK_PERIOD;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SONARSOURCE_SECURITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TYPES;

public class SearchAction implements IssuesWsAction, Startable {

  private static final String LOGIN_MYSELF = "__me__";

  static final List<String> SUPPORTED_FACETS = ImmutableList.of(
    FACET_PROJECTS,
    PARAM_MODULE_UUIDS,
    PARAM_FILE_UUIDS,
    FACET_ASSIGNED_TO_ME,
    PARAM_SEVERITIES,
    PARAM_STATUSES,
    PARAM_RESOLUTIONS,
    PARAM_RULES,
    PARAM_ASSIGNEES,
    DEPRECATED_PARAM_AUTHORS,
    PARAM_AUTHOR,
    PARAM_DIRECTORIES,
    PARAM_LANGUAGES,
    PARAM_TAGS,
    PARAM_TYPES,
    PARAM_OWASP_TOP_10,
    PARAM_SANS_TOP_25,
    PARAM_CWE,
    PARAM_CREATED_AT,
    PARAM_SONARSOURCE_SECURITY);

  private static final String INTERNAL_PARAMETER_DISCLAIMER = "This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. ";
  private static final Set<String> FACETS_REQUIRING_PROJECT_OR_ORGANIZATION = newHashSet(PARAM_MODULE_UUIDS, PARAM_FILE_UUIDS, PARAM_DIRECTORIES);
  private static final Joiner COMA_JOINER = Joiner.on(",");

  private final UserSession userSession;
  private final IssueIndex issueIndex;
  private final IssueQueryFactory issueQueryFactory;
  private final SearchResponseLoader searchResponseLoader;
  private final SearchResponseFormat searchResponseFormat;
  private final Configuration config;
  private final System2 system2;
  private final DbClient dbClient;
  private boolean isOnSonarCloud;

  public SearchAction(UserSession userSession, IssueIndex issueIndex, IssueQueryFactory issueQueryFactory, SearchResponseLoader searchResponseLoader,
    SearchResponseFormat searchResponseFormat, Configuration config, System2 system2, DbClient dbClient) {
    this.userSession = userSession;
    this.issueIndex = issueIndex;
    this.issueQueryFactory = issueQueryFactory;
    this.searchResponseLoader = searchResponseLoader;
    this.searchResponseFormat = searchResponseFormat;
    this.config = config;
    this.system2 = system2;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(ACTION_SEARCH)
      .setHandler(this)
      .setDescription(
        "Search for issues.<br>" +
          "At most one of the following parameters can be provided at the same time: %s and %s.<br>" +
          "Requires the 'Browse' permission on the specified project(s).",
        PARAM_COMPONENT_KEYS, PARAM_COMPONENT_UUIDS)
      .setSince("3.6")
      .setChangelog(
        new Change("7.8", format("added new Security Hotspots statuses : %s, %s and %s", STATUS_TO_REVIEW, STATUS_IN_REVIEW, STATUS_REVIEWED)),
        new Change("7.8", "Security hotspots are returned by default"),
        new Change("7.7", format("Value '%s' in parameter '%s' is deprecated, please use '%s' instead", DEPRECATED_PARAM_AUTHORS, FACETS, PARAM_AUTHOR)),
        new Change("7.6", format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT_KEYS)),
        new Change("7.4", "The facet 'projectUuids' is dropped in favour of the new facet 'projects'. " +
          "Note that they are not strictly identical, the latter returns the project keys."),
        new Change("7.4", format("Parameter '%s' does not accept anymore deprecated value 'debt'", FACET_MODE)),
        new Change("7.3", "response field 'fromHotspot' added to issues that are security hotspots"),
        new Change("7.3", "added facets 'sansTop25', 'owaspTop10' and 'cwe'"),
        new Change("7.2", "response field 'externalRuleEngine' added to issues that have been imported from an external rule engine"),
        new Change("7.2", format("value '%s' in parameter '%s' is deprecated, it won't have any effect", SORT_BY_ASSIGNEE, Param.SORT)),
        new Change("6.5", "parameters 'projects', 'projectUuids', 'moduleUuids', 'directories', 'fileUuids' are marked as internal"),
        new Change("6.3", "response field 'email' is renamed 'avatar'"),
        new Change("5.5", "response fields 'reporter' and 'actionPlan' are removed (drop of action plan and manual issue features)"),
        new Change("5.5", "parameters 'reporters', 'actionPlans' and 'planned' are dropped and therefore ignored (drop of action plan and manual issue features)"),
        new Change("5.5", "response field 'debt' is renamed 'effort'"))
      .setResponseExample(getClass().getResource("search-example.json"));

    action.addPagingParams(100, MAX_LIMIT);
    action.createParam(FACETS)
      .setDescription("Comma-separated list of the facets to be computed. No facet is computed by default.")
      .setPossibleValues(SUPPORTED_FACETS);
    action.createParam(FACET_MODE)
      .setDefaultValue(FACET_MODE_COUNT)
      .setDeprecatedSince("7.9")
      .setDescription("Choose the returned value for facet items, either count of issues or sum of remediation effort.")
      .setPossibleValues(FACET_MODE_COUNT, FACET_MODE_EFFORT);
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
      .setPossibleValues(Severity.ALL);
    action.createParam(PARAM_STATUSES)
      .setDescription("Comma-separated list of statuses")
      .setExampleValue(STATUS_OPEN + "," + STATUS_REOPENED)
      .setPossibleValues(STATUSES);
    action.createParam(PARAM_RESOLUTIONS)
      .setDescription("Comma-separated list of resolutions")
      .setExampleValue(RESOLUTION_FIXED + "," + RESOLUTION_REMOVED)
      .setPossibleValues(RESOLUTIONS);
    action.createParam(PARAM_RESOLVED)
      .setDescription("To match resolved or unresolved issues")
      .setBooleanPossibleValues();
    action.createParam(PARAM_RULES)
      .setDescription("Comma-separated list of coding rule keys. Format is &lt;repository&gt;:&lt;rule&gt;")
      .setExampleValue("squid:AvoidCycles");
    action.createParam(PARAM_TAGS)
      .setDescription("Comma-separated list of tags.")
      .setExampleValue("security,convention");
    action.createParam(PARAM_TYPES)
      .setDescription("Comma-separated list of types.")
      .setSince("5.5")
      .setPossibleValues((Object[]) RuleType.values())
      .setExampleValue(format("%s,%s", RuleType.CODE_SMELL, RuleType.BUG));
    action.createParam(PARAM_OWASP_TOP_10)
      .setDescription("Comma-separated list of OWASP Top 10 lowercase categories.")
      .setSince("7.3")
      .setPossibleValues("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10");
    action.createParam(PARAM_SANS_TOP_25)
      .setDescription("Comma-separated list of SANS Top 25 categories.")
      .setSince("7.3")
      .setPossibleValues(SANS_TOP_25_INSECURE_INTERACTION, SANS_TOP_25_RISKY_RESOURCE, SANS_TOP_25_POROUS_DEFENSES);
    action.createParam(PARAM_CWE)
      .setDescription("Comma-separated list of CWE identifiers. Use '" + UNKNOWN_STANDARD + "' to select issues not associated to any CWE.")
      .setExampleValue("12,125," + UNKNOWN_STANDARD);
    action.createParam(PARAM_SONARSOURCE_SECURITY)
      .setDescription("Comma-separated list of SonarSource security categories. Use '" + SONARSOURCE_OTHER_CWES_CATEGORY + "' to select issues not associated" +
        " with any category")
      .setSince("7.8")
      .setPossibleValues(ImmutableList.builder().addAll(SONARSOURCE_CWE_MAPPING.keySet()).add(SONARSOURCE_OTHER_CWES_CATEGORY).build());
    action.createParam(DEPRECATED_PARAM_AUTHORS)
      .setDeprecatedSince("7.7")
      .setDescription("This parameter is deprecated, please use '%s' instead", PARAM_AUTHOR)
      .setExampleValue("torvalds@linux-foundation.org");
    action.createParam(PARAM_AUTHOR)
      .setDescription("SCM accounts. To set several values, the parameter must be called once for each value.")
      .setExampleValue("author=torvalds@linux-foundation.org&author=linux@fondation.org");
    action.createParam(PARAM_ASSIGNEES)
      .setDescription("Comma-separated list of assignee logins. The value '__me__' can be used as a placeholder for user who performs the request")
      .setExampleValue("admin,usera,__me__");
    action.createParam(PARAM_ASSIGNED)
      .setDescription("To retrieve assigned or unassigned issues")
      .setBooleanPossibleValues();
    action.createParam(PARAM_LANGUAGES)
      .setDescription("Comma-separated list of languages. Available since 4.4")
      .setExampleValue("java,js");
    action.createParam(PARAM_CREATED_AT)
      .setDescription("Datetime to retrieve issues created during a specific analysis")
      .setExampleValue("2017-10-19T13:00:00+0200");
    action.createParam(PARAM_CREATED_AFTER)
      .setDescription("To retrieve issues created after the given date (inclusive). <br>" +
        "Either a date (server timezone) or datetime can be provided. <br>" +
        "If this parameter is set, createdSince must not be set")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");
    action.createParam(PARAM_CREATED_BEFORE)
      .setDescription("To retrieve issues created before the given date (inclusive). <br>" +
        "Either a date (server timezone) or datetime can be provided.")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");
    action.createParam(PARAM_CREATED_IN_LAST)
      .setDescription("To retrieve issues created during a time span before the current time (exclusive). " +
        "Accepted units are 'y' for year, 'm' for month, 'w' for week and 'd' for day. " +
        "If this parameter is set, createdAfter must not be set")
      .setExampleValue("1m2w (1 month 2 weeks)");
    action.createParam(PARAM_SINCE_LEAK_PERIOD)
      .setDescription("To retrieve issues created since the leak period.<br>" +
        "If this parameter is set to a truthy value, createdAfter must not be set and one component id or key must be provided.")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
  }

  private static void addComponentRelatedParams(WebService.NewAction action) {
    action.createParam(PARAM_ON_COMPONENT_ONLY)
      .setDescription("Return only issues at a component's level, not on its descendants (modules, directories, files, etc). " +
        "This parameter is only considered when componentKeys or componentUuids is set.")
      .setBooleanPossibleValues()
      .setDefaultValue("false");

    action.createParam(PARAM_COMPONENT_KEYS)
      .setDescription("Comma-separated list of component keys. Retrieve issues associated to a specific list of components (and all its descendants). " +
        "A component can be a portfolio, project, module, directory or file.")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_COMPONENT_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of components their sub-components (comma-separated list of component IDs). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "A component can be a project, module, directory or file.")
      .setDeprecatedSince("6.5")
      .setExampleValue("584a89f2-8037-4f7b-b82c-8b45d2d63fb2");

    action.createParam(PARAM_PROJECTS)
      .setDescription("To retrieve issues associated to a specific list of projects (comma-separated list of project keys). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "If this parameter is set, projectUuids must not be set.")
      .setDeprecatedKey(PARAM_PROJECT_KEYS, "6.5")
      .setInternal(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_MODULE_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of modules (comma-separated list of module IDs). " +
        INTERNAL_PARAMETER_DISCLAIMER)
      .setInternal(true)
      .setDeprecatedSince("7.6")
      .setExampleValue("7d8749e8-3070-4903-9188-bdd82933bb92");

    action.createParam(PARAM_DIRECTORIES)
      .setDescription("To retrieve issues associated to a specific list of directories (comma-separated list of directory paths). " +
        "This parameter is only meaningful when a module is selected. " +
        INTERNAL_PARAMETER_DISCLAIMER)
      .setInternal(true)
      .setSince("5.1")
      .setExampleValue("src/main/java/org/sonar/server/");

    action.createParam(PARAM_FILE_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of files (comma-separated list of file IDs). " +
        INTERNAL_PARAMETER_DISCLAIMER)
      .setInternal(true)
      .setExampleValue("bdd82933-3070-4903-9188-7d8749e8bb92");

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setInternal(true)
      .setSince("6.6");

    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001)
      .setInternal(true)
      .setSince("7.1");

    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setRequired(false)
      .setInternal(true)
      .setExampleValue("my-org")
      .setSince("6.4");
  }

  @Override
  public final void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchRequest searchRequest = toSearchWsRequest(dbSession, request);
      SearchWsResponse searchWsResponse = doHandle(dbSession, searchRequest);
      writeProtobuf(searchWsResponse, request, response);
    }
  }

  private SearchWsResponse doHandle(DbSession dbSession, SearchRequest request) {
    // prepare the Elasticsearch request
    SearchOptions options = createSearchOptionsFromRequest(dbSession, request);
    EnumSet<SearchAdditionalField> additionalFields = SearchAdditionalField.getFromRequest(request);
    IssueQuery query = issueQueryFactory.create(request);

    Set<String> facetsRequiringProjectOrOrganizationParameter = options.getFacets().stream()
      .filter(FACETS_REQUIRING_PROJECT_OR_ORGANIZATION::contains)
      .collect(toSet());
    checkArgument(facetsRequiringProjectOrOrganizationParameter.isEmpty() ||
      (!query.projectUuids().isEmpty()) || query.organizationUuid() != null, "Facet(s) '%s' require to also filter by project or organization",
      COMA_JOINER.join(facetsRequiringProjectOrOrganizationParameter));

    // execute request
    SearchResponse result = issueIndex.search(query, options);
    List<String> issueKeys = Arrays.stream(result.getHits().getHits())
      .map(SearchHit::getId)
      .collect(MoreCollectors.toList(result.getHits().getHits().length));

    // load the additional information to be returned in response
    SearchResponseLoader.Collector collector = new SearchResponseLoader.Collector(issueKeys);
    collectLoggedInUser(collector);
    collectRequestParams(collector, request);
    Facets facets = new Facets(result, system2.getDefaultTimeZone());
    if (!options.getFacets().isEmpty()) {
      // add missing values to facets. For example if assignee "john" and facet on "assignees" are requested, then
      // "john" should always be listed in the facet. If it is not present, then it is added with value zero.
      // This is a constraint from webapp UX.
      completeFacets(facets, request, query);
      collectFacets(collector, facets);
    }
    SearchResponseData preloadedData = new SearchResponseData(emptyList());
    preloadedData.addRules(ImmutableList.copyOf(query.rules()));
    SearchResponseData data = searchResponseLoader.load(preloadedData, collector, additionalFields, facets);

    // FIXME allow long in Paging
    Paging paging = forPageIndex(options.getPage()).withPageSize(options.getLimit()).andTotal((int) result.getHits().getTotalHits());
    return searchResponseFormat.formatSearch(additionalFields, data, paging, facets);
  }

  private SearchOptions createSearchOptionsFromRequest(DbSession dbSession, SearchRequest request) {
    SearchOptions options = new SearchOptions();
    options.setPage(request.getPage(), request.getPageSize());

    List<String> facets = request.getFacets();

    if (facets == null || facets.isEmpty()) {
      return options;
    }

    List<String> requestedFacets = new ArrayList<>(facets);
    if (isOnSonarCloud) {
      Optional<OrganizationDto> organizationDto = Optional.empty();
      String organizationKey = request.getOrganization();
      if (organizationKey != null) {
        organizationDto = dbClient.organizationDao().selectByKey(dbSession, organizationKey);
      }

      if (!organizationDto.isPresent() || !userSession.hasMembership(organizationDto.get())) {
        // In order to display the authors facet, the organization parameter must be set and the user
        // must be member of this organization
        requestedFacets.remove(PARAM_AUTHOR);
        requestedFacets.remove(DEPRECATED_PARAM_AUTHORS);
      }
    }

    options.addFacets(requestedFacets);
    return options;
  }

  private void completeFacets(Facets facets, SearchRequest request, IssueQuery query) {
    addMandatoryValuesToFacet(facets, PARAM_SEVERITIES, Severity.ALL);
    addMandatoryValuesToFacet(facets, PARAM_STATUSES, STATUSES);
    addMandatoryValuesToFacet(facets, PARAM_RESOLUTIONS, concat(singletonList(""), RESOLUTIONS));
    addMandatoryValuesToFacet(facets, FACET_PROJECTS, query.projectUuids());
    addMandatoryValuesToFacet(facets, PARAM_MODULE_UUIDS, query.moduleUuids());
    addMandatoryValuesToFacet(facets, PARAM_FILE_UUIDS, query.fileUuids());
    addMandatoryValuesToFacet(facets, PARAM_COMPONENT_UUIDS, request.getComponentUuids());

    List<String> assignees = Lists.newArrayList("");
    List<String> assigneesFromRequest = request.getAssigneeUuids();
    if (assigneesFromRequest != null) {
      assignees.addAll(assigneesFromRequest);
      assignees.remove(LOGIN_MYSELF);
    }
    addMandatoryValuesToFacet(facets, PARAM_ASSIGNEES, assignees);
    addMandatoryValuesToFacet(facets, FACET_ASSIGNED_TO_ME, singletonList(userSession.getUuid()));
    addMandatoryValuesToFacet(facets, PARAM_RULES, query.rules().stream().map(r -> Integer.toString(r.getId())).collect(toList()));
    addMandatoryValuesToFacet(facets, PARAM_LANGUAGES, request.getLanguages());
    addMandatoryValuesToFacet(facets, PARAM_TAGS, request.getTags());
    addMandatoryValuesToFacet(facets, PARAM_TYPES, RuleType.names());
    addMandatoryValuesToFacet(facets, PARAM_OWASP_TOP_10, request.getOwaspTop10());
    addMandatoryValuesToFacet(facets, PARAM_SANS_TOP_25, request.getSansTop25());
    addMandatoryValuesToFacet(facets, PARAM_CWE, request.getCwe());
    addMandatoryValuesToFacet(facets, PARAM_SONARSOURCE_SECURITY, request.getSonarsourceSecurity());
  }

  private static void addMandatoryValuesToFacet(Facets facets, String facetName, @Nullable Iterable<String> mandatoryValues) {
    Map<String, Long> buckets = facets.get(facetName);
    if (buckets != null && mandatoryValues != null) {
      for (String mandatoryValue : mandatoryValues) {
        if (!buckets.containsKey(mandatoryValue)) {
          buckets.put(mandatoryValue, 0L);
        }
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
    collector.addComponentUuids(facets.getBucketKeys(PARAM_MODULE_UUIDS));
    collector.addComponentUuids(facets.getBucketKeys(PARAM_FILE_UUIDS));
    collector.addComponentUuids(facets.getBucketKeys(PARAM_COMPONENT_UUIDS));
    collector.addRuleIds(facets.getBucketKeys(PARAM_RULES));
    collector.addUserUuids(facets.getBucketKeys(PARAM_ASSIGNEES));
  }

  private static void collectRequestParams(SearchResponseLoader.Collector collector, SearchRequest request) {
    collector.addComponentUuids(request.getFileUuids());
    collector.addComponentUuids(request.getModuleUuids());
    collector.addComponentUuids(request.getComponentRootUuids());
    collector.addUserUuids(request.getAssigneeUuids());
  }

  private SearchRequest toSearchWsRequest(DbSession dbSession, Request request) {
    return new SearchRequest()
      .setAdditionalFields(request.paramAsStrings(PARAM_ADDITIONAL_FIELDS))
      .setAsc(request.mandatoryParamAsBoolean(PARAM_ASC))
      .setAssigned(request.paramAsBoolean(PARAM_ASSIGNED))
      .setAssigneesUuid(getLogins(dbSession, request.paramAsStrings(PARAM_ASSIGNEES)))
      .setAuthors(request.hasParam(PARAM_AUTHOR) ? request.multiParam(PARAM_AUTHOR) : request.paramAsStrings(DEPRECATED_PARAM_AUTHORS))
      .setComponentKeys(request.paramAsStrings(PARAM_COMPONENT_KEYS))
      .setComponentUuids(request.paramAsStrings(PARAM_COMPONENT_UUIDS))
      .setCreatedAfter(request.param(PARAM_CREATED_AFTER))
      .setCreatedAt(request.param(PARAM_CREATED_AT))
      .setCreatedBefore(request.param(PARAM_CREATED_BEFORE))
      .setCreatedInLast(request.param(PARAM_CREATED_IN_LAST))
      .setDirectories(request.paramAsStrings(PARAM_DIRECTORIES))
      .setFacetMode(request.mandatoryParam(FACET_MODE))
      .setFacets(request.paramAsStrings(FACETS))
      .setFileUuids(request.paramAsStrings(PARAM_FILE_UUIDS))
      .setIssues(request.paramAsStrings(PARAM_ISSUES))
      .setLanguages(request.paramAsStrings(PARAM_LANGUAGES))
      .setModuleUuids(request.paramAsStrings(PARAM_MODULE_UUIDS))
      .setOnComponentOnly(request.paramAsBoolean(PARAM_ON_COMPONENT_ONLY))
      .setBranch(request.param(PARAM_BRANCH))
      .setPullRequest(request.param(PARAM_PULL_REQUEST))
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setProjectKeys(request.paramAsStrings(PARAM_PROJECTS))
      .setProjects(request.paramAsStrings(PARAM_PROJECTS))
      .setResolutions(request.paramAsStrings(PARAM_RESOLUTIONS))
      .setResolved(request.paramAsBoolean(PARAM_RESOLVED))
      .setRules(request.paramAsStrings(PARAM_RULES))
      .setSinceLeakPeriod(request.mandatoryParamAsBoolean(PARAM_SINCE_LEAK_PERIOD))
      .setSort(request.param(Param.SORT))
      .setSeverities(request.paramAsStrings(PARAM_SEVERITIES))
      .setStatuses(request.paramAsStrings(PARAM_STATUSES))
      .setTags(request.paramAsStrings(PARAM_TAGS))
      .setTypes(request.paramAsStrings(PARAM_TYPES))
      .setOwaspTop10(request.paramAsStrings(PARAM_OWASP_TOP_10))
      .setSansTop25(request.paramAsStrings(PARAM_SANS_TOP_25))
      .setCwe(request.paramAsStrings(PARAM_CWE))
      .setSonarsourceSecurity(request.paramAsStrings(PARAM_SONARSOURCE_SECURITY));
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
    List<String> assigneeUuid = userDtos.stream().map(UserDto::getUuid).collect(toList());

    if ((assigneeLogins != null) && firstNonNull(assigneeUuid, emptyList()).isEmpty()) {
      assigneeUuid = ImmutableList.of("non-existent-uuid");
    }
    return assigneeUuid;
  }

  @Override
  public void start() {
    this.isOnSonarCloud = config.getBoolean(SONARCLOUD_ENABLED.getKey()).orElse(false);
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}

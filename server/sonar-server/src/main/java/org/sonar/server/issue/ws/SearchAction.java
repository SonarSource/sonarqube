/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueQueryFactory;
import org.sonar.server.issue.SearchRequest;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Issues.SearchWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.DEPRECATED_FACET_MODE_DEBT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.DEPRECATED_PARAM_ACTION_PLANS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_ASSIGNED_TO_ME;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_COUNT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_EFFORT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASC;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNED;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_AUTHORS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_ROOTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_ROOT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AFTER;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_BEFORE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_IN_LAST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FILE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_MODULE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ON_COMPONENT_ONLY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PLANNED;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_REPORTERS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLUTIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLVED;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RULES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SINCE_LEAK_PERIOD;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TYPES;

public class SearchAction implements IssuesWsAction {

  private static final String INTERNAL_PARAMETER_DISCLAIMER = "This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. ";
  private static final Set<String> IGNORED_FACETS = newHashSet(PARAM_PLANNED, DEPRECATED_PARAM_ACTION_PLANS, PARAM_REPORTERS);
  private static final Set<String> FACETS_REQUIRING_PROJECT_OR_ORGANIZATION = newHashSet(PARAM_FILE_UUIDS, PARAM_DIRECTORIES, PARAM_MODULE_UUIDS);
  private static final Joiner COMA_JOINER = Joiner.on(",");
  private static final Logger LOGGER = Loggers.get(SearchAction.class);

  private final UserSession userSession;
  private final IssueIndex issueIndex;
  private final IssueQueryFactory issueQueryFactory;
  private final SearchResponseLoader searchResponseLoader;
  private final SearchResponseFormat searchResponseFormat;
  private final System2 system2;
  private final DbClient dbClient;

  public SearchAction(UserSession userSession, IssueIndex issueIndex, IssueQueryFactory issueQueryFactory,
    SearchResponseLoader searchResponseLoader, SearchResponseFormat searchResponseFormat, System2 system2,
    DbClient dbClient) {
    this.userSession = userSession;
    this.issueIndex = issueIndex;
    this.issueQueryFactory = issueQueryFactory;
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
      .setDescription(
        "Search for issues.<br>" +
          "At most one of the following parameters can be provided at the same time: %s, %s, %s, %s, %s.<br>" +
          "Requires the 'Browse' permission on the specified project(s).",
        PARAM_COMPONENT_KEYS, PARAM_COMPONENT_UUIDS, PARAM_COMPONENTS, PARAM_COMPONENT_ROOT_UUIDS, PARAM_COMPONENT_ROOTS)
      .setSince("3.6")
      .setChangelog(
        new Change("6.5", "parameters 'projects', 'projectUuids', 'moduleUuids', 'directories', 'fileUuids' are marked as internal"),
        new Change("6.3", "response field 'email' is renamed 'avatar'"),
        new Change("5.5", "response fields 'reporter' and 'actionPlan' are removed (drop of action plan and manual issue features)"),
        new Change("5.5", "parameters 'reporters', 'actionPlans' and 'planned' are dropped and therefore ignored (drop of action plan and manual issue features)"),
        new Change("5.5", "response field 'debt' is renamed 'effort'"))
      .setResponseExample(getClass().getResource("search-example.json"));

    action.addPagingParams(100, MAX_LIMIT);
    action.createParam(Param.FACETS)
      .setDescription("Comma-separated list of the facets to be computed. No facet is computed by default.<br/>" +
        "Since 5.5, facet 'actionPlans' is deprecated.<br/>" +
        "Since 5.5, facet 'reporters' is deprecated.")
      .setPossibleValues(IssueIndex.SUPPORTED_FACETS);
    action.createParam(FACET_MODE)
      .setDefaultValue(FACET_MODE_COUNT)
      .setDescription("Choose the returned value for facet items, either count of issues or sum of debt.<br/>" +
        "Since 5.5, 'debt' mode is deprecated and replaced by 'effort'")
      .setPossibleValues(FACET_MODE_COUNT, FACET_MODE_EFFORT, DEPRECATED_FACET_MODE_DEBT);
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
      .setExampleValue(Issue.STATUS_OPEN + "," + Issue.STATUS_REOPENED)
      .setPossibleValues(Issue.STATUSES);
    action.createParam(PARAM_RESOLUTIONS)
      .setDescription("Comma-separated list of resolutions")
      .setExampleValue(Issue.RESOLUTION_FIXED + "," + Issue.RESOLUTION_REMOVED)
      .setPossibleValues(Issue.RESOLUTIONS);
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
    action.createParam(PARAM_AUTHORS)
      .setDescription("Comma-separated list of SCM accounts")
      .setExampleValue("torvalds@linux-foundation.org");
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
        "This parameter is only considered when componentKeys or componentUuids is set. " +
        "Using the deprecated componentRoots or componentRootUuids parameters will set this parameter to false. " +
        "Using the deprecated components parameter will set this parameter to true.")
      .setBooleanPossibleValues()
      .setDefaultValue("false");

    action.createParam(PARAM_COMPONENT_KEYS)
      .setDescription("Comma-separated list of component keys. Retrieve issues associated to a specific list of components (and all its descendants). " +
        "A component can be a portfolio, project, module, directory or file.")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_COMPONENTS)
      .setDeprecatedSince("5.1")
      .setDescription("If used, will have the same meaning as componentKeys AND onComponentOnly=true.");

    action.createParam(PARAM_COMPONENT_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of components their sub-components (comma-separated list of component IDs). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "A component can be a project, module, directory or file.")
      .setDeprecatedSince("6.5")
      .setExampleValue("584a89f2-8037-4f7b-b82c-8b45d2d63fb2");

    action.createParam(PARAM_COMPONENT_ROOTS)
      .setDeprecatedSince("5.1")
      .setDescription("If used, will have the same meaning as componentKeys AND onComponentOnly=false.");

    action.createParam(PARAM_COMPONENT_ROOT_UUIDS)
      .setDeprecatedSince("5.1")
      .setDescription("If used, will have the same meaning as componentUuids AND onComponentOnly=false.");

    action.createParam(PARAM_PROJECTS)
      .setDescription("To retrieve issues associated to a specific list of projects (comma-separated list of project keys). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "If this parameter is set, projectUuids must not be set.")
      .setDeprecatedKey(PARAM_PROJECT_KEYS, "6.5")
      .setInternal(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_PROJECT_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of projects (comma-separated list of project IDs). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "Portfolios are not supported. If this parameter is set, '%s' must not be set.", PARAM_PROJECTS)
      .setInternal(true)
      .setExampleValue("7d8749e8-3070-4903-9188-bdd82933bb92");

    action.createParam(PARAM_MODULE_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of modules (comma-separated list of module IDs). " +
        INTERNAL_PARAMETER_DISCLAIMER)
      .setInternal(true)
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

    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setRequired(false)
      .setInternal(true)
      .setExampleValue("my-org")
      .setSince("6.4");
  }

  @Override
  public final void handle(Request request, Response response) {
    SearchWsResponse searchWsResponse = doHandle(toSearchWsRequest(request), request);
    writeProtobuf(searchWsResponse, request, response);
  }

  private SearchWsResponse doHandle(SearchRequest request, Request wsRequest) {
    // prepare the Elasticsearch request
    SearchOptions options = createSearchOptionsFromRequest(request);
    EnumSet<SearchAdditionalField> additionalFields = SearchAdditionalField.getFromRequest(request);
    IssueQuery query = issueQueryFactory.create(request);

    // execute request
    SearchResponse result = issueIndex.search(query, options);
    List<String> issueKeys = Arrays.stream(result.getHits().getHits())
      .map(SearchHit::getId)
      .collect(MoreCollectors.toList(result.getHits().getHits().length));

    // load the additional information to be returned in response
    SearchResponseLoader.Collector collector = new SearchResponseLoader.Collector(additionalFields, issueKeys);
    collectLoggedInUser(collector);
    collectRequestParams(collector, request);
    Facets facets = null;
    if (!options.getFacets().isEmpty()) {
      facets = new Facets(result, system2.getDefaultTimeZone());
      // add missing values to facets. For example if assignee "john" and facet on "assignees" are requested, then
      // "john" should always be listed in the facet. If it is not present, then it is added with value zero.
      // This is a constraint from webapp UX.
      completeFacets(facets, request, wsRequest);
      collectFacets(collector, facets);

      Set<String> facetsRequiringProjectOrOrganizationParameter = facets.getNames().stream()
        .filter(FACETS_REQUIRING_PROJECT_OR_ORGANIZATION::contains)
        .collect(toSet());
      checkArgument(facetsRequiringProjectOrOrganizationParameter.isEmpty() ||
        (!query.projectUuids().isEmpty()) || query.organizationUuid() != null, "Facet(s) '%s' require to also filter by project or organization",
        COMA_JOINER.join(facetsRequiringProjectOrOrganizationParameter));
    }
    SearchResponseData preloadedData = new SearchResponseData(emptyList());
    preloadedData.setRules(ImmutableList.copyOf(query.rules()));
    SearchResponseData data = searchResponseLoader.load(preloadedData, collector, facets);

    // format response

    // Filter and reorder facets according to the requested ordered names.
    // Must be done after loading of data as the "hidden" facet "debt"
    // can be used to get total debt.
    facets = reorderFacets(facets, options.getFacets());
    replaceRuleIdsByRuleKeys(facets, data.getRules() == null ? emptyList() : data.getRules());

    // FIXME allow long in Paging
    Paging paging = forPageIndex(options.getPage()).withPageSize(options.getLimit()).andTotal((int) result.getHits().getTotalHits());

    return searchResponseFormat.formatSearch(additionalFields, data, paging, facets);
  }

  private void replaceRuleIdsByRuleKeys(@Nullable Facets facets, List<RuleDefinitionDto> alreadyLoadedRules) {
    if (facets == null) {
      return;
    }
    LinkedHashMap<String, Long> rulesFacet = facets.get(PARAM_RULES);
    if (rulesFacet == null) {
      return;
    }

    // The facet for PARAM_RULES contains the id of the rule as the key
    // We need to update the key to be a RuleKey
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<Integer> ruleIdsToLoad = new HashSet<>();
      rulesFacet.keySet().forEach(s -> {
        try {
          ruleIdsToLoad.add(Integer.parseInt(s));
        } catch (NumberFormatException e) {
          // ignore, this is already a key
        }
      });
      ruleIdsToLoad.removeAll(
        alreadyLoadedRules
          .stream()
          .map(RuleDefinitionDto::getId)
          .collect(Collectors.toList()));

      List<RuleDefinitionDto> ruleDefinitions = Stream.concat(
        alreadyLoadedRules.stream(),
        dbClient.ruleDao().selectDefinitionByIds(dbSession, ruleIdsToLoad).stream())
        .collect(MoreCollectors.toList());
      Map<Integer, RuleKey> ruleKeyById = ruleDefinitions.stream()
        .collect(Collectors.toMap(RuleDefinitionDto::getId, RuleDefinitionDto::getKey));
      Map<String, Integer> idByRuleKeyAsString = ruleDefinitions.stream()
        .collect(Collectors.toMap(s -> s.getKey().toString(), RuleDefinitionDto::getId));

      LinkedHashMap<String, Long> newRulesFacet = new LinkedHashMap<>();
      rulesFacet.forEach((k, v) -> {
        try {
          int ruleId = Integer.parseInt(k);
          RuleKey ruleKey = ruleKeyById.get(ruleId);
          if (ruleKey != null) {
            newRulesFacet.put(ruleKey.toString(), v);
          } else {
            // RuleKey not found ES/DB incorrect?
            LOGGER.error("Rule with id {} is not available in database", k);
          }
        } catch (NumberFormatException e) {
          // RuleKey are added into the facet from the HTTP request, there may be a result for this rule from the
          // ES search (with the ruleId as a key). If so, do not add this entry again (anyway, value for ruleKey is
          // always 0 since it is added SearchAction#completeFacets).
          String ruleId = String.valueOf(idByRuleKeyAsString.get(k));
          if (!rulesFacet.containsKey(ruleId)) {
            newRulesFacet.put(k, v);
          }
        }
      });
      rulesFacet.clear();
      rulesFacet.putAll(newRulesFacet);
    }
  }

  private static SearchOptions createSearchOptionsFromRequest(SearchRequest request) {
    SearchOptions options = new SearchOptions();
    options.setPage(request.getPage(), request.getPageSize());
    options.addFacets(request.getFacets());

    return options;
  }

  private Facets reorderFacets(@Nullable Facets facets, Collection<String> orderedNames) {
    if (facets == null) {
      return null;
    }
    LinkedHashMap<String, LinkedHashMap<String, Long>> orderedFacets = new LinkedHashMap<>();
    for (String facetName : orderedNames) {
      LinkedHashMap<String, Long> facet = facets.get(facetName);
      if (facet != null) {
        orderedFacets.put(facetName, facet);
      }
    }
    return new Facets(orderedFacets, system2.getDefaultTimeZone());
  }

  private void completeFacets(Facets facets, SearchRequest request, Request wsRequest) {
    addMandatoryValuesToFacet(facets, PARAM_SEVERITIES, Severity.ALL);
    addMandatoryValuesToFacet(facets, PARAM_STATUSES, Issue.STATUSES);
    addMandatoryValuesToFacet(facets, PARAM_RESOLUTIONS, concat(singletonList(""), Issue.RESOLUTIONS));
    addMandatoryValuesToFacet(facets, PARAM_PROJECT_UUIDS, request.getProjectUuids());

    List<String> assignees = Lists.newArrayList("");
    List<String> assigneesFromRequest = request.getAssignees();
    if (assigneesFromRequest != null) {
      assignees.addAll(assigneesFromRequest);
      assignees.remove(IssueQueryFactory.LOGIN_MYSELF);
    }
    addMandatoryValuesToFacet(facets, PARAM_ASSIGNEES, assignees);
    addMandatoryValuesToFacet(facets, FACET_ASSIGNED_TO_ME, singletonList(userSession.getLogin()));
    addMandatoryValuesToFacet(facets, PARAM_RULES, request.getRules());
    addMandatoryValuesToFacet(facets, PARAM_LANGUAGES, request.getLanguages());
    addMandatoryValuesToFacet(facets, PARAM_TAGS, request.getTags());
    addMandatoryValuesToFacet(facets, PARAM_TYPES, RuleType.names());
    addMandatoryValuesToFacet(facets, PARAM_COMPONENT_UUIDS, request.getComponentUuids());

    List<String> requestedFacets = request.getFacets();
    if (requestedFacets == null) {
      return;
    }
    requestedFacets.stream()
      .filter(facetName -> !FACET_ASSIGNED_TO_ME.equals(facetName))
      .filter(facetName -> !IGNORED_FACETS.contains(facetName))
      .forEach(facetName -> {
        LinkedHashMap<String, Long> buckets = facets.get(facetName);
        List<String> requestParams = wsRequest.paramAsStrings(facetName);
        if (buckets == null || requestParams == null) {
          return;
        }
        requestParams.stream()
          .filter(param -> !buckets.containsKey(param) && !IssueQueryFactory.LOGIN_MYSELF.equals(param))
          // Prevent appearance of a glitch value due to dedicated parameter for this facet
          .forEach(param -> buckets.put(param, 0L));
      });
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
      collector.add(SearchAdditionalField.USERS, userSession.getLogin());
    }
  }

  private static void collectFacets(SearchResponseLoader.Collector collector, Facets facets) {
    Set<String> facetRules = facets.getBucketKeys(PARAM_RULES);
    if (facetRules != null) {
      collector.addAll(SearchAdditionalField.RULE_IDS_AND_KEYS, facetRules);
    }
    collector.addProjectUuids(facets.getBucketKeys(PARAM_PROJECT_UUIDS));
    collector.addComponentUuids(facets.getBucketKeys(PARAM_COMPONENT_UUIDS));
    collector.addComponentUuids(facets.getBucketKeys(PARAM_FILE_UUIDS));
    collector.addComponentUuids(facets.getBucketKeys(PARAM_MODULE_UUIDS));
    collector.addAll(SearchAdditionalField.USERS, facets.getBucketKeys(PARAM_ASSIGNEES));
  }

  private static void collectRequestParams(SearchResponseLoader.Collector collector, SearchRequest request) {
    collector.addProjectUuids(request.getProjectUuids());
    collector.addComponentUuids(request.getFileUuids());
    collector.addComponentUuids(request.getModuleUuids());
    collector.addComponentUuids(request.getComponentRootUuids());
    collector.addAll(SearchAdditionalField.USERS, request.getAssignees());
  }

  private static SearchRequest toSearchWsRequest(Request request) {
    return new SearchRequest()
      .setAdditionalFields(request.paramAsStrings(PARAM_ADDITIONAL_FIELDS))
      .setAsc(request.paramAsBoolean(PARAM_ASC))
      .setAssigned(request.paramAsBoolean(PARAM_ASSIGNED))
      .setAssignees(request.paramAsStrings(PARAM_ASSIGNEES))
      .setAuthors(request.paramAsStrings(PARAM_AUTHORS))
      .setComponentKeys(request.paramAsStrings(PARAM_COMPONENT_KEYS))
      .setComponentRootUuids(request.paramAsStrings(PARAM_COMPONENT_ROOT_UUIDS))
      .setComponentRoots(request.paramAsStrings(PARAM_COMPONENT_ROOTS))
      .setComponentUuids(request.paramAsStrings(PARAM_COMPONENT_UUIDS))
      .setComponents(request.paramAsStrings(PARAM_COMPONENTS))
      .setCreatedAfter(request.param(PARAM_CREATED_AFTER))
      .setCreatedAt(request.param(PARAM_CREATED_AT))
      .setCreatedBefore(request.param(PARAM_CREATED_BEFORE))
      .setCreatedInLast(request.param(PARAM_CREATED_IN_LAST))
      .setDirectories(request.paramAsStrings(PARAM_DIRECTORIES))
      .setFacetMode(request.mandatoryParam(FACET_MODE))
      .setFacets(request.paramAsStrings(Param.FACETS))
      .setFileUuids(request.paramAsStrings(PARAM_FILE_UUIDS))
      .setIssues(request.paramAsStrings(PARAM_ISSUES))
      .setLanguages(request.paramAsStrings(PARAM_LANGUAGES))
      .setModuleUuids(request.paramAsStrings(PARAM_MODULE_UUIDS))
      .setOnComponentOnly(request.paramAsBoolean(PARAM_ON_COMPONENT_ONLY))
      .setBranch(request.param(PARAM_BRANCH))
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setProjectKeys(request.paramAsStrings(PARAM_PROJECTS))
      .setProjectUuids(request.paramAsStrings(PARAM_PROJECT_UUIDS))
      .setProjects(request.paramAsStrings(PARAM_PROJECTS))
      .setResolutions(request.paramAsStrings(PARAM_RESOLUTIONS))
      .setResolved(request.paramAsBoolean(PARAM_RESOLVED))
      .setRules(request.paramAsStrings(PARAM_RULES))
      .setSinceLeakPeriod(request.mandatoryParamAsBoolean(PARAM_SINCE_LEAK_PERIOD))
      .setSort(request.param(Param.SORT))
      .setSeverities(request.paramAsStrings(PARAM_SEVERITIES))
      .setStatuses(request.paramAsStrings(PARAM_STATUSES))
      .setTags(request.paramAsStrings(PARAM_TAGS))
      .setTypes(request.paramAsStrings(PARAM_TYPES));
  }
}

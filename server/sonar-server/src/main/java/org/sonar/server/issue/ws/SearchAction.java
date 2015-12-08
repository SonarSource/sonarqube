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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.core.rule.RuleKeyFunctions;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueQueryService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.client.issue.IssueFilterParameters;
import org.sonarqube.ws.client.issue.SearchWsRequest;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.concat;
import static java.util.Collections.singletonList;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ACTION_PLANS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ASC;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ASSIGNED;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.AUTHORS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.COMPONENTS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.COMPONENT_ROOTS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.COMPONENT_ROOT_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.COMPONENT_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.CREATED_AFTER;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.CREATED_AT;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.CREATED_BEFORE;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.CREATED_IN_LAST;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.FACET_MODE;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.FILE_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ISSUES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.LANGUAGES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.MODULE_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ON_COMPONENT_ONLY;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.PLANNED;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.PROJECTS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.PROJECT_KEYS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.PROJECT_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.REPORTERS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.RESOLUTIONS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.RESOLVED;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.RULES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.SEVERITIES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.STATUSES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.TAGS;

public class SearchAction implements IssuesWsAction {

  private static final String INTERNAL_PARAMETER_DISCLAIMER = "This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. ";
  public static final String SEARCH_ACTION = "search";

  private final UserSession userSession;
  private final IssueService service;
  private final IssueQueryService issueQueryService;
  private final SearchResponseLoader searchResponseLoader;
  private final SearchResponseFormat searchResponseFormat;

  public SearchAction(UserSession userSession, IssueService service, IssueQueryService issueQueryService,
    SearchResponseLoader searchResponseLoader, SearchResponseFormat searchResponseFormat) {
    this.userSession = userSession;
    this.service = service;
    this.issueQueryService = issueQueryService;
    this.searchResponseLoader = searchResponseLoader;
    this.searchResponseFormat = searchResponseFormat;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(SEARCH_ACTION)
      .setHandler(this)
      .setDescription(
        "Search for issues. Requires Browse permission on project(s)")
      .setSince("3.6")
      .setResponseExample(Resources.getResource(this.getClass(), "example-search.json"));

    action.addPagingParams(100, MAX_LIMIT);
    action.createParam(Param.FACETS)
      .setDescription("Comma-separated list of the facets to be computed. No facet is computed by default.")
      .setPossibleValues(IssueIndex.SUPPORTED_FACETS);
    action.createParam(IssueFilterParameters.FACET_MODE)
      .setDefaultValue(IssueFilterParameters.FACET_MODE_COUNT)
      .setDescription("Choose the returned value for facet items, either count of issues or sum of debt.")
      .setPossibleValues(IssueFilterParameters.FACET_MODE_COUNT, IssueFilterParameters.FACET_MODE_DEBT);
    action.addSortParams(IssueQuery.SORTS, null, true);
    action.createParam(IssueFilterParameters.ADDITIONAL_FIELDS)
      .setSince("5.2")
      .setDescription("Comma-separated list of the optional fields to be returned in response.")
      .setPossibleValues(SearchAdditionalField.possibleValues());
    addComponentRelatedParams(action);
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
    action.createParam(IssueFilterParameters.RULES)
      .setDescription("Comma-separated list of coding rule keys. Format is <repository>:<rule>")
      .setExampleValue("squid:AvoidCycles");
    action.createParam(IssueFilterParameters.TAGS)
      .setDescription("Comma-separated list of tags.")
      .setExampleValue("security,convention");
    action.createParam(ACTION_PLANS)
      .setDescription("Comma-separated list of action plan keys (not names)")
      .setExampleValue("3f19de90-1521-4482-a737-a311758ff513");
    action.createParam(IssueFilterParameters.PLANNED)
      .setDescription("To retrieve issues associated to an action plan or not")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.REPORTERS)
      .setDescription("Comma-separated list of reporter logins")
      .setExampleValue("admin");
    action.createParam(IssueFilterParameters.AUTHORS)
      .setDescription("Comma-separated list of SCM accounts")
      .setExampleValue("torvalds@linux-foundation.org");
    action.createParam(IssueFilterParameters.ASSIGNEES)
      .setDescription("Comma-separated list of assignee logins. The value '__me__' can be used as a placeholder for user who performs the request")
      .setExampleValue("admin,usera,__me__");
    action.createParam(IssueFilterParameters.ASSIGNED)
      .setDescription("To retrieve assigned or unassigned issues")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.LANGUAGES)
      .setDescription("Comma-separated list of languages. Available since 4.4")
      .setExampleValue("java,js");
    action.createParam(IssueFilterParameters.CREATED_AT)
      .setDescription("To retrieve issues created in a specific analysis, identified by an ISO-formatted datetime stamp.")
      .setExampleValue("2013-05-01T13:00:00+0100");
    action.createParam(IssueFilterParameters.CREATED_AFTER)
      .setDescription("To retrieve issues created after the given date (exclusive). Format: date or datetime ISO formats. If this parameter is set, createdSince must not be set")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam(IssueFilterParameters.CREATED_BEFORE)
      .setDescription("To retrieve issues created before the given date (exclusive). Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam(IssueFilterParameters.CREATED_IN_LAST)
      .setDescription("To retrieve issues created during a time span before the current time (exclusive). " +
        "Accepted units are 'y' for year, 'm' for month, 'w' for week and 'd' for day. " +
        "If this parameter is set, createdAfter must not be set")
      .setExampleValue("1m2w (1 month 2 weeks)");
  }

  private static void addComponentRelatedParams(WebService.NewAction action) {
    action.createParam(IssueFilterParameters.ON_COMPONENT_ONLY)
      .setDescription("Return only issues at a component's level, not on its descendants (modules, directories, files, etc). " +
        "This parameter is only considered when componentKeys or componentUuids is set. " +
        "Using the deprecated componentRoots or componentRootUuids parameters will set this parameter to false. " +
        "Using the deprecated components parameter will set this parameter to true.")
      .setBooleanPossibleValues()
      .setDefaultValue("false");

    action.createParam(IssueFilterParameters.COMPONENT_KEYS)
      .setDescription("To retrieve issues associated to a specific list of components sub-components (comma-separated list of component keys). " +
        "A component can be a view, developer, project, module, directory or file. " +
        "If this parameter is set, componentUuids must not be set.")
      .setExampleValue("org.apache.struts:struts:org.apache.struts.Action");
    action.createParam(IssueFilterParameters.COMPONENTS)
      .setDeprecatedSince("5.1")
      .setDescription("If used, will have the same meaning as componentKeys AND onComponentOnly=true.");
    action.createParam(IssueFilterParameters.COMPONENT_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of components their sub-components (comma-separated list of component UUIDs). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "A component can be a project, module, directory or file. " +
        "If this parameter is set, componentKeys must not be set.")
      .setExampleValue("584a89f2-8037-4f7b-b82c-8b45d2d63fb2");

    action.createParam(IssueFilterParameters.COMPONENT_ROOTS)
      .setDeprecatedSince("5.1")
      .setDescription("If used, will have the same meaning as componentKeys AND onComponentOnly=false.");
    action.createParam(IssueFilterParameters.COMPONENT_ROOT_UUIDS)
      .setDeprecatedSince("5.1")
      .setDescription("If used, will have the same meaning as componentUuids AND onComponentOnly=false.");

    action.createParam(IssueFilterParameters.PROJECTS)
      .setDeprecatedSince("5.1")
      .setDescription("See projectKeys");
    action.createParam(IssueFilterParameters.PROJECT_KEYS)
      .setDescription("To retrieve issues associated to a specific list of projects (comma-separated list of project keys). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "If this parameter is set, projectUuids must not be set.")
      .setDeprecatedKey(IssueFilterParameters.PROJECTS)
      .setExampleValue("org.apache.struts:struts:org.apache.struts.Action");
    action.createParam(IssueFilterParameters.PROJECT_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of projects (comma-separated list of project UUIDs). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "Views are not supported. If this parameter is set, projectKeys must not be set.")
      .setExampleValue("7d8749e8-3070-4903-9188-bdd82933bb92");

    action.createParam(IssueFilterParameters.MODULE_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of modules (comma-separated list of module UUIDs). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "Views are not supported. If this parameter is set, moduleKeys must not be set.")
      .setExampleValue("7d8749e8-3070-4903-9188-bdd82933bb92");

    action.createParam(IssueFilterParameters.DIRECTORIES)
      .setDescription("Since 5.1. To retrieve issues associated to a specific list of directories (comma-separated list of directory paths). " +
        "This parameter is only meaningful when a module is selected. " +
        INTERNAL_PARAMETER_DISCLAIMER)
      .setExampleValue("src/main/java/org/sonar/server/");

    action.createParam(IssueFilterParameters.FILE_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of files (comma-separated list of file UUIDs). " +
        INTERNAL_PARAMETER_DISCLAIMER)
      .setExampleValue("bdd82933-3070-4903-9188-7d8749e8bb92");
  }

  @Override
  public final void handle(Request request, Response response) throws Exception {
    SearchWsResponse searchWsResponse = doHandle(toSearchWsRequest(request), request);
    writeProtobuf(searchWsResponse, request, response);
  }

  private SearchWsResponse doHandle(SearchWsRequest request, Request wsRequest) {
    // prepare the Elasticsearch request
    SearchOptions options = new SearchOptions();
    options.setPage(request.getPage(), request.getPageSize());
    options.addFacets(request.getFacets());
    EnumSet<SearchAdditionalField> additionalFields = SearchAdditionalField.getFromRequest(request);
    IssueQuery query = issueQueryService.createFromRequest(request);

    // execute request
    SearchResult<IssueDoc> result = service.search(query, options);
    List<String> issueKeys = from(result.getDocs()).transform(IssueDocToKey.INSTANCE).toList();

    // load the additional information to be returned in response
    SearchResponseLoader.Collector collector = new SearchResponseLoader.Collector(additionalFields, issueKeys);
    collectLoggedInUser(collector);
    collectRequestParams(collector, request);
    Facets facets = null;
    if (!options.getFacets().isEmpty()) {
      facets = result.getFacets();
      // add missing values to facets. For example if assignee "john" and facet on "assignees" are requested, then
      // "john" should always be listed in the facet. If it is not present, then it is added with value zero.
      // This is a constraint from webapp UX.
      completeFacets(facets, request, wsRequest);
      collectFacets(collector, facets);
    }
    SearchResponseData data = searchResponseLoader.load(collector, facets);

    // format response

    // Filter and reorder facets according to the requested ordered names.
    // Must be done after loading of data as the "hidden" facet "debt"
    // can be used to get total debt.
    facets = reorderFacets(facets, options.getFacets());

    // FIXME allow long in Paging
    Paging paging = forPageIndex(options.getPage()).withPageSize(options.getLimit()).andTotal((int) result.getTotal());

    return searchResponseFormat.formatSearch(additionalFields, data, paging, facets);
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
    return new Facets(orderedFacets);
  }

  private void completeFacets(Facets facets, SearchWsRequest request, Request wsRequest) {
    addMandatoryValuesToFacet(facets, IssueFilterParameters.SEVERITIES, Severity.ALL);
    addMandatoryValuesToFacet(facets, IssueFilterParameters.STATUSES, Issue.STATUSES);
    addMandatoryValuesToFacet(facets, IssueFilterParameters.RESOLUTIONS, concat(singletonList(""), Issue.RESOLUTIONS));
    addMandatoryValuesToFacet(facets, IssueFilterParameters.PROJECT_UUIDS, request.getProjectUuids());

    List<String> assignees = Lists.newArrayList("");
    List<String> assigneesFromRequest = request.getAssignees();
    if (assigneesFromRequest != null) {
      assignees.addAll(assigneesFromRequest);
      assignees.remove(IssueQueryService.LOGIN_MYSELF);
    }
    addMandatoryValuesToFacet(facets, IssueFilterParameters.ASSIGNEES, assignees);
    addMandatoryValuesToFacet(facets, IssueFilterParameters.FACET_ASSIGNED_TO_ME, singletonList(userSession.getLogin()));
    addMandatoryValuesToFacet(facets, IssueFilterParameters.REPORTERS, request.getReporters());
    addMandatoryValuesToFacet(facets, IssueFilterParameters.RULES, request.getRules());
    addMandatoryValuesToFacet(facets, IssueFilterParameters.LANGUAGES, request.getLanguages());
    addMandatoryValuesToFacet(facets, IssueFilterParameters.TAGS, request.getTags());
    List<String> actionPlans = Lists.newArrayList("");
    List<String> actionPlansFromRequest = request.getActionPlans();
    if (actionPlansFromRequest != null) {
      actionPlans.addAll(actionPlansFromRequest);
    }
    addMandatoryValuesToFacet(facets, ACTION_PLANS, actionPlans);
    addMandatoryValuesToFacet(facets, IssueFilterParameters.COMPONENT_UUIDS, request.getComponentUuids());

    for (String facetName : request.getFacets()) {
      LinkedHashMap<String, Long> buckets = facets.get(facetName);
      if (!IssueFilterParameters.FACET_ASSIGNED_TO_ME.equals(facetName)) {
        if (buckets != null) {
          List<String> requestParams = wsRequest.paramAsStrings(facetName);
          if (requestParams != null) {
            for (String param : requestParams) {
              if (!buckets.containsKey(param) && !IssueQueryService.LOGIN_MYSELF.equals(param)) {
                // Prevent appearance of a glitch value due to dedicated parameter for this facet
                buckets.put(param, 0L);
              }
            }
          }
        }
      }
    }
  }

  private void addMandatoryValuesToFacet(Facets facets, String facetName, @Nullable Iterable<String> mandatoryValues) {
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

  private void collectFacets(SearchResponseLoader.Collector collector, Facets facets) {
    Set<String> facetRules = facets.getBucketKeys(IssueFilterParameters.RULES);
    if (facetRules != null) {
      collector.addAll(SearchAdditionalField.RULES, from(facetRules).transform(RuleKeyFunctions.stringToRuleKey()));
    }
    collector.addProjectUuids(facets.getBucketKeys(IssueFilterParameters.PROJECT_UUIDS));
    collector.addComponentUuids(facets.getBucketKeys(IssueFilterParameters.COMPONENT_UUIDS));
    collector.addComponentUuids(facets.getBucketKeys(IssueFilterParameters.FILE_UUIDS));
    collector.addComponentUuids(facets.getBucketKeys(IssueFilterParameters.MODULE_UUIDS));
    collector.addAll(SearchAdditionalField.USERS, facets.getBucketKeys(IssueFilterParameters.ASSIGNEES));
    collector.addAll(SearchAdditionalField.USERS, facets.getBucketKeys(IssueFilterParameters.REPORTERS));
    collector.addAll(SearchAdditionalField.ACTION_PLANS, facets.getBucketKeys(ACTION_PLANS));
  }

  private void collectRequestParams(SearchResponseLoader.Collector collector, SearchWsRequest request) {
    collector.addProjectUuids(request.getProjectUuids());
    collector.addComponentUuids(request.getFileUuids());
    collector.addComponentUuids(request.getModuleUuids());
    collector.addComponentUuids(request.getComponentRootUuids());
    collector.addAll(SearchAdditionalField.USERS, request.getAssignees());
    collector.addAll(SearchAdditionalField.USERS, request.getReporters());
    collector.addAll(SearchAdditionalField.ACTION_PLANS, request.getActionPlans());
  }

  private static SearchWsRequest toSearchWsRequest(Request request) {
    return new SearchWsRequest()
      .setActionPlans(request.paramAsStrings(ACTION_PLANS))
      .setAdditionalFields(request.paramAsStrings(ADDITIONAL_FIELDS))
      .setAsc(request.paramAsBoolean(ASC))
      .setAssigned(request.paramAsBoolean(ASSIGNED))
      .setAssignees(request.paramAsStrings(ASSIGNEES))
      .setAuthors(request.paramAsStrings(AUTHORS))
      .setComponentKeys(request.paramAsStrings(COMPONENT_KEYS))
      .setComponentRootUuids(request.paramAsStrings(COMPONENT_ROOT_UUIDS))
      .setComponentRoots(request.paramAsStrings(COMPONENT_ROOTS))
      .setComponentUuids(request.paramAsStrings(COMPONENT_UUIDS))
      .setComponents(request.paramAsStrings(COMPONENTS))
      .setCreatedAfter(request.param(CREATED_AFTER))
      .setCreatedAt(request.param(CREATED_AT))
      .setCreatedBefore(request.param(CREATED_BEFORE))
      .setCreatedInLast(request.param(CREATED_IN_LAST))
      .setDirectories(request.paramAsStrings(DIRECTORIES))
      .setFacetMode(request.mandatoryParam(FACET_MODE))
      .setFacets(request.paramAsStrings(Param.FACETS))
      .setFileUuids(request.paramAsStrings(FILE_UUIDS))
      .setIssues(request.paramAsStrings(ISSUES))
      .setLanguages(request.paramAsStrings(LANGUAGES))
      .setModuleUuids(request.paramAsStrings(MODULE_UUIDS))
      .setOnComponentOnly(request.paramAsBoolean(ON_COMPONENT_ONLY))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setPlanned(request.paramAsBoolean(PLANNED))
      .setProjectKeys(request.paramAsStrings(PROJECT_KEYS))
      .setProjectUuids(request.paramAsStrings(PROJECT_UUIDS))
      .setProjects(request.paramAsStrings(PROJECTS))
      .setReporters(request.paramAsStrings(REPORTERS))
      .setResolutions(request.paramAsStrings(RESOLUTIONS))
      .setResolved(request.paramAsBoolean(RESOLVED))
      .setRules(request.paramAsStrings(RULES))
      .setSort(request.param(Param.SORT))
      .setSeverities(request.paramAsStrings(SEVERITIES))
      .setStatuses(request.paramAsStrings(STATUSES))
      .setTags(request.paramAsStrings(TAGS));
  }

  private enum IssueDocToKey implements Function<IssueDoc, String> {
    INSTANCE;

    @Override
    public String apply(IssueDoc input) {
      return input.key();
    }
  }
}

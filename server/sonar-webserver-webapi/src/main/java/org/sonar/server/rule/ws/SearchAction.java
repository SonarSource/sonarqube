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
package org.sonar.server.rule.ws;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules.SearchResponse;

import static java.lang.String.format;
import static org.sonar.api.server.ws.WebService.Param.ASCENDING;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.api.server.ws.WebService.Param.FIELDS;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.rule.index.RuleIndex.ALL_STATUSES_EXCEPT_REMOVED;
import static org.sonar.server.rule.index.RuleIndex.FACET_ACTIVE_SEVERITIES;
import static org.sonar.server.rule.index.RuleIndex.FACET_CWE;
import static org.sonar.server.rule.index.RuleIndex.FACET_LANGUAGES;
import static org.sonar.server.rule.index.RuleIndex.FACET_OLD_DEFAULT;
import static org.sonar.server.rule.index.RuleIndex.FACET_OWASP_TOP_10;
import static org.sonar.server.rule.index.RuleIndex.FACET_OWASP_TOP_10_2021;
import static org.sonar.server.rule.index.RuleIndex.FACET_REPOSITORIES;
import static org.sonar.server.rule.index.RuleIndex.FACET_SANS_TOP_25;
import static org.sonar.server.rule.index.RuleIndex.FACET_SEVERITIES;
import static org.sonar.server.rule.index.RuleIndex.FACET_SONARSOURCE_SECURITY;
import static org.sonar.server.rule.index.RuleIndex.FACET_STATUSES;
import static org.sonar.server.rule.index.RuleIndex.FACET_TAGS;
import static org.sonar.server.rule.index.RuleIndex.FACET_TYPES;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_DEPRECATED_KEYS;
import static org.sonar.server.rule.ws.RulesWsParameters.OPTIONAL_FIELDS;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_CWE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_OWASP_TOP_10;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_OWASP_TOP_10_2021;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_REPOSITORIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_SANS_TOP_25;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_SONARSOURCE_SECURITY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_STATUSES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TAGS;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TYPES;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements RulesWsAction {
  public static final String ACTION = "search";
  private static final String PARAM_ORGANIZATION = "organization";
  private static final Collection<String> DEFAULT_FACETS = ImmutableSet.of(PARAM_LANGUAGES, PARAM_REPOSITORIES, "tags");
  private static final String[] POSSIBLE_FACETS = new String[]{
    FACET_LANGUAGES,
    FACET_REPOSITORIES,
    FACET_TAGS,
    FACET_SEVERITIES,
    FACET_ACTIVE_SEVERITIES,
    FACET_STATUSES,
    FACET_TYPES,
    FACET_OLD_DEFAULT,
    FACET_CWE,
    FACET_OWASP_TOP_10,
    FACET_OWASP_TOP_10_2021,
    FACET_SANS_TOP_25,
    FACET_SONARSOURCE_SECURITY};

  private final RuleQueryFactory ruleQueryFactory;
  private final DbClient dbClient;
  private final RuleIndex ruleIndex;
  private final ActiveRuleCompleter activeRuleCompleter;
  private final RuleMapper mapper;
  private final RuleWsSupport ruleWsSupport;

  public SearchAction(RuleIndex ruleIndex, ActiveRuleCompleter activeRuleCompleter, RuleQueryFactory ruleQueryFactory, DbClient dbClient, RuleMapper mapper,
    RuleWsSupport ruleWsSupport) {
    this.ruleIndex = ruleIndex;
    this.activeRuleCompleter = activeRuleCompleter;
    this.ruleQueryFactory = ruleQueryFactory;
    this.dbClient = dbClient;
    this.mapper = mapper;
    this.ruleWsSupport = ruleWsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .addPagingParams(100, MAX_PAGE_SIZE)
      .setHandler(this)
      .setChangelog(
        new Change("9.8", "response fields 'total', 's', 'ps' have been deprecated, please use 'paging' object instead"),
        new Change("9.8", "The field 'paging' has been added to the response"),
        new Change("5.5", "The field 'effortToFixDescription' has been deprecated use 'gapDescription' instead"),
        new Change("5.5", "The field 'debtRemFnCoeff' has been deprecated use 'remFnGapMultiplier' instead"),
        new Change("5.5", "The field 'defaultDebtRemFnCoeff' has been deprecated use 'defaultRemFnGapMultiplier' instead"),
        new Change("5.5", "The field 'debtRemFnOffset' has been deprecated use 'remFnBaseEffort' instead"),
        new Change("5.5", "The field 'defaultDebtRemFnOffset' has been deprecated use 'defaultRemFnBaseEffort' instead"),
        new Change("7.1", "The field 'scope' has been added to the response"),
        new Change("7.1", "The field 'scope' has been added to the 'f' parameter"),
        new Change("7.2", "The field 'isExternal' has been added to the response"),
        new Change("7.2", "The field 'includeExternal' has been added to the 'f' parameter"),
        new Change("7.5", "The field 'updatedAt' has been added to the 'f' parameter"),
        new Change("9.5", "The field 'htmlDesc' has been deprecated use 'descriptionSections' instead"),
        new Change("9.5", "The field 'descriptionSections' has been added to the payload"),
        new Change("9.5", "The field 'descriptionSections' has been added to the 'f' parameter"),
        new Change("9.6", "'descriptionSections' can optionally embed a context field"),
        new Change("9.6", "The field 'educationPrinciples' has been added to the 'f' parameter")
      );

    action.createParam(FACETS)
      .setDescription("Comma-separated list of the facets to be computed. No facet is computed by default.")
      .setPossibleValues(POSSIBLE_FACETS)
      .setExampleValue(format("%s,%s", POSSIBLE_FACETS[0], POSSIBLE_FACETS[1]));

    action.createParam(PARAM_ORGANIZATION)
            .setDescription("Organization key")
            .setInternal(true);

    WebService.NewParam paramFields = action.createParam(FIELDS)
      .setDescription("Comma-separated list of additional fields to be returned in the response. All the fields are returned by default, except actives.")
      .setPossibleValues(Ordering.natural().sortedCopy(OPTIONAL_FIELDS));

    Iterator<String> it = OPTIONAL_FIELDS.iterator();
    paramFields.setExampleValue(format("%s,%s", it.next(), it.next()));
    action.setDescription("Search for a collection of relevant rules matching a specified query.<br/>")
      .setResponseExample(getClass().getResource("search-example.json"))
      .setSince("4.4")
      .setHandler(this);

    // Rule-specific search parameters
    RuleWsSupport.defineGenericRuleSearchParameters(action);
    RuleWsSupport.defineIsExternalParam(action);
  }
  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchRequest searchWsRequest = toSearchWsRequest(request);
      SearchOptions context = buildSearchOptions(searchWsRequest);
      RuleQuery query = ruleQueryFactory.createRuleSearchQuery(dbSession, request);
      SearchResult searchResult = doSearch(dbSession, query, context);
      SearchResponse responseBuilder = buildResponse(dbSession, searchWsRequest, context, searchResult, query);
      writeProtobuf(responseBuilder, request, response);
    }
  }

  private SearchResponse buildResponse(DbSession dbSession, SearchRequest request, SearchOptions context, SearchResult result, RuleQuery query) {
    SearchResponse.Builder responseBuilder = SearchResponse.newBuilder();
    writeStatistics(responseBuilder, result, context);
    doContextResponse(dbSession, request, result, responseBuilder, query);
    if (!context.getFacets().isEmpty()) {
      writeFacets(responseBuilder, request, context, result);
    }
    return responseBuilder.build();
  }

  private static void writeStatistics(SearchResponse.Builder response, SearchResult searchResult, SearchOptions context) {
    response.setTotal(searchResult.total);
    response.setP(context.getPage());
    response.setPs(context.getLimit());
    response.setPaging(formatPaging(searchResult.total, context.getPage(), context.getLimit()));
  }

  private static Common.Paging.Builder formatPaging(Long total, int pageIndex, int limit) {
    return Common.Paging.newBuilder()
      .setPageIndex(pageIndex)
      .setPageSize(limit)
      .setTotal(total.intValue());
  }

  private void writeRules(DbSession dbSession, SearchResponse.Builder response, SearchResult result, SearchOptions context) {
    Map<String, UserDto> usersByUuid = ruleWsSupport.getUsersByUuid(dbSession, result.rules);
    Map<String, List<DeprecatedRuleKeyDto>> deprecatedRuleKeysByRuleUuid = getDeprecatedRuleKeysByRuleUuid(dbSession, result.rules, context);
    result.rules.forEach(rule -> response.addRules(mapper.toWsRule(rule, result, context.getFields(), usersByUuid,
      deprecatedRuleKeysByRuleUuid)));
  }

  private Map<String, List<DeprecatedRuleKeyDto>> getDeprecatedRuleKeysByRuleUuid(DbSession dbSession, List<RuleDto> rules, SearchOptions context) {
    if (!RuleMapper.shouldReturnField(context.getFields(), FIELD_DEPRECATED_KEYS)) {
      return Collections.emptyMap();
    }

    Set<String> ruleUuidsSet = rules.stream()
      .map(RuleDto::getUuid)
      .collect(Collectors.toSet());
    if (ruleUuidsSet.isEmpty()) {
      return Collections.emptyMap();
    } else {
      return dbClient.ruleDao().selectDeprecatedRuleKeysByRuleUuids(dbSession, ruleUuidsSet).stream()
        .collect(Collectors.groupingBy(DeprecatedRuleKeyDto::getRuleUuid));
    }
  }

  private static SearchOptions buildSearchOptions(SearchRequest request) {
    SearchOptions context = loadCommonContext(request);
    SearchOptions searchOptions = new SearchOptions()
      .setLimit(context.getLimit())
      .setOffset(context.getOffset());
    if (context.getFacets().contains(RuleIndex.FACET_OLD_DEFAULT)) {
      searchOptions.addFacets(DEFAULT_FACETS);
    } else {
      searchOptions.addFacets(context.getFacets());
    }
    return searchOptions;
  }

  private static SearchOptions loadCommonContext(SearchRequest request) {
    int pageSize = Integer.parseInt(request.getPs());
    SearchOptions context = new SearchOptions().addFields(request.getF());
    if (request.getFacets() != null) {
      context.addFacets(request.getFacets());
    }
    if (pageSize < 1) {
      context.setPage(Integer.parseInt(request.getP()), 0).setLimit(MAX_PAGE_SIZE);
    } else {
      context.setPage(Integer.parseInt(request.getP()), pageSize);
    }
    return context;
  }

  private SearchResult doSearch(DbSession dbSession, RuleQuery query, SearchOptions context) {
    SearchIdResult<String> result = ruleIndex.search(query, context);
    List<String> ruleUuids = result.getUuids();
    // rule order is managed by ES, this order by must be kept when fetching rule details
    Map<String, RuleDto> rulesByRuleKey = Maps.uniqueIndex(dbClient.ruleDao().selectByUuids(dbSession,  query.getOrganization().getUuid(), ruleUuids), RuleDto::getUuid);
    List<RuleDto> rules = new ArrayList<>();
    for (String ruleUuid : ruleUuids) {
      RuleDto rule = rulesByRuleKey.get(ruleUuid);
      if (rule != null) {
        rules.add(rule);
      }
    }

    List<String> templateRuleUuids = rules.stream()
      .map(RuleDto::getTemplateUuid)
      .filter(Objects::nonNull)
      .collect(MoreCollectors.toList());
    List<RuleDto> templateRules = dbClient.ruleDao().selectByUuids(dbSession, templateRuleUuids);
    List<RuleParamDto> ruleParamDtos = dbClient.ruleDao().selectRuleParamsByRuleUuids(dbSession, ruleUuids);
    return new SearchResult()
      .setRules(rules)
      .setRuleParameters(ruleParamDtos)
      .setTemplateRules(templateRules)
      .setFacets(result.getFacets())
      .setTotal(result.getTotal());
  }

  private void doContextResponse(DbSession dbSession, SearchRequest request, SearchResult result, SearchResponse.Builder response, RuleQuery query) {
    SearchOptions contextForResponse = loadCommonContext(request);
    writeRules(dbSession, response, result, contextForResponse);
    if (contextForResponse.getFields().contains("actives")) {
      activeRuleCompleter.completeSearch(dbSession, query, result.rules, response);
    }
  }

  private static void writeFacets(SearchResponse.Builder response, SearchRequest request, SearchOptions context, SearchResult results) {
    addMandatoryFacetValues(results, FACET_LANGUAGES, request.getLanguages());
    addMandatoryFacetValues(results, FACET_REPOSITORIES, request.getRepositories());
    addMandatoryFacetValues(results, FACET_STATUSES, ALL_STATUSES_EXCEPT_REMOVED);
    addMandatoryFacetValues(results, FACET_SEVERITIES, Severity.ALL);
    addMandatoryFacetValues(results, FACET_ACTIVE_SEVERITIES, Severity.ALL);
    addMandatoryFacetValues(results, FACET_TAGS, request.getTags());
    addMandatoryFacetValues(results, FACET_TYPES, RuleType.names());
    addMandatoryFacetValues(results, FACET_CWE, request.getCwe());
    addMandatoryFacetValues(results, FACET_OWASP_TOP_10, request.getOwaspTop10());
    addMandatoryFacetValues(results, FACET_OWASP_TOP_10_2021, request.getOwaspTop10For2021());
    addMandatoryFacetValues(results, FACET_SANS_TOP_25, request.getSansTop25());
    addMandatoryFacetValues(results, FACET_SONARSOURCE_SECURITY, request.getSonarsourceSecurity());

    Common.Facet.Builder facet = Common.Facet.newBuilder();
    Common.FacetValue.Builder value = Common.FacetValue.newBuilder();
    Map<String, List<String>> facetValuesByFacetKey = new HashMap<>();
    facetValuesByFacetKey.put(FACET_LANGUAGES, request.getLanguages());
    facetValuesByFacetKey.put(FACET_REPOSITORIES, request.getRepositories());
    facetValuesByFacetKey.put(FACET_STATUSES, request.getStatuses());
    facetValuesByFacetKey.put(FACET_SEVERITIES, request.getSeverities());
    facetValuesByFacetKey.put(FACET_ACTIVE_SEVERITIES, request.getActiveSeverities());
    facetValuesByFacetKey.put(FACET_TAGS, request.getTags());
    facetValuesByFacetKey.put(FACET_TYPES, request.getTypes());
    facetValuesByFacetKey.put(FACET_CWE, request.getCwe());
    facetValuesByFacetKey.put(FACET_OWASP_TOP_10, request.getOwaspTop10());
    facetValuesByFacetKey.put(FACET_OWASP_TOP_10_2021, request.getOwaspTop10For2021());
    facetValuesByFacetKey.put(FACET_SANS_TOP_25, request.getSansTop25());
    facetValuesByFacetKey.put(FACET_SONARSOURCE_SECURITY, request.getSonarsourceSecurity());

    for (String facetName : context.getFacets()) {
      facet.clear().setProperty(facetName);
      Map<String, Long> facets = results.facets.get(facetName);
      if (facets != null) {
        Set<String> itemsFromFacets = new HashSet<>();
        for (Map.Entry<String, Long> facetValue : facets.entrySet()) {
          itemsFromFacets.add(facetValue.getKey());
          facet.addValues(value
            .clear()
            .setVal(facetValue.getKey())
            .setCount(facetValue.getValue()));
        }
        addZeroFacetsForSelectedItems(facet, facetValuesByFacetKey.get(facetName), itemsFromFacets);
      }
      response.getFacetsBuilder().addFacets(facet);
    }
  }

  private static void addZeroFacetsForSelectedItems(Common.Facet.Builder facet, @Nullable List<String> requestParams, Set<String> itemsFromFacets) {
    if (requestParams != null) {
      Common.FacetValue.Builder value = Common.FacetValue.newBuilder();
      for (String param : requestParams) {
        if (!itemsFromFacets.contains(param)) {
          facet.addValues(value.clear()
            .setVal(param)
            .setCount(0L));
        }
      }
    }
  }

  private static void addMandatoryFacetValues(SearchResult results, String facetName, @Nullable Collection<String> mandatoryValues) {
    Map<String, Long> facetValues = results.facets.get(facetName);
    if (facetValues != null) {
      Collection<String> valuesToAdd = mandatoryValues == null ? Lists.newArrayList() : mandatoryValues;
      for (String item : valuesToAdd) {
        if (!facetValues.containsKey(item)) {
          facetValues.put(item, 0L);
        }
      }
    }
  }

  private static SearchRequest toSearchWsRequest(Request request) {
    request.mandatoryParamAsBoolean(ASCENDING);
    return new SearchRequest()
            .setActiveSeverities(request.paramAsStrings(PARAM_ACTIVE_SEVERITIES))
            .setF(request.paramAsStrings(FIELDS))
            .setFacets(request.paramAsStrings(FACETS))
            .setLanguages(request.paramAsStrings(PARAM_LANGUAGES))
            .setP("" + request.mandatoryParamAsInt(PAGE))
            .setPs("" + request.mandatoryParamAsInt(PAGE_SIZE))
            .setRepositories(request.paramAsStrings(PARAM_REPOSITORIES))
            .setSeverities(request.paramAsStrings(PARAM_SEVERITIES))
            .setStatuses(request.paramAsStrings(PARAM_STATUSES))
            .setTags(request.paramAsStrings(PARAM_TAGS))
            .setTypes(request.paramAsStrings(PARAM_TYPES))
            .setCwe(request.paramAsStrings(PARAM_CWE))
            .setOwaspTop10(request.paramAsStrings(PARAM_OWASP_TOP_10))
            .setOwaspTop10For2021(request.paramAsStrings(PARAM_OWASP_TOP_10_2021))
            .setSansTop25(request.paramAsStrings(PARAM_SANS_TOP_25))
            .setSonarsourceSecurity(request.paramAsStrings(PARAM_SONARSOURCE_SECURITY));

  }

  static class SearchResult {
    private List<RuleDto> rules;
    private final ListMultimap<String, RuleParamDto> ruleParamsByRuleUuid;
    private final Map<String, RuleDto> templateRulesByRuleUuid;
    private Long total;
    private Facets facets;

    public SearchResult() {
      this.rules = new ArrayList<>();
      this.ruleParamsByRuleUuid = ArrayListMultimap.create();
      this.templateRulesByRuleUuid = new HashMap<>();
    }

    public List<RuleDto> getRules() {
      return rules;
    }

    public SearchResult setRules(List<RuleDto> rules) {
      this.rules = rules;
      return this;
    }

    public ListMultimap<String, RuleParamDto> getRuleParamsByRuleUuid() {
      return ruleParamsByRuleUuid;
    }

    public SearchResult setRuleParameters(List<RuleParamDto> ruleParams) {
      ruleParamsByRuleUuid.clear();
      for (RuleParamDto ruleParam : ruleParams) {
        ruleParamsByRuleUuid.put(ruleParam.getRuleUuid(), ruleParam);
      }
      return this;
    }

    public Map<String, RuleDto> getTemplateRulesByRuleUuid() {
      return templateRulesByRuleUuid;
    }

    public SearchResult setTemplateRules(List<RuleDto> templateRules) {
      templateRulesByRuleUuid.clear();
      for (RuleDto templateRule : templateRules) {
        templateRulesByRuleUuid.put(templateRule.getUuid(), templateRule);
      }
      return this;
    }

    @CheckForNull
    public Long getTotal() {
      return total;
    }

    public SearchResult setTotal(Long total) {
      this.total = total;
      return this;
    }

    @CheckForNull
    public Facets getFacets() {
      return facets;
    }

    public SearchResult setFacets(Facets facets) {
      this.facets = facets;
      return this;
    }
  }

  private static class SearchRequest {

    private List<String> activeSeverities;
    private List<String> f;
    private List<String> facets;
    private List<String> languages;
    private String p;
    private String ps;
    private List<String> repositories;
    private List<String> severities;
    private List<String> statuses;
    private List<String> tags;
    private List<String> types;
    private List<String> cwe;
    private List<String> owaspTop10;
    private List<String> owaspTop10For2021;
    private List<String> sansTop25;
    private List<String> sonarsourceSecurity;

    private SearchRequest setActiveSeverities(List<String> activeSeverities) {
      this.activeSeverities = activeSeverities;
      return this;
    }

    private List<String> getActiveSeverities() {
      return activeSeverities;
    }

    private SearchRequest setF(List<String> f) {
      this.f = f;
      return this;
    }

    private List<String> getF() {
      return f;
    }

    private SearchRequest setFacets(List<String> facets) {
      this.facets = facets;
      return this;
    }

    private List<String> getFacets() {
      return facets;
    }

    private SearchRequest setLanguages(List<String> languages) {
      this.languages = languages;
      return this;
    }

    private List<String> getLanguages() {
      return languages;
    }

    private SearchRequest setP(String p) {
      this.p = p;
      return this;
    }

    private String getP() {
      return p;
    }

    private SearchRequest setPs(String ps) {
      this.ps = ps;
      return this;
    }

    private String getPs() {
      return ps;
    }

    private SearchRequest setRepositories(List<String> repositories) {
      this.repositories = repositories;
      return this;
    }

    private List<String> getRepositories() {
      return repositories;
    }

    private SearchRequest setSeverities(List<String> severities) {
      this.severities = severities;
      return this;
    }

    private List<String> getSeverities() {
      return severities;
    }

    private SearchRequest setStatuses(List<String> statuses) {
      this.statuses = statuses;
      return this;
    }

    private List<String> getStatuses() {
      return statuses;
    }

    private SearchRequest setTags(List<String> tags) {
      this.tags = tags;
      return this;
    }

    private List<String> getTags() {
      return tags;
    }

    private SearchRequest setTypes(@Nullable List<String> types) {
      this.types = types;
      return this;
    }

    private List<String> getTypes() {
      return types;
    }

    public List<String> getCwe() {
      return cwe;
    }

    public SearchRequest setCwe(@Nullable List<String> cwe) {
      this.cwe = cwe;
      return this;
    }

    public List<String> getOwaspTop10() {
      return owaspTop10;
    }

    public SearchRequest setOwaspTop10(@Nullable List<String> owaspTop10) {
      this.owaspTop10 = owaspTop10;
      return this;
    }

    public List<String> getOwaspTop10For2021() {
      return owaspTop10For2021;
    }

    public SearchRequest setOwaspTop10For2021(@Nullable List<String> owaspTop10For2021) {
      this.owaspTop10For2021 = owaspTop10For2021;
      return this;
    }

    public List<String> getSansTop25() {
      return sansTop25;
    }

    public SearchRequest setSansTop25(@Nullable List<String> sansTop25) {
      this.sansTop25 = sansTop25;
      return this;
    }

    public List<String> getSonarsourceSecurity() {
      return sonarsourceSecurity;
    }

    public SearchRequest setSonarsourceSecurity(@Nullable List<String> sonarsourceSecurity) {
      this.sonarsourceSecurity = sonarsourceSecurity;
      return this;
    }
  }
}

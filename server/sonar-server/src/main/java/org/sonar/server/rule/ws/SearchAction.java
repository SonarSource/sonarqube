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
package org.sonar.server.rule.ws;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleQuery;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules.SearchResponse;

import static java.lang.String.format;
import static org.sonar.api.server.ws.WebService.Param.ASCENDING;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.api.server.ws.WebService.Param.FIELDS;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SORT;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonar.server.rule.index.RuleIndex.ALL_STATUSES_EXCEPT_REMOVED;
import static org.sonar.server.rule.index.RuleIndex.FACET_ACTIVE_SEVERITIES;
import static org.sonar.server.rule.index.RuleIndex.FACET_LANGUAGES;
import static org.sonar.server.rule.index.RuleIndex.FACET_OLD_DEFAULT;
import static org.sonar.server.rule.index.RuleIndex.FACET_REPOSITORIES;
import static org.sonar.server.rule.index.RuleIndex.FACET_SEVERITIES;
import static org.sonar.server.rule.index.RuleIndex.FACET_STATUSES;
import static org.sonar.server.rule.index.RuleIndex.FACET_TAGS;
import static org.sonar.server.rule.index.RuleIndex.FACET_TYPES;
import static org.sonar.server.rule.ws.RulesWsParameters.OPTIONAL_FIELDS;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_COMPARE_TO_PROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_INHERITANCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ORGANIZATION;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_QPROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_REPOSITORIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_RULE_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_STATUSES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TAGS;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TEMPLATE_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TYPES;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements RulesWsAction {
  public static final String ACTION = "search";

  private static final Collection<String> DEFAULT_FACETS = ImmutableSet.of(PARAM_LANGUAGES, PARAM_REPOSITORIES, "tags");
  private static final String[] POSSIBLE_FACETS = new String[] {
    FACET_LANGUAGES,
    FACET_REPOSITORIES,
    FACET_TAGS,
    FACET_SEVERITIES,
    FACET_ACTIVE_SEVERITIES,
    FACET_STATUSES,
    FACET_TYPES,
    FACET_OLD_DEFAULT};

  private final RuleQueryFactory ruleQueryFactory;
  private final DbClient dbClient;
  private final RuleIndex ruleIndex;
  private final ActiveRuleCompleter activeRuleCompleter;
  private final RuleMapper mapper;

  public SearchAction(RuleIndex ruleIndex, ActiveRuleCompleter activeRuleCompleter, RuleQueryFactory ruleQueryFactory, DbClient dbClient, RuleMapper mapper) {
    this.ruleIndex = ruleIndex;
    this.activeRuleCompleter = activeRuleCompleter;
    this.ruleQueryFactory = ruleQueryFactory;
    this.dbClient = dbClient;
    this.mapper = mapper;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .addPagingParams(100, MAX_LIMIT)
      .setHandler(this)
      .setChangelog(new Change("7.1", "The field 'scope' has been added to the response"))
      .setChangelog(new Change("7.1", "The field 'scope' has been added to the 'f' parameter"));

    action.createParam(FACETS)
      .setDescription("Comma-separated list of the facets to be computed. No facet is computed by default.")
      .setPossibleValues(POSSIBLE_FACETS)
      .setExampleValue(format("%s,%s", POSSIBLE_FACETS[0], POSSIBLE_FACETS[1]));

    WebService.NewParam paramFields = action.createParam(FIELDS)
      .setDescription("Comma-separated list of the fields to be returned in response. All the fields are returned by default, except actives." +
        "Since 5.5, following fields have been deprecated :" +
        "<ul>" +
        "<li>\"defaultDebtRemFn\" becomes \"defaultRemFn\"</li>" +
        "<li>\"debtRemFn\" becomes \"remFn\"</li>" +
        "<li>\"effortToFixDescription\" becomes \"gapDescription\"</li>" +
        "<li>\"debtOverloaded\" becomes \"remFnOverloaded\"</li>" +
        "</ul>")
      .setPossibleValues(Ordering.natural().sortedCopy(OPTIONAL_FIELDS));

    Iterator<String> it = OPTIONAL_FIELDS.iterator();
    paramFields.setExampleValue(format("%s,%s", it.next(), it.next()));
    doDefinition(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchRequest searchWsRequest = toSearchWsRequest(request);
      SearchOptions context = buildSearchOptions(searchWsRequest);
      RuleQuery query = ruleQueryFactory.createRuleQuery(dbSession, request);
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
  }

  private void doDefinition(WebService.NewAction action) {
    action.setDescription("Search for a collection of relevant rules matching a specified query.<br/>" +
      "Since 5.5, following fields in the response have been deprecated :" +
      "<ul>" +
      "<li>\"effortToFixDescription\" becomes \"gapDescription\"</li>" +
      "<li>\"debtRemFnCoeff\" becomes \"remFnGapMultiplier\"</li>" +
      "<li>\"defaultDebtRemFnCoeff\" becomes \"defaultRemFnGapMultiplier\"</li>" +
      "<li>\"debtRemFnOffset\" becomes \"remFnBaseEffort\"</li>" +
      "<li>\"defaultDebtRemFnOffset\" becomes \"defaultRemFnBaseEffort\"</li>" +
      "<li>\"debtOverloaded\" becomes \"remFnOverloaded\"</li>" +
      "</ul>")
      .setResponseExample(getClass().getResource("search-example.json"))
      .setSince("4.4")
      .setHandler(this);

    // Rule-specific search parameters
    defineRuleSearchParameters(action);
  }

  public static void defineRuleSearchParameters(WebService.NewAction action) {
    action
      .createParam(TEXT_QUERY)
      .setMinimumLength(2)
      .setDescription("UTF-8 search query")
      .setExampleValue("xpath");

    action
      .createParam(PARAM_RULE_KEY)
      .setDescription("Key of rule to search for")
      .setExampleValue("squid:S001");

    action
      .createParam(PARAM_REPOSITORIES)
      .setDescription("Comma-separated list of repositories")
      .setExampleValue("checkstyle,findbugs");

    action
      .createParam(PARAM_SEVERITIES)
      .setDescription("Comma-separated list of default severities. Not the same than severity of rules in Quality profiles.")
      .setPossibleValues(Severity.ALL)
      .setExampleValue("CRITICAL,BLOCKER");

    action
      .createParam(PARAM_LANGUAGES)
      .setDescription("Comma-separated list of languages")
      .setExampleValue("java,js");

    action
      .createParam(PARAM_STATUSES)
      .setDescription("Comma-separated list of status codes")
      .setPossibleValues(RuleStatus.values())
      .setExampleValue(RuleStatus.READY);

    action
      .createParam(PARAM_AVAILABLE_SINCE)
      .setDescription("Filters rules added since date. Format is yyyy-MM-dd")
      .setExampleValue("2014-06-22");

    action
      .createParam(PARAM_TAGS)
      .setDescription("Comma-separated list of tags. Returned rules match any of the tags (OR operator)")
      .setExampleValue("security,java8");

    action
      .createParam(PARAM_TYPES)
      .setSince("5.5")
      .setDescription("Comma-separated list of types. Returned rules match any of the tags (OR operator)")
      .setPossibleValues(RuleType.values())
      .setExampleValue(RuleType.BUG);

    action
      .createParam(PARAM_ACTIVATION)
      .setDescription("Filter rules that are activated or deactivated on the selected Quality profile. Ignored if " +
        "the parameter '" + PARAM_QPROFILE + "' is not set.")
      .setBooleanPossibleValues();

    action
      .createParam(PARAM_QPROFILE)
      .setDescription("Quality profile key to filter on. Used only if the parameter '" +
        PARAM_ACTIVATION + "' is set.")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_COMPARE_TO_PROFILE)
      .setDescription("Quality profile key to filter rules that are activated. Meant to compare easily to profile set in '%s'", PARAM_QPROFILE)
      .setInternal(true)
      .setSince("6.5")
      .setExampleValue(UUID_EXAMPLE_02);

    action
      .createParam(PARAM_INHERITANCE)
      .setDescription("Comma-separated list of values of inheritance for a rule within a quality profile. Used only if the parameter '" +
        PARAM_ACTIVATION + "' is set.")
      .setPossibleValues(ActiveRuleInheritance.NONE.name(),
        ActiveRuleInheritance.INHERITED.name(),
        ActiveRuleInheritance.OVERRIDES.name())
      .setExampleValue(ActiveRuleInheritance.INHERITED.name() + "," +
        ActiveRuleInheritance.OVERRIDES.name());

    action
      .createParam(PARAM_ACTIVE_SEVERITIES)
      .setDescription("Comma-separated list of activation severities, i.e the severity of rules in Quality profiles.")
      .setPossibleValues(Severity.ALL)
      .setExampleValue("CRITICAL,BLOCKER");

    action
      .createParam(PARAM_IS_TEMPLATE)
      .setDescription("Filter template rules")
      .setBooleanPossibleValues();

    action
      .createParam(PARAM_TEMPLATE_KEY)
      .setDescription("Key of the template rule to filter on. Used to search for the custom rules based on this template.")
      .setExampleValue("java:S001");

    action
      .createParam(SORT)
      .setDescription("Sort field")
      .setPossibleValues(RuleIndexDefinition.SORT_FIELDS)
      .setExampleValue(RuleIndexDefinition.SORT_FIELDS.iterator().next());

    action
      .createParam(ASCENDING)
      .setDescription("Ascending sort")
      .setBooleanPossibleValues()
      .setDefaultValue(true);

    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setRequired(false)
      .setInternal(true)
      .setExampleValue("my-org")
      .setSince("6.4");
  }

  private void writeRules(SearchResponse.Builder response, SearchResult result, SearchOptions context) {
    for (RuleDto rule : result.rules) {
      response.addRules(mapper.toWsRule(rule.getDefinition(), result, context.getFields(), rule.getMetadata()));
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
      context.setPage(Integer.parseInt(request.getP()), 0).setLimit(MAX_LIMIT);
    } else {
      context.setPage(Integer.parseInt(request.getP()), pageSize);
    }
    return context;
  }

  private SearchResult doSearch(DbSession dbSession, RuleQuery query, SearchOptions context) {
    SearchIdResult<Integer> result = ruleIndex.search(query, context);
    List<Integer> ruleIds = result.getIds();
    // rule order is managed by ES
    Map<Integer, RuleDto> rulesByRuleKey = Maps.uniqueIndex(
      dbClient.ruleDao().selectByIds(dbSession, query.getOrganization().getUuid(), ruleIds), RuleDto::getId);
    List<RuleDto> rules = new ArrayList<>();
    for (Integer ruleId : ruleIds) {
      RuleDto rule = rulesByRuleKey.get(ruleId);
      if (rule != null) {
        rules.add(rule);
      }
    }
    List<Integer> templateRuleIds = rules.stream()
      .map(RuleDto::getTemplateId)
      .filter(Objects::nonNull)
      .collect(MoreCollectors.toList());
    List<RuleDefinitionDto> templateRules = dbClient.ruleDao().selectDefinitionByIds(dbSession, templateRuleIds);
    List<RuleParamDto> ruleParamDtos = dbClient.ruleDao().selectRuleParamsByRuleIds(dbSession, ruleIds);
    return new SearchResult()
      .setRules(rules)
      .setRuleParameters(ruleParamDtos)
      .setTemplateRules(templateRules)
      .setFacets(result.getFacets())
      .setTotal(result.getTotal());
  }

  private void doContextResponse(DbSession dbSession, SearchRequest request, SearchResult result, SearchResponse.Builder response, RuleQuery query) {
    SearchOptions contextForResponse = loadCommonContext(request);
    writeRules(response, result, contextForResponse);
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
      .setTypes(request.paramAsStrings(PARAM_TYPES));
  }

  static class SearchResult {
    private List<RuleDto> rules;
    private final ListMultimap<Integer, RuleParamDto> ruleParamsByRuleId;
    private final Map<Integer, RuleDefinitionDto> templateRulesByRuleId;
    private Long total;
    private Facets facets;

    public SearchResult() {
      this.rules = new ArrayList<>();
      this.ruleParamsByRuleId = ArrayListMultimap.create();
      this.templateRulesByRuleId = new HashMap<>();
    }

    public List<RuleDto> getRules() {
      return rules;
    }

    public SearchResult setRules(List<RuleDto> rules) {
      this.rules = rules;
      return this;
    }

    public ListMultimap<Integer, RuleParamDto> getRuleParamsByRuleId() {
      return ruleParamsByRuleId;
    }

    public SearchResult setRuleParameters(List<RuleParamDto> ruleParams) {
      ruleParamsByRuleId.clear();
      for (RuleParamDto ruleParam : ruleParams) {
        ruleParamsByRuleId.put(ruleParam.getRuleId(), ruleParam);
      }
      return this;
    }

    public Map<Integer, RuleDefinitionDto> getTemplateRulesByRuleId() {
      return templateRulesByRuleId;
    }

    public SearchResult setTemplateRules(List<RuleDefinitionDto> templateRules) {
      templateRulesByRuleId.clear();
      for (RuleDefinitionDto templateRule : templateRules) {
        templateRulesByRuleId.put(templateRule.getId(), templateRule);
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

    private SearchRequest setTypes(List<String> types) {
      this.types = types;
      return this;
    }

    private List<String> getTypes() {
      return types;
    }
  }
}

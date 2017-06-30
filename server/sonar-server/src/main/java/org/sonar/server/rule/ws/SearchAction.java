/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
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
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleQuery;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules.SearchResponse;
import org.sonarqube.ws.client.rule.SearchWsRequest;

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
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.rule.RulesWsParameters.OPTIONAL_FIELDS;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_COMPARE_TO_PROFILE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_INHERITANCE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_QPROFILE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_REPOSITORIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_RULE_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TEMPLATE_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TYPES;

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
      .setHandler(this);

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
      SearchWsRequest searchWsRequest = toSearchWsRequest(request);
      SearchOptions context = buildSearchOptions(searchWsRequest);
      RuleQuery query = ruleQueryFactory.createRuleQuery(dbSession, request);
      SearchResult searchResult = doSearch(dbSession, query, context);
      SearchResponse responseBuilder = buildResponse(dbSession, searchWsRequest, context, searchResult, query);
      writeProtobuf(responseBuilder, request, response);
    }
  }

  private SearchResponse buildResponse(DbSession dbSession, SearchWsRequest request, SearchOptions context, SearchResult result, RuleQuery query) {
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
      .setPossibleValues(ActiveRule.Inheritance.NONE.name(),
        ActiveRule.Inheritance.INHERITED.name(),
        ActiveRule.Inheritance.OVERRIDES.name())
      .setExampleValue(ActiveRule.Inheritance.INHERITED.name() + "," +
        ActiveRule.Inheritance.OVERRIDES.name());

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

  private static SearchOptions buildSearchOptions(SearchWsRequest request) {
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

  private static SearchOptions loadCommonContext(SearchWsRequest request) {
    int pageSize = request.getPageSize();
    SearchOptions context = new SearchOptions().addFields(request.getFields());
    if (request.getFacets() != null) {
      context.addFacets(request.getFacets());
    }
    if (pageSize < 1) {
      context.setPage(request.getPage(), 0).setLimit(MAX_LIMIT);
    } else {
      context.setPage(request.getPage(), pageSize);
    }
    return context;
  }

  private SearchResult doSearch(DbSession dbSession, RuleQuery query, SearchOptions context) {
    SearchIdResult<RuleKey> result = ruleIndex.search(query, context);
    List<RuleKey> ruleKeys = result.getIds();
    // rule order is managed by ES
    Map<RuleKey, RuleDto> rulesByRuleKey = Maps.uniqueIndex(
      dbClient.ruleDao().selectByKeys(dbSession, query.getOrganization(), ruleKeys), RuleDto::getKey);
    List<RuleDto> rules = new ArrayList<>();
    for (RuleKey ruleKey : ruleKeys) {
      RuleDto rule = rulesByRuleKey.get(ruleKey);
      if (rule != null) {
        rules.add(rule);
      }
    }
    List<Integer> ruleIds = rules.stream().map(RuleDto::getId).collect(MoreCollectors.toList());
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

  private void doContextResponse(DbSession dbSession, SearchWsRequest request, SearchResult result, SearchResponse.Builder response, RuleQuery query) {
    SearchOptions contextForResponse = loadCommonContext(request);
    writeRules(response, result, contextForResponse);
    if (contextForResponse.getFields().contains("actives")) {
      activeRuleCompleter.completeSearch(dbSession, query, result.rules, response);
    }
  }

  private static void writeFacets(SearchResponse.Builder response, SearchWsRequest request, SearchOptions context, SearchResult results) {
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
        Set<String> itemsFromFacets = Sets.newHashSet();
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

  private static SearchWsRequest toSearchWsRequest(Request request) {
    return new SearchWsRequest()
      .setActivation(request.paramAsBoolean(PARAM_ACTIVATION))
      .setActiveSeverities(request.paramAsStrings(PARAM_ACTIVE_SEVERITIES))
      .setAsc(request.mandatoryParamAsBoolean(ASCENDING))
      .setAvailableSince(request.param(PARAM_AVAILABLE_SINCE))
      .setFields(request.paramAsStrings(FIELDS))
      .setFacets(request.paramAsStrings(FACETS))
      .setInheritance(request.paramAsStrings(PARAM_INHERITANCE))
      .setIsTemplate(request.paramAsBoolean(PARAM_IS_TEMPLATE))
      .setLanguages(request.paramAsStrings(PARAM_LANGUAGES))
      .setPage(request.mandatoryParamAsInt(PAGE))
      .setPageSize(request.mandatoryParamAsInt(PAGE_SIZE))
      .setQuery(request.param(TEXT_QUERY))
      .setQProfile(request.param(PARAM_QPROFILE))
      .setRepositories(request.paramAsStrings(PARAM_REPOSITORIES))
      .setRuleKey(request.param(PARAM_RULE_KEY))
      .setSort(request.param(SORT))
      .setSeverities(request.paramAsStrings(PARAM_SEVERITIES))
      .setStatuses(request.paramAsStrings(PARAM_STATUSES))
      .setTags(request.paramAsStrings(PARAM_TAGS))
      .setTemplateKey(request.param(PARAM_TEMPLATE_KEY))
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

}

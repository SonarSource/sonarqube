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
package org.sonar.server.rule.ws;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleQuery;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules.SearchResponse;

import static com.google.common.collect.FluentIterable.from;
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
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_INHERITANCE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_QPROFILE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_REPOSITORIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TEMPLATE_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TYPES;

/**
 * @since 4.4
 */
public class SearchAction implements RulesWsAction {
  public static final String ACTION = "search";

  private static final Collection<String> DEFAULT_FACETS = ImmutableSet.of(PARAM_LANGUAGES, PARAM_REPOSITORIES, "tags");

  private final DbClient dbClient;
  private final RuleIndex ruleIndex;
  private final ActiveRuleCompleter activeRuleCompleter;
  private final RuleMapper mapper;

  public SearchAction(RuleIndex ruleIndex, ActiveRuleCompleter activeRuleCompleter, DbClient dbClient, RuleMapper mapper) {
    this.ruleIndex = ruleIndex;
    this.activeRuleCompleter = activeRuleCompleter;
    this.dbClient = dbClient;
    this.mapper = mapper;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .addPagingParams(100, org.sonar.server.es.SearchOptions.MAX_LIMIT)
      .setHandler(this);

    Collection<String> possibleFacets = possibleFacets();
    WebService.NewParam paramFacets = action.createParam(Param.FACETS)
      .setDescription("Comma-separated list of the facets to be computed. No facet is computed by default.")
      .setPossibleValues(possibleFacets);
    if (possibleFacets != null && possibleFacets.size() > 1) {
      Iterator<String> it = possibleFacets.iterator();
      paramFacets.setExampleValue(String.format("%s,%s", it.next(), it.next()));
    }

    WebService.NewParam paramFields = action.createParam(Param.FIELDS)
      .setDescription("Comma-separated list of the fields to be returned in response. All the fields are returned by default, except actives.")
      .setPossibleValues(OPTIONAL_FIELDS);
    Iterator<String> it = OPTIONAL_FIELDS.iterator();
    paramFields.setExampleValue(String.format("%s,%s", it.next(), it.next()));

    doDefinition(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    try {
      org.sonar.server.es.SearchOptions context = getQueryContext(request);
      RuleQuery query = doQuery(request);
      SearchResult searchResult = doSearch(dbSession, query, context);
      SearchResponse responseBuilder = buildResponse(dbSession, request, context, searchResult);
      writeProtobuf(responseBuilder, request, response);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private SearchResponse buildResponse(DbSession dbSession, Request request, org.sonar.server.es.SearchOptions context, SearchResult result) {
    SearchResponse.Builder responseBuilder = SearchResponse.newBuilder();
    writeStatistics(responseBuilder, result, context);
    doContextResponse(dbSession, request, result, responseBuilder);
    if (!context.getFacets().isEmpty()) {
      writeFacets(responseBuilder, request, context, result);
    }
    return responseBuilder.build();
  }

  protected void writeStatistics(SearchResponse.Builder response, SearchResult searchResult, org.sonar.server.es.SearchOptions context) {
    response.setTotal(searchResult.total);
    response.setP(context.getPage());
    response.setPs(context.getLimit());
  }

  protected void doDefinition(WebService.NewAction action) {
    action.setDescription("Search for a collection of relevant rules matching a specified query.<br/>" +
      "Since 5.5, following fields in the response have been deprecated :" +
      "<ul><li>\"effortToFixDescription\" becomes \"gapDescription\"</li>" +
      "<li>\"debtRemFnCoeff\" becomes \"remFnGapMultiplier\"</li>" +
      "<li>\"defaultDebtRemFnCoeff\" becomes \"defaultRemFnGapMultiplier\"</li>" +
      "<li>\"debtRemFnOffset\" becomes \"remFnBaseEffort\"</li>" +
      "<li>\"defaultDebtRemFnOffset\" becomes \"defaultRemFnBaseEffort\"</li></ul>")
      .setResponseExample(Resources.getResource(getClass(), "example-search.json"))
      .setSince("4.4")
      .setHandler(this);

    // Rule-specific search parameters
    defineRuleSearchParameters(action);
  }

  @CheckForNull
  protected Collection<String> possibleFacets() {
    return Arrays.asList(
      FACET_LANGUAGES,
      FACET_REPOSITORIES,
      FACET_TAGS,
      FACET_SEVERITIES,
      FACET_ACTIVE_SEVERITIES,
      FACET_STATUSES,
      FACET_TYPES,
      FACET_OLD_DEFAULT);
  }

  /**
   * public visibility because used by {@link org.sonar.server.qualityprofile.ws.BulkRuleActivationActions}
   */
  public static void defineRuleSearchParameters(WebService.NewAction action) {
    action
      .createParam(Param.TEXT_QUERY)
      .setDescription("UTF-8 search query")
      .setExampleValue("xpath");

    action
      .createParam(PARAM_KEY)
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
      .setDescription("Key of Quality profile to filter on. Used only if the parameter '" +
        PARAM_ACTIVATION + "' is set.")
      .setExampleValue("sonar-way-cs-12345");

    action
      .createParam(PARAM_INHERITANCE)
      .setDescription("Value of inheritance for a rule within a quality profile Used only if the parameter '" +
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
      .createParam(Param.SORT)
      .setDescription("Sort field")
      .setPossibleValues(RuleIndexDefinition.SORT_FIELDS)
      .setExampleValue(RuleIndexDefinition.SORT_FIELDS.iterator().next());

    action
      .createParam(Param.ASCENDING)
      .setDescription("Ascending sort")
      .setBooleanPossibleValues()
      .setDefaultValue(true);
  }

  public static RuleQuery createRuleQuery(RuleQuery query, Request request) {
    query.setQueryText(request.param(Param.TEXT_QUERY));
    query.setSeverities(request.paramAsStrings(PARAM_SEVERITIES));
    query.setRepositories(request.paramAsStrings(PARAM_REPOSITORIES));
    query.setAvailableSince(request.hasParam(PARAM_AVAILABLE_SINCE) ? request.paramAsDate(PARAM_AVAILABLE_SINCE).getTime() : null);
    query.setStatuses(request.paramAsEnums(PARAM_STATUSES, RuleStatus.class));
    query.setLanguages(request.paramAsStrings(PARAM_LANGUAGES));
    query.setActivation(request.paramAsBoolean(PARAM_ACTIVATION));
    query.setQProfileKey(request.param(PARAM_QPROFILE));
    query.setTags(request.paramAsStrings(PARAM_TAGS));
    query.setInheritance(request.paramAsStrings(PARAM_INHERITANCE));
    query.setActiveSeverities(request.paramAsStrings(PARAM_ACTIVE_SEVERITIES));
    query.setIsTemplate(request.paramAsBoolean(PARAM_IS_TEMPLATE));
    query.setTemplateKey(request.param(PARAM_TEMPLATE_KEY));
    query.setTypes(request.paramAsEnums(PARAM_TYPES, RuleType.class));
    query.setKey(request.param(PARAM_KEY));

    String sortParam = request.param(Param.SORT);
    if (sortParam != null) {
      query.setSortField(sortParam);
      query.setAscendingSort(request.mandatoryParamAsBoolean(Param.ASCENDING));
    }
    return query;
  }

  private void writeRules(SearchResponse.Builder response, SearchResult result, org.sonar.server.es.SearchOptions context) {
    for (RuleDto rule : result.rules) {
      response.addRules(mapper.toWsRule(rule, result, context.getFields()));
    }
  }

  protected org.sonar.server.es.SearchOptions getQueryContext(Request request) {
    // TODO Get rid of this horrible hack: fields on request are not the same as fields for ES search ! 1/2
    org.sonar.server.es.SearchOptions context = loadCommonContext(request);
    org.sonar.server.es.SearchOptions searchQueryContext = new org.sonar.server.es.SearchOptions()
      .setLimit(context.getLimit())
      .setOffset(context.getOffset());
    if (context.getFacets().contains(RuleIndex.FACET_OLD_DEFAULT)) {
      searchQueryContext.addFacets(DEFAULT_FACETS);
    } else {
      searchQueryContext.addFacets(context.getFacets());
    }
    return searchQueryContext;
  }

  private static org.sonar.server.es.SearchOptions loadCommonContext(Request request) {
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    org.sonar.server.es.SearchOptions context = new org.sonar.server.es.SearchOptions().addFields(request.paramAsStrings(Param.FIELDS));
    List<String> facets = request.paramAsStrings(Param.FACETS);
    if (facets != null) {
      context.addFacets(facets);
    }
    if (pageSize < 1) {
      context.setPage(request.mandatoryParamAsInt(Param.PAGE), 0).setLimit(org.sonar.server.es.SearchOptions.MAX_LIMIT);
    } else {
      context.setPage(request.mandatoryParamAsInt(Param.PAGE), pageSize);
    }
    return context;
  }

  protected SearchResult doSearch(DbSession dbSession, RuleQuery query, org.sonar.server.es.SearchOptions context) {
    SearchIdResult<RuleKey> result = ruleIndex.search(query, context);
    List<RuleKey> ruleKeys = result.getIds();
    // rule order is managed by ES
    Map<RuleKey, RuleDto> rulesByRuleKey = Maps.uniqueIndex(
      dbClient.ruleDao().selectByKeys(dbSession, ruleKeys),
      new Function<RuleDto, RuleKey>() {
        @Override
        public RuleKey apply(@Nonnull RuleDto input) {
          return input.getKey();
        }
      });
    List<RuleDto> rules = new ArrayList<>();
    for (RuleKey ruleKey : ruleKeys) {
      RuleDto rule = rulesByRuleKey.get(ruleKey);
      if (rule != null) {
        rules.add(rule);
      }
    }
    List<Integer> ruleIds = from(rules).transform(RuleDtoToId.INSTANCE).toList();
    List<Integer> templateRuleIds = from(rules)
      .transform(RuleDtoToTemplateId.INSTANCE)
      .filter(Predicates.<Integer>notNull())
      .toList();
    List<RuleDto> templateRules = dbClient.ruleDao().selectByIds(dbSession, templateRuleIds);
    List<RuleParamDto> ruleParamDtos = dbClient.ruleDao().selectRuleParamsByRuleIds(dbSession, ruleIds);
    return new SearchResult()
      .setRules(rules)
      .setRuleParameters(ruleParamDtos)
      .setTemplateRules(templateRules)
      .setFacets(result.getFacets())
      .setTotal(result.getTotal());
  }

  protected RuleQuery doQuery(Request request) {
    RuleQuery plainQuery = createRuleQuery(new RuleQuery(), request);

    String qProfileKey = request.param(PARAM_QPROFILE);
    if (qProfileKey != null) {
      QualityProfileDto qProfile = activeRuleCompleter.loadProfile(qProfileKey);
      if (qProfile != null) {
        plainQuery.setLanguages(ImmutableList.of(qProfile.getLanguage()));
      }
    }

    return plainQuery;
  }

  protected void doContextResponse(DbSession dbSession, Request request, SearchResult result, SearchResponse.Builder response) {
    // TODO Get rid of this horrible hack: fields on request are not the same as fields for ES search ! 2/2
    org.sonar.server.es.SearchOptions contextForResponse = loadCommonContext(request);
    writeRules(response, result, contextForResponse);
    if (contextForResponse.getFields().contains("actives")) {
      activeRuleCompleter.completeSearch(dbSession, doQuery(request), result.rules, response);
    }
  }

  protected void writeFacets(SearchResponse.Builder response, Request request, org.sonar.server.es.SearchOptions context, SearchResult results) {
    addMandatoryFacetValues(results, FACET_LANGUAGES, request.paramAsStrings(PARAM_LANGUAGES));
    addMandatoryFacetValues(results, FACET_REPOSITORIES, request.paramAsStrings(PARAM_REPOSITORIES));
    addMandatoryFacetValues(results, FACET_STATUSES, ALL_STATUSES_EXCEPT_REMOVED);
    addMandatoryFacetValues(results, FACET_SEVERITIES, Severity.ALL);
    addMandatoryFacetValues(results, FACET_ACTIVE_SEVERITIES, Severity.ALL);
    addMandatoryFacetValues(results, FACET_TAGS, request.paramAsStrings(PARAM_TAGS));
    addMandatoryFacetValues(results, FACET_TYPES, RuleType.ALL_NAMES);

    Common.Facet.Builder facet = Common.Facet.newBuilder();
    Common.FacetValue.Builder value = Common.FacetValue.newBuilder();
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
        addZeroFacetsForSelectedItems(facet, request, facetName, itemsFromFacets);
      }
      response.getFacetsBuilder().addFacets(facet);
    }
  }

  private static void addZeroFacetsForSelectedItems(Common.Facet.Builder facet, Request request, String facetName, Set<String> itemsFromFacets) {
    List<String> requestParams = request.paramAsStrings(facetName);
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

  protected void addMandatoryFacetValues(SearchResult results, String facetName, @Nullable List<String> mandatoryValues) {
    Map<String, Long> facetValues = results.facets.get(facetName);
    if (facetValues != null) {
      List<String> valuesToAdd = mandatoryValues == null ? Lists.<String>newArrayList() : mandatoryValues;
      for (String item : valuesToAdd) {
        if (!facetValues.containsKey(item)) {
          facetValues.put(item, 0L);
        }
      }
    }
  }

  static class SearchResult {
    private List<RuleDto> rules;
    private final ListMultimap<Integer, RuleParamDto> ruleParamsByRuleId;
    private final Map<Integer, RuleDto> templateRulesByRuleId;
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

    public Map<Integer, RuleDto> getTemplateRulesByRuleId() {
      return templateRulesByRuleId;
    }

    public SearchResult setTemplateRules(List<RuleDto> templateRules) {
      templateRulesByRuleId.clear();
      for (RuleDto templateRule : templateRules) {
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

  private enum RuleDtoToId implements Function<RuleDto, Integer> {
    INSTANCE;

    @Override
    public Integer apply(@Nonnull RuleDto input) {
      return input.getId();
    }
  }

  private enum RuleDtoToTemplateId implements Function<RuleDto, Integer> {
    INSTANCE;

    @Override
    public Integer apply(@Nonnull RuleDto input) {
      return input.getTemplateId();
    }
  }

  private enum TypeToString implements Function<RuleType, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull RuleType input) {
      return input.name();
    }

  }
}

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
package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.search.ws.SearchOptions;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules.SearchResponse;

import static org.sonar.server.search.QueryContext.MAX_LIMIT;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

/**
 * @since 4.4
 */
public class SearchAction implements RulesWsAction {
  public static final String ACTION = "search";

  public static final String PARAM_REPOSITORIES = "repositories";
  public static final String PARAM_KEY = "rule_key";
  public static final String PARAM_ACTIVATION = "activation";
  public static final String PARAM_QPROFILE = "qprofile";
  public static final String PARAM_SEVERITIES = "severities";
  public static final String PARAM_AVAILABLE_SINCE = "available_since";
  public static final String PARAM_STATUSES = "statuses";
  public static final String PARAM_LANGUAGES = "languages";
  public static final String PARAM_DEBT_CHARACTERISTICS = "debt_characteristics";
  public static final String PARAM_HAS_DEBT_CHARACTERISTIC = "has_debt_characteristic";
  public static final String PARAM_TAGS = "tags";
  public static final String PARAM_INHERITANCE = "inheritance";
  public static final String PARAM_ACTIVE_SEVERITIES = "active_severities";
  public static final String PARAM_IS_TEMPLATE = "is_template";
  public static final String PARAM_TEMPLATE_KEY = "template_key";

  private static final Collection<String> DEFAULT_FACETS = ImmutableSet.of(PARAM_LANGUAGES, PARAM_REPOSITORIES, "tags");

  private final RuleService ruleService;
  private final ActiveRuleCompleter activeRuleCompleter;
  private final RuleMapping mapping;
  private final UserSession userSession;

  public SearchAction(RuleService service, ActiveRuleCompleter activeRuleCompleter, RuleMapping mapping, UserSession userSession) {
    this.userSession = userSession;
    this.ruleService = service;
    this.activeRuleCompleter = activeRuleCompleter;
    this.mapping = mapping;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .addPagingParams(100, MAX_LIMIT)
      .setHandler(this);

    Collection<String> possibleFacets = possibleFacets();
    WebService.NewParam paramFacets = action.createParam(Param.FACETS)
      .setDescription("Comma-separated list of the facets to be computed. No facet is computed by default.")
      .setPossibleValues(possibleFacets);
    if (possibleFacets != null && possibleFacets.size() > 1) {
      Iterator<String> it = possibleFacets.iterator();
      paramFacets.setExampleValue(String.format("%s,%s", it.next(), it.next()));
    }

    Collection<String> possibleFields = possibleFields();
    WebService.NewParam paramFields = action.createParam(Param.FIELDS)
      .setDescription("Comma-separated list of the fields to be returned in response. All the fields are returned by default.")
      .setPossibleValues(possibleFields);
    if (possibleFields != null && possibleFields.size() > 1) {
      Iterator<String> it = possibleFields.iterator();
      paramFields.setExampleValue(String.format("%s,%s", it.next(), it.next()));
    }

    this.doDefinition(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    QueryContext context = getQueryContext(request);
    RuleQuery query = doQuery(request);
    Result<Rule> result = doSearch(query, context);

    SearchResponse responseBuilder = buildResponse(request, context, result);
    writeProtobuf(responseBuilder, request, response);
  }

  private SearchResponse buildResponse(Request request, QueryContext context, Result<Rule> result) {
    SearchResponse.Builder responseBuilder = SearchResponse.newBuilder();
    writeStatistics(responseBuilder, result, context);
    doContextResponse(request, result, responseBuilder);
    if (context.isFacet()) {
      writeFacets(responseBuilder, request, context, result);
    }
    return responseBuilder.build();
  }

  protected void writeStatistics(SearchResponse.Builder response, Result searchResult, QueryContext context) {
    response.setTotal(searchResult.getTotal());
    response.setP(context.getPage());
    response.setPs(context.getLimit());
  }

  protected void doDefinition(WebService.NewAction action) {
    action.setDescription("Search for a collection of relevant rules matching a specified query")
      .setResponseExample(Resources.getResource(getClass(), "example-search.json"))
      .setSince("4.4")
      .setHandler(this);

    // Rule-specific search parameters
    defineRuleSearchParameters(action);
  }

  @CheckForNull
  protected Collection<String> possibleFacets() {
    return Arrays.asList(
      RuleIndex.FACET_LANGUAGES,
      RuleIndex.FACET_REPOSITORIES,
      RuleIndex.FACET_TAGS,
      RuleIndex.FACET_DEBT_CHARACTERISTICS,
      RuleIndex.FACET_SEVERITIES,
      RuleIndex.FACET_ACTIVE_SEVERITIES,
      RuleIndex.FACET_STATUSES,
      RuleIndex.FACET_OLD_DEFAULT
      );
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
      .createParam(PARAM_DEBT_CHARACTERISTICS)
      .setDescription("Comma-separated list of technical debt characteristics or sub-characteristics")
      .setExampleValue("RELIABILITY");

    action
      .createParam(PARAM_HAS_DEBT_CHARACTERISTIC)
      .setDescription("Filter rules that have a technical debt characteristic")
      .setBooleanPossibleValues();

    action
      .createParam(PARAM_TAGS)
      .setDescription("Comma-separated list of tags. Returned rules match any of the tags (OR operator)")
      .setExampleValue("security,java8");

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
      .setPossibleValues(RuleNormalizer.RuleField.NAME.field(),
        RuleNormalizer.RuleField.UPDATED_AT.field(),
        RuleNormalizer.RuleField.CREATED_AT.field(),
        RuleNormalizer.RuleField.KEY.field())
      .setExampleValue(RuleNormalizer.RuleField.NAME.field());

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
    query.setAvailableSince(request.paramAsDate(PARAM_AVAILABLE_SINCE));
    query.setStatuses(request.paramAsEnums(PARAM_STATUSES, RuleStatus.class));
    query.setLanguages(request.paramAsStrings(PARAM_LANGUAGES));
    query.setDebtCharacteristics(request.paramAsStrings(PARAM_DEBT_CHARACTERISTICS));
    query.setHasDebtCharacteristic(request.paramAsBoolean(PARAM_HAS_DEBT_CHARACTERISTIC));
    query.setActivation(request.paramAsBoolean(PARAM_ACTIVATION));
    query.setQProfileKey(request.param(PARAM_QPROFILE));
    query.setTags(request.paramAsStrings(PARAM_TAGS));
    query.setInheritance(request.paramAsStrings(PARAM_INHERITANCE));
    query.setActiveSeverities(request.paramAsStrings(PARAM_ACTIVE_SEVERITIES));
    query.setIsTemplate(request.paramAsBoolean(PARAM_IS_TEMPLATE));
    query.setTemplateKey(request.param(PARAM_TEMPLATE_KEY));
    query.setKey(request.param(PARAM_KEY));

    String sortParam = request.param(Param.SORT);
    if (sortParam != null) {
      query.setSortField(RuleNormalizer.RuleField.of(sortParam));
      query.setAscendingSort(request.mandatoryParamAsBoolean(Param.ASCENDING));
    }
    return query;
  }

  private void writeRules(SearchResponse.Builder response, Result<Rule> result, QueryContext context) {
    for (Rule rule : result.getHits()) {
      response.addRules(mapping.buildRuleResponse(rule, context));
    }
  }

  protected QueryContext getQueryContext(Request request) {
    // TODO Get rid of this horrible hack: fields on request are not the same as fields for ES search ! 1/2
    QueryContext context = loadCommonContext(request);
    QueryContext searchQueryContext = mapping.newQueryOptions(SearchOptions.create(request))
      .setLimit(context.getLimit())
      .setOffset(context.getOffset())
      .setScroll(context.isScroll());
    if (context.facets().contains(RuleIndex.FACET_OLD_DEFAULT)) {
      searchQueryContext.addFacets(DEFAULT_FACETS);
    } else {
      searchQueryContext.addFacets(context.facets());
    }
    return searchQueryContext;
  }

  private QueryContext loadCommonContext(Request request) {
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    QueryContext context = new QueryContext(userSession).addFieldsToReturn(request.paramAsStrings(Param.FIELDS));
    List<String> facets = request.paramAsStrings(Param.FACETS);
    if (facets != null) {
      context.addFacets(facets);
    }
    if (pageSize < 1) {
      context.setPage(request.mandatoryParamAsInt(Param.PAGE), 0).setMaxLimit();
    } else {
      context.setPage(request.mandatoryParamAsInt(Param.PAGE), pageSize);
    }
    return context;
  }

  protected Result<Rule> doSearch(RuleQuery query, QueryContext context) {
    return ruleService.search(query, context);
  }

  protected RuleQuery doQuery(Request request) {
    RuleQuery plainQuery = createRuleQuery(ruleService.newRuleQuery(), request);

    String qProfileKey = request.param(PARAM_QPROFILE);
    if (qProfileKey != null) {
      QualityProfileDto qProfile = activeRuleCompleter.loadProfile(qProfileKey);
      if (qProfile != null) {
        plainQuery.setLanguages(ImmutableList.of(qProfile.getLanguage()));
      }
    }

    return plainQuery;
  }

  protected void doContextResponse(Request request, Result<Rule> result, SearchResponse.Builder response) {
    // TODO Get rid of this horrible hack: fields on request are not the same as fields for ES search ! 2/2
    QueryContext contextForResponse = loadCommonContext(request);
    writeRules(response, result, contextForResponse);
    if (contextForResponse.getFieldsToReturn().contains("actives")) {
      activeRuleCompleter.completeSearch(doQuery(request), result.getHits(), response);
    }
  }

  protected Collection<String> possibleFields() {
    Builder<String> builder = ImmutableList.builder();
    builder.addAll(mapping.supportedFields());
    return builder.add("actives").build();
  }

  protected void writeFacets(SearchResponse.Builder response, Request request, QueryContext context, Result<?> results) {
    addMandatoryFacetValues(results, RuleIndex.FACET_DEBT_CHARACTERISTICS, request.paramAsStrings(PARAM_DEBT_CHARACTERISTICS));
    addMandatoryFacetValues(results, RuleIndex.FACET_LANGUAGES, request.paramAsStrings(PARAM_LANGUAGES));
    addMandatoryFacetValues(results, RuleIndex.FACET_REPOSITORIES, request.paramAsStrings(PARAM_REPOSITORIES));
    addMandatoryFacetValues(results, RuleIndex.FACET_STATUSES, RuleIndex.ALL_STATUSES_EXCEPT_REMOVED);
    addMandatoryFacetValues(results, RuleIndex.FACET_SEVERITIES, Severity.ALL);
    addMandatoryFacetValues(results, RuleIndex.FACET_ACTIVE_SEVERITIES, Severity.ALL);
    addMandatoryFacetValues(results, RuleIndex.FACET_TAGS, request.paramAsStrings(PARAM_TAGS));

    mergeNoneAndEmptyBucketOnCharacteristics(results);

    Common.Facet.Builder facet = Common.Facet.newBuilder();
    Common.FacetValue.Builder value = Common.FacetValue.newBuilder();
    for (String facetName : context.facets()) {
      facet.clear().setProperty(facetName);
      if (results.getFacets().containsKey(facetName)) {
        Set<String> itemsFromFacets = Sets.newHashSet();
        for (FacetValue facetValue : results.getFacets().get(facetName)) {
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

  protected void mergeNoneAndEmptyBucketOnCharacteristics(Result<?> results) {
    if (results.getFacets().containsKey(RuleIndex.FACET_DEBT_CHARACTERISTICS)) {
      Collection<FacetValue> characValues = results.getFacetValues(RuleIndex.FACET_DEBT_CHARACTERISTICS);
      if (characValues == null) {
        return;
      }

      long mergedCount = 0L;
      Iterator<FacetValue> characIterator = characValues.iterator();
      while (characIterator.hasNext()) {
        FacetValue characValue = characIterator.next();
        if ("".equals(characValue.getKey()) || DebtCharacteristic.NONE.equals(characValue.getKey())) {
          mergedCount += characValue.getValue();
          characIterator.remove();
        }
      }

      FacetValue mergedNoneValue = new FacetValue(DebtCharacteristic.NONE, mergedCount);
      characValues.add(mergedNoneValue);
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

  protected void addMandatoryFacetValues(Result<?> results, String facetName, @Nullable List<String> mandatoryValues) {
    Collection<FacetValue> facetValues = results.getFacetValues(facetName);
    if (facetValues != null) {
      Map<String, Long> valuesByItem = Maps.newHashMap();
      for (FacetValue value : facetValues) {
        valuesByItem.put(value.getKey(), value.getValue());
      }
      List<String> valuesToAdd = mandatoryValues == null ? Lists.<String>newArrayList() : mandatoryValues;
      for (String item : valuesToAdd) {
        if (!valuesByItem.containsKey(item)) {
          facetValues.add(new FacetValue(item, 0));
        }
      }
    }
  }
}

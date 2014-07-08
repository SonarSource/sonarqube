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
import com.google.common.io.Resources;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.rule.index.RuleDoc;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.search.ws.SearchOptions;

import java.util.Collection;
import java.util.Map;

/**
 * @since 4.4
 */
public class SearchAction implements RequestHandler {

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
  public static final String PARAM_FACETS = "facets";

  public static final String SEARCH_ACTION = "search";

  private final RuleService ruleService;
  private final ActiveRuleCompleter activeRuleCompleter;
  private final RuleMapping mapping;

  public SearchAction(RuleService service, ActiveRuleCompleter activeRuleCompleter, RuleMapping mapping) {
    this.ruleService = service;
    this.activeRuleCompleter = activeRuleCompleter;
    this.mapping = mapping;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(SEARCH_ACTION)
      .setDescription("Search for a collection of relevant rules matching a specified query")
      .setResponseExample(Resources.getResource(getClass(), "example-search.json"))
      .setSince("4.4")
      .setHandler(this);

    // Generic search parameters
    SearchOptions.defineFieldsParam(action,
      ImmutableList.<String>builder().addAll(mapping.supportedFields()).add("actives").build());
    SearchOptions.definePageParams(action);

    // Rule-specific search parameters
    defineRuleSearchParameters(action);

    // Other parameters
    action.createParam(PARAM_FACETS)
      .setDescription("Compute predefined facets")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
  }

  /**
   * public visibility because used by {@link org.sonar.server.qualityprofile.ws.BulkRuleActivationActions}
   */
  public static void defineRuleSearchParameters(WebService.NewAction action) {
    action
      .createParam(SearchOptions.PARAM_TEXT_QUERY)
      .setDescription("UTF-8 search query")
      .setExampleValue("xpath");

    action
      .createParam(PARAM_KEY)
      .setDescription("Single or list of keys of rule to search for")
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
      .setExampleValue("java:Sonar way");

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
      .createParam(SearchOptions.PARAM_SORT)
      .setDescription("Sort field")
      .setPossibleValues(RuleNormalizer.RuleField.NAME.field(),
        RuleNormalizer.RuleField.UPDATED_AT.field(),
        RuleNormalizer.RuleField.CREATED_AT.field(),
        RuleNormalizer.RuleField.KEY.field())
      .setExampleValue(RuleNormalizer.RuleField.NAME.field());

    action
      .createParam(SearchOptions.PARAM_ASCENDING)
      .setDescription("Ascending sort")
      .setBooleanPossibleValues()
      .setDefaultValue(true);
  }

  @Override
  public void handle(Request request, Response response) {
    RuleQuery query = createRuleQuery(ruleService.newRuleQuery(), request);
    SearchOptions searchOptions = SearchOptions.create(request);
    QueryOptions queryOptions = mapping.newQueryOptions(searchOptions);
    queryOptions.setFacet(request.mandatoryParamAsBoolean(PARAM_FACETS));

    Result<Rule> results = ruleService.search(query, queryOptions);

    JsonWriter json = response.newJsonWriter().beginObject();
    searchOptions.writeStatistics(json, results);
    writeRules(results, json, searchOptions);
    if (searchOptions.hasField("actives")) {
      activeRuleCompleter.completeSearch(query, results.getHits(), json);
    }
    if (queryOptions.isFacet()) {
      writeFacets(results, json);
    }
    json.endObject().close();
  }

  public static RuleQuery createRuleQuery(RuleQuery query, Request request) {
    query.setQueryText(request.param(SearchOptions.PARAM_TEXT_QUERY));
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

    String sortParam = request.param(SearchOptions.PARAM_SORT);
    if (sortParam != null) {
      query.setSortField(RuleNormalizer.RuleField.of(sortParam));
      query.setAscendingSort(request.mandatoryParamAsBoolean(SearchOptions.PARAM_ASCENDING));
    }
    return query;
  }

  private void writeRules(Result<Rule> result, JsonWriter json, SearchOptions options) {
    json.name("rules").beginArray();
    for (Rule rule : result.getHits()) {
      mapping.write((RuleDoc) rule, json, options);
    }
    json.endArray();
  }

  private void writeFacets(Result<Rule> results, JsonWriter json) {
    json.name("facets").beginArray();
    for (Map.Entry<String, Collection<FacetValue>> facet : results.getFacets().entrySet()) {
      json.beginObject();
      json.prop("property", facet.getKey());
      json.name("values").beginArray();
      for (FacetValue facetValue : facet.getValue()) {
        json.beginObject();
        json.prop("val", facetValue.getKey());
        json.prop("count", facetValue.getValue());
        json.endObject();
      }
      json.endArray().endObject();
    }
    json.endArray();
  }
}

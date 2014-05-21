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
package org.sonar.server.rule2.ws;

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
import org.sonar.server.rule2.Rule;
import org.sonar.server.rule2.RuleService;
import org.sonar.server.rule2.index.RuleDoc;
import org.sonar.server.rule2.index.RuleNormalizer;
import org.sonar.server.rule2.index.RuleQuery;
import org.sonar.server.rule2.index.RuleResult;
import org.sonar.server.search.ws.SearchOptions;

import java.util.Collection;
import java.util.Map;

/**
 * @since 4.4
 */
public class SearchAction implements RequestHandler {

  private static final String PARAM_REPOSITORIES = "repositories";
  private static final String PARAM_ACTIVATION = "activation";
  private static final String PARAM_QPROFILE = "qprofile";
  private static final String PARAM_SEVERITIES = "severities";
  private static final String PARAM_STATUSES = "statuses";
  private static final String PARAM_LANGUAGES = "languages";
  private static final String PARAM_DEBT_CHARACTERISTICS = "debt_characteristics";
  private static final String PARAM_HAS_DEBT_CHARACTERISTIC = "has_debt_characteristic";
  private static final String PARAM_TAGS = "tags";
  private static final String PARAM_ALL_OF_TAGS = "all_of_tags";

  private final RuleService service;
  private final RuleMapping mapping;

  public SearchAction(RuleService service, RuleMapping mapping) {
    this.service = service;
    this.mapping = mapping;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("search")
      .setDescription("Search for a collection of relevant rules matching a specified query")
      .setResponseExample(Resources.getResource(getClass(), "example-search.json"))
      .setSince("4.4")
      .setHandler(this);

    SearchOptions.defineFieldsParam(action,
      ImmutableList.<String>builder().addAll(mapping.supportedFields()).add("actives").build());
    SearchOptions.definePageParams(action);
    defineRuleSearchParameters(action);
  }

  /**
   * public visibility because used by {@link org.sonar.server.qualityprofile.ws.BulkRuleActivationActions}
   */
  public static void defineRuleSearchParameters(WebService.NewAction action) {
    action
      .createParam(SearchOptions.PARAM_TEXT_QUERY)
      .setDescription("UTF-8 search query")
      .setExampleValue("null pointer");

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
      .setExampleValue(RuleStatus.READY.toString());

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
      .createParam(PARAM_ALL_OF_TAGS)
      .setDescription("Comma-separated list of tags. Returned rules match all the tags (AND operator)")
      .setExampleValue("security,java8");

    action
      .createParam(PARAM_QPROFILE)
      .setDescription("Key of Quality profile")
      .setExampleValue("java:Sonar way");

    action
      .createParam(PARAM_ACTIVATION)
      .setDescription("Used only if 'qprofile' is set")
      .setExampleValue("java:Sonar way")
      .setPossibleValues("false", "true", "all");

    // TODO limit the fields to sort on + document possible values + default value ?
    action
      .createParam(SearchOptions.PARAM_SORT)
      .setDescription("Sort field")
      .setExampleValue(RuleNormalizer.RuleField.LANGUAGE.key());

    action
      .createParam(SearchOptions.PARAM_ASCENDING)
      .setDescription("Ascending sort")
      .setBooleanPossibleValues()
      .setDefaultValue("true");
  }

  @Override
  public void handle(Request request, Response response) {
    RuleQuery query = createRuleQuery(request);
    SearchOptions searchOptions = SearchOptions.create(request);

    RuleResult results = service.search(query, mapping.newQueryOptions(searchOptions));

    JsonWriter json = response.newJsonWriter().beginObject();
    searchOptions.writeStatistics(json, results);
    writeRules(results, json, searchOptions);
    if (searchOptions.hasField("actives")) {
      writeActiveRules(results, json);
    }
    json.endObject();
    json.close();
  }

  private RuleQuery createRuleQuery(Request request) {
    RuleQuery query = service.newRuleQuery();
    query.setQueryText(request.param(SearchOptions.PARAM_TEXT_QUERY));
    query.setSeverities(request.paramAsStrings(PARAM_SEVERITIES));
    query.setRepositories(request.paramAsStrings(PARAM_REPOSITORIES));
    query.setStatuses(request.paramAsEnums(PARAM_STATUSES, RuleStatus.class));
    query.setLanguages(request.paramAsStrings(PARAM_LANGUAGES));
    query.setDebtCharacteristics(request.paramAsStrings(PARAM_DEBT_CHARACTERISTICS));
    query.setHasDebtCharacteristic(request.paramAsBoolean(PARAM_HAS_DEBT_CHARACTERISTIC));
    query.setActivation(request.param(PARAM_ACTIVATION));
    query.setQProfileKey(request.param(PARAM_QPROFILE));
    query.setSortField(RuleQuery.SortField.valueOfOrNull(request.param(SearchOptions.PARAM_SORT)));
    query.setAscendingSort(request.mandatoryParamAsBoolean(SearchOptions.PARAM_ASCENDING));
    query.setTags(request.paramAsStrings(PARAM_TAGS));
    return query;
  }

  private void writeRules(RuleResult result, JsonWriter json, SearchOptions options) {
    json.name("rules").beginArray();
    for (Rule rule : result.getHits()) {
      mapping.write((RuleDoc) rule, json, options);
    }
    json.endArray();
  }

  private void writeActiveRules(RuleResult result, JsonWriter json) {
    json.name("actives").beginObject();
    for (Map.Entry<String, Collection<ActiveRule>> entry : result.getActiveRules().asMap().entrySet()) {
      // rule key
      json.name(entry.getKey());
      json.beginArray();
      for (ActiveRule activeRule : entry.getValue()) {
        json
          .beginObject()
          .prop("qProfile", activeRule.key().qProfile().toString())
          .prop("inherit", activeRule.inheritance().toString())
          .prop("severity", activeRule.severity());
        if (activeRule.parentKey() != null) {
          json.prop("parent", activeRule.parentKey().toString());
        }
        json.name("params").beginArray();
        for (Map.Entry<String, String> param : activeRule.params().entrySet()) {
          json
            .beginObject()
            .prop("key", param.getKey())
            .prop("value", param.getValue())
            .endObject();
        }
        json.endArray().endObject();
      }
      json.endArray();
    }
    json.endObject();
  }
}

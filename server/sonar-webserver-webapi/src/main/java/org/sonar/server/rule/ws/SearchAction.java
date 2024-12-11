/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.CleanCodeAttributeCategory;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.rule.ws.RulesResponseFormatter.SearchResult;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.SearchResponse;
import org.sonarqube.ws.client.issue.IssuesWsParameters;

import static java.lang.String.format;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.INFO;
import static org.sonar.api.server.ws.WebService.Param.ASCENDING;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.api.server.ws.WebService.Param.FIELDS;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.rule.index.RuleIndex.ALL_STATUSES_EXCEPT_REMOVED;
import static org.sonar.server.rule.index.RuleIndex.FACET_ACTIVE_IMPACT_SEVERITY;
import static org.sonar.server.rule.index.RuleIndex.FACET_ACTIVE_SEVERITIES;
import static org.sonar.server.rule.index.RuleIndex.FACET_CLEAN_CODE_ATTRIBUTE_CATEGORY;
import static org.sonar.server.rule.index.RuleIndex.FACET_CWE;
import static org.sonar.server.rule.index.RuleIndex.FACET_IMPACT_SEVERITY;
import static org.sonar.server.rule.index.RuleIndex.FACET_IMPACT_SOFTWARE_QUALITY;
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
import static org.sonar.server.rule.ws.RulesWsParameters.OPTIONAL_FIELDS;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVE_IMPACT_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_CWE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_IMPACT_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_IMPACT_SOFTWARE_QUALITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_OWASP_TOP_10;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_OWASP_TOP_10_2021;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_PRIORITIZED_RULE;
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

  private static final Collection<String> DEFAULT_FACETS = Set.of(PARAM_LANGUAGES, PARAM_REPOSITORIES, "tags");
  private static final String[] POSSIBLE_FACETS = new String[] {
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
    FACET_SONARSOURCE_SECURITY,
    FACET_CLEAN_CODE_ATTRIBUTE_CATEGORY,
    FACET_IMPACT_SEVERITY,
    FACET_IMPACT_SOFTWARE_QUALITY,
    FACET_ACTIVE_IMPACT_SEVERITY
  };

  private final RuleQueryFactory ruleQueryFactory;
  private final DbClient dbClient;
  private final RuleIndex ruleIndex;
  private final RulesResponseFormatter rulesResponseFormatter;

  public SearchAction(RuleIndex ruleIndex, RuleQueryFactory ruleQueryFactory, DbClient dbClient, RulesResponseFormatter rulesResponseFormatter) {
    this.ruleIndex = ruleIndex;
    this.ruleQueryFactory = ruleQueryFactory;
    this.dbClient = dbClient;
    this.rulesResponseFormatter = rulesResponseFormatter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .addPagingParams(100, MAX_PAGE_SIZE)
      .setHandler(this)
      .setChangelog(
        new Change("5.5", "The field 'effortToFixDescription' has been deprecated, use 'gapDescription' instead"),
        new Change("5.5", "The field 'debtRemFnCoeff' has been deprecated, use 'remFnGapMultiplier' instead"),
        new Change("5.5", "The field 'defaultDebtRemFnCoeff' has been deprecated, use 'defaultRemFnGapMultiplier' instead"),
        new Change("5.5", "The field 'debtRemFnOffset' has been deprecated, use 'remFnBaseEffort' instead"),
        new Change("5.5", "The field 'defaultDebtRemFnOffset' has been deprecated, use 'defaultRemFnBaseEffort' instead"),
        new Change("5.5", "The field 'debtOverloaded' has been deprecated, use 'remFnOverloaded' instead"),
        new Change("7.1", "The field 'scope' has been added to the response"),
        new Change("7.1", "The field 'scope' has been added to the 'f' parameter"),
        new Change("7.2", "The field 'isExternal' has been added to the response"),
        new Change("7.2", "The field 'includeExternal' has been added to the 'f' parameter"),
        new Change("7.5", "The field 'updatedAt' has been added to the 'f' parameter"),
        new Change("9.5", "The field 'htmlDesc' has been deprecated, use 'descriptionSections' instead"),
        new Change("9.5", "The field 'descriptionSections' has been added to the payload"),
        new Change("9.5", "The field 'descriptionSections' has been added to the 'f' parameter"),
        new Change("9.6", "'descriptionSections' can optionally embed a context field"),
        new Change("9.6", "The field 'educationPrinciples' has been added to the 'f' parameter"),
        new Change("9.8", "response fields 'total', 's', 'ps' have been deprecated, please use 'paging' object instead"),
        new Change("9.8", "The field 'paging' has been added to the response"),
        new Change("10.0", "The deprecated field 'effortToFixDescription' has been removed, use 'gapDescription' instead."),
        new Change("10.0", "The deprecated field 'debtRemFnCoeff' has been removed, use 'remFnGapMultiplier' instead."),
        new Change("10.0", "The deprecated field 'defaultDebtRemFnCoeff' has been removed, use 'defaultRemFnGapMultiplier' instead."),
        new Change("10.0", "The deprecated field 'debtRemFnOffset' has been removed, use 'remFnBaseEffort' instead."),
        new Change("10.0", "The deprecated field 'defaultDebtRemFnOffset' has been removed, use 'defaultRemFnBaseEffort' instead."),
        new Change("10.0", "The deprecated field 'debtOverloaded' has been removed, use 'remFnOverloaded' instead."),
        new Change("10.0", "The field 'defaultDebtRemFnType' has been deprecated, use 'defaultRemFnType' instead"),
        new Change("10.0", "The field 'debtRemFnType' has been deprecated, use 'remFnType' instead"),
        new Change("10.0", "The value 'debtRemFn' for the 'f' parameter has been deprecated, use 'remFn' instead"),
        new Change("10.0", "The value 'defaultDebtRemFn' for the 'f' parameter has been deprecated, use 'defaultRemFn' instead"),
        new Change("10.0", "The value 'sansTop25' for the parameter 'facets' has been deprecated"),
        new Change("10.0", "Parameter 'sansTop25' is deprecated"),
        new Change("10.2", "Add 'impacts', 'cleanCodeAttribute', 'cleanCodeAttributeCategory' fields to the response"),
        new Change("10.2", "The fields 'type' and 'severity' are deprecated in the response. Use 'impacts' instead."),
        new Change("10.2", "The field 'cleanCodeAttribute' has been added to the 'f' parameter."),
        new Change("10.2", "The value 'severity' for the 'f' parameter has been deprecated."),
        new Change("10.2",
          format("The values '%s', '%s' and '%s' have been added to the 'facets' parameter.", FACET_CLEAN_CODE_ATTRIBUTE_CATEGORY, FACET_IMPACT_SOFTWARE_QUALITY,
            FACET_IMPACT_SEVERITY)),
        new Change("10.2", format("The values 'severity' and 'types' for the 'facets' parameter have been deprecated. Use '%s' and '%s' instead.", FACET_IMPACT_SEVERITY,
          FACET_IMPACT_SOFTWARE_QUALITY)),
        new Change("10.2",
          format("Parameters '%s', '%s', and '%s' are now deprecated. Use '%s' and '%s' instead.", PARAM_SEVERITIES, PARAM_TYPES, PARAM_ACTIVE_SEVERITIES,
            PARAM_IMPACT_SOFTWARE_QUALITIES, PARAM_IMPACT_SEVERITIES)),
        new Change("10.6", format("Parameter '%s has been added", PARAM_PRIORITIZED_RULE)),
        new Change("10.8", format("Possible values '%s' and '%s' for response field 'impactSeverities' of 'facets' have been added", INFO.name(), BLOCKER.name())),
        new Change("10.8", format("Possible values '%s' and '%s' for response field 'severity' of 'impacts' have been added", INFO.name(), BLOCKER.name())),
        new Change("10.8", format("Parameter '%s' now supports values: '%s','%s'", IssuesWsParameters.PARAM_SEVERITIES, INFO.name(), BLOCKER.name())),
        new Change("10.8", "The field 'impacts' has been added to the response"),
        new Change("10.8", format("The parameters '%s','%s and '%s' are not deprecated anymore.", PARAM_SEVERITIES, PARAM_TYPES, PARAM_ACTIVE_SEVERITIES)),
        new Change("10.8", "The values 'severity' and 'types' for the 'facets' parameter are not deprecated anymore."),
        new Change("10.8", "The fields 'type' and 'severity' in the response are not deprecated anymore."),
        new Change("10.8", "The value 'severity' for the 'f' parameter is not deprecated anymore."),
        new Change("2025.2", format("The facet '%s' has been added.", FACET_ACTIVE_IMPACT_SEVERITY)));

    action.createParam(FACETS)
      .setDescription("Comma-separated list of the facets to be computed. No facet is computed by default.")
      .setPossibleValues(POSSIBLE_FACETS)
      .setExampleValue(format("%s,%s", POSSIBLE_FACETS[0], POSSIBLE_FACETS[1]));

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
    RuleWsSupport.definePrioritizedRuleParam(action);
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
    response.setTotal(searchResult.getTotal());
    response.setP(context.getPage());
    response.setPs(context.getLimit());
    response.setPaging(formatPaging(searchResult.getTotal(), context.getPage(), context.getLimit()));
  }

  private static Common.Paging.Builder formatPaging(Long total, int pageIndex, int limit) {
    return Common.Paging.newBuilder()
      .setPageIndex(pageIndex)
      .setPageSize(limit)
      .setTotal(total.intValue());
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
    Map<String, RuleDto> rulesByRuleKey = Maps.uniqueIndex(dbClient.ruleDao().selectByUuids(dbSession, ruleUuids), RuleDto::getUuid);
    List<RuleDto> rules = new LinkedList<>();
    for (String ruleUuid : ruleUuids) {
      RuleDto rule = rulesByRuleKey.get(ruleUuid);
      if (rule != null) {
        rules.add(rule);
      }
    }

    List<String> templateRuleUuids = rules.stream()
      .map(RuleDto::getTemplateUuid)
      .filter(Objects::nonNull)
      .toList();
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
    response.addAllRules(rulesResponseFormatter.formatRulesSearch(dbSession, result, contextForResponse.getFields()));
    if (contextForResponse.getFields().contains("actives")) {
      Rules.Actives actives = rulesResponseFormatter.formatActiveRules(dbSession, query.getQProfile(), result.getRules());
      Set<String> qProfiles = actives.getActivesMap().values()
        .stream()
        .map(Rules.ActiveList::getActiveListList)
        .flatMap(List::stream)
        .map(Rules.Active::getQProfile)
        .collect(Collectors.toSet());

      Rules.QProfiles profiles = rulesResponseFormatter.formatQualityProfiles(dbSession, qProfiles);
      response.setActives(actives);
      response.setQProfiles(profiles);
    }
  }

  private static void writeFacets(SearchResponse.Builder response, SearchRequest request, SearchOptions context, SearchResult results) {
    Facets resultsFacets = results.getFacets();
    if (resultsFacets == null) {
      return;
    }
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
    addMandatoryFacetValues(results, PARAM_IMPACT_SOFTWARE_QUALITIES, enumToStringCollection(SoftwareQuality.values()));
    addMandatoryFacetValues(results, PARAM_IMPACT_SEVERITIES, enumToStringCollection(org.sonar.api.issue.impact.Severity.values()));
    addMandatoryFacetValues(results, PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES, enumToStringCollection(CleanCodeAttributeCategory.values()));

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
    facetValuesByFacetKey.put(FACET_CLEAN_CODE_ATTRIBUTE_CATEGORY, request.getCleanCodeAttributesCategories());
    facetValuesByFacetKey.put(FACET_IMPACT_SOFTWARE_QUALITY, request.getImpactSoftwareQualities());
    facetValuesByFacetKey.put(FACET_IMPACT_SEVERITY, request.getImpactSeverities());
    facetValuesByFacetKey.put(FACET_ACTIVE_IMPACT_SEVERITY, request.getActiveImpactSeverities());

    for (String facetName : context.getFacets()) {
      facet.clear().setProperty(facetName);
      Map<String, Long> facets = resultsFacets.get(facetName);
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

  private static Collection<String> enumToStringCollection(Enum<?>... enumValues) {
    return Arrays.stream(enumValues).map(Enum::name).toList();
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
    Facets facets = results.getFacets();
    if (facets == null) {
      return;
    }
    Map<String, Long> facetValues = facets.get(facetName);
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
      .setImpactSeverities(request.paramAsStrings(PARAM_IMPACT_SEVERITIES))
      .setImpactSoftwareQualities(request.paramAsStrings(PARAM_IMPACT_SOFTWARE_QUALITIES))
      .setCleanCodeAttributesCategories(request.paramAsStrings(PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES))
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
      .setSonarsourceSecurity(request.paramAsStrings(PARAM_SONARSOURCE_SECURITY))
      .setPrioritizedRule(request.paramAsBoolean(PARAM_PRIORITIZED_RULE))
      .setActiveImpactSeverities(request.paramAsStrings(PARAM_ACTIVE_IMPACT_SEVERITIES));
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
    private List<String> impactSeverities;
    private List<String> impactSoftwareQualities;
    private List<String> cleanCodeAttributesCategories;
    private List<String> activeImpactSeverities;
    private Boolean prioritizedRule;

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

    /**
     * @deprecated SansTop25 report is outdated, it has been completely deprecated in version 10.0 and will be removed from version 11.0
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public List<String> getSansTop25() {
      return sansTop25;
    }

    @Deprecated(since = "10.0", forRemoval = true)
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

    public List<String> getImpactSeverities() {
      return impactSeverities;
    }

    public SearchRequest setImpactSeverities(@Nullable List<String> impactSeverities) {
      this.impactSeverities = impactSeverities;
      return this;
    }

    public List<String> getImpactSoftwareQualities() {
      return impactSoftwareQualities;
    }

    public SearchRequest setImpactSoftwareQualities(@Nullable List<String> impactSoftwareQualities) {
      this.impactSoftwareQualities = impactSoftwareQualities;
      return this;
    }

    public List<String> getCleanCodeAttributesCategories() {
      return cleanCodeAttributesCategories;
    }

    public SearchRequest setCleanCodeAttributesCategories(@Nullable List<String> cleanCodeAttributesCategories) {
      this.cleanCodeAttributesCategories = cleanCodeAttributesCategories;
      return this;
    }

    @CheckForNull
    public Boolean getPrioritizedRule() {
      return prioritizedRule;
    }

    public SearchRequest setPrioritizedRule(@Nullable Boolean prioritizedRule) {
      this.prioritizedRule = prioritizedRule;
      return this;
    }

    public SearchRequest setActiveImpactSeverities(@Nullable List<String> activeImpactSeverities) {
      this.activeImpactSeverities = activeImpactSeverities;
      return this;
    }

    @CheckForNull
    public List<String> getActiveImpactSeverities() {
      return activeImpactSeverities;
    }
  }
}

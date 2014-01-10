/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.rule;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.ServerExtension;
import org.sonar.api.rules.Rule;
import org.sonar.server.qualityprofile.Paging;
import org.sonar.server.qualityprofile.PagingResult;
import org.sonar.server.qualityprofile.QProfileRule;
import org.sonar.server.qualityprofile.QProfileRuleResult;
import org.sonar.server.search.SearchIndex;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.sonar.server.rule.RuleRegistry.*;

public class ProfileRules implements ServerExtension {

  private static final String FIELD_PARENT = "_parent";
  private static final String FIELD_SOURCE = "_source";

  private final SearchIndex index;

  public ProfileRules(SearchIndex index) {
    this.index = index;
  }

  public QProfileRule getFromActiveRuleId(int activeRuleId) {
    GetResponse activeRuleResponse = index.client().prepareGet(INDEX_RULES, TYPE_ACTIVE_RULE, Integer.toString(activeRuleId))
      .setFields(FIELD_SOURCE, FIELD_PARENT)
      .execute().actionGet();
    Map<String, Object> activeRuleSource = activeRuleResponse.getSourceAsMap();
    Map<String, Object> ruleSource = index.client().prepareGet(INDEX_RULES, TYPE_RULE, (String) activeRuleResponse.getField(FIELD_PARENT).getValue())
      .execute().actionGet().getSourceAsMap();
    return new QProfileRule(ruleSource, activeRuleSource);
  }

  public QProfileRule getFromRuleId(Integer ruleId) {
    Map<String, Object> ruleSource = index.client().prepareGet(INDEX_RULES, TYPE_RULE, Integer.toString(ruleId))
      .execute().actionGet().getSourceAsMap();
    return new QProfileRule(ruleSource);
  }

  public QProfileRuleResult searchProfileRules(ProfileRuleQuery query, Paging paging) {
    SearchRequestBuilder builder = index.client().prepareSearch(INDEX_RULES).setTypes(TYPE_RULE)
      .setPostFilter(ruleFilter(query)
        .must(hasChildFilter(TYPE_ACTIVE_RULE, childFilter(query))))
      .setSize(paging.pageSize())
      .setFrom(paging.offset());
    addOrder(query, builder);
    SearchHits ruleHits = index.executeRequest(builder);

    List<Integer> ruleIds = Lists.newArrayList();
    for (SearchHit ruleHit: ruleHits) {
      ruleIds.add(Integer.valueOf(ruleHit.id()));
    }

    List<QProfileRule> result = Lists.newArrayList();

    if (!ruleIds.isEmpty()) {
      SearchRequestBuilder activeRuleBuilder = index.client().prepareSearch(INDEX_RULES).setTypes(TYPE_ACTIVE_RULE)
        .setPostFilter(boolFilter()
          .must(
            termFilter(ActiveRuleDocument.FIELD_PROFILE_ID, query.profileId()),
            hasParentFilter(TYPE_RULE, termsFilter(RuleDocument.FIELD_ID, ruleIds))
          ))
        .addFields(FIELD_SOURCE, FIELD_PARENT)
        .setSize(ruleHits.getHits().length);
      SearchHits activeRuleHits = index.executeRequest(activeRuleBuilder);

      Map<String, SearchHit> activeRuleByParent = Maps.newHashMap();
      for (SearchHit activeRuleHit: activeRuleHits) {
        activeRuleByParent.put((String) activeRuleHit.field(FIELD_PARENT).getValue(), activeRuleHit);
      }

      for (SearchHit ruleHit: ruleHits) {
        result.add(new QProfileRule(ruleHit.sourceAsMap(), activeRuleByParent.get(ruleHit.id()).sourceAsMap()));
      }
    }
    return new QProfileRuleResult(result, PagingResult.create(paging.pageSize(), paging.pageIndex(), ruleHits.getTotalHits()));
  }

  // FIXME Due to a bug in E/S, As the query filter contain a filter with has_parent, nothing will be returned
  public List<Integer> searchProfileRuleIds(ProfileRuleQuery query) {
    BoolFilterBuilder filter = activeRuleFilter(query);

    SearchRequestBuilder builder = index.client()
      .prepareSearch(INDEX_RULES)
      .setTypes(TYPE_ACTIVE_RULE)
      .setPostFilter(filter);
    List<String> documentIds = index.findDocumentIds(builder, 2);
    return newArrayList(Iterables.transform(documentIds, new Function<String, Integer>() {
      @Override
      public Integer apply(String input) {
        return Integer.valueOf(input);
      }
    }));
  }

  public QProfileRuleResult searchInactiveProfileRules(ProfileRuleQuery query, Paging paging) {
    BoolFilterBuilder filter = ruleFilter(query);
    addMustTermOrTerms(filter, RuleDocument.FIELD_SEVERITY, query.severities());
    filter.mustNot(
      hasChildFilter(TYPE_ACTIVE_RULE,
        termFilter(ActiveRuleDocument.FIELD_PROFILE_ID, query.profileId())));

    SearchRequestBuilder builder = index.client().prepareSearch(INDEX_RULES).setTypes(TYPE_RULE)
      .setPostFilter(filter)
      .addFields(FIELD_SOURCE, FIELD_PARENT)
      .setSize(paging.pageSize())
      .setFrom(paging.offset());
    addOrder(query, builder);

    SearchHits hits = index.executeRequest(builder);
    List<QProfileRule> result = Lists.newArrayList();
    for (SearchHit hit : hits.getHits()) {
      result.add(new QProfileRule(hit.sourceAsMap()));
    }
    return new QProfileRuleResult(result, PagingResult.create(paging.pageSize(), paging.pageIndex(), hits.getTotalHits()));
  }

  protected BoolFilterBuilder childFilter(ProfileRuleQuery query) {
    BoolFilterBuilder filter = boolFilter().must(
      termFilter(ActiveRuleDocument.FIELD_PROFILE_ID, query.profileId())
    );
    addMustTermOrTerms(filter, ActiveRuleDocument.FIELD_SEVERITY, query.severities());
    String inheritance = query.inheritance();
    if (inheritance != null) {
      addMustTermOrTerms(filter, ActiveRuleDocument.FIELD_INHERITANCE, newArrayList(inheritance));
    } else if (query.noInheritance()) {
      filter.mustNot(getTermOrTerms(ActiveRuleDocument.FIELD_INHERITANCE, newArrayList(QProfileRule.INHERITED, QProfileRule.OVERRIDES)));
    }
    return filter;
  }

  protected BoolFilterBuilder activeRuleFilter(ProfileRuleQuery query) {
    BoolFilterBuilder filter = boolFilter().must(
      termFilter(ActiveRuleDocument.FIELD_PROFILE_ID, query.profileId()),
      hasParentFilter(TYPE_RULE, ruleFilter(query))
    );
    addMustTermOrTerms(filter, ActiveRuleDocument.FIELD_SEVERITY, query.severities());
    String inheritance = query.inheritance();
    if (inheritance != null) {
      addMustTermOrTerms(filter, ActiveRuleDocument.FIELD_INHERITANCE, newArrayList(inheritance));
    } else if (query.noInheritance()) {
      filter.mustNot(getTermOrTerms(ActiveRuleDocument.FIELD_INHERITANCE, newArrayList(QProfileRule.INHERITED, QProfileRule.OVERRIDES)));
    }
    return filter;
  }

  public long countProfileRules(ProfileRuleQuery query) {
    return index.executeCount(
      index.client()
        .prepareCount(INDEX_RULES)
        .setTypes(TYPE_ACTIVE_RULE)
        .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), activeRuleFilter(query)))
    );
  }

  public long countInactiveProfileRules(ProfileRuleQuery query) {
    return index.executeCount(index.client().prepareCount(INDEX_RULES).setTypes(TYPE_RULE)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
        boolFilter()
          .must(ruleFilter(query))
          .mustNot(hasChildFilter(TYPE_ACTIVE_RULE, termFilter(ActiveRuleDocument.FIELD_PROFILE_ID, query.profileId()))))));
  }

  private BoolFilterBuilder ruleFilter(ProfileRuleQuery query) {
    BoolFilterBuilder result = boolFilter();

    if (StringUtils.isNotBlank(query.language())) {
      result.must(termFilter(RuleDocument.FIELD_LANGUAGE, query.language()));
    }

    addMustTermOrTerms(result, RuleDocument.FIELD_REPOSITORY_KEY, query.repositoryKeys());
    if (query.statuses().isEmpty()) {
      result.mustNot(termFilter(RuleDocument.FIELD_STATUS, Rule.STATUS_REMOVED));
    } else {
      addMustTermOrTerms(result, RuleDocument.FIELD_STATUS, query.statuses());
    }

    if (StringUtils.isNotBlank(query.nameOrKey())) {
      result.must(
        queryFilter(
          multiMatchQuery(query.nameOrKey(), RuleDocument.FIELD_NAME, RuleDocument.FIELD_KEY)
            .operator(Operator.AND)));
    }

    return result;
  }

  private void addMustTermOrTerms(BoolFilterBuilder filter, String field, Collection<String> terms) {
    FilterBuilder termOrTerms = getTermOrTerms(field, terms);
    if (termOrTerms != null) {
      filter.must(termOrTerms);
    }
  }

  private FilterBuilder getTermOrTerms(String field, Collection<String> terms) {
    if (terms.isEmpty()) {
      return null;
    } else {
      if (terms.size() == 1) {
        return termFilter(field, terms.iterator().next());
      } else {
        return termsFilter(field, terms.toArray());
      }
    }
  }

  private void addOrder(ProfileRuleQuery query, SearchRequestBuilder builder) {
    SortOrder sortOrder = query.asc() ? SortOrder.ASC : SortOrder.DESC;
    if (query.sort().equals(ProfileRuleQuery.SORT_BY_RULE_NAME)) {
      builder.addSort(RuleDocument.FIELD_NAME + ".raw", sortOrder);
    } else if (query.sort().equals(ProfileRuleQuery.SORT_BY_CREATION_DATE)) {
      builder.addSort(RuleDocument.FIELD_CREATED_AT, sortOrder);
    }
  }
}

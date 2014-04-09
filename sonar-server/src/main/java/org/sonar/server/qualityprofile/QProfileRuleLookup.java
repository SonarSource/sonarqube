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
package org.sonar.server.qualityprofile;

import org.sonar.server.paging.Paging;
import org.sonar.server.paging.PagingResult;

import com.google.common.annotations.VisibleForTesting;
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
import org.sonar.server.es.ESIndex;
import org.sonar.server.rule.RuleDocument;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.sonar.server.rule.RuleRegistry.INDEX_RULES;
import static org.sonar.server.rule.RuleRegistry.TYPE_RULE;

public class QProfileRuleLookup implements ServerExtension {

  private static final int PAGE_SIZE = 100;

  private static final String FIELD_PARENT = "_parent";
  private static final String FIELD_SOURCE = "_source";

  private final ESIndex index;

  public QProfileRuleLookup(ESIndex index) {
    this.index = index;
  }

  @CheckForNull
  public QProfileRule findByActiveRuleId(int activeRuleId) {
    GetResponse activeRuleResponse = index.client().prepareGet(INDEX_RULES, ESActiveRule.TYPE_ACTIVE_RULE, Integer.toString(activeRuleId))
      .setFields(FIELD_SOURCE, FIELD_PARENT)
      .execute().actionGet();
    Map<String, Object> activeRuleSource = activeRuleResponse.getSourceAsMap();
    if (activeRuleSource != null) {
      Map<String, Object> ruleSource = index.client().prepareGet(INDEX_RULES, TYPE_RULE, (String) activeRuleResponse.getField(FIELD_PARENT).getValue())
        .execute().actionGet().getSourceAsMap();
      if (ruleSource != null) {
        return new QProfileRule(ruleSource, activeRuleSource);
      }
    }
    return null;
  }

  @CheckForNull
  public QProfileRule findByProfileIdAndRuleId(int profileId, int ruleId) {
    Map<String, Object> ruleSource = index.client().prepareGet(INDEX_RULES, TYPE_RULE, Integer.toString(ruleId))
      .execute().actionGet().getSourceAsMap();
    if (ruleSource != null) {
      SearchHits activeRuleHits = searchActiveRules(ProfileRuleQuery.create(profileId), newArrayList(ruleId), FIELD_SOURCE, FIELD_PARENT);
      long resultSize = activeRuleHits.totalHits();
      if (resultSize > 0) {
        if (resultSize == 1) {
          return new QProfileRule(ruleSource, activeRuleHits.getAt(0).sourceAsMap());
        } else {
          throw new IllegalStateException("There is more than one result.");
        }
      }
    }
    return null;
  }

  @CheckForNull
  public QProfileRule findByRuleId(int ruleId) {
    Map<String, Object> ruleSource = index.client().prepareGet(INDEX_RULES, TYPE_RULE, Integer.toString(ruleId))
      .execute().actionGet().getSourceAsMap();
    if (ruleSource != null) {
      return new QProfileRule(ruleSource);
    }
    return null;
  }

  @CheckForNull
  public QProfileRule findParentProfileRule(QProfileRule rule) {
    Integer parentId = rule.activeRuleParentId();
    if (parentId != null) {
      return findByActiveRuleId(parentId);
    }
    return null;
  }

  public QProfileRuleResult search(ProfileRuleQuery query, Paging paging) {
    SearchHits ruleHits = searchRules(query, paging, ruleFilterForActiveRuleSearch(query).must(hasChildFilter(ESActiveRule.TYPE_ACTIVE_RULE, activeRuleFilter(query))));
    List<Integer> ruleIds = Lists.newArrayList();
    for (SearchHit ruleHit : ruleHits) {
      ruleIds.add(Integer.valueOf(ruleHit.id()));
    }

    List<QProfileRule> result = Lists.newArrayList();
    if (!ruleIds.isEmpty()) {
      SearchHits activeRuleHits = searchActiveRules(query, ruleIds, FIELD_SOURCE, FIELD_PARENT);

      Map<String, SearchHit> activeRuleByParent = Maps.newHashMap();
      for (SearchHit activeRuleHit : activeRuleHits) {
        activeRuleByParent.put((String) activeRuleHit.field(FIELD_PARENT).getValue(), activeRuleHit);
      }

      for (SearchHit ruleHit : ruleHits) {
        result.add(new QProfileRule(ruleHit.sourceAsMap(), activeRuleByParent.get(ruleHit.id()).sourceAsMap()));
      }
    }
    return new QProfileRuleResult(result, PagingResult.create(paging.pageSize(), paging.pageIndex(), ruleHits.getTotalHits()));
  }

  public List<Integer> searchProfileRuleIds(final ProfileRuleQuery query) {
    return searchProfileRuleIds(query, PAGE_SIZE);
  }

  @VisibleForTesting
  List<Integer> searchProfileRuleIds(final ProfileRuleQuery query, int pageSize) {
    final List<Integer> activeRuleIds = newArrayList();
    new Search(pageSize) {
      @Override
      public int search(int currentPage) {
        Paging paging = Paging.create(pageSize, currentPage);
        SearchHits ruleHits = searchRules(query, paging, ruleFilterForActiveRuleSearch(query).must(hasChildFilter(ESActiveRule.TYPE_ACTIVE_RULE, activeRuleFilter(query))));
        List<Integer> ruleIds = Lists.newArrayList();
        for (SearchHit ruleHit : ruleHits) {
          ruleIds.add(Integer.valueOf(ruleHit.id()));
        }

        if (!ruleIds.isEmpty()) {
          SearchHits activeRuleHits = searchActiveRules(query, ruleIds, ActiveRuleDocument.FIELD_ID);
          for (SearchHit activeRuleHit : activeRuleHits) {
            activeRuleIds.add((Integer) activeRuleHit.field(ActiveRuleDocument.FIELD_ID).getValue());
          }
        }
        return ruleHits.getHits().length;
      }
    }.execute();
    return activeRuleIds;
  }

  public long countProfileRules(ProfileRuleQuery query) {
    return index.executeCount(
      index.client()
        .prepareCount(INDEX_RULES)
        .setTypes(ESActiveRule.TYPE_ACTIVE_RULE)
        .setQuery(QueryBuilders.filteredQuery(
          QueryBuilders.matchAllQuery(),
          activeRuleFilter(query).must(hasParentFilter(TYPE_RULE, ruleFilterForActiveRuleSearch(query))))
        )
    );
  }

  public long countProfileRules(int ruleId) {
    return index.executeCount(
      index.client()
        .prepareCount(INDEX_RULES)
        .setTypes(ESActiveRule.TYPE_ACTIVE_RULE)
        .setQuery(QueryBuilders.filteredQuery(
          QueryBuilders.matchAllQuery(),
          boolFilter().must(hasParentFilter(TYPE_RULE, boolFilter().must(termFilter(RuleDocument.FIELD_ID, ruleId))))
        )
    ));
  }

  public QProfileRuleResult searchInactives(ProfileRuleQuery query, Paging paging) {
    SearchHits hits = searchRules(query, paging, ruleFilterForInactiveRuleSearch(query), FIELD_SOURCE, FIELD_PARENT);
    List<QProfileRule> result = Lists.newArrayList();
    for (SearchHit hit : hits.getHits()) {
      result.add(new QProfileRule(hit.sourceAsMap()));
    }
    return new QProfileRuleResult(result, PagingResult.create(paging.pageSize(), paging.pageIndex(), hits.getTotalHits()));
  }

  public List<Integer> searchInactiveProfileRuleIds(final ProfileRuleQuery query) {
    final List<Integer> ruleIds = newArrayList();

    new Search(PAGE_SIZE) {
      @Override
      public int search(int currentPage) {
        Paging paging = Paging.create(pageSize, currentPage);
        SearchHits hits = searchRules(query, paging, ruleFilterForInactiveRuleSearch(query), RuleDocument.FIELD_ID);
        for (SearchHit hit : hits.getHits()) {
          ruleIds.add((Integer) hit.field(RuleDocument.FIELD_ID).getValue());
        }
        return hits.getHits().length;
      }
    }.execute();

    return ruleIds;
  }

  public long countInactiveProfileRules(ProfileRuleQuery query) {
    return index.executeCount(index.client().prepareCount(INDEX_RULES).setTypes(TYPE_RULE)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
        ruleFilterForInactiveRuleSearch(query))));
  }

  private SearchHits searchRules(ProfileRuleQuery query, Paging paging, FilterBuilder filterBuilder, String... fields) {
    SearchRequestBuilder builder = index.client().prepareSearch(INDEX_RULES).setTypes(TYPE_RULE)
      .setPostFilter(filterBuilder)
      .setSize(paging.pageSize())
      .setFrom(paging.offset());
    if (fields.length > 0) {
      builder.addFields(fields);
    }
    addOrder(query, builder);
    return index.executeRequest(builder);
  }

  private SearchHits searchActiveRules(ProfileRuleQuery query, List<Integer> ruleIds, String... fields) {
    SearchRequestBuilder activeRuleBuilder = index.client().prepareSearch(INDEX_RULES).setTypes(ESActiveRule.TYPE_ACTIVE_RULE)
      .setPostFilter(boolFilter()
        .must(
          termFilter(ActiveRuleDocument.FIELD_PROFILE_ID, query.profileId()),
          hasParentFilter(TYPE_RULE, termsFilter(RuleDocument.FIELD_ID, ruleIds))
        ))
      .setSize(ruleIds.size());
    if (fields.length > 0) {
      activeRuleBuilder.addFields(fields);
    }
    return index.executeRequest(activeRuleBuilder);
  }

  private BoolFilterBuilder activeRuleFilter(ProfileRuleQuery query) {
    BoolFilterBuilder filter = boolFilter().must(termFilter(ActiveRuleDocument.FIELD_PROFILE_ID, query.profileId()));
    addMustTermOrTerms(filter, ActiveRuleDocument.FIELD_SEVERITY, query.severities());
    String inheritance = query.inheritance();
    if (inheritance != null) {
      addMustTermOrTerms(filter, ActiveRuleDocument.FIELD_INHERITANCE, newArrayList(inheritance));
    } else if (query.noInheritance()) {
      filter.mustNot(getTermOrTerms(ActiveRuleDocument.FIELD_INHERITANCE, newArrayList(QProfileRule.INHERITED, QProfileRule.OVERRIDES)));
    }
    return filter;
  }

  private BoolFilterBuilder ruleFilterForActiveRuleSearch(ProfileRuleQuery query) {
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

    for (String tag: query.tags()) {
      result.must(
        queryFilter(
          multiMatchQuery(tag, RuleDocument.FIELD_ADMIN_TAGS, RuleDocument.FIELD_SYSTEM_TAGS)));
    }

    if (StringUtils.isNotBlank(query.nameOrKey())) {
      result.must(
        queryFilter(
          multiMatchQuery(query.nameOrKey().trim(), RuleDocument.FIELD_NAME, RuleDocument.FIELD_NAME + ".search", RuleDocument.FIELD_KEY)
            .operator(Operator.AND)));
    }

    return result;
  }

  private BoolFilterBuilder ruleFilterForInactiveRuleSearch(ProfileRuleQuery query) {
    BoolFilterBuilder filter = ruleFilterForActiveRuleSearch(query)
      .mustNot(hasChildFilter(ESActiveRule.TYPE_ACTIVE_RULE, termFilter(ActiveRuleDocument.FIELD_PROFILE_ID, query.profileId())));
    addMustTermOrTerms(filter, RuleDocument.FIELD_SEVERITY, query.severities());

    for (String tag: query.tags()) {
      filter.must(
        queryFilter(
          multiMatchQuery(tag, RuleDocument.FIELD_ADMIN_TAGS, RuleDocument.FIELD_SYSTEM_TAGS)));
    }

    return filter;
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

  private abstract static class Search {

    int pageSize = 100;

    protected Search(int pageSize) {
      this.pageSize = pageSize;
    }

    abstract int search(int currentPage);

    void execute() {
      int currentPage = 1;
      boolean hasNextPage = true;
      while (hasNextPage) {
        int resultSize = search(currentPage);
        if (resultSize < pageSize) {
          hasNextPage = false;
        } else {
          currentPage++;
        }
      }
    }
  }

  public static class QProfileRuleResult {

    private final List<QProfileRule> rules;
    private final PagingResult paging;

    public QProfileRuleResult(List<QProfileRule> rules, PagingResult paging) {
      this.rules = rules;
      this.paging = paging;
    }

    public List<QProfileRule> rules() {
      return rules;
    }

    public PagingResult paging() {
      return paging;
    }
  }
}

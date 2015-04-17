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
package org.sonar.server.qualityprofile.index;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.SearchClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ActiveRuleIndex extends BaseIndex<ActiveRule, ActiveRuleDto, ActiveRuleKey> {

  public static final String COUNT_ACTIVE_RULES = "countActiveRules";

  public ActiveRuleIndex(ActiveRuleNormalizer normalizer, SearchClient node) {
    super(IndexDefinition.ACTIVE_RULE, normalizer, node);
  }

  @Override
  protected String getKeyValue(ActiveRuleKey key) {
    return key.toString();
  }

  @Override
  protected Map mapKey() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("path", ActiveRuleNormalizer.ActiveRuleField.KEY.field());
    return mapping;
  }

  @Override
  protected Map mapProperties() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    for (IndexField field : ActiveRuleNormalizer.ActiveRuleField.ALL_FIELDS) {
      mapping.put(field.field(), mapField(field));
    }
    return mapping;
  }

  @Override
  protected Map mapDomain() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("dynamic", false);
    mapping.put("_id", mapKey());
    mapping.put("_parent", mapParent());
    mapping.put("_routing", mapRouting());
    mapping.put("properties", mapProperties());
    return mapping;
  }

  private Map mapRouting() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("required", true);
    mapping.put("path", ActiveRuleNormalizer.ActiveRuleField.RULE_KEY.field());
    return mapping;
  }

  private Object mapParent() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("type", this.getParentType());
    return mapping;
  }

  @Override
  public ActiveRule toDoc(Map<String, Object> fields) {
    return new ActiveRuleDoc(fields);
  }

  /**
   * finder methods
   */
  public List<ActiveRule> findByRule(RuleKey key) {
    SearchRequestBuilder request = getClient().prepareSearch(this.getIndexName())
      .setQuery(QueryBuilders
        .hasParentQuery(this.getParentType(),
          QueryBuilders.idsQuery(this.getParentType())
            .addIds(key.toString())
        ))
      .setRouting(key.toString())
      // TODO replace by scrolling
      .setSize(Integer.MAX_VALUE);

    SearchResponse response = request.get();

    List<ActiveRule> activeRules = new ArrayList<ActiveRule>();
    for (SearchHit hit : response.getHits()) {
      activeRules.add(toDoc(hit.getSource()));
    }
    return activeRules;
  }

  public Iterator<ActiveRule> findByProfile(String key) {
    SearchRequestBuilder request = getClient().prepareSearch(getIndexName())
      .setTypes(IndexDefinition.ACTIVE_RULE.getIndexType())
      .setSearchType(SearchType.SCAN)
      .setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES))
      .setSize(100)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), FilterBuilders.boolFilter()
        .must(FilterBuilders.termFilter(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field(), key))
        .mustNot(FilterBuilders.hasParentFilter(this.getParentType(),
          FilterBuilders.termFilter(RuleNormalizer.RuleField.STATUS.field(), RuleStatus.REMOVED.name())))))
      .setRouting(key);

    SearchResponse response = request.get();
    return scroll(response.getScrollId());
  }

  private String getParentType() {
    return IndexDefinition.RULE.getIndexType();
  }

  public Long countByQualityProfileKey(String key) {
    return countByField(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY,
      FilterBuilders.hasParentFilter(IndexDefinition.RULE.getIndexType(),
        FilterBuilders.notFilter(
          FilterBuilders.termFilter(RuleNormalizer.RuleField.STATUS.field(), "REMOVED")))).get(key);
  }

  public Map<String, Long> countAllByQualityProfileKey() {
    return countByField(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY,
      FilterBuilders.hasParentFilter(IndexDefinition.RULE.getIndexType(),
        FilterBuilders.notFilter(
          FilterBuilders.termFilter(RuleNormalizer.RuleField.STATUS.field(), "REMOVED"))));
  }

  public Multimap<String, FacetValue> getStatsByProfileKey(String key) {
    return getStatsByProfileKeys(ImmutableList.of(key)).get(key);
  }

  public Map<String, Multimap<String, FacetValue>> getStatsByProfileKeys(List<String> keys) {
    SearchRequestBuilder request = getClient().prepareSearch(this.getIndexName())
      .setQuery(QueryBuilders.filteredQuery(
        QueryBuilders.termsQuery(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field(), keys),
        FilterBuilders.boolFilter()
          .mustNot(FilterBuilders.hasParentFilter(this.getParentType(),
            FilterBuilders.termFilter(RuleNormalizer.RuleField.STATUS.field(), RuleStatus.REMOVED.name())))))
      .addAggregation(AggregationBuilders.terms(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field())
        .field(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field()).size(0)
        .subAggregation(AggregationBuilders.terms(ActiveRuleNormalizer.ActiveRuleField.INHERITANCE.field())
          .field(ActiveRuleNormalizer.ActiveRuleField.INHERITANCE.field()))
        .subAggregation(AggregationBuilders.terms(ActiveRuleNormalizer.ActiveRuleField.SEVERITY.field())
          .field(ActiveRuleNormalizer.ActiveRuleField.SEVERITY.field()))
        .subAggregation(AggregationBuilders.count(COUNT_ACTIVE_RULES)))
      .setSize(0)
      .setTypes(this.getIndexType());
    SearchResponse response = request.get();
    Map<String, Multimap<String, FacetValue>> stats = new HashMap<String, Multimap<String, FacetValue>>();
    Aggregation aggregation = response.getAggregations().get(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field());
    for (Terms.Bucket value : ((Terms) aggregation).getBuckets()) {
      stats.put(value.getKey()
        , this.processAggregations(value.getAggregations()));
    }

    return stats;
  }
}

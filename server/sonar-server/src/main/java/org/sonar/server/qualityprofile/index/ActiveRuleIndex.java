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
package org.sonar.server.qualityprofile.index;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.valuecount.InternalValueCount;
import org.sonar.api.rule.RuleStatus;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.search.FacetValue;

import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_STATUS;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_ACTIVE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE;

/**
 * The unique entry-point to interact with Elasticsearch index "active rules".
 * All the requests are listed here.
 */
public class ActiveRuleIndex extends BaseIndex {

  public static final String COUNT_ACTIVE_RULES = "countActiveRules";

  public ActiveRuleIndex(EsClient client) {
    super(client);
  }

  public Map<String, Long> countAllByQualityProfileKey() {
    return countByField(FIELD_ACTIVE_RULE_PROFILE_KEY,
      QueryBuilders.hasParentQuery(TYPE_RULE,
        QueryBuilders.boolQuery().mustNot(
          QueryBuilders.termQuery(FIELD_RULE_STATUS, RuleStatus.REMOVED.name()))));
  }

  public Map<String, Long> countAllDeprecatedByQualityProfileKey() {
    return countByField(FIELD_ACTIVE_RULE_PROFILE_KEY,
      QueryBuilders.hasParentQuery(TYPE_RULE,
        QueryBuilders.boolQuery().must(
          QueryBuilders.termQuery(FIELD_RULE_STATUS, RuleStatus.DEPRECATED.name()))));
  }

  private Map<String, Long> countByField(String indexField, QueryBuilder filter) {
    Map<String, Long> counts = new HashMap<>();

    SearchRequestBuilder request = getClient().prepareSearch(INDEX)
      .setTypes(TYPE_ACTIVE_RULE)
      .setQuery(QueryBuilders.filteredQuery(
        QueryBuilders.matchAllQuery(),
        filter))
      .setSize(0)
      .addAggregation(AggregationBuilders
        .terms(indexField)
        .field(indexField)
        .order(Terms.Order.count(false))
        .size(Integer.MAX_VALUE)
        .minDocCount(0));

    SearchResponse response = request.get();

    Terms values = response.getAggregations().get(indexField);

    for (Terms.Bucket value : values.getBuckets()) {
      counts.put(value.getKeyAsString(), value.getDocCount());
    }
    return counts;
  }

  public Map<String, Multimap<String, FacetValue>> getStatsByProfileKeys(List<String> keys) {
    SearchRequestBuilder request = getClient().prepareSearch(INDEX)
      .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termsQuery(FIELD_ACTIVE_RULE_PROFILE_KEY, keys)).filter(
        QueryBuilders.boolQuery()
          .mustNot(QueryBuilders.hasParentQuery(TYPE_RULE,
            QueryBuilders.termQuery(FIELD_RULE_STATUS, RuleStatus.REMOVED.name())))))
      .addAggregation(AggregationBuilders.terms(FIELD_ACTIVE_RULE_PROFILE_KEY)
        .field(RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_KEY).size(0)
        .subAggregation(AggregationBuilders.terms(FIELD_ACTIVE_RULE_INHERITANCE)
          .field(RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE))
        .subAggregation(AggregationBuilders.terms(FIELD_ACTIVE_RULE_SEVERITY)
          .field(RuleIndexDefinition.FIELD_ACTIVE_RULE_SEVERITY))
        .subAggregation(AggregationBuilders.count(COUNT_ACTIVE_RULES)))
      .setSize(0)
      .setTypes(TYPE_ACTIVE_RULE);
    SearchResponse response = request.get();
    Map<String, Multimap<String, FacetValue>> stats = new HashMap<>();
    Aggregation aggregation = response.getAggregations().get(FIELD_ACTIVE_RULE_PROFILE_KEY);
    for (Terms.Bucket value : ((Terms) aggregation).getBuckets()) {
      stats.put(value.getKeyAsString(), processAggregations(value.getAggregations()));
    }

    return stats;
  }

  private static Multimap<String, FacetValue> processAggregations(@Nullable Aggregations aggregations) {
    Multimap<String, FacetValue> stats = ArrayListMultimap.create();
    if (aggregations == null) {
      return stats;
    }
    for (Aggregation aggregation : aggregations.asList()) {
      if (aggregation instanceof StringTerms) {
        for (Terms.Bucket value : ((Terms) aggregation).getBuckets()) {
          FacetValue facetValue = new FacetValue(value.getKeyAsString(), value.getDocCount());
          stats.put(aggregation.getName(), facetValue);
        }
      } else if (aggregation instanceof InternalValueCount) {
        InternalValueCount count = (InternalValueCount) aggregation;
        FacetValue facetValue = new FacetValue(count.getName(), count.getValue());
        stats.put(count.getName(), facetValue);
      }
    }
    return stats;
  }

}

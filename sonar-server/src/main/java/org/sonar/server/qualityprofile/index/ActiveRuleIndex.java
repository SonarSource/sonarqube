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

import com.google.common.collect.Multimap;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.ESNode;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.IndexField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActiveRuleIndex extends BaseIndex<ActiveRule, ActiveRuleDto, ActiveRuleKey> {

  public ActiveRuleIndex(Profiling profiling, ActiveRuleNormalizer normalizer, WorkQueue workQueue, ESNode node) {
    super(IndexDefinition.ACTIVE_RULE, normalizer, workQueue, node, profiling);
  }

  @Override
  protected String getKeyValue(ActiveRuleKey key) {
    return key.toString();
  }

  @Override
  protected Settings getIndexSettings() throws IOException {
    return ImmutableSettings.builder()
      .put("index.number_of_replicas", 0)
      .put("index.number_of_shards", 1)
      .build();
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

  public List<ActiveRule> findByProfile(QualityProfileKey key) {
    SearchRequestBuilder request = getClient().prepareSearch(getIndexName())
      .setQuery(QueryBuilders.termQuery(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field(), key.toString()))
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

  private String getParentType() {
    return IndexDefinition.RULE.getIndexType();
  }

  public Long countByQualityProfileKey(QualityProfileKey key) {
    CountRequestBuilder request = getClient().prepareCount(getIndexName())
      .setQuery(QueryBuilders.termQuery(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field(), key.toString()))
      .setRouting(key.toString());
    return request.get().getCount();
  }

  public Multimap<String, FacetValue> getStatsByProfileKey(QualityProfileKey key) {
    return getStatsByProfileKeys(ImmutableList.of(key)).get(key);
  }

  public Map<QualityProfileKey, Multimap<String, FacetValue>> getStatsByProfileKeys(List<QualityProfileKey> keys) {
    String[] stringKeys = new String[keys.size()];
    for (int i = 0; i < keys.size(); i++) {
      stringKeys[i] = keys.get(i).toString();
    }

    SearchResponse response = getClient().prepareSearch(this.getIndexName())
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
        FilterBuilders.termsFilter(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field(), stringKeys)))
      .addAggregation(AggregationBuilders.terms(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field())
        .field(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field())
        .subAggregation(AggregationBuilders.terms(ActiveRuleNormalizer.ActiveRuleField.INHERITANCE.field())
          .field(ActiveRuleNormalizer.ActiveRuleField.INHERITANCE.field()))
        .subAggregation(AggregationBuilders.terms(ActiveRuleNormalizer.ActiveRuleField.SEVERITY.field())
          .field(ActiveRuleNormalizer.ActiveRuleField.SEVERITY.field()))
        .subAggregation(AggregationBuilders.count("countActiveRules")))
      .setSize(0)
      .setTypes(this.getIndexType())
      .get();

    System.out.println("response = " + response);
    Map<QualityProfileKey, Multimap<String, FacetValue>> stats = new HashMap<QualityProfileKey, Multimap<String, FacetValue>>();
    Aggregation aggregation = response.getAggregations().get(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field());
    for (Terms.Bucket value : ((Terms) aggregation).getBuckets()) {
      stats.put(QualityProfileKey.parse(value.getKey())
        , this.processAggregations(value.getAggregations()));
    }

    return stats;
  }
}

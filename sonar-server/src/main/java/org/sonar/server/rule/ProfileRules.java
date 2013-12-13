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

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.MatchQueryBuilder.Operator;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.sonar.server.qualityprofile.Paging;
import org.sonar.server.qualityprofile.QProfileRule;
import org.sonar.server.search.SearchIndex;

import java.util.Collection;
import java.util.List;

import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;

public class ProfileRules {

  private final SearchIndex index;

  public ProfileRules(SearchIndex index) {
    this.index = index;
  }

  public List<QProfileRule> searchActiveRules(ProfileRuleQuery query, Paging paging) {
    BoolFilterBuilder filter = boolFilter().must(
            termFilter("profileId", query.profileId()),
            hasParentFilter("rule", parentRuleFilter(query))
        );
    addMustTermOrTerms(filter, "severity", query.severities());

    SearchRequestBuilder builder = index.client().prepareSearch("rules").setTypes("active_rule")
      .setFilter(filter)
      .addFields("_source", "_parent")
      .setSize(paging.pageSize())
      .setFrom(paging.offset());
    SearchHits hits = index.executeRequest(builder);

    String[] activeRuleSources = new String[hits.getHits().length];
    String[] parentIds = new String[hits.getHits().length];
    int hitCounter = 0;

    List<QProfileRule> result = Lists.newArrayList();
    for (SearchHit hit: hits.getHits()) {
      activeRuleSources[hitCounter] = hit.sourceAsString();
      parentIds[hitCounter] = hit.field("_parent").value();
      hitCounter ++;
    }

    if (hitCounter > 0) {
      MultiGetItemResponse[] responses = index.client().prepareMultiGet().add("rules", "rule", parentIds)
        .execute().actionGet().getResponses();

      for (int i = 0; i < hitCounter; i ++) {
        result.add(new QProfileRule(responses[i].getResponse().getSourceAsString(), activeRuleSources[i]));
      }
    }

    return result;
  }

  private FilterBuilder parentRuleFilter(ProfileRuleQuery query) {
    if (! query.hasParentRuleCriteria()) {
      return FilterBuilders.matchAllFilter();
    }

    BoolFilterBuilder result = boolFilter();

    addMustTermOrTerms(result, "repositoryKey", query.repositoryKeys());
    addMustTermOrTerms(result, "status", query.statuses());

    if (StringUtils.isNotBlank(query.nameOrKey())) {
      result.must(
        queryFilter(
          multiMatchQuery(query.nameOrKey(), "name", "key")
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
}

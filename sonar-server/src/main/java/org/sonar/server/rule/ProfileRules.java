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

import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.sonar.api.utils.Paging;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.qualityprofile.QProfileRule;
import org.sonar.server.search.SearchIndex;

import java.util.List;

import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;

public class ProfileRules {

  private final SearchIndex index;

  public ProfileRules(SearchIndex index) {
    this.index = index;
  }

  public List<QProfileRule> searchActiveRules(QProfile profile, Paging paging) {
    SearchHits hits = index.client().prepareSearch("rules").setTypes("active_rule")
      .setFilter(boolFilter()
          .must(
              termFilter("profileId", profile.id())
          ))
      .addFields("_source", "_parent")
      .setSize(paging.pageSize())
      .setFrom(paging.offset())
      .execute().actionGet().getHits();

    String[] activeRuleSources = new String[hits.getHits().length];
    String[] parentIds = new String[hits.getHits().length];
    int hitCounter = 0;

    List<QProfileRule> result = Lists.newArrayList();
    for (SearchHit hit: hits.getHits()) {
      activeRuleSources[hitCounter] = hit.sourceAsString();
      parentIds[hitCounter] = hit.field("_parent").value();
      hitCounter ++;
    }

    MultiGetItemResponse[] responses = index.client().prepareMultiGet().add("rules", "rule", parentIds)
      .execute().actionGet().getResponses();

    for (int i = 0; i < hitCounter; i ++) {
      result.add(new QProfileRule(responses[i].getResponse().getSourceAsString(), activeRuleSources[i]));
    }

    return result;
  }
}

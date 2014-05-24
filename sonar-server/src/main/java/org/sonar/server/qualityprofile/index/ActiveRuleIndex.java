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

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.search.ESNode;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.IndexDefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ActiveRuleIndex extends BaseIndex<ActiveRule, ActiveRuleDto, ActiveRuleKey> {

  public ActiveRuleIndex(ActiveRuleNormalizer normalizer, WorkQueue workQueue, ESNode node) {
    super(IndexDefinition.ACTIVE_RULE, normalizer, workQueue, node);
  }

  @Override
  protected String getKeyValue(ActiveRuleKey key) {
    return key.toString();
  }

  @Override
  protected XContentBuilder getIndexSettings() throws IOException {
    return null;
  }

  @Override
  protected XContentBuilder getMapping() throws IOException {
    XContentBuilder mapping = jsonBuilder().startObject()
      .startObject(this.indexDefinition.getIndexType())
      .field("dynamic", "strict")
      .startObject("_parent")
      .field("type", this.getParentType())
      .endObject()
      .startObject("_id")
      .field("path", ActiveRuleNormalizer.ActiveRuleField.KEY.key())
      .endObject()
      .startObject("_routing")
      .field("required", true)
      .field("path", ActiveRuleNormalizer.ActiveRuleField.RULE_KEY.key())
      .endObject();


    mapping.startObject("properties");
    addMatchField(mapping, ActiveRuleNormalizer.ActiveRuleField.KEY.key(), "string");
    addMatchField(mapping, ActiveRuleNormalizer.ActiveRuleField.RULE_KEY.key(), "string");
    addMatchField(mapping, ActiveRuleNormalizer.ActiveRuleField.INHERITANCE.key(), "string");
    addMatchField(mapping, ActiveRuleNormalizer.ActiveRuleField.SEVERITY.key(), "string");
    addMatchField(mapping, ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.key(), "string");
    mapping.startObject(ActiveRuleNormalizer.ActiveRuleField.PARAMS.key())
      .field("type", "object")
      .startObject("properties")
      .startObject("_id")
      .field("type", "string")
      .endObject()
      .startObject(ActiveRuleNormalizer.ActiveRuleParamField.NAME.key())
      .field("type", "string")
      .endObject()
      .startObject(ActiveRuleNormalizer.ActiveRuleParamField.VALUE.key())
      .field("type", "string")
      .endObject()
      .endObject();
    mapping.endObject();

    mapping.endObject().endObject();
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
      .setRouting(key.toString());

    SearchResponse response = request.get();

    List<ActiveRule> activeRules = new ArrayList<ActiveRule>();
    for (SearchHit hit : response.getHits()) {
      activeRules.add(toDoc(hit.getSource()));
    }
    return activeRules;
  }

  public List<ActiveRule> findByQProfile(QualityProfileKey key) {
    SearchRequestBuilder request = getClient().prepareSearch(this.getIndexName())
      .setQuery(QueryBuilders.termQuery(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.key(), key.toString()))
      .setRouting(key.toString());

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


}

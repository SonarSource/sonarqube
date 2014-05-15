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
package org.sonar.server.rule2.index;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.server.qualityprofile.index.ActiveRuleDoc;
import org.sonar.server.rule2.Rule;
import org.sonar.server.search.Result;

import java.util.Collection;
import java.util.Map;

public class RuleResult extends Result<Rule> {

  private Multimap<String,ActiveRuleDoc> activeRules = ArrayListMultimap.create();

  public RuleResult(SearchResponse response) {
    super(response);

    for (SearchHit hit : response.getHits()) {
      String ruleKey = hit.getFields().get(RuleNormalizer.RuleField.KEY.key()).getValue();
      if (hit.getFields().containsKey(RuleNormalizer.RuleField.ACTIVE.key())) {
        Map<String, Map<String, Object>> activeRulesForHit =
          hit.getFields().get(RuleNormalizer.RuleField.ACTIVE.key()).getValue();
        for (Map.Entry<String, Map<String, Object>> activeRule : activeRulesForHit.entrySet()) {
          System.out.println("ruleKey = " + ruleKey);
          System.out.println("activeRule = " + activeRule);
          activeRules.put(ruleKey,
            new ActiveRuleDoc(ActiveRuleKey.parse(activeRule.getKey()), activeRule.getValue()));
        }
      }
    }
  }

  @Override
  protected Rule getSearchResult(Map<String, Object> fields) {
    return new RuleDoc(fields);
  }

  public Collection<Rule> getRules() {
    return super.getHits();
  }

  public  Multimap<String,ActiveRuleDoc> getActiveRules() {
    System.out.println("activeRules = " + activeRules);
    return this.activeRules;
  }
}

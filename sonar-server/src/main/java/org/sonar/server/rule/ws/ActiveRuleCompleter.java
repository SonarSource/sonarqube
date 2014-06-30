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
package org.sonar.server.rule.ws;

import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.index.RuleQuery;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Add details about active rules to api/rules/search and api/rules/show
 * web services.
 */
public class ActiveRuleCompleter implements ServerComponent {
  private final QProfileLoader loader;

  public ActiveRuleCompleter(QProfileLoader loader) {
    this.loader = loader;
  }

  void completeSearch(RuleQuery query, Collection<Rule> rules, JsonWriter json) {
    json.name("actives").beginObject();

    if (query.getQProfileKey() != null) {
      // Load details of active rules on the selected profile
      for (Rule rule : rules) {
        ActiveRule activeRule = loader.getActiveRule(ActiveRuleKey.of(query.getQProfileKey(), rule.key()));
        if (activeRule != null) {
          writeActiveRules(rule.key(), Arrays.asList(activeRule), json);
        }
      }
    } else {
      // Load details of all active rules
      for (Rule rule : rules) {
        writeActiveRules(rule.key(), loader.findActiveRulesByRule(rule.key()), json);
      }
    }
    json.endObject();
  }

  void completeShow(Rule rule, JsonWriter json) {
    json.name("actives").beginArray();
    for (ActiveRule activeRule : loader.findActiveRulesByRule(rule.key())) {
      writeActiveRule(activeRule, json);
    }
    json.endArray();
  }

  private void writeActiveRules(RuleKey ruleKey, Collection<ActiveRule> activeRules, JsonWriter json) {
    if (!activeRules.isEmpty()) {
      json.name(ruleKey.toString());
      json.beginArray();
      for (ActiveRule activeRule : activeRules) {
        writeActiveRule(activeRule, json);
      }
      json.endArray();
    }
  }

  private void writeActiveRule(ActiveRule activeRule, JsonWriter json) {
    json
      .beginObject()
      .prop("qProfile", activeRule.key().qProfile().toString())
      .prop("inherit", activeRule.inheritance().toString())
      .prop("severity", activeRule.severity());
    if (activeRule.parentKey() != null) {
      json.prop("parent", activeRule.parentKey().toString());
    }
    json.name("params").beginArray();
    for (Map.Entry<String, String> param : activeRule.params().entrySet()) {
      json
        .beginObject()
        .prop("key", param.getKey())
        .prop("value", param.getValue())
        .endObject();
    }
    json.endArray().endObject();
  }
}

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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.sonar.api.ServerSide;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.index.RuleQuery;

import javax.annotation.CheckForNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Add details about active rules to api/rules/search and api/rules/show
 * web services.
 */
@ServerSide
public class ActiveRuleCompleter {

  private static final Logger LOG = Loggers.get(ActiveRuleCompleter.class);

  private final QProfileLoader loader;
  private final Languages languages;

  public ActiveRuleCompleter(QProfileLoader loader, Languages languages) {
    this.loader = loader;
    this.languages = languages;
  }

  void completeSearch(RuleQuery query, Collection<Rule> rules, JsonWriter json) {
    Collection<String> harvestedProfileKeys = writeActiveRules(json, query, rules);

    writeProfiles(json, harvestedProfileKeys);
  }

  private Collection<String> writeActiveRules(JsonWriter json, RuleQuery query, Collection<Rule> rules) {
    Collection<String> qProfileKeys = Sets.newHashSet();

    json.name("actives").beginObject();
    String profileKey = query.getQProfileKey();
    if (profileKey != null) {
      // Load details of active rules on the selected profile
      for (Rule rule : rules) {
        ActiveRule activeRule = loader.getActiveRule(ActiveRuleKey.of(profileKey, rule.key()));
        if (activeRule != null) {
          qProfileKeys = writeActiveRules(rule.key(), Arrays.asList(activeRule), json);
        }
      }
    } else {
      // Load details of all active rules
      for (Rule rule : rules) {
        qProfileKeys = writeActiveRules(rule.key(), loader.findActiveRulesByRule(rule.key()), json);
      }
    }
    json.endObject();

    return qProfileKeys;
  }

  void completeShow(Rule rule, JsonWriter json) {
    json.name("actives").beginArray();
    for (ActiveRule activeRule : loader.findActiveRulesByRule(rule.key())) {
      writeActiveRule(activeRule, json);
    }
    json.endArray();
  }

  private Collection<String> writeActiveRules(RuleKey ruleKey, Collection<ActiveRule> activeRules, JsonWriter json) {
    Collection<String> qProfileKeys = Sets.newHashSet();
    if (!activeRules.isEmpty()) {
      json.name(ruleKey.toString());
      json.beginArray();
      for (ActiveRule activeRule : activeRules) {
        qProfileKeys.add(writeActiveRule(activeRule, json));
      }
      json.endArray();
    }
    return qProfileKeys;
  }

  private String writeActiveRule(ActiveRule activeRule, JsonWriter json) {
    json
      .beginObject()
      .prop("qProfile", activeRule.key().qProfile().toString())
      .prop("inherit", activeRule.inheritance().toString())
      .prop("severity", activeRule.severity());
    ActiveRuleKey parentKey = activeRule.parentKey();
    if (parentKey != null) {
      json.prop("parent", parentKey.toString());
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
    return activeRule.key().qProfile();
  }

  private void writeProfiles(JsonWriter json, Collection<String> harvestedProfileKeys) {
    Map<String, QualityProfileDto> qProfilesByKey = Maps.newHashMap();
    for (String qProfileKey : harvestedProfileKeys) {
      if (!qProfilesByKey.containsKey(qProfileKey)) {
        QualityProfileDto profile = loadProfile(qProfileKey);
        if (profile == null) {
          LOG.warn("Could not find quality profile with key " + qProfileKey);
          continue;
        }
        qProfilesByKey.put(qProfileKey, profile);
        String parentKee = profile.getParentKee();
        if (parentKee != null && !qProfilesByKey.containsKey(parentKee)) {
          qProfilesByKey.put(parentKee, loadProfile(parentKee));
        }
      }
    }
    json.name("qProfiles").beginObject();
    for (QualityProfileDto profile : qProfilesByKey.values()) {
      writeProfile(json, profile);
    }
    json.endObject();
  }

  @CheckForNull
  QualityProfileDto loadProfile(String qProfileKey) {
    return loader.getByKey(qProfileKey);
  }

  private void writeProfile(JsonWriter json, QualityProfileDto profile) {
    Language language = languages.get(profile.getLanguage());
    String langName = language == null ? profile.getLanguage() : language.getName();
    json.name(profile.getKey()).beginObject()
      .prop("name", profile.getName())
      .prop("lang", profile.getLanguage())
      .prop("langName", langName)
      .prop("parent", profile.getParentKee())
      .endObject();
  }

}

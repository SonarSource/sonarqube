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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.index.RuleQuery;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.SearchResponse;
import org.sonarqube.ws.Rules.ShowResponse;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Sets.newHashSet;

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

  void completeSearch(RuleQuery query, Collection<Rule> rules, SearchResponse.Builder searchResponse) {
    Collection<String> harvestedProfileKeys = writeActiveRules(searchResponse, query, rules);
    searchResponse.setQProfiles(buildQProfiles(harvestedProfileKeys));
  }

  private Collection<String> writeActiveRules(SearchResponse.Builder response, RuleQuery query, Collection<Rule> rules) {
    Collection<String> qProfileKeys = newHashSet();
    Rules.Actives.Builder activesBuilder = response.getActivesBuilder();

    String profileKey = query.getQProfileKey();
    if (profileKey != null) {
      // Load details of active rules on the selected profile
      for (Rule rule : rules) {
        ActiveRule activeRule = loader.getActiveRule(ActiveRuleKey.of(profileKey, rule.key()));
        if (activeRule != null) {
          qProfileKeys = writeActiveRules(rule.key(), Arrays.asList(activeRule), activesBuilder);
        }
      }
    } else {
      // Load details of all active rules
      for (Rule rule : rules) {
        qProfileKeys = writeActiveRules(rule.key(), loader.findActiveRulesByRule(rule.key()), activesBuilder);
      }
    }

    response.setActives(activesBuilder);
    return qProfileKeys;
  }

  void completeShow(Rule rule, ShowResponse.Builder response) {
    for (ActiveRule activeRule : loader.findActiveRulesByRule(rule.key())) {
      response.addActives(buildActiveRuleResponse(activeRule));
    }
  }

  private static Collection<String> writeActiveRules(RuleKey ruleKey, Collection<ActiveRule> activeRules, Rules.Actives.Builder activesBuilder) {
    Collection<String> qProfileKeys = newHashSet();
    Rules.ActiveList.Builder activeRulesListResponse = Rules.ActiveList.newBuilder();
    for (ActiveRule activeRule : activeRules) {
      activeRulesListResponse.addActiveList(buildActiveRuleResponse(activeRule));
      qProfileKeys.add(activeRule.key().qProfile());
    }
    activesBuilder
      .getMutableActives()
      .put(ruleKey.toString(), activeRulesListResponse.build());
    return qProfileKeys;
  }

  private static Rules.Active buildActiveRuleResponse(ActiveRule activeRule) {
    Rules.Active.Builder activeRuleResponse = Rules.Active.newBuilder();
    activeRuleResponse.setQProfile(activeRule.key().qProfile());
    activeRuleResponse.setInherit(activeRule.inheritance().toString());
    activeRuleResponse.setSeverity(activeRule.severity());
    ActiveRuleKey parentKey = activeRule.parentKey();
    if (parentKey != null) {
      activeRuleResponse.setParent(parentKey.toString());
    }
    Rules.Active.Param.Builder paramBuilder = Rules.Active.Param.newBuilder();
    for (Map.Entry<String, String> param : activeRule.params().entrySet()) {
      activeRuleResponse.addParams(paramBuilder.clear()
        .setKey(param.getKey())
        .setValue(nullToEmpty(param.getValue())));
    }

    return activeRuleResponse.build();
  }

  private Rules.QProfiles.Builder buildQProfiles(Collection<String> harvestedProfileKeys) {
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

    Rules.QProfiles.Builder qProfilesResponse = Rules.QProfiles.newBuilder();
    Map<String, Rules.QProfile> qProfilesMapResponse = qProfilesResponse.getMutableQProfiles();
    for (QualityProfileDto profile : qProfilesByKey.values()) {
      writeProfile(qProfilesMapResponse, profile);
    }

    return qProfilesResponse;
  }

  @CheckForNull
  QualityProfileDto loadProfile(String qProfileKey) {
    return loader.getByKey(qProfileKey);
  }

  private void writeProfile(Map<String, Rules.QProfile> profilesResponse, QualityProfileDto profile) {
    Rules.QProfile.Builder profileResponse = Rules.QProfile.newBuilder();
    if (profile.getName() != null) {
      profileResponse.setName(profile.getName());
    }
    if (profile.getLanguage() != null) {
      profileResponse.setLang(profile.getLanguage());
      Language language = languages.get(profile.getLanguage());
      String langName = language == null ? profile.getLanguage() : language.getName();
      profileResponse.setLangName(langName);
    }
    if (profile.getParentKee() != null) {
      profileResponse.setParent(profile.getParentKee());
    }

    profilesResponse.put(profile.getKey(), profileResponse.build());
  }

}

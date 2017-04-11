/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.rule.ws;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.rule.index.RuleQuery;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.SearchResponse;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singletonList;
import static org.sonar.core.util.Protobuf.setNullable;

/**
 * Add details about active rules to api/rules/search and api/rules/show
 * web services.
 */
@ServerSide
public class ActiveRuleCompleter {

  private static final Logger LOG = Loggers.get(ActiveRuleCompleter.class);

  private final DbClient dbClient;
  private final Languages languages;

  public ActiveRuleCompleter(DbClient dbClient, Languages languages) {
    this.dbClient = dbClient;
    this.languages = languages;
  }

  void completeSearch(DbSession dbSession, RuleQuery query, List<RuleDto> rules, SearchResponse.Builder searchResponse) {
    Collection<String> harvestedProfileKeys = writeActiveRules(dbSession, searchResponse, query, rules);
    searchResponse.setQProfiles(buildQProfiles(dbSession, harvestedProfileKeys));
  }

  private Collection<String> writeActiveRules(DbSession dbSession, SearchResponse.Builder response, RuleQuery query, List<RuleDto> rules) {
    Collection<String> qProfileKeys = new HashSet<>();
    Rules.Actives.Builder activesBuilder = response.getActivesBuilder();

    String profileKey = query.getQProfileKey();
    if (profileKey != null) {
      // Load details of active rules on the selected profile
      List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().selectByProfileKey(dbSession, profileKey);
      Map<RuleKey, ActiveRuleDto> activeRuleByRuleKey = activeRuleDtos.stream().collect(MoreCollectors.uniqueIndex(d -> d.getKey().ruleKey()));
      ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey = activeRuleDtosToActiveRuleParamDtos(dbSession, activeRuleDtos);

      for (RuleDto rule : rules) {
        ActiveRuleDto activeRule = activeRuleByRuleKey.get(rule.getKey());
        if (activeRule != null) {
          qProfileKeys = writeActiveRules(rule.getKey(), singletonList(activeRule), activeRuleParamsByActiveRuleKey, activesBuilder);
        }
      }
    } else {
      // Load details of all active rules
      List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().selectByRuleIds(dbSession, query.getOrganizationUuid(), Lists.transform(rules, RuleDto::getId));
      Multimap<RuleKey, ActiveRuleDto> activeRulesByRuleKey = from(activeRuleDtos).index(d -> d.getKey().ruleKey());
      ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey = activeRuleDtosToActiveRuleParamDtos(dbSession, activeRuleDtos);
      for (RuleDto rule : rules) {
        qProfileKeys = writeActiveRules(rule.getKey(), activeRulesByRuleKey.get(rule.getKey()), activeRuleParamsByActiveRuleKey, activesBuilder);
      }
    }

    response.setActives(activesBuilder);
    return qProfileKeys;
  }

  private static Collection<String> writeActiveRules(RuleKey ruleKey, Collection<ActiveRuleDto> activeRules,
    ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey, Rules.Actives.Builder activesBuilder) {
    Collection<String> qProfileKeys = newHashSet();
    Rules.ActiveList.Builder activeRulesListResponse = Rules.ActiveList.newBuilder();
    for (ActiveRuleDto activeRule : activeRules) {
      activeRulesListResponse.addActiveList(buildActiveRuleResponse(activeRule, activeRuleParamsByActiveRuleKey.get(activeRule.getKey())));
      qProfileKeys.add(activeRule.getKey().qProfile());
    }
    activesBuilder
      .getMutableActives()
      .put(ruleKey.toString(), activeRulesListResponse.build());
    return qProfileKeys;
  }

  private ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleDtosToActiveRuleParamDtos(DbSession dbSession, List<ActiveRuleDto> activeRuleDtos) {
    Map<Integer, ActiveRuleKey> activeRuleIdsByKey = new HashMap<>();
    for (ActiveRuleDto activeRuleDto : activeRuleDtos) {
      activeRuleIdsByKey.put(activeRuleDto.getId(), activeRuleDto.getKey());
    }
    List<ActiveRuleParamDto> activeRuleParamDtos = dbClient.activeRuleDao().selectParamsByActiveRuleIds(dbSession, Lists.transform(activeRuleDtos, ActiveRuleDto::getId));
    ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey = ArrayListMultimap.create(activeRuleDtos.size(), 10);
    for (ActiveRuleParamDto activeRuleParamDto : activeRuleParamDtos) {
      ActiveRuleKey activeRuleKey = activeRuleIdsByKey.get(activeRuleParamDto.getActiveRuleId());
      activeRuleParamsByActiveRuleKey.put(activeRuleKey, activeRuleParamDto);
    }

    return activeRuleParamsByActiveRuleKey;
  }

  List<Rules.Active> completeShow(DbSession dbSession, OrganizationDto organization, RuleDefinitionDto rule) {
    List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().selectByRuleId(dbSession, organization, rule.getId());
    Map<Integer, ActiveRuleKey> activeRuleIdsByKey = new HashMap<>();
    for (ActiveRuleDto activeRuleDto : activeRuleDtos) {
      activeRuleIdsByKey.put(activeRuleDto.getId(), activeRuleDto.getKey());
    }

    List<Integer> activeRuleIds = activeRuleDtos.stream().map(ActiveRuleDto::getId).collect(Collectors.toList());
    List<ActiveRuleParamDto> activeRuleParamDtos = dbClient.activeRuleDao().selectParamsByActiveRuleIds(dbSession, activeRuleIds);
    ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey = ArrayListMultimap.create(activeRuleDtos.size(), 10);
    for (ActiveRuleParamDto activeRuleParamDto : activeRuleParamDtos) {
      ActiveRuleKey activeRuleKey = activeRuleIdsByKey.get(activeRuleParamDto.getActiveRuleId());
      activeRuleParamsByActiveRuleKey.put(activeRuleKey, activeRuleParamDto);
    }

    return activeRuleDtos.stream()
      .map(activeRule -> buildActiveRuleResponse(activeRule, activeRuleParamsByActiveRuleKey.get(activeRule.getKey())))
      .collect(Collectors.toList());
  }

  private static Rules.Active buildActiveRuleResponse(ActiveRuleDto activeRule, List<ActiveRuleParamDto> parameters) {
    Rules.Active.Builder activeRuleResponse = Rules.Active.newBuilder();
    activeRuleResponse.setQProfile(activeRule.getKey().qProfile());
    String inheritance = activeRule.getInheritance();
    activeRuleResponse.setInherit(inheritance != null ? inheritance : ActiveRule.Inheritance.NONE.name());
    activeRuleResponse.setSeverity(activeRule.getSeverityString());
    activeRuleResponse.setCreatedAt(DateUtils.formatDateTime(activeRule.getCreatedAt()));
    Rules.Active.Param.Builder paramBuilder = Rules.Active.Param.newBuilder();
    for (ActiveRuleParamDto parameter : parameters) {
      activeRuleResponse.addParams(paramBuilder.clear()
        .setKey(parameter.getKey())
        .setValue(nullToEmpty(parameter.getValue())));
    }

    return activeRuleResponse.build();
  }

  private Rules.QProfiles.Builder buildQProfiles(DbSession dbSession, Collection<String> harvestedProfileKeys) {
    Map<String, QualityProfileDto> qProfilesByKey = new HashMap<>();
    for (String qProfileKey : harvestedProfileKeys) {
      if (!qProfilesByKey.containsKey(qProfileKey)) {
        QualityProfileDto profile = loadProfile(dbSession, qProfileKey);
        if (profile == null) {
          LOG.warn("Could not find quality profile with key " + qProfileKey);
          continue;
        }
        qProfilesByKey.put(qProfileKey, profile);
        String parentKee = profile.getParentKee();
        if (parentKee != null && !qProfilesByKey.containsKey(parentKee)) {
          qProfilesByKey.put(parentKee, loadProfile(dbSession, parentKee));
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
  private QualityProfileDto loadProfile(DbSession dbSession, String qProfileKey) {
    return dbClient.qualityProfileDao().selectByKey(dbSession, qProfileKey);
  }

  private void writeProfile(Map<String, Rules.QProfile> profilesResponse, QualityProfileDto profile) {
    Rules.QProfile.Builder profileResponse = Rules.QProfile.newBuilder();
    setNullable(profile.getName(), profileResponse::setName);

    if (profile.getLanguage() != null) {
      profileResponse.setLang(profile.getLanguage());
      Language language = languages.get(profile.getLanguage());
      String langName = language == null ? profile.getLanguage() : language.getName();
      profileResponse.setLangName(langName);
    }
    setNullable(profile.getParentKee(), profileResponse::setParent);

    profilesResponse.put(profile.getKey(), profileResponse.build());
  }
}

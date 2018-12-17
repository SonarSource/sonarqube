/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;
import org.sonar.server.rule.index.RuleQuery;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.SearchResponse;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

/**
 * Add details about active rules to api/rules/search and api/rules/show
 * web services.
 */
@ServerSide
public class ActiveRuleCompleter {

  private final DbClient dbClient;
  private final Languages languages;

  public ActiveRuleCompleter(DbClient dbClient, Languages languages) {
    this.dbClient = dbClient;
    this.languages = languages;
  }

  void completeSearch(DbSession dbSession, RuleQuery query, List<RuleDto> rules, SearchResponse.Builder searchResponse) {
    Set<String> profileUuids = writeActiveRules(dbSession, searchResponse, query, rules);
    searchResponse.setQProfiles(buildQProfiles(dbSession, profileUuids));
  }

  private Set<String> writeActiveRules(DbSession dbSession, SearchResponse.Builder response, RuleQuery query, List<RuleDto> rules) {
    final Set<String> profileUuids = new HashSet<>();
    Rules.Actives.Builder activesBuilder = response.getActivesBuilder();

    QProfileDto profile = query.getQProfile();
    if (profile != null) {
      // Load details of active rules on the selected profile
      List<OrgActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByProfile(dbSession, profile);
      Map<RuleKey, OrgActiveRuleDto> activeRuleByRuleKey = activeRules.stream()
        .collect(uniqueIndex(ActiveRuleDto::getRuleKey));
      ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey = loadParams(dbSession, activeRules);

      for (RuleDto rule : rules) {
        OrgActiveRuleDto activeRule = activeRuleByRuleKey.get(rule.getKey());
        if (activeRule != null) {
          profileUuids.addAll(writeActiveRules(rule.getKey(), singletonList(activeRule), activeRuleParamsByActiveRuleKey, activesBuilder));
        }
      }
    } else {
      // Load details of all active rules
      List<Integer> ruleIds = Lists.transform(rules, RuleDto::getId);
      List<OrgActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByRuleIds(dbSession, query.getOrganization(), ruleIds);
      Multimap<RuleKey, OrgActiveRuleDto> activeRulesByRuleKey = activeRules.stream()
        .collect(MoreCollectors.index(OrgActiveRuleDto::getRuleKey));
      ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey = loadParams(dbSession, activeRules);
      rules.forEach(rule -> profileUuids.addAll(writeActiveRules(rule.getKey(), activeRulesByRuleKey.get(rule.getKey()), activeRuleParamsByActiveRuleKey, activesBuilder)));
    }

    response.setActives(activesBuilder);
    return profileUuids;
  }

  private static Set<String> writeActiveRules(RuleKey ruleKey, Collection<OrgActiveRuleDto> activeRules,
    ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey, Rules.Actives.Builder activesBuilder) {
    final Set<String> profileUuids = new HashSet<>();
    Rules.ActiveList.Builder activeRulesListResponse = Rules.ActiveList.newBuilder();
    for (OrgActiveRuleDto activeRule : activeRules) {
      activeRulesListResponse.addActiveList(buildActiveRuleResponse(activeRule, activeRuleParamsByActiveRuleKey.get(activeRule.getKey())));
      profileUuids.add(activeRule.getProfileUuid());
    }
    activesBuilder
      .getMutableActives()
      .put(ruleKey.toString(), activeRulesListResponse.build());
    return profileUuids;
  }

  private ListMultimap<ActiveRuleKey, ActiveRuleParamDto> loadParams(DbSession dbSession, List<OrgActiveRuleDto> activeRules) {
    Map<Integer, ActiveRuleKey> activeRuleIdsByKey = new HashMap<>();
    for (OrgActiveRuleDto activeRule : activeRules) {
      activeRuleIdsByKey.put(activeRule.getId(), activeRule.getKey());
    }
    List<ActiveRuleParamDto> activeRuleParams = dbClient.activeRuleDao().selectParamsByActiveRuleIds(dbSession, Lists.transform(activeRules, ActiveRuleDto::getId));
    ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey = ArrayListMultimap.create(activeRules.size(), 10);
    for (ActiveRuleParamDto activeRuleParam : activeRuleParams) {
      ActiveRuleKey activeRuleKey = activeRuleIdsByKey.get(activeRuleParam.getActiveRuleId());
      activeRuleParamsByActiveRuleKey.put(activeRuleKey, activeRuleParam);
    }

    return activeRuleParamsByActiveRuleKey;
  }

  List<Rules.Active> completeShow(DbSession dbSession, OrganizationDto organization, RuleDefinitionDto rule) {
    List<OrgActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByRuleId(dbSession, organization, rule.getId());
    Map<Integer, ActiveRuleKey> activeRuleIdsByKey = new HashMap<>();
    for (OrgActiveRuleDto activeRuleDto : activeRules) {
      activeRuleIdsByKey.put(activeRuleDto.getId(), activeRuleDto.getKey());
    }

    List<Integer> activeRuleIds = activeRules.stream().map(ActiveRuleDto::getId).collect(Collectors.toList());
    List<ActiveRuleParamDto> activeRuleParams = dbClient.activeRuleDao().selectParamsByActiveRuleIds(dbSession, activeRuleIds);
    ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey = ArrayListMultimap.create(activeRules.size(), 10);
    for (ActiveRuleParamDto activeRuleParamDto : activeRuleParams) {
      ActiveRuleKey activeRuleKey = activeRuleIdsByKey.get(activeRuleParamDto.getActiveRuleId());
      activeRuleParamsByActiveRuleKey.put(activeRuleKey, activeRuleParamDto);
    }

    return activeRules.stream()
      .map(activeRule -> buildActiveRuleResponse(activeRule, activeRuleParamsByActiveRuleKey.get(activeRule.getKey())))
      .collect(Collectors.toList());
  }

  private static Rules.Active buildActiveRuleResponse(OrgActiveRuleDto activeRule, List<ActiveRuleParamDto> parameters) {
    Rules.Active.Builder builder = Rules.Active.newBuilder();
    builder.setQProfile(activeRule.getProfileUuid());
    String inheritance = activeRule.getInheritance();
    builder.setInherit(inheritance != null ? inheritance : ActiveRuleInheritance.NONE.name());
    builder.setSeverity(activeRule.getSeverityString());
    builder.setCreatedAt(DateUtils.formatDateTime(activeRule.getCreatedAt()));
    builder.setUpdatedAt(DateUtils.formatDateTime(activeRule.getUpdatedAt()));
    Rules.Active.Param.Builder paramBuilder = Rules.Active.Param.newBuilder();
    for (ActiveRuleParamDto parameter : parameters) {
      builder.addParams(paramBuilder.clear()
        .setKey(parameter.getKey())
        .setValue(nullToEmpty(parameter.getValue())));
    }

    return builder.build();
  }

  private Rules.QProfiles.Builder buildQProfiles(DbSession dbSession, Set<String> profileUuids) {
    Rules.QProfiles.Builder result = Rules.QProfiles.newBuilder();
    if (profileUuids.isEmpty()) {
      return result;
    }

    // load profiles
    Map<String, QProfileDto> profilesByUuid = dbClient.qualityProfileDao().selectByUuids(dbSession, new ArrayList<>(profileUuids))
      .stream()
      .collect(Collectors.toMap(QProfileDto::getKee, Function.identity()));

    // load associated parents
    List<String> parentUuids = profilesByUuid.values().stream()
      .map(QProfileDto::getParentKee)
      .filter(StringUtils::isNotEmpty)
      .filter(uuid -> !profilesByUuid.containsKey(uuid))
      .collect(MoreCollectors.toList());
    if (!parentUuids.isEmpty()) {
      dbClient.qualityProfileDao().selectByUuids(dbSession, parentUuids)
        .forEach(p -> profilesByUuid.put(p.getKee(), p));
    }

    Map<String, Rules.QProfile> qProfilesMapResponse = result.getMutableQProfiles();
    profilesByUuid.values().forEach(p -> writeProfile(qProfilesMapResponse, p));

    return result;
  }

  private void writeProfile(Map<String, Rules.QProfile> profilesResponse, QProfileDto profile) {
    Rules.QProfile.Builder profileResponse = Rules.QProfile.newBuilder();
    ofNullable(profile.getName()).ifPresent(profileResponse::setName);

    if (profile.getLanguage() != null) {
      profileResponse.setLang(profile.getLanguage());
      Language language = languages.get(profile.getLanguage());
      String langName = language == null ? profile.getLanguage() : language.getName();
      profileResponse.setLangName(langName);
    }
    ofNullable(profile.getParentKee()).ifPresent(profileResponse::setParent);

    profilesResponse.put(profile.getKee(), profileResponse.build());
  }
}

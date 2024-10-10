/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.rule.ImpactFormatter;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.Facets;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_DEPRECATED_KEYS;

@ServerSide
public class RulesResponseFormatter {
  private final DbClient dbClient;
  private final RuleWsSupport ruleWsSupport;
  private final RuleMapper mapper;
  private final Languages languages;

  public RulesResponseFormatter(DbClient dbClient, RuleWsSupport ruleWsSupport, RuleMapper mapper, Languages languages) {
    this.dbClient = dbClient;
    this.ruleWsSupport = ruleWsSupport;
    this.mapper = mapper;
    this.languages = languages;
  }

  public List<Rules.Rule> formatRulesSearch(DbSession dbSession, SearchResult result, Set<String> fields) {
    List<RuleDto> rules = result.getRules();
    Map<String, UserDto> usersByUuid = ruleWsSupport.getUsersByUuid(dbSession, rules);
    Map<String, List<DeprecatedRuleKeyDto>> deprecatedRuleKeysByRuleUuid = getDeprecatedRuleKeysByRuleUuid(dbSession, rules, fields);

    return rules.stream()
      .map(rule -> mapper.toWsRule(rule, result, fields, usersByUuid, deprecatedRuleKeysByRuleUuid))
      .toList();
  }

  public List<Rules.Rule> formatRulesList(DbSession dbSession, SearchResult result) {
    Set<String> fields = Set.of("repo", "name", "severity", "lang", "internalKey", "templateKey", "params", "actives", "createdAt", "updatedAt", "deprecatedKeys", "langName");
    return formatRulesSearch(dbSession, result, fields);
  }

  private Map<String, List<DeprecatedRuleKeyDto>> getDeprecatedRuleKeysByRuleUuid(DbSession dbSession, List<RuleDto> rules, Set<String> fields) {
    if (!RuleMapper.shouldReturnField(fields, FIELD_DEPRECATED_KEYS)) {
      return Collections.emptyMap();
    }

    Set<String> ruleUuidsSet = rules.stream()
      .map(RuleDto::getUuid)
      .collect(Collectors.toSet());
    if (ruleUuidsSet.isEmpty()) {
      return Collections.emptyMap();
    } else {
      return dbClient.ruleDao().selectDeprecatedRuleKeysByRuleUuids(dbSession, ruleUuidsSet).stream()
        .collect(Collectors.groupingBy(DeprecatedRuleKeyDto::getRuleUuid));
    }
  }

  public Rules.QProfiles formatQualityProfiles(DbSession dbSession, Set<String> profileUuids) {
    Rules.QProfiles.Builder result = Rules.QProfiles.newBuilder();
    if (profileUuids.isEmpty()) {
      return result.build();
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
      .toList();
    if (!parentUuids.isEmpty()) {
      dbClient.qualityProfileDao().selectByUuids(dbSession, parentUuids)
        .forEach(p -> profilesByUuid.put(p.getKee(), p));
    }

    profilesByUuid.values().forEach(p -> writeProfile(result, p));

    return result.build();
  }

  public Rules.Actives formatActiveRules(DbSession dbSession, @Nullable QProfileDto profile, List<RuleDto> rules) {
    Rules.Actives.Builder activesBuilder = Rules.Actives.newBuilder();

    if (profile != null) {
      // Load details of active rules on the selected profile
      List<OrgActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByProfile(dbSession, profile);
      Map<RuleKey, OrgActiveRuleDto> activeRuleByRuleKey = activeRules.stream()
        .collect(Collectors.toMap(ActiveRuleDto::getRuleKey, Function.identity()));
      ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey = loadParams(dbSession, activeRules);

      for (RuleDto rule : rules) {
        OrgActiveRuleDto activeRule = activeRuleByRuleKey.get(rule.getKey());
        if (activeRule != null) {
          writeActiveRules(rule.getKey(), singletonList(activeRule), activeRuleParamsByActiveRuleKey, activesBuilder);
        }
      }
    } else {
      // Load details of all active rules
      List<String> ruleUuids = Lists.transform(rules, RuleDto::getUuid);
      List<OrgActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByRuleUuids(dbSession, ruleUuids);
      Multimap<RuleKey, OrgActiveRuleDto> activeRulesByRuleKey = activeRules.stream()
        .collect(MoreCollectors.index(OrgActiveRuleDto::getRuleKey));
      ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey = loadParams(dbSession, activeRules);
      rules.forEach(rule -> writeActiveRules(rule.getKey(), activeRulesByRuleKey.get(rule.getKey()), activeRuleParamsByActiveRuleKey, activesBuilder));
    }

    return activesBuilder.build();
  }

  private static void writeActiveRules(RuleKey ruleKey, Collection<OrgActiveRuleDto> activeRules,
    ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey, Rules.Actives.Builder activesBuilder) {
    Rules.ActiveList.Builder activeRulesListResponse = Rules.ActiveList.newBuilder();
    for (OrgActiveRuleDto activeRule : activeRules) {
      activeRulesListResponse.addActiveList(buildActiveRuleResponse(activeRule, activeRuleParamsByActiveRuleKey.get(activeRule.getKey())));
    }
    activesBuilder
      .getMutableActives()
      .put(ruleKey.toString(), activeRulesListResponse.build());
  }

  private ListMultimap<ActiveRuleKey, ActiveRuleParamDto> loadParams(DbSession dbSession, List<OrgActiveRuleDto> activeRules) {
    Map<String, ActiveRuleKey> activeRuleUuidsByKey = new HashMap<>();
    for (OrgActiveRuleDto activeRule : activeRules) {
      activeRuleUuidsByKey.put(activeRule.getUuid(), activeRule.getKey());
    }
    List<ActiveRuleParamDto> activeRuleParams = dbClient.activeRuleDao().selectParamsByActiveRuleUuids(dbSession, Lists.transform(activeRules, ActiveRuleDto::getUuid));
    ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey = ArrayListMultimap.create(activeRules.size(), 10);
    for (ActiveRuleParamDto activeRuleParam : activeRuleParams) {
      ActiveRuleKey activeRuleKey = activeRuleUuidsByKey.get(activeRuleParam.getActiveRuleUuid());
      activeRuleParamsByActiveRuleKey.put(activeRuleKey, activeRuleParam);
    }

    return activeRuleParamsByActiveRuleKey;
  }

  public List<Rules.Active> formatActiveRule(DbSession dbSession, RuleDto rule) {
    List<OrgActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByOrgRuleUuid(dbSession, rule.getUuid());
    Map<String, ActiveRuleKey> activeRuleUuidsByKey = new HashMap<>();
    for (OrgActiveRuleDto activeRuleDto : activeRules) {
      activeRuleUuidsByKey.put(activeRuleDto.getUuid(), activeRuleDto.getKey());
    }

    List<String> activeRuleUuids = activeRules.stream().map(ActiveRuleDto::getUuid).toList();
    List<ActiveRuleParamDto> activeRuleParams = dbClient.activeRuleDao().selectParamsByActiveRuleUuids(dbSession, activeRuleUuids);
    ListMultimap<ActiveRuleKey, ActiveRuleParamDto> activeRuleParamsByActiveRuleKey = ArrayListMultimap.create(activeRules.size(), 10);
    for (ActiveRuleParamDto activeRuleParamDto : activeRuleParams) {
      ActiveRuleKey activeRuleKey = activeRuleUuidsByKey.get(activeRuleParamDto.getActiveRuleUuid());
      activeRuleParamsByActiveRuleKey.put(activeRuleKey, activeRuleParamDto);
    }

    return activeRules.stream()
      .map(activeRule -> buildActiveRuleResponse(activeRule, activeRuleParamsByActiveRuleKey.get(activeRule.getKey())))
      .toList();
  }

  private static Rules.Active buildActiveRuleResponse(OrgActiveRuleDto activeRule, List<ActiveRuleParamDto> parameters) {
    Rules.Active.Builder builder = Rules.Active.newBuilder();
    builder.setQProfile(activeRule.getOrgProfileUuid());
    String inheritance = activeRule.getInheritance();
    builder.setInherit(inheritance != null ? inheritance : ActiveRuleInheritance.NONE.name());
    builder.setSeverity(activeRule.getSeverityString());
    builder.setPrioritizedRule(activeRule.isPrioritizedRule());
    builder.setCreatedAt(DateUtils.formatDateTime(activeRule.getCreatedAt()));
    builder.setUpdatedAt(DateUtils.formatDateTime(activeRule.getUpdatedAt()));
    builder.setImpacts(mapImpacts(activeRule.getImpacts()));
    Rules.Active.Param.Builder paramBuilder = Rules.Active.Param.newBuilder();
    for (ActiveRuleParamDto parameter : parameters) {
      builder.addParams(paramBuilder.clear()
        .setKey(parameter.getKey())
        .setValue(nullToEmpty(parameter.getValue())));
    }

    return builder.build();
  }

  private void writeProfile(Rules.QProfiles.Builder profilesResponse, QProfileDto profile) {
    Rules.QProfile.Builder profileResponse = Rules.QProfile.newBuilder();
    ofNullable(profile.getName()).ifPresent(profileResponse::setName);

    if (profile.getLanguage() != null) {
      profileResponse.setLang(profile.getLanguage());
      Language language = languages.get(profile.getLanguage());
      String langName = language == null ? profile.getLanguage() : language.getName();
      profileResponse.setLangName(langName);
    }
    ofNullable(profile.getParentKee()).ifPresent(profileResponse::setParent);

    profilesResponse.putQProfiles(profile.getKee(), profileResponse.build());
  }

  public Rules.Rule formatRule(DbSession dbSession, SearchResult searchResult) {
    RuleDto rule = searchResult.getRules().get(0);
    return mapper.toWsRule(rule, searchResult, Collections.emptySet(),
      ruleWsSupport.getUsersByUuid(dbSession, searchResult.getRules()), emptyMap());
  }

  public static Rules.Impacts mapImpacts(Map<SoftwareQuality, Severity> impacts) {
    Rules.Impacts.Builder impactsBuilder = Rules.Impacts.newBuilder();
    impacts.forEach((quality, severity) -> impactsBuilder.addImpacts(Common.Impact.newBuilder()
      .setSoftwareQuality(Common.SoftwareQuality.valueOf(quality.name()))
      .setSeverity(ImpactFormatter.mapImpactSeverity(severity))));
    return impactsBuilder.build();
  }

  static class SearchResult {
    private List<RuleDto> rules;
    private final ListMultimap<String, RuleParamDto> ruleParamsByRuleUuid;
    private final Map<String, RuleDto> templateRulesByRuleUuid;
    private Long total;
    private Facets facets;

    public SearchResult() {
      this.rules = new ArrayList<>();
      this.ruleParamsByRuleUuid = ArrayListMultimap.create();
      this.templateRulesByRuleUuid = new HashMap<>();
    }

    public List<RuleDto> getRules() {
      return rules;
    }

    public SearchResult setRules(List<RuleDto> rules) {
      this.rules = rules;
      return this;
    }

    public ListMultimap<String, RuleParamDto> getRuleParamsByRuleUuid() {
      return ruleParamsByRuleUuid;
    }

    public SearchResult setRuleParameters(List<RuleParamDto> ruleParams) {
      ruleParamsByRuleUuid.clear();
      for (RuleParamDto ruleParam : ruleParams) {
        ruleParamsByRuleUuid.put(ruleParam.getRuleUuid(), ruleParam);
      }
      return this;
    }

    public Map<String, RuleDto> getTemplateRulesByRuleUuid() {
      return templateRulesByRuleUuid;
    }

    public SearchResult setTemplateRules(List<RuleDto> templateRules) {
      templateRulesByRuleUuid.clear();
      for (RuleDto templateRule : templateRules) {
        templateRulesByRuleUuid.put(templateRule.getUuid(), templateRule);
      }
      return this;
    }

    public Long getTotal() {
      return total;
    }

    public SearchResult setTotal(Long total) {
      this.total = total;
      return this;
    }

    @CheckForNull
    public Facets getFacets() {
      return facets;
    }

    public SearchResult setFacets(Facets facets) {
      this.facets = facets;
      return this;
    }
  }
}

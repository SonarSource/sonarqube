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
package org.sonar.server.rule;

import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;

import static java.util.Optional.ofNullable;

public class ActiveRuleService {

  private final DbClient dbClient;
  private final Languages languages;

  public ActiveRuleService(DbClient dbClient, Languages languages) {
    this.dbClient = dbClient;
    this.languages = languages;
  }

  public List<ActiveRuleRestReponse.ActiveRule> buildDefaultActiveRules() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<QProfileDto> qualityProfiles = dbClient.qualityProfileDao().selectDefaultProfiles(dbSession, getLanguageKeys());
      return loadActiveRules(qualityProfiles, dbSession);
    }
  }

  public List<ActiveRuleRestReponse.ActiveRule> buildActiveRules(String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<QProfileDto> qualityProfiles = getQualityProfiles(dbSession, projectUuid);
      return loadActiveRules(qualityProfiles, dbSession);
    }
  }

  private Set<String> getLanguageKeys() {
    return Arrays.stream(languages.all()).map(Language::getKey).collect(Collectors.toSet());
  }

  private List<ActiveRuleRestReponse.ActiveRule> loadActiveRules(List<QProfileDto> qualityProfiles, DbSession dbSession) {

    Map<String, QProfileDto> profilesByUuids = qualityProfiles.stream().collect(Collectors.toMap(QProfileDto::getKee, Function.identity()));

    Map<String, List<RuleParamDto>> ruleParamsByRuleUuid = dbClient.ruleDao().selectAllRuleParams(dbSession).stream()
      .collect(Collectors.groupingBy(RuleParamDto::getRuleUuid));

    Map<String, List<ActiveRuleParamDto>> activeRuleParamsByActiveRuleUuid = dbClient.activeRuleDao().selectAllParamsByProfileUuids(dbSession, profilesByUuids.keySet()).stream()
      .collect(Collectors.groupingBy(ActiveRuleParamDto::getActiveRuleUuid));

    List<OrgActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().selectByProfileUuids(dbSession, profilesByUuids.keySet());
    Map<String, RuleDto> templateRulesByUuid = getTemplateRulesByUuid(dbSession, activeRuleDtos);
    Map<String, List<DeprecatedRuleKeyDto>> deprecatedRuleKeysByRuleUuid = getDeprecatedRuleKeysByRuleUuid(dbSession);

    return activeRuleDtos.stream()
      .map(
        activeRuleDto -> buildActiveRule(activeRuleDto, profilesByUuids, templateRulesByUuid, deprecatedRuleKeysByRuleUuid, ruleParamsByRuleUuid, activeRuleParamsByActiveRuleUuid))
      .toList();
  }

  private static ActiveRuleRestReponse.ActiveRule buildActiveRule(OrgActiveRuleDto activeRuleDto,
    Map<String, QProfileDto> profilesByUuids,
    Map<String, RuleDto> templateRulesByUuid,
    Map<String, List<DeprecatedRuleKeyDto>> deprecatedRuleKeysByRuleUuid,
    Map<String, List<RuleParamDto>> ruleParamsByRuleUuid,
    Map<String, List<ActiveRuleParamDto>> activeRuleParamsByActiveRuleUuid) {
    ActiveRuleRestReponse.Builder activeRuleBuilder = new ActiveRuleRestReponse.Builder();
    activeRuleBuilder.setRuleKey(toRuleKey(activeRuleDto.getRuleKey()));
    activeRuleBuilder.setName(activeRuleDto.getName());
    activeRuleBuilder.setSeverity(activeRuleDto.getSeverityString());
    activeRuleBuilder.setCreatedAt(DateUtils.formatDateTime(activeRuleDto.getCreatedAt()));
    activeRuleBuilder.setUpdatedAt(DateUtils.formatDateTime(activeRuleDto.getUpdatedAt()));
    // same as RuleForIndexingDto mapping
    activeRuleBuilder.setInternalKey(activeRuleDto.getConfigKey());
    activeRuleBuilder.setQProfilKey(profilesByUuids.get(activeRuleDto.getOrgProfileUuid()).getKee());
    activeRuleBuilder.setLanguage(activeRuleDto.getLanguage());
    Optional<RuleDto> templateRule = ofNullable(templateRulesByUuid.get(activeRuleDto.getTemplateUuid()));
    templateRule.ifPresent(template -> activeRuleBuilder.setTemplateRuleKey(template.getKey().toString()));

    List<ActiveRuleRestReponse.RuleKey> deprecatedKeys = buildDeprecatedKeys(activeRuleDto, deprecatedRuleKeysByRuleUuid);
    activeRuleBuilder.addAllDeprecatedKeys(deprecatedKeys);

    List<RuleParamDto> ruleParams = ruleParamsByRuleUuid.get(activeRuleDto.getRuleUuid());
    List<ActiveRuleParamDto> activeRuleParams = ofNullable(activeRuleParamsByActiveRuleUuid.get(activeRuleDto.getUuid())).orElse(List.of());
    List<ActiveRuleRestReponse.Param> params = buildParams(ruleParams, activeRuleParams);
    activeRuleBuilder.setParams(params);

    activeRuleBuilder.setImpacts(activeRuleDto.getImpacts());

    return activeRuleBuilder.build();
  }

  private static List<ActiveRuleRestReponse.Param> buildParams(@Nullable List<RuleParamDto> ruleParams, List<ActiveRuleParamDto> activeRuleParams) {
    if (ruleParams == null) {
      return List.of();
    }
    return ruleParams.stream()
      .map(parameter -> {
        Optional<ActiveRuleParamDto> activeRuleParamDto = activeRuleParams.stream()
          .filter(arp -> arp.getRulesParameterUuid().equals(parameter.getUuid()))
          .findAny();
        String paramValue = activeRuleParamDto.map(ActiveRuleParamDto::getValue).orElse(parameter.getDefaultValue());
        return new ActiveRuleRestReponse.Param(parameter.getName(), Strings.nullToEmpty(paramValue));
      })
      .toList();
  }

  private static List<ActiveRuleRestReponse.RuleKey> buildDeprecatedKeys(OrgActiveRuleDto activeRuleDto, Map<String, List<DeprecatedRuleKeyDto>> deprecatedRuleKeysByRuleUuid) {
    List<DeprecatedRuleKeyDto> deprecatedRuleKeyDtos = deprecatedRuleKeysByRuleUuid.getOrDefault(activeRuleDto.getUuid(), List.of());
    return deprecatedRuleKeyDtos.stream()
      .map(ActiveRuleService::toRuleKey)
      .toList();
  }

  private List<QProfileDto> getQualityProfiles(DbSession dbSession, String projectUuid) {
    List<QProfileDto> defaultProfiles = dbClient.qualityProfileDao().selectDefaultProfiles(dbSession, getLanguageKeys());
    Map<String, QProfileDto> projectProfiles = dbClient.qualityProfileDao().selectAssociatedToProjectAndLanguages(dbSession, projectUuid, getLanguageKeys())
      .stream().collect(Collectors.toMap(QProfileDto::getLanguage, Function.identity()));

    return defaultProfiles.stream()
      .map(defaultProfile -> projectProfiles.getOrDefault(defaultProfile.getLanguage(), defaultProfile))
      .toList();
  }

  private Map<String, RuleDto> getTemplateRulesByUuid(DbSession dbSession, List<OrgActiveRuleDto> activeRuleDtos) {
    Set<String> templateRuleUuids = activeRuleDtos.stream().map(OrgActiveRuleDto::getTemplateUuid).filter(Objects::nonNull).collect(Collectors.toSet());
    return dbClient.ruleDao().selectByUuids(dbSession, templateRuleUuids).stream().collect(Collectors.toMap(RuleDto::getUuid, p -> p));
  }

  private static ActiveRuleRestReponse.RuleKey toRuleKey(RuleKey ruleKey) {
    return new ActiveRuleRestReponse.RuleKey(ruleKey.repository(), ruleKey.rule());
  }

  private static ActiveRuleRestReponse.RuleKey toRuleKey(DeprecatedRuleKeyDto r) {
    return new ActiveRuleRestReponse.RuleKey(r.getOldRepositoryKey(), r.getOldRuleKey());
  }

  private Map<String, List<DeprecatedRuleKeyDto>> getDeprecatedRuleKeysByRuleUuid(DbSession dbSession) {
    return dbClient.ruleDao().selectAllDeprecatedRuleKeys(dbSession).stream()
      .collect(Collectors.groupingBy(DeprecatedRuleKeyDto::getRuleUuid));
  }

}

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
package org.sonar.server.qualityprofile.builtin;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.DefaultQProfileDto;
import org.sonar.db.qualityprofile.OrgQProfileDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.ServerRuleFinder;
import org.sonar.server.util.TypeValidations;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

public class BuiltInQProfileInsertImpl implements BuiltInQProfileInsert {
  private final DbClient dbClient;
  private final ServerRuleFinder ruleFinder;
  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final TypeValidations typeValidations;
  private final ActiveRuleIndexer activeRuleIndexer;
  private RuleRepository ruleRepository;
  private final SonarQubeVersion sonarQubeVersion;

  public BuiltInQProfileInsertImpl(DbClient dbClient, ServerRuleFinder ruleFinder, System2 system2, UuidFactory uuidFactory,
    TypeValidations typeValidations, ActiveRuleIndexer activeRuleIndexer, SonarQubeVersion sonarQubeVersion) {
    this.dbClient = dbClient;
    this.ruleFinder = ruleFinder;
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.typeValidations = typeValidations;
    this.activeRuleIndexer = activeRuleIndexer;
    this.sonarQubeVersion = sonarQubeVersion;
  }

  @Override
  public void create(DbSession dbSession, DbSession batchDbSession, BuiltInQProfile builtInQProfile) {
    initRuleRepository(batchDbSession);

    Date now = new Date(system2.now());
    RulesProfileDto ruleProfile = insertRulesProfile(batchDbSession, builtInQProfile, now);

    List<ActiveRuleChange> changes = builtInQProfile.getActiveRules().stream()
      .map(activeRule -> insertActiveRule(batchDbSession, ruleProfile, activeRule, now.getTime()))
      .toList();

    List<QProfileChangeDto> changeDtos = changes.stream()
      .map(ActiveRuleChange::toSystemChangedDto)
      .peek(dto -> dto.setSqVersion(sonarQubeVersion.toString()))
      .toList();
    dbClient.qProfileChangeDao().bulkInsert(batchDbSession, changeDtos);

    associateToOrganizations(dbSession, batchDbSession, builtInQProfile, ruleProfile);

    activeRuleIndexer.commitAndIndex(batchDbSession, changes);
  }


  private void associateToOrganizations(DbSession dbSession, DbSession batchDbSession, BuiltInQProfile builtIn, RulesProfileDto rulesProfileDto) {
    List<String> orgUuids = dbClient.organizationDao().selectAllUuids(dbSession);
    Set<String> orgUuidsWithoutDefault = dbClient.defaultQProfileDao().selectUuidsOfOrganizationsWithoutDefaultProfile(dbSession, builtIn.getLanguage());

    List<DefaultQProfileDto> defaults = new ArrayList<>();
    orgUuids.forEach(orgUuid -> {
      OrgQProfileDto dto = new OrgQProfileDto()
              .setOrganizationUuid(orgUuid)
              .setRulesProfileUuid(rulesProfileDto.getUuid())
              .setUuid(uuidFactory.create());

      if (builtIn.isDefault() && orgUuidsWithoutDefault.contains(orgUuid)) {
        // rows of table default_qprofiles must be inserted after
        // in order to benefit from batch SQL inserts
        defaults.add(new DefaultQProfileDto()
                .setQProfileUuid(dto.getUuid())
                .setOrganizationUuid(orgUuid)
                .setLanguage(builtIn.getLanguage()));
      }

      dbClient.qualityProfileDao().insert(batchDbSession, dto);
    });

    defaults.forEach(defaultQProfileDto -> dbClient.defaultQProfileDao().insertOrUpdate(dbSession, defaultQProfileDto));
  }

  private void initRuleRepository(DbSession dbSession) {
    if (ruleRepository == null) {
      ruleRepository = new RuleRepository(dbClient, dbSession, ruleFinder);
    }
  }

  private RulesProfileDto insertRulesProfile(DbSession dbSession, BuiltInQProfile builtIn, Date now) {
    RulesProfileDto dto = new RulesProfileDto()
      .setUuid(uuidFactory.create())
      .setName(builtIn.getName())
      .setLanguage(builtIn.getLanguage())
      .setIsBuiltIn(true)
      .setRulesUpdatedAtAsDate(now);
    dbClient.qualityProfileDao().insert(dbSession, dto);
    return dto;
  }

  private ActiveRuleChange insertActiveRule(DbSession batchDbSession, RulesProfileDto rulesProfileDto, BuiltInQProfile.ActiveRule activeRule, long now) {
    RuleKey ruleKey = activeRule.getRuleKey();
    RuleDto ruleDefinitionDto = ruleRepository.getDefinition(ruleKey)
      .orElseThrow(() -> new IllegalStateException("RuleDefinition not found for key " + ruleKey));

    ActiveRuleDto dto = new ActiveRuleDto();
    dto.setProfileUuid(rulesProfileDto.getUuid());
    dto.setRuleUuid(ruleDefinitionDto.getUuid());
    dto.setKey(ActiveRuleKey.of(rulesProfileDto, ruleDefinitionDto.getKey()));
    dto.setSeverity(firstNonNull(activeRule.getSeverity(), ruleDefinitionDto.getSeverityString()));
    dto.setUpdatedAt(now);
    dto.setCreatedAt(now);
    dbClient.activeRuleDao().insert(batchDbSession, dto);

    List<ActiveRuleParamDto> paramDtos = insertActiveRuleParams(batchDbSession, activeRule, dto);

    ActiveRuleChange change = new ActiveRuleChange(ActiveRuleChange.Type.ACTIVATED, dto, ruleDefinitionDto);
    change.setSeverity(dto.getSeverityString());
    paramDtos.forEach(paramDto -> change.setParameter(paramDto.getKey(), paramDto.getValue()));
    return change;
  }

  private List<ActiveRuleParamDto> insertActiveRuleParams(DbSession session, BuiltInQProfile.ActiveRule activeRule, ActiveRuleDto activeRuleDto) {
    Map<String, String> valuesByParamKey = activeRule.getParams().stream()
      .collect(Collectors.toMap(BuiltInQualityProfilesDefinition.OverriddenParam::key, BuiltInQualityProfilesDefinition.OverriddenParam::overriddenValue));
    List<ActiveRuleParamDto> rules = ruleRepository.getRuleParams(activeRule.getRuleKey()).stream()
      .map(param -> createParamDto(param, Optional.ofNullable(valuesByParamKey.get(param.getName())).orElse(param.getDefaultValue())))
      .filter(Objects::nonNull)
      .toList();

    rules.forEach(paramDto -> dbClient.activeRuleDao().insertParam(session, activeRuleDto, paramDto));
    return rules;
  }

  @CheckForNull
  private ActiveRuleParamDto createParamDto(RuleParamDto param, @Nullable String value) {
    if (value == null) {
      return null;
    }
    ActiveRuleParamDto paramDto = ActiveRuleParamDto.createFor(param);
    paramDto.setValue(validateParam(param, value));
    return paramDto;
  }

  private String validateParam(RuleParamDto ruleParam, String value) {
    RuleParamType ruleParamType = RuleParamType.parse(ruleParam.getType());
    if (ruleParamType.multiple()) {
      List<String> values = newArrayList(Splitter.on(",").split(value));
      typeValidations.validate(values, ruleParamType.type(), ruleParamType.values());
    } else {
      typeValidations.validate(value, ruleParamType.type(), ruleParamType.values());
    }
    return value;
  }

  private static class RuleRepository {
    private final Map<RuleKey, Set<RuleParamDto>> params;
    private final ServerRuleFinder ruleFinder;

    private RuleRepository(DbClient dbClient, DbSession session, ServerRuleFinder ruleFinder) {
      this.ruleFinder = ruleFinder;
      this.params = new HashMap<>();

      for (RuleParamDto ruleParam : dbClient.ruleDao().selectAllRuleParams(session)) {
        Optional<RuleKey> ruleKey = ruleFinder.findDtoByUuid(ruleParam.getRuleUuid())
          .map(r -> RuleKey.of(r.getRepositoryKey(), r.getRuleKey()));

        if (ruleKey.isPresent()) {
          params.computeIfAbsent(ruleKey.get(), r -> new HashSet<>()).add(ruleParam);
        }
      }
    }

    private Optional<RuleDto> getDefinition(RuleKey ruleKey) {
      return ruleFinder.findDtoByKey(requireNonNull(ruleKey, "RuleKey can't be null"));
    }

    private Set<RuleParamDto> getRuleParams(RuleKey ruleKey) {
      return params.getOrDefault(requireNonNull(ruleKey, "RuleKey can't be null"), emptySet());
    }
  }
}

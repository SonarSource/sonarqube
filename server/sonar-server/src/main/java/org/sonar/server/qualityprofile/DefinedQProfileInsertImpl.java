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
package org.sonar.server.qualityprofile;

import com.google.common.base.Splitter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.util.TypeValidations;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

public class DefinedQProfileInsertImpl implements DefinedQProfileInsert {
  private final DbClient dbClient;
  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final TypeValidations typeValidations;
  private RegisterQualityProfiles.RuleRepository ruleRepository;

  public DefinedQProfileInsertImpl(DbClient dbClient, System2 system2, UuidFactory uuidFactory, TypeValidations typeValidations) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.typeValidations = typeValidations;
  }

  @Override
  public void create(DbSession session, DefinedQProfile definedQProfile, OrganizationDto organization, List<ActiveRuleChange> changes) {
    initRuleRepository(session);

    checkArgument(definedQProfile.getParentQProfileName() == null,
      "Inheritance of Quality Profiles is not supported yet");
    // Optional.ofNullable(definedQProfile.getParentQProfileName())
    // .map(parentQProfileName -> dbClient.qualityProfileDao().selectByNameAndLanguage(organization, parentQProfileName.getName(),
    // parentQProfileName.getLanguage(), session))
    // .map(parentQualityProfileDto -> dbClient.activeRuleDao().selectByRuleIds());

    Date now = new Date(system2.now());
    QualityProfileDto profileDto = insertQualityProfile(session, definedQProfile, organization, now);

    List<ActiveRuleChange> localChanges = definedQProfile.getActiveRules()
      .stream()
      .map(activeRule -> insertActiveRule(session, profileDto, activeRule, now.getTime()))
      .collect(MoreCollectors.toList());

    localChanges.forEach(change -> dbClient.qProfileChangeDao().insert(session, change.toDto(null)));

    insertTemplate(session, definedQProfile, organization);

    changes.addAll(localChanges);
  }

  private void initRuleRepository(DbSession session) {
    if (ruleRepository == null) {
      ruleRepository = new RegisterQualityProfiles.RuleRepository(dbClient, session);
    }
  }

  private QualityProfileDto insertQualityProfile(DbSession session, DefinedQProfile definedQProfile, OrganizationDto organization, Date now) {
    QualityProfileDto profileDto = QualityProfileDto.createFor(uuidFactory.create())
      .setName(definedQProfile.getName())
      .setOrganizationUuid(organization.getUuid())
      .setLanguage(definedQProfile.getLanguage())
      .setDefault(definedQProfile.isDefault())
      .setRulesUpdatedAtAsDate(now);
    dbClient.qualityProfileDao().insert(session, profileDto);
    return profileDto;
  }

  private ActiveRuleChange insertActiveRule(DbSession session, QualityProfileDto profileDto, org.sonar.api.rules.ActiveRule activeRule, long now) {
    RuleKey ruleKey = RuleKey.of(activeRule.getRepositoryKey(), activeRule.getRuleKey());
    RuleDefinitionDto ruleDefinitionDto = ruleRepository.getDefinition(ruleKey)
      .orElseThrow(() -> new IllegalStateException("RuleDefinition not found for key " + ruleKey));

    ActiveRuleDto dto = ActiveRuleDto.createFor(profileDto, ruleDefinitionDto);
    dto.setSeverity(
      firstNonNull(
        activeRule.getSeverity().name(),
        // context.parentSeverity(),
        ruleDefinitionDto.getSeverityString()));
    // ActiveRule.Inheritance inheritance = activeRule.getInheritance();
    // if (inheritance != null) {
    // activeRule.setInheritance(inheritance.name());
    // }
    dto.setUpdatedAt(now);
    dto.setCreatedAt(now);
    dbClient.activeRuleDao().insert(session, dto);

    List<ActiveRuleParamDto> paramDtos = insertActiveRuleParams(session, activeRule, ruleKey, dto);

    ActiveRuleChange change = ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(profileDto.getKey(), ruleKey));
    change.setSeverity(dto.getSeverityString());
    paramDtos.forEach(paramDto -> change.setParameter(paramDto.getKey(), paramDto.getValue()));
    return change;
  }

  private List<ActiveRuleParamDto> insertActiveRuleParams(DbSession session, org.sonar.api.rules.ActiveRule activeRule, RuleKey ruleKey, ActiveRuleDto activeRuleDto) {
    Map<String, String> valuesByParamKey = activeRule.getActiveRuleParams()
      .stream()
      .collect(MoreCollectors.uniqueIndex(ActiveRuleParam::getParamKey, ActiveRuleParam::getValue));
    return ruleRepository.getRuleParams(ruleKey)
      .stream()
      .map(param -> {
        String activeRuleValue = valuesByParamKey.get(param.getName());
        return createParamDto(param, activeRuleValue == null ? param.getDefaultValue() : activeRuleValue);
      })
      .filter(Objects::nonNull)
      .peek(paramDto -> dbClient.activeRuleDao().insertParam(session, activeRuleDto, paramDto))
      .collect(MoreCollectors.toList());
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

  @CheckForNull
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

  private void insertTemplate(DbSession session, DefinedQProfile qualityProfile, OrganizationDto organization) {
    LoadedTemplateDto template = new LoadedTemplateDto(organization.getUuid(), qualityProfile.getLoadedTemplateType());
    dbClient.loadedTemplateDao().insert(template, session);
  }
}

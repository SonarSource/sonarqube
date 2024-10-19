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
package org.sonar.server.rule.registration;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.rule.PluginRuleUpdate;
import org.sonar.server.rule.RuleDescriptionSectionsGeneratorResolver;

import static com.google.common.collect.Sets.difference;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * The class detects changes between the rule definition coming from plugins during startup and rule from database.
 * In case any changes are detected the rule is updated with the new information from plugin.
 */
public class StartupRuleUpdater {

  private static final Logger LOG = Loggers.get(StartupRuleUpdater.class);

  private final DbClient dbClient;
  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final RuleDescriptionSectionsGeneratorResolver sectionsGeneratorResolver;

  public StartupRuleUpdater(DbClient dbClient, System2 system2, UuidFactory uuidFactory,
    RuleDescriptionSectionsGeneratorResolver sectionsGeneratorResolver) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.sectionsGeneratorResolver = sectionsGeneratorResolver;
  }

  /**
   * Returns true in case there was any change detected between rule in the database and rule from the plugin.
   */
  RuleChange findChangesAndUpdateRule(RulesDefinition.Rule ruleDef, RuleDto ruleDto) {
    RuleChange ruleChange = new RuleChange(ruleDto);
    boolean ruleMerged = mergeRule(ruleDef, ruleDto, ruleChange);
    boolean debtDefinitionsMerged = mergeDebtDefinitions(ruleDef, ruleDto);
    boolean tagsMerged = mergeTags(ruleDef, ruleDto);
    boolean securityStandardsMerged = mergeSecurityStandards(ruleDef, ruleDto);
    boolean educationPrinciplesMerged = mergeEducationPrinciples(ruleDef, ruleDto);
    ruleChange.ruleDefinitionChanged = ruleMerged || debtDefinitionsMerged || tagsMerged || securityStandardsMerged || educationPrinciplesMerged;
    return ruleChange;
  }

  void updateDeprecatedKeys(RulesRegistrationContext context, RulesDefinition.Rule ruleDef, RuleDto rule, DbSession dbSession) {
    Set<SingleDeprecatedRuleKey> deprecatedRuleKeysFromDefinition = SingleDeprecatedRuleKey.from(ruleDef);
    Set<SingleDeprecatedRuleKey> deprecatedRuleKeysFromDB = context.getDBDeprecatedKeysFor(rule);

    // DeprecatedKeys that must be deleted
    List<String> uuidsToBeDeleted = difference(deprecatedRuleKeysFromDB, deprecatedRuleKeysFromDefinition).stream()
      .map(SingleDeprecatedRuleKey::getUuid)
      .toList();

    dbClient.ruleDao().deleteDeprecatedRuleKeys(dbSession, uuidsToBeDeleted);

    // DeprecatedKeys that must be created
    Sets.SetView<SingleDeprecatedRuleKey> deprecatedRuleKeysToBeCreated = difference(deprecatedRuleKeysFromDefinition, deprecatedRuleKeysFromDB);

    deprecatedRuleKeysToBeCreated
      .forEach(r -> dbClient.ruleDao().insert(dbSession, new DeprecatedRuleKeyDto()
        .setUuid(uuidFactory.create())
        .setRuleUuid(rule.getUuid())
        .setOldRepositoryKey(r.getOldRepositoryKey())
        .setOldRuleKey(r.getOldRuleKey())
        .setCreatedAt(system2.now())));
  }

  private boolean mergeRule(RulesDefinition.Rule def, RuleDto dto, RuleChange ruleChange) {
    boolean changed = false;
    if (!Objects.equals(dto.getName(), def.name())) {
      dto.setName(def.name());
      changed = true;
    }
    if (mergeDescription(def, dto)) {
      changed = true;
    }
    if (!Objects.equals(dto.getPluginKey(), def.pluginKey())) {
      dto.setPluginKey(def.pluginKey());
      changed = true;
    }
    if (!Objects.equals(dto.getConfigKey(), def.internalKey())) {
      dto.setConfigKey(def.internalKey());
      changed = true;
    }
    String severity = def.severity();
    if (!Objects.equals(dto.getSeverityString(), severity)) {
      dto.setSeverity(severity);
      changed = true;
    }
    boolean isTemplate = def.template();
    if (isTemplate != dto.isTemplate()) {
      dto.setIsTemplate(isTemplate);
      changed = true;
    }
    if (def.status() != dto.getStatus()) {
      dto.setStatus(def.status());
      changed = true;
    }
    if (!Objects.equals(dto.getScope().name(), def.scope().name())) {
      dto.setScope(RuleDto.Scope.valueOf(def.scope().name()));
      changed = true;
    }
    if (!Objects.equals(dto.getLanguage(), def.repository().language())) {
      dto.setLanguage(def.repository().language());
      changed = true;
    }
    RuleType type = RuleType.valueOf(def.type().name());
    if (!Objects.equals(dto.getType(), type.getDbConstant())) {
      dto.setType(type);
      changed = true;
    }
    changed |= mergeCleanCodeAttribute(def, dto, ruleChange);
    changed |= mergeImpacts(def, dto, ruleChange);
    if (dto.isAdHoc()) {
      dto.setIsAdHoc(false);
      changed = true;
    }
    return changed;
  }

  private static boolean mergeCleanCodeAttribute(RulesDefinition.Rule def, RuleDto dto, RuleChange ruleChange) {
    if (dto.getEnumType() == RuleType.SECURITY_HOTSPOT) {
      return false;
    }
    boolean changed = false;
    CleanCodeAttribute defCleanCodeAttribute = def.cleanCodeAttribute();
    if (!Objects.equals(dto.getCleanCodeAttribute(), defCleanCodeAttribute) && (defCleanCodeAttribute != null)) {
      ruleChange.addCleanCodeAttributeChange(dto.getCleanCodeAttribute(), defCleanCodeAttribute);
      dto.setCleanCodeAttribute(defCleanCodeAttribute);
      changed = true;
    }
    // apply non-nullable default
    if (dto.getCleanCodeAttribute() == null) {
      dto.setCleanCodeAttribute(CleanCodeAttribute.defaultCleanCodeAttribute());
      changed = true;
    }
    return changed;
  }

  boolean mergeImpacts(RulesDefinition.Rule def, RuleDto dto, RuleChange ruleChange) {
    if (dto.getEnumType() == RuleType.SECURITY_HOTSPOT) {
      return false;
    }

    Map<SoftwareQuality, Severity> impactsFromPlugin = def.defaultImpacts();
    Map<SoftwareQuality, Severity> impactsFromDb = dto.getDefaultImpacts().stream().collect(Collectors.toMap(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity));

    if (impactsFromPlugin.isEmpty()) {
      throw new IllegalStateException("There should be at least one impact defined for the rule " + def.key());
    }

    if (!Objects.equals(impactsFromDb, impactsFromPlugin)) {
      dto.replaceAllDefaultImpacts(impactsFromPlugin.entrySet()
        .stream()
        .map(e -> new ImpactDto().setSoftwareQuality(e.getKey()).setSeverity(e.getValue()))
        .collect(Collectors.toSet()));
      ruleChange.addImpactsChange(removeDuplicatedImpacts(impactsFromDb, impactsFromPlugin), removeDuplicatedImpacts(impactsFromPlugin, impactsFromDb));

      return true;
    }

    return false;
  }

  /**
   * Returns a new map that contains only the impacts from the first map that are not present in the map passed as a second argument.
   */
  private static Map<SoftwareQuality, Severity> removeDuplicatedImpacts(Map<SoftwareQuality, Severity> impactsA, Map<SoftwareQuality, Severity> impactsB) {
    return impactsA.entrySet().stream()
      .filter(entry -> !impactsB.containsKey(entry.getKey()) || !impactsB.get(entry.getKey()).equals(entry.getValue()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static boolean mergeEducationPrinciples(RulesDefinition.Rule ruleDef, RuleDto dto) {
    boolean changed = false;
    if (dto.getEducationPrinciples().size() != ruleDef.educationPrincipleKeys().size() ||
      !dto.getEducationPrinciples().containsAll(ruleDef.educationPrincipleKeys())) {
      dto.setEducationPrinciples(ruleDef.educationPrincipleKeys());
      changed = true;
    }
    return changed;
  }

  private static boolean mergeTags(RulesDefinition.Rule ruleDef, RuleDto dto) {
    boolean changed = false;

    if (RuleStatus.REMOVED == ruleDef.status()) {
      dto.setSystemTags(emptySet());
      changed = true;
    } else if (dto.getSystemTags().size() != ruleDef.tags().size() || !dto.getSystemTags().containsAll(ruleDef.tags())) {
      dto.setSystemTags(ruleDef.tags());
      changed = true;
    }
    return changed;
  }

  private static boolean mergeSecurityStandards(RulesDefinition.Rule ruleDef, RuleDto dto) {
    boolean changed = false;
    Set<String> securityStandards = dto.getSecurityStandards();

    if (RuleStatus.REMOVED == ruleDef.status()) {
      dto.setSecurityStandards(emptySet());
      changed = true;
    } else if (securityStandards.size() != ruleDef.securityStandards().size() || !securityStandards.containsAll(ruleDef.securityStandards())) {
      dto.setSecurityStandards(ruleDef.securityStandards());
      changed = true;
    }
    return changed;
  }

  private static boolean containsHtmlDescription(RulesDefinition.Rule rule) {
    return isNotEmpty(rule.htmlDescription()) || !rule.ruleDescriptionSections().isEmpty();
  }

  private static boolean ruleDescriptionSectionsUnchanged(RuleDto ruleDto, Set<RuleDescriptionSectionDto> newRuleDescriptionSectionDtos) {
    if (ruleDto.getRuleDescriptionSectionDtos().size() != newRuleDescriptionSectionDtos.size()) {
      return false;
    }
    return ruleDto.getRuleDescriptionSectionDtos().stream()
      .allMatch(sectionDto -> contains(newRuleDescriptionSectionDtos, sectionDto));
  }

  private static boolean contains(Set<RuleDescriptionSectionDto> sectionDtos, RuleDescriptionSectionDto sectionDto) {
    return sectionDtos.stream()
      .filter(s -> s.getKey().equals(sectionDto.getKey()) && s.getContent().equals(sectionDto.getContent()))
      .anyMatch(s -> Objects.equals(s.getContext(), sectionDto.getContext()));
  }

  private static boolean mergeDebtDefinitions(RuleDto dto, @Nullable String remediationFunction,
    @Nullable String remediationCoefficient, @Nullable String remediationOffset, @Nullable String gapDescription) {
    boolean changed = false;

    if (!Objects.equals(dto.getDefRemediationFunction(), remediationFunction)) {
      dto.setDefRemediationFunction(remediationFunction);
      changed = true;
    }
    if (!Objects.equals(dto.getDefRemediationGapMultiplier(), remediationCoefficient)) {
      dto.setDefRemediationGapMultiplier(remediationCoefficient);
      changed = true;
    }
    if (!Objects.equals(dto.getDefRemediationBaseEffort(), remediationOffset)) {
      dto.setDefRemediationBaseEffort(remediationOffset);
      changed = true;
    }
    if (!Objects.equals(dto.getGapDescription(), gapDescription)) {
      dto.setGapDescription(gapDescription);
      changed = true;
    }
    return changed;
  }

  private static boolean mergeDebtDefinitions(RulesDefinition.Rule def, RuleDto dto) {
    // Debt definitions are set to null if the sub-characteristic and the remediation function are null
    DebtRemediationFunction debtRemediationFunction = def.debtRemediationFunction();
    boolean hasDebt = debtRemediationFunction != null;
    if (hasDebt) {
      return mergeDebtDefinitions(dto,
        debtRemediationFunction.type().name(),
        debtRemediationFunction.gapMultiplier(),
        debtRemediationFunction.baseEffort(),
        def.gapDescription());
    }
    return mergeDebtDefinitions(dto, null, null, null, null);
  }

  private boolean mergeDescription(RulesDefinition.Rule rule, RuleDto ruleDto) {
    Set<RuleDescriptionSectionDto> newRuleDescriptionSectionDtos = sectionsGeneratorResolver.generateFor(rule);
    if (ruleDescriptionSectionsUnchanged(ruleDto, newRuleDescriptionSectionDtos)) {
      return false;
    }
    ruleDto.replaceRuleDescriptionSectionDtos(newRuleDescriptionSectionDtos);
    if (containsHtmlDescription(rule)) {
      ruleDto.setDescriptionFormat(RuleDto.Format.HTML);
      return true;
    } else if (isNotEmpty(rule.markdownDescription())) {
      ruleDto.setDescriptionFormat(RuleDto.Format.MARKDOWN);
      return true;
    }
    return false;
  }

  void mergeParams(RulesRegistrationContext context, RulesDefinition.Rule ruleDef, RuleDto rule, DbSession session) {
    List<RuleParamDto> paramDtos = context.getRuleParametersFor(rule.getUuid());
    Map<String, RuleParamDto> existingParamsByName = new HashMap<>();

    Profiler profiler = Profiler.create(LOG);
    for (RuleParamDto paramDto : paramDtos) {
      RulesDefinition.Param paramDef = ruleDef.param(paramDto.getName());
      if (paramDef == null) {
        profiler.start();
        dbClient.activeRuleDao().deleteParamsByRuleParam(session, paramDto);
        profiler.stopDebug(format("Propagate deleted param with name %s to active rules of rule %s", paramDto.getName(), rule.getKey()));
        dbClient.ruleDao().deleteRuleParam(session, paramDto.getUuid());
      } else {
        if (mergeParam(paramDto, paramDef)) {
          dbClient.ruleDao().updateRuleParam(session, rule, paramDto);
        }
        existingParamsByName.put(paramDto.getName(), paramDto);
      }
    }

    // Create newly parameters
    for (RulesDefinition.Param param : ruleDef.params()) {
      RuleParamDto paramDto = existingParamsByName.get(param.key());
      if (paramDto != null) {
        continue;
      }
      paramDto = RuleParamDto.createFor(rule)
        .setName(param.key())
        .setDescription(param.description())
        .setDefaultValue(param.defaultValue())
        .setType(param.type().toString());
      dbClient.ruleDao().insertRuleParam(session, rule, paramDto);
      if (StringUtils.isEmpty(param.defaultValue())) {
        continue;
      }
      // Propagate the default value to existing active rule parameters
      profiler.start();
      for (ActiveRuleDto activeRule : dbClient.activeRuleDao().selectByRuleUuid(session, rule.getUuid())) {
        ActiveRuleParamDto activeParam = ActiveRuleParamDto.createFor(paramDto).setValue(param.defaultValue());
        dbClient.activeRuleDao().insertParam(session, activeRule, activeParam);
      }
      profiler.stopDebug(format("Propagate new param with name %s to active rules of rule %s", paramDto.getName(), rule.getKey()));
    }
  }

  private static boolean mergeParam(RuleParamDto paramDto, RulesDefinition.Param paramDef) {
    boolean changed = false;
    if (!Objects.equals(paramDto.getType(), paramDef.type().toString())) {
      paramDto.setType(paramDef.type().toString());
      changed = true;
    }
    if (!Objects.equals(paramDto.getDefaultValue(), paramDef.defaultValue())) {
      paramDto.setDefaultValue(paramDef.defaultValue());
      changed = true;
    }
    if (!Objects.equals(paramDto.getDescription(), paramDef.description())) {
      paramDto.setDescription(paramDef.description());
      changed = true;
    }
    return changed;
  }

  public static class RuleChange {
    private boolean ruleDefinitionChanged = false;
    private final String ruleUuid;
    private PluginRuleUpdate pluginRuleUpdate;

    public RuleChange(RuleDto ruleDto) {
      this.ruleUuid = ruleDto.getUuid();
    }

    private void createPluginRuleUpdateIfNeeded() {
      if (pluginRuleUpdate == null) {
        pluginRuleUpdate = new PluginRuleUpdate();
        pluginRuleUpdate.setRuleUuid(ruleUuid);
      }
    }

    public void addImpactsChange(Map<SoftwareQuality, Severity> oldImpacts, Map<SoftwareQuality, Severity> newImpacts) {
      createPluginRuleUpdateIfNeeded();
      oldImpacts.forEach(pluginRuleUpdate::addOldImpact);
      newImpacts.forEach(pluginRuleUpdate::addNewImpact);
    }

    public void addCleanCodeAttributeChange(@Nullable CleanCodeAttribute oldAttribute, @Nullable CleanCodeAttribute newAttribute) {
      createPluginRuleUpdateIfNeeded();
      pluginRuleUpdate.setOldCleanCodeAttribute(oldAttribute);
      pluginRuleUpdate.setNewCleanCodeAttribute(newAttribute);
    }

    public boolean hasRuleDefinitionChanged() {
      return ruleDefinitionChanged;
    }

    @CheckForNull
    public PluginRuleUpdate getPluginRuleUpdate() {
      return pluginRuleUpdate;
    }
  }
}

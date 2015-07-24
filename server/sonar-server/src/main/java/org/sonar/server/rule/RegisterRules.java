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
package org.sonar.server.rule;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Format;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.debt.CharacteristicDao;
import org.sonar.db.debt.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.startup.RegisterDebtModel;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Register rules at server startup
 */
public class RegisterRules implements Startable {

  private static final Logger LOG = Loggers.get(RegisterRules.class);

  private final RuleDefinitionsLoader defLoader;
  private final RuleActivator ruleActivator;
  private final DbClient dbClient;
  private final Languages languages;
  private final CharacteristicDao characteristicDao;

  /**
   * @param registerDebtModel used only to be started after init of the technical debt model
   */
  public RegisterRules(RuleDefinitionsLoader defLoader, RuleActivator ruleActivator, DbClient dbClient, Languages languages, RegisterDebtModel registerDebtModel) {
    this(defLoader, ruleActivator, dbClient, languages);
  }

  @VisibleForTesting
  RegisterRules(RuleDefinitionsLoader defLoader, RuleActivator ruleActivator,
    DbClient dbClient, Languages languages) {
    this.defLoader = defLoader;
    this.ruleActivator = ruleActivator;
    this.dbClient = dbClient;
    this.languages = languages;
    this.characteristicDao = dbClient.debtCharacteristicDao();
  }

  @Override
  public void start() {
    Profiler profiler = Profiler.create(LOG).startInfo("Register rules");
    DbSession session = dbClient.openSession(false);
    try {
      Map<RuleKey, RuleDto> allRules = loadRules(session);
      Map<String, CharacteristicDto> allCharacteristics = loadCharacteristics(session);

      RulesDefinition.Context context = defLoader.load();
      for (RulesDefinition.ExtendedRepository repoDef : getRepositories(context)) {
        if (languages.get(repoDef.language()) != null) {
          for (RulesDefinition.Rule ruleDef : repoDef.rules()) {
            registerRule(ruleDef, allRules, allCharacteristics, session);
          }
          session.commit();
        }
      }
      List<RuleDto> activeRules = processRemainingDbRules(allRules.values(), session);
      removeActiveRulesOnStillExistingRepositories(session, activeRules, context);
      session.commit();
      profiler.stopDebug();
    } finally {
      session.close();
    }
  }

  @Override
  public void stop() {
    // nothing
  }

  private void registerRule(RulesDefinition.Rule ruleDef, Map<RuleKey, RuleDto> allRules, Map<String, CharacteristicDto> allCharacteristics, DbSession session) {
    RuleKey ruleKey = RuleKey.of(ruleDef.repository().key(), ruleDef.key());

    RuleDto rule = allRules.containsKey(ruleKey) ? allRules.remove(ruleKey) : createRuleDto(ruleDef, session);

    boolean executeUpdate = false;
    if (mergeRule(ruleDef, rule)) {
      executeUpdate = true;
    }

    CharacteristicDto subCharacteristic = characteristic(ruleDef, rule.getSubCharacteristicId(), allCharacteristics);
    if (mergeDebtDefinitions(ruleDef, rule, subCharacteristic)) {
      executeUpdate = true;
    }

    if (mergeTags(ruleDef, rule)) {
      executeUpdate = true;
    }

    if (executeUpdate) {
      dbClient.deprecatedRuleDao().update(session, rule);
    }

    mergeParams(ruleDef, rule, session);
  }

  private Map<RuleKey, RuleDto> loadRules(DbSession session) {
    Map<RuleKey, RuleDto> rules = new HashMap<>();
    for (RuleDto rule : dbClient.deprecatedRuleDao().selectByNonManual(session)) {
      rules.put(rule.getKey(), rule);
    }
    return rules;
  }

  private Map<String, CharacteristicDto> loadCharacteristics(DbSession session) {
    Map<String, CharacteristicDto> characteristics = new HashMap<>();
    for (CharacteristicDto characteristicDto : characteristicDao.selectEnabledCharacteristics(session)) {
      characteristics.put(characteristicDto.getKey(), characteristicDto);
    }
    return characteristics;
  }

  @CheckForNull
  private static CharacteristicDto characteristic(RulesDefinition.Rule ruleDef, @Nullable Integer overridingCharacteristicId, Map<String, CharacteristicDto> allCharacteristics) {
    String subCharacteristic = ruleDef.debtSubCharacteristic();
    String repo = ruleDef.repository().key();
    String ruleKey = ruleDef.key();

    // Rule is not linked to a default characteristic or characteristic has been disabled by user
    if (subCharacteristic == null) {
      return null;
    }
    CharacteristicDto characteristicDto = allCharacteristics.get(subCharacteristic);
    if (characteristicDto == null) {
      // Log a warning only if rule has not been overridden by user
      if (overridingCharacteristicId == null) {
        LOG.warn(String.format("Unknown Characteristic '%s' was found on rule '%s:%s'", subCharacteristic, repo, ruleKey));
      }
    } else if (characteristicDto.getParentId() == null) {
      throw MessageException.of(String.format("Rule '%s:%s' cannot be linked on the root characteristic '%s'", repo, ruleKey, subCharacteristic));
    }
    return characteristicDto;
  }

  private List<RulesDefinition.ExtendedRepository> getRepositories(RulesDefinition.Context context) {
    List<RulesDefinition.ExtendedRepository> repositories = new ArrayList<>();
    for (RulesDefinition.Repository repoDef : context.repositories()) {
      repositories.add(repoDef);
    }
    for (RulesDefinition.ExtendedRepository extendedRepoDef : context.extendedRepositories()) {
      if (context.repository(extendedRepoDef.key()) == null) {
        LOG.warn(String.format("Extension is ignored, repository %s does not exist", extendedRepoDef.key()));
      } else {
        repositories.add(extendedRepoDef);
      }
    }
    return repositories;
  }

  private RuleDto createRuleDto(RulesDefinition.Rule ruleDef, DbSession session) {
    RuleDto ruleDto = RuleDto.createFor(RuleKey.of(ruleDef.repository().key(), ruleDef.key()))
      .setIsTemplate(ruleDef.template())
      .setConfigKey(ruleDef.internalKey())
      .setLanguage(ruleDef.repository().language())
      .setName(ruleDef.name())
      .setSeverity(ruleDef.severity())
      .setStatus(ruleDef.status())
      .setEffortToFixDescription(ruleDef.effortToFixDescription())
      .setSystemTags(ruleDef.tags());
    if (ruleDef.htmlDescription() != null) {
      ruleDto.setDescription(ruleDef.htmlDescription());
      ruleDto.setDescriptionFormat(Format.HTML);
    } else {
      ruleDto.setDescription(ruleDef.markdownDescription());
      ruleDto.setDescriptionFormat(Format.MARKDOWN);
    }

    dbClient.deprecatedRuleDao().insert(session, ruleDto);
    return ruleDto;
  }

  private boolean mergeRule(RulesDefinition.Rule def, RuleDto dto) {
    boolean changed = false;
    if (!StringUtils.equals(dto.getName(), def.name())) {
      dto.setName(def.name());
      changed = true;
    }
    if (mergeDescription(def, dto)) {
      changed= true;
    }
    if (!dto.getSystemTags().containsAll(def.tags())) {
      dto.setSystemTags(def.tags());
      changed = true;
    }
    if (!StringUtils.equals(dto.getConfigKey(), def.internalKey())) {
      dto.setConfigKey(def.internalKey());
      changed = true;
    }
    String severity = def.severity();
    if (!ObjectUtils.equals(dto.getSeverityString(), severity)) {
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
    if (!StringUtils.equals(dto.getLanguage(), def.repository().language())) {
      dto.setLanguage(def.repository().language());
      changed = true;
    }
    return changed;
  }

  private boolean mergeDescription(RulesDefinition.Rule def, RuleDto dto) {
    boolean changed = false;
    if (def.htmlDescription() != null && !StringUtils.equals(dto.getDescription(), def.htmlDescription())) {
      dto.setDescription(def.htmlDescription());
      dto.setDescriptionFormat(Format.HTML);
      changed = true;
    } else if (def.markdownDescription() != null && !StringUtils.equals(dto.getDescription(), def.markdownDescription())) {
      dto.setDescription(def.markdownDescription());
      dto.setDescriptionFormat(Format.MARKDOWN);
      changed = true;
    }
    return changed;
  }

  private boolean mergeDebtDefinitions(RulesDefinition.Rule def, RuleDto dto, @Nullable CharacteristicDto subCharacteristic) {
    // Debt definitions are set to null if the sub-characteristic and the remediation function are null
    DebtRemediationFunction debtRemediationFunction = subCharacteristic != null ? def.debtRemediationFunction() : null;
    boolean hasDebt = subCharacteristic != null && debtRemediationFunction != null;
    if (hasDebt) {
      return mergeDebtDefinitions(def, dto,
        subCharacteristic.getId(),
        debtRemediationFunction.type().name(),
        debtRemediationFunction.coefficient(),
        debtRemediationFunction.offset(),
        def.effortToFixDescription());
    }
    return mergeDebtDefinitions(def, dto, null, null, null, null, null);
  }

  private boolean mergeDebtDefinitions(RulesDefinition.Rule def, RuleDto dto, @Nullable Integer characteristicId, @Nullable String remediationFunction,
    @Nullable String remediationCoefficient, @Nullable String remediationOffset, @Nullable String effortToFixDescription) {
    boolean changed = false;

    if (!ObjectUtils.equals(dto.getDefaultSubCharacteristicId(), characteristicId)) {
      dto.setDefaultSubCharacteristicId(characteristicId);
      changed = true;
    }
    if (!StringUtils.equals(dto.getDefaultRemediationFunction(), remediationFunction)) {
      dto.setDefaultRemediationFunction(remediationFunction);
      changed = true;
    }
    if (!StringUtils.equals(dto.getDefaultRemediationCoefficient(), remediationCoefficient)) {
      dto.setDefaultRemediationCoefficient(remediationCoefficient);
      changed = true;
    }
    if (!StringUtils.equals(dto.getDefaultRemediationOffset(), remediationOffset)) {
      dto.setDefaultRemediationOffset(remediationOffset);
      changed = true;
    }
    if (!StringUtils.equals(dto.getEffortToFixDescription(), effortToFixDescription)) {
      dto.setEffortToFixDescription(effortToFixDescription);
      changed = true;
    }
    return changed;
  }

  private void mergeParams(RulesDefinition.Rule ruleDef, RuleDto rule, DbSession session) {
    List<RuleParamDto> paramDtos = dbClient.deprecatedRuleDao().selectRuleParamsByRuleKey(session, rule.getKey());
    Map<String, RuleParamDto> existingParamsByName = Maps.newHashMap();

    for (RuleParamDto paramDto : paramDtos) {
      RulesDefinition.Param paramDef = ruleDef.param(paramDto.getName());
      if (paramDef == null) {
        dbClient.activeRuleDao().deleteParamsByRuleParam(session, rule, paramDto.getName());
        dbClient.deprecatedRuleDao().deleteRuleParam(session, rule, paramDto);
      } else {
        if (mergeParam(paramDto, paramDef)) {
          dbClient.deprecatedRuleDao().updateRuleParam(session, rule, paramDto);
        }
        existingParamsByName.put(paramDto.getName(), paramDto);
      }
    }

    // Create newly parameters
    for (RulesDefinition.Param param : ruleDef.params()) {
      RuleParamDto paramDto = existingParamsByName.get(param.key());
      if (paramDto == null) {
        paramDto = RuleParamDto.createFor(rule)
          .setName(param.key())
          .setDescription(param.description())
          .setDefaultValue(param.defaultValue())
          .setType(param.type().toString());
        dbClient.deprecatedRuleDao().insertRuleParam(session, rule, paramDto);
        if (!StringUtils.isEmpty(param.defaultValue())) {
          // Propagate the default value to existing active rules
          for (ActiveRuleDto activeRule : dbClient.activeRuleDao().selectByRule(session, rule)) {
            ActiveRuleParamDto activeParam = ActiveRuleParamDto.createFor(paramDto).setValue(param.defaultValue());
            dbClient.activeRuleDao().insertParam(session, activeRule, activeParam);
          }
        }
      }
    }
  }

  private boolean mergeParam(RuleParamDto paramDto, RulesDefinition.Param paramDef) {
    boolean changed = false;
    if (!StringUtils.equals(paramDto.getType(), paramDef.type().toString())) {
      paramDto.setType(paramDef.type().toString());
      changed = true;
    }
    if (!StringUtils.equals(paramDto.getDefaultValue(), paramDef.defaultValue())) {
      paramDto.setDefaultValue(paramDef.defaultValue());
      changed = true;
    }
    if (!StringUtils.equals(paramDto.getDescription(), paramDef.description())) {
      paramDto.setDescription(paramDef.description());
      changed = true;
    }
    return changed;
  }

  private boolean mergeTags(RulesDefinition.Rule ruleDef, RuleDto dto) {
    boolean changed = false;

    if (RuleStatus.REMOVED == ruleDef.status()) {
      dto.setSystemTags(Collections.<String>emptySet());
      changed = true;
    } else if (!dto.getSystemTags().containsAll(ruleDef.tags())
      || !Sets.intersection(dto.getTags(), ruleDef.tags()).isEmpty()) {
      dto.setSystemTags(ruleDef.tags());
      // remove end-user tags that are now declared as system
      RuleTagHelper.applyTags(dto, ImmutableSet.copyOf(dto.getTags()));
      changed = true;
    }
    return changed;
  }

  private List<RuleDto> processRemainingDbRules(Collection<RuleDto> existingRules, DbSession session) {
    // custom rules check status of template, so they must be processed at the end
    List<RuleDto> customRules = newArrayList();
    List<RuleDto> removedRules = newArrayList();

    for (RuleDto rule : existingRules) {
      if (rule.getTemplateId() != null) {
        customRules.add(rule);
      } else if (rule.getStatus() != RuleStatus.REMOVED) {
        removeRule(session, removedRules, rule);
      }
    }

    for (RuleDto customRule : customRules) {
      RuleDto template = dbClient.deprecatedRuleDao().selectTemplate(customRule, session);
      if (template != null && template.getStatus() != RuleStatus.REMOVED) {
        if (updateCustomRuleFromTemplateRule(customRule, template)) {
          dbClient.deprecatedRuleDao().update(session, customRule);
        }
      } else {
        removeRule(session, removedRules, customRule);
      }
    }

    session.commit();
    return removedRules;
  }

  private void removeRule(DbSession session, List<RuleDto> removedRules, RuleDto rule) {
    LOG.info(String.format("Disable rule %s", rule.getKey()));
    rule.setStatus(RuleStatus.REMOVED);
    rule.setSystemTags(Collections.<String>emptySet());
    rule.setTags(Collections.<String>emptySet());
    dbClient.deprecatedRuleDao().update(session, rule);
    removedRules.add(rule);
    if (removedRules.size() % 100 == 0) {
      session.commit();
    }
  }

  private static boolean updateCustomRuleFromTemplateRule(RuleDto customRule, RuleDto templateRule) {
    boolean changed = false;
    if (!StringUtils.equals(customRule.getLanguage(), templateRule.getLanguage())) {
      customRule.setLanguage(templateRule.getLanguage());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getConfigKey(), templateRule.getConfigKey())) {
      customRule.setConfigKey(templateRule.getConfigKey());
      changed = true;
    }
    if (!ObjectUtils.equals(customRule.getDefaultSubCharacteristicId(), templateRule.getDefaultSubCharacteristicId())) {
      customRule.setDefaultSubCharacteristicId(templateRule.getDefaultSubCharacteristicId());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getDefaultRemediationFunction(), templateRule.getDefaultRemediationFunction())) {
      customRule.setDefaultRemediationFunction(templateRule.getDefaultRemediationFunction());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getDefaultRemediationCoefficient(), templateRule.getDefaultRemediationCoefficient())) {
      customRule.setDefaultRemediationCoefficient(templateRule.getDefaultRemediationCoefficient());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getDefaultRemediationOffset(), templateRule.getDefaultRemediationOffset())) {
      customRule.setDefaultRemediationOffset(templateRule.getDefaultRemediationOffset());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getEffortToFixDescription(), templateRule.getEffortToFixDescription())) {
      customRule.setEffortToFixDescription(templateRule.getEffortToFixDescription());
      changed = true;
    }
    if (customRule.getStatus() != templateRule.getStatus()) {
      customRule.setStatus(templateRule.getStatus());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getSeverityString(), templateRule.getSeverityString())) {
      customRule.setSeverity(templateRule.getSeverityString());
      changed = true;
    }
    return changed;
  }

  /**
   * SONAR-4642
   * <p/>
   * Remove active rules on repositories that still exists.
   * <p/>
   * For instance, if the javascript repository do not provide anymore some rules, active rules related to this rules will be removed.
   * But if the javascript repository do not exists anymore, then related active rules will not be removed.
   * <p/>
   * The side effect of this approach is that extended repositories will not be managed the same way.
   * If an extended repository do not exists anymore, then related active rules will be removed.
   */
  private void removeActiveRulesOnStillExistingRepositories(DbSession session, Collection<RuleDto> removedRules, RulesDefinition.Context context) {
    List<String> repositoryKeys = newArrayList(Iterables.transform(context.repositories(), new Function<RulesDefinition.Repository, String>() {
      @Override
      public String apply(RulesDefinition.Repository input) {
        return input.key();
      }
    }
      ));

    for (RuleDto rule : removedRules) {
      // SONAR-4642 Remove active rules only when repository still exists
      if (repositoryKeys.contains(rule.getRepositoryKey())) {
        ruleActivator.deactivate(session, rule);
      }
    }
  }
}

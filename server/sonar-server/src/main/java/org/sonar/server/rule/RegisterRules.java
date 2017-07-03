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
package org.sonar.server.rule;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto.Format;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndexer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

/**
 * Register rules at server startup
 */
public class RegisterRules implements Startable {

  private static final Logger LOG = Loggers.get(RegisterRules.class);

  private final RuleDefinitionsLoader defLoader;
  private final RuleActivator ruleActivator;
  private final DbClient dbClient;
  private final RuleIndexer ruleIndexer;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final Languages languages;
  private final System2 system2;
  private final OrganizationFlags organizationFlags;
  private final WebServerRuleFinder webServerRuleFinder;

  public RegisterRules(RuleDefinitionsLoader defLoader, RuleActivator ruleActivator, DbClient dbClient, RuleIndexer ruleIndexer,
    ActiveRuleIndexer activeRuleIndexer, Languages languages, System2 system2, OrganizationFlags organizationFlags,
    WebServerRuleFinder webServerRuleFinder) {
    this.defLoader = defLoader;
    this.ruleActivator = ruleActivator;
    this.dbClient = dbClient;
    this.ruleIndexer = ruleIndexer;
    this.activeRuleIndexer = activeRuleIndexer;
    this.languages = languages;
    this.system2 = system2;
    this.organizationFlags = organizationFlags;
    this.webServerRuleFinder = webServerRuleFinder;
  }

  @Override
  public void start() {
    Profiler profiler = Profiler.create(LOG).startInfo("Register rules");
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<RuleKey, RuleDefinitionDto> allRules = loadRules(dbSession);
      List<RuleKey> keysToIndex = new ArrayList<>();

      RulesDefinition.Context context = defLoader.load();
      boolean orgsEnabled = organizationFlags.isEnabled(dbSession);
      for (RulesDefinition.ExtendedRepository repoDef : getRepositories(context)) {
        if (languages.get(repoDef.language()) != null) {
          for (RulesDefinition.Rule ruleDef : repoDef.rules()) {
            RuleKey ruleKey = RuleKey.of(ruleDef.repository().key(), ruleDef.key());
            if (ruleDef.template() && orgsEnabled) {
              RuleDefinitionDto ruleDefinition = allRules.get(ruleKey);
              if (ruleDefinition != null && ruleDefinition.getStatus() == RuleStatus.REMOVED) {
                LOG.debug("Template rule {} kept removed, because organizations are enabled.", ruleKey);
                allRules.remove(ruleKey);
              } else {
                LOG.info("Template rule {} will not be imported, because organizations are enabled.", ruleKey);
              }
              continue;
            }
            boolean relevantForIndex = registerRule(ruleDef, allRules, dbSession);
            if (relevantForIndex) {
              keysToIndex.add(ruleKey);
            }
          }
          dbSession.commit();
        }
      }
      List<RuleDefinitionDto> removedRules = processRemainingDbRules(allRules.values(), dbSession);
      List<ActiveRuleChange> changes = removeActiveRulesOnStillExistingRepositories(dbSession, removedRules, context);
      dbSession.commit();
      keysToIndex.addAll(removedRules.stream().map(RuleDefinitionDto::getKey).collect(Collectors.toList()));

      persistRepositories(dbSession, context.repositories());
      ruleIndexer.commitAndIndex(dbSession, keysToIndex);
      activeRuleIndexer.commitAndIndex(dbSession, changes);
      profiler.stopDebug();

      webServerRuleFinder.startCaching();
    }
  }

  private void persistRepositories(DbSession dbSession, List<RulesDefinition.Repository> repositories) {
    dbClient.ruleRepositoryDao().truncate(dbSession);
    List<RuleRepositoryDto> dtos = repositories
      .stream()
      .map(r -> new RuleRepositoryDto(r.key(), r.language(), r.name()))
      .collect(MoreCollectors.toList(repositories.size()));
    dbClient.ruleRepositoryDao().insert(dbSession, dtos);
    dbSession.commit();
  }

  @Override
  public void stop() {
    // nothing
  }

  private boolean registerRule(RulesDefinition.Rule ruleDef, Map<RuleKey, RuleDefinitionDto> allRules, DbSession session) {
    RuleKey ruleKey = RuleKey.of(ruleDef.repository().key(), ruleDef.key());

    RuleDefinitionDto existingRule = allRules.remove(ruleKey);
    boolean newRule;
    RuleDefinitionDto rule;
    if (existingRule == null) {
      rule = createRuleDto(ruleDef, session);
      newRule = true;
    } else {
      rule = existingRule;
      newRule = false;
    }

    boolean executeUpdate = false;
    if (mergeRule(ruleDef, rule)) {
      executeUpdate = true;
    }

    if (mergeDebtDefinitions(ruleDef, rule)) {
      executeUpdate = true;
    }

    if (mergeTags(ruleDef, rule)) {
      executeUpdate = true;
    }

    if (executeUpdate) {
      update(session, rule);
    }

    mergeParams(ruleDef, rule, session);
    return newRule || executeUpdate;
  }

  private Map<RuleKey, RuleDefinitionDto> loadRules(DbSession session) {
    Map<RuleKey, RuleDefinitionDto> rules = new HashMap<>();
    for (RuleDefinitionDto rule : dbClient.ruleDao().selectAllDefinitions(session)) {
      rules.put(rule.getKey(), rule);
    }
    return rules;
  }

  private List<RulesDefinition.ExtendedRepository> getRepositories(RulesDefinition.Context context) {
    List<RulesDefinition.ExtendedRepository> repositories = new ArrayList<>();
    for (RulesDefinition.Repository repoDef : context.repositories()) {
      repositories.add(repoDef);
    }
    for (RulesDefinition.ExtendedRepository extendedRepoDef : context.extendedRepositories()) {
      if (context.repository(extendedRepoDef.key()) == null) {
        LOG.warn(format("Extension is ignored, repository %s does not exist", extendedRepoDef.key()));
      } else {
        repositories.add(extendedRepoDef);
      }
    }
    return repositories;
  }

  private RuleDefinitionDto createRuleDto(RulesDefinition.Rule ruleDef, DbSession session) {
    RuleDefinitionDto ruleDto = new RuleDefinitionDto()
      .setRuleKey(RuleKey.of(ruleDef.repository().key(), ruleDef.key()))
      .setIsTemplate(ruleDef.template())
      .setConfigKey(ruleDef.internalKey())
      .setLanguage(ruleDef.repository().language())
      .setName(ruleDef.name())
      .setSeverity(ruleDef.severity())
      .setStatus(ruleDef.status())
      .setGapDescription(ruleDef.gapDescription())
      .setSystemTags(ruleDef.tags())
      .setType(RuleType.valueOf(ruleDef.type().name()))
      .setCreatedAt(system2.now())
      .setUpdatedAt(system2.now());
    if (ruleDef.htmlDescription() != null) {
      ruleDto.setDescription(ruleDef.htmlDescription());
      ruleDto.setDescriptionFormat(Format.HTML);
    } else {
      ruleDto.setDescription(ruleDef.markdownDescription());
      ruleDto.setDescriptionFormat(Format.MARKDOWN);
    }

    dbClient.ruleDao().insert(session, ruleDto);
    return ruleDto;
  }

  private boolean mergeRule(RulesDefinition.Rule def, RuleDefinitionDto dto) {
    boolean changed = false;
    if (!StringUtils.equals(dto.getName(), def.name())) {
      dto.setName(def.name());
      changed = true;
    }
    if (mergeDescription(def, dto)) {
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
    RuleType type = RuleType.valueOf(def.type().name());
    if (!ObjectUtils.equals(dto.getType(), type.getDbConstant())) {
      dto.setType(type);
      changed = true;
    }
    return changed;
  }

  private boolean mergeDescription(RulesDefinition.Rule def, RuleDefinitionDto dto) {
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

  private boolean mergeDebtDefinitions(RulesDefinition.Rule def, RuleDefinitionDto dto) {
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

  private boolean mergeDebtDefinitions(RuleDefinitionDto dto, @Nullable String remediationFunction,
    @Nullable String remediationCoefficient, @Nullable String remediationOffset, @Nullable String effortToFixDescription) {
    boolean changed = false;

    if (!StringUtils.equals(dto.getDefRemediationFunction(), remediationFunction)) {
      dto.setDefRemediationFunction(remediationFunction);
      changed = true;
    }
    if (!StringUtils.equals(dto.getDefRemediationGapMultiplier(), remediationCoefficient)) {
      dto.setDefRemediationGapMultiplier(remediationCoefficient);
      changed = true;
    }
    if (!StringUtils.equals(dto.getDefRemediationBaseEffort(), remediationOffset)) {
      dto.setDefRemediationBaseEffort(remediationOffset);
      changed = true;
    }
    if (!StringUtils.equals(dto.getGapDescription(), effortToFixDescription)) {
      dto.setGapDescription(effortToFixDescription);
      changed = true;
    }
    return changed;
  }

  private void mergeParams(RulesDefinition.Rule ruleDef, RuleDefinitionDto rule, DbSession session) {
    List<RuleParamDto> paramDtos = dbClient.ruleDao().selectRuleParamsByRuleKey(session, rule.getKey());
    Map<String, RuleParamDto> existingParamsByName = Maps.newHashMap();

    Profiler profiler = Profiler.create(Loggers.get(getClass()));
    for (RuleParamDto paramDto : paramDtos) {
      RulesDefinition.Param paramDef = ruleDef.param(paramDto.getName());
      if (paramDef == null) {
        profiler.start();
        dbClient.activeRuleDao().deleteParamsByRuleParamOfAllOrganizations(session, paramDto);
        profiler.stopDebug(format("Propagate deleted param with name %s to active rules of rule %s", paramDto.getName(), rule.getKey()));
        dbClient.ruleDao().deleteRuleParam(session, paramDto.getId());
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
      for (ActiveRuleDto activeRule : dbClient.activeRuleDao().selectByRuleIdOfAllOrganizations(session, rule.getId())) {
        ActiveRuleParamDto activeParam = ActiveRuleParamDto.createFor(paramDto).setValue(param.defaultValue());
        dbClient.activeRuleDao().insertParam(session, activeRule, activeParam);
      }
      profiler.stopDebug(format("Propagate new param with name %s to active rules of rule %s", paramDto.getName(), rule.getKey()));
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

  private static boolean mergeTags(RulesDefinition.Rule ruleDef, RuleDefinitionDto dto) {
    boolean changed = false;

    if (RuleStatus.REMOVED == ruleDef.status()) {
      dto.setSystemTags(Collections.emptySet());
      changed = true;
    } else if (dto.getSystemTags().size() != ruleDef.tags().size() ||
      !dto.getSystemTags().containsAll(ruleDef.tags())) {
      dto.setSystemTags(ruleDef.tags());
      // FIXME this can't be implemented easily with organization support: remove end-user tags that are now declared as system
      // RuleTagHelper.applyTags(dto, ImmutableSet.copyOf(dto.getTags()));
      changed = true;
    }
    return changed;
  }

  private List<RuleDefinitionDto> processRemainingDbRules(Collection<RuleDefinitionDto> existingRules, DbSession session) {
    // custom rules check status of template, so they must be processed at the end
    List<RuleDefinitionDto> customRules = newArrayList();
    List<RuleDefinitionDto> removedRules = newArrayList();

    for (RuleDefinitionDto rule : existingRules) {
      if (rule.isCustomRule()) {
        customRules.add(rule);
      } else if (rule.getStatus() != RuleStatus.REMOVED) {
        removeRule(session, removedRules, rule);
      }
    }

    for (RuleDefinitionDto customRule : customRules) {
      Integer templateId = customRule.getTemplateId();
      checkNotNull(templateId, "Template id of the custom rule '%s' is null", customRule);
      Optional<RuleDefinitionDto> template = dbClient.ruleDao().selectDefinitionById(templateId, session);
      if (template.isPresent() && template.get().getStatus() != RuleStatus.REMOVED) {
        if (updateCustomRuleFromTemplateRule(customRule, template.get())) {
          update(session, customRule);
        }
      } else {
        removeRule(session, removedRules, customRule);
      }
    }

    session.commit();
    return removedRules;
  }

  private void removeRule(DbSession session, List<RuleDefinitionDto> removedRules, RuleDefinitionDto rule) {
    LOG.info(format("Disable rule %s", rule.getKey()));
    rule.setStatus(RuleStatus.REMOVED);
    rule.setSystemTags(Collections.emptySet());
    update(session, rule);
    // FIXME resetting the tags for all organizations must be handled a different way
    // rule.setTags(Collections.emptySet());
    // update(session, rule.getMetadata());
    removedRules.add(rule);
    if (removedRules.size() % 100 == 0) {
      session.commit();
    }
  }

  private static boolean updateCustomRuleFromTemplateRule(RuleDefinitionDto customRule, RuleDefinitionDto templateRule) {
    boolean changed = false;
    if (!StringUtils.equals(customRule.getLanguage(), templateRule.getLanguage())) {
      customRule.setLanguage(templateRule.getLanguage());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getConfigKey(), templateRule.getConfigKey())) {
      customRule.setConfigKey(templateRule.getConfigKey());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getDefRemediationFunction(), templateRule.getDefRemediationFunction())) {
      customRule.setDefRemediationFunction(templateRule.getDefRemediationFunction());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getDefRemediationGapMultiplier(), templateRule.getDefRemediationGapMultiplier())) {
      customRule.setDefRemediationGapMultiplier(templateRule.getDefRemediationGapMultiplier());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getDefRemediationBaseEffort(), templateRule.getDefRemediationBaseEffort())) {
      customRule.setDefRemediationBaseEffort(templateRule.getDefRemediationBaseEffort());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getGapDescription(), templateRule.getGapDescription())) {
      customRule.setGapDescription(templateRule.getGapDescription());
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
  private List<ActiveRuleChange> removeActiveRulesOnStillExistingRepositories(DbSession session, Collection<RuleDefinitionDto> removedRules, RulesDefinition.Context context) {
    List<String> repositoryKeys = newArrayList(Iterables.transform(context.repositories(), RulesDefinition.Repository::key));

    List<ActiveRuleChange> changes = new ArrayList<>();
    Profiler profiler = Profiler.create(Loggers.get(getClass()));
    for (RuleDefinitionDto rule : removedRules) {
      // SONAR-4642 Remove active rules only when repository still exists
      if (repositoryKeys.contains(rule.getRepositoryKey())) {
        profiler.start();
        changes.addAll(ruleActivator.delete(session, rule));
        profiler.stopDebug(format("Remove active rule for rule %s", rule.getKey()));
      }
    }
    return changes;
  }

  private void update(DbSession session, RuleDefinitionDto rule) {
    rule.setUpdatedAt(system2.now());
    dbClient.ruleDao().update(session, rule);
  }

}

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
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.action.EmbeddedIndexAction;
import org.sonar.server.search.action.IndexAction;
import org.sonar.server.search.action.KeyIndexAction;

import javax.annotation.Nullable;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Register rules at server startup
 */
public class RegisterRules implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterRules.class);

  private final RuleDefinitionsLoader defLoader;
  private final RuleActivator ruleActivator;
  private final DbClient dbClient;
  private final CharacteristicDao characteristicDao;


  public RegisterRules(RuleDefinitionsLoader defLoader, RuleActivator ruleActivator,
                       DbClient dbClient) {
    this(defLoader, ruleActivator, dbClient, System2.INSTANCE);
  }


  @VisibleForTesting
  RegisterRules(RuleDefinitionsLoader defLoader, RuleActivator ruleActivator,
                DbClient dbClient, System2 system) {
    this.defLoader = defLoader;
    this.ruleActivator = ruleActivator;
    this.dbClient = dbClient;
    this.characteristicDao = dbClient.debtCharacteristicDao();
  }

  @Override
  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Register rules");
    DbSession session = dbClient.openSession(false);
    try {
      Map<RuleKey, RuleDto> allRules = loadRules(session);

      RulesDefinition.Context context = defLoader.load();
      for (RulesDefinition.ExtendedRepository repoDef : getRepositories(context)) {
        for (RulesDefinition.Rule ruleDef : repoDef.rules()) {
          RuleKey ruleKey = RuleKey.of(ruleDef.repository().key(), ruleDef.key());

          RuleDto rule = allRules.containsKey(ruleKey) ? allRules.remove(ruleKey) : createRuleDto(ruleDef, session);

          boolean executeUpdate = false;
          if (mergeRule(ruleDef, rule)) {
            executeUpdate = true;
          }

          if (rule.getSubCharacteristicId() != null) {
            CharacteristicDto characteristicDto = characteristicDao.selectById(rule.getSubCharacteristicId(), session);
            if (characteristicDto != null && mergeDebtDefinitions(ruleDef, rule, characteristicDto)) {
              executeUpdate = true;
            }
          }

          if (mergeTags(ruleDef, rule)) {
            executeUpdate = true;
          }

          if (executeUpdate) {
            dbClient.ruleDao().update(session, rule);
          } else {
            // TODO replace this hack by index synchronizer
            session.enqueue(new KeyIndexAction<RuleKey>(IndexDefinition.RULE.getIndexType(),
              IndexAction.Method.UPSERT, rule.getKey()));
          }

          mergeParams(ruleDef, rule, session);
        }
        session.commit();
      }
      List<RuleDto> activeRules = processRemainingDbRules(allRules.values(), session);
      removeActiveRulesOnStillExistingRepositories(activeRules, context);
      session.commit();

    } finally {
      session.close();
      profiler.stop();
    }

  }

  @Override
  public void stop() {
    // nothing
  }

  private Map<RuleKey, RuleDto> loadRules(DbSession session) {
    Map<RuleKey, RuleDto> rules = new HashMap<RuleKey, RuleDto>();
    for (RuleDto rule : dbClient.ruleDao().findByNonManual(session)) {
      rules.put(rule.getKey(), rule);
    }
    return rules;
  }

  private List<RulesDefinition.ExtendedRepository> getRepositories(RulesDefinition.Context context) {
    List<RulesDefinition.ExtendedRepository> repositories = new ArrayList<RulesDefinition.ExtendedRepository>();
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
      .setDescription(ruleDef.htmlDescription())
      .setLanguage(ruleDef.repository().language())
      .setName(ruleDef.name())
      .setSeverity(ruleDef.severity())
      .setStatus(ruleDef.status())
      .setEffortToFixDescription(ruleDef.effortToFixDescription())
      .setSystemTags(ruleDef.tags());

    dbClient.ruleDao().insert(session, ruleDto);
    return ruleDto;
  }

  private boolean mergeRule(RulesDefinition.Rule def, RuleDto dto) {
    boolean changed = false;
    if (!StringUtils.equals(dto.getName(), def.name())) {
      dto.setName(def.name());
      changed = true;
    }
    if (!StringUtils.equals(dto.getDescription(), def.htmlDescription())) {
      dto.setDescription(def.htmlDescription());
      changed = true;
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
    if (!StringUtils.equals(dto.getEffortToFixDescription(), def.effortToFixDescription())) {
      dto.setEffortToFixDescription(def.effortToFixDescription());
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
    List<RuleParamDto> paramDtos = dbClient.ruleDao().findRuleParamsByRuleKey(session, rule.getKey());
    List<String> existingParamDtoNames = new ArrayList<String>();

    for (RuleParamDto paramDto : paramDtos) {
      RulesDefinition.Param paramDef = ruleDef.param(paramDto.getName());
      if (paramDef == null) {
        //TODO cascade on the activeRule upon RuleDeletion
        //activeRuleDao.removeRuleParam(paramDto, sqlSession);
        dbClient.ruleDao().removeRuleParam(session, rule, paramDto);
      } else {
        // TODO validate that existing active rules still match constraints
        // TODO store param name
        if (mergeParam(paramDto, paramDef)) {
          dbClient.ruleDao().updateRuleParam(session, rule, paramDto);
        } else {
          // TODO to be replaced by synchronizer
          session.enqueue(new EmbeddedIndexAction<RuleKey>(IndexDefinition.RULE.getIndexType(),
            IndexAction.Method.UPSERT, paramDto, rule.getKey()));
        }
        existingParamDtoNames.add(paramDto.getName());
      }
    }
    for (RulesDefinition.Param param : ruleDef.params()) {
      if (!existingParamDtoNames.contains(param.key())) {
        RuleParamDto paramDto = RuleParamDto.createFor(rule)
          .setName(param.key())
          .setDescription(param.description())
          .setDefaultValue(param.defaultValue())
          .setType(param.type().toString());
        dbClient.ruleDao().addRuleParam(session, rule, paramDto);
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
    } else if (!dto.getSystemTags().containsAll(ruleDef.tags())) {
      dto.setSystemTags(ruleDef.tags());
      // remove end-user tags that are now declared as system
      RuleTagHelper.applyTags(dto, ImmutableSet.copyOf(dto.getTags()));
      changed = true;
    }
    return changed;
  }

  private List<RuleDto> processRemainingDbRules(Collection<RuleDto> ruleDtos, DbSession session) {
    List<RuleDto> removedRules = newArrayList();
    for (RuleDto ruleDto : ruleDtos) {
      boolean toBeRemoved = true;
      // Update custom rules from template
      if (ruleDto.getTemplateId() != null) {
        RuleDto template = dbClient.ruleDao().getTemplate(ruleDto, session);
        if (template != null && RuleStatus.REMOVED != template.getStatus()) {
          ruleDto.setLanguage(template.getLanguage());
          ruleDto.setStatus(template.getStatus());
          ruleDto.setDefaultSubCharacteristicId(template.getDefaultSubCharacteristicId());
          ruleDto.setDefaultRemediationFunction(template.getDefaultRemediationFunction());
          ruleDto.setDefaultRemediationCoefficient(template.getDefaultRemediationCoefficient());
          ruleDto.setDefaultRemediationOffset(template.getDefaultRemediationOffset());
          ruleDto.setEffortToFixDescription(template.getEffortToFixDescription());
          dbClient.ruleDao().update(session, ruleDto);
          toBeRemoved = false;
        }
      }
      if (toBeRemoved && RuleStatus.REMOVED != ruleDto.getStatus()) {
        LOG.info(String.format("Disable rule %s", ruleDto.getKey()));
        ruleDto.setStatus(RuleStatus.REMOVED);
        ruleDto.setSystemTags(Collections.<String>emptySet());
        ruleDto.setTags(Collections.<String>emptySet());
        dbClient.ruleDao().update(session, ruleDto);
        removedRules.add(ruleDto);
        if (removedRules.size() % 100 == 0) {
          session.commit();
        }
      }
    }

    if (!removedRules.isEmpty()) {
      session.commit();
    }
    return removedRules;
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
  private void removeActiveRulesOnStillExistingRepositories(Collection<RuleDto> removedRules, RulesDefinition.Context context) {
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
        ruleActivator.deactivate(rule);
      }
    }
  }
}

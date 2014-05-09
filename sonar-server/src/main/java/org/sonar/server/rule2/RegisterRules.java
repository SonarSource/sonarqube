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
package org.sonar.server.rule2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.qualityprofile.ProfilesManager;
import org.sonar.server.rule.RuleDefinitionsLoader;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Register rules at server startup
 *
 * @since 4.2
 */
public class RegisterRules implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterRules.class);

  private final RuleDefinitionsLoader defLoader;
  private final ProfilesManager profilesManager;
  private final MyBatis myBatis;
  private final RuleDao ruleDao;
  private final ActiveRuleDao activeRuleDao;
  private final CharacteristicDao characteristicDao;


  public RegisterRules(RuleDefinitionsLoader defLoader, ProfilesManager profilesManager,
                       MyBatis myBatis, RuleDao ruleDao, ActiveRuleDao activeRuleDao,
                       CharacteristicDao characteristicDao) {
    this(defLoader, profilesManager, myBatis, ruleDao, activeRuleDao, characteristicDao, System2.INSTANCE);
  }

  @VisibleForTesting
  RegisterRules(RuleDefinitionsLoader defLoader, ProfilesManager profilesManager,
                MyBatis myBatis, RuleDao ruleDao, ActiveRuleDao activeRuleDao,
                CharacteristicDao characteristicDao, System2 system) {
    this.defLoader = defLoader;
    this.profilesManager = profilesManager;
    this.myBatis = myBatis;
    this.ruleDao = ruleDao;
    this.activeRuleDao = activeRuleDao;
    this.characteristicDao = characteristicDao;
  }

  @Override
  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Register rules");
    DbSession session = myBatis.openSession(false);
    try {

      Map<RuleKey, RuleDto> allRules = getRulesByKey(session);

      RulesDefinition.Context context = defLoader.load();
      for (RulesDefinition.ExtendedRepository repoDef : getRepositories(context)) {
        for (RulesDefinition.Rule ruleDef : repoDef.rules()) {

          RuleKey ruleKey = RuleKey.of(ruleDef.repository().key(), ruleDef.key());

          RuleDto rule = allRules.containsKey(ruleKey) ?
            allRules.remove(ruleKey) :
            this.createRuleDto(ruleDef, session);

          boolean executeUpdate = false;
          if (mergeRule(ruleDef, rule)) {
            executeUpdate = true;
          }

          if(rule.getSubCharacteristicId() != null) {
            CharacteristicDto characteristicDto = characteristicDao.selectById(rule.getSubCharacteristicId(), session);
            if (characteristicDto != null && mergeDebtDefinitions(ruleDef, rule, characteristicDto)) {
              executeUpdate = true;
            }
          }

          if (mergeTags(ruleDef, rule)) {
            executeUpdate = true;
          }

          if (executeUpdate) {
            ruleDao.update(rule, session);
          }

          mergeParams(ruleDef, rule, session);

        }
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

  private Map<RuleKey, RuleDto> getRulesByKey(DbSession session) {
    Map<RuleKey, RuleDto> rules = new HashMap<RuleKey, RuleDto>();
    for (RuleDto rule : ruleDao.findAll(session)) {
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
      .setCardinality(ruleDef.template() ? Cardinality.MULTIPLE : Cardinality.SINGLE)
      .setConfigKey(ruleDef.internalKey())
      .setDescription(ruleDef.htmlDescription())
      .setLanguage(ruleDef.repository().language())
      .setName(ruleDef.name())
      .setSeverity(ruleDef.severity())
      .setStatus(ruleDef.status().name());

    return ruleDao.insert(ruleDto, session);
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
    Cardinality cardinality = def.template() ? Cardinality.MULTIPLE : Cardinality.SINGLE;
    if (!cardinality.equals(dto.getCardinality())) {
      dto.setCardinality(cardinality);
      changed = true;
    }
    String status = def.status().name();
    if (!StringUtils.equals(dto.getStatus(), status)) {
      dto.setStatus(status);
      changed = true;
    }
    if (!StringUtils.equals(dto.getLanguage(), def.repository().language())) {
      dto.setLanguage(def.repository().language());
      changed = true;
    }
    return changed;
  }

  private boolean mergeDebtDefinitions(RulesDefinition.Rule def, RuleDto dto, @Nullable CharacteristicDto subCharacteristic) {
    // Debt definitions are set to null if the sub-characteristic and the remediation function are null
    DebtRemediationFunction debtRemediationFunction = subCharacteristic != null ? def.debtRemediationFunction() : null;
    boolean hasDebt = subCharacteristic != null && debtRemediationFunction != null;
    return mergeDebtDefinitions(def, dto,
      hasDebt ? subCharacteristic.getId() : null,
      debtRemediationFunction != null ? debtRemediationFunction.type().name() : null,
      hasDebt ? debtRemediationFunction.coefficient() : null,
      hasDebt ? debtRemediationFunction.offset() : null,
      hasDebt ? def.effortToFixDescription() : null);
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
    List<RuleParamDto> paramDtos = ruleDao.findRuleParamsByRuleKey(rule.getKey(), session);
    List<String> existingParamDtoNames = new ArrayList<String>();

    for (RuleParamDto paramDto : paramDtos) {
      RulesDefinition.Param paramDef = ruleDef.param(paramDto.getName());
      if (paramDef == null) {
        //TODO cascade on the activeRule upon RuleDeletion
        //activeRuleDao.removeRuleParam(paramDto, sqlSession);
        ruleDao.removeRuleParam(rule, paramDto, session);
      } else {
        // TODO validate that existing active rules still match constraints
        // TODO store param name
        if (mergeParam(paramDto, paramDef)) {
          ruleDao.updateRuleParam(rule, paramDto, session);
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
        ruleDao.addRuleParam(rule, paramDto, session);
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

    //the Rule is not active and dto has tags
    if (!Rule.STATUS_REMOVED.equals(ruleDef.status())) {
      dto.setSystemTags(Collections.EMPTY_SET);
      dto.setTags(Collections.EMPTY_SET);
      changed = true;
    } else if (!dto.getSystemTags().containsAll(ruleDef.tags())) {
      dto.getSystemTags().addAll(ruleDef.tags());
      changed = true;
    }
//    //TODO Check that with JUNIT for tag removal
//    for (String tag : tags) {
//      // tag previously declared by plugin
//      if (!ruleDef.tags().contains(tag)) {
//        // not declared anymore
//        dto.getSystemTags().remove(tag);
//        buffer.removeTag(tag, dto.getId());
//      } else {
//        dto.getSystemTags().add(tag);
//        existingSystemTags.add(tag);
//      }
//    }
    return changed;
  }

  private List<RuleDto> processRemainingDbRules(Collection<RuleDto> ruleDtos, DbSession session) {
    List<RuleDto> removedRules = newArrayList();
    for (RuleDto ruleDto : ruleDtos) {
      boolean toBeRemoved = true;
      // Update custom rules from template
      if (ruleDto.getParentId() != null) {
        RuleDto parent = ruleDao.getParent(ruleDto, session);
        if (parent != null && !Rule.STATUS_REMOVED.equals(parent.getStatus())) {
          ruleDto.setLanguage(parent.getLanguage());
          ruleDto.setStatus(parent.getStatus());
          ruleDto.setDefaultSubCharacteristicId(parent.getDefaultSubCharacteristicId());
          ruleDto.setDefaultRemediationFunction(parent.getDefaultRemediationFunction());
          ruleDto.setDefaultRemediationCoefficient(parent.getDefaultRemediationCoefficient());
          ruleDto.setDefaultRemediationOffset(parent.getDefaultRemediationOffset());
          ruleDto.setEffortToFixDescription(parent.getEffortToFixDescription());
          ruleDao.update(ruleDto, session);
          toBeRemoved = false;
        }
      }
      if (toBeRemoved && !Rule.STATUS_REMOVED.equals(ruleDto.getStatus())) {
        LOG.info(String.format("Disable rule %s", ruleDto.getKey()));
        ruleDto.setStatus(Rule.STATUS_REMOVED);
        ruleDto.setSystemTags(Collections.EMPTY_SET);
        ruleDto.setTags(Collections.EMPTY_SET);
      }

      ruleDao.update(ruleDto, session);
      removedRules.add(ruleDto);
      if (removedRules.size() % 100 == 0) {
        session.commit();
      }
    }

    session.commit();
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
        profilesManager.removeActivatedRules(rule.getId());
      }
    }
  }
}
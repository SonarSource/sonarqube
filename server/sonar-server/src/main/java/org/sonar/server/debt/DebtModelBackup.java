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
package org.sonar.server.debt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.Reader;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.debt.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.debt.DebtModelXMLExporter.DebtModel;
import org.sonar.server.debt.DebtModelXMLExporter.RuleDebt;
import org.sonar.server.debt.DebtPredicates.CharacteristicDtoMatchKey;
import org.sonar.server.debt.DebtPredicates.CharacteristicDtoParentIdMatchId;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.RuleOperations;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;

@ServerSide
public class DebtModelBackup {

  private static final Logger LOG = Loggers.get(DebtModelBackup.class);

  private final DbClient dbClient;
  private final DebtModelOperations debtModelOperations;
  private final RuleOperations ruleOperations;
  private final DebtModelPluginRepository debtModelPluginRepository;
  private final DebtCharacteristicsXMLImporter characteristicsXMLImporter;
  private final DebtRulesXMLImporter rulesXMLImporter;
  private final DebtModelXMLExporter debtModelXMLExporter;
  private final RuleDefinitionsLoader defLoader;
  private final System2 system2;
  private final UserSession userSession;

  public DebtModelBackup(DbClient dbClient, DebtModelOperations debtModelOperations, RuleOperations ruleOperations,
    DebtModelPluginRepository debtModelPluginRepository, DebtCharacteristicsXMLImporter characteristicsXMLImporter, DebtRulesXMLImporter rulesXMLImporter,
    DebtModelXMLExporter debtModelXMLExporter, RuleDefinitionsLoader defLoader, System2 system2, UserSession userSession) {
    this.dbClient = dbClient;
    this.debtModelOperations = debtModelOperations;
    this.ruleOperations = ruleOperations;
    this.debtModelPluginRepository = debtModelPluginRepository;
    this.characteristicsXMLImporter = characteristicsXMLImporter;
    this.rulesXMLImporter = rulesXMLImporter;
    this.debtModelXMLExporter = debtModelXMLExporter;
    this.defLoader = defLoader;
    this.system2 = system2;
    this.userSession = userSession;
  }

  public String backup() {
    return backupFromLanguage(null);
  }

  public String backup(String languageKey) {
    return backupFromLanguage(languageKey);
  }

  private String backupFromLanguage(@Nullable String languageKey) {
    checkPermission();

    DbSession session = dbClient.openSession(false);
    try {
      DebtModel debtModel = new DebtModel();
      List<CharacteristicDto> characteristicDtos = dbClient.debtCharacteristicDao().selectEnabledCharacteristics(session);
      for (CharacteristicDto characteristicDto : characteristicDtos) {
        if (characteristicDto.getParentId() == null) {
          debtModel.addRootCharacteristic(toDebtCharacteristic(characteristicDto));
          for (CharacteristicDto sub : subCharacteristics(characteristicDto.getId(), characteristicDtos)) {
            debtModel.addSubCharacteristic(toDebtCharacteristic(sub), characteristicDto.getKey());
          }
        }
      }

      List<RuleDebt> rules = newArrayList();
      for (RuleDto rule : dbClient.deprecatedRuleDao().selectEnabledAndNonManual(session)) {
        if (languageKey == null || languageKey.equals(rule.getLanguage())) {
          RuleDebt ruleDebt = toRuleDebt(rule, debtModel);
          if (ruleDebt != null) {
            rules.add(ruleDebt);
          }
        }
      }
      return debtModelXMLExporter.export(debtModel, rules);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Reset from provided model
   */
  public void reset() {
    checkPermission();

    Date updateDate = new Date(system2.now());
    DbSession session = dbClient.openSession(false);
    try {
      // Restore characteristics
      List<CharacteristicDto> allCharacteristicDtos = restoreCharacteristics(session, loadModelFromPlugin(DebtModelPluginRepository.DEFAULT_MODEL), updateDate);

      // Restore rules
      List<RuleDto> ruleDtos = dbClient.deprecatedRuleDao().selectEnabledAndNonManual(session);
      if (!ruleDtos.isEmpty()) {

        // Load default rule definitions
        RulesDefinition.Context context = defLoader.load();
        List<RulesDefinition.Rule> rules = newArrayList();
        for (RulesDefinition.Repository repoDef : context.repositories()) {
          rules.addAll(repoDef.rules());
        }

        resetRules(ruleDtos, rules, allCharacteristicDtos, updateDate, session);
      }

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void resetRules(List<RuleDto> ruleDtos, List<RulesDefinition.Rule> rules, List<CharacteristicDto> allCharacteristicDtos, Date updateDate, DbSession session) {
    for (RuleDto rule : ruleDtos) {
      // Restore default debt definitions

      RulesDefinition.Rule ruleDef;
      Integer ruleTemplateId = rule.getTemplateId();
      if (ruleTemplateId != null) {
        RuleDto templateRule = rule(ruleTemplateId, ruleDtos);
        ruleDef = ruleDef(templateRule.getRepositoryKey(), templateRule.getRuleKey(), rules);
      } else {
        ruleDef = ruleDef(rule.getRepositoryKey(), rule.getRuleKey(), rules);
      }

      if (ruleDef != null) {
        String subCharacteristicKey = ruleDef.debtSubCharacteristic();
        CharacteristicDto subCharacteristicDto = characteristicByKey(subCharacteristicKey, allCharacteristicDtos, false);
        DebtRemediationFunction remediationFunction = ruleDef.debtRemediationFunction();
        boolean hasDebtDefinition = subCharacteristicDto != null && remediationFunction != null;

        rule.setDefaultSubCharacteristicId(hasDebtDefinition ? subCharacteristicDto.getId() : null);
        rule.setDefaultRemediationFunction(hasDebtDefinition ? remediationFunction.type().name() : null);
        rule.setDefaultRemediationCoefficient(hasDebtDefinition ? remediationFunction.coefficient() : null);
        rule.setDefaultRemediationOffset(hasDebtDefinition ? remediationFunction.offset() : null);
      }

      // Reset overridden debt definitions
      rule.setSubCharacteristicId(null);
      rule.setRemediationFunction(null);
      rule.setRemediationCoefficient(null);
      rule.setRemediationOffset(null);
      rule.setUpdatedAt(updateDate);
      dbClient.deprecatedRuleDao().update(session, rule);
    }
  }

  /**
   * Restore model from a given XML model (characteristics and rule debt are restored from XML)
   */
  public ValidationMessages restoreFromXml(String xml) {
    return restoreXmlModel(xml, null);
  }

  /**
   * Restore model from a given XML model and a given language (only debt of rules on given language are restored from XML)
   */
  public ValidationMessages restoreFromXml(String xml, final String languageKey) {
    return restoreXmlModel(xml, languageKey);
  }

  private ValidationMessages restoreXmlModel(String xml, @Nullable final String languageKey) {
    checkPermission();

    ValidationMessages validationMessages = ValidationMessages.create();
    Date updateDate = new Date(system2.now());
    DbSession session = dbClient.openSession(false);
    try {
      List<CharacteristicDto> allCharacteristicDtos = restoreCharacteristics(session, characteristicsXMLImporter.importXML(xml), updateDate);
      restoreRules(allCharacteristicDtos, rules(languageKey, session), rulesXMLImporter.importXML(xml, validationMessages), validationMessages, updateDate, session);

      session.commit();
    } catch (IllegalArgumentException e) {
      LOG.debug("Error when restoring the model", e);
      validationMessages.addErrorText(e.getMessage());
    } finally {
      MyBatis.closeQuietly(session);
    }
    return validationMessages;
  }

  private void restoreRules(List<CharacteristicDto> allCharacteristicDtos, List<RuleDto> rules, List<RuleDebt> ruleDebts,
    ValidationMessages validationMessages, Date updateDate, DbSession session) {
    for (RuleDto rule : rules) {
      RuleDebt ruleDebt = ruleDebt(rule.getRepositoryKey(), rule.getRuleKey(), ruleDebts);
      String subCharacteristicKey = ruleDebt != null ? ruleDebt.subCharacteristicKey() : null;
      CharacteristicDto subCharacteristicDto = subCharacteristicKey != null ? characteristicByKey(ruleDebt.subCharacteristicKey(), allCharacteristicDtos, true) : null;
      ruleOperations.updateRule(rule, subCharacteristicDto,
        ruleDebt != null ? ruleDebt.function() : null,
        ruleDebt != null ? ruleDebt.coefficient() : null,
        ruleDebt != null ? ruleDebt.offset() : null, session);
      rule.setUpdatedAt(updateDate);
      ruleDebts.remove(ruleDebt);
    }

    for (RuleDebt ruleDebt : ruleDebts) {
      validationMessages.addWarningText(String.format("The rule '%s' does not exist.", ruleDebt.ruleKey()));
    }
  }

  @VisibleForTesting
  List<CharacteristicDto> restoreCharacteristics(DbSession session, DebtModel targetModel, Date updateDate) {
    List<CharacteristicDto> sourceCharacteristics = dbClient.debtCharacteristicDao().selectEnabledCharacteristics(session);

    List<CharacteristicDto> result = newArrayList();

    // Create new characteristics
    for (DebtCharacteristic characteristic : targetModel.rootCharacteristics()) {
      CharacteristicDto rootCharacteristicDto = restoreCharacteristic(characteristic, null, sourceCharacteristics, updateDate, session);
      result.add(rootCharacteristicDto);
      for (DebtCharacteristic subCharacteristic : targetModel.subCharacteristics(characteristic.key())) {
        result.add(restoreCharacteristic(subCharacteristic, rootCharacteristicDto.getId(), sourceCharacteristics, updateDate, session));
      }
    }
    // Disable no more existing characteristics
    for (CharacteristicDto sourceCharacteristic : sourceCharacteristics) {
      if (targetModel.characteristicByKey(sourceCharacteristic.getKey()) == null) {
        debtModelOperations.delete(sourceCharacteristic, updateDate, session);
      }
    }
    return result;
  }

  private CharacteristicDto restoreCharacteristic(DebtCharacteristic targetCharacteristic, @Nullable Integer parentId, List<CharacteristicDto> sourceCharacteristics,
    Date updateDate, DbSession session) {
    CharacteristicDto sourceCharacteristic = characteristicByKey(targetCharacteristic.key(), sourceCharacteristics, false);
    if (sourceCharacteristic == null) {
      CharacteristicDto newCharacteristic = toDto(targetCharacteristic, parentId).setCreatedAt(updateDate);
      dbClient.debtCharacteristicDao().insert(session, newCharacteristic);
      return newCharacteristic;
    } else {
      // Update only if modifications
      if (ObjectUtils.notEqual(sourceCharacteristic.getName(), targetCharacteristic.name()) ||
        ObjectUtils.notEqual(sourceCharacteristic.getOrder(), targetCharacteristic.order()) ||
        ObjectUtils.notEqual(sourceCharacteristic.getParentId(), parentId)) {
        sourceCharacteristic.setName(targetCharacteristic.name());
        sourceCharacteristic.setOrder(targetCharacteristic.order());
        sourceCharacteristic.setParentId(parentId);
        sourceCharacteristic.setUpdatedAt(updateDate);
        dbClient.debtCharacteristicDao().update(sourceCharacteristic, session);
      }
      return sourceCharacteristic;
    }
  }

  private DebtModel loadModelFromPlugin(String pluginKey) {
    Reader xmlFileReader = null;
    try {
      xmlFileReader = debtModelPluginRepository.createReaderForXMLFile(pluginKey);
      return characteristicsXMLImporter.importXML(xmlFileReader);
    } finally {
      IOUtils.closeQuietly(xmlFileReader);
    }
  }

  private List<RuleDto> rules(@Nullable String languageKey, DbSession session) {
    List<RuleDto> rules = dbClient.deprecatedRuleDao().selectEnabledAndNonManual(session);
    if (languageKey == null) {
      return rules;
    } else {
      return newArrayList(Iterables.filter(rules, new RuleDtoMatchLanguage(languageKey)));
    }
  }

  @CheckForNull
  private static RuleDebt ruleDebt(String ruleRepo, String ruleKey, List<RuleDebt> ruleDebts) {
    if (ruleDebts.isEmpty()) {
      return null;
    }
    return Iterables.find(ruleDebts, new RuleDebtMatchRuleRepoAndRuleKey(ruleRepo, ruleKey), null);
  }

  private static RuleDto rule(int id, List<RuleDto> rules) {
    return Iterables.find(rules, new RuleDtoMatchId(id));
  }

  @CheckForNull
  private static RulesDefinition.Rule ruleDef(String ruleRepo, String ruleKey, List<RulesDefinition.Rule> rules) {
    return Iterables.find(rules, new RuleDefMatchRuleRepoAndRuleKey(ruleRepo, ruleKey), null);
  }

  private static CharacteristicDto characteristicByKey(@Nullable final String key, List<CharacteristicDto> characteristicDtos, boolean failIfNotFound) {
    if (key == null) {
      return null;
    }
    CharacteristicDto dto = Iterables.find(characteristicDtos, new CharacteristicDtoMatchKey(key), null);
    if (dto == null && failIfNotFound) {
      throw new NotFoundException(String.format("Characteristic '%s' has not been found", key));
    }
    return dto;
  }

  private static List<CharacteristicDto> subCharacteristics(Integer parentId, List<CharacteristicDto> allCharacteristics) {
    return newArrayList(Iterables.filter(allCharacteristics, new CharacteristicDtoParentIdMatchId(parentId)));
  }

  @CheckForNull
  private static RuleDebt toRuleDebt(RuleDto rule, DebtModel debtModel) {
    RuleDebt ruleDebt = new RuleDebt().setRuleKey(RuleKey.of(rule.getRepositoryKey(), rule.getRuleKey()));
    Integer effectiveSubCharacteristicId = rule.getSubCharacteristicId() != null ? rule.getSubCharacteristicId() : rule.getDefaultSubCharacteristicId();
    DebtCharacteristic subCharacteristic = (effectiveSubCharacteristicId != null && !RuleDto.DISABLED_CHARACTERISTIC_ID.equals(effectiveSubCharacteristicId)) ?
      debtModel.characteristicById(effectiveSubCharacteristicId) : null;
    if (subCharacteristic != null) {
      ruleDebt.setSubCharacteristicKey(subCharacteristic.key());

      String overriddenFunction = rule.getRemediationFunction();
      String defaultFunction = rule.getDefaultRemediationFunction();
      if (overriddenFunction != null) {
        ruleDebt.setFunction(overriddenFunction);
        ruleDebt.setCoefficient(rule.getRemediationCoefficient());
        ruleDebt.setOffset(rule.getRemediationOffset());
        return ruleDebt;
      } else if (defaultFunction != null) {
        ruleDebt.setFunction(defaultFunction);
        ruleDebt.setCoefficient(rule.getDefaultRemediationCoefficient());
        ruleDebt.setOffset(rule.getDefaultRemediationOffset());
        return ruleDebt;
      }
    }
    return null;
  }

  private static CharacteristicDto toDto(DebtCharacteristic characteristic, @Nullable Integer parentId) {
    return new CharacteristicDto()
      .setKey(characteristic.key())
      .setName(characteristic.name())
      .setOrder(characteristic.order())
      .setParentId(parentId)
      .setEnabled(true)
      .setCreatedAt(((DefaultDebtCharacteristic) characteristic).createdAt())
      .setUpdatedAt(((DefaultDebtCharacteristic) characteristic).updatedAt());
  }

  private static DebtCharacteristic toDebtCharacteristic(CharacteristicDto characteristic) {
    return new DefaultDebtCharacteristic()
      .setId(characteristic.getId())
      .setKey(characteristic.getKey())
      .setName(characteristic.getName())
      .setOrder(characteristic.getOrder())
      .setParentId(characteristic.getParentId())
      .setCreatedAt(characteristic.getCreatedAt())
      .setUpdatedAt(characteristic.getUpdatedAt());
  }

  private void checkPermission() {
    userSession.checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
  }

  private static class RuleDtoMatchLanguage implements Predicate<RuleDto> {
    private final String languageKey;

    public RuleDtoMatchLanguage(String languageKey) {
      this.languageKey = languageKey;
    }

    @Override
    public boolean apply(@Nonnull RuleDto input) {
      return languageKey.equals(input.getLanguage());
    }
  }

  private static class RuleDebtMatchRuleRepoAndRuleKey implements Predicate<RuleDebt> {

    private final String ruleRepo;
    private final String ruleKey;

    public RuleDebtMatchRuleRepoAndRuleKey(String ruleRepo, String ruleKey) {
      this.ruleRepo = ruleRepo;
      this.ruleKey = ruleKey;
    }

    @Override
    public boolean apply(@Nullable RuleDebt input) {
      return input != null && ruleRepo.equals(input.ruleKey().repository()) && ruleKey.equals(input.ruleKey().rule());
    }
  }

  private static class RuleDtoMatchId implements Predicate<RuleDto> {
    private final int id;

    public RuleDtoMatchId(int id) {
      this.id = id;
    }

    @Override
    public boolean apply(@Nonnull RuleDto input) {
      return id == input.getId();
    }
  }

  private static class RuleDefMatchRuleRepoAndRuleKey implements Predicate<RulesDefinition.Rule> {

    private final String ruleRepo;
    private final String ruleKey;

    public RuleDefMatchRuleRepoAndRuleKey(String ruleRepo, String ruleKey) {
      this.ruleRepo = ruleRepo;
      this.ruleKey = ruleKey;
    }

    @Override
    public boolean apply(@Nonnull RulesDefinition.Rule input) {
      return ruleRepo.equals(input.repository().key()) && ruleKey.equals(input.key());
    }
  }
}

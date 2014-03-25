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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Reader;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.server.debt.DebtModelXMLExporter.DebtModel;
import static org.sonar.server.debt.DebtModelXMLExporter.RuleDebt;

public class DebtModelBackup implements ServerComponent {

  private final MyBatis mybatis;
  private final CharacteristicDao dao;
  private final RuleDao ruleDao;
  private final DebtModelOperations debtModelOperations;
  private final DebtModelPluginRepository debtModelPluginRepository;
  private final DebtCharacteristicsXMLImporter characteristicsXMLImporter;
  private final DebtRulesXMLImporter rulesXMLImporter;
  private final DebtModelXMLExporter debtModelXMLExporter;
  private final System2 system2;

  public DebtModelBackup(MyBatis mybatis, CharacteristicDao dao, RuleDao ruleDao, DebtModelOperations debtModelOperations, DebtModelPluginRepository debtModelPluginRepository,
                         DebtCharacteristicsXMLImporter characteristicsXMLImporter, DebtRulesXMLImporter rulesXMLImporter,
                         DebtModelXMLExporter debtModelXMLExporter) {
    this(mybatis, dao, ruleDao, debtModelOperations, debtModelPluginRepository, characteristicsXMLImporter, rulesXMLImporter, debtModelXMLExporter,
      System2.INSTANCE);
  }

  @VisibleForTesting
  DebtModelBackup(MyBatis mybatis, CharacteristicDao dao, RuleDao ruleDao, DebtModelOperations debtModelOperations, DebtModelPluginRepository debtModelPluginRepository,
                  DebtCharacteristicsXMLImporter characteristicsXMLImporter, DebtRulesXMLImporter rulesXMLImporter,
                  DebtModelXMLExporter debtModelXMLExporter, System2 system2) {
    this.mybatis = mybatis;
    this.dao = dao;
    this.ruleDao = ruleDao;
    this.debtModelOperations = debtModelOperations;
    this.debtModelPluginRepository = debtModelPluginRepository;
    this.characteristicsXMLImporter = characteristicsXMLImporter;
    this.rulesXMLImporter = rulesXMLImporter;
    this.debtModelXMLExporter = debtModelXMLExporter;
    this.system2 = system2;
  }

  public String backup() {
    return backupFromLanguage(null);
  }

  public String backup(String languageKey) {
    return backupFromLanguage(languageKey);
  }

  private String backupFromLanguage(@Nullable String languageKey) {
    checkPermission();

    SqlSession session = mybatis.openSession();
    try {
      DebtModel debtModel = new DebtModel();
      List<CharacteristicDto> characteristicDtos = dao.selectEnabledCharacteristics(session);
      for (CharacteristicDto characteristicDto : characteristicDtos) {
        if (characteristicDto.getParentId() == null) {
          debtModel.addRootCharacteristic(toDebtCharacteristic(characteristicDto));
          for (CharacteristicDto sub : subCharacteristics(characteristicDto.getId(), characteristicDtos)) {
            debtModel.addSubCharacteristic(toDebtCharacteristic(sub), characteristicDto.getKey());
          }
        }
      }

      List<RuleDebt> rules = newArrayList();
      for (RuleDto rule : ruleDao.selectEnablesAndNonManual(session)) {
        if ((languageKey == null || languageKey.equals(rule.getLanguage())) && rule.hasCharacteristic()) {
          Integer characteristicId = rule.getCharacteristicId() != null ? rule.getCharacteristicId() : rule.getDefaultCharacteristicId();
          rules.add(toRuleDebt(rule, debtModel.characteristicById(characteristicId).key()));
        }
      }
      return debtModelXMLExporter.export(debtModel, rules);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Restore from provided model
   */
  public void restore() {
    restoreProvided(loadModelFromPlugin(DebtModelPluginRepository.DEFAULT_MODEL), null);
  }

  /**
   * Restore from plugins providing rules for a given language
   */
  public void restore(String languageKey) {
    restoreProvided(loadModelFromPlugin(DebtModelPluginRepository.DEFAULT_MODEL), languageKey);
  }

  private void restoreProvided(DebtModel modelToImport, @Nullable String languageKey) {
    checkPermission();

    Date updateDate = new Date(system2.now());
    SqlSession session = mybatis.openSession();
    try {
      restoreCharacteristics(modelToImport, updateDate, session);
      for (RuleDto rule : ruleDao.selectEnablesAndNonManual(session)) {
        if (languageKey == null || languageKey.equals(rule.getLanguage())) {
          rule.setCharacteristicId(null);
          rule.setRemediationFunction(null);
          rule.setRemediationFactor(null);
          rule.setRemediationOffset(null);
          rule.setUpdatedAt(updateDate);
          ruleDao.update(rule, session);
          // TODO index rules in E/S
        }
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Restore model from a given XML model
   */
  public ValidationMessages restoreFromXml(String xml) {
    DebtModel debtModel = characteristicsXMLImporter.importXML(xml);
    ValidationMessages validationMessages = ValidationMessages.create();
    List<RuleDebt> ruleDebts = rulesXMLImporter.importXML(xml, validationMessages);
    restore(debtModel, ruleDebts, null, validationMessages);
    return validationMessages;
  }

  /**
   * Restore model from a given XML model and a given language
   */
  public ValidationMessages restoreFromXml(String xml, String languageKey) {
    DebtModel debtModel = characteristicsXMLImporter.importXML(xml);
    ValidationMessages validationMessages = ValidationMessages.create();
    List<RuleDebt> ruleDebts = rulesXMLImporter.importXML(xml, validationMessages);
    restore(debtModel, ruleDebts, languageKey, validationMessages);
    return validationMessages;
  }

  private void restore(DebtModel modelToImport, List<RuleDebt> ruleDebts, @Nullable String languageKey, ValidationMessages validationMessages) {
    checkPermission();

    Date updateDate = new Date(system2.now());
    SqlSession session = mybatis.openSession();
    try {
      List<CharacteristicDto> characteristicDtos = restoreCharacteristics(modelToImport, updateDate, session);
      restoreRules(characteristicDtos, languageKey, ruleDebts, validationMessages, updateDate, session);

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void restoreRules(List<CharacteristicDto> characteristicDtos, @Nullable String languageKey, List<RuleDebt> ruleDebts,
                            ValidationMessages validationMessages, Date updateDate, SqlSession session) {
    for (RuleDto rule : ruleDao.selectEnablesAndNonManual(session)) {
      if (languageKey == null || languageKey.equals(rule.getLanguage())) {
        RuleDebt ruleDebt = ruleDebtByRule(rule, ruleDebts);
        if (ruleDebt == null) {
          rule.setCharacteristicId(rule.getDefaultCharacteristicId() != null ? RuleDto.DISABLED_CHARACTERISTIC_ID : null);
          rule.setRemediationFunction(null);
          rule.setRemediationFactor(null);
          rule.setRemediationOffset(null);
        } else {
          CharacteristicDto characteristicDto = characteristicByKey(ruleDebt.characteristicKey(), characteristicDtos, false);
          // Characteristic cannot be null as it has been created just before

          boolean isSameCharacteristic = characteristicDto.getId().equals(rule.getDefaultCharacteristicId());
          boolean isSameFunction = isSameRemediationFunction(ruleDebt, rule);
          rule.setCharacteristicId((!isSameCharacteristic ? characteristicDto.getId() : null));
          rule.setRemediationFunction((!isSameFunction ? ruleDebt.function().name() : null));
          rule.setRemediationFactor((!isSameFunction ? ruleDebt.factor() : null));
          rule.setRemediationOffset((!isSameFunction ? ruleDebt.offset() : null));
        }

        ruleDebts.remove(ruleDebt);
        rule.setUpdatedAt(updateDate);
        ruleDao.update(rule, session);
        // TODO index rules in E/S
      }
    }

    for (RuleDebt ruleDebt : ruleDebts) {
      validationMessages.addWarningText(String.format("The rule '%s' does not exist.", ruleDebt.ruleKey()));
    }
  }

  @VisibleForTesting
  List<CharacteristicDto> restoreCharacteristics(DebtModel targetModel, Date updateDate, SqlSession session) {
    List<CharacteristicDto> sourceCharacteristics = dao.selectEnabledCharacteristics(session);

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
        debtModelOperations.disableCharacteristic(sourceCharacteristic, updateDate, session);
      }
    }
    return result;
  }

  private CharacteristicDto restoreCharacteristic(DebtCharacteristic targetCharacteristic, @Nullable Integer parentId, List<CharacteristicDto> sourceCharacteristics,
                                                  Date updateDate, SqlSession session) {
    CharacteristicDto sourceCharacteristic = characteristicByKey(targetCharacteristic.key(), sourceCharacteristics, true);
    if (sourceCharacteristic == null) {
      CharacteristicDto newCharacteristic = toDto(targetCharacteristic, parentId).setCreatedAt(updateDate);
      dao.insert(newCharacteristic, session);
      return newCharacteristic;
    } else {
      // Update only if modifications
      if (ObjectUtils.notEqual(sourceCharacteristic.getName(), targetCharacteristic.name()) ||
        ObjectUtils.notEqual(sourceCharacteristic.getOrder(), targetCharacteristic.order())) {
        sourceCharacteristic.setName(targetCharacteristic.name());
        sourceCharacteristic.setOrder(targetCharacteristic.order());
        sourceCharacteristic.setUpdatedAt(updateDate);
        dao.update(sourceCharacteristic, session);
      }
      return sourceCharacteristic;
    }
  }

  private static boolean isSameRemediationFunction(RuleDebt ruleDebt, RuleDto rule) {
    return new EqualsBuilder()
      .append(ruleDebt.function().name(), rule.getDefaultRemediationFunction())
      .append(ruleDebt.factor(), rule.getDefaultRemediationFactor())
      .append(ruleDebt.offset(), rule.getDefaultRemediationOffset())
      .isEquals();
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

  @CheckForNull
  private static RuleDebt ruleDebtByRule(final RuleDto rule, List<RuleDebt> ruleDebts) {
    if (ruleDebts.isEmpty()) {
      return null;
    }
    return Iterables.find(ruleDebts, new Predicate<RuleDebt>() {
      @Override
      public boolean apply(RuleDebt input) {
        return rule.getRepositoryKey().equals(input.ruleKey().repository()) && rule.getRuleKey().equals(input.ruleKey().rule());
      }
    }, null);
  }

  private static CharacteristicDto characteristicByKey(final String key, List<CharacteristicDto> characteristicDtos, boolean canByNull) {
    CharacteristicDto dto = Iterables.find(characteristicDtos, new Predicate<CharacteristicDto>() {
      @Override
      public boolean apply(CharacteristicDto input) {
        return key.equals(input.getKey());
      }
    }, null);
    if (dto == null && !canByNull) {
      throw new IllegalStateException(String.format("Characteristic with key '%s' has not been found ", key));
    }
    return dto;
  }

  private static List<CharacteristicDto> subCharacteristics(final Integer parentId, List<CharacteristicDto> allCharacteristics) {
    return newArrayList(Iterables.filter(allCharacteristics, new Predicate<CharacteristicDto>() {
      @Override
      public boolean apply(CharacteristicDto input) {
        return parentId.equals(input.getParentId());
      }
    }));
  }

  private static RuleDebt toRuleDebt(RuleDto rule, String characteristicKey) {
    RuleDebt ruleDebt = new RuleDebt().setRuleKey(RuleKey.of(rule.getRepositoryKey(), rule.getRuleKey()));
    String function = rule.getRemediationFunction() != null ? rule.getRemediationFunction() : rule.getDefaultRemediationFunction();
    String factor = rule.getRemediationFactor() != null ? rule.getRemediationFactor() : rule.getDefaultRemediationFactor();
    String offset = rule.getRemediationOffset() != null ? rule.getRemediationOffset() : rule.getDefaultRemediationOffset();
    ruleDebt.setCharacteristicKey(characteristicKey);
    ruleDebt.setFunction(DebtRemediationFunction.Type.valueOf(function));
    ruleDebt.setFactor(factor);
    ruleDebt.setOffset(offset);
    return ruleDebt;
  }

  private static CharacteristicDto toDto(DebtCharacteristic characteristic, @Nullable Integer parentId) {
    return new CharacteristicDto()
      .setKey(characteristic.key())
      .setName(characteristic.name())
      .setOrder(characteristic.order())
      .setParentId(parentId)
      .setEnabled(true)
      .setCreatedAt(characteristic.createdAt())
      .setUpdatedAt(characteristic.updatedAt());
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
    UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
  }

}

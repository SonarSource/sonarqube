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
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.TechnicalDebtModelRepository;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class DebtModelRestore implements ServerComponent {

  private final MyBatis mybatis;
  private final CharacteristicDao dao;
  private final RuleDao ruleDao;
  private final DebtModelOperations debtModelOperations;
  private final TechnicalDebtModelRepository debtModelPluginRepository;
  private final RuleRepositories ruleRepositories;
  private final DebtCharacteristicsXMLImporter characteristicsXMLImporter;
  private final DebtRulesXMLImporter rulesXMLImporter;
  private final System2 system2;

  public DebtModelRestore(MyBatis mybatis, CharacteristicDao dao, RuleDao ruleDao, DebtModelOperations debtModelOperations, TechnicalDebtModelRepository debtModelPluginRepository,
                          RuleRepositories ruleRepositories, DebtCharacteristicsXMLImporter characteristicsXMLImporter, DebtRulesXMLImporter rulesXMLImporter) {
    this(mybatis, dao, ruleDao, debtModelOperations, debtModelPluginRepository, ruleRepositories, characteristicsXMLImporter, rulesXMLImporter, System2.INSTANCE);
  }

  @VisibleForTesting
  DebtModelRestore(MyBatis mybatis, CharacteristicDao dao, RuleDao ruleDao, DebtModelOperations debtModelOperations, TechnicalDebtModelRepository debtModelPluginRepository,
                   RuleRepositories ruleRepositories, DebtCharacteristicsXMLImporter characteristicsXMLImporter, DebtRulesXMLImporter rulesXMLImporter,
                   System2 system2) {
    this.mybatis = mybatis;
    this.dao = dao;
    this.ruleDao = ruleDao;
    this.debtModelOperations = debtModelOperations;
    this.debtModelPluginRepository = debtModelPluginRepository;
    this.ruleRepositories = ruleRepositories;
    this.characteristicsXMLImporter = characteristicsXMLImporter;
    this.rulesXMLImporter = rulesXMLImporter;
    this.system2 = system2;
  }

  /**
   * Restore from provided model
   */
  public ValidationMessages restore() {
    ValidationMessages validationMessages = ValidationMessages.create();
    restore(loadModelFromPlugin(TechnicalDebtModelRepository.DEFAULT_MODEL), Collections.<DebtRulesXMLImporter.RuleDebt>emptyList(),
      Collections.<RuleRepositories.Repository>emptyList(), false, validationMessages);
    return validationMessages;
  }

  /**
   * Restore from plugins providing rules for a given language
   */
  public ValidationMessages restore(String languageKey) {
    ValidationMessages validationMessages = ValidationMessages.create();
    restore(loadModelFromPlugin(TechnicalDebtModelRepository.DEFAULT_MODEL), Collections.<DebtRulesXMLImporter.RuleDebt>emptyList(),
      ruleRepositories.repositoriesForLang(languageKey), false, validationMessages);
    return validationMessages;
  }

  /**
   * Restore model from a given XML model
   */
  public ValidationMessages restoreFromXml(String xml) {
    DebtModel debtModel = characteristicsXMLImporter.importXML(xml);
    ValidationMessages validationMessages = ValidationMessages.create();
    List<DebtRulesXMLImporter.RuleDebt> ruleDebts = rulesXMLImporter.importXML(xml, validationMessages);
    restore(debtModel, ruleDebts, Collections.<RuleRepositories.Repository>emptyList(), true, validationMessages);
    return validationMessages;
  }

  /**
   * Restore model from a given XML model and a given language
   */
  public ValidationMessages restoreFromXml(String xml, String languageKey) {
    DebtModel debtModel = characteristicsXMLImporter.importXML(xml);
    ValidationMessages validationMessages = ValidationMessages.create();
    List<DebtRulesXMLImporter.RuleDebt> ruleDebts = rulesXMLImporter.importXML(xml, validationMessages);
    restore(debtModel, ruleDebts, ruleRepositories.repositoriesForLang(languageKey), true, validationMessages);
    return validationMessages;
  }

  private void restore(DebtModel modelToImport, List<DebtRulesXMLImporter.RuleDebt> ruleDebts, Collection<RuleRepositories.Repository> repositories,
                       boolean disableCharacteristicWhenRuleNotFound, ValidationMessages validationMessages) {
    checkPermission();

    Date updateDate = new Date(system2.now());
    SqlSession session = mybatis.openSession();
    try {
      List<CharacteristicDto> persisted = dao.selectEnabledCharacteristics();
      List<CharacteristicDto> characteristicDtos = restoreCharacteristics(modelToImport, persisted, updateDate, session);
      restoreRules(characteristicDtos, repositories, ruleDebts, disableCharacteristicWhenRuleNotFound, validationMessages, updateDate, session);

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void restoreRules(List<CharacteristicDto> characteristicDtos, Collection<RuleRepositories.Repository> repositories, List<DebtRulesXMLImporter.RuleDebt> ruleDebts,
                            boolean disableCharacteristicWhenRuleNotFound, ValidationMessages validationMessages, Date updateDate, SqlSession session) {
    List<String> repositoryKeys = newArrayList(Iterables.transform(repositories, new Function<RuleRepositories.Repository, String>() {
      @Override
      public String apply(RuleRepositories.Repository input) {
        return input.getKey();
      }
    }));
    for (RuleDto rule : ruleDao.selectEnablesAndNonManual(session)) {
      if (repositories.isEmpty() || repositoryKeys.contains(rule.getRepositoryKey())) {
        DebtRulesXMLImporter.RuleDebt ruleDebt = ruleDebtByRule(rule, ruleDebts);
        if (ruleDebt == null) {
          rule.setCharacteristicId(disableCharacteristicWhenRuleNotFound ? RuleDto.DISABLED_CHARACTERISTIC_ID : null);
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

    for (DebtRulesXMLImporter.RuleDebt ruleDebt : ruleDebts) {
      validationMessages.addWarningText(String.format("The rule '%s' does not exist.", ruleDebt.ruleKey()));
    }
  }

  static boolean isSameRemediationFunction(DebtRulesXMLImporter.RuleDebt ruleDebt, RuleDto rule) {
    return new EqualsBuilder()
      .append(ruleDebt.function().name(), rule.getDefaultRemediationFunction())
      .append(ruleDebt.factor(), rule.getDefaultRemediationFactor())
      .append(ruleDebt.offset(), rule.getDefaultRemediationOffset())
      .isEquals();
  }

  @VisibleForTesting
  List<CharacteristicDto> restoreCharacteristics(DebtModel targetModel, List<CharacteristicDto> sourceCharacteristics, Date updateDate, SqlSession session) {
    List<CharacteristicDto> result = newArrayList();

    // Restore not existing characteristics
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

  private DebtModel loadModelFromPlugin(String pluginKey) {
    Reader xmlFileReader = null;
    try {
      xmlFileReader = debtModelPluginRepository.createReaderForXMLFile(pluginKey);
      return characteristicsXMLImporter.importXML(xmlFileReader);
    } finally {
      IOUtils.closeQuietly(xmlFileReader);
    }
  }

  private CharacteristicDto characteristicByKey(final String key, List<CharacteristicDto> existingModel, boolean canByNull) {
    CharacteristicDto dto = Iterables.find(existingModel, new Predicate<CharacteristicDto>() {
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

  @CheckForNull
  private DebtRulesXMLImporter.RuleDebt ruleDebtByRule(final RuleDto rule, List<DebtRulesXMLImporter.RuleDebt> ruleDebts) {
    if (ruleDebts.isEmpty()) {
      return null;
    }
    return Iterables.find(ruleDebts, new Predicate<DebtRulesXMLImporter.RuleDebt>() {
      @Override
      public boolean apply(DebtRulesXMLImporter.RuleDebt input) {
        return rule.getRepositoryKey().equals(input.ruleKey().repository()) && rule.getRuleKey().equals(input.ruleKey().rule());
      }
    }, null);
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

  private void checkPermission() {
    UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
  }


}

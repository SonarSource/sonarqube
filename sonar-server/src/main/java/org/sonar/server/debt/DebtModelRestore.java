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
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.utils.System2;
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
  private final DebtCharacteristicsXMLImporter importer;
  private final System2 system2;

  public DebtModelRestore(MyBatis mybatis, CharacteristicDao dao, RuleDao ruleDao, DebtModelOperations debtModelOperations, TechnicalDebtModelRepository debtModelPluginRepository,
                          RuleRepositories ruleRepositories, DebtCharacteristicsXMLImporter importer) {
    this(mybatis, dao, ruleDao, debtModelOperations, debtModelPluginRepository, ruleRepositories, importer, System2.INSTANCE);
  }

  @VisibleForTesting
  DebtModelRestore(MyBatis mybatis, CharacteristicDao dao, RuleDao ruleDao, DebtModelOperations debtModelOperations, TechnicalDebtModelRepository debtModelPluginRepository,
                   RuleRepositories ruleRepositories, DebtCharacteristicsXMLImporter importer,
                   System2 system2) {
    this.mybatis = mybatis;
    this.dao = dao;
    this.ruleDao = ruleDao;
    this.debtModelOperations = debtModelOperations;
    this.debtModelPluginRepository = debtModelPluginRepository;
    this.ruleRepositories = ruleRepositories;
    this.importer = importer;
    this.system2 = system2;
  }

  /**
   * Restore from provided model
   */
  public void restore() {
    restore(Collections.<RuleRepositories.Repository>emptyList());
  }

  /**
   * Restore from plugins providing rules for a given language
   */
  public void restore(String languageKey) {
    restore(ruleRepositories.repositoriesForLang(languageKey));
  }

  private void restore(Collection<RuleRepositories.Repository> repositories) {
    checkPermission();

    Date updateDate = new Date(system2.now());
    SqlSession session = mybatis.openSession();
    try {
      List<CharacteristicDto> persisted = dao.selectEnabledCharacteristics();
      DebtModel providedModel = loadModelFromXml(TechnicalDebtModelRepository.DEFAULT_MODEL);
      restoreCharacteristics(providedModel, persisted, updateDate, session);
      resetOverridingRuleDebt(repositories, updateDate, session);

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void resetOverridingRuleDebt(Collection<RuleRepositories.Repository> repositories, Date updateDate, SqlSession session) {
    List<String> repositoryKeys = newArrayList(Iterables.transform(repositories, new Function<RuleRepositories.Repository, String>() {
      @Override
      public String apply(RuleRepositories.Repository input) {
        return input.getKey();
      }
    }));
    for (RuleDto rule : ruleDao.selectOverridingDebt(repositoryKeys, session)) {
      rule.setCharacteristicId(null);
      rule.setRemediationFunction(null);
      rule.setRemediationFactor(null);
      rule.setRemediationOffset(null);
      rule.setUpdatedAt(updateDate);
      ruleDao.update(rule, session);
      // TODO index rules in E/S
    }
  }

  @VisibleForTesting
  void restoreCharacteristics(DebtModel targetModel, List<CharacteristicDto> sourceCharacteristics, Date updateDate, SqlSession session) {
    // Restore not existing characteristics
    for (DebtCharacteristic characteristic : targetModel.rootCharacteristics()) {
      CharacteristicDto rootCharacteristicDto = restoreCharacteristic(characteristic, null, sourceCharacteristics, updateDate, session);
      for (DebtCharacteristic subCharacteristic : targetModel.subCharacteristics(characteristic.key())) {
        restoreCharacteristic(subCharacteristic, rootCharacteristicDto.getId(), sourceCharacteristics, updateDate, session);
      }
    }
    // Disable no more existing characteristics
    for (CharacteristicDto sourceCharacteristic : sourceCharacteristics) {
      if (targetModel.characteristicByKey(sourceCharacteristic.getKey()) == null) {
        debtModelOperations.disableCharacteristic(sourceCharacteristic, updateDate, session);
      }
    }
  }

  private CharacteristicDto restoreCharacteristic(DebtCharacteristic targetCharacteristic, @Nullable Integer parentId, List<CharacteristicDto> sourceCharacteristics,
                                                  Date updateDate, SqlSession session) {
    CharacteristicDto sourceCharacteristic = dtoByKey(sourceCharacteristics, targetCharacteristic.key());
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

  private DebtModel loadModelFromXml(String pluginKey) {
    Reader xmlFileReader = null;
    try {
      xmlFileReader = debtModelPluginRepository.createReaderForXMLFile(pluginKey);
      return importer.importXML(xmlFileReader);
    } finally {
      IOUtils.closeQuietly(xmlFileReader);
    }
  }

  @CheckForNull
  private CharacteristicDto dtoByKey(List<CharacteristicDto> existingModel, final String key) {
    return Iterables.find(existingModel, new Predicate<CharacteristicDto>() {
      @Override
      public boolean apply(CharacteristicDto input) {
        return key.equals(input.getKey());
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

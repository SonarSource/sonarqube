/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.core.technicaldebt;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerExtension;
import org.sonar.api.rules.Rule;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import java.io.Reader;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class TechnicalDebtModelSynchronizer implements ServerExtension {

  private static final Logger LOG = LoggerFactory.getLogger(TechnicalDebtModelSynchronizer.class);

  private final MyBatis mybatis;
  private final CharacteristicDao dao;
  private final TechnicalDebtModelRepository languageModelFinder;
  private final TechnicalDebtXMLImporter importer;

  public TechnicalDebtModelSynchronizer(MyBatis mybatis, CharacteristicDao dao,
                                        TechnicalDebtModelRepository modelRepository, TechnicalDebtXMLImporter importer) {
    this.mybatis = mybatis;
    this.dao = dao;
    this.languageModelFinder = modelRepository;
    this.importer = importer;
  }

  public List<CharacteristicDto> synchronize(ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    SqlSession session = mybatis.openSession();

    List<CharacteristicDto> model = newArrayList();
    try {
      model = synchronize(messages, rulesCache, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
    return model;
  }

  public List<CharacteristicDto> synchronize(ValidationMessages messages, TechnicalDebtRuleCache rulesCache, SqlSession session) {
    DefaultTechnicalDebtModel defaultModel = loadModelFromXml(TechnicalDebtModelRepository.DEFAULT_MODEL, messages, rulesCache);
    List<CharacteristicDto> model = loadOrCreateModelFromDb(defaultModel, session);
    disableRequirementsOnRemovedRules(model, rulesCache, session);
    mergePlugins(model, defaultModel, messages, rulesCache, session);
    messages.log(LOG);

    return model;
  }

  private List<CharacteristicDto> loadOrCreateModelFromDb(DefaultTechnicalDebtModel defaultModel, SqlSession session) {
    List<CharacteristicDto> characteristicDtos = loadModel();
    if (characteristicDtos.isEmpty()) {
      return createTechnicalDebtModel(defaultModel, session);
    }
    return characteristicDtos;
  }

  private List<CharacteristicDto> loadModel() {
    return dao.selectEnabledCharacteristics();
  }

  private List<CharacteristicDto> createTechnicalDebtModel(DefaultTechnicalDebtModel defaultModel, SqlSession session) {
    List<CharacteristicDto> characteristics = newArrayList();
    for (DefaultCharacteristic rootCharacteristic : defaultModel.rootCharacteristics()) {
      CharacteristicDto rootCharacteristicDto = CharacteristicDto.toDto(rootCharacteristic, null);
      dao.insert(rootCharacteristicDto, session);
      characteristics.add(rootCharacteristicDto);
      for (DefaultCharacteristic characteristic : rootCharacteristic.children()) {
        CharacteristicDto characteristicDto = CharacteristicDto.toDto(characteristic, rootCharacteristicDto.getId());
        dao.insert(characteristicDto, session);
        characteristics.add(characteristicDto);
      }
    }
    return characteristics;
  }

  private void mergePlugins(List<CharacteristicDto> existingModel, DefaultTechnicalDebtModel defaultModel, ValidationMessages messages, TechnicalDebtRuleCache rulesCache,
                            SqlSession session) {
    for (String pluginKey : getContributingPluginListWithoutSqale()) {
      DefaultTechnicalDebtModel pluginModel = loadModelFromXml(pluginKey, messages, rulesCache);
      checkPluginDoNotAddNewCharacteristic(pluginModel, defaultModel);
      mergePlugin(pluginModel, existingModel, messages, rulesCache, session);
    }
  }

  private void mergePlugin(DefaultTechnicalDebtModel pluginModel, List<CharacteristicDto> existingModel, ValidationMessages messages, TechnicalDebtRuleCache rulesCache,
                           SqlSession session) {
    if (!messages.hasErrors()) {
      for (DefaultRequirement pluginRequirement : pluginModel.requirements()) {
        Rule rule = rulesCache.getByRuleKey(pluginRequirement.ruleKey());
        if (!isRequirementExists(existingModel, rule)) {
          CharacteristicDto characteristicDto = findCharacteristic(existingModel, pluginRequirement.characteristic().key());
          Integer rootId = characteristicDto.getRootId();
          if (rootId == null) {
            throw new IllegalArgumentException("Requirement on rule '" + pluginRequirement.ruleKey() + "' should not be linked on a root characteristic.");
          }
          CharacteristicDto requirementDto = CharacteristicDto.toDto(pluginRequirement, characteristicDto.getId(), rootId, rule.getId());
          dao.insert(requirementDto, session);
        }
      }
    }
  }

  public DefaultTechnicalDebtModel loadModelFromXml(String pluginKey, ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    Reader xmlFileReader = null;
    try {
      xmlFileReader = languageModelFinder.createReaderForXMLFile(pluginKey);
      return importer.importXML(xmlFileReader, messages, rulesCache);
    } finally {
      IOUtils.closeQuietly(xmlFileReader);
    }
  }

  private void checkPluginDoNotAddNewCharacteristic(DefaultTechnicalDebtModel pluginModel, DefaultTechnicalDebtModel defaultModel) {
    List<DefaultCharacteristic> characteristics = defaultModel.characteristics();
    for (DefaultCharacteristic characteristic : pluginModel.characteristics()) {
      if (!characteristics.contains(characteristic)) {
        throw new IllegalArgumentException("The characteristic : " + characteristic.key() + " cannot be used as it's not available in default characteristics.");
      }
    }
  }

  private void disableRequirementsOnRemovedRules(List<CharacteristicDto> existingModel, TechnicalDebtRuleCache rulesCache, SqlSession session) {
    for (CharacteristicDto characteristicDto : existingModel) {
      Integer ruleId = characteristicDto.getRuleId();
      if (ruleId != null && !rulesCache.exists(ruleId)) {
        dao.disable(characteristicDto.getId(), session);
      }
    }
  }

  private Collection<String> getContributingPluginListWithoutSqale() {
    Collection<String> pluginList = newArrayList(languageModelFinder.getContributingPluginList());
    pluginList.remove(TechnicalDebtModelRepository.DEFAULT_MODEL);
    return pluginList;
  }

  private CharacteristicDto findCharacteristic(List<CharacteristicDto> existingModel, final String key) {
    return Iterables.find(existingModel, new Predicate<CharacteristicDto>() {
      @Override
      public boolean apply(CharacteristicDto input) {
        String key = input.getKey();
        return input.getRuleId() == null && key != null && key.equals(key);
      }
    });
  }

  private boolean isRequirementExists(List<CharacteristicDto> existingModel, final Rule rule) {
    return Iterables.any(existingModel, new Predicate<CharacteristicDto>() {
      @Override
      public boolean apply(CharacteristicDto input) {
        Integer ruleId = input.getRuleId();
        return ruleId != null && ruleId.equals(rule.getId());
      }
    });
  }

}

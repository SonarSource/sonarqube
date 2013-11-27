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

import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerExtension;
import org.sonar.api.technicaldebt.Characteristic;
import org.sonar.api.technicaldebt.Requirement;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.persistence.MyBatis;

import java.io.Reader;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class TechnicalDebtManager implements ServerExtension {

  private static final Logger LOG = LoggerFactory.getLogger(TechnicalDebtManager.class);

  private final MyBatis mybatis;
  private final TechnicalDebtModelService service;
  private final TechnicalDebtFinder modelFinder;
  private final TechnicalDebtModelRepository languageModelFinder;
  private final TechnicalDebtXMLImporter importer;

  public TechnicalDebtManager(MyBatis mybatis, TechnicalDebtModelService service, TechnicalDebtFinder modelFinder,
                              TechnicalDebtModelRepository modelRepository, TechnicalDebtXMLImporter importer) {
    this.mybatis = mybatis;
    this.service = service;
    this.modelFinder = modelFinder;
    this.languageModelFinder = modelRepository;
    this.importer = importer;
  }

  public TechnicalDebtModel initAndMergePlugins(ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    SqlSession session = mybatis.openSession();

    TechnicalDebtModel model = null;
    try {
      model = initAndMergePlugins(messages, rulesCache, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
    return model;
  }

  public TechnicalDebtModel initAndMergePlugins(ValidationMessages messages, TechnicalDebtRuleCache rulesCache, SqlSession session) {
    TechnicalDebtModel defaultModel = loadModelFromXml(TechnicalDebtModelRepository.DEFAULT_MODEL, messages, rulesCache);
    TechnicalDebtModel model = loadOrCreateModelFromDb(defaultModel, messages, session);
    disableRequirementsOnRemovedRules(model, session);
    mergePlugins(model, defaultModel, messages, rulesCache, session);
    messages.log(LOG);

    return model;
  }

  public TechnicalDebtModel loadModel() {
    return modelFinder.findAll();
  }

  private TechnicalDebtModel loadOrCreateModelFromDb(TechnicalDebtModel defaultModel, ValidationMessages messages, SqlSession session) {
    TechnicalDebtModel model = loadModel();
    if (model.isEmpty()) {
      createTechnicalDebtModel(defaultModel, session);
      return defaultModel;
    }
    return model;
  }

  private void createTechnicalDebtModel(TechnicalDebtModel defaultModel, SqlSession session) {
    for (Characteristic rootCharacteristic : defaultModel.rootCharacteristics()) {
      service.create(rootCharacteristic, session);
      for (Characteristic characteristic : rootCharacteristic.children()) {
        service.create(characteristic, session);
      }
    }
  }

  private void mergePlugins(TechnicalDebtModel existingModel, TechnicalDebtModel defaultModel, ValidationMessages messages, TechnicalDebtRuleCache rulesCache, SqlSession session) {
    for (String pluginKey : getContributingPluginListWithoutSqale()) {
      TechnicalDebtModel pluginModel = loadModelFromXml(pluginKey, messages, rulesCache);
      checkPluginDoNotAddNewCharacteristic(pluginModel, defaultModel);
      mergePlugin(pluginModel, existingModel, messages, rulesCache, session);
    }
  }

  public void mergePlugin(TechnicalDebtModel pluginModel, TechnicalDebtModel existingModel, ValidationMessages messages, TechnicalDebtRuleCache rulesCache, SqlSession session) {
    if (!messages.hasErrors()) {
      List<Requirement> existingRequirements = existingModel.requirements();
      for (Requirement pluginRequirement : pluginModel.requirements()) {
        if (!existingRequirements.contains(pluginRequirement)) {
          Characteristic characteristic = existingModel.characteristicByKey(pluginRequirement.characteristic().key());
          service.create(pluginRequirement, characteristic, rulesCache, session);
        }
      }
    }
  }

  public TechnicalDebtModel loadModelFromXml(String pluginKey, ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    Reader xmlFileReader = null;
    try {
      xmlFileReader = languageModelFinder.createReaderForXMLFile(pluginKey);
      return importer.importXML(xmlFileReader, messages, rulesCache);
    } finally {
      IOUtils.closeQuietly(xmlFileReader);
    }
  }

  private void checkPluginDoNotAddNewCharacteristic(TechnicalDebtModel pluginModel, TechnicalDebtModel defaultModel) {
    List<Characteristic> characteristics = defaultModel.characteristics();
    for (Characteristic characteristic : pluginModel.characteristics()) {
      if (!characteristics.contains(characteristic)) {
        throw new IllegalArgumentException("The characteristic : " + characteristic.key() + " cannot be used as it's not available in default characteristics.");
      }
    }
  }

  private void disableRequirementsOnRemovedRules(TechnicalDebtModel model, SqlSession session) {
    for (Requirement requirement : model.requirements()) {
      if (requirement.ruleKey() == null) {
        requirement.characteristic().removeRequirement(requirement);
        service.disable(requirement, session);
      }
    }
  }

  private Collection<String> getContributingPluginListWithoutSqale() {
    Collection<String> pluginList = newArrayList(languageModelFinder.getContributingPluginList());
    pluginList.remove(TechnicalDebtModelRepository.DEFAULT_MODEL);
    return pluginList;
  }

}

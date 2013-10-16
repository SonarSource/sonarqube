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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerExtension;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.qualitymodel.ModelFinder;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.io.Reader;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class TechnicalDebtManager implements ServerExtension {

  private static final Logger LOG = LoggerFactory.getLogger(TechnicalDebtManager.class);

  private static final int REQUIREMENT_LEVEL = 3;

  private DatabaseSessionFactory sessionFactory;
  private ModelFinder modelFinder;
  private TechnicalDebtModelRepository languageModelFinder;
  private TechnicalDebtXMLImporter importer;

  public TechnicalDebtManager(DatabaseSessionFactory sessionFactory, ModelFinder modelFinder,
                              TechnicalDebtModelRepository modelRepository, TechnicalDebtXMLImporter importer) {
    this.sessionFactory = sessionFactory;
    this.modelFinder = modelFinder;
    this.languageModelFinder = modelRepository;
    this.importer = importer;
  }

  public Model initAndMergePlugins(ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    DatabaseSession session = sessionFactory.getSession();

    Model model = initAndMergePlugins(messages, rulesCache, session);

    session.commit();
    return model;
  }

  public Model initAndMergePlugins(ValidationMessages messages, TechnicalDebtRuleCache rulesCache, DatabaseSession session) {
    disableRequirementsOnRemovedRules(rulesCache);

    Model defaultModel = loadModelFromXml(TechnicalDebtModelRepository.DEFAULT_MODEL, messages, rulesCache);
    Model model = loadOrCreateModelFromDb(defaultModel, messages, rulesCache);
    mergePlugins(model, defaultModel, messages, rulesCache);
    session.save(model);
    return model;
  }

  public Model loadModel(){
    return modelFinder.findByName(TechnicalDebtModel.MODEL_NAME);
  }

  private Model loadOrCreateModelFromDb(Model defaultModel, ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    Model model = loadModel();
    if (model == null) {
      model = Model.createByName(TechnicalDebtModel.MODEL_NAME);
    }
    mergePlugin(defaultModel, model, messages, rulesCache);
    return model;
  }

  private void mergePlugins(Model existingModel, Model defaultModel, ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    for (String pluginKey : getContributingPluginListWithoutSqale()) {
      Model pluginModel = loadModelFromXml(pluginKey, messages, rulesCache);
      checkPluginDoNotAddNewCharacteristic(pluginModel, defaultModel);
      mergePlugin(pluginModel, existingModel, messages, rulesCache);
    }
  }

  public void mergePlugin(Model pluginModel, Model existingModel, ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    messages.log(LOG);
    if (!messages.hasErrors()) {
      new TechnicalDebtMergeModel(existingModel).mergeWith(pluginModel, messages, rulesCache);
      messages.log(LOG);
    }
  }

  public Model loadModelFromXml(String pluginKey, ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    Reader xmlFileReader = null;
    try {
      xmlFileReader = languageModelFinder.createReaderForXMLFile(pluginKey);
      return importer.importXML(xmlFileReader, messages, rulesCache);
    } finally {
      IOUtils.closeQuietly(xmlFileReader);
    }
  }

  private void checkPluginDoNotAddNewCharacteristic(Model pluginModel, Model defaultModel) {
    List<Characteristic> defaultCharacteristics = defaultModel.getCharacteristics();
    for (Characteristic characteristic : pluginModel.getCharacteristics()) {
      if (!characteristic.hasRule() && !defaultCharacteristics.contains(characteristic)) {
        throw new IllegalArgumentException("The characteristic : " + characteristic.getKey() + " cannot be used as it's not available in default ones.");
      }
    }
  }

  private void disableRequirementsOnRemovedRules(TechnicalDebtRuleCache rulesCache) {
    Model existingModel = modelFinder.findByName(TechnicalDebtModel.MODEL_NAME);
    if (existingModel != null) {
      for (Characteristic requirement : existingModel.getCharacteristicsByDepth(REQUIREMENT_LEVEL)) {
        if (!rulesCache.exists(requirement.getRule())) {
          existingModel.removeCharacteristic(requirement);
        }
      }
    }
  }

  private Collection<String> getContributingPluginListWithoutSqale() {
    Collection<String> pluginList = newArrayList(languageModelFinder.getContributingPluginList());
    pluginList.remove(TechnicalDebtModelRepository.DEFAULT_MODEL);
    return pluginList;
  }

}

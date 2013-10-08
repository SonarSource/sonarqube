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
package org.sonar.server.technicaldebt;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerExtension;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.qualitymodel.ModelFinder;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.technicaldebt.*;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.io.Reader;
import java.util.Collection;

/**
 * TODO test merge properties + property.value + text_value
 */

public class TechnicalDebtManager implements ServerExtension {

  private static final Logger LOG = LoggerFactory.getLogger(TechnicalDebtManager.class);

  private static final int REQUIREMENT_LEVEL = 3;

  private DatabaseSessionFactory sessionFactory;
  private ModelFinder modelFinder;
  private TechnicalDebtModelFinder languageModelFinder;
  private TechnicalDebtXMLImporter importer;

  public TechnicalDebtManager(DatabaseSessionFactory sessionFactory, ModelFinder modelFinder,
                              TechnicalDebtModelFinder languageModelFinder, TechnicalDebtXMLImporter importer) {
    this.sessionFactory = sessionFactory;
    this.modelFinder = modelFinder;
    this.languageModelFinder = languageModelFinder;
    this.importer = importer;
  }

  public Model init(ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    DatabaseSession session = sessionFactory.getSession();

    disableRequirementsOnRemovedRules(rulesCache);

    Model defaultModel = loadModelFromXml(TechnicalDebtModelFinder.DEFAULT_MODEL, messages, rulesCache);
    Model model = loadOrCreateModelFromDb(defaultModel, messages, rulesCache);
    loadRequirementsFromPlugins(model, defaultModel, messages, rulesCache);

    session.save(model);
    session.commit();
    return model;
  }

  private Model loadOrCreateModelFromDb(Model defaultModel, ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    Model model = modelFinder.findByName(TechnicalDebtModel.MODEL_NAME);
    if (model == null) {
      model = Model.createByName(TechnicalDebtModel.MODEL_NAME);
      merge(defaultModel, model, defaultModel, messages, rulesCache);
    }
    return model;
  }

  private void merge(Model pluginModel, Model existingModel, Model defaultModel, ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    messages.log(LOG);
    if (!messages.hasErrors()) {
      new TechnicalDebtMergeModel(existingModel, defaultModel.getCharacteristics()).mergeWith(pluginModel, messages, rulesCache);
      messages.log(LOG);
    }
  }

  private void loadRequirementsFromPlugins(Model existingModel, Model defaultModel, ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    for (String pluginKey : getContributingPluginListWithoutSqale()) {
      Model pluginModel = loadModelFromXml(pluginKey, messages, rulesCache);
      merge(pluginModel, existingModel, defaultModel, messages, rulesCache);
    }
  }

  private Model loadModelFromXml(String pluginKey, ValidationMessages messages, TechnicalDebtRuleCache rulesCache) {
    Reader xmlFileReader = null;
    try {
      xmlFileReader = languageModelFinder.createReaderForXMLFile(pluginKey);
      return importer.importXML(xmlFileReader, messages, rulesCache);
    } finally {
      IOUtils.closeQuietly(xmlFileReader);
    }
  }

  /**
   * Disable requirements linked on removed rules
   */
  private void disableRequirementsOnRemovedRules(TechnicalDebtRuleCache rulesCache) {
    Model existingModel = modelFinder.findByName(TechnicalDebtModel.MODEL_NAME);
    if (existingModel != null) {
      for (Characteristic requirement : existingModel.getCharacteristicsByDepth(REQUIREMENT_LEVEL)) {
        if (!rulesCache.exists(requirement.getRule())) {
          existingModel.removeCharacteristic(requirement);
        }
      }
      sessionFactory.getSession().commit();
    }
  }

  private Collection<String> getContributingPluginListWithoutSqale() {
    return languageModelFinder.getContributingPluginList();
  }

}

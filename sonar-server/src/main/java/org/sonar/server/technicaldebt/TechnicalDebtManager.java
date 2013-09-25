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
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.startup.RegisterTechnicalDebtModel;

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
  private XMLImporter importer;

  public TechnicalDebtManager(DatabaseSessionFactory sessionFactory, ModelFinder modelFinder,
                              TechnicalDebtModelFinder languageModelFinder, XMLImporter importer) {
    this.sessionFactory = sessionFactory;
    this.modelFinder = modelFinder;
    this.languageModelFinder = languageModelFinder;
    this.importer = importer;
  }

  public Model reset(ValidationMessages messages, RuleCache rulesCache) {
    DatabaseSession session = sessionFactory.getSession();

    resetExistingModel();
    Model model = populateModel(messages, rulesCache);

    session.save(model);
    session.commit();
    return model;
  }

  private Model populateModel(ValidationMessages messages, RuleCache ruleCache) {
    Model model = getModel();
    importDefaultModel(model, messages, ruleCache);
    populateModelWithInitialValues(model, messages, ruleCache);
    return model;
  }

  private Model getModel() {
    Model existingModel = modelFinder.findByName(RegisterTechnicalDebtModel.TECHNICAL_DEBT_MODEL);
    if (existingModel == null) {
      return Model.createByName(RegisterTechnicalDebtModel.TECHNICAL_DEBT_MODEL);
    }
    return existingModel;
  }

  private void populateModelWithInitialValues(Model initialModel, ValidationMessages messages, RuleCache ruleCache) {
    for (String pluginKey : getContributingPluginListWithoutSqale()) {
      mergePlugin(initialModel, pluginKey, messages, ruleCache);
    }
  }

  private void importDefaultModel(Model initialModel, ValidationMessages messages, RuleCache ruleCache) {
    mergePlugin(initialModel, TechnicalDebtModelFinder.DEFAULT_MODEL, messages, ruleCache);
  }

  private void mergePlugin(Model initialModel, String pluginKey, ValidationMessages messages, RuleCache ruleCache) {
    Model model = null;
    Reader xmlFileReader = null;
    try {
      xmlFileReader = languageModelFinder.createReaderForXMLFile(pluginKey);
      model = importer.importXML(xmlFileReader, messages, ruleCache);
    } finally {
      IOUtils.closeQuietly(xmlFileReader);
    }
    messages.log(LOG);
    if (!messages.hasErrors()) {
      new TechnicalDebtModel(initialModel).mergeWith(model, messages, ruleCache);
      messages.log(LOG);
    }
  }

  private void resetExistingModel() {
    Model existingModel = modelFinder.findByName(RegisterTechnicalDebtModel.TECHNICAL_DEBT_MODEL);
    if (existingModel != null) {
      for (Characteristic root : existingModel.getCharacteristicsByDepth(REQUIREMENT_LEVEL)) {
        existingModel.removeCharacteristic(root);
      }
      sessionFactory.getSession().commit();
    }
  }

  private Collection<String> getContributingPluginListWithoutSqale(){
    return languageModelFinder.getContributingPluginList();
  }

}

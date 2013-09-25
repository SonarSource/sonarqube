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

import java.io.Reader;
import java.util.Collection;
import java.util.List;

/**
 * TODO test merge properties + property.value + text_value
 */

public class TechnicalDebtManager implements ServerExtension {

  private static final Logger LOG = LoggerFactory.getLogger(TechnicalDebtManager.class);

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

  public void restore(Model model, ValidationMessages messages, RuleCache rulesCache) {
    Model persisted = modelFinder.findByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    if (persisted != null) {
      disable(persisted);
    }
    merge(model, messages, rulesCache);
  }

  public Model resetModel(ValidationMessages messages, RuleCache ruleCache) {
    Model persisted = modelFinder.findByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    if (persisted != null) {
      disable(persisted);
    }

    Model model = modelFinder.findByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    importDefaultSqaleModel(model, messages, ruleCache);
    populateModelWithInitialValues(model, messages, ruleCache);

    DatabaseSession session = sessionFactory.getSession();
    session.save(model);
    session.commit();

    return model;
  }

  public Model createInitialModel(ValidationMessages messages, RuleCache ruleCache) {
    Model initialModel = Model.createByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    importDefaultSqaleModel(initialModel, messages, ruleCache);
    populateModelWithInitialValues(initialModel, messages, ruleCache);
    return initialModel;
  }

  public void merge(List<String> pluginKeys, ValidationMessages messages, RuleCache ruleCache) {
    for (String pluginKey : pluginKeys) {
      ValidationMessages currentMessages = ValidationMessages.create();
      Model model = null;
      Reader xmlFileReader = null;
      try {
        xmlFileReader = languageModelFinder.createReaderForXMLFile(pluginKey);
        model = importer.importXML(xmlFileReader, currentMessages, ruleCache);
      } finally {
        IOUtils.closeQuietly(xmlFileReader);
      }
      if (!currentMessages.hasErrors()) {
        merge(model, messages, ruleCache);
      } else {
        for (String error : currentMessages.getErrors()) {
          messages.addErrorText(error);
        }
      }
    }
  }

  public void merge(Model with, ValidationMessages messages, RuleCache ruleCache) {
    DatabaseSession session = sessionFactory.getSession();
    Model sqale = modelFinder.findByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    if (sqale == null) {
      sqale = Model.createByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
      session.saveWithoutFlush(sqale);
    }
    new TechnicalDebtModel(sqale).mergeWith(with, messages, ruleCache);
    session.saveWithoutFlush(sqale);
    session.commit();
  }

  private void populateModelWithInitialValues(Model initialModel, ValidationMessages messages, RuleCache ruleCache) {
    for (String pluginKey : getContributingPluginListWithoutSqale()) {
      mergePlugin(initialModel, pluginKey, messages, ruleCache);
    }
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

  private void disable(Model persisted) {
    for (Characteristic root : persisted.getRootCharacteristics()) {
      persisted.removeCharacteristic(root);
    }
    sessionFactory.getSession().commit();
  }

  private Collection<String> getContributingPluginListWithoutSqale(){
    return languageModelFinder.getContributingPluginList();
  }

  private void importDefaultSqaleModel(Model initialModel, ValidationMessages messages, RuleCache ruleCache) {
    mergePlugin(initialModel, TechnicalDebtModelFinder.DEFAULT_MODEL, messages, ruleCache);
  }
}

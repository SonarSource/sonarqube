/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.qualitymodel;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.qualitymodel.ModelDefinition;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.SonarException;
import org.sonar.jpa.session.DatabaseSessionFactory;

import javax.persistence.Query;

public final class DefaultModelManager implements ServerComponent, ModelManager {

  private ModelDefinition[] definitions;
  private DatabaseSessionFactory sessionFactory;

  public DefaultModelManager(DatabaseSessionFactory sessionFactory, ModelDefinition[] definitions) {
    this.sessionFactory = sessionFactory;
    this.definitions = definitions;
  }

  /**
   * This constructor is used when there are no templates
   */
  public DefaultModelManager(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
    this.definitions = new ModelDefinition[0];
  }

  /**
   * Executed when the server starts
   */
  @Override
  public ModelManager registerDefinitions() {
    DatabaseSession session = sessionFactory.getSession();
    for (ModelDefinition definition : definitions) {
      if (StringUtils.isNotBlank(definition.getName()) && !exists(session, definition.getName())) {
        Logs.INFO.info("Register quality model: " + definition.getName());
        Model model = definition.createModel();
        if (StringUtils.isBlank(model.getName())) {
          model.setName(definition.getName());
        }
        insert(session, model);
        session.commit();
      }
    }
    return this;
  }

  @Override
  public Model reset(String name) {
    ModelDefinition definition = findDefinitionByName(name);
    if (definition == null) {
      throw new SonarException("Can not reset quality model. Definition not found: " + name);
    }

    LoggerFactory.getLogger(getClass()).info("Reset quality model: " + name);
    Model model = definition.createModel();
    return reset(model);
  }


  Model reset(Model model) {
    DatabaseSession session = sessionFactory.getSession();
    try {
      delete(session, model.getName());
      model = insert(session, model);
      session.commit();
      return model;

    } catch (RuntimeException e) {
      session.rollback();
      throw e;
    }
  }
  @Override
  public ModelDefinition findDefinitionByName(String name) {
    for (ModelDefinition definition : definitions) {
      if (StringUtils.equals(name, definition.getName())) {
        return definition;
      }
    }
    return null;
  }

  public static void delete(DatabaseSession session, String name) {
    Model model = session.getSingleResult(Model.class, "name", name);
    if (model != null) {
      session.removeWithoutFlush(model);
      session.commit();
    }
  }

  public static Model insert(DatabaseSession session, Model model) {
    return (Model) session.saveWithoutFlush(model);
  }

  public static boolean exists(DatabaseSession session, String name) {
    Query query = session.getEntityManager().createQuery("SELECT COUNT(qm) FROM " + Model.class.getSimpleName() + " qm WHERE qm.name=:name");
    query.setParameter("name", name);
    Number count = (Number) query.getSingleResult();
    return count.intValue() > 0;
  }
}

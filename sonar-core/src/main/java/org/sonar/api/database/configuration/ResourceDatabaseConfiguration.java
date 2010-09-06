/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.database.configuration;

import org.apache.commons.configuration.BaseConfiguration;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.api.database.model.ResourceModel;

import java.util.List;

/**
 *
 * IMPORTANT : This class can't be moved to org.sonar.jpa.dao for backward-compatibility reasons.
 * This class is still used in some plugins.
 * 
 * @since 1.10
 */
public class ResourceDatabaseConfiguration extends BaseConfiguration {
  private final DatabaseSessionFactory sessionFactory;
  private Integer resourceId = null;

  public ResourceDatabaseConfiguration(DatabaseSessionFactory sessionFactory, ResourceModel resource) {
    this.sessionFactory = sessionFactory;
    if (resource != null) {
      this.resourceId = resource.getId();
    }
    load();
  }

  public ResourceDatabaseConfiguration(DatabaseSessionFactory sessionFactory, Integer resourceId) {
    this.sessionFactory = sessionFactory;
    this.resourceId = resourceId;
    load();
  }

  public ResourceDatabaseConfiguration(DatabaseSessionFactory sessionFactory, String resourceKey) {
    this.sessionFactory = sessionFactory;

    ResourceModel resource = sessionFactory.getSession().getSingleResult(ResourceModel.class, "key", resourceKey);
    if (resource != null) {
      this.resourceId = resource.getId();
    }
    load();
  }

  public void load() {
    clear();

    loadResourceProperties();
  }

  private void loadResourceProperties() {
    if (resourceId != null) {
      List<Property> properties = sessionFactory.getSession()
          .createQuery("from " + Property.class.getSimpleName() + " p where p.resourceId=:resourceId and p.userId is null")
          .setParameter("resourceId", resourceId)
          .getResultList();

      registerProperties(properties);
    }
  }

  private void registerProperties(List<Property> properties) {
    if (properties != null) {
      for (Property property : properties) {
        setProperty(property.getKey(), property.getValue());
      }
    }
  }

}

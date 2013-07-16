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
package org.sonar.batch;

import org.apache.commons.configuration.BaseConfiguration;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.configuration.Property;
import org.sonar.api.database.model.ResourceModel;

import java.util.List;

/**
 * @deprecated in 3.7. Replaced by {@link org.sonar.api.config.Settings}
 */
@Deprecated
public class ResourceDatabaseConfiguration extends BaseConfiguration {
  private final DatabaseSession session;
  private Integer resourceId = null;

  public ResourceDatabaseConfiguration(DatabaseSession session, ResourceModel resource) {
    this.session = session;
    if (resource != null) {
      this.resourceId = resource.getId();
    }
    load();
  }

  public ResourceDatabaseConfiguration(DatabaseSession session, Integer resourceId) {
    this.session = session;
    this.resourceId = resourceId;
    load();
  }

  public ResourceDatabaseConfiguration(DatabaseSession session, String resourceKey) {
    this.session = session;

    ResourceModel resource = session.getSingleResult(ResourceModel.class, "key", resourceKey);
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
      List<Property> properties = session
        .createQuery("from " + Property.class.getSimpleName() + " p where p.resourceId=:resourceId")
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

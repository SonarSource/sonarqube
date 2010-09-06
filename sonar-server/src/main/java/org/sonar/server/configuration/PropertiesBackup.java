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
package org.sonar.server.configuration;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.collections.CollectionUtils;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.configuration.Property;

import java.util.ArrayList;
import java.util.List;

public class PropertiesBackup implements Backupable {

  private DatabaseSession databaseSession;
  private static final String FROM_GLOBAL_PROPERTIES = "from " + Property.class.getSimpleName() + " p WHERE p.resourceId IS NULL and user_id is null";

  public PropertiesBackup(DatabaseSession databaseSession) {
    this.databaseSession = databaseSession;
  }

  public void exportXml(SonarConfig sonarConfig) {
    List<Property> xmlProperties = new ArrayList<Property>();

    List<Property> dbProperties = databaseSession.createQuery(FROM_GLOBAL_PROPERTIES).getResultList();
    if (dbProperties != null) {
      for (Property dbProperty : dbProperties) {
        xmlProperties.add(new Property(dbProperty.getKey(), dbProperty.getValue()));
      }
      sonarConfig.setProperties(xmlProperties);
    }
  }

  public void importXml(SonarConfig sonarConfig) {
    clearProperties();

    if (CollectionUtils.isNotEmpty(sonarConfig.getProperties())) {
      for (Property xmlProperty : sonarConfig.getProperties()) {
        databaseSession.save(new Property(xmlProperty.getKey(), xmlProperty.getValue()));
      }
    }
  }

  private void clearProperties() {
    databaseSession.createQuery("delete " + FROM_GLOBAL_PROPERTIES).executeUpdate();
  }

  public void configure(XStream xStream) {
    xStream.alias("property", Property.class);
  }
}

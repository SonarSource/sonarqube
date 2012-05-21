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
package org.sonar.server.configuration;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
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
        String propKey = dbProperty.getKey();
        if (!CoreProperties.SERVER_ID.equals(propKey)) {
          // "sonar.core.id" must never be restored, it is unique for a server and it created once at the 1rst server startup
          xmlProperties.add(new Property(dbProperty.getKey(), dbProperty.getValue()));
        }
      }
      sonarConfig.setProperties(xmlProperties);
    }
  }

  public void importXml(SonarConfig sonarConfig) {
    LoggerFactory.getLogger(getClass()).info("Restore properties");
    clearProperties();

    if (CollectionUtils.isNotEmpty(sonarConfig.getProperties())) {
      for (Property xmlProperty : sonarConfig.getProperties()) {
        String propKey = xmlProperty.getKey();
        if (!CoreProperties.SERVER_ID.equals(propKey)) {
          // "sonar.core.id" must never be restored, it is unique for a server and it created once at the 1rst server startup
          databaseSession.save(new Property(propKey, xmlProperty.getValue()));
        }
      }
    }
  }

  private void clearProperties() {
    // "sonar.core.id" property should not be cleared, because it is the unique key used to identify the server
    // and it is used by the batch to verify that it connects to the same DB as the remote server (see SONAR-3126).
    databaseSession.createQuery("delete " + FROM_GLOBAL_PROPERTIES + " and prop_key != '" + CoreProperties.SERVER_ID + "'").executeUpdate();
  }

  public void configure(XStream xStream) {
    xStream.alias("property", Property.class);
  }
}

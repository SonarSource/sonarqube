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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.thoughtworks.xstream.XStream;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.configuration.Property;
import org.sonar.core.properties.PropertyDto;
import org.sonar.server.platform.PersistentSettings;

import java.util.List;
import java.util.Map;

public class PropertiesBackup implements Backupable {

  private final PersistentSettings persistentSettings;

  public PropertiesBackup(PersistentSettings persistentSettings) {
    this.persistentSettings = persistentSettings;
  }

  public void exportXml(SonarConfig sonarConfig) {
    List<Property> xmlProperties = Lists.newArrayList();

    for (PropertyDto property : persistentSettings.getGlobalProperties()) {
      // "sonar.core.id" must never be restored, it is unique for a server and it created once at the 1rst server startup
      if (!CoreProperties.SERVER_ID.equals(property.getKey())) {
        xmlProperties.add(new Property(property.getKey(), property.getValue()));
      }
    }
    sonarConfig.setProperties(xmlProperties);
  }

  public void importXml(SonarConfig sonarConfig) {
    LoggerFactory.getLogger(getClass()).info("Restore properties");

    // "sonar.core.id" property should not be cleared, because it is the unique key used to identify the server
    // and it is used by the batch to verify that it connects to the same DB as the remote server (see SONAR-3126).
    String serverId = persistentSettings.getString(CoreProperties.SERVER_ID);
    String serverStartTime = persistentSettings.getString(CoreProperties.SERVER_STARTTIME);

    Map<String, String> properties = Maps.newHashMap();
    if (sonarConfig.getProperties() != null && !sonarConfig.getProperties().isEmpty()) {
      for (Property xmlProperty : sonarConfig.getProperties()) {
        properties.put(xmlProperty.getKey(), xmlProperty.getValue());
      }
    }
    properties.put(CoreProperties.SERVER_ID, serverId);
    properties.put(CoreProperties.SERVER_STARTTIME, serverStartTime);
    persistentSettings.deleteProperties();
    persistentSettings.saveProperties(properties);
  }

  public void configure(XStream xStream) {
    xStream.alias("property", Property.class);
  }
}

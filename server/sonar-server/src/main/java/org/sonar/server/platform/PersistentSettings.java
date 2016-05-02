/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.picocontainer.Startable;
import org.sonar.api.config.Settings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

/**
 * @since 3.2
 */
public class PersistentSettings implements Startable {
  private final DbClient dbClient;
  private final PropertiesDao propertiesDao;
  private final ServerSettings serverSettings;

  public PersistentSettings(DbClient dbClient, ServerSettings serverSettings) {
    this.dbClient = dbClient;
    this.propertiesDao = dbClient.propertiesDao();
    this.serverSettings = serverSettings;
  }

  @Override
  public void start() {
    Map<String, String> databaseProperties = Maps.newHashMap();
    for (PropertyDto property : getGlobalProperties()) {
      databaseProperties.put(property.getKey(), property.getValue());
    }
    serverSettings.activateDatabaseSettings(databaseProperties);
  }

  @Override
  public void stop() {
    // nothing to do
  }

  public PersistentSettings saveProperty(String key, @Nullable String value) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      saveProperty(dbSession, key, value);
      dbSession.commit();
    } finally {
      dbClient.closeSession(dbSession);
    }
    return this;
  }

  public PersistentSettings saveProperty(DbSession dbSession, String key, @Nullable String value) {
    serverSettings.setProperty(key, value);
    propertiesDao.insertProperty(dbSession, new PropertyDto().setKey(key).setValue(value));
    return this;
  }

  public PersistentSettings deleteProperty(String key) {
    serverSettings.removeProperty(key);
    propertiesDao.deleteGlobalProperty(key);
    return this;
  }

  public PersistentSettings deleteProperties() {
    serverSettings.clear();
    propertiesDao.deleteGlobalProperties();
    return this;
  }

  public PersistentSettings saveProperties(Map<String, String> properties) {
    serverSettings.addProperties(properties);
    propertiesDao.insertGlobalProperties(properties);
    return this;
  }

  public String getString(String key) {
    return serverSettings.getString(key);
  }

  public Map<String, String> getProperties() {
    return serverSettings.getProperties();
  }

  public Settings getSettings() {
    return serverSettings.getSettings();
  }

  public List<PropertyDto> getGlobalProperties() {
    return propertiesDao.selectGlobalProperties();
  }
}

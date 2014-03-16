/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.platform;

import com.google.common.collect.Maps;
import org.picocontainer.Startable;
import org.sonar.api.config.Settings;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * @since 3.2
 */
public class PersistentSettings implements Startable {
  private final PropertiesDao propertiesDao;
  private final ServerSettings settings;

  public PersistentSettings(PropertiesDao propertiesDao, ServerSettings settings) {
    this.propertiesDao = propertiesDao;
    this.settings = settings;
  }

  @Override
  public void start() {
    Map<String, String> databaseProperties = Maps.newHashMap();
    for (PropertyDto property : getGlobalProperties()) {
      databaseProperties.put(property.getKey(), property.getValue());
    }
    settings.activateDatabaseSettings(databaseProperties);
  }

  @Override
  public void stop() {
    // nothing to do
  }

  public PersistentSettings saveProperty(String key, @Nullable String value) {
    settings.setProperty(key, value);
    propertiesDao.setProperty(new PropertyDto().setKey(key).setValue(value));
    return this;
  }

  public PersistentSettings deleteProperty(String key) {
    settings.removeProperty(key);
    propertiesDao.deleteGlobalProperty(key);
    return this;
  }

  public PersistentSettings deleteProperties() {
    settings.clear();
    propertiesDao.deleteGlobalProperties();
    return this;
  }

  public PersistentSettings saveProperties(Map<String, String> properties) {
    settings.addProperties(properties);
    propertiesDao.saveGlobalProperties(properties);
    return this;
  }

  public String getString(String key) {
    return settings.getString(key);
  }

  public Map<String, String> getProperties() {
    return settings.getProperties();
  }

  public Settings getSettings() {
    return settings;
  }

  public List<PropertyDto> getGlobalProperties() {
    return propertiesDao.selectGlobalProperties();
  }
}

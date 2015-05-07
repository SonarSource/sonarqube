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

package org.sonar.server.properties;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.sonar.api.ServerSide;
import org.sonar.api.config.Settings;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

import java.util.List;
import java.util.Map;

@ServerSide
public class ProjectSettingsFactory {

  private final PropertiesDao dao;
  private final Settings settings;

  public ProjectSettingsFactory(Settings settings, PropertiesDao dao) {
    this.dao = dao;
    this.settings = settings;
  }

  public Settings newProjectSettings(long projectId) {
    List<PropertyDto> propertyList = dao.selectProjectProperties(projectId);

    return new ProjectSettings(settings, getPropertyMap(propertyList));
  }

  @VisibleForTesting
  Map<String, String> getPropertyMap(List<PropertyDto> propertyDtoList) {
    Map<String, String> propertyMap = Maps.newHashMap();
    for (PropertyDto property : propertyDtoList) {
      String key = property.getKey();
      String value = property.getValue();
      propertyMap.put(key, value);
    }

    return propertyMap;
  }
}

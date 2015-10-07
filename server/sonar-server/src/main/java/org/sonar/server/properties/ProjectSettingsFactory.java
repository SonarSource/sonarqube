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

import java.util.List;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

@ServerSide
public class ProjectSettingsFactory {

  private final PropertiesDao dao;
  private final Settings settings;

  public ProjectSettingsFactory(Settings settings, PropertiesDao dao) {
    this.dao = dao;
    this.settings = settings;
  }

  public Settings newProjectSettings(String projectKey) {
    List<PropertyDto> propertyList = dao.selectProjectProperties(projectKey);
    Settings projectSettings = new Settings(settings);
    for (PropertyDto property : propertyList) {
      projectSettings.setProperty(property.getKey(), property.getValue());
    }
    return projectSettings;
  }
}

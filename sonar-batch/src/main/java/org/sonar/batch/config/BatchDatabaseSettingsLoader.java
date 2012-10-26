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
package org.sonar.batch.config;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

import java.util.List;

/**
 * @since 2.12
 */
public final class BatchDatabaseSettingsLoader {

  private PropertiesDao propertiesDao;
  private BootstrapSettings settings;
  private ProjectReactor reactor;

  public BatchDatabaseSettingsLoader(PropertiesDao propertiesDao, BootstrapSettings settings, ProjectReactor reactor) {
    this.propertiesDao = propertiesDao;
    this.settings = settings;
    this.reactor = reactor;
  }

  public void start() {
    String branch = settings.getString(CoreProperties.PROJECT_BRANCH_PROPERTY);
    String projectKey = reactor.getRoot().getKey();
    if (StringUtils.isNotBlank(branch)) {
      projectKey = String.format("%s:%s", projectKey, branch);
    }
    setIfNotDefined(propertiesDao.selectProjectProperties(projectKey));
    setIfNotDefined(propertiesDao.selectGlobalProperties());
    settings.updateDeprecatedCommonsConfiguration();
  }

  private void setIfNotDefined(List<PropertyDto> dbProperties) {
    for (PropertyDto dbProperty : dbProperties) {
      if (!settings.hasKey(dbProperty.getKey())) {
        settings.setProperty(dbProperty.getKey(), dbProperty.getValue());
      }
    }
  }
}

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

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.database.configuration.Property;
import org.sonar.core.config.ConfigurationUtils;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.List;

/**
 * @since 2.12
 */
public final class BatchSettingsEnhancer {

  private DatabaseSessionFactory dbFactory;
  private BatchSettings settings;
  private ProjectReactor reactor;

  public BatchSettingsEnhancer(DatabaseSessionFactory dbFactory, BatchSettings settings, ProjectReactor reactor) {
    this.dbFactory = dbFactory;
    this.settings = settings;
    this.reactor = reactor;
  }

  public void start() {
    setIfNotDefined(ConfigurationUtils.getProjectProperties(dbFactory, reactor.getRoot().getKey(), settings.getString(CoreProperties.PROJECT_BRANCH_PROPERTY)));
    setIfNotDefined(ConfigurationUtils.getGeneralProperties(dbFactory));
    settings.updateDeprecatedCommonsConfiguration();
  }

  private void setIfNotDefined(List<Property> dbProperties) {
    for (Property dbProperty : dbProperties) {
      if (!settings.hasKey(dbProperty.getKey())) {
        settings.setProperty(dbProperty.getKey(), dbProperty.getValue());
      }
    }
  }
}

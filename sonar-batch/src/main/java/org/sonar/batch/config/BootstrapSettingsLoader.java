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
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Property;
import org.sonar.wsclient.services.PropertyQuery;

import java.util.List;

/**
 * Load global settings and project settings. Note that the definition of modules is
 * incomplete before the execution of ProjectBuilder extensions, so module settings
 * are not loaded yet.
 * @since 3.4
 */
public final class BootstrapSettingsLoader {

  private BootstrapSettings settings;
  private ProjectReactor reactor;
  private Sonar wsClient;

  public BootstrapSettingsLoader(BootstrapSettings settings, ProjectReactor reactor, Sonar wsClient) {
    this.settings = settings;
    this.reactor = reactor;
    this.wsClient = wsClient;
  }

  public void start() {
    LoggerFactory.getLogger(BootstrapSettingsLoader.class).info("Load project settings");
    String branch = settings.getString(CoreProperties.PROJECT_BRANCH_PROPERTY);
    String projectKey = reactor.getRoot().getKey();
    if (StringUtils.isNotBlank(branch)) {
      projectKey = String.format("%s:%s", projectKey, branch);
    }
    List<Property> wsProperties = wsClient.findAll(PropertyQuery.createForAll().setResourceKeyOrId(projectKey));
    for (Property wsProperty : wsProperties) {
      setIfNotDefined(wsProperty);
    }
    settings.updateDeprecatedCommonsConfiguration();
  }

  private void setIfNotDefined(Property wsProperty) {
    if (!settings.hasKey(wsProperty.getKey())) {
      settings.setProperty(wsProperty.getKey(), wsProperty.getValue());
    }
  }
}

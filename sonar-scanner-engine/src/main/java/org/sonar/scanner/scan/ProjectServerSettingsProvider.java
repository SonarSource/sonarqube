/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.scanner.scan;

import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.CoreProperties;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.repository.settings.ProjectSettingsLoader;

public class ProjectServerSettingsProvider extends ProviderAdapter {

  private static final Logger LOG = Loggers.get(ProjectConfigurationProvider.class);

  private static final String MODULE_LEVEL_ARCHIVED_SETTINGS_WARNING = "Settings that were previously configured at " +
    "sub-project level are not used anymore. Transition the settings listed in ‘General Settings -> General -> " +
    "Archived Sub-Projects Settings' at project level, and clear the property to prevent the analysis from " +
    "displaying this warning.";

  private ProjectServerSettings singleton = null;

  public ProjectServerSettings provide(ProjectSettingsLoader loader, AnalysisWarnings analysisWarnings) {
    if (singleton == null) {
      Map<String, String> serverSideSettings = loader.loadProjectSettings();
      if (StringUtils.isNotBlank(serverSideSettings.get(CoreProperties.MODULE_LEVEL_ARCHIVED_SETTINGS))) {
        LOG.warn(MODULE_LEVEL_ARCHIVED_SETTINGS_WARNING);
        analysisWarnings.addUnique(MODULE_LEVEL_ARCHIVED_SETTINGS_WARNING);
      }
      singleton = new ProjectServerSettings(serverSideSettings);
    }
    return singleton;
  }
}

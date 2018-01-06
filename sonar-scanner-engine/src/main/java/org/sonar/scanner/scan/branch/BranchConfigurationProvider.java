/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.scan.branch;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.picocontainer.annotations.Nullable;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.bootstrap.ProjectKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.repository.settings.SettingsLoader;

public class BranchConfigurationProvider extends ProviderAdapter {

  private static final Logger LOG = Loggers.get(BranchConfigurationProvider.class);
  private static final String LOG_MSG = "Load branch configuration";

  private BranchConfiguration branchConfiguration = null;

  public BranchConfiguration provide(@Nullable BranchConfigurationLoader loader, GlobalConfiguration globalConfiguration, ProjectKey projectKey,
    SettingsLoader settingsLoader, ProjectBranches branches) {
    if (branchConfiguration == null) {
      if (loader == null) {
        branchConfiguration = new DefaultBranchConfiguration();
      } else {
        Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
        Supplier<Map<String, String>> settingsSupplier = createSettingsSupplier(globalConfiguration, projectKey, settingsLoader);
        branchConfiguration = loader.load(globalConfiguration.getProperties(), settingsSupplier, branches);
        profiler.stopInfo();
      }
    }
    return branchConfiguration;
  }

  private static Supplier<Map<String, String>> createSettingsSupplier(GlobalConfiguration globalConfiguration, ProjectKey projectKey, SettingsLoader settingsLoader) {
    return () -> {
      Map<String, String> settings = new HashMap<>();
      settings.putAll(globalConfiguration.getProperties());
      settings.putAll(settingsLoader.load(projectKey.get()));
      return settings;
    };
  }
}

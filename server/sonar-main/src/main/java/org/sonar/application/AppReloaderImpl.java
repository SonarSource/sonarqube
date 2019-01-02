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
package org.sonar.application;

import java.io.IOException;
import java.util.Objects;
import org.sonar.application.config.AppSettings;
import org.sonar.application.config.AppSettingsLoader;
import org.sonar.application.config.ClusterSettings;
import org.sonar.process.MessageException;
import org.sonar.process.Props;

import static java.lang.String.format;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;
import static org.sonar.process.ProcessProperties.Property.PATH_WEB;

public class AppReloaderImpl implements AppReloader {

  private final AppSettingsLoader settingsLoader;
  private final FileSystem fileSystem;
  private final AppState appState;
  private final AppLogging logging;

  public AppReloaderImpl(AppSettingsLoader settingsLoader, FileSystem fileSystem, AppState appState, AppLogging logging) {
    this.settingsLoader = settingsLoader;
    this.fileSystem = fileSystem;
    this.appState = appState;
    this.logging = logging;
  }

  @Override
  public void reload(AppSettings settings) throws IOException {
    if (ClusterSettings.isClusterEnabled(settings)) {
      throw new IllegalStateException("Restart is not possible with cluster mode");
    }
    AppSettings reloaded = settingsLoader.load();
    ensureUnchangedConfiguration(settings.getProps(), reloaded.getProps());
    settings.reload(reloaded.getProps());

    fileSystem.reset();
    logging.configure();
    appState.reset();
  }

  private static void ensureUnchangedConfiguration(Props oldProps, Props newProps) {
    verifyUnchanged(oldProps, newProps, PATH_DATA.getKey());
    verifyUnchanged(oldProps, newProps, PATH_WEB.getKey());
    verifyUnchanged(oldProps, newProps, PATH_LOGS.getKey());
    verifyUnchanged(oldProps, newProps, PATH_TEMP.getKey());
    verifyUnchanged(oldProps, newProps, CLUSTER_ENABLED.getKey());
  }

  private static void verifyUnchanged(Props initialProps, Props newProps, String propKey) {
    String initialValue = initialProps.nonNullValue(propKey);
    String newValue = newProps.nonNullValue(propKey);
    if (!Objects.equals(initialValue, newValue)) {
      throw new MessageException(format("Property [%s] cannot be changed on restart: [%s] => [%s]", propKey, initialValue, newValue));
    }
  }

}

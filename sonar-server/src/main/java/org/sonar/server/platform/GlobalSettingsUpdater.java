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
package org.sonar.server.platform;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.config.GlobalPropertyChangeHandler;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Update cache of global settings (see org.sonar.api.config.Settings) and notify org.sonar.api.config.GlobalPropertyChangeHandler extensions
 *
 * @since 3.0
 */
public class GlobalSettingsUpdater {
  private ServerSettings settings;
  private Configuration deprecatedConf;
  private List<GlobalPropertyChangeHandler> changeHandlers;

  public GlobalSettingsUpdater(ServerSettings settings, Configuration config, List<GlobalPropertyChangeHandler> changeHandlers) {
    this.settings = settings;
    this.deprecatedConf = config;
    this.changeHandlers = changeHandlers;
  }

  public GlobalSettingsUpdater(ServerSettings settings, Configuration config) {
    this(settings, config, Collections.<GlobalPropertyChangeHandler>emptyList());
  }

  public void setProperty(String key, @Nullable String value) {
    settings.setProperty(key, value);
    deprecatedConf.setProperty(key, value);

    GlobalPropertyChangeHandler.PropertyChange change = GlobalPropertyChangeHandler.PropertyChange.create(key, value);
    for (GlobalPropertyChangeHandler changeHandler : changeHandlers) {
      changeHandler.onChange(change);
    }
  }
}

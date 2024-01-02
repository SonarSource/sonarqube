/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db;

import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static org.sonar.api.CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS;

/**
 * Checks if there are any language specific parameters set for calculating technical debt as functionality is deprecated from 9.9.
 * If found, it is logged as a warning. This requires to be defined in platform level 4 ({@link org.sonar.server.platform.platformlevel.PlatformLevel4}).
 */
public class CheckLanguageSpecificParamsAtStartup implements Startable {

  private static final Logger LOG = Loggers.get(CheckLanguageSpecificParamsAtStartup.class);

  private final Configuration config;

  public CheckLanguageSpecificParamsAtStartup(Configuration config) {
    this.config = config;
  }

  @Override
  public void start() {
    String[] languageSpecificParams = config.getStringArray(LANGUAGE_SPECIFIC_PARAMETERS);
    if (languageSpecificParams.length > 0) {
      LOG.warn("The development cost used for calculating the technical debt is currently configured with {} language specific parameters [Key: languageSpecificParameters]. " +
        "Please be aware that this functionality is deprecated, and will be removed in a future version.", languageSpecificParams.length);
    }
  }

  @Override
  public void stop() {
    // do nothing
  }

}

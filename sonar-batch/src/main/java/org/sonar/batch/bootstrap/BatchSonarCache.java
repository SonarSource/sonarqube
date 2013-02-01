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
package org.sonar.batch.bootstrap;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.task.TaskExtension;
import org.sonar.batch.cache.SonarCache;

import java.io.File;

public class BatchSonarCache implements TaskExtension {

  private Settings settings;
  private SonarCache cache;

  public BatchSonarCache(Settings settings) {
    this.settings = settings;
  }

  public void start() {
    // Try to get Sonar user home from property
    String sonarUserHome = settings.getString(CoreProperties.SONAR_USER_HOME_PROPERTY);
    if (StringUtils.isBlank(sonarUserHome)) {
      // Try to get Sonar user home from environment variable
      sonarUserHome = settings.getString(CoreProperties.SONAR_USER_HOME);
    }
    if (StringUtils.isBlank(sonarUserHome)) {
      // Default
      sonarUserHome = System.getProperty("user.home") + File.separator + ".sonar";
    }

    SonarCache.Builder builder = SonarCache.create(new File(sonarUserHome));
    this.cache = builder.build();
  }

  public SonarCache getCache() {
    return cache;
  }
}

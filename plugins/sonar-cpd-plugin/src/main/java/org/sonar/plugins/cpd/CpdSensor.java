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
package org.sonar.plugins.cpd;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

public class CpdSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(CpdSensor.class);

  private CpdEngine sonarEngine;
  private CpdEngine sonarBridgeEngine;
  private Settings settings;

  public CpdSensor(SonarEngine sonarEngine, SonarBridgeEngine sonarBridgeEngine, Settings settings) {
    this.sonarEngine = sonarEngine;
    this.sonarBridgeEngine = sonarBridgeEngine;
    this.settings = settings;
  }

  public boolean shouldExecuteOnProject(Project project) {
    if (isSkipped(project)) {
      LOG.info("Detection of duplicated code is skipped");
      return false;
    }

    if (!getEngine(project).isLanguageSupported(project.getLanguage())) {
      LOG.debug("Detection of duplicated code is not supported for {}.", project.getLanguage());
      return false;
    }

    return true;
  }

  @VisibleForTesting
  CpdEngine getEngine(Project project) {
    if (sonarEngine.isLanguageSupported(project.getLanguage())) {
      return sonarEngine;
    } else {
      return sonarBridgeEngine;
    }
  }

  @VisibleForTesting
  boolean isSkipped(Project project) {
    String key = "sonar.cpd." + project.getLanguageKey() + ".skip";
    if (settings.hasKey(key)) {
      return settings.getBoolean(key);
    }
    return settings.getBoolean(CoreProperties.CPD_SKIP_PROPERTY);
  }

  public void analyse(Project project, SensorContext context) {
    CpdEngine engine = getEngine(project);
    LOG.info("{} is used", engine);
    engine.analyse(project, context);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}

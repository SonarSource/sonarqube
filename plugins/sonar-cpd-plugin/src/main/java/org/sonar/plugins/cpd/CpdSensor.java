/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.cpd;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ModuleLanguages;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;

public class CpdSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(CpdSensor.class);

  private CpdEngine sonarEngine;
  private CpdEngine sonarBridgeEngine;
  private Settings settings;
  private ModuleLanguages moduleLanguages;

  public CpdSensor(SonarEngine sonarEngine, SonarBridgeEngine sonarBridgeEngine, Settings settings, ModuleLanguages moduleLanguages) {
    this.sonarEngine = sonarEngine;
    this.sonarBridgeEngine = sonarBridgeEngine;
    this.settings = settings;
    this.moduleLanguages = moduleLanguages;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @VisibleForTesting
  CpdEngine getEngine(Language language) {
    if (sonarEngine.isLanguageSupported(language)) {
      return sonarEngine;
    } else {
      return sonarBridgeEngine;
    }
  }

  @VisibleForTesting
  boolean isSkipped(Language language) {
    String key = "sonar.cpd." + language.getKey() + ".skip";
    if (settings.hasKey(key)) {
      return settings.getBoolean(key);
    }
    return settings.getBoolean(CoreProperties.CPD_SKIP_PROPERTY);
  }

  public void analyse(Project project, SensorContext context) {
    for (Language language : moduleLanguages.getModuleLanguages()) {
      if (isSkipped(language)) {
        LOG.info("Detection of duplicated code is skipped for {}.", language);
        continue;
      }

      CpdEngine engine = getEngine(language);
      if (!engine.isLanguageSupported(language)) {
        LOG.debug("Detection of duplicated code is not supported for {}.", language);
        continue;
      }
      LOG.info("{} is used for {}", engine, language);
      engine.analyse(project, context);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}

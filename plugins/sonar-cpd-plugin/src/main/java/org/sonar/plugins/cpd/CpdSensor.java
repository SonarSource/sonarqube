/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.Logs;

public class CpdSensor implements Sensor {

  private CpdEngine sonarEngine;
  private CpdEngine pmdEngine;

  public CpdSensor(SonarEngine sonarEngine, PmdEngine pmdEngine) {
    this.sonarEngine = sonarEngine;
    this.pmdEngine = pmdEngine;
  }

  public boolean shouldExecuteOnProject(Project project) {
    if (isSkipped(project)) {
      LoggerFactory.getLogger(getClass()).info("Detection of duplicated code is skipped");
      return false;
    }

    if (!getEngine(project).isLanguageSupported(project.getLanguage())) {
      LoggerFactory.getLogger(getClass()).info("Detection of duplication code is not supported for {}.", project.getLanguage());
      return false;
    }

    return true;
  }

  private CpdEngine getEngine(Project project) {
    if (isSonarEngineEnabled(project)) {
      if (sonarEngine.isLanguageSupported(project.getLanguage())) {
        return sonarEngine;
      } else {
        // fallback to PMD
        return pmdEngine;
      }
    } else {
      return pmdEngine;
    }
  }

  boolean isSonarEngineEnabled(Project project) {
    Configuration conf = project.getConfiguration();
    return StringUtils.equalsIgnoreCase(conf.getString(CoreProperties.CPD_ENGINE, CoreProperties.CPD_ENGINE_DEFAULT_VALUE), "sonar");
  }

  boolean isSkipped(Project project) {
    Configuration conf = project.getConfiguration();
    return conf.getBoolean("sonar.cpd." + project.getLanguageKey() + ".skip",
        conf.getBoolean(CoreProperties.CPD_SKIP_PROPERTY, false));
  }

  public void analyse(Project project, SensorContext context) {
    CpdEngine engine = getEngine(project);
    Logs.INFO.info("{} would be used", engine);
    engine.analyse(project, context);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}

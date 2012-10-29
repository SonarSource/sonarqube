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
package org.sonar.plugins.cobertura;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.plugins.java.api.JavaSettings;

public class CoberturaSettings implements BatchExtension {
  private Settings settings;
  private JavaSettings javaSettings;

  public CoberturaSettings(Settings settings, JavaSettings javaSettings) {
    this.settings = settings;
    this.javaSettings = javaSettings;
  }

  public boolean isEnabled(Project project) {
    return Java.KEY.equals(project.getLanguageKey())
      && CoberturaPlugin.PLUGIN_KEY.equals(javaSettings.getEnabledCoveragePlugin())
      && !project.getFileSystem().mainFiles(Java.KEY).isEmpty()
      && project.getAnalysisType().isDynamic(true);
  }

  public String getMaxMemory() {
    // http://jira.codehaus.org/browse/SONAR-2897: there used to be a typo in the parameter name (was "sonar.cobertura.maxmen")
    return StringUtils.defaultIfEmpty(
      settings.getString("sonar.cobertura.maxmen"),
      settings.getString(CoreProperties.COBERTURA_MAXMEM_PROPERTY)
    );
  }
}

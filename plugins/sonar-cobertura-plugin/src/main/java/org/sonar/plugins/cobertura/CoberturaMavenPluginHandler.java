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
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.batch.maven.MavenSurefireUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.plugins.cobertura.api.CoberturaUtils;

public class CoberturaMavenPluginHandler implements MavenPluginHandler {

  private Settings settings;

  public CoberturaMavenPluginHandler(Settings settings) {
    this.settings = settings;
  }

  public String getGroupId() {
    return CoberturaUtils.COBERTURA_GROUP_ID;
  }

  public String getArtifactId() {
    return CoberturaUtils.COBERTURA_ARTIFACT_ID;
  }

  public String getVersion() {
    return "2.5.1";
  }

  public boolean isFixedVersion() {
    return false;
  }

  public String[] getGoals() {
    return new String[] { "cobertura" };
  }

  public void configure(Project project, MavenPlugin coberturaPlugin) {
    configureCobertura(project, coberturaPlugin);
    MavenSurefireUtils.configure(project);
  }

  private void configureCobertura(Project project, MavenPlugin coberturaPlugin) {
    coberturaPlugin.setParameter("formats/format", "xml");
    for (String pattern : project.getExclusionPatterns()) {
      if (pattern.endsWith(".java")) {
        pattern = StringUtils.substringBeforeLast(pattern, ".") + ".class";

      } else if (StringUtils.substringAfterLast(pattern, "/").indexOf(".") < 0) {
        pattern = pattern + ".class";
      }
      coberturaPlugin.addParameter("instrumentation/excludes/exclude", pattern);
    }
    String maxmem = "";
    // http://jira.codehaus.org/browse/SONAR-2897: there used to be a typo in the parameter name (was "sonar.cobertura.maxmen")
    if (settings.hasKey("sonar.cobertura.maxmen")) {
      maxmem = settings.getString("sonar.cobertura.maxmen");
    } else {
      // use the "normal" key
      maxmem = settings.getString(CoreProperties.COBERTURA_MAXMEM_PROPERTY);
    }
    coberturaPlugin.setParameter("maxmem", maxmem);
  }
}

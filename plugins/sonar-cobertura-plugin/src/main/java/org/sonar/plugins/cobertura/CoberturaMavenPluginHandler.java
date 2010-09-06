/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
import org.sonar.api.batch.maven.MavenUtils;
import org.sonar.api.resources.Project;

public class CoberturaMavenPluginHandler implements MavenPluginHandler {

  public static final String GROUP_ID = MavenUtils.GROUP_ID_CODEHAUS_MOJO;
  public static final String ARTIFACT_ID = "cobertura-maven-plugin";

  public String getGroupId() {
    return GROUP_ID;
  }

  public String getArtifactId() {
    return ARTIFACT_ID;
  }

  public String getVersion() {
    return "2.4";
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
    coberturaPlugin.setParameter("maxmem", project.getConfiguration().getString(CoreProperties.COBERTURA_MAXMEM_PROPERTY,
        CoreProperties.COBERTURA_MAXMEM_DEFAULT_VALUE));
  }
}

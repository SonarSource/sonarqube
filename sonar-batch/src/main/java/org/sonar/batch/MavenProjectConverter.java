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
package org.sonar.batch;

import com.google.common.collect.Maps;
import org.apache.maven.project.MavenProject;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrapper.ProjectDefinition;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class MavenProjectConverter {

  private MavenProjectConverter() {
  }

  public static ProjectDefinition convert(List<MavenProject> poms) {
    Map<String, MavenProject> paths = Maps.newHashMap(); // projects by canonical path
    Map<MavenProject, ProjectDefinition> defs = Maps.newHashMap();

    try {
      for (MavenProject pom : poms) {
        String basedir = pom.getBasedir().getCanonicalPath();
        paths.put(basedir, pom);
        defs.put(pom, convert(pom));
      }

      for (Map.Entry<String, MavenProject> entry : paths.entrySet()) {
        MavenProject pom = entry.getValue();
        for (Object moduleId : pom.getModules()) {
          File modulePath = new File(pom.getBasedir(), (String) moduleId);
          MavenProject module = paths.get(modulePath.getCanonicalPath());
          defs.get(pom).addModule(defs.get(module));
        }
      }
    } catch (IOException e) {
      throw new SonarException(e);
    }

    return defs.get(poms.get(0));
  }

  public static ProjectDefinition convert(MavenProject pom) {
    Properties properties = new Properties();

    String key = new StringBuilder().append(pom.getGroupId()).append(":").append(pom.getArtifactId()).toString();
    setProperty(properties, CoreProperties.PROJECT_KEY_PROPERTY, key);
    setProperty(properties, CoreProperties.PROJECT_VERSION_PROPERTY, pom.getVersion());
    setProperty(properties, CoreProperties.PROJECT_NAME_PROPERTY, pom.getName());
    setProperty(properties, CoreProperties.PROJECT_DESCRIPTION_PROPERTY, pom.getDescription());
    properties.putAll(pom.getModel().getProperties());

    ProjectDefinition def = new ProjectDefinition(pom.getBasedir(), null, properties); // TODO work directory ?
    def.addContainerExtension(pom);
    return def;
  }

  private static void setProperty(Properties properties, String key, String value) {
    if (value != null) {
      properties.setProperty(key, value);
    }
  }
}

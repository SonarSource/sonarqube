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
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class MavenProjectConverter {

  private MavenProjectConverter() {
    // only static methods
  }

  public static ProjectDefinition convert(List<MavenProject> poms, MavenProject root) {
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
          defs.get(pom).addSubProject(defs.get(module));
        }
      }
    } catch (IOException e) {
      throw new SonarException(e);
    }

    return defs.get(root);
  }

  /**
   * Visibility has been relaxed for tests.
   */
  static ProjectDefinition convert(MavenProject pom) {
    String key = new StringBuilder().append(pom.getGroupId()).append(":").append(pom.getArtifactId()).toString();
    ProjectDefinition definition = ProjectDefinition.create(pom.getModel().getProperties());
    definition.setKey(key)
        .setVersion(pom.getVersion())
        .setName(pom.getName())
        .setDescription(pom.getDescription())
        .addContainerExtension(pom);
    synchronizeFileSystem(pom, definition);
    return definition;
  }

  public static void synchronizeFileSystem(MavenProject pom, ProjectDefinition into) {
    into.setBaseDir(pom.getBasedir());
    into.setWorkDir(new File(resolvePath(pom.getBuild().getDirectory(), pom.getBasedir()), "sonar"));
    into.setSourceDirs((String[]) pom.getCompileSourceRoots().toArray(new String[pom.getCompileSourceRoots().size()]));
    into.setTestDirs((String[]) pom.getTestCompileSourceRoots().toArray(new String[pom.getTestCompileSourceRoots().size()]));
  }

  static File resolvePath(String path, File basedir) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      try {
        file = new File(basedir, path).getCanonicalFile();
      } catch (IOException e) {
        throw new SonarException("Unable to resolve path '" + path + "'", e);
      }
    }
    return file;
  }
}

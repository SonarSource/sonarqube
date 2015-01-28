/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.deprecated.components;

import com.google.common.collect.Lists;
import org.apache.maven.project.MavenProject;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.resources.ProjectFileSystem;

import javax.annotation.Nullable;

import java.io.File;
import java.util.List;

public class DefaultProjectClasspath extends ProjectClasspath {

  private ProjectDefinition def;
  private ProjectFileSystem projectFileSystem;

  public DefaultProjectClasspath(ProjectDefinition def, ProjectFileSystem projectFileSystem) {
    this(def, projectFileSystem, null);
  }

  public DefaultProjectClasspath(ProjectDefinition def, ProjectFileSystem projectFileSystem, @Nullable MavenProject pom) {
    super(pom);
    this.def = def;
    this.projectFileSystem = projectFileSystem;
  }

  @Override
  protected List<File> createElements() {
    if (pom != null) {
      return super.createElements();
    } else {
      List<File> elements = Lists.newArrayList();
      for (String path : def.getBinaries()) {
        elements.add(projectFileSystem.resolvePath(path));
      }
      for (String path : def.getLibraries()) {
        elements.add(projectFileSystem.resolvePath(path));
      }
      return elements;
    }
  }
}

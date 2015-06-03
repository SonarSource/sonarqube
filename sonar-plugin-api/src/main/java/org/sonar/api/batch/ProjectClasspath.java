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
package org.sonar.api.batch;

import com.google.common.collect.Lists;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.utils.SonarException;

/**
 * @since 2.2
 * @deprecated since 4.5 this is some Java specific stuff that should by handled by SQ Java plugin
 */
@Deprecated
public class ProjectClasspath implements BatchComponent {

  private final ProjectDefinition def;
  private final ProjectFileSystem projectFileSystem;
  private List<File> elements;
  private URLClassLoader classloader;

  public ProjectClasspath(ProjectDefinition def, ProjectFileSystem projectFileSystem) {
    this.def = def;
    this.projectFileSystem = projectFileSystem;
  }

  public URLClassLoader getClassloader() {
    if (classloader == null) {
      classloader = createClassLoader();
    }
    return classloader;
  }

  /**
   * bytecode directory + JARs (dependencies)
   */
  public List<File> getElements() {
    if (elements == null) {
      elements = createElements();
    }
    return elements;
  }

  protected URLClassLoader createClassLoader() {
    try {
      List<URL> urls = Lists.newArrayList();
      for (File file : getElements()) {
        urls.add(file.toURI().toURL());
      }
      return new URLClassLoader(urls.toArray(new URL[urls.size()]), null);

    } catch (MalformedURLException e) {
      throw new SonarException("Fail to create the project classloader. Classpath element is unvalid.", e);
    }
  }

  protected List<File> createElements() {
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

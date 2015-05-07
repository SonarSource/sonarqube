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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import org.sonar.api.BatchSide;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 2.2
 * @deprecated since 4.5 this is some Java specific stuff that should by handled by SQ Java plugin
 */
@Deprecated
@BatchSide
public class ProjectClasspath {

  protected MavenProject pom;
  private List<File> elements;
  private URLClassLoader classloader;

  public ProjectClasspath(MavenProject pom) {
    this.pom = pom;
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
      List<URL> urls = new ArrayList<>();
      for (File file : getElements()) {
        urls.add(file.toURI().toURL());
      }
      return new URLClassLoader(urls.toArray(new URL[urls.size()]), null);

    } catch (MalformedURLException e) {
      throw new SonarException("Fail to create the project classloader. Classpath element is unvalid.", e);
    }
  }

  protected List<File> createElements() {
    try {
      List<File> files = new ArrayList<>();
      if (pom.getCompileClasspathElements() != null) {
        for (String classPathString : pom.getCompileClasspathElements()) {
          files.add(new File(classPathString));
        }
      }

      if (pom.getBuild().getOutputDirectory() != null) {
        File outputDirectoryFile = new File(pom.getBuild().getOutputDirectory());
        if (outputDirectoryFile.exists()) {
          files.add(outputDirectoryFile);
        }
      }
      return files;
    } catch (DependencyResolutionRequiredException e) {
      throw new SonarException("Fail to create the project classloader", e);
    }
  }
}

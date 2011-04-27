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

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrapper.ProjectDefinition;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Implementation of {@link org.sonar.api.resources.ProjectFileSystem} based on {@link ProjectDefinition} and {@link MavenProject}.
 */
public class DefaultProjectFileSystem2 extends DefaultProjectFileSystem {

  private ProjectDefinition def;
  private MavenProject pom;

  public DefaultProjectFileSystem2(Project project, Languages languages, ProjectDefinition def) {
    super(project, languages);
    this.def = def;
  }

  /**
   * For Maven.
   */
  public DefaultProjectFileSystem2(Project project, Languages languages, ProjectDefinition def, MavenProject pom) {
    this(project, languages, def);
    this.pom = pom;
  }

  public File getBasedir() {
    if (pom != null) {
      return pom.getBasedir();
    } else {
      return def.getBaseDir();
    }
  }

  public File getBuildDir() {
    if (pom != null) {
      return resolvePath(pom.getBuild().getDirectory());
    } else {
      // TODO workaround
      return new File(getSonarWorkingDirectory(), "target");
    }
  }

  public File getBuildOutputDir() {
    if (pom != null) {
      return resolvePath(pom.getBuild().getOutputDirectory());
    } else {
      if (def.getBinaries().isEmpty()) {
        // workaround for IndexOutOfBoundsException
        return new File(getBuildDir(), "classes");
      }
      // TODO we assume that there is only one directory which contains compiled code
      return resolvePath(def.getBinaries().get(0));
    }
  }

  public List<File> getSourceDirs() {
    if (pom != null) {
      // Maven can modify source directories during Sonar execution - see MavenPhaseExecutor.
      return resolvePaths(pom.getCompileSourceRoots());
    } else {
      return resolvePaths(def.getSourceDirs());
    }
  }

  /**
   * @deprecated since 2.6, because should be immutable
   */
  @Deprecated
  public DefaultProjectFileSystem addSourceDir(File dir) {
    if (dir == null) {
      throw new IllegalArgumentException("Can not add null to project source dirs");
    }
    if (pom != null) {
      pom.getCompileSourceRoots().add(0, dir.getAbsolutePath());
    } else {
      def.addSourceDir(dir.getAbsolutePath());
    }
    return this;
  }

  /**
   * Maven can modify test directories during Sonar execution - see MavenPhaseExecutor.
   */
  public List<File> getTestDirs() {
    if (pom != null) {
      // Maven can modify test directories during Sonar execution - see MavenPhaseExecutor.
      return resolvePaths(pom.getTestCompileSourceRoots());
    } else {
      return resolvePaths(def.getTestDirs());
    }
  }

  /**
   * @deprecated since 2.6, because should be immutable
   */
  @Deprecated
  public DefaultProjectFileSystem addTestDir(File dir) {
    if (dir == null) {
      throw new IllegalArgumentException("Can not add null to project test dirs");
    }
    if (pom != null) {
      pom.getTestCompileSourceRoots().add(0, dir.getAbsolutePath());
    } else {
      def.addTestDir(dir.getAbsolutePath());
    }
    return this;
  }

  /**
   * TODO Godin: seems that used only by Cobertura and Clover
   */
  public File getReportOutputDir() {
    if (pom != null) {
      return resolvePath(pom.getReporting().getOutputDirectory());
    } else {
      return new File(getBuildDir(), "site");
    }
  }

  @Override
  public File getSonarWorkingDirectory() {
    if (pom != null) {
      try {
        File dir = new File(getBuildDir(), "sonar");
        FileUtils.forceMkdir(dir);
        return dir;

      } catch (IOException e) {
        throw new SonarException("Unable to retrieve Sonar working directory.", e);
      }
    } else {
      return def.getWorkDir();
    }
  }

}

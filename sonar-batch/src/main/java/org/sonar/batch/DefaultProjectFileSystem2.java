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
package org.sonar.batch;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
import org.sonar.api.batch.FileFilter;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Implementation of {@link org.sonar.api.resources.ProjectFileSystem} based on {@link ProjectDefinition} and {@link MavenProject}.
 */
public class DefaultProjectFileSystem2 extends DefaultProjectFileSystem {

  private ProjectDefinition def;
  private MavenProject pom;

  public DefaultProjectFileSystem2(Project project, Languages languages, ProjectDefinition def, FileFilter[] fileFilters) {
    super(project, languages, fileFilters);
    this.def = def;
  }

  /**
   * For Maven.
   */
  public DefaultProjectFileSystem2(Project project, Languages languages, ProjectDefinition def, MavenProject pom, FileFilter[] fileFilters) {
    this(project, languages, def, fileFilters);
    this.pom = pom;
  }

  public DefaultProjectFileSystem2(Project project, Languages languages, ProjectDefinition def) {
    this(project, languages, def, new FileFilter[0]);
  }

  /**
   * For Maven.
   */
  public DefaultProjectFileSystem2(Project project, Languages languages, ProjectDefinition def, MavenProject pom) {
    this(project, languages, def, pom, new FileFilter[0]);
  }

  @Override
  public File getBasedir() {
    return def.getBaseDir();
  }

  @Override
  public File getBuildDir() {
    if (pom != null) {
      return resolvePath(pom.getBuild().getDirectory());
    } else {
      // TODO workaround
      return new File(getSonarWorkingDirectory(), "target");
    }
  }

  @Override
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

  @Override
  public List<File> getSourceDirs() {
    List<File> unfiltered = resolvePaths(def.getSourceDirs());
    return ImmutableList.copyOf(Iterables.filter(unfiltered, DIRECTORY_EXISTS));
  }

  /**
   * @deprecated since 2.6, because should be immutable
   */
  @Override
  @Deprecated
  public DefaultProjectFileSystem addSourceDir(File dir) {
    if (dir == null) {
      throw new IllegalArgumentException("Can not add null to project source dirs");
    }
    if (pom != null) {
      pom.getCompileSourceRoots().add(0, dir.getAbsolutePath());
    }
    def.addSourceDirs(dir.getAbsolutePath());
    return this;
  }

  /**
   * Maven can modify test directories during Sonar execution - see MavenPhaseExecutor.
   */
  @Override
  public List<File> getTestDirs() {
    List<File> unfiltered = resolvePaths(def.getTestDirs());
    return ImmutableList.copyOf(Iterables.filter(unfiltered, DIRECTORY_EXISTS));
  }

  /**
   * @deprecated since 2.6, because should be immutable
   */
  @Override
  @Deprecated
  public DefaultProjectFileSystem addTestDir(File dir) {
    if (dir == null) {
      throw new IllegalArgumentException("Can not add null to project test dirs");
    }
    if (pom != null) {
      pom.getTestCompileSourceRoots().add(0, dir.getAbsolutePath());
    }
    def.addTestDirs(dir.getAbsolutePath());
    return this;
  }

  /**
   * TODO Godin: seems that used only by Cobertura and Clover
   */
  @Override
  public File getReportOutputDir() {
    if (pom != null) {
      return resolvePath(pom.getReporting().getOutputDirectory());
    } else {
      return new File(getBuildDir(), "site");
    }
  }

  @Override
  public File getSonarWorkingDirectory() {
    try {
      FileUtils.forceMkdir(def.getWorkDir());
      return def.getWorkDir();
    } catch (IOException e) {
      throw new SonarException("Unable to retrieve Sonar working directory.", e);
    }
  }

  @Override
  protected List<File> getInitialSourceFiles() {
    return resolvePaths(def.getSourceFiles());
  }

  @Override
  protected List<File> getInitialTestFiles() {
    return resolvePaths(def.getTestFiles());
  }
}

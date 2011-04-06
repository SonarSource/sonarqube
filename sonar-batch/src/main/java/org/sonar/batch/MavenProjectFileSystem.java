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
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Implementation of {@link ProjectFileSystem} based on {@link MavenProject}.
 */
public class MavenProjectFileSystem extends DefaultProjectFileSystem {

  private MavenProject pom;

  public MavenProjectFileSystem(Project project, Languages languages) {
    super(project, languages);
    this.pom = project.getPom();
  }

  @Override
  public File getBasedir() {
    return pom.getBasedir();
  }

  @Override
  public File getBuildDir() {
    return resolvePath(pom.getBuild().getDirectory());
  }

  @Override
  public File getBuildOutputDir() {
    return resolvePath(pom.getBuild().getOutputDirectory());
  }

  /**
   * Maven can modify source directories during Sonar execution - see MavenPhaseExecutor.
   */
  @Override
  public List<File> getSourceDirs() {
    return resolvePaths(pom.getCompileSourceRoots());
  }

  @Override
  public DefaultProjectFileSystem addSourceDir(File dir) {
    if (dir == null) {
      throw new IllegalArgumentException("Can not add null to project source dirs");
    }
    pom.getCompileSourceRoots().add(0, dir.getAbsolutePath());
    return this;
  }

  /**
   * Maven can modify test directories during Sonar execution - see MavenPhaseExecutor.
   */
  @Override
  public List<File> getTestDirs() {
    return resolvePaths(pom.getTestCompileSourceRoots());
  }

  /**
   * @deprecated since 2.6, because should be immutable
   */
  @Override
  public DefaultProjectFileSystem addTestDir(File dir) {
    if (dir == null) {
      throw new IllegalArgumentException("Can not add null to project test dirs");
    }
    pom.getTestCompileSourceRoots().add(0, dir.getAbsolutePath());
    return this;
  }

  @Override
  public File getReportOutputDir() {
    return resolvePath(pom.getReporting().getOutputDirectory());
  }

  @Override
  public File getSonarWorkingDirectory() {
    try {
      File dir = new File(getBuildDir(), "sonar");
      FileUtils.forceMkdir(dir);
      return dir;

    } catch (IOException e) {
      throw new SonarException("Unable to retrieve Sonar working directory.", e);
    }
  }
}

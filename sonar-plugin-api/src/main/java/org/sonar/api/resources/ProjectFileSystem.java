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
package org.sonar.api.resources;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @since 1.10
 */
public interface ProjectFileSystem {
  /**
   * Source encoding. It's the default plateform charset if it is not defined in the project
   * (Maven property 'project.build.sourceEncoding').
   */
  Charset getSourceCharset();

  /**
   * Project root directory.
   */
  File getBasedir();

  /**
   * Build directory. It's "${basedir}/target" by default in Maven projects.
   */
  File getBuildDir();

  File getBuildOutputDir();

  List<File> getSourceDirs();

  ProjectFileSystem addSourceDir(File dir);

  List<File> getTestDirs();

  ProjectFileSystem addTestDir(File dir);

  File getReportOutputDir();

  File getSonarWorkingDirectory();

  /**
   * Get file from path. It can be absolute or relative to project basedir. For example resolvePath("pom.xml") or resolvePath("src/main/java")
   */
  File resolvePath(String path);

  /**
   * Source files, excluding unit tests and files matching project exclusion patterns.
   *
   * @param langs language filter. Check all files, whatever their language, if null or empty.
   */
  List<File> getSourceFiles(Language... langs);

  /**
   * Java source files, excluding unit tests and files matching project exclusion patterns.
   * Shortcut for getSourceFiles(Java.INSTANCE)
   */
  List<File> getJavaSourceFiles();

  /**
   * Check if the project has Java files, excluding unit tests and files matching project exclusion patterns.
   */
  boolean hasJavaSourceFiles();

  /**
   * Unit test files, excluding files matching project exclusion patterns.
   */
  List<File> getTestFiles(Language... langs);

  /**
   * Check if the project has unit test files, excluding files matching project exclusion patterns.
   */
  boolean hasTestFiles(Language lang);

  /**
   * Save data into a new file of Sonar working directory.
   *
   * @return the created file
   */
  File writeToWorkingDirectory(String content, String fileName) throws IOException;

  File getFileFromBuildDirectory(String filename);

  Resource toResource(File file);
}

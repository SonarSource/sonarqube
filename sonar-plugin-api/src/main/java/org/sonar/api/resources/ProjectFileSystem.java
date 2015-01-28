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
package org.sonar.api.resources;

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.fs.FileSystem;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @since 1.10
 * @deprecated since 3.5 replaced by {@link FileSystem}
 */
@Deprecated
public interface ProjectFileSystem extends BatchComponent {
  /**
   * Source encoding.
   * Never null, it returns the default platform charset if it is not defined in project.
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

  /**
   * Directory where classes are placed. It's "${basedir}/target/classes" by default in Maven projects.
   */
  File getBuildOutputDir();

  /**
   * The list of existing directories with sources
   */
  List<File> getSourceDirs();

  /**
   * Adds a source directory
   * 
   * @return the current object
   * @deprecated since 2.6 - ProjectFileSystem should be immutable
   *             See http://jira.codehaus.org/browse/SONAR-2126
   */
  @Deprecated
  ProjectFileSystem addSourceDir(File dir);

  /**
   * The list of existing directories with tests
   */
  List<File> getTestDirs();

  /**
   * Adds a test directory
   * 
   * @return the current object
   * @deprecated since 2.6 - ProjectFileSystem should be immutable
   *             See http://jira.codehaus.org/browse/SONAR-2126
   */
  @Deprecated
  ProjectFileSystem addTestDir(File dir);

  /**
   * @return the directory where reporting is placed. Default is target/sites
   */
  File getReportOutputDir();

  /**
   * @return the Sonar working directory. Default is "target/sonar"
   */
  File getSonarWorkingDirectory();

  /**
   * @return file in canonical form from specified path. Path can be absolute or relative to project basedir.
   *         For example resolvePath("pom.xml") or resolvePath("src/main/java")
   * @deprecated since 5.0 use {@link FileSystem#resolvePath(String)}
   */
  File resolvePath(String path);

  /**
   * Source files, excluding unit tests and files matching project exclusion patterns.
   * 
   * @param langs language filter. Check all files, whatever their language, if null or empty.
   * @deprecated since 2.6 use {@link #mainFiles(String...)} instead.
   *             See http://jira.codehaus.org/browse/SONAR-2126
   */
  @Deprecated
  List<File> getSourceFiles(Language... langs);

  /**
   * Java source files, excluding unit tests and files matching project exclusion patterns. Shortcut for getSourceFiles(Java.INSTANCE)
   * 
   * @deprecated since 2.6 use {@link #mainFiles(String...)} instead.
   *             See http://jira.codehaus.org/browse/SONAR-2126
   */
  @Deprecated
  List<File> getJavaSourceFiles();

  /**
   * Check if the project has Java files, excluding unit tests and files matching project exclusion patterns.
   * 
   * @deprecated since 2.6 - API should be language agnostic
   */
  @Deprecated
  boolean hasJavaSourceFiles();

  /**
   * Unit test files, excluding files matching project exclusion patterns.
   * 
   * @deprecated since 2.6 use {@link #testFiles(String...)} instead.
   *             See http://jira.codehaus.org/browse/SONAR-2126
   */
  @Deprecated
  List<File> getTestFiles(Language... langs);

  /**
   * Check if the project has unit test files, excluding files matching project exclusion patterns.
   * 
   * @deprecated since 2.6 - use language key instead of Language object
   */
  @Deprecated
  boolean hasTestFiles(Language lang);

  /**
   * Save data into a new file of Sonar working directory.
   * 
   * @return the created file
   */
  File writeToWorkingDirectory(String content, String fileName) throws IOException;

  File getFileFromBuildDirectory(String filename);

  Resource toResource(File file);

  /**
   * Source files, excluding unit tests and files matching project exclusion patterns.
   * 
   * @param langs language filter. If null or empty, will return empty list
   * @since 2.6
   */
  List<InputFile> mainFiles(String... langs);

  /**
   * Source files of unit tests. Exclusion patterns are not applied.
   * 
   * @param langs language filter. If null or empty, will return empty list
   * @since 2.6
   */
  List<InputFile> testFiles(String... langs);

}

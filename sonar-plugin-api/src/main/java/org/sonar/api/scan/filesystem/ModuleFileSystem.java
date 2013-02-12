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
package org.sonar.api.scan.filesystem;

import org.sonar.api.BatchComponent;

import javax.annotation.CheckForNull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @since 3.5
 */
public interface ModuleFileSystem extends BatchComponent {
  /**
   * Base directory.
   */
  File baseDir();

  /**
   * Optional directory used by the build tool to generate various kinds of data (test reports, temp files, ...).
   * In Maven, it's given by the property ${project.build.directory}, which value is generally ${project.basedir}/target.
   */
  @CheckForNull
  File buildDir();

  /**
   * Source directories. Non-existing directories are excluded.
   * Example in Maven : ${project.basedir}/src/main/java
   */
  List<File> sourceDirs();

  /**
   * The files that are located in source directories and that match preconditions (inclusions/exclusions/{@link FileFilter})
   */
  List<File> sourceFiles();

  /**
   * The subset of {@link #sourceFiles()} matching the given language. For example {@code sourceFilesOfLang("java")} return all the source
   * files suffixed with .java or .jav.
   */
  List<File> sourceFilesOfLang(String language);

  /**
   * Test directories. Non-existing directories are excluded.
   * Example in Maven : ${project.basedir}/src/test/java
   */
  List<File> testDirs();

  /**
   * The files that are located in test directories and that match preconditions (inclusions/exclusions/{@link FileFilter})
   */
  List<File> testFiles();

  /**
   * The subset of {@link #testFiles()} matching the given language. For example {@code testFilesOfLang("java")} return all the test
   * files suffixed with .java or .jav.
   */
  List<File> testFilesOfLang(String language);

  /**
   * Optional directories that contain the compiled sources, for example java bytecode.
   * Note that :
   * <ul>
   * <li>Maven projects have only a single binary directory, which is generally ${project.basedir}/target/classes</li>
   * <li>Binary directories can be empty</li>
   * <li>Test binary directories are not supported yet.</li>
   * </ul>
   */
  List<File> binaryDirs();

  /**
   * Charset of source and test files. If it's not defined, then return the platform default charset.
   */
  Charset sourceCharset();

  /**
   * Working directory used by Sonar. This directory can be used for example to store intermediary reports.
   */
  File workingDir();
}

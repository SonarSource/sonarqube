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
package org.sonar.api.scan.filesystem;

import org.sonar.api.BatchSide;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

import javax.annotation.CheckForNull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @since 3.5
 * @deprecated in 4.2. Replaced by {@link org.sonar.api.batch.fs.FileSystem}
 */
@Deprecated
@BatchSide
public interface ModuleFileSystem {

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
   * Source directories.
   * @deprecated since 4.2 use {@link FileSystem#files(org.sonar.api.batch.fs.FilePredicate)} to get all files with type {@link InputFile.Type#MAIN}.
   */
  List<File> sourceDirs();

  /**
   * Test directories. Non-existing directories are excluded.
   * Example in Maven : ${project.basedir}/src/test/java
   * @deprecated since 4.2 use {@link FileSystem#files(org.sonar.api.batch.fs.FilePredicate)} to get all files with type {@link InputFile.Type#TEST}.
   */
  List<File> testDirs();

  /**
   * Optional directories that contain the compiled sources, for example java bytecode.
   * Note that :
   * <ul>
   * <li>Maven projects have only a single binary directory, which is generally ${project.basedir}/target/classes</li>
   * <li>Binary directories can be empty</li>
   * <li>Test binary directories are not supported yet.</li>
   * </ul>
   * @deprecated since 4.2 sonar.binaries will be converted to java specific property
   */
  List<File> binaryDirs();

  /**
   * Search for files. Never return null.
   */
  List<File> files(FileQuery query);

  /**
   * Default charset for files of the module. If it's not defined, then
   * return the platform default charset. When trying to read an input file it is better to rely on
   * {@link InputFile#encoding()} as encoding may be different for each file.
   */
  Charset sourceCharset();

  /**
   * Working directory used by Sonar. This directory can be used for example to
   * store intermediary reports.
   */
  File workingDir();
}

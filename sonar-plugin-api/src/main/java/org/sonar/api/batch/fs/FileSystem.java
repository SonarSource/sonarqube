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
package org.sonar.api.batch.fs;

import org.sonar.api.BatchComponent;

import javax.annotation.CheckForNull;
import java.io.File;
import java.nio.charset.Charset;
import java.util.SortedSet;

/**
 * <p>The unit tests needing an instance of FileSystem can use the implementation
 * {@link org.sonar.api.batch.fs.internal.DefaultFileSystem} and the related {@link org.sonar.api.scan.filesystem.internal.DefaultInputFile}:</p>
 * <pre>
 *   DefaultFileSystem fs = new DefaultFileSystem();
 *   fs.add(new DefaultInputFile("src/foo/bar.php"));
 * </pre>
 *
 * @since 4.2
 */
public interface FileSystem extends BatchComponent {

  /**
   * Absolute base directory of module
   */
  File baseDir();

  /**
   * Default encoding of input files. If it's not defined, then
   * the platform default encoding is returned
   */
  Charset encoding();

  /**
   * Absolute work directory. It can be used to
   * store third-party analysis reports.
   * <p/>
   * The work directory can be located outside {@link #baseDir()}.
   */
  File workDir();

  /**
   * Returns the single element matching the predicate. If more than one elements match
   * the predicate, then {@link IllegalArgumentException} is thrown. Returns {@code null}
   * if no files match.
   * @see org.sonar.api.batch.fs.FilePredicates
   */
  @CheckForNull
  InputFile inputFile(FilePredicate predicate);

  /**
   * Input files matching the given attributes. Return all the files if the parameter
   * <code>attributes</code> is empty.
   * <p/>
   * Important - result is an {@link java.lang.Iterable} to benefit from streaming and decreasing
   * memory consumption. It should be iterated only once, else copy it into a list :
   * {@code com.google.common.collect.Lists.newArrayList(inputFiles(predicate))}
   * @see org.sonar.api.batch.fs.FilePredicates
   */
  Iterable<InputFile> inputFiles(FilePredicate predicate);

  /**
   * Returns true if at least one {@link org.sonar.api.batch.fs.InputFile} matches
   * the given predicate. This method can be faster than checking if {@link #inputFiles(org.sonar.api.batch.fs.FilePredicate...)}
   * has elements.
   * @see org.sonar.api.batch.fs.FilePredicates
   */
  boolean hasFiles(FilePredicate predicate);

  /**
   * Files matching the given predicate.
   * @see org.sonar.api.batch.fs.FilePredicates
   */
  Iterable<File> files(FilePredicate predicate);

  /**
   * Languages detected in all files, whatever their type (main or test)
   */
  SortedSet<String> languages();
}

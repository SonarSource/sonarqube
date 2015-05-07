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

import org.sonar.api.BatchSide;

import javax.annotation.CheckForNull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.SortedSet;

/**
 * The {@link FileSystem} manages all the source files to be analyzed.
 * <p/>
 * This is not an extension point so it must not be implemented by plugins. It must be injected as a
 * constructor parameter :
 * <pre>
 * public class MySensor implements Sensor {
 *   private final FileSystem fs;
 *
 *   public MySensor(FileSystem fs) {
 *     this.fs = fs;
 *   }
 * }
 * </pre>
 *
 * <h2>How to use in unit tests</h2>
 * The unit tests needing an instance of FileSystem can use the implementation
 * {@link org.sonar.api.batch.fs.internal.DefaultFileSystem} and the related {@link org.sonar.api.batch.fs.internal.DefaultInputFile},
 * for example :
 * <pre>
 * DefaultFileSystem fs = new DefaultFileSystem();
 * fs.add(new DefaultInputFile("myprojectKey", "src/foo/bar.php"));
 * </pre>
 *
 * @since 4.2
 */
@BatchSide
public interface FileSystem {

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
   * Factory of {@link FilePredicate}
   */
  FilePredicates predicates();

  /**
   * Returns the single element matching the predicate. If more than one elements match
   * the predicate, then {@link IllegalArgumentException} is thrown. Returns {@code null}
   * if no files match.
   *
   * <p/>
   * How to use :
   * <pre>
   * InputFile file = fs.inputFile(fs.predicates().hasRelativePath("src/Foo.php"));
   * </pre>
   *
   * @see #predicates()
   */
  @CheckForNull
  InputFile inputFile(FilePredicate predicate);

  /**
   * Returns {@link InputDir} matching the current {@link File}.
   * @return null if directory is not indexed.
   * @throw {@link IllegalArgumentException} is File is null or not a directory.
   * 
   * @since 4.5
   */
  @CheckForNull
  InputDir inputDir(File dir);

  /**
   * Input files matching the given attributes. Return all the files if the parameter
   * <code>attributes</code> is empty.
   * <p/>
   * <b>Important</b> - result is an {@link java.lang.Iterable} to benefit from streaming and decreasing
   * memory consumption. It should be iterated only once, else copy it into a list :
   * {@code com.google.common.collect.Lists.newArrayList(inputFiles(predicate))}
   * <p/>
   * How to use :
   * <pre>
   * FilePredicates p = fs.predicates();
   * Iterable<InputFile> files = fs.inputFiles(p.and(p.hasLanguage("java"), p.hasType(InputFile.Type.MAIN)));
   * </pre>
   *
   * @see #predicates()
   */
  Iterable<InputFile> inputFiles(FilePredicate predicate);

  /**
   * Returns true if at least one {@link org.sonar.api.batch.fs.InputFile} matches
   * the given predicate. This method can be faster than checking if {@link #inputFiles(org.sonar.api.batch.fs.FilePredicate)}
   * has elements.
   * @see #predicates()
   */
  boolean hasFiles(FilePredicate predicate);

  /**
   * Files matching the given predicate.
   * @see #predicates()
   */
  Iterable<File> files(FilePredicate predicate);

  /**
   * Languages detected in all files, whatever their type (main or test)
   */
  SortedSet<String> languages();

  /**
   * Utility method mainly used to resolve location of reports.
   * @return file in canonical form from specified path. Path can be absolute or relative to project basedir.
   *         For example resolvePath("pom.xml") or resolvePath("src/main/java")
   * @since 5.0
   */
  File resolvePath(String path);

  /**
   * Interface of the underlying file index.
   */
  interface Index {
    Iterable<InputFile> inputFiles();

    @CheckForNull
    InputFile inputFile(String relativePath);

    @CheckForNull
    InputDir inputDir(String relativePath);
  }
}

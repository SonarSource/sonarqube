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

import java.io.File;
import java.nio.file.Path;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

/**
 * This layer over {@link java.io.File} adds information for code analyzers.
 * For unit testing purpose you can create some {@link DefaultInputFile} and initialize
 * all fields using 
 * 
 * <pre>
 *   new DefaultInputFile("moduleKey", "relative/path/from/module/baseDir.java")
 *     .setModuleBaseDir(path)
 *     .initMetadata(new FileMetadata().readMetadata(someReader));
 * </pre>
 *
 * @since 4.2
 */
public interface InputFile extends InputPath {

  enum Type {
    MAIN, TEST
  }

  /** 
   * Status regarding previous analysis
   */
  enum Status {
    SAME, CHANGED, ADDED
  }

  /**
   * Path relative to module base directory. Path is unique and identifies file
   * within given <code>{@link FileSystem}</code>. File separator is the forward
   * slash ('/'), even on Microsoft Windows.
   * <p/>
   * Returns <code>src/main/java/com/Foo.java</code> if module base dir is
   * <code>/path/to/module</code> and if file is
   * <code>/path/to/module/src/main/java/com/Foo.java</code>.
   * <p/>
   * Relative path is not null and is normalized ('foo/../foo' is replaced by 'foo').
   */
  @Override
  String relativePath();

  /**
   * Normalized absolute path. File separator is forward slash ('/'), even on Microsoft Windows.
   * <p/>
   * This is not canonical path. Symbolic links are not resolved. For example if /project/src links
   * to /tmp/src and basedir is /project, then this method returns /project/src/index.php. Use
   * {@code file().getCanonicalPath()} to resolve symbolic link.
   */
  @Override
  String absolutePath();

  /**
   * The underlying absolute {@link java.io.File}
   */
  @Override
  File file();

  /**
   * The underlying absolute {@link Path}
   */
  @Override
  Path path();

  /**
   * Language, for example "java" or "php". Can be null if indexation of all files is enabled and no language claims to support the file.
   */
  @CheckForNull
  String language();

  /**
   * Does it contain main or test code ?
   */
  Type type();

  /**
   * Status regarding previous analysis
   */
  Status status();

  /**
   * Number of physical lines. This method supports all end-of-line characters. Formula is (number of line break + 1). Returns
   * 1 if the file is empty.</br> Returns 2 for <tt>foo\nbar</tt>. Returns 3 for <tt>foo\nbar\n</tt>.
   */
  int lines();

  /**
   * Check if the file content is empty (ignore potential BOM).
   * @since 5.2
   */
  boolean isEmpty();

  /**
   * Returns a {@link TextPointer} in the given file.
   * @param line Line of the pointer. Start at 1.
   * @param lineOffset Offset in the line. Start at 0.
   * @throw {@link IllegalArgumentException} if line or offset is not valid for the given file.
   * @since 5.2
   */
  TextPointer newPointer(int line, int lineOffset);

  /**
   * Returns a {@link TextRange} in the given file.
   * @param start start pointer
   * @param end end pointer
   * @throw {@link IllegalArgumentException} if start or stop pointers are not valid for the given file.
   * @since 5.2
   */
  TextRange newRange(TextPointer start, TextPointer end);

  /**
   * Returns a {@link TextRange} in the given file.
   * <ul>
   * <li><code>newRange(1, 0, 1, 1)</code> selects the first character at line 1</li>
   * <li><code>newRange(1, 0, 1, 10)</code> selects the 10 first characters at line 1</li>
   * </ul>
   * @throw {@link IllegalArgumentException} if start or stop positions are not valid for the given file.
   * @since 5.2
   */
  TextRange newRange(int startLine, int startLineOffset, int endLine, int endLineOffset);

  /**
   * Returns a {@link TextRange} in the given file that select the full line.
   * @param line Start at 1.
   * @throw {@link IllegalArgumentException} if line is not valid for the given file.
   * @since 5.2
   */
  TextRange selectLine(int line);
}

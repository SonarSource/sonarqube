/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.sensor.SensorDescriptor;

/**
 * This layer over {@link java.io.File} adds information for code analyzers.
 * For unit testing purpose, use TestInputFileBuilder and initialize
 * the needed fields:
 * 
 * <pre>
 *   new TestInputFileBuilder("moduleKey", "relative/path/from/module/baseDir.java")
 *     .setModuleBaseDir(path)
 *     .build();
 * </pre>
 *
 * @since 4.2
 */
public interface InputFile extends IndexedFile {

  enum Type {
    MAIN, TEST
  }

  /** 
   * Status regarding previous analysis
   * @deprecated since 7.7 preview mode was dropped
   */
  @Deprecated
  enum Status {
    SAME, CHANGED, ADDED
  }

  /**
   * Relative path to module (for normal Sensors) or project (for {@link SensorDescriptor#global() global} Sensors) base directory.
   * File separator is the forward
   * slash ('/'), even on Microsoft Windows.
   * <br>
   * Returns <code>src/main/java/com/Foo.java</code> if module base dir is
   * <code>/path/to/module</code> and if file is
   * <code>/path/to/module/src/main/java/com/Foo.java</code>.
   * <br>
   * Relative path is not null and is normalized ('foo/../foo' is replaced by 'foo').
   * @deprecated since 6.6 use {@link #inputStream()} for file content, {@link #filename()} for file name, {@link #uri()} for an unique identifier, and {@link #toString()} for logging
   */
  @Deprecated
  @Override
  String relativePath();

  /**
   * Normalized absolute path. File separator is forward slash ('/'), even on Microsoft Windows.
   * <br>
   * This is not canonical path. Symbolic links are not resolved. For example if /project/src links
   * to /tmp/src and basedir is /project, then this method returns /project/src/index.php. Use
   * {@code file().getCanonicalPath()} to resolve symbolic link.
   * @deprecated since 6.6 use {@link #inputStream()} for file content, {@link #filename()} for file name, {@link #uri()} for an unique identifier, and {@link #toString()} for logging
   */
  @Deprecated
  @Override
  String absolutePath();

  /**
   * The underlying absolute {@link java.io.File}. It should not be used to read the file in the filesystem.
   * @see #contents()
   * @see #inputStream()
   * @deprecated since 6.6 use {@link #inputStream()} for file content, {@link #filename()} for file name, {@link #uri()} for an unique identifier, and {@link #toString()} for logging
   */
  @Deprecated
  @Override
  File file();

  /**
   * The underlying absolute {@link Path}.
   * It should not be used to read the file in the filesystem.
   * @see #contents()
   * @see #inputStream()
   * @since 5.1
   * @deprecated since 6.6 use {@link #inputStream()} for file content, {@link #filename()} for file name, {@link #uri()} for an unique identifier, and {@link #toString()} for logging
   */
  @Deprecated
  @Override
  Path path();

  /**
   * Language, for example "java" or "php". Can be null if indexation of all files is enabled and no language claims to support the file.
   */
  @CheckForNull
  @Override
  String language();

  /**
   * Does it contain main or test code ?
   */
  @Override
  Type type();

  /**
   * Creates a stream of the file's contents. Depending on the runtime context, the source might be a file in a physical or virtual filesystem.
   * Typically, it won't be buffered. <b>The stream must be closed by the caller</b>.
   * Since 6.4 BOM is automatically filtered out.
   * @since 6.2
   */
  @Override
  InputStream inputStream() throws IOException;

  /**
   * Fetches the entire contents of the file, decoding with the {@link #charset}.
   * Since 6.4 BOM is automatically filtered out.
   * @since 6.2
   */
  String contents() throws IOException;

  /**
   * @deprecated since 7.7 preview/issue mode was removed
   */
  @Deprecated
  Status status();

  /**
   * Number of physical lines. This method supports all end-of-line characters. Formula is (number of line break + 1). 
   * <p>
   * Returns 1 if the file is empty.
   * <br> 
   * Returns 2 for <tt>foo\nbar</tt>. 
   * <br>
   * Returns 3 for <tt>foo\nbar\n</tt>.
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
   * @throws IllegalArgumentException if line or offset is not valid for the given file.
   * @since 5.2
   */
  TextPointer newPointer(int line, int lineOffset);

  /**
   * Returns a {@link TextRange} in the given file.
   * @param start start pointer
   * @param end end pointer
   * @throws IllegalArgumentException if start or stop pointers are not valid for the given file.
   * @since 5.2
   */
  TextRange newRange(TextPointer start, TextPointer end);

  /**
   * Returns a {@link TextRange} in the given file.
   * <ul>
   * <li><code>newRange(1, 0, 1, 1)</code> selects the first character at line 1</li>
   * <li><code>newRange(1, 0, 1, 10)</code> selects the 10 first characters at line 1</li>
   * </ul>
   * @throws IllegalArgumentException if start or stop positions are not valid for the given file.
   * @since 5.2
   */
  TextRange newRange(int startLine, int startLineOffset, int endLine, int endLineOffset);

  /**
   * Returns a {@link TextRange} in the given file that select the full line.
   * @param line Start at 1.
   * @throws IllegalArgumentException if line is not valid for the given file.
   * @since 5.2
   */
  TextRange selectLine(int line);

  /**
   * Charset to be used to decode this specific file.
   * @since 6.0
   */
  Charset charset();

  /**
   * Return a string to identify this file (suitable for logs).
   */
  @Override
  String toString();
}

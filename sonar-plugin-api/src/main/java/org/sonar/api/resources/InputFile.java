/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @since 2.6
 */
public interface InputFile {
  /**
   * The source base directory, different than the project basedir.
   * 
   * <p>For example in maven projects, the basedir of a source file stored in
   * <code>src/main/java/org/foo/</code> is the directory <code>src/main/java</code>.</p>
   */
  File getFileBaseDir();

  /**
   * Get the underlying file.
   *
   * @return the file
   */
  File getFile();

  /**
   * Path relative to basedir. Directory separator is slash <code>'/'</code>, whatever the platform.
   *
   * <p>Example on windows: if file basedir is <code>c:\project\src\</code> and file is <code>c:\project\src\org\foo\Bar.java</code>, then relative path
   * is <code>org/foo/Bar.java</code></p>
   *
   * <p>Example on unix: if file basedir is <code>/project/src</code> and file is <code>/project/src/org/foo/Bar.java</code>, then relative path
   * is <code>org/foo/Bar.java</code> as well.</p>
   */
  String getRelativePath();

  /**
   * Get an {@link InputStream} that reads from the file.
   *
   * <p>The returned stream is buffered so there is no need to use a
   * <code>BufferedInputStream</code></p>
   *
   * @return the stream
   * @throws FileNotFoundException if the file is not found
   * @since 3.1
   */
  InputStream getInputStream() throws FileNotFoundException;
}

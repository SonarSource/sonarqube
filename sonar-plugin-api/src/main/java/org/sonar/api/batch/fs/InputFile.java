/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import java.io.Serializable;

/**
 * This layer over {@link java.io.File} adds information for code analyzers.
 *
 * @since 4.2
 */
public interface InputFile extends Serializable {

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
  String relativePath();

  /**
   * Normalized absolute path. File separator is forward slash ('/'), even on Microsoft Windows.
   * <p/>
   * This is not canonical path. Symbolic links are not resolved. For example if /project/src links
   * to /tmp/src and basedir is /project, then this method returns /project/src/index.php. Use
   * {@code file().getCanonicalPath()} to resolve symbolic link.
   */
  String absolutePath();

  /**
   * The underlying absolute {@link java.io.File}
   */
  File file();

  /**
   * Language, for example "java" or "php". It's automatically guessed if it is not
   * set in project settings.
   */
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
   * Number of physical lines. This method supports all end-of-line characters. Returns
   * zero if the file is empty.
   */
  int lines();
}

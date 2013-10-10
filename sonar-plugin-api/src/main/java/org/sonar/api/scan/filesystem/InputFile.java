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
package org.sonar.api.scan.filesystem;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.Serializable;
import java.util.Map;

public interface InputFile extends Serializable {

  /**
   * Canonical path of source directory.
   * Example: <code>/path/to/module/src/main/java</code> or <code>C:\path\to\module\src\main\java</code>
   */
  String ATTRIBUTE_SOURCEDIR_PATH = "srcDirPath";

  /**
   * Relative path from source directory. File separator is the forward slash ('/'),
   * even on MSWindows.
   */
  String ATTRIBUTE_SOURCE_RELATIVE_PATH = "srcRelPath";

  /**
   * Detected language
   */
  String ATTRIBUTE_LANGUAGE = "lang";

  /**
   *
   */
  String ATTRIBUTE_TYPE = "type";
  String TYPE_SOURCE = "source";
  String TYPE_TEST = "test";

  String ATTRIBUTE_STATUS = "status";
  String STATUS_SAME = "same";
  String STATUS_CHANGED = "changed";
  String STATUS_ADDED = "added";

  String ATTRIBUTE_HASH = "hash";
  String ATTRIBUTE_EXTENSION = "extension";


  /**
   * Path from module base directory. Path is unique and identifies file within given
   * <code>{@link org.sonar.api.scan.filesystem.ModuleFileSystem}</code>. File separator is the forward slash ('/'),
   * even on MSWindows.
   * <p/>
   * If:
   * <ul>
   * <li>Module base dir is <code>/absolute/path/to/module</code></li>
   * <li>File is <code>/absolute/path/to/module/src/main/java/com/Foo.java</code></li>
   * </ul>
   * then the path is <code>src/main/java/com/Foo.java</code>
   * <p/>
   * On MSWindows, if:
   * <ul>
   * <li>Module base dir is <code>C:\absolute\path\to\module</code></li>
   * <li>File is <code>C:\absolute\path\to\module\src\main\java\com\Foo.java</code></li>
   * </ul>
   * then the path is <code>src/main/java/com/Foo.java</code>.
   * <p/>
   * Returned relative path is never null.
   */
  String relativePath();

  /**
   * Canonical path.
   */
  String path();

  /**
   * Not-null related {@link java.io.File}
   */
  File file();

  /**
   * Not-null filename, including extension
   */

  String name();

  /**
   * Does the given attribute have the given value ?
   */
  boolean has(String attribute, String value);

  /**
   * See list of attribute keys in constants starting with ATTRIBUTE_.
   */
  @CheckForNull
  String attribute(String key);

  Map<String, String> attributes();
}

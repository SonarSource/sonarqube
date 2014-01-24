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
package org.sonar.api.scan.filesystem.internal;

import javax.annotation.CheckForNull;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Map;

public interface InputFile extends Serializable {

  /**
   * Canonical path of source directory.
   * Example: <code>/path/to/module/src/main/java</code> or <code>C:\path\to\module\src\main\java</code>
   * @deprecated since 4.2 No more sonar.sources
   */
  @Deprecated
  String ATTRIBUTE_SOURCEDIR_PATH = "SRC_DIR_PATH";

  /**
   * Relative path from source directory. File separator is the forward slash ('/'),
   * even on MSWindows.
   * @deprecated since 4.2 No more sonar.sources
   */
  @Deprecated
  String ATTRIBUTE_SOURCE_RELATIVE_PATH = "SRC_REL_PATH";

  /**
   * Detected language
   */
  String ATTRIBUTE_LANGUAGE = "LANG";

  /**
   *
   */
  String ATTRIBUTE_TYPE = "TYPE";
  String TYPE_SOURCE = "SOURCE";
  String TYPE_TEST = "TEST";

  String ATTRIBUTE_STATUS = "STATUS";
  String STATUS_SAME = "SAME";
  String STATUS_CHANGED = "CHANGED";
  String STATUS_ADDED = "ADDED";

  String ATTRIBUTE_HASH = "HASH";

  /**
   * Path is relative from module base directory. Path is unique and identifies file
   * within given <code>{@link org.sonar.api.scan.filesystem.ModuleFileSystem}</code>.
   * File separator is the forward slash ('/'), even on MSWindows.
   * <p/>
   * Returns <code>src/main/java/com/Foo.java</code> if module base dir is
   * <code>/absolute/path/to/module</code> and if file is
   * <code>/absolute/path/to/module/src/main/java/com/Foo.java</code>.
   * <p/>
   * Returned path is never null.
   */
  String path();

  /**
   * Not-null canonical path. File separator is forward slash ('/'), even on MSWindows.
   */
  String absolutePath();

  File file();

  Charset encoding();

  /**
   * Not-null filename, including extension
   */
  String name();

  /**
   * Not-null type (is it a source file or a unit test file?).
   * See constant values prefixed by <code>TYPE_</code>, for example {@link #TYPE_SOURCE}.
   */
  String type();

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

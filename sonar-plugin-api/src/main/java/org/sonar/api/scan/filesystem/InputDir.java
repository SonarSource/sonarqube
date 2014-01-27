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

/**
 * @since 4.2
 */
public interface InputDir extends Serializable {

  /**
   * Path is relative from module base directory. Path is unique and identifies file
   * within given <code>{@link org.sonar.api.scan.filesystem.ModuleFileSystem}</code>.
   * File separator is the forward slash ('/'), even on MSWindows.
   * <p/>
   * Returns <code>src/main/java/com</code> if module base dir is
   * <code>/absolute/path/to/module</code> and if directory is
   * <code>/absolute/path/to/module/src/main/java/com</code>.
   * <p/>
   * Returned path is never null.
   */
  String path();

  /**
   * Not-null canonical path. File separator is forward slash ('/'), even on MSWindows.
   */
  String absolutePath();

  File file();

  /**
   * Not-null directory name
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

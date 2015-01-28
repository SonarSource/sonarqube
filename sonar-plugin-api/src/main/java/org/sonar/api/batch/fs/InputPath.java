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
import java.io.Serializable;
import java.nio.file.Path;

/**
 * Layer over {@link java.io.File} for files or directories.
 *
 * @since 4.5
 * @see InputFile
 * @see InputDir
 */
public interface InputPath extends Serializable {

  /**
   * @see InputFile#relativePath()
   * @see InputDir#relativePath()
   */
  String relativePath();

  /**
   * @see InputFile#absolutePath()
   * @see InputDir#absolutePath()
   */
  String absolutePath();

  /**
   * @see InputFile#file()
   * @see InputDir#file()
   */
  File file();

  /**
   * @see InputFile#path()
   * @see InputDir#path()
   * @since 5.1
   */
  Path path();

}

/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.net.URI;
import java.nio.file.Path;

/**
 * Layer over {@link java.io.File} for files or directories.
 *
 * @since 4.5
 * @see InputFile
 * @see InputDir
 */
public interface InputPath extends InputComponent {

  /**
   * @see InputFile#relativePath()
   * @see InputDir#relativePath()
   * @deprecated since 6.5 use {@link #uri()}
   */
  @Deprecated
  String relativePath();

  /**
   * @see InputFile#absolutePath()
   * @see InputDir#absolutePath()
   * @deprecated since 6.5 use {@link #uri()}
   */
  @Deprecated
  String absolutePath();

  /**
   * @see InputFile#file()
   * @see InputDir#file()
   * @deprecated since 6.5 use {@link #uri()}
   */
  @Deprecated
  File file();

  /**
   * @see InputFile#path()
   * @see InputDir#path()
   * @since 5.1
   * @deprecated since 6.5 use {@link #uri()}
   */
  @Deprecated
  Path path();

  /**
   * Identifier of the component. The only guarantee is that it is unique in the project.
   * You should not assume it is a file:// URI.
   * @since 6.5
   */
  URI uri();

}

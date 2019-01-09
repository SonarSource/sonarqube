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
package org.sonar.api.batch.scm;

import java.nio.file.Path;
import org.sonar.api.scanner.ScannerSide;

/**
 * @since 7.6
 */
@ScannerSide
public interface IgnoreCommand {

  /**
   * Check if a file is ignored by the scm.
   * {@link #init(Path)} must be called before the first call to this method and {@link #clean()} after the last one
   *
   * @param file Absolute path of a project file
   * @return true if the given file is ignored by the scm, false otherwise
   */
  boolean isIgnored(Path file);

  /**
   * Must be called before the calling {@link #isIgnored(Path)}
   *
   * It should contains any operation (e.g. building cache) required before handling {@link #isIgnored(Path)} calls.
   *
   * @param baseDir the root directory of the project
   */
  void init(Path baseDir);

  /**
   * To be called after the last call to {@link #isIgnored(Path)}.
   *
   * Cache or any other resources used should be cleared.
   */
  void clean();
}

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
import java.nio.file.Path;

/**
 * Layer over {@link java.io.File} for directories. You can access InputDir using {@link FileSystem#inputDir(File)}.
 *
 * @since 4.5
 * @deprecated since 6.6 Ability to report issues or measures on directories will soon be dropped. Report issues on project if needed. 
 */
@Deprecated
public interface InputDir extends InputPath {

  /**
   * Path relative to module base directory. Path is unique and identifies directory
   * within given <code>{@link FileSystem}</code>. File separator is the forward
   * slash ('/'), even on Microsoft Windows.
   * <br>
   * Returns <code>src/main/java/com</code> if module base dir is
   * <code>/path/to/module</code> and if directory is
   * <code>/path/to/module/src/main/java/com</code>.
   * <br>
   * Relative path is not null and is normalized ('foo/../foo' is replaced by 'foo').
   */
  @Override
  String relativePath();

  /**
   * Normalized absolute path. File separator is forward slash ('/'), even on Microsoft Windows.
   * <br>
   * This is not canonical path. Symbolic links are not resolved. For example if /project/src links
   * to /tmp/src and basedir is /project, then this method returns /project/src. Use
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

}

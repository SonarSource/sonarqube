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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import javax.annotation.CheckForNull;

/**
 * Represents the indexed view of an {@link InputFile}. Accessing any of data exposed here won't trigger the expensive generation of 
 * metadata for the {@link InputFile}.
 * 
 * @since 6.3 
 */
public interface IndexedFile extends InputPath {
  /**
   * Path relative to module base directory. Path is unique and identifies file
   * within given <code>{@link FileSystem}</code>. File separator is the forward
   * slash ('/'), even on Microsoft Windows.
   * <br>
   * Returns <code>src/main/java/com/Foo.java</code> if module base dir is
   * <code>/path/to/module</code> and if file is
   * <code>/path/to/module/src/main/java/com/Foo.java</code>.
   * <br>
   * Relative path is not null and is normalized ('foo/../foo' is replaced by 'foo').
   * @deprecated since 6.6 use {@link #inputStream()}, {@link #filename()} or {@link #uri()}
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
   * @deprecated since 6.6 use {@link #inputStream()}, {@link #filename()} or {@link #uri()}
   */
  @Deprecated
  @Override
  String absolutePath();

  /**
   * The underlying absolute {@link java.io.File}. It should not be used to read the file in the filesystem.
   * @see #contents()
   * @see #inputStream()
   * @deprecated since 6.6 use {@link #inputStream()}, {@link #filename()} or {@link #uri()}
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
   * @deprecated since 6.6 use {@link #inputStream()}, {@link #filename()} or {@link #uri()}
   */
  @Deprecated
  @Override
  Path path();

  /**
   * Identifier of the file. The only guarantee is that it is unique in the project.
   * You should not assume it is a file:// URI.
   * @since 6.6
   */
  URI uri();

  /**
   * Filename for this file (inclusing extension). For example: MyClass.java.
   * @since 6.6
   */
  String filename();

  /**
   * Language, for example "java" or "php". Can be null if indexation of all files is enabled and no language claims to support the file.
   */
  @CheckForNull
  String language();

  /**
   * Does it contain main or test code ?
   */
  InputFile.Type type();

  /**
   * Creates a stream of the file's contents. Depending on the runtime context, the source might be a file in a physical or virtual filesystem.
   * Typically, it won't be buffered. <b>The stream must be closed by the caller</b>.
   * @since 6.2
   */
  InputStream inputStream() throws IOException;
}

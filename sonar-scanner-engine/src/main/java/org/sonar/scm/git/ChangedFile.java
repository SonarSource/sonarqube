/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.scm.git;

import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ChangedFile {
  @Nullable
  private final String oldFilePath;
  private final String filePath;
  private final Path absoluteFilePath;

  public ChangedFile(String filePath, Path absoluteFilePath) {
    this(filePath, absoluteFilePath, null);
  }

  public ChangedFile(String filePath, Path absoluteFilePath, @Nullable String oldFilePath) {
    this.filePath = filePath;
    this.oldFilePath = oldFilePath;
    this.absoluteFilePath =  absoluteFilePath;
  }

  @CheckForNull
  public String getOldFilePath() {
    return oldFilePath;
  }

  public boolean isMoved() {
    return Objects.nonNull(this.getOldFilePath());
  }

  public String getFilePath() {
    return filePath;
  }

  public Path getAbsolutFilePath() {
    return absoluteFilePath;
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.sonar.db.component.ComponentValidator.MAX_COMPONENT_NAME_LENGTH;

/**
 * The attributes specific to a Component of type {@link Component.Type#FILE}.
 */
@Immutable
public class FileAttributes {
  private final boolean unitTest;
  @CheckForNull
  private final String languageKey;
  private final boolean markedAsUnchanged;
  private final int lines;
  private String oldRelativePath;

  public FileAttributes(boolean unitTest, @Nullable String languageKey, int lines) {
    this(unitTest, languageKey, lines, false, null);
  }

  public FileAttributes(boolean unitTest, @Nullable String languageKey, int lines, boolean markedAsUnchanged, @Nullable String oldRelativePath) {
    this.unitTest = unitTest;
    this.languageKey = languageKey;
    this.markedAsUnchanged = markedAsUnchanged;
    checkArgument(lines > 0, "Number of lines must be greater than zero");
    this.lines = lines;
    this.oldRelativePath = formatOldRelativePath(oldRelativePath);
  }

  public boolean isMarkedAsUnchanged() {
    return markedAsUnchanged;
  }

  public boolean isUnitTest() {
    return unitTest;
  }

  @CheckForNull
  public String getLanguageKey() {
    return languageKey;
  }

  /**
   * The old relative path of a file when a move is detected by the SCM in the scope of a Pull Request.
   */
  @CheckForNull
  public String getOldRelativePath() {
    return oldRelativePath;
  }

  /**
   * Number of lines of the file, can never be less than 1
   */
  public int getLines() {
    return lines;
  }

  @Override
  public String toString() {
    return "FileAttributes{" +
      "languageKey='" + languageKey + '\'' +
      ", unitTest=" + unitTest +
      ", lines=" + lines +
      ", markedAsUnchanged=" + markedAsUnchanged +
      ", oldRelativePath='" + oldRelativePath + '\'' +
      '}';
  }

  private static String formatOldRelativePath(@Nullable String path) {
    return abbreviate(trimToNull(path), MAX_COMPONENT_NAME_LENGTH);
  }
}

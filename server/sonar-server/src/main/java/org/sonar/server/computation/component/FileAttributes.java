/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * The attributes specific to a Component of type {@link org.sonar.server.computation.component.Component.Type#FILE}.
 */
@Immutable
public class FileAttributes {
  private final boolean unitTest;
  @CheckForNull
  private final String languageKey;

  public FileAttributes(boolean unitTest, @Nullable String languageKey) {
    this.unitTest = unitTest;
    this.languageKey = languageKey;
  }

  public boolean isUnitTest() {
    return unitTest;
  }

  @CheckForNull
  public String getLanguageKey() {
    return languageKey;
  }

  @Override
  public String toString() {
    return "FileAttributes{" +
      "languageKey='" + languageKey + '\'' +
      ", unitTest=" + unitTest +
      '}';
  }
}

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
package org.sonar.xoo;

import org.sonar.api.config.Configuration;
import org.sonar.api.resources.Language;

public class Xoo implements Language {

  public static final String KEY = "xoo";
  public static final String NAME = "Xoo";
  public static final String FILE_SUFFIXES_KEY = "sonar.xoo.file.suffixes";
  public static final String DEFAULT_FILE_SUFFIXES = ".xoo";

  private final Configuration config;

  public Xoo(Configuration config) {
    this.config = config;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String[] getFileSuffixes() {
    return config.getStringArray(FILE_SUFFIXES_KEY);
  }

  @Override
  public boolean publishAllFiles() {
    return true;
  }
}

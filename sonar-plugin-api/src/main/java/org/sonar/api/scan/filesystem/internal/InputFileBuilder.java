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
package org.sonar.api.scan.filesystem.internal;

import org.sonar.api.utils.PathUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * FOR UNIT-TESTING ONLY
 *
 * @since 4.0
 */
public class InputFileBuilder {

  private final Map<String, String> attributes = new HashMap<String, String>();
  private final File file;
  private final String relativePath;

  public static void _FOR_UNIT_TESTING_ONLY_() {
    // For those who don't read javadoc
  }

  public InputFileBuilder(File file, String relativePath) {
    this.file = file;
    this.relativePath = relativePath;
  }

  public InputFileBuilder attribute(String key, @Nullable String value) {
    if (value != null) {
      attributes.put(key, value);
    }
    return this;
  }

  public InputFileBuilder type(@Nullable String type) {
    return attribute(InputFile.ATTRIBUTE_TYPE, type);
  }

  public InputFileBuilder language(@Nullable String language) {
    return attribute(InputFile.ATTRIBUTE_LANGUAGE, language);
  }

  public InputFileBuilder hash(@Nullable String hash) {
    return attribute(InputFile.ATTRIBUTE_HASH, hash);
  }

  public InputFileBuilder status(@Nullable String status) {
    return attribute(InputFile.ATTRIBUTE_STATUS, status);
  }

  public InputFileBuilder sourceDir(File dir) {
    return attribute(InputFile.ATTRIBUTE_SOURCEDIR_PATH, PathUtils.canonicalPath(dir));
  }

  public InputFileBuilder sourceDir(@Nullable String path) {
    return attribute(InputFile.ATTRIBUTE_SOURCEDIR_PATH, PathUtils.sanitize(path));
  }

  public DefaultInputFile build() {
    return DefaultInputFile.create(file, relativePath, attributes);
  }
}

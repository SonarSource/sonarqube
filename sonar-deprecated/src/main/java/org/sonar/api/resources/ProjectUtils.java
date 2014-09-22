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
package org.sonar.api.resources;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

/**
 * @since 1.10
 * @deprecated see method comments
 */
@Deprecated
public final class ProjectUtils {

  private ProjectUtils() {
    // utility class with only static methods
  }

  /**
   * @since 2.7
   * @deprecated in 4.2. Replaced by org.sonar.api.resources.InputFileUtils#toFiles()
   */
  @Deprecated
  public static List<java.io.File> toIoFiles(Collection<InputFile> inputFiles) {
    List<java.io.File> files = Lists.newArrayList();
    for (InputFile inputFile : inputFiles) {
      files.add(inputFile.getFile());
    }
    return files;
  }
}

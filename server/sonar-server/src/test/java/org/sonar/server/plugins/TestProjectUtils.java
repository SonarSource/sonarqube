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
package org.sonar.server.plugins;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Collection;

public class TestProjectUtils {

  /**
   * Get the artifact of plugins stored in src/test/projects
   */
  public static File jarOf(String dirName) {
    File target = FileUtils.toFile(TestProjectUtils.class.getResource(String.format("/%s/target/", dirName)));
    Collection<File> jars = FileUtils.listFiles(target, new String[] {"jar"}, false);
    if (jars == null || jars.size() != 1) {
      throw new IllegalArgumentException("Test project is badly defined: " + dirName);
    }
    return jars.iterator().next();
  }
}

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
package org.sonar.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.apache.commons.io.IOUtils;

import static org.sonar.process.FileUtils2.deleteQuietly;

public final class ConfigurationUtils {

  private ConfigurationUtils() {
    // Utility class
  }

  static Props loadPropsFromCommandLineArgs(String[] args) {
    if (args.length != 1) {
      throw new IllegalArgumentException("Only a single command-line argument is accepted " +
        "(absolute path to configuration file)");
    }

    File propertyFile = new File(args[0]);
    Properties properties = new Properties();
    Reader reader = null;
    try {
      reader = new InputStreamReader(new FileInputStream(propertyFile), StandardCharsets.UTF_8);
      properties.load(reader);
    } catch (Exception e) {
      throw new IllegalStateException("Could not read properties from file: " + args[0], e);
    } finally {
      IOUtils.closeQuietly(reader);
      deleteQuietly(propertyFile);
    }
    return new Props(properties);
  }
}

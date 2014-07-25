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
package org.sonar.process;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

public final class ConfigurationUtils {

  private ConfigurationUtils() {
    // Utility class
  }

  public static Properties interpolateVariables(Properties properties, Map<String, String> variables) {
    Properties result = new Properties();
    Enumeration keys = properties.keys();
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      String value = (String) properties.get(key);
      String interpolatedValue = StrSubstitutor.replace(value, variables, "${env:", "}");
      result.setProperty(key, interpolatedValue);
    }
    return result;
  }

  public static Props loadPropsFromCommandLineArgs(String[] args) {
    if (args.length != 1) {
      throw new IllegalStateException("Only a single command-line argument is accepted " +
        "(absolute path to configuration file)");
    }

    File propertyFile = new File(args[0]);
    if (!propertyFile.exists()) {
      throw new IllegalStateException("Property file '" + args[0] + "' does not exist! ");
    }

    Properties properties = new Properties();
    FileReader reader = null;
    try {
      reader = new FileReader(propertyFile);
      properties.load(reader);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read properties from file '" + args[0] + "'", e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
    return new Props(properties);
  }
}

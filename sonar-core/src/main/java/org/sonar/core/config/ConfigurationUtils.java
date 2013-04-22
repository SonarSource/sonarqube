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
package org.sonar.core.config;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;

import javax.annotation.WillClose;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * @since 2.12
 */
public final class ConfigurationUtils {

  private ConfigurationUtils() {
  }

  public static void copyProperties(Properties from, Map<String, String> to) {
    for (Map.Entry<Object, Object> entry : from.entrySet()) {
      String key = (String) entry.getKey();
      to.put(key, entry.getValue().toString());
    }
  }

  public static Properties openProperties(File file) throws IOException {
    FileInputStream input = FileUtils.openInputStream(file);
    return readInputStream(input);
  }

  /**
   * Note that the input stream is closed in this method.
   */
  public static Properties readInputStream(@WillClose InputStream input) throws IOException {
    try {
      Properties p = new Properties();
      p.load(input);
      return p;

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  public static Properties interpolateEnvVariables(Properties properties) {
    return interpolateVariables(properties, System.getenv());
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

  public static void copyToCommonsConfiguration(Map<String, String> input, Configuration commonsConfig) {
    // update deprecated configuration
    commonsConfig.clear();
    for (Map.Entry<String, String> entry : input.entrySet()) {
      String key = entry.getKey();
      commonsConfig.setProperty(key, entry.getValue());
    }
  }
}

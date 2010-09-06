/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.checkstyle;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public enum CheckstyleVersion {
  INSTANCE;

  private static final String PROPERTIES_PATH = "/org/sonar/plugins/checkstyle/checkstyle-plugin.properties";
  private String version;

  public static String getVersion() {
    return INSTANCE.version;
  }

  private CheckstyleVersion() {
    InputStream input = getClass().getResourceAsStream(PROPERTIES_PATH);
    try {
      Properties properties = new Properties();
      properties.load(input);
      this.version = properties.getProperty("checkstyle.version");

    } catch (IOException e) {
      LoggerFactory.getLogger(getClass()).warn("Can not load the Checkstyle version from the file " + PROPERTIES_PATH);
      this.version = "";

    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}

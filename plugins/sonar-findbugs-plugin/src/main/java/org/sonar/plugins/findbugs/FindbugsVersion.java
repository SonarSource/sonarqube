/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.findbugs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

/**
 * @since 2.4
 */
public enum FindbugsVersion {
  INSTANCE;

  private static final String PROPERTIES_PATH = "/org/sonar/plugins/findbugs/findbugs-plugin.properties";
  private String version;

  public static String getVersion() {
    return INSTANCE.version;
  }

  private FindbugsVersion() {
    InputStream input = getClass().getResourceAsStream(PROPERTIES_PATH);
    try {
      Properties properties = new Properties();
      properties.load(input);
      this.version = properties.getProperty("findbugs.version");

    } catch (IOException e) {
      LoggerFactory.getLogger(getClass()).warn("Can not load the Findbugs version from the file " + PROPERTIES_PATH);
      this.version = "";

    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}

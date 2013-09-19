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
package org.sonar.server.platform;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.commons.lang.StringUtils;
import org.sonar.core.config.ConfigurationUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Search fo the Sonar installation directory in the following ordered steps :
 * <ol>
 *   <li>system property SONAR_HOME</li>
 *   <li>environment variable SONAR_HOME</li>
 *   <li>property SONAR_HOME in the file WEB-INF/classes/sonar-war.properties</li>
 * </ol>
 *
 * @since 2.12
 */
final class SonarHome {

  private SonarHome() {
    // only static methods
  }

  static final String PROPERTY = "SONAR_HOME";
  static Supplier<File> homeSupplier = Suppliers.memoize(new Supplier<File>() {
    public File get() {
      File home = locate();
      System.setProperty(PROPERTY, home.getAbsolutePath());
      return home;
    }
  });

  static File getHome() {
    return homeSupplier.get();
  }

  static File locate() {
    String value = System.getProperty(PROPERTY);
    if (StringUtils.isBlank(value)) {
      value = System.getenv(PROPERTY);
      if (StringUtils.isBlank(value)) {
        value = openWarProperties().getProperty(PROPERTY);
      }
    }

    if (StringUtils.isBlank(value)) {
      throw new IllegalStateException("SonarQube value is not defined. " +
          "Please set the environment variable/system property " + PROPERTY + " or edit the file WEB-INF/classes/sonar-war.properties");
    }

    File dir = new File(value);
    if (!dir.isDirectory() || !dir.exists()) {
      throw new IllegalStateException(PROPERTY + " is not valid: " + value + ". Please fix the environment variable/system property SONAR_HOME or " +
          "the file WEB-INF/classes/sonar-war.properties");
    }
    return dir;
  }

  private static Properties openWarProperties() {
    try {
      InputStream input = SonarHome.class.getResourceAsStream("/sonar-war.properties");
      // it closes the stream
      return ConfigurationUtils.readInputStream(input);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to load the file sonar-war.properties", e);
    }
  }
}

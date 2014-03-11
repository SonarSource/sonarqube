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
package org.sonar.server.platform;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.commons.lang.StringUtils;

import java.io.File;

/**
 * Search fo the Sonar installation directory in the following ordered steps :
 * <ol>
 * <li>system property SONAR_HOME</li>
 * <li>environment variable SONAR_HOME</li>
 * </ol>
 *
 * @since 2.12
 */
final class SonarHome {

  private SonarHome() {
    // only static methods
  }

  static final String SONAR_HOME = "SONAR_HOME";

  static Supplier<File> homeSupplier = Suppliers.memoize(new Supplier<File>() {
    public File get() {
      File home = locate();
      System.setProperty(SONAR_HOME, home.getAbsolutePath());
      return home;
    }
  });

  static File getHome() {
    return homeSupplier.get();
  }

  static File locate() {
    String value = System.getProperty(SONAR_HOME);
    if (StringUtils.isBlank(value)) {
      value = System.getenv(SONAR_HOME);
    }

    if (StringUtils.isBlank(value)) {
      throw new IllegalStateException("The system property or env variable " + SONAR_HOME + " is not set");
    }

    File dir = new File(value);
    if (!dir.isDirectory() || !dir.exists()) {
      throw new IllegalStateException(SONAR_HOME + " is not valid: " + value + ". Please fix the env variable/system " +
        "property " + SONAR_HOME);
    }
    return dir;
  }
}

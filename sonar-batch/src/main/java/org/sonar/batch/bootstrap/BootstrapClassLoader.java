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
package org.sonar.batch.bootstrap;

import org.sonar.api.utils.SonarException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * ClassLoader extended with the JDBC Driver hosted on the server-side.
 */
public class BootstrapClassLoader {

  private URLClassLoader classLoader;

  public BootstrapClassLoader(ExtensionDownloader extensionDownloader) {
    this(extensionDownloader.downloadJdbcDriver());
  }

  BootstrapClassLoader(File jdbcDriver) {
    try {
      ClassLoader parentClassLoader = BootstrapClassLoader.class.getClassLoader();
      classLoader = URLClassLoader.newInstance(new URL[]{jdbcDriver.toURI().toURL()}, parentClassLoader);

    } catch (MalformedURLException e) {
      throw new SonarException("Fail to get URL of : " + jdbcDriver.getAbsolutePath(), e);
    }
  }

  public URLClassLoader getClassLoader() {
    return classLoader;
  }
}

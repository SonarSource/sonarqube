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
package org.sonar.server.app;

import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;

import javax.servlet.ServletContext;

/**
 * Disable taglib and web-fragment.xml scanning of Tomcat. Should speed up startup.
 */

class NullJarScanner implements JarScanner {

  @Override
  public void scan(JarScanType jarScanType, ServletContext servletContext, JarScannerCallback jarScannerCallback) {
    // doing nothing is fast!
  }

  @Override
  public JarScanFilter getJarScanFilter() {
    return null;
  }

  @Override
  public void setJarScanFilter(JarScanFilter jarScanFilter) {
    // no need to filter
  }
}

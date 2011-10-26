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
package org.sonar.persistence;

import java.io.OutputStream;
import java.sql.DriverManager;

public final class DerbyUtils {

  private DerbyUtils() {
  }

  public static final OutputStream DEV_NULL = new OutputStream() {
    public void write(int b) {
    }
  };

  /**
   * The embedded derby still creates the file derby.log in the execution directory. This method moves it to target/tmp-test.
   */
  public static void fixDerbyLogs() {
    System.setProperty("derby.stream.error.field", "org.sonar.persistence.DerbyUtils.DEV_NULL");
  }

  public static void dropInMemoryDatabase() {
    try {
      DriverManager.getConnection("jdbc:derby:memory:sonar;drop=true");
    } catch (Exception e) {
      // silently ignore
    }
  }
}

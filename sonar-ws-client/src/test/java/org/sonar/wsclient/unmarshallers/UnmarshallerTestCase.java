/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.unmarshallers;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.sonar.wsclient.JdkUtils;
import org.sonar.wsclient.services.WSUtils;

import java.io.IOException;

public abstract class UnmarshallerTestCase {

  @BeforeClass
  public static void setupWsUtils() {
    WSUtils.setInstance(new JdkUtils());
  }

  public static String loadFile(String path) {
    try {
      return IOUtils.toString(UnmarshallerTestCase.class.getResourceAsStream(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}

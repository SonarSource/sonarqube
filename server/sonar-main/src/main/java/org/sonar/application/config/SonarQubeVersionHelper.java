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
package org.sonar.application.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static java.lang.String.format;

public class SonarQubeVersionHelper {
  private static final String SONARQUBE_VERSION_PATH = "/sonarqube-version.txt";

  private static String sonarqubeVersion;

  private SonarQubeVersionHelper() {
    // only static methods
  }

  public static String getSonarqubeVersion() {
    if (sonarqubeVersion == null) {
      loadVersion();
    }
    return sonarqubeVersion;
  }

  private static synchronized void loadVersion() {
    try {
      try (BufferedReader in = new BufferedReader(
        new InputStreamReader(
          SonarQubeVersionHelper.class.getResourceAsStream(SONARQUBE_VERSION_PATH),
          StandardCharsets.UTF_8
        ))) {
        sonarqubeVersion = in.readLine();
      }
    } catch (IOException e) {
      throw new IllegalStateException(format("Cannot load %s from classpath", SONARQUBE_VERSION_PATH), e);
    }
  }
}

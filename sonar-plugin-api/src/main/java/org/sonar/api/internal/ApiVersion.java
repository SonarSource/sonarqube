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
package org.sonar.api.internal;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;

/**
 * For internal use
 *
 * @since 6.0
 */
public class ApiVersion {

  private static final String FILE_PATH = "/sonar-api-version.txt";

  private ApiVersion() {
    // only static methods
  }

  public static Version load(System2 system) {
    try {
      URL url = system.getResource(FILE_PATH);
      String versionInFile = Resources.toString(url, StandardCharsets.UTF_8);
      return Version.parse(versionInFile);
    } catch (IOException e) {
      throw new IllegalStateException("Can not load " + FILE_PATH + " from classpath", e);
    }
  }
}

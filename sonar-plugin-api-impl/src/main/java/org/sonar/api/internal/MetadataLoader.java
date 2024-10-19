/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import org.sonar.api.SonarEdition;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * For internal use
 *
 * @since 7.8
 */
public class MetadataLoader {

  private static final String SQ_VERSION_FILE_PATH = "/sq-version.txt";
  private static final String SONAR_API_VERSION_FILE_PATH = "/sonar-api-version.txt";
  private static final String EDITION_FILE_PATH = "/sonar-edition.txt";
  private static final String SQ_VERSION_EOL_FILE_PATH = "/sq-version-eol.txt";
  public static final String CAN_NOT_LOAD_FROM_CLASSPATH = "Can not load %s from classpath";

  private MetadataLoader() {
    // only static methods
  }

  public static Version loadApiVersion(System2 system) {
    return getVersion(system, SONAR_API_VERSION_FILE_PATH);
  }

  public static Version loadSQVersion(System2 system) {
    return getVersion(system, SQ_VERSION_FILE_PATH);
  }

  public static String loadSqVersionEol(System2 system) {
    return getParamFromFile(system, SQ_VERSION_EOL_FILE_PATH);
  }

  private static Version getVersion(System2 system, String versionFilePath) {
    return Version.parse(getParamFromFile(system, versionFilePath));
  }

  private static String getParamFromFile(System2 system, String filePath) {
    URL url = system.getResource(filePath);
    try (Scanner scanner = new Scanner(url.openStream(), StandardCharsets.UTF_8)) {
      return scanner.nextLine();
    } catch (IOException e) {
      throw new IllegalStateException(format(CAN_NOT_LOAD_FROM_CLASSPATH, filePath), e);
    }
  }

  public static SonarEdition loadEdition(System2 system) {
    URL url = system.getResource(EDITION_FILE_PATH);
    if (url == null) {
      return SonarEdition.COMMUNITY;
    }
    try (Scanner scanner = new Scanner(url.openStream(), StandardCharsets.UTF_8)) {
      String editionInFile = scanner.nextLine();
      return parseEdition(editionInFile);
    } catch (IOException e) {
      throw new IllegalStateException(format(CAN_NOT_LOAD_FROM_CLASSPATH, EDITION_FILE_PATH), e);
    }
  }

  static SonarEdition parseEdition(String edition) {
    String str = trimToEmpty(edition.toUpperCase(Locale.ENGLISH));
    try {
      return SonarEdition.valueOf(str);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(format("Invalid edition found in '%s': '%s'", EDITION_FILE_PATH, str));
    }
  }
}

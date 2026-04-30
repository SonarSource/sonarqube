/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class DbVersionPackageConsistencyTest {

  @TestFactory
  Collection<DynamicTest> each_version_package_should_not_exceed_current_commercial_version() throws IOException {
    int currentVersion = readCurrentVersion();

    Path versionDir = Paths.get("src/main/java/org/sonar/server/platform/db/migration/version");
    assertThat(versionDir).exists();

    try (Stream<Path> paths = Files.list(versionDir)) {
      return paths
        .filter(Files::isDirectory)
        .map(p -> p.getFileName().toString())
        .filter(name -> name.matches("v\\d+"))
        .map(name -> dynamicTest(name + " must not exceed current version " + currentVersion, () -> assertThat(Integer.parseInt(name.substring(1)))
          .describedAs(
            "Package '%s' references a version beyond the current version %d (from private/gradle.properties). "
              + "Package names under 'version/' must not reference a future release.",
            name, currentVersion)
          .isLessThanOrEqualTo(currentVersion)))
        .toList();
    }
  }

  private static int readCurrentVersion() throws IOException {
    Path gradleProperties = Paths.get("../../private/gradle.properties");
    assertThat(gradleProperties)
      .describedAs("private/gradle.properties not found — this test requires an enterprise/commercial build")
      .exists();
    Properties properties = new Properties();
    try (InputStream is = Files.newInputStream(gradleProperties)) {
      properties.load(is);
    }
    String commercialVersion = properties.getProperty("commercialVersion");
    assertThat(commercialVersion)
      .describedAs("commercialVersion not found in private/gradle.properties")
      .isNotNull();
    String[] parts = commercialVersion.split("\\.");
    assertThat(parts).hasSize(2);
    return Integer.parseInt(parts[0]) * 100 + Integer.parseInt(parts[1]);
  }
}

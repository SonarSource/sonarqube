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
package org.sonar.process;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MinimumViableSystem {

  private final Map<String, String> requiredJavaOptions = new HashMap<String, String>();

  public MinimumViableSystem setRequiredJavaOption(String propertyKey, String expectedValue) {
    requiredJavaOptions.put(propertyKey, expectedValue);
    return this;
  }

  /**
   * Entry point for all checks
   */
  public void check() {
    checkJavaVersion();
    checkJavaOptions();
    checkWritableTempDir();
  }

  /**
   * Verify that temp directory is writable
   */
  private void checkWritableTempDir() {
    checkWritableDir(System.getProperty("java.io.tmpdir"));
  }

  void checkWritableDir(String tempPath) {
    try {
      File tempFile = File.createTempFile("check", "tmp", new File(tempPath));
      FileUtils.deleteQuietly(tempFile);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Temp directory is not writable: %s", tempPath), e);
    }
  }

  void checkJavaOptions() {
    for (Map.Entry<String, String> entry : requiredJavaOptions.entrySet()) {
      String value = System.getProperty(entry.getKey());
      if (!StringUtils.equals(value, entry.getValue())) {
        throw new MessageException(String.format(
          "JVM option '%s' must be set to '%s'. Got '%s'", entry.getKey(), entry.getValue(), StringUtils.defaultString(value)));
      }
    }
  }

  void checkJavaVersion() {
    String javaVersion = System.getProperty("java.specification.version");
    checkJavaVersion(javaVersion);
  }

  void checkJavaVersion(String javaVersion) {
    if (!javaVersion.startsWith("1.6") && !javaVersion.startsWith("1.7") && !javaVersion.startsWith("1.8")) {
      // still better than "java.lang.UnsupportedClassVersionError: Unsupported major.minor version 49.0
      throw new MessageException(String.format("Supported versions of Java are 1.6, 1.7 and 1.8. Got %s.", javaVersion));
    }
  }

}

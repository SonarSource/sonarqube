/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.process;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

import static java.lang.String.format;
import static org.sonar.process.FileUtils2.deleteQuietly;

public class MinimumViableSystem {

  /**
   * Verify that temp directory is writable
   */
  public MinimumViableSystem checkWritableTempDir() {
    checkWritableDir(System.getProperty("java.io.tmpdir"));
    return this;
  }

  // Visible for testing
  void checkWritableDir(String tempPath) {
    try {
      File tempFile = File.createTempFile("check", "tmp", new File(tempPath));
      deleteQuietly(tempFile);
    } catch (IOException e) {
      throw new IllegalStateException(format("Temp directory is not writable: %s", tempPath), e);
    }
  }

  public MinimumViableSystem checkRequiredJavaOptions(Map<String, String> requiredJavaOptions) {
    for (Map.Entry<String, String> entry : requiredJavaOptions.entrySet()) {
      String value = System.getProperty(entry.getKey());
      if (!StringUtils.equals(value, entry.getValue())) {
        throw new MessageException(format(
          "JVM option '%s' must be set to '%s'. Got '%s'", entry.getKey(), entry.getValue(), StringUtils.defaultString(value)));
      }
    }
    return this;
  }
}

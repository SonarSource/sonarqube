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
package org.sonar.process.monitor;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Reads process output and writes to logs
 */
class StreamGobbler extends Thread {

  private final InputStream is;
  private final Logger logger;

  StreamGobbler(InputStream is, String processKey) {
    this(is, processKey, LoggerFactory.getLogger("gobbler"));
  }

  StreamGobbler(InputStream is, String processKey, Logger logger) {
    super(String.format("Gobbler[%s]", processKey));
    this.is = is;
    this.logger = logger;
  }

  @Override
  public void run() {
    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    try {
      String line;
      while ((line = br.readLine()) != null) {
        logger.info(line);
      }
    } catch (Exception ignored) {
      // ignored
    } finally {
      IOUtils.closeQuietly(br);
    }
  }

  static void waitUntilFinish(@Nullable StreamGobbler gobbler) {
    if (gobbler != null) {
      try {
        gobbler.join();
      } catch (InterruptedException ignored) {
        // consider as finished
      }
    }
  }
}

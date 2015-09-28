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
package org.sonar.server.computation.step;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.computation.batch.BatchReportReader;

public class LogScannerContextStep implements ComputationStep {

  private static final Logger LOGGER = Loggers.get(LogScannerContextStep.class);

  private final BatchReportReader reportReader;

  public LogScannerContextStep(BatchReportReader reportReader) {
    this.reportReader = reportReader;
  }

  @Override
  public void execute() {
    Reader logReader = reportReader.readScannerLogs();
    if (logReader != null) {
      log(logReader);
    }
  }

  private void log(Reader logReader) {
    BufferedReader linesReader = null;
    try {
      linesReader = new BufferedReader(logReader);
      String line = linesReader.readLine();
      while (line != null) {
        LOGGER.info(line);
        line = linesReader.readLine();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to read scanner logs", e);
    } finally {
      IOUtils.closeQuietly(linesReader);
    }
  }

  @Override
  public String getDescription() {
    return "Log Scanner Context";
  }
}

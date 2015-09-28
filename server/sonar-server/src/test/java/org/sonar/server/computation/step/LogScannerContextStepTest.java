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

import java.io.InputStreamReader;
import java.io.StringReader;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.computation.batch.BatchReportReaderRule;

import static org.assertj.core.api.Assertions.assertThat;

public class LogScannerContextStepTest {

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public LogTester logTester = new LogTester();

  LogScannerContextStep underTest = new LogScannerContextStep(reportReader);

  @Test
  public void do_nothing_if_no_scanner_logs() {
    underTest.execute();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void log_scanner_logs() {
    reportReader.setScannerLogs(new StringReader("log1\nlog2"));

    underTest.execute();

    assertThat(logTester.logs(LoggerLevel.INFO)).containsExactly("log1", "log2");
  }


}

/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonarsource.sonarqube.perf.server;

import com.sonar.orchestrator.Orchestrator;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.sonarsource.sonarqube.perf.PerfTestCase;
import org.sonarsource.sonarqube.perf.ServerLogs;

public class ServerTest extends PerfTestCase {
  private static final int TIMEOUT_3_MINUTES = 1000 * 60 * 3;

  @Rule
  public Timeout timeout = new Timeout(TIMEOUT_3_MINUTES);

  // ES + TOMCAT
  @Test
  public void server_startup_and_shutdown() throws Exception {
    String defaultWebJavaOptions = "-Xmx768m -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djruby.management.enabled=false";
    Orchestrator orchestrator = Orchestrator.builderEnv()
      .setOrchestratorProperty("javaVersion", "LATEST_RELEASE")
      .addPlugin("java")

      // See http://wiki.apache.org/tomcat/HowTo/FasterStartUp
      // Sometimes source of entropy is too small and Tomcat spends ~20 seconds on the step :
      // "Creation of SecureRandom instance for session ID generation using [SHA1PRNG]"
      // Using /dev/urandom fixes the issue on linux
      .setServerProperty("sonar.web.javaOpts", defaultWebJavaOptions + " -Djava.security.egd=file:/dev/./urandom")
      .build();
    try {
      ServerLogs.clear(orchestrator);
      orchestrator.start();

      // compare dates of first and last log
      long firstLogDate = ServerLogs.extractFirstDate(readLogLines(orchestrator)).getTime();
      long startedAtDate = extractStartedAtDate(orchestrator);
      assertDurationAround(startedAtDate - firstLogDate, 38_000);

      ServerLogs.clear(orchestrator);
      orchestrator.stop();

      List<String> lines = readLogLines(orchestrator);
      long firstStopLogDate = ServerLogs.extractFirstDate(lines).getTime();
      long stopDate = extractStopDate(lines);
      assertDurationLessThan(stopDate - firstStopLogDate, 10_000);

    } finally {
      orchestrator.stop();
    }
  }

  private static long extractStartedAtDate(Orchestrator orchestrator) throws IOException {
    Date startedAtDate = extractStartedDate(readLogLines(orchestrator));
    // if SQ never starts, the test will fail with timeout
    while (startedAtDate == null) {
      try {
        Thread.sleep(100);
        startedAtDate = extractStartedDate(readLogLines(orchestrator));
      } catch (InterruptedException e) {
        // ignored
      }
    }
    return startedAtDate.getTime();
  }

  private static Date extractStartedDate(List<String> lines) {
    Collections.reverse(lines);
    Date end = null;
    for (String line : lines) {
      if (line.contains("Compute Engine is up")) {
        end = ServerLogs.extractDate(line);
        break;
      }
    }
    return end;
  }

  private static long extractStopDate(List<String> lines) throws IOException {
    Collections.reverse(lines);
    Date end = ServerLogs.extractFirstDate(lines);
    return end.getTime();
  }

  private static List<String> readLogLines(Orchestrator orchestrator) throws IOException {
    return FileUtils.readLines(orchestrator.getServer().getCeLogs());
  }
}

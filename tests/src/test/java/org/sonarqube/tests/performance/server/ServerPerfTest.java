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
package org.sonarqube.tests.performance.server;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.sonarqube.tests.performance.AbstractPerfTest;
import org.sonarqube.tests.performance.ServerLogs;

import static org.apache.commons.io.FileUtils.readLines;

public class ServerPerfTest extends AbstractPerfTest {
  private static final int TIMEOUT_3_MINUTES = 1000 * 60 * 3;

  @Rule
  public Timeout timeout = new Timeout(TIMEOUT_3_MINUTES);

  // ES + TOMCAT
  @Test
  public void server_startup_and_shutdown() throws Exception {
    String defaultWebJavaOptions = "-Xmx768m -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true -Dfile.encoding=UTF-8";
    Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin(FileLocation.byWildcardMavenFilename(new File("../plugins/sonar-xoo-plugin/target"), "sonar-xoo-plugin-*.jar"))

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
      long firstLogDate = ServerLogs.extractFirstDate(readLines(orchestrator.getServer().getAppLogs())).getTime();
      long startedAtDate = extractStartedAtDate(orchestrator);
      assertDurationAround(startedAtDate - firstLogDate, 32_000);

      ServerLogs.clear(orchestrator);
      orchestrator.stop();

      List<String> lines = readLines(orchestrator.getServer().getAppLogs());
      long firstStopLogDate = ServerLogs.extractFirstDate(lines).getTime();
      long stopDate = extractStopDate(lines);
      assertDurationLessThan(stopDate - firstStopLogDate, 10_000);

    } finally {
      orchestrator.stop();
    }
  }

  private static long extractStartedAtDate(Orchestrator orchestrator) throws IOException {
    Date startedAtDate = extractStartedDate(readLines(orchestrator.getServer().getCeLogs()));
    // if SQ never starts, the test will fail with timeout
    while (startedAtDate == null) {
      try {
        Thread.sleep(100);
        startedAtDate = extractStartedDate(readLines(orchestrator.getServer().getCeLogs()));
      } catch (InterruptedException e) {
        // ignored
        Thread.currentThread().interrupt();
      }
    }
    return startedAtDate.getTime();
  }

  private static Date extractStartedDate(List<String> lines) {
    Collections.reverse(lines);
    Date end = null;
    for (String line : lines) {
      if (line.contains("Compute Engine is operational")) {
        end = ServerLogs.extractDate(line);
        break;
      }
    }
    return end;
  }

  private static long extractStopDate(List<String> lines) {
    Collections.reverse(lines);
    Date end = ServerLogs.extractFirstDate(lines);
    return end.getTime();
  }

}

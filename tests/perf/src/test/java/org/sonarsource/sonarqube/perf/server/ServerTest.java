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
import org.sonarsource.sonarqube.perf.PerfTestCase;
import org.sonarsource.sonarqube.perf.ServerLogs;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class ServerTest extends PerfTestCase {

  // ES + TOMCAT
  @Test
  public void server_startup_and_shutdown() throws Exception {
    String defaultWebJavaOptions = "-Xmx768m -XX:MaxPermSize=160m -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djruby.management.enabled=false";
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
      long startupDuration = start(orchestrator);
      assertDurationAround(startupDuration, 46000);

      long shutdownDuration = stop(orchestrator);
      // can't use percent margins because logs are second-grained but not milliseconds
      assertDurationLessThan(shutdownDuration, 4000);

    } finally {
      orchestrator.stop();
    }
  }

  long start(Orchestrator orchestrator) throws IOException {
    ServerLogs.clear(orchestrator);
    orchestrator.start();
    return logsPeriod(orchestrator);
  }

  long stop(Orchestrator orchestrator) throws Exception {
    ServerLogs.clear(orchestrator);
    orchestrator.stop();
    return logsPeriod(orchestrator);
  }

  private long logsPeriod(Orchestrator orchestrator) throws IOException {
    // compare dates of first and last log
    List<String> lines = FileUtils.readLines(orchestrator.getServer().getLogs());
    if (lines.size() < 2) {
      throw new IllegalStateException("Fail to estimate server shutdown or startup duration. Not enough logs.");
    }
    Date start = ServerLogs.extractFirstDate(lines);
    Collections.reverse(lines);
    Date end = ServerLogs.extractFirstDate(lines);
    return end.getTime() - start.getTime();
  }
}

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
package org.sonarsource.sonarqube.perf.scanner.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.sonarsource.sonarqube.perf.PerfTestCase;
import java.io.IOException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class DuplicationTest extends PerfTestCase {

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = ScannerPerfTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void setUp() throws IOException {
    // Execute a first analysis to prevent any side effects with cache of plugin JAR files
    orchestrator.executeBuild(newScanner("-Xmx512m -server", "sonar.profile", "one-xoo-issue-per-line"));
  }

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  /**
   * SONAR-3060
   */
  @Test
  public void hugeJavaFile() {
    MavenBuild build = MavenBuild.create(FileLocation.of("projects/huge-file/pom.xml").getFile())
      .setEnvironmentVariable("MAVEN_OPTS", "-Xmx1024m")
      .setProperty("sonar.sourceEncoding", "UTF-8")
      .setCleanSonarGoals();
    orchestrator.executeBuild(build);
    Resource file = getResource("com.sonarsource.it.samples:huge-file:src/main/java/huge/HugeFile.java");
    assertThat(file.getMeasureValue("duplicated_lines")).isGreaterThan(50000.0);
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics(key, "duplicated_lines", "duplicated_blocks", "duplicated_files", "duplicated_lines_density", "useless-duplicated-lines"));
  }
}

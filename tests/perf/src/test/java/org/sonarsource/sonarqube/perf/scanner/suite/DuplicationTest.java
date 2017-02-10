/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarsource.sonarqube.perf.scanner.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.measure.ComponentWsRequest;
import org.sonarsource.sonarqube.perf.PerfTestCase;

import static java.lang.Double.parseDouble;
import static java.util.Arrays.asList;
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
    Map<String, Double> measure = getMeasures("com.sonarsource.it.samples:huge-file:src/main/java/huge/HugeFile.java");
    assertThat(measure.get("duplicated_lines")).isGreaterThan(50000.0);
  }

  private Map<String, Double> getMeasures(String key) {
    return newWsClient().measures().component(new ComponentWsRequest()
      .setComponentKey(key)
      .setMetricKeys(asList("duplicated_lines", "duplicated_blocks", "duplicated_files", "duplicated_lines_density")))
      .getComponent().getMeasuresList()
      .stream()
      .collect(Collectors.toMap(WsMeasures.Measure::getMetric, measure -> parseDouble(measure.getValue())));
  }

  private WsClient newWsClient() {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(orchestrator.getServer().getUrl())
      .build());
  }
}

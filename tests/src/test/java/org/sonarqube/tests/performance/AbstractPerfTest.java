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
package org.sonarqube.tests.performance;

import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.rules.TestName;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractPerfTest {
  static final double ACCEPTED_DURATION_VARIATION_IN_PERCENTS = 12.0;

  @Rule
  public TestName testName = new TestName();

  protected void assertDurationAround(long duration, long expectedDuration) {
    double variation = 100.0 * (0.0 + duration - expectedDuration) / expectedDuration;
    System.out.printf("Test %s : executed in %d ms (%.2f %% from target)\n", testName.getMethodName(), duration, variation);
    assertThat(Math.abs(variation)).as(String.format("Expected %d ms, got %d ms", expectedDuration, duration)).isLessThan(ACCEPTED_DURATION_VARIATION_IN_PERCENTS);
  }

  protected void assertDurationLessThan(long duration, long maxDuration) {
    System.out.printf("Test %s : %d ms (max allowed is %d)\n", testName.getMethodName(), duration, maxDuration);
    assertThat(duration).as(String.format("Expected less than %d ms, got %d ms", maxDuration, duration)).isLessThanOrEqualTo(maxDuration);
  }

  protected Properties readProfiling(File baseDir, String moduleKey) throws IOException {
    File profilingFile = new File(baseDir, ".sonar/profiling/" + moduleKey + "-profiler.properties");
    Properties props = new Properties();
    FileInputStream in = FileUtils.openInputStream(profilingFile);
    try {
      props.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
    return props;
  }

  /**
   * New batch analysis with most features disabled by default (empty QP, no CPD, no SCM, ...)
   */
  public static SonarScanner newScanner(String sonarRunnerOpts, String... props) {
    SonarScanner scanner = SonarScanner.create()
      .setProperties(
        "sonar.scm.disabled", "true",
        "sonar.cpd.exclusions", "**")
      .setProperties(props);
    scanner
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", sonarRunnerOpts)
      .setEnvironmentVariable("SONAR_SCANNER_OPTS", sonarRunnerOpts)
      .setProjectDir(FileLocation.of("projects/performance/xoo-sample").getFile());
    return scanner;
  }
}

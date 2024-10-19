/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.mediumtest.coverage;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.xoo.XooPlugin;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericCoverageMediumIT {
  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way");

  @Test
  public void singleReport() {

    File projectDir = new File("test-resources/mediumtest/xoo/sample-generic-coverage");

    AnalysisResult result = tester
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .property("sonar.coverageReportPaths", "coverage.xml")
      .execute();

    InputFile noConditions = result.inputFile("xources/hello/NoConditions.xoo");
    assertThat(result.coverageFor(noConditions, 6).getHits()).isTrue();
    assertThat(result.coverageFor(noConditions, 6).getConditions()).isZero();
    assertThat(result.coverageFor(noConditions, 6).getCoveredConditions()).isZero();

    assertThat(result.coverageFor(noConditions, 7).getHits()).isFalse();

    InputFile withConditions = result.inputFile("xources/hello/WithConditions.xoo");
    assertThat(result.coverageFor(withConditions, 3).getHits()).isTrue();
    assertThat(result.coverageFor(withConditions, 3).getConditions()).isEqualTo(2);
    assertThat(result.coverageFor(withConditions, 3).getCoveredConditions()).isOne();

    assertThat(logTester.logs()).noneMatch(l -> l.contains("Please use 'sonar.coverageReportPaths'"));

  }

  @Test
  public void twoReports() {

    File projectDir = new File("test-resources/mediumtest/xoo/sample-generic-coverage");

    AnalysisResult result = tester
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .property("sonar.coverageReportPaths", "coverage.xml,coverage2.xml")
      .execute();

    InputFile noConditions = result.inputFile("xources/hello/NoConditions.xoo");
    assertThat(result.coverageFor(noConditions, 6).getHits()).isTrue();
    assertThat(result.coverageFor(noConditions, 6).getConditions()).isZero();
    assertThat(result.coverageFor(noConditions, 6).getCoveredConditions()).isZero();

    assertThat(result.coverageFor(noConditions, 7).getHits()).isTrue();

    InputFile withConditions = result.inputFile("xources/hello/WithConditions.xoo");
    assertThat(result.coverageFor(withConditions, 3).getHits()).isTrue();
    assertThat(result.coverageFor(withConditions, 3).getConditions()).isEqualTo(2);
    assertThat(result.coverageFor(withConditions, 3).getCoveredConditions()).isEqualTo(2);

    assertThat(logTester.logs()).noneMatch(l -> l.contains("Please use 'sonar.coverageReportPaths'"));
  }
  
}

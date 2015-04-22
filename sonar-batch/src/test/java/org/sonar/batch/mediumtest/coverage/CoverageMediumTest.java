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
package org.sonar.batch.mediumtest.coverage;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CoverageMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .build();

  @Before
  public void prepare() {
    tester.start();
  }

  @After
  public void stop() {
    tester.stop();
  }

  @Test
  public void unitTests() throws IOException {

    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File xooUtCoverageFile = new File(srcDir, "sample.xoo.coverage");
    FileUtils.write(xooFile, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}");
    FileUtils.write(xooUtCoverageFile, "2:2:2:1\n3:1");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .start();

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.coverageFor(file, 2).getUtHits()).isTrue();
    assertThat(result.coverageFor(file, 2).getItHits()).isFalse();
    assertThat(result.coverageFor(file, 2).getConditions()).isEqualTo(2);
    assertThat(result.coverageFor(file, 2).getUtCoveredConditions()).isEqualTo(1);
    assertThat(result.coverageFor(file, 2).getItCoveredConditions()).isEqualTo(0);
    assertThat(result.coverageFor(file, 2).getOverallCoveredConditions()).isEqualTo(0);

    assertThat(result.measures()).contains(new DefaultMeasure<Integer>()
      .forMetric(CoreMetrics.LINES_TO_COVER)
      .onFile(new DefaultInputFile("com.foo.project", "src/sample.xoo"))
      .withValue(2));

    assertThat(result.measures()).contains(new DefaultMeasure<Integer>()
      .forMetric(CoreMetrics.UNCOVERED_LINES)
      .onFile(new DefaultInputFile("com.foo.project", "src/sample.xoo"))
      .withValue(0));

    assertThat(result.measures()).contains(new DefaultMeasure<Integer>()
      .forMetric(CoreMetrics.CONDITIONS_TO_COVER)
      .onFile(new DefaultInputFile("com.foo.project", "src/sample.xoo"))
      .withValue(2));

    assertThat(result.measures()).contains(new DefaultMeasure<String>()
      .forMetric(CoreMetrics.COVERED_CONDITIONS_BY_LINE)
      .onFile(new DefaultInputFile("com.foo.project", "src/sample.xoo"))
      .withValue("2=1"));
  }

}

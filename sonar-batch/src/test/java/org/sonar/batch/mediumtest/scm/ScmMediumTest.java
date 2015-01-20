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
package org.sonar.batch.mediumtest.scm;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ScmMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .bootstrapProperties(ImmutableMap.of("sonar.analysis.mode", "sensor"))
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
  public void testScmMeasure() throws IOException {

    File baseDir = prepareProject();

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.scm.provider", "xoo")
        .build())
      .start();

    assertThat(result.measures()).hasSize(5);

    assertThat(result.measures()).contains(new DefaultMeasure<Integer>()
      .forMetric(CoreMetrics.LINES)
      .onFile(new DefaultInputFile("com.foo.project", "src/sample.xoo"))
      .withValue(5));

    assertThat(result.measures()).contains(new DefaultMeasure<String>()
      .forMetric(CoreMetrics.SCM_AUTHORS_BY_LINE)
      .onFile(new DefaultInputFile("com.foo.project", "src/sample.xoo"))
      .withValue("1=;2=julien;3=julien;4=julien;5=simon"));
  }

  @Test
  public void noScmOnEmptyFile() throws IOException {

    File baseDir = prepareProject();

    // Clear file content
    FileUtils.write(new File(baseDir, "src/sample.xoo"), "");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.scm.provider", "xoo")
        .build())
      .start();

    // lines + qprofile
    assertThat(result.measures()).hasSize(2);

    assertThat(result.measures()).contains(new DefaultMeasure<Integer>()
      .forMetric(CoreMetrics.LINES)
      .onFile(new DefaultInputFile("com.foo.project", "src/sample.xoo"))
      .withValue(1));
  }

  @Test
  public void failIfMissingFile() throws IOException {

    File baseDir = prepareProject();
    File xooFile = new File(baseDir, "src/sample2.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent\n3\n4\n5");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Some files were not blamed");

    tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.scm.provider", "xoo")
        .build())
      .start();

  }

  @Test
  public void configureUsingScmURL() throws IOException {

    File baseDir = prepareProject();

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.links.scm_dev", "scm:xoo:foobar")
        .build())
      .start();

    assertThat(result.measures()).hasSize(5);

    assertThat(result.measures()).contains(new DefaultMeasure<Integer>()
      .forMetric(CoreMetrics.LINES)
      .onFile(new DefaultInputFile("com.foo.project", "src/sample.xoo"))
      .withValue(5));

    assertThat(result.measures()).contains(new DefaultMeasure<String>()
      .forMetric(CoreMetrics.SCM_AUTHORS_BY_LINE)
      .onFile(new DefaultInputFile("com.foo.project", "src/sample.xoo"))
      .withValue("1=;2=julien;3=julien;4=julien;5=simon"));
  }

  @Test
  public void testAutoDetection() throws IOException {

    File baseDir = prepareProject();
    new File(baseDir, ".xoo").createNewFile();

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

    assertThat(result.measures()).hasSize(5);

    assertThat(result.measures()).contains(new DefaultMeasure<Integer>()
      .forMetric(CoreMetrics.LINES)
      .onFile(new DefaultInputFile("com.foo.project", "src/sample.xoo"))
      .withValue(5));

    assertThat(result.measures()).contains(new DefaultMeasure<String>()
      .forMetric(CoreMetrics.SCM_AUTHORS_BY_LINE)
      .onFile(new DefaultInputFile("com.foo.project", "src/sample.xoo"))
      .withValue("1=;2=julien;3=julien;4=julien;5=simon"));
  }

  private File prepareProject() throws IOException {
    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File xooMeasureFile = new File(srcDir, "sample.xoo.measures");
    File xooScmFile = new File(srcDir, "sample.xoo.scm");
    FileUtils.write(xooFile, "Sample xoo\ncontent\n3\n4\n5");
    FileUtils.write(xooMeasureFile, "lines:5");
    FileUtils.write(xooScmFile,
      // revision,author,dateTime
      "1,,2013-01-04\n" +
        "1,julien,2013-01-04\n" +
        "2,julien,2013-02-03\n" +
        "2,julien,2013-02-03\n" +
        "3,simon,2013-03-04\n"
      );
    return baseDir;
  }

  @Test
  public void testDisableScmSensor() throws IOException {

    File baseDir = prepareProject();

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.scm.disabled", "true")
        .put("sonar.scm.provider", "xoo")
        .build())
      .start();

    assertThat(result.measures()).hasSize(2);
  }

}

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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.BatchMediumTester.TaskBuilder;
import org.sonar.batch.protocol.input.FileData;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Changesets.Changeset;
import org.sonar.batch.protocol.output.BatchReport.Component;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.xoo.XooPlugin;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ScmMediumTest {

  private static final String SAMPLE_XOO_CONTENT = "Sample xoo\ncontent";

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addFileData("com.foo.project", "src/sample2.xoo", new FileData(DigestUtils.md5Hex(SAMPLE_XOO_CONTENT), null))
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

    BatchReport.Changesets fileScm = getChangesets(baseDir, 0);

    assertThat(fileScm.getChangesetIndexByLineList()).hasSize(5);

    Changeset changesetLine1 = fileScm.getChangeset(fileScm.getChangesetIndexByLine(0));
    assertThat(changesetLine1.hasAuthor()).isFalse();

    Changeset changesetLine2 = fileScm.getChangeset(fileScm.getChangesetIndexByLine(1));
    assertThat(changesetLine2.getAuthor()).isEqualTo("julien");

    Changeset changesetLine3 = fileScm.getChangeset(fileScm.getChangesetIndexByLine(2));
    assertThat(changesetLine3.getAuthor()).isEqualTo("julien");

    Changeset changesetLine4 = fileScm.getChangeset(fileScm.getChangesetIndexByLine(3));
    assertThat(changesetLine4.getAuthor()).isEqualTo("julien");

    Changeset changesetLine5 = fileScm.getChangeset(fileScm.getChangesetIndexByLine(4));
    assertThat(changesetLine5.getAuthor()).isEqualTo("simon");
  }

  private BatchReport.Changesets getChangesets(File baseDir, int fileId) {
    File reportDir = new File(baseDir, ".sonar/batch-report");
    BatchReportReader reader = new BatchReportReader(reportDir);

    Component project = reader.readComponent(reader.readMetadata().getRootComponentRef());
    Component dir = reader.readComponent(project.getChildRef(0));
    Component file = reader.readComponent(dir.getChildRef(fileId));
    BatchReport.Changesets fileScm = reader.readChangesets(file.getRef());
    return fileScm;
  }

  @Test
  public void noScmOnEmptyFile() throws IOException {

    File baseDir = prepareProject();

    // Clear file content
    FileUtils.write(new File(baseDir, "src/sample.xoo"), "");

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

    BatchReport.Changesets changesets = getChangesets(baseDir, 0);

    assertThat(changesets).isNull();
  }

  @Test
  public void sample2_dont_need_blame() throws IOException {

    File baseDir = prepareProject();
    File xooFile = new File(baseDir, "src/sample2.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent\n3\n4\n5");

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

    BatchReport.Changesets file1Scm = getChangesets(baseDir, 0);
    assertThat(file1Scm).isNotNull();

    BatchReport.Changesets file2Scm = getChangesets(baseDir, 1);
    assertThat(file2Scm).isNull();
  }

  @Test
  public void forceReload() throws IOException {

    File baseDir = prepareProject();
    File xooFileNoScm = new File(baseDir, "src/sample2.xoo");
    FileUtils.write(xooFileNoScm, SAMPLE_XOO_CONTENT);
    File xooScmFile = new File(baseDir, "src/sample2.xoo.scm");
    FileUtils.write(xooScmFile,
      // revision,author,dateTime
      "1,foo,2013-01-04\n" +
        "1,bar,2013-01-04\n"
      );

    TaskBuilder taskBuilder = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.scm.provider", "xoo")
        // Force reload
        .put("sonar.scm.forceReloadAll", "true")
        .build());

    taskBuilder.start();

    BatchReport.Changesets file1Scm = getChangesets(baseDir, 0);
    assertThat(file1Scm).isNotNull();

    BatchReport.Changesets file2Scm = getChangesets(baseDir, 1);
    assertThat(file2Scm).isNotNull();
  }

  @Test
  public void configureUsingScmURL() throws IOException {

    File baseDir = prepareProject();

    tester.newTask()
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

    BatchReport.Changesets file1Scm = getChangesets(baseDir, 0);
    assertThat(file1Scm).isNotNull();
  }

  @Test
  public void testAutoDetection() throws IOException {

    File baseDir = prepareProject();
    new File(baseDir, ".xoo").createNewFile();

    tester.newTask()
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

    BatchReport.Changesets file1Scm = getChangesets(baseDir, 0);
    assertThat(file1Scm).isNotNull();
  }

  private File prepareProject() throws IOException {
    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile1 = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile1, "Sample xoo\ncontent\n3\n4\n5");
    File xooScmFile1 = new File(srcDir, "sample.xoo.scm");
    FileUtils.write(xooScmFile1,
      // revision,author,dateTime
      "1,,2013-01-04\n" +
        "2,julien,2013-01-04\n" +
        "3,julien,2013-02-03\n" +
        "3,julien,2013-02-03\n" +
        "4,simon,2013-03-04\n"
      );

    return baseDir;
  }

  @Test
  public void testDisableScmSensor() throws IOException {

    File baseDir = prepareProject();

    tester.newTask()
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
        .put("sonar.cpd.xoo.skip", "true")
        .build())
      .start();

    BatchReport.Changesets changesets = getChangesets(baseDir, 0);
    assertThat(changesets).isNull();
  }

}

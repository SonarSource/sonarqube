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
package org.sonarqube.tests.source;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.qa.util.Tester;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.moveFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.tests.source.SourceSuite.ORCHESTRATOR;
import static org.sonarqube.tests.source.ZipUtils.unzip;
import static util.ItUtils.projectDir;

public class NoScmThenScmTest {

  private static final String PROJECT_DIRECTORY = "xoo-sample-without-then-with-scm";
  private static final String PROJECT_NAME = "sample-without-then-with-scm";
  private static final String PATH_TO_SAMPLE = "src/main/xoo/sample/Sample.xoo";
  private static final String FILE_TO_ANALYSE = PROJECT_NAME + ":" + PATH_TO_SAMPLE;

  @ClassRule
  public static Orchestrator orchestrator = ORCHESTRATOR;
  private SourceScmWS ws;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void setUp() {
    ws = new SourceScmWS(tester);
  }

  @Test
  public void without_and_then_with_scm_on_same_file() throws ParseException, IOException {

    File source = disposableWorkspaceFor(PROJECT_DIRECTORY);

    // First run
    orchestrator.executeBuild(newScannerWithoutSCM(source));

    Map<Integer, LineData> scmData = ws.getScmData(FILE_TO_ANALYSE);
    assertThat(scmData.size()).isEqualTo(1);
    assertThat(scmData.get(1).revision).isEmpty();
    assertThat(scmData.get(1).author).isEmpty();
    assertThat(scmData.get(1).date).isInSameMinuteWindowAs(new Date());

    // Restore SCM Repository
    unzip(new File(source, "scm-repository.zip"), source.getAbsolutePath());

    // 2nd run
    orchestrator.executeBuild(newScannerWithSCM(source));

    scmData = ws.getScmData(FILE_TO_ANALYSE);
    assertThat(scmData.size()).isEqualTo(1);
    assertThat(scmData.get(1).revision).isEqualTo("6664564f3358c9abf5f595eca31f228b3a95c886");
    assertThat(scmData.get(1).author).isEqualTo("guillaume.jambet@sonarsource.com");
    assertThat(scmData.get(1).date).isEqualToIgnoringMillis("2018-01-18T09:56:01");
  }

  @Test
  public void without_and_then_with_scm_on_modified_file() throws ParseException, IOException {

    File source = disposableWorkspaceFor(PROJECT_DIRECTORY);

    // First run
    orchestrator.executeBuild(newScannerWithoutSCM(source));

    Map<Integer, LineData> scmData = ws.getScmData(FILE_TO_ANALYSE);
    assertThat(scmData.size()).isEqualTo(1);
    assertThat(scmData.get(1).revision).isEmpty();
    assertThat(scmData.get(1).author).isEmpty();
    assertThat(scmData.get(1).date).isInSameMinuteWindowAs(new Date());

    // Restore SCM Repository
    unzip(new File(source, "scm-repository.zip"), source.getAbsolutePath());

    // Swap analysed fo to a modified one
    File sample = new File(source, PATH_TO_SAMPLE);
    sample.delete();
    moveFile(new File(source, "src/main/xoo/sample/Sample.xoo.updated"), sample);

    // 2nd run
    orchestrator.executeBuild(newScannerWithSCM(source));

    scmData = ws.getScmData(FILE_TO_ANALYSE);
    assertThat(scmData.get(1).revision).isEmpty();
    assertThat(scmData.get(1).author).isEmpty();
    assertThat(scmData.get(1).date).isEqualTo(scmData.get(1).date);

    assertThat(scmData.get(4).revision).isEmpty();
    assertThat(scmData.get(4).author).isEmpty();
    assertThat(scmData.get(4).date).isInSameMinuteWindowAs(new Date());

  }

  @Test
  public void without_and_then_with_scm_on_modified_file_with_new_lines() throws ParseException, IOException {

    File source = disposableWorkspaceFor(PROJECT_DIRECTORY);

    // First run
    orchestrator.executeBuild(newScannerWithoutSCM(source));

    Map<Integer, LineData> scmData = ws.getScmData(FILE_TO_ANALYSE);
    assertThat(scmData.size()).isEqualTo(1);
    assertThat(scmData.get(1).revision).isEmpty();
    assertThat(scmData.get(1).author).isEmpty();
    assertThat(scmData.get(1).date).isInSameMinuteWindowAs(new Date());

    // Restore SCM Repository
    unzip(new File(source, "scm-repository.zip"), source.getAbsolutePath());

    // Swap analysed fo to a modified one
    File sample = new File(source, PATH_TO_SAMPLE);
    sample.delete();
    moveFile(new File(source, "src/main/xoo/sample/Sample.xoo.new"), sample);

    // 2nd run
    orchestrator.executeBuild(newScannerWithSCM(source));

    scmData = ws.getScmData(FILE_TO_ANALYSE);
    assertThat(scmData.get(1).revision).isEmpty();
    assertThat(scmData.get(1).author).isEmpty();
    assertThat(scmData.get(1).date).isEqualToIgnoringMillis("2018-01-17T10:35:23");

    assertThat(scmData.get(5).revision).isEmpty();
    assertThat(scmData.get(5).author).isEmpty();
    assertThat(scmData.get(5).date).isInSameMinuteWindowAs(new Date());

  }

  private SonarScanner newScannerWithoutSCM(File source) {
    return SonarScanner.create(source);
  }

  private SonarScanner newScannerWithSCM(File source) {
    return SonarScanner.create(source)
      .setProperty("sonar.scm.provider", "git")
      .setProperty("sonar.scm.disabled", "false");
  }

  private File disposableWorkspaceFor(String project) throws IOException {
    File origin = projectDir("scm/" + project);
    copyDirectory(origin.getParentFile(), temporaryFolder.getRoot());
    return new File(temporaryFolder.getRoot(), project);
  }

}

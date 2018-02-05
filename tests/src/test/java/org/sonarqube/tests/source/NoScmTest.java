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
import static util.ItUtils.projectDir;

public class NoScmTest {

  private final String PROJECT_DIRECTORY = "xoo-sample-without-scm";
  private final String PROJECT_NAME = "sample-without-scm";
  private final String PATH_TO_SAMPLE = "src/main/xoo/sample/Sample.xoo";
  private final String FILE_TO_ANALYSE = PROJECT_NAME + ":" + PATH_TO_SAMPLE;
  private final String PATH_TO_INACTIVATED_SAMPLE = "src/main/xoo/sample/Sample.xoo.new";

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
  public void two_analysis_without_scm_on_same_file() throws ParseException, IOException {

    File source = disposableWorkspaceFor(PROJECT_DIRECTORY);

    // First run
    SonarScanner scanner = SonarScanner.create(source);

    orchestrator.executeBuild(scanner);
    Map<Integer, LineData> scmData1 = ws.getScmData(FILE_TO_ANALYSE);

    assertThat(scmData1.size()).isEqualTo(1);
    assertThat(scmData1.get(1).revision).isEmpty();
    assertThat(scmData1.get(1).author).isEmpty();
    assertThat(scmData1.get(1).date).isInSameMinuteWindowAs(new Date());

    // 2nd run
    scanner = SonarScanner.create(source);

    orchestrator.executeBuild(scanner);
    Map<Integer, LineData> scmData2 = ws.getScmData(FILE_TO_ANALYSE);

    assertThat(scmData2.size()).isEqualTo(1);
    assertThat(scmData2.get(1).revision).isEmpty();
    assertThat(scmData2.get(1).author).isEmpty();
    assertThat(scmData2.get(1).date).isEqualTo(scmData1.get(1).date);

  }

  @Test
  public void two_analysis_without_scm_on_modified_file() throws ParseException, IOException {

    File source = disposableWorkspaceFor(PROJECT_DIRECTORY);

    // First run
    SonarScanner scanner = SonarScanner.create(source);

    orchestrator.executeBuild(scanner);
    Map<Integer, LineData> scmData = ws.getScmData(FILE_TO_ANALYSE);

    assertThat(scmData.size()).isEqualTo(1);
    assertThat(scmData.get(1).revision).isEmpty();
    assertThat(scmData.get(1).author).isEmpty();
    assertThat(scmData.get(1).date).isInSameMinuteWindowAs(new Date());

    // Swap analysed fo to a modified one
    File sample = new File(source, PATH_TO_SAMPLE);
    sample.delete();
    moveFile(new File(source, PATH_TO_INACTIVATED_SAMPLE), sample);

    // 2nd run
    scanner = SonarScanner.create(source);

    orchestrator.executeBuild(scanner);
    scmData = ws.getScmData(FILE_TO_ANALYSE);

    assertThat(scmData.size()).isEqualTo(3);
    assertThat(scmData.get(1).revision).isEmpty();
    assertThat(scmData.get(1).author).isEmpty();
    assertThat(scmData.get(1).date).isInSameMinuteWindowAs(new Date());

    assertThat(scmData.get(5).revision).isEmpty();
    assertThat(scmData.get(5).author).isEmpty();
    assertThat(scmData.get(5).date).isAfter(scmData.get(1).date);
    
    assertThat(scmData.get(11).revision).isEmpty();
    assertThat(scmData.get(11).author).isEmpty();
    assertThat(scmData.get(11).date).isInSameMinuteWindowAs(new Date());

    tester.openBrowser()
      .openCode("sample-without-scm", "sample-without-scm:src/main/xoo/sample/Sample.xoo")
      .getSourceViewer()
      .shouldHaveNewLines(5, 6, 7, 8, 9, 10)
      .shouldNotHaveNewLines(1, 2, 3, 4, 11, 12, 13);
  }

  private File disposableWorkspaceFor(String project) throws IOException {
    File origin = projectDir("scm/" + project);
    copyDirectory(origin.getParentFile(), temporaryFolder.getRoot());
    return new File(temporaryFolder.getRoot(), project);
  }

}

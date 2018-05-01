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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarqube.ws.client.sources.HashRequest;
import util.ItUtils;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.tests.source.SourceSuite.ORCHESTRATOR;
import static util.ItUtils.projectDir;

public class SignificantCodeTest {
  private final String PROJECT_DIRECTORY = "sample-xoo";
  private final String PROJECT_NAME = "sample-xoo";
  private final String FILE_BASE = "file.xoo";
  private final String FILE_CHANGED = "file_changed.xoo";
  private final String FILE_ADDITIONAL_LINE = "file_additional_line.xoo";
  private final String FILE_TO_ANALYSE = PROJECT_NAME + ":" + FILE_BASE;

  @ClassRule
  public static Orchestrator orchestrator = ORCHESTRATOR;

  private SourceScmWS ws;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void setUp() {
    ORCHESTRATOR.resetData();
    ws = new SourceScmWS(tester);
  }

  @Test
  public void changes_in_margins_should_not_change_line_date() throws ParseException, IOException {
    Path projectDirectory = disposableWorkspaceFor(PROJECT_DIRECTORY);
    deployFileWithsignificantCodeRanges(projectDirectory, 7);

    // 1st run
    SonarScanner scanner = SonarScanner.create(projectDirectory.toFile())
      .setProjectKey(PROJECT_NAME)
      .setSourceDirs(FILE_BASE);
    orchestrator.executeBuild(scanner);
    Map<Integer, LineData> scmData1 = ws.getScmData(FILE_TO_ANALYSE);

    // 2nd run
    deployChangedFile(projectDirectory, FILE_CHANGED);

    scanner = SonarScanner.create(projectDirectory.toFile())
      .setProjectKey(PROJECT_NAME)
      .setSourceDirs(FILE_BASE);
    orchestrator.executeBuild(scanner);

    // Check that only line 4 is considered as changed
    Map<Integer, LineData> scmData2 = ws.getScmData(FILE_TO_ANALYSE);

    for (Map.Entry<Integer, LineData> e : scmData2.entrySet()) {
      if (e.getKey() == 4) {
        assertThat(e.getValue().date).isAfter(scmData1.get(1).date);
      } else {
        assertThat(e.getValue().date).isEqualTo(scmData1.get(1).date);
      }
    }
  }

  @Test
  public void migration_should_not_affect_unchanged_lines() throws IOException, ParseException {
    Path projectDirectory = disposableWorkspaceFor(PROJECT_DIRECTORY);

    // 1st run
    SonarScanner scanner = SonarScanner.create(projectDirectory.toFile())
      .setProjectKey(PROJECT_NAME)
      .setSourceDirs(FILE_BASE);
    orchestrator.executeBuild(scanner);
    Map<Integer, LineData> scmData1 = ws.getScmData(FILE_TO_ANALYSE);
    String[] lineHashes1 = getLineHashes();

    // 2nd run
    deployFileWithsignificantCodeRanges(projectDirectory, 7);
    scanner = SonarScanner.create(projectDirectory.toFile())
      .setProjectKey(PROJECT_NAME)
      .setSourceDirs(FILE_BASE);
    orchestrator.executeBuild(scanner);

    // Check that no line was modified
    Map<Integer, LineData> scmData2 = ws.getScmData(FILE_TO_ANALYSE);

    for (Map.Entry<Integer, LineData> e : scmData2.entrySet()) {
      assertThat(e.getValue().date).isEqualTo(scmData1.get(1).date);
    }

    // Check that line hashes changed for all lines, even though the file didn't change
    String[] lineHashes2 = getLineHashes();

    for (int i = 0; i < lineHashes2.length; i++) {
      assertThat(lineHashes2[i]).isNotEqualTo(lineHashes1[i]);
    }
  }

  @Test
  public void issue_tracking() throws Exception {
    ORCHESTRATOR.getServer().provisionProject(PROJECT_NAME, PROJECT_NAME);
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/one-xoo-issue-per-line.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_NAME, "xoo", "one-xoo-issue-per-line");

    Path projectDirectory = disposableWorkspaceFor(PROJECT_DIRECTORY);

    // 1st run
    deployFileWithsignificantCodeRanges(projectDirectory, 7);
    SonarScanner scanner = SonarScanner.create(projectDirectory.toFile())
      .setProjectKey(PROJECT_NAME)
      .setSourceDirs(FILE_BASE);
    orchestrator.executeBuild(scanner);

    Map<Integer, Issue> issues1 = getIssues().stream().collect(Collectors.toMap(i -> i.getLine(), i -> i));

    // 2nd run
    deployChangedFile(projectDirectory, FILE_ADDITIONAL_LINE);
    deployFileWithsignificantCodeRanges(projectDirectory, 7);

    scanner = SonarScanner.create(projectDirectory.toFile())
      .setProjectKey(PROJECT_NAME)
      .setSourceDirs(FILE_BASE);
    orchestrator.executeBuild(scanner);

    List<Issue> issues2 = getIssues();

    // Check that all issues were tracking except the issue on the new line
    assertThat(issues1.size()).isEqualTo(11);
    assertThat(issues2.size()).isEqualTo(12);

    for (Issue issue : issues2) {
      if (issue.getLine() < 9) {
        assertThat(issue.getKey()).isEqualTo(issues1.get(issue.getLine()).getKey());
      } else if (issue.getLine() == 9) {
        // this is the new issue
        List<String> keys = issues1.values().stream().map(i -> i.getKey()).collect(Collectors.toList());
        assertThat(issue.getKey()).isNotIn(keys);
      } else {
        assertThat(issue.getKey()).isEqualTo(issues1.get(issue.getLine() - 1).getKey());
      }
    }

  }

  private String[] getLineHashes() {
    String hashes = tester.wsClient().sources().hash(new HashRequest().setKey(FILE_TO_ANALYSE));
    return StringUtils.split(hashes, "\n");
  }

  private List<Issue> getIssues() {
    SearchWsResponse response = tester.wsClient().issues().search(new SearchRequest().setComponentKeys(Collections.singletonList(FILE_TO_ANALYSE)));
    return response.getIssuesList();
  }

  private void deployFileWithsignificantCodeRanges(Path projectBaseDir, int lineWithoutSignificantCode) throws IOException {
    Path file = projectBaseDir.resolve("file.xoo");
    Path significantLineRangesFile = projectBaseDir.resolve("file.xoo.significantCode");

    int numLines = Files.readAllLines(file).size();
    List<String> lines = IntStream.rangeClosed(1, numLines)
      .mapToObj(i -> i == lineWithoutSignificantCode ? "" : i + ",6,72")
      .collect(Collectors.toList());

    Files.write(significantLineRangesFile, lines);
  }

  private void deployChangedFile(Path projectBaseDir, String fileName) throws IOException {
    Path file = projectBaseDir.resolve(FILE_BASE);
    Path file_changed = projectBaseDir.resolve(fileName);
    Files.move(file_changed, file, StandardCopyOption.REPLACE_EXISTING);
  }

  private Path disposableWorkspaceFor(String project) throws IOException {
    File origin = projectDir("significantCode/" + project);
    copyDirectory(origin.getParentFile(), temporaryFolder.getRoot());
    return temporaryFolder.getRoot().toPath().resolve(project);
  }
}

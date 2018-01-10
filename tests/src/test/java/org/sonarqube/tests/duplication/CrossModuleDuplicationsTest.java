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
package org.sonarqube.tests.duplication;

import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.qa.util.Tester;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;

public class CrossModuleDuplicationsTest {
  private static final String PROJECT_KEY = "cross-module";
  private static final String PROJECT_DIR = "duplications/" + PROJECT_KEY;
  private File projectDir;

  @ClassRule
  public static Orchestrator orchestrator = DuplicationSuite.ORCHESTRATOR;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void setUp() throws IOException {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/duplication/xoo-duplication-profile.xml"));

    FileUtils.copyDirectory(ItUtils.projectDir(PROJECT_DIR), temp.getRoot());
    projectDir = temp.getRoot();
  }

  @Test
  public void testDuplications() {
    analyzeProject(projectDir, PROJECT_KEY, true);
    verifyDuplicationMeasures(PROJECT_KEY, 2, 54, 2, 56.3);

    // File1 is the one duplicated in both modules
    verifyDuplicationMeasures(PROJECT_KEY + ":module1:src/main/xoo/sample/File1.xoo", 1, 27, 1, 75);
    verifyDuplicationMeasures(PROJECT_KEY + ":module2:src/main/xoo/sample/File1.xoo", 1, 27, 1, 75);
  }

  @Test
  // SONAR-6184
  public void testGhostDuplication() throws IOException {
    analyzeProject(projectDir, PROJECT_KEY, true);

    verifyDuplicationMeasures(PROJECT_KEY + ":module1", 1, 27, 1, 45);
    verifyDuplicationMeasures(PROJECT_KEY + ":module2", 1, 27, 1, 75);

    // move File2 from module1 to module2
    File src = FileUtils.getFile(projectDir, "module1", "src", "main", "xoo", "sample", "File2.xoo");
    File dst = FileUtils.getFile(projectDir, "module2", "src", "main", "xoo", "sample", "File2.xoo");
    FileUtils.moveFile(src, dst);

    src = new File(src.getParentFile(), "File2.xoo.measures");
    dst = new File(dst.getParentFile(), "File2.xoo.measures");
    FileUtils.moveFile(src, dst);

    // duplication should remain unchanged (except for % of duplication)
    analyzeProject(projectDir, PROJECT_KEY, false);
    verifyDuplicationMeasures(PROJECT_KEY + ":module1", 1, 27, 1, 75);
    verifyDuplicationMeasures(PROJECT_KEY + ":module2", 1, 27, 1, 45);
  }

  @Test
  // SONAR-6184
  public void testDuplicationFix() {
    analyzeProject(projectDir, PROJECT_KEY, true);

    verifyDuplicationMeasures(PROJECT_KEY + ":module1", 1, 27, 1, 45);
    verifyDuplicationMeasures(PROJECT_KEY + ":module2", 1, 27, 1, 75);

    // remove File1 from module1
    File f1 = FileUtils.getFile(projectDir, "module1", "src", "main", "xoo", "sample", "File1.xoo");
    File f1m = FileUtils.getFile(projectDir, "module2", "src", "main", "xoo", "sample", "File1.xoo.measures");
    f1.delete();
    f1m.delete();

    // duplication should be 0
    analyzeProject(projectDir, PROJECT_KEY, false);
    verifyDuplicationMeasures(PROJECT_KEY + ":module1", 0, 0, 0, 0);
    verifyDuplicationMeasures(PROJECT_KEY + ":module2", 0, 0, 0, 0);
  }

  private static void analyzeProject(File projectDir, String projectKey, boolean create, String... additionalProperties) {
    if (create) {
      orchestrator.getServer().provisionProject(projectKey, projectKey);
      orchestrator.getServer().associateProjectToQualityProfile(projectKey, "xoo", "xoo-duplication-profile");
    }

    SonarScanner sonarRunner = SonarScanner.create(projectDir);
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

    for (int i = 0; i < additionalProperties.length; i += 2) {
      builder.put(additionalProperties[i], additionalProperties[i + 1]);
    }
    SonarScanner scan = sonarRunner.setDebugLogs(true).setProperties(builder.build());
    orchestrator.executeBuild(scan); }

  private static void verifyDuplicationMeasures(String componentKey, int duplicatedBlocks, int duplicatedLines, int duplicatedFiles, double duplicatedLinesDensity) {
    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, componentKey, "duplicated_lines", "duplicated_blocks", "duplicated_files", "duplicated_lines_density");
    assertThat(measures.get("duplicated_blocks").intValue()).isEqualTo(duplicatedBlocks);
    assertThat(measures.get("duplicated_lines").intValue()).isEqualTo(duplicatedLines);
    assertThat(measures.get("duplicated_files").intValue()).isEqualTo(duplicatedFiles);
    assertThat(measures.get("duplicated_lines_density")).isEqualTo(duplicatedLinesDensity);
  }
}

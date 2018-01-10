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
package org.sonarqube.tests.performance.server;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.tests.performance.AbstractPerfTest;
import org.sonarqube.tests.performance.ServerLogs;

public class ComputeEnginePerfTest extends AbstractPerfTest {
  private static int MAX_HEAP_SIZE_IN_MEGA = 600;

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator
    .builderEnv()
    .addPlugin(FileLocation.byWildcardMavenFilename(new File("../plugins/sonar-xoo-plugin/target"), "sonar-xoo-plugin-*.jar"))
    .setServerProperty(
      "sonar.web.javaOpts",
      String.format("-Xms%dm -Xmx%dm -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true", MAX_HEAP_SIZE_IN_MEGA, MAX_HEAP_SIZE_IN_MEGA))
    .restoreProfileAtStartup(FileLocation.ofClasspath("/one-xoo-issue-per-line.xml"))
    .build();

  private static File bigProjectBaseDir;

  @BeforeClass
  public static void classSetUp() throws IOException {
    bigProjectBaseDir = createProject(4, 10, 20);
  }

  @Before
  public void before() {
    orchestrator.resetData();
  }

  @Test
  public void analyse_big_project() throws Exception {
    SonarScanner scanner = SonarScanner.create()
      .setProperties(
        "sonar.projectKey", "big-project",
        "sonar.projectName", "Big Project",
        "sonar.projectVersion", "1.0",
        "sonar.sources", "src",
        "sonar.profile", "one-xoo-issue-per-line")
      .setProjectDir(bigProjectBaseDir);

    orchestrator.executeBuild(scanner);

    assertComputationDurationAround(378_000L);
  }

  private void assertComputationDurationAround(long expectedDuration) throws IOException {
    Long duration = ServerLogs.extractComputationTotalTime(orchestrator);

    assertDurationAround(duration, expectedDuration);
  }

  private static File createProject(int dirDepth, int nbDirByLayer, int nbIssuesByFile) throws IOException {
    File rootDir = temp.newFolder();
    File projectProperties = new File(rootDir, "sonar-project.properties");

    StringBuilder moduleListBuilder = new StringBuilder(nbDirByLayer * ("module".length() + 2));

    for (int i = 1; i <= nbDirByLayer; i++) {
      moduleListBuilder.append("module").append(i);
      File moduleDir = new File(rootDir, "module" + i + "/src");
      moduleDir.mkdirs();
      if (i != nbDirByLayer) {
        moduleListBuilder.append(",");
      }

      createProjectFiles(moduleDir, dirDepth - 1, nbDirByLayer, nbIssuesByFile);
    }

    FileUtils.write(projectProperties, "sonar.modules=", true);
    FileUtils.write(projectProperties, moduleListBuilder.toString(), true);
    FileUtils.write(projectProperties, "\n", true);
    FileUtils.write(projectProperties, "sonar.source=src", true);

    return rootDir;
  }

  private static void createProjectFiles(File dir, int depth, int nbFilesByDir, int nbIssuesByFile) throws IOException {
    dir.mkdir();
    for (int i = 1; i <= nbFilesByDir; i++) {
      File xooFile = new File(dir, "file" + i + ".xoo");
      String line = xooFile.getAbsolutePath() + i + "\n";
      FileUtils.write(xooFile, StringUtils.repeat(line, nbIssuesByFile));
      File xooMeasureFile = new File(dir, "file" + i + ".xoo.measures");
      FileUtils.write(xooMeasureFile, "lines:" + nbIssuesByFile);
      if (depth > 1) {
        createProjectFiles(new File(dir, "dir" + i), depth - 1, nbFilesByDir, nbIssuesByFile);
      }
    }
  }
}

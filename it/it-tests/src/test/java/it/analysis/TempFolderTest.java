/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category3Suite;
import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class TempFolderTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void deleteData() {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/analysis/TempFolderTest/one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("sample", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
  }

  // SONAR-4748
  @Test
  public void should_create_in_temp_folder() throws IOException {
    File projectDir = ItUtils.projectDir("shared/xoo-sample");
    BuildResult result = scan();

    assertThat(result.getLogs()).doesNotContain("Creating temp directory:");
    assertThat(result.getLogs()).doesNotContain("Creating temp file:");

    result = scan("sonar.createTempFiles", "true");
    assertThat(result.getLogs()).contains(
      "Creating temp directory: " + projectDir.getCanonicalPath() + File.separator + ".sonar" + File.separator + ".sonartmp" + File.separator + "sonar-it");
    assertThat(result.getLogs()).contains(
      "Creating temp file: " + projectDir.getCanonicalPath() + File.separator + ".sonar" + File.separator + ".sonartmp" + File.separator + "sonar-it");

    // Verify temp folder is deleted after analysis
    assertThat(new File(projectDir, ".sonar/.sonartmp/sonar-it")).doesNotExist();
  }

  // SONAR-4748
  @Test
  public void should_not_use_system_tmp_dir() throws Exception {
    String oldTmp = System.getProperty("java.io.tmpdir");
    try {
      File tmp = temp.newFolder();
      assertThat(tmp.list()).isEmpty();

      SonarScanner runner = configureScanner()
        .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Djava.io.tmpdir=" + tmp.getAbsolutePath());
      orchestrator.executeBuild(runner);

      // TODO There is one remaining file waiting for SONARPLUGINS-3185
      assertThat(tmp.list()).hasSize(1);
      assertThat(tmp.list()[0]).matches("sonar-runner-batch(.*).jar");
    } finally {
      System.setProperty("java.io.tmpdir", oldTmp);
    }
  }

  private BuildResult scan(String... props) {
    SonarScanner runner = configureScanner(props);
    return orchestrator.executeBuild(runner);
  }

  private SonarScanner configureScanner(String... props) {
    return SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperties(props);
  }

}

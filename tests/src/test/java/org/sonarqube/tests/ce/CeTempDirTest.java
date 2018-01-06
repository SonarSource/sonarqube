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
package org.sonarqube.tests.ce;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category4Suite;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class CeTempDirTest {
  @Rule
  public Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Test
  public void temp_files_are_deleted_when_processing_analysis_report() {
    File ceTempDir = new File(orchestrator.getServer().getHome(), "temp/ce");
    assertThatDirIsEmpty(ceTempDir);

    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    assertThat(ceTempDir).isDirectory().exists();
    assertThatDirIsEmpty(ceTempDir);
  }

  private void assertThatDirIsEmpty(File dir) {
    if (dir.exists()) {
      assertThat(FileUtils.listFiles(dir, null, true).size()).isEqualTo(0);
    }
  }
}

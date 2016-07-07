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

import java.io.IOException;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;

import static org.assertj.core.api.Assertions.*;

import it.Category3Suite;
import util.ItUtils;

public class SaveDataTwiceTest {
  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  // SONAR-7783
  @Test
  public void should_create_in_temp_folder() throws IOException {
    BuildResult scan = scan("sonar.it.savedatatwice", "true");
    assertThat(scan.isSuccess()).isFalse();
    assertThat(scan.getLogs()).contains("Trying to save highlighting twice for the same file is not supported");
  }

  private BuildResult scan(String... props) {
    SonarScanner runner = configureScanner(props);
    return orchestrator.executeBuildQuietly(runner);
  }

  private SonarScanner configureScanner(String... props) {
    return SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperties(props);
  }
}

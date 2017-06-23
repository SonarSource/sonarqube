/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.Category4Suite;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static util.ItUtils.getComponent;
import static util.ItUtils.runProjectAnalysis;
import static util.selenium.Selenese.runSelenese;

public class CrossProjectDuplicationsOnRemoveFileTest {

  static final String ORIGIN_PROJECT = "origin-project";
  static final String DUPLICATE_PROJECT = "duplicate-project";
  static final String DUPLICATE_FILE = DUPLICATE_PROJECT + ":src/main/xoo/sample/File1.xoo";

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @BeforeClass
  public static void analyzeProjects() {
    orchestrator.resetData();
    ItUtils.restoreProfile(orchestrator, CrossProjectDuplicationsOnRemoveFileTest.class.getResource("/duplication/xoo-duplication-profile.xml"));

    analyzeProject(ORIGIN_PROJECT, "duplications/cross-project/origin");
    analyzeProject(DUPLICATE_PROJECT, "duplications/cross-project/duplicate");

    // Remove origin project
    orchestrator.getServer().adminWsClient().post("api/projects/bulk_delete", "keys", ORIGIN_PROJECT);
    assertThat(getComponent(orchestrator, ORIGIN_PROJECT)).isNull();
  }

  @Test
  public void duplications_show_ws_does_not_contain_key_of_deleted_file() throws Exception {
    String duplication = orchestrator.getServer().adminWsClient().get("api/duplications/show", "key", DUPLICATE_FILE);

    assertEquals(IOUtils.toString(CrossProjectDuplicationsTest.class.getResourceAsStream(
      "/duplication/CrossProjectDuplicationsOnRemoveFileTest/duplications_on_removed_file-expected.json"), "UTF-8"),
      duplication, false);

    // Only one file should be reference, so the reference 2 on origin-project must not exist
    assertThat(duplication).doesNotContain("\"2\"");
    assertThat(duplication).doesNotContain("origin-project");
  }

  /**
   * SONAR-3277
   */
  @Test
  public void display_message_in_viewer_when_duplications_with_deleted_files_are_found() throws Exception {
    // TODO stas, please replace this IT by a medium test
    runSelenese(orchestrator,
      "/duplication/CrossProjectDuplicationsOnRemoveFileTest/duplications-with-deleted-project.html");
  }

  private static void analyzeProject(String projectKey, String path) {
    orchestrator.getServer().provisionProject(projectKey, projectKey);
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "xoo", "xoo-duplication-profile");

    runProjectAnalysis(orchestrator, path,
      "sonar.cpd.cross_project", "true",
      "sonar.projectKey", projectKey,
      "sonar.projectName", projectKey);
  }

}

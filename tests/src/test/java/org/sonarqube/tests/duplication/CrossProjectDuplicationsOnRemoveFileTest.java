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

import com.sonar.orchestrator.Orchestrator;
import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static util.ItUtils.getComponent;
import static util.ItUtils.runProjectAnalysis;
import static util.selenium.Selenese.runSelenese;

public class CrossProjectDuplicationsOnRemoveFileTest {

  private static final String ORIGIN_PROJECT = "origin-project";
  private static final String DUPLICATE_PROJECT = "duplicate-project";
  private static final String DUPLICATE_FILE = DUPLICATE_PROJECT + ":src/main/xoo/sample/File1.xoo";

  @ClassRule
  public static Orchestrator orchestrator = DuplicationSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void duplications_show_ws_does_not_contain_key_of_deleted_file() throws Exception {
    // analyze projects
    ItUtils.restoreProfile(orchestrator, CrossProjectDuplicationsOnRemoveFileTest.class.getResource("/duplication/xoo-duplication-profile.xml"));
    analyzeProject(ORIGIN_PROJECT, "duplications/cross-project/origin");
    analyzeProject(DUPLICATE_PROJECT, "duplications/cross-project/duplicate");

    // Remove origin project
    tester.wsClient().wsConnector().call(new PostRequest("api/projects/bulk_delete").setParam("keys", ORIGIN_PROJECT));
    assertThat(getComponent(orchestrator, ORIGIN_PROJECT)).isNull();

    // api/duplications/show does not return the deleted file
    String json = tester.wsClient().wsConnector().call(new GetRequest("api/duplications/show").setParam("key", DUPLICATE_FILE)).content();
    assertEquals(IOUtils.toString(CrossProjectDuplicationsTest.class.getResourceAsStream(
      "/duplication/CrossProjectDuplicationsOnRemoveFileTest/duplications_on_removed_file-expected.json"), "UTF-8"),
      json, false);
    // Only one file should be reference, so the reference 2 on origin-project must not exist
    assertThat(json).doesNotContain("\"2\"");
    assertThat(json).doesNotContain("origin-project");

    // SONAR-3277 display message in source viewer when duplications on deleted files are found
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

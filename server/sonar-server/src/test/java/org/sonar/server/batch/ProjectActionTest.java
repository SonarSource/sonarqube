/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.batch;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.scanner.protocol.input.FileData;
import org.sonar.scanner.protocol.input.MultiModuleProjectRepository;
import org.sonar.scanner.protocol.input.ProjectRepositories;
import org.sonar.scanner.protocol.input.SingleProjectRepository;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Batch.WsProjectResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class ProjectActionTest {

  private ProjectDataLoader projectDataLoader = mock(ProjectDataLoader.class);
  private WsActionTester ws = new WsActionTester(new ProjectAction(projectDataLoader));

  @Test
  public void project_referentials() {
    String projectKey = "org.codehaus.sonar:sonar";

    ProjectRepositories projectReferentials = mock(SingleProjectRepository.class);

    ArgumentCaptor<ProjectDataQuery> queryArgumentCaptor = ArgumentCaptor.forClass(ProjectDataQuery.class);
    when(projectDataLoader.load(queryArgumentCaptor.capture())).thenReturn(projectReferentials);

    TestResponse response = ws.newRequest()
      .setParam("key", projectKey)
      .setParam("branch", "my_branch")
      .setParam("profile", "Default")
      .setParam("preview", "false")
      .execute();
    assertJson(response.getInput()).isSimilarTo("{\"fileDataByPath\": {}}");

    assertThat(queryArgumentCaptor.getValue().getProjectKey()).isEqualTo(projectKey);
    assertThat(queryArgumentCaptor.getValue().getProfileName()).isEqualTo("Default");
    assertThat(queryArgumentCaptor.getValue().getBranch()).isEqualTo("my_branch");
  }

  /**
   * SONAR-7084
   */
  @Test
  public void do_not_fail_when_a_path_is_null() {
    String projectKey = "org.codehaus.sonar:sonar";

    ProjectRepositories projectRepositories = new MultiModuleProjectRepository()
      .addFileDataToModule("module-1", null, new FileData(null, null));
    when(projectDataLoader.load(any(ProjectDataQuery.class))).thenReturn(projectRepositories);

    WsProjectResponse wsProjectResponse = ws.newRequest()
      .setParam("key", projectKey)
      .setParam("profile", "Default")
      .executeProtobuf(WsProjectResponse.class);
    assertThat(wsProjectResponse.getFileDataByModuleAndPathMap()).isEmpty();
  }

  @Test
  public void use_new_file_structure_for_projects_without_submodules() {
    String projectKey = "org.codehaus.sonar:sonar";

    ProjectRepositories projectRepositories = new SingleProjectRepository()
      .addFileData("src/main/java/SomeClass.java", new FileData("789456", "123456789"));
    when(projectDataLoader.load(any(ProjectDataQuery.class))).thenReturn(projectRepositories);

    WsProjectResponse wsProjectResponse = ws.newRequest()
      .setParam("key", projectKey)
      .setParam("profile", "Default")
      .executeProtobuf(WsProjectResponse.class);
    assertThat(wsProjectResponse.getFileDataByModuleAndPathMap()).isEmpty();
    assertThat(wsProjectResponse.getFileDataByPathCount()).isEqualTo(1);
    assertThat(wsProjectResponse.getFileDataByPathMap().get("src/main/java/SomeClass.java")).isNotNull();
  }

  @Test
  public void use_old_file_structure_for_projects_with_submodules() {
    String projectKey = "org.codehaus.sonar:sonar";

    ProjectRepositories projectRepositories = new MultiModuleProjectRepository()
      .addFileDataToModule("module-1", "src/main/java/SomeClass.java", new FileData("789456", "123456789"));
    when(projectDataLoader.load(any(ProjectDataQuery.class))).thenReturn(projectRepositories);

    WsProjectResponse wsProjectResponse = ws.newRequest()
      .setParam("key", projectKey)
      .setParam("profile", "Default")
      .executeProtobuf(WsProjectResponse.class);

    assertThat(wsProjectResponse.getFileDataByPathMap()).isEmpty();
    assertThat(wsProjectResponse.getFileDataByModuleAndPathCount()).isEqualTo(1);
    WsProjectResponse.FileDataByPath moduleData = wsProjectResponse.getFileDataByModuleAndPathMap().get("module-1");
    assertThat(moduleData).isNotNull();
    assertThat(moduleData.getFileDataByPathCount()).isEqualTo(1);
    WsProjectResponse.FileData fileData = moduleData.getFileDataByPathMap().get("src/main/java/SomeClass.java");
    assertThat(fileData).isNotNull();
    assertThat(fileData.getHash()).isEqualTo("789456");
    assertThat(fileData.getRevision()).isEqualTo("123456789");
  }
}

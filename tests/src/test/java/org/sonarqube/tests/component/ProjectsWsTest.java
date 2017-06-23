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
package org.sonarqube.tests.component;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category4Suite;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsProjects.BulkUpdateKeyWsResponse;
import org.sonarqube.ws.WsProjects.BulkUpdateKeyWsResponse.Key;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.component.ShowWsRequest;
import org.sonarqube.ws.client.project.BulkUpdateKeyWsRequest;
import org.sonarqube.ws.client.project.UpdateKeyWsRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class ProjectsWsTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;
  private static final String PROJECT_KEY = "sample";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private WsClient wsClient;

  @Before
  public void inspectProject() {
    orchestrator.resetData();
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    wsClient = ItUtils.newAdminWsClient(orchestrator);
  }

  /**
   * SONAR-3105
   */
  @Test
  public void projects_web_service() throws IOException {
    SonarScanner build = SonarScanner.create(projectDir("shared/xoo-sample"));
    orchestrator.executeBuild(build);

    String url = orchestrator.getServer().getUrl() + "/api/projects/index?key=sample&versions=true";
    HttpClient httpclient = new DefaultHttpClient();
    try {
      HttpGet get = new HttpGet(url);
      HttpResponse response = httpclient.execute(get);

      assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
      String content = IOUtils.toString(response.getEntity().getContent());
      assertThat(content).doesNotContain("error");
      assertThat(content).contains("sample");
      EntityUtils.consume(response.getEntity());

    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }

  @Test
  public void update_key() {
    String newProjectKey = "another_project_key";
    WsComponents.Component project = wsClient.components().show(new ShowWsRequest().setKey(PROJECT_KEY)).getComponent();
    assertThat(project.getKey()).isEqualTo(PROJECT_KEY);

    wsClient.projects().updateKey(UpdateKeyWsRequest.builder()
      .setKey(PROJECT_KEY)
      .setNewKey(newProjectKey)
      .build());

    assertThat(wsClient.components().show(new ShowWsRequest().setId(project.getId())).getComponent().getKey()).isEqualTo(newProjectKey);
  }

  @Test
  public void bulk_update_key() {
    String newProjectKey = "another_project_key";
    WsComponents.Component project = wsClient.components().show(new ShowWsRequest().setKey(PROJECT_KEY)).getComponent();
    assertThat(project.getKey()).isEqualTo(PROJECT_KEY);

    BulkUpdateKeyWsResponse result = wsClient.projects().bulkUpdateKey(BulkUpdateKeyWsRequest.builder()
      .setKey(PROJECT_KEY)
      .setFrom(PROJECT_KEY)
      .setTo(newProjectKey)
      .build());

    assertThat(wsClient.components().show(new ShowWsRequest().setId(project.getId())).getComponent().getKey()).isEqualTo(newProjectKey);
    assertThat(result.getKeysCount()).isEqualTo(1);
    assertThat(result.getKeys(0))
      .extracting(Key::getKey, Key::getNewKey, Key::getDuplicate)
      .containsOnlyOnce(PROJECT_KEY, newProjectKey, false);
  }
}

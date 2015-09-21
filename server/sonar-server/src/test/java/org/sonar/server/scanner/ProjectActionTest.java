/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.scanner;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectActionTest {

  ProjectDataLoader projectDataLoader = mock(ProjectDataLoader.class);

  WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new ScannerWs(mock(ScannerIndex.class), new ProjectAction(projectDataLoader)));
  }

  @Test
  public void project_referentials() throws Exception {
    String projectKey = "org.codehaus.sonar:sonar";

    ProjectRepositories projectReferentials = mock(ProjectRepositories.class);
    when(projectReferentials.toJson()).thenReturn("{\"settingsByModule\": {}}");

    ArgumentCaptor<ProjectDataQuery> queryArgumentCaptor = ArgumentCaptor.forClass(ProjectDataQuery.class);
    when(projectDataLoader.load(queryArgumentCaptor.capture())).thenReturn(projectReferentials);

    WsTester.TestRequest request = tester.newGetRequest("scanner", "project")
      .setParam("key", projectKey)
      .setParam("profile", "Default")
      .setParam("preview", "false");
    request.execute().assertJson("{\"settingsByModule\": {}}");

    assertThat(queryArgumentCaptor.getValue().getModuleKey()).isEqualTo(projectKey);
    assertThat(queryArgumentCaptor.getValue().getProfileName()).isEqualTo("Default");
    assertThat(queryArgumentCaptor.getValue().isIssuesMode()).isFalse();
  }

}

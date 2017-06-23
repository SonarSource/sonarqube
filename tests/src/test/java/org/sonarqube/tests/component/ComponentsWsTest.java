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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.component.SearchWsRequest;
import org.sonarqube.ws.client.component.ShowWsRequest;
import util.ItUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class ComponentsWsTest {
  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;
  private static final String FILE_KEY = "sample:src/main/xoo/sample/Sample.xoo";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  WsClient wsClient;

  @Before
  public void inspectProject() {
    orchestrator.resetData();
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    wsClient = ItUtils.newAdminWsClient(orchestrator);
  }

  @Test
  public void show() {
    WsComponents.ShowWsResponse response = wsClient.components().show(new ShowWsRequest().setKey(FILE_KEY));

    assertThat(response).isNotNull();
    assertThat(response.getComponent().getKey()).isEqualTo(FILE_KEY);
    assertThat(response.getAncestorsList()).isNotEmpty();
  }

  @Test
  public void search() {
    WsComponents.SearchWsResponse response = wsClient.components().search(new SearchWsRequest()
      .setQualifiers(singletonList("FIL")));

    assertThat(response).isNotNull();
    assertThat(response.getComponents(0).getKey()).isEqualTo(FILE_KEY);
  }
}

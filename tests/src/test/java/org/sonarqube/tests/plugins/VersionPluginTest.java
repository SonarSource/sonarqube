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
package org.sonarqube.tests.plugins;

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.Category3Suite;
import java.io.IOException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;

public class VersionPluginTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;
  private static WsClient wsClient;

  @BeforeClass
  public static void init_ws_cient() {
    wsClient = newAdminWsClient(orchestrator);
  }

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  @Test
  public void check_functional_version() {
    assertThat(wsClient.wsConnector().call(new GetRequest("api/plugins/installed")).content()).contains("1.0.2 (build 42)");

  }
}

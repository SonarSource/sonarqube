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
package org.sonarqube.tests.projectEvent;

import com.google.common.collect.Lists;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category4Suite;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.services.Event;
import org.sonar.wsclient.services.EventQuery;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.client.WsResponse;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

@Ignore("refactor using wsClient")
public class EventTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Before
  public void setUp() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void old_ws_events_does_not_allow_creating_events_on_modules() {
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-multi-modules-sample"));
    orchestrator.executeBuild(sampleProject);

    WsConnector wsConnector = ItUtils.newAdminWsClient(orchestrator).wsConnector();
    WsResponse response = wsConnector.call(newCreateEventRequest("com.sonarsource.it.samples:multi-modules-sample", "bar"));
    assertThat(response.code())
      .isEqualTo(200);

    assertThat(wsConnector.call(newCreateEventRequest("com.sonarsource.it.samples:multi-modules-sample:module_a", "bar")).code())
      .isEqualTo(400);
  }

  private static PostRequest newCreateEventRequest(String componentKey, String eventName) {
    return new PostRequest("/api/events")
      .setParam("resource", componentKey)
      .setParam("name", eventName)
      .setParam("category", "Foo");
  }

  /**
   * SONAR-3308
   */
  @Test
  public void keep_only_one_event_per_version_in_project_history() throws Exception {
    // first analyse the 1.0-SNAPSHOT version
    executeAnalysis();
    // then analyse the 1.0 version
    executeAnalysis("sonar.projectVersion", "1.0");
    // and do this all over again
    executeAnalysis();
    executeAnalysis("sonar.projectVersion", "1.0");

    // there should be only 1 "0.1-SNAPSHOT" event and only 1 "0.1" event
    List<Event> events = orchestrator.getServer().getWsClient().findAll(new EventQuery().setResourceKey("sample"));
    assertThat(events.size()).isEqualTo(2);
    List<String> eventNames = Lists.newArrayList(events.get(0).getName(), events.get(1).getName());
    assertThat(eventNames).contains("1.0", "1.0-SNAPSHOT");
  }

  private static void executeAnalysis(String... properties) {
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample")).setProperties(properties);
    orchestrator.executeBuild(sampleProject);
  }
}

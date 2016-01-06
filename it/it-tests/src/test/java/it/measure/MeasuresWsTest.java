/*
 * SonarQube Integration Tests :: Tests
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
package it.measure;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import it.Category4Suite;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.ComponentTreeWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.measure.ComponentTreeWsRequest;
import util.ItUtils;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

public class MeasuresWsTest {
  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;
  private static final String FILE_KEY = "sample:src/main/xoo/sample/Sample.xoo";
  WsClient wsClient;

  @BeforeClass
  public static void initPeriods() throws Exception {
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "30");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_version");
  }

  @AfterClass
  public static void resetPeriods() throws Exception {
    ItUtils.resetPeriods(orchestrator);
  }

  @Before
  public void inspectProject() {
    orchestrator.resetData();
    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));

    wsClient = ItUtils.newAdminWsClient(orchestrator);
  }

  @Test
  public void component_tree() {
    ComponentTreeWsResponse response = wsClient.measures().componentTree(new ComponentTreeWsRequest()
      .setBaseComponentKey("sample")
      .setMetricKeys(singletonList("ncloc"))
      .setAdditionalFields(newArrayList("metrics", "periods")));

    assertThat(response).isNotNull();
    assertThat(response.getMetrics().getMetricsList()).extracting("key").containsOnly("ncloc");
    List<WsMeasures.Component> components = response.getComponentsList();
    assertThat(components).hasSize(2).extracting("key").containsOnly("sample:src/main/xoo/sample", FILE_KEY);
    assertThat(components.get(0).getMeasuresList().get(0).getValue()).isEqualTo("13");
  }
}

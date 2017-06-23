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
package org.sonarqube.tests.measure;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.ComponentTreeWsResponse;
import org.sonarqube.ws.WsMeasures.ComponentWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.measure.ComponentTreeWsRequest;
import org.sonarqube.ws.client.measure.ComponentWsRequest;
import util.ItUtils;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

public class MeasuresWsTest {
  @ClassRule
  public static final Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  private static final String FILE_KEY = "sample:src/main/xoo/sample/Sample.xoo";
  private static final String DIR_KEY = "sample:src/main/xoo/sample";
  WsClient wsClient;

  @BeforeClass
  public static void initPeriod() throws Exception {
    setServerProperty(orchestrator, "sonar.leak.period", "previous_analysis");
  }

  @AfterClass
  public static void resetPeriod() throws Exception {
    ItUtils.resetPeriod(orchestrator);
  }

  @Before
  public void inspectProject() {
    orchestrator.resetData();

    wsClient = ItUtils.newAdminWsClient(orchestrator);
  }

  @Test
  public void component_tree() {
    scanXooSample();

    ComponentTreeWsResponse response = wsClient.measures().componentTree(new ComponentTreeWsRequest()
      .setBaseComponentKey("sample")
      .setMetricKeys(singletonList("ncloc"))
      .setAdditionalFields(newArrayList("metrics", "periods")));

    assertThat(response).isNotNull();
    assertThat(response.getBaseComponent().getKey()).isEqualTo("sample");
    assertThat(response.getMetrics().getMetricsList()).extracting("key").containsOnly("ncloc");
    List<WsMeasures.Component> components = response.getComponentsList();
    assertThat(components).hasSize(2).extracting("key").containsOnly(DIR_KEY, FILE_KEY);
    assertThat(components.get(0).getMeasuresList().get(0).getValue()).isEqualTo("13");
  }

  /**
   * @see SONAR-7958
   */
  @Test
  public void component_tree_supports_module_move_down() {
    String projectKey = "sample";
    String newModuleKey = "sample:new_module";
    String moduleAKey = "module_a";
    String dirKey = "module_a:src/main/xoo/sample";
    String fileKey = "module_a:src/main/xoo/sample/Sample.xoo";

    scanXooSampleModuleMoveV1();

    verifyComponentTreeWithChildren(projectKey, moduleAKey);
    verifyComponentTreeWithChildren(moduleAKey, dirKey);
    verifyComponentTreeWithChildren(dirKey, fileKey);

    scanXooSampleModuleMoveV2();

    verifyComponentTreeWithChildren(projectKey, newModuleKey);
    verifyComponentTreeWithChildren(newModuleKey, moduleAKey);
    verifyComponentTreeWithChildren(moduleAKey, dirKey);
    verifyComponentTreeWithChildren(dirKey, fileKey);
  }

  /**
   * @see SONAR-7958
   */
  @Test
  public void component_tree_supports_module_move_up() {
    String projectKey = "sample";
    String newModuleKey = "sample:new_module";
    String moduleAKey = "module_a";
    String dirKey = "module_a:src/main/xoo/sample";
    String fileKey = "module_a:src/main/xoo/sample/Sample.xoo";

    scanXooSampleModuleMoveV2();

    verifyComponentTreeWithChildren(projectKey, newModuleKey);
    verifyComponentTreeWithChildren(newModuleKey, moduleAKey);
    verifyComponentTreeWithChildren(moduleAKey, dirKey);
    verifyComponentTreeWithChildren(dirKey, fileKey);

    scanXooSampleModuleMoveV1();

    verifyComponentTreeWithChildren(projectKey, moduleAKey);
    verifyComponentTreeWithChildren(moduleAKey, dirKey);
    verifyComponentTreeWithChildren(dirKey, fileKey);
  }

  private void verifyComponentTreeWithChildren(String baseComponentKey, String... childKeys) {
    ComponentTreeWsResponse response = wsClient.measures().componentTree(new ComponentTreeWsRequest()
      .setBaseComponentKey(baseComponentKey)
      .setMetricKeys(singletonList("ncloc"))
      .setStrategy("children"));

    assertThat(response.getBaseComponent().getKey()).isEqualTo(baseComponentKey);
    assertThat(response.getComponentsList())
      .extracting("key").containsOnly(childKeys);
  }

  @Test
  public void component() {
    scanXooSample();

    ComponentWsResponse response = wsClient.measures().component(new ComponentWsRequest()
      .setComponentKey("sample")
      .setMetricKeys(singletonList("ncloc"))
      .setAdditionalFields(newArrayList("metrics", "periods")));

    WsMeasures.Component component = response.getComponent();
    assertThat(component.getKey()).isEqualTo("sample");
    assertThat(component.getMeasuresList()).isNotEmpty();
    assertThat(response.getMetrics().getMetricsList()).extracting("key").containsOnly("ncloc");
  }

  private void scanXooSample() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));
  }

  private void scanXooSampleModuleMoveV1() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample-module-move-v1")));
  }

  private void scanXooSampleModuleMoveV2() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample-module-move-v2")));
  }
}

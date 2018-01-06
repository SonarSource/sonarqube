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
package org.sonarqube.tests.measure;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.ComponentTreeWsResponse;
import org.sonarqube.ws.Measures.ComponentWsResponse;
import org.sonarqube.ws.client.measures.ComponentTreeRequest;
import org.sonarqube.ws.client.measures.ComponentRequest;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class MeasuresWsTest {

  private static final String FILE_KEY = "sample:src/main/xoo/sample/Sample.xoo";
  private static final String DIR_KEY = "sample:src/main/xoo/sample";

  @ClassRule
  public static final Orchestrator orchestrator = MeasureSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void setUp() {
    tester.settings().setGlobalSettings("sonar.leak.period", "previous_version");
  }

  @Test
  public void component_tree() {
    scanXooSample();

    ComponentTreeWsResponse response = tester.wsClient().measures().componentTree(new ComponentTreeRequest()
      .setComponent("sample")
      .setMetricKeys(singletonList("ncloc"))
      .setAdditionalFields(asList("metrics", "periods")));

    assertThat(response).isNotNull();
    assertThat(response.getBaseComponent().getKey()).isEqualTo("sample");
    assertThat(response.getMetrics().getMetricsList()).extracting("key").containsOnly("ncloc");
    List<Measures.Component> components = response.getComponentsList();
    assertThat(components).hasSize(2).extracting("key").containsOnly(DIR_KEY, FILE_KEY);
    assertThat(components.get(0).getMeasuresList().get(0).getValue()).isEqualTo("13");
  }

  /**
   * SONAR-7958
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
   * SONAR-7958
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
    ComponentTreeWsResponse response = tester.wsClient().measures().componentTree(new ComponentTreeRequest()
      .setComponent(baseComponentKey)
      .setMetricKeys(singletonList("ncloc"))
      .setStrategy("children"));

    assertThat(response.getBaseComponent().getKey()).isEqualTo(baseComponentKey);
    assertThat(response.getComponentsList())
      .extracting("key").containsOnly(childKeys);
  }

  @Test
  public void component() {
    scanXooSample();

    ComponentWsResponse response = tester.wsClient().measures().component(new ComponentRequest()
      .setComponent("sample")
      .setMetricKeys(singletonList("ncloc"))
      .setAdditionalFields(newArrayList("metrics", "periods")));

    Measures.Component component = response.getComponent();
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

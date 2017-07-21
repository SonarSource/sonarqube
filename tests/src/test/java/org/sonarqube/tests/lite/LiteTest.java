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
package org.sonarqube.tests.lite;

import com.sonar.orchestrator.Orchestrator;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.rules.RuleChain;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.client.component.TreeWsRequest;
import org.sonarqube.ws.client.issue.IssuesService;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import org.sonarqube.ws.client.measure.ComponentTreeWsRequest;
import org.sonarqube.ws.client.measure.ComponentWsRequest;
import org.sonarqube.ws.client.measure.MeasuresService;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.xooPlugin;

@Ignore
public class LiteTest {

  private static final String PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-sample";

  private static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setOrchestratorProperty("sonar.web.context", "/sonarqube")
    .addPlugin(xooPlugin())
    .build();

  private static Tester tester = new Tester(orchestrator);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator)
    .around(tester);

  @BeforeClass
  public static void setUp() {
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample");
  }

  @Test
  public void call_issues_ws() {
    // all issues
    IssuesService issuesService = tester.wsClient().issues();
    Issues.SearchWsResponse response = issuesService.search(new SearchWsRequest());
    assertThat(response.getIssuesCount()).isGreaterThan(0);

    // project issues
    response = issuesService.search(new SearchWsRequest().setProjectKeys(singletonList(PROJECT_KEY)));
    assertThat(response.getIssuesCount()).isGreaterThan(0);
  }

  @Test
  public void call_components_ws() {
    // files in project
    WsComponents.TreeWsResponse tree = tester.wsClient().components().tree(new TreeWsRequest()
      .setBaseComponentKey(PROJECT_KEY)
      .setQualifiers(singletonList("FIL")));
    assertThat(tree.getComponentsCount()).isEqualTo(4);
    tree.getComponentsList().forEach(c -> {
      assertThat(c.getQualifier()).isEqualTo("FIL");
      assertThat(c.getName()).endsWith(".xoo");
    });
  }

  @Test
  public void call_measures_ws() {
    // project measures
    MeasuresService measuresService = tester.wsClient().measures();
    WsMeasures.ComponentWsResponse component = measuresService.component(new ComponentWsRequest()
      .setComponentKey(PROJECT_KEY)
      .setMetricKeys(asList("lines", "ncloc", "files")));
    assertThat(component.getComponent().getMeasuresCount()).isEqualTo(3);

    // file measures
    WsMeasures.ComponentTreeWsResponse tree = measuresService.componentTree(new ComponentTreeWsRequest()
      .setBaseComponentKey(PROJECT_KEY)
      .setQualifiers(singletonList("FIL"))
      .setMetricKeys(asList("lines", "ncloc")));
    assertThat(tree.getComponentsCount()).isEqualTo(4);
    tree.getComponentsList().forEach(c -> {
      assertThat(c.getMeasuresList()).extracting(m -> m.getMetric()).containsOnly("lines", "ncloc");
    });
  }
}

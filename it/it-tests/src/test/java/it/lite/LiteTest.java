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
package it.lite;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.component.TreeWsRequest;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import org.sonarqube.ws.client.measure.ComponentTreeWsRequest;
import org.sonarqube.ws.client.measure.ComponentWsRequest;
import pageobjects.Navigation;
import pageobjects.RuleItem;
import pageobjects.RulesPage;
import util.ItUtils;

import static com.codeborne.selenide.Condition.hasText;
import static com.codeborne.selenide.Condition.or;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.xooPlugin;

public class LiteTest {

  private static final String PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-sample";

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setOrchestratorProperty("sonar.web.context", "/sonarqube")
    .addPlugin(xooPlugin())
    .build();

  private static WsClient wsClient;

  @BeforeClass
  public static void setUp() {
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-multi-modules-sample");
    wsClient = ItUtils.newWsClient(ORCHESTRATOR);
  }

  @Test
  public void call_issues_ws() {
    // all issues
    Issues.SearchWsResponse response = wsClient.issues().search(new SearchWsRequest());
    assertThat(response.getIssuesCount()).isGreaterThan(0);

    // project issues
    response = wsClient.issues().search(new SearchWsRequest().setProjectKeys(singletonList(PROJECT_KEY)));
    assertThat(response.getIssuesCount()).isGreaterThan(0);
  }

  @Test
  public void call_components_ws() {
    // files in project
    WsComponents.TreeWsResponse tree = wsClient.components().tree(new TreeWsRequest()
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
    WsMeasures.ComponentWsResponse component = wsClient.measures().component(new ComponentWsRequest()
      .setComponentKey(PROJECT_KEY)
      .setMetricKeys(asList("lines", "ncloc", "files")));
    assertThat(component.getComponent().getMeasuresCount()).isEqualTo(3);

    // file measures
    WsMeasures.ComponentTreeWsResponse tree = wsClient.measures().componentTree(new ComponentTreeWsRequest()
      .setBaseComponentKey(PROJECT_KEY)
      .setQualifiers(singletonList("FIL"))
      .setMetricKeys(asList("lines", "ncloc")));
    assertThat(tree.getComponentsCount()).isEqualTo(4);
    tree.getComponentsList().forEach(c -> {
      assertThat(c.getMeasuresList()).extracting(m -> m.getMetric()).containsOnly("lines", "ncloc");
    });
  }

  @Test
  public void open_page_rules() {
    RulesPage rulesPage = Navigation.get(ORCHESTRATOR)
      .openHomepage()
      .clickOnRules();

    // wait for rules to be displayed
    rulesPage.getRules().shouldHave(CollectionCondition.sizeGreaterThan(0));

    assertThat(rulesPage.getTotal()).isGreaterThan(0);
    for (RuleItem ruleItem : rulesPage.getRulesAsItems()) {
      ruleItem.getTitle().should(Condition.visible);
      ruleItem.getMetadata().should(or("have type", hasText("Bug"), hasText("Code Smell"), hasText("Vulnerability")));
    }
  }
}

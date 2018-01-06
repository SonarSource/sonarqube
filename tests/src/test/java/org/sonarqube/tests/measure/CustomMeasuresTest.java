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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class CustomMeasuresTest {

  private static final String PROJECT_KEY = "sample";

  @ClassRule
  public static Orchestrator orchestrator = MeasureSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void custom_measures_should_be_integrated_during_project_analysis() {
    analyzeProject();
    setBurnedBudget(1200.3);
    setTeamSize(4);

    assertThat(getMeasureAsDouble("team_size")).isNull();
    assertThat(getMeasureAsDouble("burned_budget")).isNull();

    analyzeProject();

    assertThat(getMeasureAsDouble("burned_budget")).isEqualTo(1200.3);
    assertThat(getMeasureAsDouble("team_size")).isEqualTo(4d);
  }

  @Test
  public void should_update_value() {
    analyzeProject();
    setTeamSize(4);
    analyzeProject();
    updateTeamSize(15);
    assertThat(getMeasureAsDouble("team_size")).isEqualTo(4d);
    analyzeProject();// the value is available when the project is analyzed again
    assertThat(getMeasureAsDouble("team_size")).isEqualTo(15d);
  }

  @Test
  public void should_delete_custom_measure() {
    analyzeProject();
    setTeamSize(4);
    analyzeProject();
    deleteCustomMeasure("team_size");
    assertThat(getMeasureAsDouble("team_size")).isEqualTo(4d);// the value is still available. It will be removed during next
    // analyzed

    analyzeProject();
    assertThat(getMeasureAsDouble("team_size")).isNull();
  }

  private void analyzeProject() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));
  }

  private void setTeamSize(int i) {
    PostRequest postRequest = new PostRequest("api/custom_measures/create")
      .setParam("projectKey", PROJECT_KEY)
      .setParam("metricKey", "team_size")
      .setParam("value", String.valueOf(i));
    tester.wsClient().wsConnector().call(postRequest);
  }

  private void updateTeamSize(int i) {
    GetRequest getRequest = new GetRequest("api/custom_measures/search")
      .setParam("projectKey", PROJECT_KEY)
      .setParam("metricKey", "team_size");
    String response = tester.wsClient().wsConnector().call(getRequest).content();
    Matcher jsonObjectMatcher = Pattern.compile(".*?\"id\"\\s*:\\s*\"(.*?)\".*", Pattern.MULTILINE).matcher(response);
    jsonObjectMatcher.find();
    String customMeasureId = jsonObjectMatcher.group(1);
    PostRequest postRequest = new PostRequest("api/custom_measures/update")
      .setParam("id", customMeasureId)
      .setParam("value", String.valueOf(i));
    tester.wsClient().wsConnector().call(postRequest);
  }

  private void setBurnedBudget(double d) {
    PostRequest postRequest = new PostRequest("api/custom_measures/create")
      .setParam("projectKey", PROJECT_KEY)
      .setParam("metricKey", "burned_budget")
      .setParam("value", String.valueOf(d));
    tester.wsClient().wsConnector().call(postRequest);
  }

  private void deleteCustomMeasure(String metricKey) {
    GetRequest getRequest = new GetRequest("api/custom_measures/search")
      .setParam("projectKey", PROJECT_KEY)
      .setParam("metricKey", metricKey);
    String response = tester.wsClient().wsConnector().call(getRequest).content();
    Matcher jsonObjectMatcher = Pattern.compile(".*?\"id\"\\s*:\\s*\"(.*?)\".*", Pattern.MULTILINE).matcher(response);
    jsonObjectMatcher.find();
    String customMeasureId = jsonObjectMatcher.group(1);

    PostRequest postRequest = new PostRequest("api/custom_measures/delete")
      .setParam("id", customMeasureId);
    tester.wsClient().wsConnector().call(postRequest);
  }

  private Double getMeasureAsDouble(String metricKey) {
    return ItUtils.getMeasureAsDouble(orchestrator, PROJECT_KEY, metricKey);
  }
}

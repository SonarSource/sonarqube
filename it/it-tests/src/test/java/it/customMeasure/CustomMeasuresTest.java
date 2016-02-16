/*
 * SonarQube
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
package it.customMeasure;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import it.Category1Suite;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.QaOnly;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

@Category(QaOnly.class)
public class CustomMeasuresTest {

  public static final String PROJECT_KEY = "sample";
  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Before
  public void deleteProjects() {
    orchestrator.resetData();
  }

  @Test
  public void custom_measures_should_be_integrated_during_project_analysis() {
    analyzeProject();
    setBurnedBudget(1200.3);
    setTeamSize(4);

    assertThat(getMeasure("team_size")).isNull();
    assertThat(getMeasure("burned_budget")).isNull();

    analyzeProject();

    assertThat(getMeasure("burned_budget").getValue()).isEqualTo(1200.3);
    assertThat(getMeasure("team_size").getIntValue()).isEqualTo(4);
  }

  @Test
  public void should_update_value() {
    analyzeProject();
    setTeamSize(4);
    analyzeProject();
    updateTeamSize(15);
    assertThat(getMeasure("team_size").getIntValue()).isEqualTo(4);
    analyzeProject();// the value is available when the project is analyzed again
    assertThat(getMeasure("team_size").getIntValue()).isEqualTo(15);
  }

  @Test
  public void should_delete_custom_measure() {
    analyzeProject();
    setTeamSize(4);
    analyzeProject();
    deleteCustomMeasure("team_size");
    assertThat(getMeasure("team_size").getIntValue()).isEqualTo(4);// the value is still available. It will be removed during next analyzed

    analyzeProject();
    assertThat(getMeasure("team_size")).isNull();
  }

  private void analyzeProject() {
    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));
  }

  private void setTeamSize(int i) {
    orchestrator.getServer().adminWsClient().post("api/custom_measures/create", "projectKey", PROJECT_KEY, "metricKey", "team_size", "value", String.valueOf(i));
  }

  private void updateTeamSize(int i) {
    String response = orchestrator.getServer().adminWsClient().get("api/custom_measures/search", "projectKey", PROJECT_KEY, "metricKey", "team_size");
    Matcher jsonObjectMatcher = Pattern.compile(".*?\"id\"\\s*:\\s*\"(.*?)\".*", Pattern.MULTILINE).matcher(response);
    jsonObjectMatcher.find();
    String customMeasureId = jsonObjectMatcher.group(1);
    orchestrator.getServer().adminWsClient().post("api/custom_measures/update", "id", customMeasureId, "value", String.valueOf(i));
  }

  private void setBurnedBudget(double d) {
    orchestrator.getServer().adminWsClient().post("api/custom_measures/create", "projectKey", PROJECT_KEY, "metricKey", "burned_budget", "value", String.valueOf(d));
  }

  private void deleteCustomMeasure(String metricKey) {
    String response = orchestrator.getServer().adminWsClient().get("api/custom_measures/search", "projectKey", PROJECT_KEY, "metricKey", metricKey);
    Matcher jsonObjectMatcher = Pattern.compile(".*?\"id\"\\s*:\\s*\"(.*?)\".*", Pattern.MULTILINE).matcher(response);
    jsonObjectMatcher.find();
    String customMeasureId = jsonObjectMatcher.group(1);
    orchestrator.getServer().adminWsClient().post("api/custom_measures/delete", "id", customMeasureId);
  }

  private Measure getMeasure(String metricKey) {
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_KEY, metricKey));
    return resource != null ? resource.getMeasure(metricKey) : null;
  }
}

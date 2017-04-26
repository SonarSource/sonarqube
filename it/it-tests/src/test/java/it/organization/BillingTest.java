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
package it.organization;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category6Suite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import util.ItUtils;
import util.user.UserRule;

import static it.Category6Suite.enableOrganizationsSupport;
import static java.lang.String.format;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.sonarqube.ws.WsCe.TaskResponse;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newOrganizationKey;
import static util.ItUtils.newProjectKey;
import static util.ItUtils.projectDir;
import static util.ItUtils.resetSettings;
import static util.ItUtils.setServerProperty;

public class BillingTest {
  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static WsClient adminClient;

  @BeforeClass
  public static void setUp() throws Exception {
    adminClient = newAdminWsClient(orchestrator);
    enableOrganizationsSupport();
    resetSettings(orchestrator, "sonar.billing.preventProjectAnalysis");
  }

  @AfterClass
  public static void tearDown() throws Exception {
    resetSettings(orchestrator, "sonar.billing.preventProjectAnalysis");
  }

  @Test
  public void execute_successfully_ce_analysis_on_organization() {
    String organizationKey = createOrganization();
    setServerProperty(orchestrator, "sonar.billing.preventProjectAnalysis", "false");

    String taskUuid = executeAnalysis(organizationKey);

    TaskResponse taskResponse = adminClient.ce().task(taskUuid);
    assertThat(taskResponse.getTask().hasErrorMessage()).isFalse();
  }

  @Test
  public void fail_to_execute_ce_analysis_on_organization() {
    String organizationKey = createOrganization();
    setServerProperty(orchestrator, "sonar.billing.preventProjectAnalysis", "true");

    String taskUuid = executeAnalysis(organizationKey);

    TaskResponse taskResponse = adminClient.ce().task(taskUuid);
    assertThat(taskResponse.getTask().hasErrorMessage()).isTrue();
    assertThat(taskResponse.getTask().getErrorMessage()).contains(format("Organization %s cannot perform analysis", organizationKey));
  }

  private static String createOrganization() {
    String key = newOrganizationKey();
    adminClient.organizations().create(new CreateWsRequest.Builder().setKey(key).setName(key).build()).getOrganization();
    return key;
  }

  private static String executeAnalysis(String organizationKey) {
    BuildResult buildResult = orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"),
      "sonar.organization", organizationKey,
      "sonar.projectKey", newProjectKey(),
      "sonar.login", "admin",
      "sonar.password", "admin"));
    return ItUtils.extractCeTaskId(buildResult);
  }
}

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
package org.sonarqube.tests.qualityProfile;

import com.sonar.orchestrator.Orchestrator;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.tests.Byteman;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Qualityprofiles;
import org.sonarqube.ws.client.rules.SearchRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.tests.Byteman.Process.WEB;

public class ActiveRuleEsResilienceTest {
  private static final String RULE_ONE_BUG_PER_LINE = "xoo:OneBugIssuePerLine";

  @ClassRule
  public static final Orchestrator orchestrator;
  private static final Byteman byteman;

  static {
    byteman = new Byteman(Orchestrator.builderEnv(), WEB);
    orchestrator = byteman
      .getOrchestratorBuilder()
      .setServerProperty("sonar.search.recovery.delayInMs", "1000")
      .setServerProperty("sonar.search.recovery.minAgeInMs", "3000")
      .addPlugin(ItUtils.xooPlugin())
      .build();
  }

  @Before
  public void before() throws Exception {
    byteman.activateScript("resilience/active_rule_indexer.btm");
  }

  @After
  public void after() throws Exception {
    byteman.deactivateAllRules();
  }

  @Rule
  public TestRule timeout = new DisableOnDebug(Timeout.builder()
    .withLookingForStuckThread(true)
    .withTimeout(60L, TimeUnit.SECONDS)
    .build());

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void activation_and_deactivation_of_rule_is_resilient_to_indexing_errors() throws Exception {
    Organizations.Organization organization = tester.organizations().generate();
    Qualityprofiles.CreateWsResponse.QualityProfile profile = tester.qProfiles().createXooProfile(organization);

    // step 1. activation
    tester.qProfiles().activateRule(profile.getKey(), RULE_ONE_BUG_PER_LINE);

    assertThat(searchActiveRules(profile)).isEqualTo(0);
    while (searchActiveRules(profile) == 0) {
      // rule is indexed by the recovery daemon, which runs every 3 seconds
      Thread.sleep(500L);
    }
    assertThat(searchActiveRules(profile)).isEqualTo(1);

    // step 2. deactivation
    tester.qProfiles().deactivateRule(profile, RULE_ONE_BUG_PER_LINE);
    while (searchActiveRules(profile) == 1) {
      // rule is indexed by the recovery daemon, which runs every 3 seconds
      Thread.sleep(500L);
    }
    assertThat(searchActiveRules(profile)).isEqualTo(0);
  }

  private long searchActiveRules(Qualityprofiles.CreateWsResponse.QualityProfile profile) {
    SearchRequest request = new SearchRequest().setActivation("true").setQprofile(profile.getKey());
    return tester.wsClient().rules().search(request).getRulesCount();
  }
}

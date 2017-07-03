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
package org.sonarqube.tests.qualityProfile;

import com.sonar.orchestrator.Orchestrator;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.client.rule.SearchWsRequest;
import util.ItUtils;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class ActiveRuleEsResilienceTest {
  private static final String RULE_ONE_BUG_PER_LINE = "xoo:OneBugIssuePerLine";

  @ClassRule
  public static final Orchestrator orchestrator = Orchestrator.builderEnv()
    .setServerProperty("sonar.web.javaAdditionalOpts",
      format("-javaagent:%s=script:%s,boot:%s", findBytemanJar(), findBytemanScript(), findBytemanJar()))
    .setServerProperty("sonar.search.recovery.delayInMs", "500")
    .setServerProperty("sonar.search.recovery.minAgeInMs", "3000")
    .addPlugin(ItUtils.xooPlugin())
    .build();

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
    QualityProfiles.CreateWsResponse.QualityProfile profile = tester.qProfiles().createXooProfile(organization);

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

  private long searchActiveRules(QualityProfiles.CreateWsResponse.QualityProfile profile) {
    SearchWsRequest request = new SearchWsRequest().setActivation(true).setQProfile(profile.getKey());
    return tester.wsClient().rules().search(request).getRulesCount();
  }

  private static String findBytemanJar() {
    // see pom.xml, Maven copies and renames the artifact.
    File jar = new File("target/byteman.jar");
    if (!jar.exists()) {
      throw new IllegalStateException("Can't find " + jar + ". Please execute 'mvn generate-test-resources' on integration tests once.");
    }
    return jar.getAbsolutePath();
  }

  private static String findBytemanScript() {
    // see pom.xml, Maven copies and renames the artifact.
    File script = new File("resilience/active_rule_indexer.btm");
    if (!script.exists()) {
      throw new IllegalStateException("Can't find " + script);
    }
    return script.getAbsolutePath();
  }
}

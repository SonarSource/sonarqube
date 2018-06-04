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
import java.io.File;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.qualityprofiles.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static util.ItUtils.newOrchestratorBuilder;
import static util.ItUtils.pluginArtifact;

public class QualityProfileUpdateTest {
  private static Orchestrator orchestrator;
  private static Tester tester;

  @After
  public void tearDown() {
    if (tester != null) {
      tester.after();
    }
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  // SONAR-10363
  public void updating_an_analyzer_must_update_default_quality_profile() {
    orchestrator = newOrchestratorBuilder()
      .addPlugin(pluginArtifact("foo-plugin-v1"))
//      .setServerProperty("sonar.sonarcloud.enabled", "true")
      .build();
    orchestrator.start();
    tester = new Tester(orchestrator);
    tester.before();

    SearchWsResponse result = tester.qProfiles().service().search(new SearchRequest());
    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getName, QualityProfile::getLanguage, QualityProfile::getIsBuiltIn, QualityProfile::getIsDefault)
      .containsExactlyInAnyOrder(
        tuple("Basic", "foo", true, true));

    assertThat(getRulesForProfile(result.getProfilesList(), "Basic"))
      .extracting(Rules.Rule::getKey)
      .containsOnly("foo:UnchangedRule",
        "foo:ChangedRule",
        "foo:ToBeDeactivatedRule",
        "foo:ToBeRemovedRule",
        "foo:RuleWithUnchangedParameter",
        "foo:RuleWithChangedParameter",
        "foo:RuleWithRemovedParameter",
        "foo:RuleWithAddedParameter",
        "foo:ToBeRenamed",
        "foo:ToBeRenamedAndMoved");

    tester.wsClient().wsConnector().call(new PostRequest("api/plugins/uninstall").setParam("key", "foo")).failIfNotSuccessful();

    File pluginsDir = new File(orchestrator.getServer().getHome() + "/extensions/plugins");
    orchestrator.getConfiguration().locators().copyToDirectory(pluginArtifact("foo-plugin-v3"), pluginsDir);
    orchestrator.restartServer();

    result = tester.qProfiles().service().search(new SearchRequest());
    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getName, QualityProfile::getLanguage, QualityProfile::getIsBuiltIn, QualityProfile::getIsDefault)
      .containsExactlyInAnyOrder(
        tuple("New Basic", "foo", true, true),
        tuple("Basic", "foo", true, false));

    assertThat(getRulesForProfile(result.getProfilesList(), "Basic"))
      .isEmpty();

    assertThat(getRulesForProfile(result.getProfilesList(), "New Basic"))
      .extracting(Rules.Rule::getKey)
      .containsOnly("foo2:UnchangedRule");
  }

  private List<Rules.Rule> getRulesForProfile(List<QualityProfile> qualityProfiles, String profileName) {
    return tester.wsClient().rules().search(new org.sonarqube.ws.client.rules.SearchRequest()
      .setQprofile(getProfileKey(qualityProfiles, profileName))
      .setActivation("true"))
      .getRulesList();
  }

  private String getProfileKey(List<QualityProfile> qualityProfiles, String name) {
    return qualityProfiles.stream()
      .filter(qp -> name.equals(qp.getName()))
      .map(QualityProfile::getKey)
      .findFirst()
      .orElseThrow(IllegalStateException::new);
  }
}

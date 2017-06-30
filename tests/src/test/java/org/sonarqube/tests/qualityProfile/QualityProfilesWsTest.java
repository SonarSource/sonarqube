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
import java.util.function.Predicate;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.ShowResponse;
import org.sonarqube.ws.QualityProfiles.ShowResponse.CompareToSonarWay;
import org.sonarqube.ws.QualityProfiles.ShowResponse.QualityProfile;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;
import org.sonarqube.ws.client.qualityprofile.ShowRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityProfilesWsTest {
  private static final String RULE_ONE_BUG_PER_LINE = "xoo:OneBugIssuePerLine";
  private static final String RULE_ONE_ISSUE_PER_LINE = "xoo:OneIssuePerLine";

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void show() {
    Organization org = tester.organizations().generate();
    CreateWsResponse.QualityProfile xooProfile = tester.qProfiles().createXooProfile(org);
    tester.qProfiles().activateRule(xooProfile, RULE_ONE_BUG_PER_LINE);
    tester.qProfiles().activateRule(xooProfile, RULE_ONE_ISSUE_PER_LINE);

    ShowResponse result = tester.qProfiles().service().show(new ShowRequest().setProfile(xooProfile.getKey()));

    assertThat(result.getProfile())
      .extracting(QualityProfile::getName, QualityProfile::getLanguage, QualityProfile::getIsBuiltIn, QualityProfile::getIsDefault,
        QualityProfile::getActiveRuleCount, QualityProfile::getActiveDeprecatedRuleCount)
      .containsExactly(xooProfile.getName(), xooProfile.getLanguage(), false, false, 2L, 0L);
  }

  @Test
  public void show_with_sonar_way_comparison() {
    Organization org = tester.organizations().generate();
    CreateWsResponse.QualityProfile xooProfile = tester.qProfiles().createXooProfile(org);
    tester.qProfiles().activateRule(xooProfile, RULE_ONE_BUG_PER_LINE);
    tester.qProfiles().activateRule(xooProfile, RULE_ONE_ISSUE_PER_LINE);
    SearchWsResponse.QualityProfile sonarWay = getProfile(org, p -> "Sonar way".equals(p.getName()) && "xoo".equals(p.getLanguage()) && p.getIsBuiltIn());

    CompareToSonarWay result = tester.qProfiles().service().show(new ShowRequest()
      .setProfile(xooProfile.getKey())
      .setCompareToSonarWay(true)).getCompareToSonarWay();

    assertThat(result)
      .extracting(CompareToSonarWay::getProfile, CompareToSonarWay::getProfileName, CompareToSonarWay::getMissingRuleCount)
      .containsExactly(sonarWay.getKey(), sonarWay.getName(), 2L);
  }

  @Test
  public void bulk_activate_missing_rules_from_sonar_way_profile() {
    Organization org = tester.organizations().generate();
    CreateWsResponse.QualityProfile xooProfile = tester.qProfiles().createXooProfile(org);
    tester.qProfiles().activateRule(xooProfile, RULE_ONE_BUG_PER_LINE);
    tester.qProfiles().activateRule(xooProfile, RULE_ONE_ISSUE_PER_LINE);
    SearchWsResponse.QualityProfile sonarWay = getProfile(org, p -> "Sonar way".equals(p.getName()) && "xoo".equals(p.getLanguage()) && p.getIsBuiltIn());

    // Bulk activate missing rules from the Sonar way profile
    tester.wsClient().wsConnector().call(new PostRequest("api/qualityprofiles/activate_rules")
      .setParam("targetProfile", xooProfile.getKey())
      .setParam("qprofile", xooProfile.getKey())
      .setParam("activation", "false")
      .setParam("compareToProfile", sonarWay.getKey())).failIfNotSuccessful();

    // Check that the profile has no missing rule from the Sonar way profile
    assertThat(tester.qProfiles().service().show(new ShowRequest()
      .setProfile(xooProfile.getKey())
      .setCompareToSonarWay(true)).getCompareToSonarWay())
        .extracting(CompareToSonarWay::getProfile, CompareToSonarWay::getProfileName, CompareToSonarWay::getMissingRuleCount)
        .containsExactly(sonarWay.getKey(), sonarWay.getName(), 0L);
  }

  private SearchWsResponse.QualityProfile getProfile(Organization organization, Predicate<SearchWsResponse.QualityProfile> filter) {
    return tester.qProfiles().service().search(new SearchWsRequest()
      .setOrganizationKey(organization.getKey())).getProfilesList()
      .stream()
      .filter(filter)
      .findAny().orElseThrow(IllegalStateException::new);
  }
}

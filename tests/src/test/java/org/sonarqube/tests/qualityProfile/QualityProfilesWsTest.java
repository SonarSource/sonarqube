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
import java.util.function.Predicate;
import org.json.JSONException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse;
import org.sonarqube.ws.Qualityprofiles.ShowResponse;
import org.sonarqube.ws.Qualityprofiles.ShowResponse.CompareToSonarWay;
import org.sonarqube.ws.Qualityprofiles.ShowResponse.QualityProfile;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.qualityprofiles.ChangelogRequest;
import org.sonarqube.ws.client.qualityprofiles.SearchRequest;
import org.sonarqube.ws.client.qualityprofiles.ShowRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityProfilesWsTest {
  private static final String RULE_ONE_BUG_PER_LINE = "xoo:OneBugIssuePerLine";
  private static final String RULE_ONE_ISSUE_PER_LINE = "xoo:OneIssuePerLine";

  private static final String EXPECTED_CHANGELOG = "{\"total\":2,\"p\":1,\"ps\":50,\"events\":[" +
    "{\"authorLogin\":\"admin\",\"authorName\":\"Administrator\",\"action\":\"ACTIVATED\",\"ruleKey\":\"xoo:OneIssuePerLine\",\"ruleName\":\"One Issue Per Line\",\"params\":{\"severity\":\"MAJOR\"}}," +
    "{\"authorLogin\":\"admin\",\"authorName\":\"Administrator\",\"action\":\"ACTIVATED\",\"ruleKey\":\"xoo:OneBugIssuePerLine\",\"ruleName\":\"One Bug Issue Per Line\",\"params\":{\"severity\":\"MAJOR\"}}" +
    "]}";
  private static final String EXPECTED_CHANGELOG_EMPTY = "{\"total\":0,\"p\":1,\"ps\":50,\"events\":[]}";

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

    ShowResponse result = tester.qProfiles().service().show(new ShowRequest().setKey(xooProfile.getKey()));

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
      .setKey(xooProfile.getKey())
      .setCompareToSonarWay("true")).getCompareToSonarWay();

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
      .setParam("targetKey", xooProfile.getKey())
      .setParam("qprofile", xooProfile.getKey())
      .setParam("activation", "false")
      .setParam("compareToProfile", sonarWay.getKey())).failIfNotSuccessful();

    // Check that the profile has no missing rule from the Sonar way profile
    assertThat(tester.qProfiles().service().show(new ShowRequest()
      .setKey(xooProfile.getKey())
      .setCompareToSonarWay("true")).getCompareToSonarWay())
      .extracting(CompareToSonarWay::getProfile, CompareToSonarWay::getProfileName, CompareToSonarWay::getMissingRuleCount)
      .containsExactly(sonarWay.getKey(), sonarWay.getName(), 0L);
  }

  @Test
  public void redirect_profiles_export_to_api_qualityprofiles_export() {
    WsResponse response = tester.wsClient().wsConnector().call(new GetRequest("profiles/export?language=xoo&format=XooFakeExporter"));
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.requestUrl()).endsWith("/api/qualityprofiles/export?language=xoo&format=XooFakeExporter");
    assertThat(response.content()).isEqualTo("xoo -> Basic -> 1");

    // Check 'name' parameter is taken into account
    assertThat(tester.wsClient().wsConnector()
      .call(new GetRequest("profiles/export?language=xoo&qualityProfile=empty&format=XooFakeExporter")).content())
      .isEqualTo("xoo -> empty -> 0");
  }

  @Test
  public void changelog() throws JSONException {
    Organization org = tester.organizations().generate();
    CreateWsResponse.QualityProfile profile = tester.qProfiles().createXooProfile(org);

    String changelog = tester.wsClient().qualityprofiles().changelog(new ChangelogRequest()
      .setOrganization(org.getKey())
      .setLanguage(profile.getLanguage())
      .setQualityProfile(profile.getName()));
    JSONAssert.assertEquals(EXPECTED_CHANGELOG_EMPTY, changelog, JSONCompareMode.STRICT);

    tester.qProfiles().activateRule(profile, RULE_ONE_BUG_PER_LINE);
    tester.qProfiles().activateRule(profile, RULE_ONE_ISSUE_PER_LINE);

    String changelog2 = tester.wsClient().qualityprofiles().changelog(new ChangelogRequest()
      .setOrganization(org.getKey())
      .setLanguage(profile.getLanguage())
      .setQualityProfile(profile.getName()));
    JSONAssert.assertEquals(EXPECTED_CHANGELOG, changelog2, JSONCompareMode.LENIENT);

    String changelog3 = tester.wsClient().qualityprofiles().changelog(new ChangelogRequest()
      .setOrganization(org.getKey())
      .setLanguage(profile.getLanguage())
      .setQualityProfile(profile.getName())
      .setSince("2999-12-31T23:59:59+0000"));
    JSONAssert.assertEquals(EXPECTED_CHANGELOG_EMPTY, changelog3, JSONCompareMode.STRICT);
  }

  private SearchWsResponse.QualityProfile getProfile(Organization organization, Predicate<SearchWsResponse.QualityProfile> filter) {
    return tester.qProfiles().service().search(new SearchRequest()
      .setOrganization(organization.getKey())).getProfilesList()
      .stream()
      .filter(filter)
      .findAny().orElseThrow(IllegalStateException::new);
  }
}

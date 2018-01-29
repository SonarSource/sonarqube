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
package org.sonarqube.tests.rule;

import com.sonar.orchestrator.Orchestrator;
import java.util.List;
import java.util.function.Predicate;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.ws.Common.RuleScope;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.ShowResponse;
import org.sonarqube.ws.client.rules.SearchRequest;
import org.sonarqube.ws.client.rules.ShowRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class RulesWsTest {

  private static final String RULE_HAS_TAG = "xoo:HasTag";
  private static final String RULE_ONE_ISSUE_PER_LINE = "xoo:OneIssuePerLine";
  private static final String RULE_ONE_ISSUE_PER_FILE = "xoo:OneIssuePerFile";
  private static final String RULE_ONE_ISSUE_PER_TEST_FILE = "xoo:OneIssuePerTestFile";
  private static final String RULE_ONE_BUG_PER_LINE = "xoo:OneBugIssuePerLine";
  private static final String PROFILE_SONAR_WAY = "Sonar way";
  private static final String LANGUAGE_XOO = "xoo";

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void search_activated_rules() {
    Organization org = tester.organizations().generate();
    SearchWsResponse.QualityProfile sonarWay = getProfile(org, p -> PROFILE_SONAR_WAY.equals(p.getName()) && LANGUAGE_XOO.equals(p.getLanguage()) && p.getIsBuiltIn());

    List<Rules.Rule> result = tester.wsClient().rules().search(new SearchRequest().setQprofile(sonarWay.getKey()).setActivation("true"))
      .getRulesList();

    assertThat(result)
      .extracting(Rules.Rule::getKey)
      .containsExactlyInAnyOrder(RULE_HAS_TAG, RULE_ONE_ISSUE_PER_LINE, RULE_ONE_ISSUE_PER_FILE);
  }

  @Test
  public void search_sonar_way_rules_not_activated() {
    Organization org = tester.organizations().generate();
    CreateWsResponse.QualityProfile xooProfile = tester.qProfiles().createXooProfile(org);
    tester.qProfiles().activateRule(xooProfile, RULE_ONE_BUG_PER_LINE);
    tester.qProfiles().activateRule(xooProfile, RULE_ONE_ISSUE_PER_LINE);
    SearchWsResponse.QualityProfile sonarWay = getProfile(org, p -> PROFILE_SONAR_WAY.equals(p.getName()) && LANGUAGE_XOO.equals(p.getLanguage()) && p.getIsBuiltIn());

    List<Rules.Rule> result = tester.wsClient().rules().search(new SearchRequest()
      .setQprofile(xooProfile.getKey())
      .setActivation("false")
      .setCompareToProfile(sonarWay.getKey()))
      .getRulesList();

    assertThat(result)
      .extracting(Rules.Rule::getKey)
      .containsExactlyInAnyOrder(RULE_HAS_TAG, RULE_ONE_ISSUE_PER_FILE);
  }

  @Test
  public void show_rule_with_test_scope() {
    ShowResponse show = tester.wsClient().rules().show(new ShowRequest().setKey(RULE_ONE_ISSUE_PER_TEST_FILE));
    assertThat(show.getRule().getScope()).isEqualTo(RuleScope.TEST);

  }

  private SearchWsResponse.QualityProfile getProfile(Organization organization, Predicate<SearchWsResponse.QualityProfile> filter) {
    return tester.qProfiles().service().search(new org.sonarqube.ws.client.qualityprofiles.SearchRequest()
      .setOrganization(organization.getKey())).getProfilesList()
      .stream()
      .filter(filter)
      .findAny().orElseThrow(IllegalStateException::new);
  }
}

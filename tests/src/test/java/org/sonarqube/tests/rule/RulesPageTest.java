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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.RuleDetails;
import org.sonarqube.qa.util.pageobjects.RulesPage;
import org.sonarqube.tests.Category4Suite;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse.QualityProfile;
import org.sonarqube.ws.client.qualityprofiles.ChangeParentRequest;
import org.sonarqube.ws.client.qualityprofiles.CreateRequest;
import org.sonarqube.ws.client.qualityprofiles.DeleteRequest;
import util.ProjectAnalysisRule;

import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;

public class RulesPageTest {
  private static final String SAMPLE_RULE = "Has Tag";
  private static final String SAMPLE_RULE2 = "Branches should have sufficient coverage by tests";
  private static final String SAMPLE_SECURITY_RULE = "One Vulnerability Issue Per Module";
  private static final String SAMPLE_TEMPLATE_RULE = "Template of rule";

  private static final String XOO_LANG = "xoo";
  private static final String XOO2_LANG = "xoo2";

  private static final String INHERITED_PROFILE = "Inherited";

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category4Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(ORCHESTRATOR).disableOrganizations();

  @Rule
  public ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  @Before
  public void before() {
    createInheritedQualityProfile();
  }

  @After
  public void after() {
    deleteInheritedQualityProfile();
  }

  @Test
  public void should_search_rules() {
    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRules(SAMPLE_RULE, SAMPLE_RULE2);
    page.search("branches");
    page.shouldNotDisplayRules(SAMPLE_RULE).shouldDisplayRules(SAMPLE_RULE2);
  }

  @Test
  public void should_filter_rules_by_language() {
    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRuleWithLanguage(SAMPLE_RULE, XOO2_LANG);
    page.selectFacetItemByText("languages", XOO_LANG);
    page.shouldNotDisplayRuleWithLanguage(SAMPLE_RULE, XOO2_LANG);
    page.shouldDisplayRuleWithLanguage(SAMPLE_RULE, XOO_LANG);
    assertThat(url()).contains("languages=" + XOO_LANG);
  }

  @Test
  public void should_filter_rules_by_type() {
    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRules(SAMPLE_RULE, SAMPLE_SECURITY_RULE);
    page.selectFacetItemByText("types", "VULNERABILITY");
    page.shouldNotDisplayRules(SAMPLE_RULE).shouldDisplayRules(SAMPLE_SECURITY_RULE);
    assertThat(url()).contains("types=VULNERABILITY");
  }

  @Test
  public void should_filter_rules_by_default_severity() {
    // TODO add a BLOCKER xoo rule
    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRules(SAMPLE_RULE);
    page.openFacet("severities").selectFacetItemByText("severities", "BLOCKER");
    page.shouldNotDisplayRules(SAMPLE_RULE);
    assertThat(url()).contains("severities=BLOCKER");
  }

  @Test
  public void should_filter_rules_by_availability_date() {
    // TODO
    // all rules have the same "available since" date, because this is the date when server started
  }

  @Test
  public void should_filter_rules_by_template() {
    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRules(SAMPLE_RULE, SAMPLE_TEMPLATE_RULE);

    page.openFacet("is_template").selectFacetItemByText("is_template", "Show Templates Only");
    page.shouldNotDisplayRules(SAMPLE_RULE).shouldDisplayRules(SAMPLE_TEMPLATE_RULE);
    assertThat(url()).contains("is_template=true");

    page.selectFacetItemByText("is_template", "Hide Templates");
    page.shouldDisplayRules(SAMPLE_RULE).shouldNotDisplayRules(SAMPLE_TEMPLATE_RULE);
    assertThat(url()).contains("is_template=false");
  }

  @Test
  public void should_filter_rules_by_quality_profile() {
    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRules(SAMPLE_RULE);

    page.openFacet("qprofile").selectFacetItemByText("qprofile", "empty");
    page.shouldNotDisplayRules(SAMPLE_RULE).shouldHaveTotalRules(0);
    assertThat(url()).contains("qprofile="); // TODO we don't know profile key

    page.selectFacetItemByText("qprofile", INHERITED_PROFILE);
    page.shouldDisplayRules(SAMPLE_RULE);
  }

  @Test
  public void should_filter_rules_by_inheritance() {
    RulesPage page = tester.openBrowser().openRules();
    page.shouldHaveDisabledFacet("inheritance").shouldDisplayRules(SAMPLE_RULE);

    page.openFacet("qprofile").selectFacetItemByText("qprofile", INHERITED_PROFILE);
    page.shouldNotHaveDisabledFacet("inheritance").openFacet("inheritance");

    page.selectFacetItem("inheritance", "NONE");
    page.shouldDisplayRules(SAMPLE_RULE2).shouldNotDisplayRules(SAMPLE_RULE);
    assertThat(url()).contains("inheritance=NONE");

    page.selectFacetItem("inheritance", "INHERITED");
    page.shouldDisplayRules("One Issue Per Line").shouldNotDisplayRules(SAMPLE_RULE);
    assertThat(url()).contains("inheritance=INHERITED");

    page.selectFacetItem("inheritance", "OVERRIDES");
    page.shouldDisplayRules(SAMPLE_RULE).shouldNotDisplayRules(SAMPLE_RULE2);
    assertThat(url()).contains("inheritance=OVERRIDES");
  }

  @Test
  public void should_filter_rules_by_activation_severity() {
    RulesPage page = tester.openBrowser().openRules();
    page.shouldHaveDisabledFacet("active_severities").shouldDisplayRules(SAMPLE_RULE, SAMPLE_RULE2);

    page.openFacet("qprofile").selectFacetItemByText("qprofile", INHERITED_PROFILE);
    page.shouldNotHaveDisabledFacet("active_severities").openFacet("active_severities");

    page.selectFacetItem("active_severities", "BLOCKER");
    page.shouldDisplayRules(SAMPLE_RULE).shouldNotDisplayRules(SAMPLE_RULE2);
    assertThat(url()).contains("active_severities=BLOCKER");
  }

  @Test
  public void should_clear_all_filters() {
    RulesPage page = tester.openBrowser().openRules()
      .shouldDisplayRuleWithLanguage(SAMPLE_RULE, XOO_LANG)
      .shouldDisplayRuleWithLanguage(SAMPLE_RULE2, XOO_LANG)
      .shouldDisplayRuleWithLanguage(SAMPLE_RULE, XOO2_LANG);

    page.search("branches")
      .shouldNotDisplayRules(SAMPLE_RULE);

    page.selectFacetItemByText("languages", XOO_LANG)
      .shouldNotDisplayRuleWithLanguage(SAMPLE_RULE, XOO2_LANG);

    page.clearAllFilters()
      .shouldDisplayRuleWithLanguage(SAMPLE_RULE, XOO_LANG)
      .shouldDisplayRuleWithLanguage(SAMPLE_RULE2, XOO_LANG)
      .shouldDisplayRuleWithLanguage(SAMPLE_RULE, XOO2_LANG);
  }

  @Test
  public void should_load_more_rules() {
    // TODO
    // due to the infinite scrolling, it's not possible to test
  }

  @Test
  public void should_filter_similar_rules() {
    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRules(SAMPLE_RULE, SAMPLE_SECURITY_RULE);
    page.takeRuleByName(SAMPLE_SECURITY_RULE).filterSimilarRules("types");
    page.shouldNotDisplayRules(SAMPLE_RULE).shouldDisplayRules(SAMPLE_SECURITY_RULE);
    assertThat(url()).contains("types=VULNERABILITY");
  }

  @Test
  public void should_display_rule_details() {
    RulesPage page = tester.openBrowser().openRules();
    RuleDetails ruleDetails = page.takeRuleByName(SAMPLE_RULE).open();
    ruleDetails
      .shouldHaveType("Code Smell")
      .shouldHaveSeverity("Major")
      .shouldHaveNoTags()
      .shouldHaveDescription("Search for a given tag in Xoo files");
  }

  @Test
  public void should_display_rule_profiles() {
    RulesPage page = tester.openBrowser().openRules();
    page.openFacet("qprofile").selectFacetItemByText("qprofile", "Basic").shouldHaveTotalRules(1);
    page.openFirstRule().shouldBeActivatedOn("Basic");
  }

  @Test
  public void should_display_rule_issues() {
    analyzeProjectWithIssues();

    RulesPage page = tester.openBrowser().openRules();
    page.selectFacetItem("languages", XOO_LANG);
    page.takeRuleByName("One Issue Per Line").open()
      .shouldHaveTotalIssues(17).shouldHaveIssuesOnProject("Sample", 17);
  }

  @Test
  public void should_extend_rule_description() {
    String admin = tester.users().generateAdministrator().getLogin();
    RuleDetails ruleDetails = tester.openBrowser().logIn().submitCredentials(admin).openRules().openFirstRule();

    ruleDetails.extendDescription().cancel();
    ruleDetails.extendDescription().type("my extended description").submit();
    ruleDetails.extendDescription().type("another description").submit();
    ruleDetails.extendDescription().remove();
  }

  @Test
  public void should_change_rule_tags() {}

  @Test
  public void should_create_edit_delete_custom_rule() {}

  @Test
  public void should_reactivate_custom_rule() {}

  @Test
  public void should_activate_rule_from_list() {}

  @Test
  public void should_activate_rule_from_details() {}

  @Test
  public void should_deactivate_rule_from_list() {}

  @Test
  public void should_deactivate_rule_from_details() {}

  @Test
  public void should_synchronize_activation_between_list_and_details() {}

  @Test
  public void should_change_rule_activation() {}

  @Test
  public void should_revert_rule_activation_to_parent_definition() {}

  private void createInheritedQualityProfile() {
    QualityProfile profile = tester.qProfiles().service().create(new CreateRequest().setName(INHERITED_PROFILE).setLanguage("xoo")).getProfile();
    tester.qProfiles().service().changeParent(new ChangeParentRequest().setLanguage("xoo").setParentQualityProfile("Sonar way").setQualityProfile(INHERITED_PROFILE));
    // activate a rule
    tester.qProfiles().activateRule(profile, "common-xoo:InsufficientBranchCoverage");
    // override a rule
    tester.qProfiles().activateRule(profile, "xoo:HasTag", "BLOCKER");
  }

  private void deleteInheritedQualityProfile() {
    tester.qProfiles().service().delete(new DeleteRequest().setLanguage("xoo").setQualityProfile(INHERITED_PROFILE));
  }

  private void analyzeProjectWithIssues() {
    String qualityProfileKey = projectAnalysisRule.registerProfile("/issue/IssueActionTest/xoo-one-issue-per-line-profile.xml");
    String projectKey = projectAnalysisRule.registerProject("shared/xoo-sample");
    projectAnalysisRule.newProjectAnalysis(projectKey).withQualityProfile(qualityProfileKey).run();
  }

}

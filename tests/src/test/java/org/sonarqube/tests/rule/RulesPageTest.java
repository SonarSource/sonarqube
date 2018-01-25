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
import util.ProjectAnalysisRule;

import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;

public class RulesPageTest {
  private static final String SAMPLE_RULE = "xoo:HasTag";
  private static final String SAMPLE_RULE_XOO2 = "xoo2:HasTag";
  private static final String SAMPLE_RULE2 = "common-xoo:InsufficientBranchCoverage";
  private static final String SAMPLE_SECURITY_RULE = "xoo:OneVulnerabilityIssuePerModule";
  private static final String SAMPLE_TEMPLATE_RULE = "xoo:xoo-template";
  private static final String XOO_LANG = "xoo";
  private static int profileIndex = 0;

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category4Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(ORCHESTRATOR).disableOrganizations();

  @Rule
  public ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  @Test
  public void should_search_rules() {
    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRules(SAMPLE_RULE, SAMPLE_RULE2);
    page.search("branches");
    page.shouldNotDisplayRules(SAMPLE_RULE).shouldDisplayRules(SAMPLE_RULE2);
    assertThat(url()).contains("q=branches");
  }

  @Test
  public void should_filter_rules_by_language() {
    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRules(SAMPLE_RULE, SAMPLE_RULE_XOO2);
    page.selectFacetItem("languages", XOO_LANG);
    page.shouldNotDisplayRules(SAMPLE_RULE_XOO2).shouldDisplayRules(SAMPLE_RULE);
    assertThat(url()).contains("languages=" + XOO_LANG);
  }

  @Test
  public void should_filter_rules_by_type() {
    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRules(SAMPLE_RULE, SAMPLE_SECURITY_RULE);
    page.selectFacetItem("types", "VULNERABILITY");
    page.shouldNotDisplayRules(SAMPLE_RULE).shouldDisplayRules(SAMPLE_SECURITY_RULE);
    assertThat(url()).contains("types=VULNERABILITY");
  }

  @Test
  public void should_filter_rules_by_default_severity() {
    String ruleKey = "custom_blocker_rule";
    String blockerRule = "xoo:" + ruleKey;
    createBlockerCustomRule(ruleKey);

    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRules(SAMPLE_RULE, blockerRule);
    page.openFacet("severities").selectFacetItem("severities", "BLOCKER");
    page.shouldNotDisplayRules(SAMPLE_RULE).shouldDisplayRules(blockerRule);
    assertThat(url()).contains("severities=BLOCKER");

    deleteCustomRule(blockerRule);
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

    page.openFacet("template").selectFacetItem("template", "true");
    page.shouldNotDisplayRules(SAMPLE_RULE).shouldDisplayRules(SAMPLE_TEMPLATE_RULE);
    assertThat(url()).contains("template=true");

    page.selectFacetItem("template", "false");
    page.shouldNotDisplayRules(SAMPLE_TEMPLATE_RULE).shouldDisplayRules(SAMPLE_RULE);
    assertThat(url()).contains("template=false");
  }

  @Test
  public void should_filter_rules_by_quality_profile() {
    QualityProfile empty = createQualityProfile();
    QualityProfile profile = createQualityProfile();
    tester.qProfiles().activateRule(profile, SAMPLE_RULE);

    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRules(SAMPLE_RULE);

    page.openFacet("profile").selectFacetItem("profile", empty.getKey());
    page.shouldNotDisplayRules(SAMPLE_RULE);
    assertThat(url()).contains("qprofile="); // TODO we don't know profile key

    page.selectFacetItem("profile", profile.getKey());
    page.shouldDisplayRules(SAMPLE_RULE);
    assertThat(url()).contains("qprofile=" + profile.getKey());
  }

  @Test
  public void should_filter_rules_by_inheritance() {
    QualityProfile profile = createInheritedQualityProfile();

    RulesPage page = tester.openBrowser().openRules();
    page.shouldHaveDisabledFacet("inheritance").shouldDisplayRules(SAMPLE_RULE);

    page.openFacet("profile").selectFacetItem("profile", profile.getKey());
    page.shouldNotHaveDisabledFacet("inheritance").openFacet("inheritance");

    page.selectFacetItem("inheritance", "NONE");
    page.shouldDisplayRules(SAMPLE_RULE2).shouldNotDisplayRules(SAMPLE_RULE);
    assertThat(url()).contains("inheritance=NONE");

    page.selectFacetItem("inheritance", "INHERITED");
    page.shouldDisplayRules("xoo:OneIssuePerLine").shouldNotDisplayRules(SAMPLE_RULE);
    assertThat(url()).contains("inheritance=INHERITED");

    page.selectFacetItem("inheritance", "OVERRIDES");
    page.shouldDisplayRules(SAMPLE_RULE).shouldNotDisplayRules(SAMPLE_RULE2);
    assertThat(url()).contains("inheritance=OVERRIDES");
  }

  @Test
  public void should_filter_rules_by_activation_severity() {
    QualityProfile profile = createQualityProfile();
    tester.qProfiles().activateRule(profile, SAMPLE_RULE, "BLOCKER");

    RulesPage page = tester.openBrowser().openRules();
    page.shouldHaveDisabledFacet("activationSeverities").shouldDisplayRules(SAMPLE_RULE, SAMPLE_RULE2);

    page.openFacet("profile").selectFacetItem("profile", profile.getKey());
    page.shouldNotHaveDisabledFacet("activationSeverities").openFacet("activationSeverities");

    page.selectFacetItem("activationSeverities", "BLOCKER");
    page.shouldDisplayRules(SAMPLE_RULE).shouldNotDisplayRules(SAMPLE_RULE2);
    assertThat(url()).contains("active_severities=BLOCKER");
  }

  @Test
  public void should_clear_all_filters() {
    RulesPage page = tester.openBrowser().openRules()
      .shouldDisplayRules(SAMPLE_RULE, SAMPLE_RULE_XOO2, SAMPLE_RULE2);

    page.search("branches")
      .shouldNotDisplayRules(SAMPLE_RULE);

    page.selectFacetItem("languages", XOO_LANG)
      .shouldNotDisplayRules(SAMPLE_RULE_XOO2);

    page.clearAllFilters()
      .shouldDisplayRules(SAMPLE_RULE, SAMPLE_RULE_XOO2, SAMPLE_RULE2);
  }

  @Test
  public void should_load_more_rules() {
    // TODO
  }

  @Test
  public void should_filter_similar_rules() {
    RulesPage page = tester.openBrowser().openRules();
    page.shouldDisplayRules(SAMPLE_RULE, SAMPLE_SECURITY_RULE);
    page.takeRule(SAMPLE_SECURITY_RULE).filterSimilarRules("type");
    page.shouldNotDisplayRules(SAMPLE_RULE).shouldDisplayRules(SAMPLE_SECURITY_RULE);
    assertThat(url()).contains("types=VULNERABILITY");
  }

  @Test
  public void should_display_rule_details() {
    RulesPage page = tester.openBrowser().openRules();
    RuleDetails ruleDetails = page.takeRule(SAMPLE_RULE).open();
    ruleDetails
      .shouldHaveType("Code Smell")
      .shouldHaveSeverity("Major")
      .shouldHaveDescription("Search for a given tag in Xoo files")
      .tags().shouldHaveNoTags();
  }

  @Test
  public void should_display_rule_issues() {
    analyzeProjectWithIssues();

    RulesPage page = tester.openBrowser().openRules();
    page.selectFacetItem("languages", XOO_LANG);
    page.takeRule("xoo:OneIssuePerLine").open()
      .shouldHaveTotalIssues(17).shouldHaveIssuesOnProject("Sample", 17);
  }

  @Test
  public void should_extend_rule_description() {
    RuleDetails ruleDetails = openRulesAsAdmin().openFirstRule();
    ruleDetails.extendDescription().cancel();
    ruleDetails.extendDescription().type("my extended description").submit();
    ruleDetails.extendDescription().type("another description").submit();
    ruleDetails.extendDescription().remove();
  }

  @Test
  public void should_change_rule_tags() {
    RuleDetails ruleDetails = openRulesAsAdmin().takeRule(SAMPLE_RULE2).open();
    ruleDetails.tags()
      .shouldHaveTags("bad-practice")
      .edit().search("foo").select("+ foo").done()
      .edit().select("convention").done()
      .shouldHaveTags("bad-practice", "convention", "foo");
  }

  @Test
  public void should_create_edit_delete_reactivate_custom_rule() {
    String customRuleName = "custom_rule_name";
    String customRuleKey = "xoo:" + customRuleName;
    RuleDetails ruleDetails = openRulesAsAdmin().takeRule(SAMPLE_TEMPLATE_RULE).open();

    ruleDetails.createCustomRule(customRuleName).shouldHaveCustomRule(customRuleKey);
    ruleDetails.deleteCustomRule(customRuleKey).shouldNotHaveCustomRule(customRuleKey);
    ruleDetails.reactivateCustomRule(customRuleName).shouldHaveCustomRule(customRuleKey);
  }

  @Test
  public void should_activate_deactivate_rule_from_list() {
    QualityProfile profile = createQualityProfile();

    RulesPage page = openRulesAsAdmin();
    page.openFacet("profile").selectFacetItem("profile", profile.getKey()).selectInactive();
    page.shouldDisplayRules(SAMPLE_SECURITY_RULE);

    page.activateRule(SAMPLE_SECURITY_RULE).deactivateRule(SAMPLE_SECURITY_RULE);
  }

  @Test
  public void should_activate_rule_from_details() {
    // make sure we have at least two quality profiles, so we can choose one of them in the select
    createQualityProfile();
    QualityProfile profile = createQualityProfile();

    RulesPage page = openRulesAsAdmin();
    RuleDetails ruleDetails = page.takeRule(SAMPLE_SECURITY_RULE).open();
    ruleDetails.shouldNotBeActivatedOn(profile.getName()).activate().select(profile.getName()).save();
    ruleDetails.shouldBeActivatedOn(profile.getKey());
  }

  @Test
  public void should_synchronize_activation_between_list_and_details() {
    // make sure we have at least two quality profiles, so we can choose one of them in the select
    createQualityProfile();
    QualityProfile profile = createQualityProfile();
    RulesPage page = openRulesAsAdmin();

    page.openFacet("profile").selectFacetItem("profile", profile.getKey()).selectInactive();
    page.shouldDisplayRules(SAMPLE_SECURITY_RULE);

    RuleDetails ruleDetails = page.takeRule(SAMPLE_SECURITY_RULE).open();

    ruleDetails.shouldNotBeActivatedOn(profile.getKey())
      .activate().select(profile.getKey()).save();

    ruleDetails.shouldBeActivatedOn(profile.getKey());

    page.closeDetails();
    page.takeRule(SAMPLE_SECURITY_RULE).shouldDisplayDeactivate();
  }

  @Test
  public void should_change_rule_activation() {
    String ruleWithParameters = "xoo:RuleWithParameters";
    // make sure we have at least two quality profiles, so we can choose one of them in the select
    createQualityProfile();
    QualityProfile profile = createQualityProfile();
    RuleDetails ruleDetails = openRulesAsAdmin().takeRule(ruleWithParameters).open();

    ruleDetails.activate().select(profile.getKey()).save();
    ruleDetails.shouldBeActivatedOn(profile.getKey());

    ruleDetails.changeActivationOn(profile.getKey())
      .fill("string", "foo").fill("integer", "123").save();

    ruleDetails
      .activationShouldHaveParameter(profile.getKey(),"string", "foo")
      .activationShouldHaveParameter(profile.getKey(),"integer", "123");
  }

  @Test
  public void should_revert_rule_activation_to_parent_definition() {
    QualityProfile profile = createInheritedQualityProfile();

    RuleDetails ruleDetails = openRulesAsAdmin().takeRule(SAMPLE_RULE).open();
    ruleDetails.activationShouldHaveSeverity(profile.getKey(), "BLOCKER");
    ruleDetails.revertActivationToParentDefinition(profile.getKey());
    ruleDetails.activationShouldHaveSeverity(profile.getKey(), "MAJOR");
  }

  private RulesPage openRulesAsAdmin() {
    String admin = tester.users().generateAdministrator().getLogin();
    return tester.openBrowser().logIn().submitCredentials(admin).openRules();
  }

  private String randomQualityProfileName() {
    return "random_profile_" + String.valueOf(profileIndex++);
  }

  private QualityProfile createQualityProfile() {
    return tester.qProfiles().service().create(new CreateRequest().setName(randomQualityProfileName()).setLanguage("xoo")).getProfile();
  }

  private QualityProfile createInheritedQualityProfile() {
    QualityProfile profile = createQualityProfile();
    tester.qProfiles().service().changeParent(new ChangeParentRequest().setQualityProfile(profile.getName()).setLanguage("xoo").setParentQualityProfile("Sonar way"));
    // activate a rule
    tester.qProfiles().activateRule(profile, SAMPLE_RULE2);
    // override a rule
    tester.qProfiles().activateRule(profile, SAMPLE_RULE, "BLOCKER");
    return profile;
  }

  private void analyzeProjectWithIssues() {
    String qualityProfileKey = projectAnalysisRule.registerProfile("/issue/IssueActionTest/xoo-one-issue-per-line-profile.xml");
    String projectKey = projectAnalysisRule.registerProject("shared/xoo-sample");
    projectAnalysisRule.newProjectAnalysis(projectKey).withQualityProfile(qualityProfileKey).run();
  }

  private void createBlockerCustomRule(String ruleKey) {
    tester.wsClient().rules().create(new org.sonarqube.ws.client.rules.CreateRequest()
      .setTemplateKey(SAMPLE_TEMPLATE_RULE)
      .setSeverity("BLOCKER")
      .setMarkdownDescription(ruleKey).setName(ruleKey).setCustomKey(ruleKey));
  }

  private void deleteCustomRule(String ruleKey) {
    tester.wsClient().rules().delete(new org.sonarqube.ws.client.rules.DeleteRequest().setKey(ruleKey));
  }

}

/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.education;

import org.sonar.api.server.rule.RuleDescriptionSection;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.education.sensors.EducationPrinciplesSensor;
import org.sonar.education.sensors.EducationWithContextsSensor;
import org.sonar.education.sensors.EducationWithDetectedContextSensor;

import static org.sonar.education.sensors.EducationWithSingleContextSensor.EDUCATION_WITH_SINGLE_CONTEXT_RULE_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.INTRODUCTION_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.RESOURCES_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;

public class EducationRulesDefinition implements RulesDefinition {

  public static final String EDUCATION_RULE_REPOSITORY_KEY = "edu";
  public static final String EDUCATION_KEY = "education";

  private static final String IGNORED_FAKE_SECTION = "fake_section_to_be_ignored";

  public static final String[] CONTEXTS = {"spring", "hibernate", "apache-commons", "vaadin", "mybatis"};

  private static final String HTML_LOREM_IPSUM = "<a href=\"https://google.com\">Lorem</a> ipsum dolor sit amet, consectetur adipiscing " +
    "elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco " +
    "laboris nisi ut aliquip ex ea commodo consequat.<br />Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
    "fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

  @Override
  public void define(Context context) {
    NewRepository repo = context.createRepository(EDUCATION_RULE_REPOSITORY_KEY, EDUCATION_KEY);

    createRuleWithDescriptionSectionsAndEducationPrinciples(repo);
    createRuleWithDescriptionSectionsAndSingleContext(repo);
    createRuleWithDescriptionSectionsAndMultipleContexts(repo);
    createRuleWithDescriptionSectionsAndMultipleContextsIncludingOneDetected(repo);

    repo.done();
  }

  private void createRuleWithDescriptionSectionsAndEducationPrinciples(NewRepository repo) {
    String[] educationPrinciples = {"defense_in_depth", "never_trust_user_input"};
    String ruleKey = EducationPrinciplesSensor.EDUCATION_WITH_GENERIC_CONCEPTS_RULE_KEY;
    NewRule ruleWithSingleContext = repo.createRule(ruleKey).setName("Rule with description sections and education principles")
      .addTags(EDUCATION_KEY);

    ruleWithSingleContext
      .setHtmlDescription(String.format("This rule contains description sections. To trigger an issue using this rule you need to " +
          "scan any text file with a line containing %s key word. This rule also contains 2 education principles - %s and %s",
        ruleKey, educationPrinciples[0], educationPrinciples[1]))
      .addEducationPrincipleKeys(educationPrinciples);

    addNonContextualizedDescriptionSections(ruleWithSingleContext);
    descriptionSection(HOW_TO_FIX_SECTION_KEY);
  }

  private void createRuleWithDescriptionSectionsAndSingleContext(NewRepository repo) {
    String ruleKey = EDUCATION_WITH_SINGLE_CONTEXT_RULE_KEY;
    NewRule ruleWithSingleContext = repo.createRule(ruleKey).setName("Rule with description sections and single " +
        "contexts for 'how to fix'")
      .addTags(EDUCATION_KEY);

    ruleWithSingleContext
      .setHtmlDescription(String.format("This rule contains description sections and single context for 'how to fix' section. To " +
        "trigger an issue using this rule you need to scan any text file with a line containing %s key word.", ruleKey));

    addNonContextualizedDescriptionSections(ruleWithSingleContext);

    ruleWithSingleContext.addDescriptionSection(descriptionHowToFixSectionWithContext(CONTEXTS[0]));
  }

  private void createRuleWithDescriptionSectionsAndMultipleContexts(NewRepository repo) {
    NewRule ruleWithMultipleContexts = repo.createRule(EducationWithContextsSensor.EDUCATION_WITH_CONTEXTS_RULE_KEY)
      .setName("Rule with description sections and multiple contexts for 'how to fix'")
      .addTags(EDUCATION_KEY);

    ruleWithMultipleContexts
      .setHtmlDescription(String.format("This rule contains description sections and multiple contexts for 'how to fix' section. To trigger " +
          "an issue using this rule you need to scan any text file with a line containing %s keyword.",
        EducationWithContextsSensor.EDUCATION_WITH_CONTEXTS_RULE_KEY));

    addNonContextualizedDescriptionSections(ruleWithMultipleContexts);

    for (String context : CONTEXTS) {
      ruleWithMultipleContexts.addDescriptionSection(descriptionHowToFixSectionWithContext(context));
    }
  }

  private void createRuleWithDescriptionSectionsAndMultipleContextsIncludingOneDetected(NewRepository repo) {
    NewRule ruleWithMultipleContexts = repo.createRule(EducationWithDetectedContextSensor.EDUCATION_WITH_DETECTED_CONTEXT_RULE_KEY)
      .setName("Rule with description sections and multiple contexts (including one detected) for 'how to fix'")
      .addTags(EDUCATION_KEY);

    ruleWithMultipleContexts
      .setHtmlDescription(String.format("This rule contains description sections and multiple contexts (including one detected) for " +
          "'how to fix' section. To trigger an issue using this rule you need to scan any text file with a line containing %s keyword.",
        EducationWithDetectedContextSensor.EDUCATION_WITH_DETECTED_CONTEXT_RULE_KEY));

    addNonContextualizedDescriptionSections(ruleWithMultipleContexts);

    for (String context : CONTEXTS) {
      ruleWithMultipleContexts.addDescriptionSection(descriptionHowToFixSectionWithContext(context));
    }
  }

  private static void addNonContextualizedDescriptionSections(NewRule newRule) {
    newRule.addDescriptionSection(descriptionSection(INTRODUCTION_SECTION_KEY))
      .addDescriptionSection(descriptionSection(ROOT_CAUSE_SECTION_KEY))
      .addDescriptionSection(descriptionSection(ASSESS_THE_PROBLEM_SECTION_KEY))
      .addDescriptionSection(descriptionSection(RESOURCES_SECTION_KEY))
      .addDescriptionSection(descriptionSection(IGNORED_FAKE_SECTION));
  }

  private static RuleDescriptionSection descriptionHowToFixSectionWithContext(String context) {
    var ruleContext = new org.sonar.api.server.rule.Context(context, context);
    return RuleDescriptionSection.builder()
      .sectionKey(HOW_TO_FIX_SECTION_KEY)
      .context(ruleContext)
      .htmlContent(String.format("%s: %s", HOW_TO_FIX_SECTION_KEY, HTML_LOREM_IPSUM))
      .build();
  }

  private static RuleDescriptionSection descriptionSection(String sectionKey) {
    return RuleDescriptionSection.builder()
      .sectionKey(sectionKey)
      .htmlContent(String.format("%s: %s", sectionKey, HTML_LOREM_IPSUM))
      .build();
  }
}

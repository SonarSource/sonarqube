/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.rule;

import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;

import static org.sonar.server.rule.CommonRuleKeys.commonRepositoryForLang;

/**
 * Declare the few rules that are automatically created by core for all languages. These rules
 * check measure values against thresholds defined in Quality profiles.
 */
// this class must not be mixed with other RulesDefinition so it does implement the interface RulesDefinitions.
// It replaces the common-rules that are still embedded within plugins.
public class CommonRuleDefinitionsImpl implements CommonRuleDefinitions {

  private final Languages languages;

  public CommonRuleDefinitionsImpl(Languages languages) {
    this.languages = languages;
  }

  @Override
  public void define(RulesDefinition.Context context) {
    for (Language language : languages.all()) {
      RulesDefinition.NewRepository repo = context.createRepository(commonRepositoryForLang(language.getKey()), language.getKey());
      repo.setName("Common " + language.getName());
      defineBranchCoverageRule(repo);
      defineLineCoverageRule(repo);
      defineCommentDensityRule(repo);
      defineDuplicatedBlocksRule(repo);
      defineFailedUnitTestRule(repo);
      defineSkippedUnitTestRule(repo);
      repo.done();
    }
  }

  private static void defineBranchCoverageRule(RulesDefinition.NewRepository repo) {
    RulesDefinition.NewRule rule = repo.createRule(CommonRuleKeys.INSUFFICIENT_BRANCH_COVERAGE);
    rule.setName("Branches should have sufficient coverage by tests")
      .addTags("bad-practice")
      .setHtmlDescription("An issue is created on a file as soon as the branch coverage on this file is less than the required threshold. "
        + "It gives the number of branches to be covered in order to reach the required threshold.")
      .setDebtRemediationFunction(rule.debtRemediationFunctions().linear("5min"))
      .setGapDescription("number of uncovered conditions")
      .setSeverity(Severity.MAJOR);
    rule.createParam(CommonRuleKeys.INSUFFICIENT_BRANCH_COVERAGE_PROPERTY)
      .setName("The minimum required branch coverage ratio")
      .setDefaultValue("65")
      .setType(RuleParamType.FLOAT);
  }

  private static void defineLineCoverageRule(RulesDefinition.NewRepository repo) {
    RulesDefinition.NewRule rule = repo.createRule(CommonRuleKeys.INSUFFICIENT_LINE_COVERAGE);
    rule.setName("Lines should have sufficient coverage by tests")
      .addTags("bad-practice")
      .setHtmlDescription("An issue is created on a file as soon as the line coverage on this file is less than the required threshold. " +
        "It gives the number of lines to be covered in order to reach the required threshold.")
      .setDebtRemediationFunction(rule.debtRemediationFunctions().linear("2min"))
      .setGapDescription("number of lines under the coverage threshold")
      .setSeverity(Severity.MAJOR);
    rule.createParam(CommonRuleKeys.INSUFFICIENT_LINE_COVERAGE_PROPERTY)
      .setName("The minimum required line coverage ratio")
      .setDefaultValue("65")
      .setType(RuleParamType.FLOAT);
  }

  private static void defineCommentDensityRule(RulesDefinition.NewRepository repo) {
    RulesDefinition.NewRule rule = repo.createRule(CommonRuleKeys.INSUFFICIENT_COMMENT_DENSITY);
    rule.setName("Source files should have a sufficient density of comment lines")
      .addTags("convention")
      .setHtmlDescription("An issue is created on a file as soon as the density of comment lines on this file is less than the required threshold. " +
        "The number of comment lines to be written in order to reach the required threshold is provided by each issue message.")
      .setDebtRemediationFunction(rule.debtRemediationFunctions().linear("2min"))
      .setGapDescription("number of lines required to meet minimum density")
      .setSeverity(Severity.MAJOR);
    rule.createParam(CommonRuleKeys.INSUFFICIENT_COMMENT_DENSITY_PROPERTY)
      .setName("The minimum required comment density")
      .setDefaultValue("25")
      .setType(RuleParamType.FLOAT);
  }

  private static void defineDuplicatedBlocksRule(RulesDefinition.NewRepository repo) {
    RulesDefinition.NewRule rule = repo.createRule(CommonRuleKeys.DUPLICATED_BLOCKS);
    rule.setName("Source files should not have any duplicated blocks")
      .addTags("pitfall")
      .setHtmlDescription("An issue is created on a file as soon as there is at least one block of duplicated code on this file")
      .setDebtRemediationFunction(rule.debtRemediationFunctions().linearWithOffset("10min", "10min"))
      .setGapDescription("number of duplicate blocks")
      .setSeverity(Severity.MAJOR);
  }

  private static void defineFailedUnitTestRule(RulesDefinition.NewRepository repo) {
    RulesDefinition.NewRule rule = repo.createRule(CommonRuleKeys.FAILED_UNIT_TESTS);
    rule
      .setName("Failed unit tests should be fixed")
      .addTags("bug")
      .setHtmlDescription(
        "Test failures or errors generally indicate that regressions have been introduced. Those tests should be handled as soon as possible to reduce the cost to fix the corresponding regressions.")
      .setDebtRemediationFunction(rule.debtRemediationFunctions().linear("10min"))
      .setGapDescription("number of failed tests")
      .setSeverity(Severity.MAJOR);
  }

  private static void defineSkippedUnitTestRule(RulesDefinition.NewRepository repo) {
    RulesDefinition.NewRule rule = repo.createRule(CommonRuleKeys.SKIPPED_UNIT_TESTS);
    rule.setName("Skipped unit tests should be either removed or fixed")
      .addTags("pitfall")
      .setHtmlDescription("Skipped unit tests are considered as dead code. Either they should be activated again (and updated) or they should be removed.")
      .setDebtRemediationFunction(rule.debtRemediationFunctions().linear("10min"))
      .setGapDescription("number of skipped tests")
      .setSeverity(Severity.MAJOR);
  }
}

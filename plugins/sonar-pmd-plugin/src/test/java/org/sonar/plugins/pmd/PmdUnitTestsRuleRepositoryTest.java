/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.pmd;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.XMLRuleParser;
import org.sonar.test.i18n.RuleRepositoryTestHelper;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class PmdUnitTestsRuleRepositoryTest {
  PmdUnitTestsRuleRepository repository;

  @Before
  public void setUpRuleRepository() {
    repository = new PmdUnitTestsRuleRepository(new XMLRuleParser());
  }

  @Test
  public void should_have_correct_name_and_key() {
    assertThat(repository.getKey()).isEqualTo("pmd-unit-tests");
    assertThat(repository.getLanguage()).isEqualTo("java");
    assertThat(repository.getName()).isEqualTo("PMD Unit Tests");
  }

  @Test
  public void should_load_repository_from_xml() {
    List<Rule> rules = repository.createRules();

    assertThat(rules).onProperty("key").containsOnly(
        "JUnitStaticSuite",
        "JUnitSpelling",
        "JUnitAssertionsShouldIncludeMessage",
        "JUnitTestsShouldIncludeAssert",
        "TestClassWithoutTestCases",
        "UnnecessaryBooleanAssertion",
        "UseAssertEqualsInsteadOfAssertTrue",
        "UseAssertSameInsteadOfAssertTrue",
        "UseAssertNullInsteadOfAssertTrue",
        "SimplifyBooleanAssertion");
  }

  @Test
  public void should_provide_a_name_and_description_for_each_rule() {
    List<Rule> rules = RuleRepositoryTestHelper.createRulesWithNameAndDescription("pmd", repository);

    assertThat(rules).onProperty("name").excludes(null, "");
    assertThat(rules).onProperty("description").excludes(null, "");
  }
}

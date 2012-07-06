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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.PropertyType;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.XMLRuleParser;
import org.sonar.test.TestUtils;
import org.sonar.test.i18n.RuleRepositoryTestHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PmdRuleRepositoryTest {
  PmdRuleRepository repository;

  ServerFileSystem fileSystem = mock(ServerFileSystem.class);

  @Before
  public void setUpRuleRepository() {
    repository = new PmdRuleRepository(fileSystem, new XMLRuleParser());
  }

  @Test
  public void should_have_correct_name_and_key() {
    assertThat(repository.getKey()).isEqualTo("pmd");
    assertThat(repository.getLanguage()).isEqualTo("java");
    assertThat(repository.getName()).isEqualTo("PMD");
  }

  @Test
  public void should_load_repository_from_xml() {
    List<Rule> rules = repository.createRules();

    assertThat(rules.size()).isGreaterThan(100);
  }

  @Test
  public void should_load_extensions() {
    File file = TestUtils.getResource("/org/sonar/plugins/pmd/rules-extension.xml");
    when(fileSystem.getExtensions("pmd", "xml")).thenReturn(Arrays.asList(file));

    List<Rule> rules = repository.createRules();

    assertThat(rules).onProperty("key").contains("Extension");
  }

  @Test
  public void should_exclude_junit_rules() {
    List<Rule> rules = repository.createRules();

    assertThat(rules).onProperty("key").excludes("JUnitStaticSuite");
  }

  @Test
  public void should_use_text_parameter_for_xpath_rule() {
    Rule xpathRule = Iterables.find(repository.createRules(), new Predicate<Rule>() {
      public boolean apply(Rule rule) {
        return rule.getKey().equals("XPathRule");
      }
    });

    assertThat(xpathRule.getParam("xpath").getType()).isEqualTo(PropertyType.TEXT.name());
  }

  @Test
  public void should_provide_a_name_and_description_for_each_rule() {
    List<Rule> rules = RuleRepositoryTestHelper.createRulesWithNameAndDescription("pmd", repository);

    assertThat(rules).onProperty("name").excludes(null, "");
    assertThat(rules).onProperty("description").excludes(null, "");
  }
}

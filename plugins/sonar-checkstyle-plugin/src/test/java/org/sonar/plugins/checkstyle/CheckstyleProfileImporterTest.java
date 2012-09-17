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
package org.sonar.plugins.checkstyle;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.test.TestUtils;

import java.io.Reader;
import java.io.StringReader;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckstyleProfileImporterTest {

  private ValidationMessages messages;
  private CheckstyleProfileImporter importer;

  @Before
  public void before() {
    messages = ValidationMessages.create();

    /*
     * The mocked rule finder defines 2 rules :
     * 
     * - JavadocCheck with 2 paramters format and ignore, default priority is MAJOR
     * - EqualsHashCodeCheck without parameters, default priority is BLOCKER
     */
    importer = new CheckstyleProfileImporter(newRuleFinder());
  }

  @Test
  public void importSimpleProfile() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileImporterTest/simple.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    assertThat(profile.getActiveRules().size(), is(2));
    assertNotNull(profile.getActiveRuleByConfigKey("checkstyle", "Checker/TreeWalker/EqualsHashCode"));
    assertNotNull(profile.getActiveRuleByConfigKey("checkstyle", "Checker/JavadocPackage"));
    assertThat(messages.hasErrors(), is(false));
  }

  @Test
  public void importParameters() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileImporterTest/simple.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    ActiveRule javadocCheck = profile.getActiveRuleByConfigKey("checkstyle", "Checker/JavadocPackage");
    assertThat(javadocCheck.getActiveRuleParams().size(), is(2));
    assertThat(javadocCheck.getParameter("format"), is("abcde"));
    assertThat(javadocCheck.getParameter("ignore"), is("true"));
    assertThat(javadocCheck.getParameter("severity"), nullValue()); // checkstyle internal parameter
  }

  @Test
  public void importPriorities() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileImporterTest/simple.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    ActiveRule javadocCheck = profile.getActiveRuleByConfigKey("checkstyle", "Checker/JavadocPackage");
    assertThat(javadocCheck.getSeverity(), is(RulePriority.BLOCKER));
  }

  @Test
  public void priorityIsOptional() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileImporterTest/simple.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    ActiveRule activeRule = profile.getActiveRuleByConfigKey("checkstyle", "Checker/TreeWalker/EqualsHashCode");
    assertThat(activeRule.getSeverity(), is(RulePriority.BLOCKER)); // reuse the rule default priority
  }

  @Test
  public void idPropertyShouldBeTheRuleKey() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileImporterTest/idPropertyShouldBeTheRuleKey.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    assertNull(profile.getActiveRuleByConfigKey("checkstyle", "Checker/JavadocPackage"));
    assertThat(messages.getWarnings().size(), is(1));
  }

  @Test
  public void shouldUseTheIdPropertyToFindRule() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileImporterTest/shouldUseTheIdPropertyToFindRule.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    assertNotNull(profile.getActiveRuleByConfigKey("checkstyle", "Checker/JavadocPackage"));
    assertThat(profile.getActiveRuleByConfigKey("checkstyle", "Checker/JavadocPackage").getRule().getKey(),
        is("com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocPackageCheck_12345"));
    assertThat(messages.getWarnings().size(), is(0));
  }

  @Test
  public void testUnvalidXML() {
    Reader reader = new StringReader("not xml");
    importer.importProfile(reader, messages);
    assertThat(messages.getErrors().size(), is(1));
  }

  @Test
  public void importingFiltersIsNotSupported() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileImporterTest/importingFiltersIsNotSupported.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    assertNull(profile.getActiveRuleByConfigKey("checkstyle", "Checker/SuppressionCommentFilter"));
    assertNull(profile.getActiveRuleByConfigKey("checkstyle", "Checker/TreeWalker/FileContentsHolder"));
    assertThat(profile.getActiveRules().size(), is(2));
    assertThat(messages.getWarnings().size(), is(1)); // no warning for FileContentsHolder
  }

  private RuleFinder newRuleFinder() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.find(any(RuleQuery.class))).thenAnswer(new Answer<Rule>() {
      public Rule answer(InvocationOnMock iom) throws Throwable {
        RuleQuery query = (RuleQuery) iom.getArguments()[0];
        Rule rule = null;
        if (StringUtils.equals(query.getConfigKey(), "Checker/JavadocPackage")) {
          rule = Rule.create(query.getRepositoryKey(), "com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocPackageCheck", "Javadoc Package")
              .setConfigKey("Checker/JavadocPackage")
              .setSeverity(RulePriority.MAJOR);
          rule.createParameter("format");
          rule.createParameter("ignore");

        } else if (StringUtils.equals(query.getConfigKey(), "Checker/TreeWalker/EqualsHashCode")) {
          rule = Rule.create(query.getRepositoryKey(), "com.puppycrawl.tools.checkstyle.checks.coding.EqualsHashCodeCheck", "Equals HashCode")
              .setConfigKey("Checker/TreeWalker/EqualsHashCode")
              .setSeverity(RulePriority.BLOCKER);

        } else if (StringUtils.equals(query.getKey(), "com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocPackageCheck_12345")) {
          rule = Rule.create(query.getRepositoryKey(), "com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocPackageCheck_12345", "Javadoc Package")
              .setConfigKey("Checker/JavadocPackage")
              .setSeverity(RulePriority.MAJOR);
          rule.createParameter("format");
          rule.createParameter("ignore");
        }
        return rule;
      }
    });
    return ruleFinder;
  }

}

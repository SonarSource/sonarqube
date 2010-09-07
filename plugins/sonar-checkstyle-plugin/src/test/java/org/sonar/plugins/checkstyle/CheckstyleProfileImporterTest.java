/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.profiles.ProfilePrototype;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.test.TestUtils;

import java.io.Reader;
import java.io.StringReader;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class CheckstyleProfileImporterTest {

  private ValidationMessages messages;
  private CheckstyleProfileImporter importer;

  @Before
  public void before() {
    messages = ValidationMessages.create();
    importer = new CheckstyleProfileImporter();
  }

  @Test
  public void importSimpleProfile() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileImporterTest/simple.xml"));
    ProfilePrototype profile = importer.importProfile(reader, messages);

    assertThat(profile.getRules().size(), is(2));
    assertNotNull(profile.getRuleByConfigKey("checkstyle", "Checker/TreeWalker/EqualsHashCode"));
    assertNotNull(profile.getRuleByConfigKey("checkstyle", "Checker/JavadocPackage"));
    assertThat(messages.hasErrors(), is(false));
  }

  @Test
  public void importParameters() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileImporterTest/simple.xml"));
    ProfilePrototype profile = importer.importProfile(reader, messages);

    ProfilePrototype.RulePrototype javadocCheck = profile.getRuleByConfigKey("checkstyle", "Checker/JavadocPackage");
    assertThat(javadocCheck.getParameters().size(), is(2));
    assertThat(javadocCheck.getParameter("format"), is("abcde"));
    assertThat(javadocCheck.getParameter("ignore"), is("true"));
    assertThat(javadocCheck.getParameter("severity"), nullValue()); // checkstyle internal parameter
  }

  @Test
  public void importPriorities() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileImporterTest/simple.xml"));
    ProfilePrototype profile = importer.importProfile(reader, messages);

    ProfilePrototype.RulePrototype javadocCheck = profile.getRuleByConfigKey("checkstyle", "Checker/JavadocPackage");
    assertThat(javadocCheck.getPriority(), is(RulePriority.BLOCKER));
  }

  @Test
  public void priorityIsOptional() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileImporterTest/simple.xml"));
    ProfilePrototype profile = importer.importProfile(reader, messages);

    ProfilePrototype.RulePrototype check = profile.getRuleByConfigKey("checkstyle", "Checker/TreeWalker/EqualsHashCode");
    assertThat(check.getPriority(), nullValue());
  }

  @Test
  public void idPropertyIsNotSupported() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileImporterTest/idProperty.xml"));
    ProfilePrototype profile = importer.importProfile(reader, messages);

    ProfilePrototype.RulePrototype check = profile.getRuleByConfigKey("checkstyle", "Checker/JavadocPackage");
    assertThat(check.getParameter("id"), nullValue());
    assertThat(messages.getWarnings().size(), is(1));
  }

  @Test
  public void testUnvalidXML() {
    Reader reader = new StringReader("not xml");
    importer.importProfile(reader, messages);
    assertThat(messages.getErrors().size(), is(1));
  }
}

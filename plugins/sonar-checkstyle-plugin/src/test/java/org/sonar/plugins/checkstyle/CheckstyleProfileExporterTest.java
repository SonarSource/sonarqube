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

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.test.TestUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringWriter;

public class CheckstyleProfileExporterTest {

  @Test
  public void alwaysSetFileContentsHolderAndSuppressionCommentFilter() throws IOException, SAXException {
    RulesProfile profile = RulesProfile.create("sonar way", "java");

    StringWriter writer = new StringWriter();
    new CheckstyleProfileExporter().exportProfile(profile, writer);

    TestUtils.assertSimilarXml(
        TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileExporterTest/alwaysSetFileContentsHolderAndSuppressionCommentFilter.xml"),
        writer.toString());
  }
  @Test
  public void noCheckstyleRulesToExport() throws IOException, SAXException {
    RulesProfile profile = RulesProfile.create("sonar way", "java");

    // this is a PMD rule
    profile.activateRule(Rule.create("pmd", "PmdRule1", "PMD rule one"), null);

    StringWriter writer = new StringWriter();
    new CheckstyleProfileExporter().exportProfile(profile, writer);

    TestUtils.assertSimilarXml(
        TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileExporterTest/noCheckstyleRulesToExport.xml"),
        writer.toString());
  }

  @Test
  public void singleCheckstyleRulesToExport() throws IOException, SAXException {
    RulesProfile profile = RulesProfile.create("sonar way", "java");
    profile.activateRule(Rule.create("pmd", "PmdRule1", "PMD rule one"), null);
    profile.activateRule(
        Rule.create("checkstyle", "com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocPackageCheck", "Javadoc").setConfigKey("Checker/JavadocPackage"),
        RulePriority.MAJOR
    );
    profile.activateRule(Rule.create("checkstyle", "com.puppycrawl.tools.checkstyle.checks.sizes.LineLengthCheck", "Line Length").setConfigKey("Checker/TreeWalker/LineLength"),
        RulePriority.CRITICAL);
    profile.activateRule(Rule.create("checkstyle", "com.puppycrawl.tools.checkstyle.checks.naming.LocalFinalVariableNameCheck", "Local Variable").setConfigKey("Checker/TreeWalker/Checker/TreeWalker/LocalFinalVariableName"),
        RulePriority.MINOR);

    StringWriter writer = new StringWriter();
    new CheckstyleProfileExporter().exportProfile(profile, writer);

    TestUtils.assertSimilarXml(
        TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileExporterTest/singleCheckstyleRulesToExport.xml"),
        writer.toString());
  }

  @Test
  public void addTheIdPropertyWhenManyInstancesWithTheSameConfigKey() throws IOException, SAXException {
    RulesProfile profile = RulesProfile.create("sonar way", "java");
    Rule rule1 = Rule.create("checkstyle", "com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocPackageCheck", "Javadoc").setConfigKey("Checker/JavadocPackage");
    Rule rule2 = Rule.create("checkstyle", "com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocPackageCheck_12345", "Javadoc").setConfigKey("Checker/JavadocPackage");

    profile.activateRule(rule1, RulePriority.MAJOR);
    profile.activateRule(rule2, RulePriority.CRITICAL);

    StringWriter writer = new StringWriter();
    new CheckstyleProfileExporter().exportProfile(profile, writer);

    TestUtils.assertSimilarXml(
        TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileExporterTest/addTheIdPropertyWhenManyInstancesWithTheSameConfigKey.xml"),
        writer.toString());
  }

  @Test
  public void exportParameters() throws IOException, SAXException {
    RulesProfile profile = RulesProfile.create("sonar way", "java");
    Rule rule = Rule.create("checkstyle", "com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocPackageCheck", "Javadoc")
        .setConfigKey("Checker/JavadocPackage");
    rule.createParameter("format");
    rule.createParameter("message"); // not set in the profile and no default value => not exported in checkstyle
    rule.createParameter("ignore").setDefaultValue("true");

    profile.activateRule(rule, RulePriority.MAJOR)
        .setParameter("format", "abcde");

    StringWriter writer = new StringWriter();
    new CheckstyleProfileExporter().exportProfile(profile, writer);

    TestUtils.assertSimilarXml(
        TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileExporterTest/exportParameters.xml"),
        writer.toString());
  }


  @Test
  public void addCustomFilters() throws IOException, SAXException {
    Configuration conf = new BaseConfiguration();
    conf.addProperty(CheckstyleConstants.FILTERS_KEY,
    		"<module name=\"SuppressionCommentFilter\">"
        + "<property name=\"offCommentFormat\" value=\"BEGIN GENERATED CODE\"/>"
        + "<property name=\"onCommentFormat\" value=\"END GENERATED CODE\"/>" + "</module>"
        +"<module name=\"SuppressWithNearbyCommentFilter\">"
        + "<property name=\"commentFormat\" value=\"CHECKSTYLE IGNORE (\\w+) FOR NEXT (\\d+) LINES\"/>"
        + "<property name=\"checkFormat\" value=\"$1\"/>"
        + "<property name=\"messageFormat\" value=\"$2\"/>"
        + "</module>");

    RulesProfile profile = RulesProfile.create("sonar way", "java");
    StringWriter writer = new StringWriter();
    new CheckstyleProfileExporter(conf).exportProfile(profile, writer);

    TestUtils.assertSimilarXml(
        TestUtils.getResourceContent("/org/sonar/plugins/checkstyle/CheckstyleProfileExporterTest/addCustomFilters.xml"),
        writer.toString());
  }
}

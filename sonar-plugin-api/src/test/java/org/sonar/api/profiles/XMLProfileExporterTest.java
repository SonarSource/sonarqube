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
package org.sonar.api.profiles;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.Assert.assertTrue;

public class XMLProfileExporterTest {

  @Test
  public void exportEmptyProfile() throws IOException, SAXException {
    Writer writer = new StringWriter();
    RulesProfile profile = RulesProfile.create("sonar way", "java");
    XMLProfileExporter.create().exportProfile(profile, writer);

    assertSimilarXml("/org/sonar/api/profiles/XMLProfileExporterTest/exportEmptyProfile.xml", writer.toString());
  }

  @Test
  public void exportProfile() throws IOException, SAXException {
    Writer writer = new StringWriter();
    RulesProfile profile = RulesProfile.create("sonar way", "java");
    profile.activateRule(Rule.create("checkstyle", "IllegalRegexp", "illegal regexp"), RulePriority.BLOCKER);
    XMLProfileExporter.create().exportProfile(profile, writer);

    assertSimilarXml("/org/sonar/api/profiles/XMLProfileExporterTest/exportProfile.xml", writer.toString());
  }

  @Test
  public void exportRuleParameters() throws IOException, SAXException {
    Writer writer = new StringWriter();
    RulesProfile profile = RulesProfile.create("sonar way", "java");
    Rule rule = Rule.create("checkstyle", "IllegalRegexp", "illegal regexp");
    rule.createParameter("format");
    rule.createParameter("message");
    rule.createParameter("tokens");

    ActiveRule activeRule = profile.activateRule(rule, RulePriority.BLOCKER);
    activeRule.setParameter("format", "foo");
    activeRule.setParameter("message", "with special characters < > &");
    // the tokens parameter is not set
    XMLProfileExporter.create().exportProfile(profile, writer);

    assertSimilarXml("/org/sonar/api/profiles/XMLProfileExporterTest/exportRuleParameters.xml", writer.toString());
  }

  public static void assertSimilarXml(String pathToExpectedXml, String xml) throws IOException, SAXException {
    InputStream stream = XMLProfileExporterTest.class.getResourceAsStream(pathToExpectedXml);
    try {
      Diff diff = isSimilarXml(IOUtils.toString(stream), xml);
      String message = "Diff: " + diff.toString() + CharUtils.LF + "XML: " + xml;
      assertTrue(message, diff.similar());

    } finally {
      IOUtils.closeQuietly(stream);
    }
  }

  static Diff isSimilarXml(String expectedXml, String xml) throws IOException, SAXException {
    XMLUnit.setIgnoreWhitespace(true);
    Diff diff = XMLUnit.compareXML(xml, expectedXml);
    return diff;
  }
}

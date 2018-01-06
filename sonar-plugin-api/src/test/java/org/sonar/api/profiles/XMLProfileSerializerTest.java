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
package org.sonar.api.profiles;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThat;

public class XMLProfileSerializerTest {

  @Test
  public void exportEmptyProfile() throws IOException, SAXException {
    Writer writer = new StringWriter();
    RulesProfile profile = RulesProfile.create("sonar way", "java");
    new XMLProfileSerializer().write(profile, writer);

    assertSimilarXml("exportEmptyProfile.xml", writer.toString());
  }

  @Test
  public void exportProfile() throws IOException, SAXException {
    Writer writer = new StringWriter();
    RulesProfile profile = RulesProfile.create("sonar way", "java");
    profile.activateRule(Rule.create("checkstyle", "IllegalRegexp", "illegal regexp"), RulePriority.BLOCKER);
    new XMLProfileSerializer().write(profile, writer);

    assertSimilarXml("exportProfile.xml", writer.toString());
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
    new XMLProfileSerializer().write(profile, writer);

    assertSimilarXml("exportRuleParameters.xml", writer.toString());
  }

  private void assertSimilarXml(String fileWithExpectedXml, String xml) throws IOException, SAXException {
    String pathToExpectedXml = "XMLProfileSerializerTest/" + fileWithExpectedXml;
    assertThat(xml).isXmlEqualTo(IOUtils.toString(getClass().getResource(pathToExpectedXml)));
  }
}

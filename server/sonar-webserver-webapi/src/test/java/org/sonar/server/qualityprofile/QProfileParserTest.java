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
package org.sonar.server.qualityprofile;

import java.io.Reader;
import java.io.StringReader;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QProfileParserTest {

  @Test
  public void readOlderVersionXml() {
    Reader backup = new StringReader("<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile>" +
      "<name>custom rule</name>" +
      "<language>js</language>" +
      "<rules><rule>" +
      "<repositoryKey>sonarjs</repositoryKey>" +
      "<key>s001</key>" +
      "<type>CODE_SMELL</type>" +
      "<priority>CRITICAL</priority>" +
      "<name>custom rule name</name>" +
      "<templateKey>rule_mc8</templateKey>" +
      "<description>custom rule description</description>" +
      "<parameters><parameter>" +
      "<key>bar</key>" +
      "<value>baz</value>" +
      "</parameter>" +
      "</parameters>" +
      "</rule></rules></profile>");
    var parser = new QProfileParser();
    var importedQProfile = parser.readXml(backup);
    assertThat(importedQProfile.getRules()).hasSize(1);
    var importedRule = importedQProfile.getRules().get(0);
    assertThat(importedRule.getDescription()).isEqualTo("custom rule description");
  }

  @Test
  public void readNewVersionXml() {
    Reader backup = new StringReader("<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile>" +
      "<name>custom rule</name>" +
      "<language>js</language>" +
      "<rules><rule>" +
      "<repositoryKey>sonarjs</repositoryKey>" +
      "<key>s001</key>" +
      "<type>CODE_SMELL</type>" +
      "<priority>CRITICAL</priority>" +
      "<name>custom rule name</name>" +
      "<templateKey>rule_mc8</templateKey>" +
      "<descriptionSections><descriptionSection><key>default</key><content>custom rule description</content></descriptionSection></descriptionSections>" +
      "<parameters><parameter>" +
      "<key>bar</key>" +
      "<value>baz</value>" +
      "</parameter>" +
      "</parameters>" +
      "</rule></rules></profile>");
    var parser = new QProfileParser();
    var importedQProfile = parser.readXml(backup);
    assertThat(importedQProfile.getRules()).hasSize(1);
    var importedRule = importedQProfile.getRules().get(0);
    assertThat(importedRule.getRuleDescriptionSections()).hasSize(1);
    var section = importedRule.getRuleDescriptionSections().iterator().next();
    assertThat(section.getKey()).isEqualTo("default");
    assertThat(section.getDescription()).isEqualTo("custom rule description");
  }
}

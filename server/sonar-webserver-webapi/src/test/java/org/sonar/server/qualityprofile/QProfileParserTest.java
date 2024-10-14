/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.INFO;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;

class QProfileParserTest {

  @Test
  void readXml() {
    Reader backup = new StringReader("""
      <?xml version='1.0' encoding='UTF-8'?>
      <profile>
        <name>custom rule</name>
        <language>js</language>
        <rules>
          <rule>
            <repositoryKey>sonarjs</repositoryKey>
            <key>s001</key>
            <type>CODE_SMELL</type>
            <priority>CRITICAL</priority>
            <impacts>
              <impact>
                <softwareQuality>MAINTAINABILITY</softwareQuality>
                <severity>BLOCKER</severity>
               </impact>
               <impact>
                <softwareQuality>SECURITY</softwareQuality>
                <severity>INFO</severity>
               </impact>
              </impacts>
            <name>custom rule name</name>
            <templateKey>rule_mc8</templateKey>
            <description>custom rule description</description>
            <parameters>
              <parameter>
                <key>bar</key>
                <value>baz</value>
              </parameter>
            </parameters>
          </rule>
          <rule>
            <repositoryKey>sonarjs</repositoryKey>
            <key>s002</key>
            <type>CODE_SMELL</type>
            <priority>CRITICAL</priority>
            <prioritizedRule>true</prioritizedRule>
            <name>custom rule name</name>
            <templateKey>rule_mc8</templateKey>
            <description>custom rule description</description>
            <parameters>
              <parameter>
                <key>bar</key>
                <value>baz</value>
              </parameter>
            </parameters>
          </rule>
          <rule>
            <repositoryKey>sonarjs</repositoryKey>
            <key>s003</key>
            <type>CODE_SMELL</type>
            <priority>CRITICAL</priority>
            <prioritizedRule>false</prioritizedRule>
            <impacts></impacts>
            <name>custom rule name</name>
            <templateKey>rule_mc8</templateKey>
            <description>custom rule description</description>
            <parameters>
              <parameter>
                <key>bar</key>
                <value>baz</value>
              </parameter>
            </parameters>
          </rule>
        </rules>
      </profile>""");
    var parser = new QProfileParser();
    var importedQProfile = parser.readXml(backup);
    assertThat(importedQProfile.getRules()).hasSize(3);
    var importedRule = importedQProfile.getRules().get(0);
    assertThat(importedRule.getDescription()).isEqualTo("custom rule description");
    assertThat(importedRule.getPrioritizedRule()).isFalse();
    assertThat(importedRule.getImpacts()).isEqualTo(Map.of(MAINTAINABILITY, BLOCKER, SECURITY, INFO));
    importedRule = importedQProfile.getRules().get(1);
    assertThat(importedRule.getPrioritizedRule()).isTrue();
    assertThat(importedRule.getImpacts()).isEmpty();
    importedRule = importedQProfile.getRules().get(2);
    assertThat(importedRule.getPrioritizedRule()).isFalse();
    assertThat(importedRule.getImpacts()).isEmpty();
  }

  @Test
    //test that exception is thrown if impacts has incorrect arguments
  void readXml_whenImpactsIncorrectArguments_shouldThrow() {
    Reader backup = new StringReader("""
      <?xml version='1.0' encoding='UTF-8'?>
      <profile>
        <name>custom rule</name>
        <language>js</language>
        <rules>
          <rule>
            <repositoryKey>sonarjs</repositoryKey>
            <key>s001</key>
            <type>CODE_SMELL</type>
            <priority>CRITICAL</priority>
            <impacts>
              <impact>
                <softwareQuality>MAINTAINABILITY</softwareQuality>
                <severity>BLOCKER</severity>
              </impact>
              <impact>
                <softwareQuality>SECURITY</softwareQuality>
                <severity>INFO</severity>
              </impact>
              <impact>
                <softwareQuality>RELIABILITY</softwareQuality>
                <severity>MAJOR</severity>
              <impact>
            </impacts>
            <name>custom rule name</name>
            <templateKey>rule_mc8</templateKey>
            <description>custom rule description</description>
            <parameters>
              <parameter>
                <key>bar</key>
                <value>baz</value>
              </parameter>
            </parameters>
          </rule>
        </rules>
      </profile>""");
    var parser = new QProfileParser();
    assertThatThrownBy(() -> parser.readXml(backup))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No enum constant org.sonar.api.issue.impact.Severity.MAJOR");
  }
}

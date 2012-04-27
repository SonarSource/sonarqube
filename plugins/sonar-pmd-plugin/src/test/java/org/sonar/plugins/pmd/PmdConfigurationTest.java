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

import net.sourceforge.pmd.Report;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PmdConfigurationTest {
  PmdConfiguration configuration;

  Settings settings = new Settings();
  ProjectFileSystem fs = mock(ProjectFileSystem.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUpPmdConfiguration() {
    configuration = new PmdConfiguration(fs, settings);
  }

  @Test
  public void should_return_default_target_xml_report_when_property_is_not_set() {
    File targetXMLReport = configuration.getTargetXMLReport();

    assertThat(targetXMLReport).isNull();
  }

  @Test
  public void should_set_target_xml_report() {
    when(fs.resolvePath("pmd-result.xml")).thenReturn(new File("/workingDir/pmd-result.xml"));

    settings.setProperty(PmdConfiguration.PROPERTY_GENERATE_XML, true);
    File targetXMLReport = configuration.getTargetXMLReport();

    assertThat(targetXMLReport).isEqualTo(new File("/workingDir/pmd-result.xml"));
  }

  @Test
  public void should_dump_xml_rule_set() throws IOException {
    when(fs.writeToWorkingDirectory("<rules>", "pmd.xml")).thenReturn(new File("/workingDir/pmd.xml"));

    File rulesFile = configuration.dumpXmlRuleSet("pmd", "<rules>");

    assertThat(rulesFile).isEqualTo(new File("/workingDir/pmd.xml"));
  }

  @Test
  public void should_fail_to_dump_xml_rule_set() throws IOException {
    when(fs.writeToWorkingDirectory("<xml>", "pmd.xml")).thenThrow(new IOException("BUG"));

    expectedException.expect(SonarException.class);
    expectedException.expectMessage("Fail to save the PMD configuration");

    configuration.dumpXmlRuleSet("pmd", "<xml>");
  }

  @Test
  public void should_dump_xml_report() throws IOException {
    when(fs.writeToWorkingDirectory(matches(".*[\r\n]*<pmd.*[\r\n].*</pmd>"), eq("pmd-result.xml"))).thenReturn(new File("/workingDir/pmd-result.xml"));

    settings.setProperty(PmdConfiguration.PROPERTY_GENERATE_XML, true);
    File reportFile = configuration.dumpXmlReport(new Report());

    assertThat(reportFile).isEqualTo(new File("/workingDir/pmd-result.xml"));
  }

  @Test
  public void should_ignore_xml_report_when_property_is_not_set() {
    File reportFile = configuration.dumpXmlReport(new Report());

    assertThat(reportFile).isNull();
    verifyZeroInteractions(fs);
  }
}

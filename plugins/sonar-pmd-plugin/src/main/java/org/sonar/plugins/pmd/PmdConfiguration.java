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
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.XMLRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.Property;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

@org.sonar.api.Properties({
  @Property(
    key = PmdConfiguration.PROPERTY_GENERATE_XML,
    defaultValue = "false",
    name = "Generate XML Report",
    project = false,
    global = false
  )
})
public class PmdConfiguration implements BatchExtension {
  private static final Logger LOG = LoggerFactory.getLogger(PmdConfiguration.class);

  public static final String PROPERTY_GENERATE_XML = "sonar.pmd.generateXml";
  public static final String PMD_RESULT_XML = "pmd-result.xml";

  private final ProjectFileSystem projectFileSystem;
  private final Settings settings;

  public PmdConfiguration(ProjectFileSystem projectFileSystem, Settings settings) {
    this.projectFileSystem = projectFileSystem;
    this.settings = settings;
  }

  public File getTargetXMLReport() {
    if (settings.getBoolean(PROPERTY_GENERATE_XML)) {
      return projectFileSystem.resolvePath(PMD_RESULT_XML);
    }
    return null;
  }

  public File dumpXmlRuleSet(String repositoryKey, String rulesXml) {
    try {
      File configurationFile = projectFileSystem.writeToWorkingDirectory(rulesXml, repositoryKey + ".xml");

      LOG.info("PMD configuration: " + configurationFile.getAbsolutePath());

      return configurationFile;
    } catch (IOException e) {
      throw new SonarException("Fail to save the PMD configuration", e);
    }
  }

  public File dumpXmlReport(Report report) {
    if (!settings.getBoolean(PROPERTY_GENERATE_XML)) {
      return null;
    }

    try {
      String reportAsString = reportToString(report);

      File reportFile = projectFileSystem.writeToWorkingDirectory(reportAsString, PMD_RESULT_XML);

      LOG.info("PMD output report: " + reportFile.getAbsolutePath());

      return reportFile;
    } catch (IOException e) {
      throw new SonarException("Fail to save the PMD report", e);
    }
  }

  private static String reportToString(Report report) throws IOException {
    StringWriter output = new StringWriter();

    Renderer xmlRenderer = new XMLRenderer();
    xmlRenderer.setWriter(output);
    xmlRenderer.start();
    xmlRenderer.renderFileReport(report);
    xmlRenderer.end();

    return output.toString();
  }
}

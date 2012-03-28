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

import org.sonar.api.BatchExtension;
import org.sonar.api.Property;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

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

  public static final String PROPERTY_GENERATE_XML = "sonar.pmd.generateXml";

  private PmdProfileExporter pmdProfileExporter;
  private RulesProfile rulesProfile;
  private Project project;
  private Settings settings;

  public PmdConfiguration(PmdProfileExporter pmdRulesRepository, RulesProfile rulesProfile, Project project, Settings settings) {
    this.pmdProfileExporter = pmdRulesRepository;
    this.rulesProfile = rulesProfile;
    this.project = project;
    this.settings = settings;
  }

  public List<String> getRulesets() {
    return Arrays.asList(saveXmlFile().getAbsolutePath());
  }

  private File saveXmlFile() {
    try {
      StringWriter pmdConfiguration = new StringWriter();
      pmdProfileExporter.exportProfile(rulesProfile, pmdConfiguration);
      return project.getFileSystem().writeToWorkingDirectory(pmdConfiguration.toString(), "pmd.xml");

    } catch (IOException e) {
      throw new SonarException("Fail to save the PMD configuration", e);
    }
  }

  public File getTargetXMLReport() {
    if (settings.getBoolean(PROPERTY_GENERATE_XML)) {
      return new File(project.getFileSystem().getSonarWorkingDirectory(), "pmd-result.xml");
    }
    return null;
  }
}

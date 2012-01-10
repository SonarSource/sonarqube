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
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

public class PmdConfiguration implements BatchExtension {

  private PmdProfileExporter pmdProfileExporter;
  private RulesProfile rulesProfile;
  private Project project;

  public PmdConfiguration(PmdProfileExporter pmdRulesRepository, RulesProfile rulesProfile, Project project) {
    this.pmdProfileExporter = pmdRulesRepository;
    this.rulesProfile = rulesProfile;
    this.project = project;
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
      throw new RuntimeException("Fail to save the PMD configuration", e);
    }
  }
}

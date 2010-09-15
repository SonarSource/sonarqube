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
package org.sonar.plugins.pmd;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenUtils;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;

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
    if (project.getReuseExistingRulesConfig()) {
      return getDeclaredRulesets();
    }
    return Arrays.asList(saveXmlFile().getAbsolutePath());
  }

  private List<String> getDeclaredRulesets() {
    List<String> rulesets = null;
    MavenPlugin mavenPlugin = MavenPlugin.getPlugin(project.getPom(), MavenUtils.GROUP_ID_APACHE_MAVEN, "maven-pmd-plugin");
    if (mavenPlugin != null) {
      String[] params = mavenPlugin.getParameters("rulesets/ruleset");
      if (params != null) {
        rulesets = Arrays.asList(params);
      }
    }
    if (rulesets == null || rulesets.isEmpty()) {
      throw new RuntimeException("The PMD configuration to reuse can not be found. Check the property "
          + CoreProperties.REUSE_RULES_CONFIGURATION_PROPERTY + " or add the property rulesets/ruleset to the Maven PMD plugin");
    }
    return rulesets;
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
